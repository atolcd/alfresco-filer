package com.atolcd.alfresco.filer.core.service.impl;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import org.alfresco.model.ContentModel;
import org.alfresco.repo.copy.AbstractBaseCopyService;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.service.cmr.repository.DuplicateChildNodeNameException;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.namespace.QName;
import org.alfresco.util.GUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.ConcurrencyFailureException;

import com.atolcd.alfresco.filer.core.model.PropertyInheritancePayload;
import com.atolcd.alfresco.filer.core.model.RepositoryNode;
import com.atolcd.alfresco.filer.core.model.RepositoryNodeDifference;
import com.atolcd.alfresco.filer.core.model.UpdateAndMoveFileableParameters;
import com.atolcd.alfresco.filer.core.service.FilerFolderService;
import com.atolcd.alfresco.filer.core.service.FilerModelService;
import com.atolcd.alfresco.filer.core.service.FilerUpdateService;
import com.atolcd.alfresco.filer.core.service.PropertyInheritanceService;

import edu.umd.cs.findbugs.annotations.Nullable;

public class FilerUpdateServiceImpl extends AbstractBaseCopyService implements FilerUpdateService {

  private static final Logger LOGGER = LoggerFactory.getLogger(FilerUpdateServiceImpl.class);

  @Nullable
  private FilerModelService filerModelService;
  @Nullable
  private FilerFolderService filerFolderService;
  @Nullable
  private PropertyInheritanceService propertyInheritanceService;
  @Nullable
  private NodeService nodeService;

  private final List<Consumer<UpdateAndMoveFileableParameters>> lstUpdateAndMoveConsumers = new ArrayList<>();

  @Override
  public void updateAndMoveFileable(final RepositoryNode initialNode, final RepositoryNode originalNode,
      final RepositoryNode resultingNode) {
    NodeRef parent = resultingNode.getParent().get();
    // Disable behaviours on parent in case node needs to be moved
    filerModelService.runWithoutSubscriberBehaviour(parent, () -> {
      // Run as System because current user may not have the update permission on the node (he might not be the owner)
      AuthenticationUtil.runAsSystem(() -> {
        updateAndMoveFileableImpl(initialNode, originalNode, resultingNode);
        return null;
      });
    });
  }

  private void updateAndMoveFileableImpl(final RepositoryNode initialNode, final RepositoryNode originalNode,
      final RepositoryNode resultingNode) {
    // Ignore naming policy if node has working copy aspect
    if (resultingNode.getAspects().contains(ContentModel.ASPECT_WORKING_COPY)) {
      String name = originalNode.getName().get();
      resultingNode.getProperties().put(ContentModel.PROP_NAME, name);
    }

    // Point d'extension pour les projets métiers
    onUpdateAndMoveFileableImpl(initialNode, originalNode, resultingNode);

    // Update node (ignore node name for now) if filer made changes
    RepositoryNodeDifference originalDifference = updateFileable(originalNode, resultingNode);
    if (LOGGER.isDebugEnabled() && !originalDifference.isEmpty()) {
      LOGGER.debug("Node updated: {}", originalDifference);
    }
    // Lock target segment to prevent its deletion by another transaction
    filerFolderService.lockFolder(resultingNode.getParent().get());
    // Move and rename node
    moveAndRenameFileable(originalNode, resultingNode);
    // Update property inheritance on children
    RepositoryNodeDifference initialDifference = new RepositoryNodeDifference(initialNode, resultingNode);
    PropertyInheritancePayload inheritance = propertyInheritanceService.getPayload(initialDifference);
    propertyInheritanceService.setInheritance(resultingNode.getNodeRef().get(), inheritance);
    if (LOGGER.isDebugEnabled() && !inheritance.isEmpty()) {
      LOGGER.debug("Node inheritance updated: {}", inheritance);
    }
  }

