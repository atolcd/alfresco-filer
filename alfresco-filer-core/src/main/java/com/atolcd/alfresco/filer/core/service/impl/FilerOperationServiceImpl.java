package com.atolcd.alfresco.filer.core.service.impl;

import java.util.Collections;
import java.util.function.Consumer;

import org.alfresco.model.ContentModel;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.security.PermissionService;
import org.alfresco.service.namespace.QName;
import org.alfresco.service.namespace.RegexQNamePattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.atolcd.alfresco.filer.core.model.FilerAction;
import com.atolcd.alfresco.filer.core.model.RepositoryNode;
import com.atolcd.alfresco.filer.core.service.FilerFolderService;
import com.atolcd.alfresco.filer.core.service.FilerModelService;
import com.atolcd.alfresco.filer.core.service.FilerOperationService;
import com.atolcd.alfresco.filer.core.service.FilerUpdateService;
import com.atolcd.alfresco.filer.core.util.FilerNodeUtils;
import com.atolcd.alfresco.filer.core.util.FilerTransactionUtils;

public class FilerOperationServiceImpl implements FilerOperationService {

  private static final Logger LOGGER = LoggerFactory.getLogger(FilerOperationServiceImpl.class);

  private FilerModelService filerModelService;
  private FilerFolderService filerFolderService;
  private FilerUpdateService filerUpdateService;
  private NodeService nodeService;
  private PermissionService permissionService;

  @Override
  public void execute(final FilerAction action, final RepositoryNode node) {
    filerModelService.runWithoutFileableBehaviour(node.getNodeRef(), () -> {
      action.execute(node);
    });
  }

  @Override
  public void setSegment(final NodeRef nodeRef) {
    nodeService.addAspect(nodeRef, filerModelService.getSegmentAspect(), Collections.emptyMap());
  }

  @Override
  public void setFileable(final NodeRef nodeRef) {
    nodeService.addAspect(nodeRef, filerModelService.getFileableAspect(), Collections.emptyMap());
  }

  @Override
  public void setSubscriber(final NodeRef nodeRef) {
    nodeService.addAspect(nodeRef, filerModelService.getSubscriberAspect(), Collections.emptyMap());
  }

  @Override
  public NodeRef getFolder(final NodeRef parent, final String name, final Consumer<NodeRef> onGet) {
    RepositoryNode node = RepositoryNode.builder().parent(parent).named(name).build();
    try {
      filerFolderService.fetchFolder(node, onGet);
    } catch (RuntimeException e) { // NOPMD - for logging purposes
      LOGGER.error("Could not get filer folder: {}", node, e);
      throw e;
    }
    return node.getNodeRef();
  }

  @Override
  public NodeRef getOrCreateFolder(final NodeRef parent, final QName type, final String name,
      final Consumer<NodeRef> onGet, final Consumer<NodeRef> onCreate) {
    RepositoryNode node = RepositoryNode.builder().parent(parent).type(type).named(name).build();
    try {
      filerFolderService.fetchOrCreateFolder(node, onGet, onCreate);
    } catch (RuntimeException e) { // NOPMD - for logging purposes
      LOGGER.error("Could not get or create filer folder: {}", node, e);
      throw e;
    }
    return node.getNodeRef();
  }

  @Override
  public void updateFileable(final RepositoryNode node, final NodeRef destination, final String newName) {
    node.setParent(destination);
    node.getProperties().put(ContentModel.PROP_NAME, newName);
    RepositoryNode initialNode = FilerTransactionUtils.getInitialNode(node.getNodeRef());
    RepositoryNode originalNode = FilerNodeUtils.getOriginalNode(node);
    try {
      filerUpdateService.updateAndMoveFileable(initialNode, originalNode, node);
    } catch (RuntimeException e) { // NOPMD - for logging purposes
      LOGGER.error("Could not update fileable: {}", node, e);
      throw e;
    }
    // Delete previous parent if it became an empty segment
    deleteSegment(originalNode.getParent());
  }

  @Override
  public void updateFolder(final RepositoryNode node, final Consumer<NodeRef> onGet, final Consumer<NodeRef> onCreate) {
    try {
      filerFolderService.updateFolder(node, onGet, onCreate);
    } catch (RuntimeException e) { // NOPMD - for logging purposes
      LOGGER.error("Could not update filer folder: {}", node, e);
      throw e;
    }
  }

  @Override
  public void deleteSegment(final NodeRef nodeRef) {
    try {
      // Run as System because current user may not have the permission to see all nodes nor to remove nodes
      AuthenticationUtil.runAsSystem(() -> {
        deleteSegmentRecursively(nodeRef);
        return null;
      });
    } catch (RuntimeException e) { // NOPMD - for logging purposes
      LOGGER.error("Could not remove filer segment: {}", nodeRef, e);
      throw e;
    }
  }

  private void deleteSegmentRecursively(final NodeRef nodeRef) {
    // Check that this is indeed a filer
    if (nodeService.exists(nodeRef) && nodeService.hasAspect(nodeRef, filerModelService.getSegmentAspect())) {
      // Lock it, to prevent any concurrent removal that may fail to find that it became empty
      // Indeed, another thread can be deleting the last child but may have not committed yet
      filerFolderService.lockFolder(nodeRef);
      deleteEmptySegment(nodeRef);
    }
  }

  private void deleteEmptySegment(final NodeRef nodeRef) {
    // Check that it has no child anymore
    if (nodeService.getChildAssocs(nodeRef, ContentModel.ASSOC_CONTAINS, RegexQNamePattern.MATCH_ALL).isEmpty()) {
      // Get parent nodeRef before deleting child... so the association still exists
      NodeRef parent = nodeService.getPrimaryParent(nodeRef).getParentRef();
      if (LOGGER.isDebugEnabled()) {
        LOGGER.debug("Deleting empty filer segment: {}{node={}, path=\"{}\"}",
            nodeService.getProperty(nodeRef, ContentModel.PROP_NAME),
            nodeRef.getId(),
            nodeService.getPath(nodeRef).toDisplayPath(nodeService, permissionService));
      }
      filerFolderService.deleteFolder(nodeRef);
      // Check if parent is now also an empty filer (policies are not applied recursively)
      if (parent != null) {
        deleteSegmentRecursively(parent);
      }
    }
  }

  public void setFilerModelService(final FilerModelService filerModelService) {
    this.filerModelService = filerModelService;
  }

  public void setFilerFolderService(final FilerFolderService filerFolderService) {
    this.filerFolderService = filerFolderService;
  }

  public void setFilerUpdateService(final FilerUpdateService filerUpdateService) {
    this.filerUpdateService = filerUpdateService;
  }

  public void setNodeService(final NodeService nodeService) {
    this.nodeService = nodeService;
  }

  public void setPermissionService(final PermissionService permissionService) {
    this.permissionService = permissionService;
  }
}