  /**
   * Déclenchée par la méthode {@link FilerUpdateServiceImpl#updateAndMoveFileableImpl(RepositoryNode, RepositoryNode,
   * RepositoryNode)}. Permet aux modules qui utilisent le Filer d'exécuter du code pendant cet événement.
   * @param initialNode
   * @param originalNode
   * @param resultingNode
   */
  private void onUpdateAndMoveFileableImpl(final RepositoryNode initialNode, final RepositoryNode originalNode,
      final RepositoryNode resultingNode) {
    UpdateAndMoveFileableParameters param = new UpdateAndMoveFileableParameters();
    param.setInitialNode(initialNode);
    param.setOriginalNode(originalNode);
    param.setResultingNode(resultingNode);

    this.lstUpdateAndMoveConsumers.forEach(c -> c.accept(param));
  }

  @Override
  public void addOnUpdateAndMoveFileable(final Consumer<UpdateAndMoveFileableParameters> consumer) {
    this.lstUpdateAndMoveConsumers.add(consumer);
  }

  private void moveAndRenameFileable(final RepositoryNode originalNode, final RepositoryNode resultingNode) {
    NodeRef nodeRef = resultingNode.getNodeRef().get();
    String name = resultingNode.getName().get();
    boolean nameChanged = !name.equals(originalNode.getName().get());
    // Move node if parent or name changed
    if (!resultingNode.getParent().equals(originalNode.getParent()) || nameChanged) {
      // We can't set the node's name to the new name at the same time as the move.
      // To avoid incorrect violations of the name constraints, the cm:name is set to something random and will be reset
      // to the correct name later.
      String tempName = GUID.generate();
      nodeService.setProperty(nodeRef, ContentModel.PROP_NAME, tempName);
      AssociationCopyInfo info = getAssociationCopyInfo(nodeService, nodeRef, originalNode.getParent().get(), name, nameChanged);
      QName typeQName = info.getSourceParentAssoc().getTypeQName();
      nodeService.moveNode(nodeRef, resultingNode.getParent().get(), typeQName, info.getTargetAssocQName());
      // During concurrent node update, name generation can produce multiple identical node names.
      // If the problem occurs, we catch the specific exception related to duplicate node name
      // and throw a ConcurrencyFailureException which will cause a retry of the whole transaction
      // in RetryingTransactionHelper, that will run a name generation.
      try {
        nodeService.setProperty(nodeRef, ContentModel.PROP_NAME, name);
      } catch (DuplicateChildNodeNameException e) {
        // We only pass the cause of the DuplicateChildNodeNameException to the ConcurrencyFailureException
        // because DuplicateChildNodeNameException implements DoNotRetryException which would not trigger
        // the retrying transaction mechanism.
        throw new ConcurrencyFailureException("Could not rename node to: " + name, e.getCause()); // NOPMD - Preserve stack trace:
                                                                                                  // above comment
      }
    }
  }

  private RepositoryNodeDifference updateFileable(final RepositoryNode originalNode, final RepositoryNode resultingNode) {
    NodeRef nodeRef = resultingNode.getNodeRef().get();
    RepositoryNodeDifference difference = new RepositoryNodeDifference(originalNode, resultingNode);
    // Update properties
    for (QName property : difference.getPropertiesToRemove()) {
      nodeService.removeProperty(nodeRef, property);
    }
    // Do not update name, it will be updated after move
    Map<QName, Serializable> propertiesToAdd = new HashMap<>(difference.getPropertiesToAdd());
    propertiesToAdd.remove(ContentModel.PROP_NAME);
    nodeService.addProperties(nodeRef, propertiesToAdd);
    // Update aspects when properties are already set
    for (QName aspect : difference.getAspectsToRemove()) {
      nodeService.removeAspect(nodeRef, aspect);
    }
    for (QName aspect : difference.getAspectsToAdd()) {
      nodeService.addAspect(nodeRef, aspect, null);
    }
    // Update type at the end, so that mandatory aspects and properties are set
    if (difference.getTypeToSet().isPresent()) {
      nodeService.setType(nodeRef, difference.getTypeToSet().get());
    }
    return difference;
  }

  public void setFilerModelService(final FilerModelService filerModelService) {
    this.filerModelService = filerModelService;
  }

  public void setFilerFolderService(final FilerFolderService filerFolderService) {
    this.filerFolderService = filerFolderService;
  }

  public void setPropertyInheritanceService(final PropertyInheritanceService propertyInheritanceService) {
    this.propertyInheritanceService = propertyInheritanceService;
  }

  public void setNodeService(final NodeService nodeService) {
    this.nodeService = nodeService;
  }
}
