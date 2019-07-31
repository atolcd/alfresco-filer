package com.atolcd.alfresco.filer.core.service.impl;

import java.util.Arrays;
import java.util.Collection;
import java.util.Optional;
import java.util.stream.Stream;

import org.alfresco.model.ContentModel;
import org.alfresco.service.cmr.lock.LockService;
import org.alfresco.service.cmr.lock.NodeLockedException;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.security.PermissionService;
import org.alfresco.service.namespace.QName;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.atolcd.alfresco.filer.core.model.FilerAction;
import com.atolcd.alfresco.filer.core.model.FilerEvent;
import com.atolcd.alfresco.filer.core.model.RepositoryNode;
import com.atolcd.alfresco.filer.core.model.RepositoryNodeDifference;
import com.atolcd.alfresco.filer.core.model.UpdateFilerEvent;
import com.atolcd.alfresco.filer.core.service.FilerOperationService;
import com.atolcd.alfresco.filer.core.service.FilerRegistry;
import com.atolcd.alfresco.filer.core.service.FilerService;
import com.atolcd.alfresco.filer.core.service.PropertyInheritanceService;
import com.atolcd.alfresco.filer.core.util.FilerNodeUtils;
import com.atolcd.alfresco.filer.core.util.FilerTransactionUtils;

public class FilerServiceImpl implements FilerService {

  private static final Collection<QName> IGNORED_PROPERTIES = Arrays.asList(
      ContentModel.PROP_CONTENT, // Because content is never used to build a filer plan
      ContentModel.PROP_LAST_THUMBNAIL_MODIFICATION_DATA, // Added by Share while browsing parent folder
      ContentModel.PROP_CASCADE_CRC, ContentModel.PROP_CASCADE_TX, // Added by CascadeUpdateAspect on move */
      ContentModel.PROP_MODIFIER, ContentModel.PROP_MODIFIED
  );

  private static final Collection<QName> IGNORED_ASPECTS = Arrays.asList(
      ContentModel.ASPECT_THUMBNAIL_MODIFICATION // Added by Share while browsing parent folder
  );
  private static final Logger LOGGER = LoggerFactory.getLogger(FilerServiceImpl.class);

  private FilerRegistry filerRegistry;
  private FilerOperationService filerOperationService;
  private PropertyInheritanceService propertyInheritanceService;
  private NodeService nodeService;
  private PermissionService permissionService;
  private LockService lockService;

  @Override
  public void initFileable(final NodeRef nodeRef) {
    if (nodeService.exists(nodeRef)) {
      RepositoryNode initialNode = RepositoryNode.builder().nodeRef(nodeRef)
        .aspects(nodeService.getAspects(nodeRef))
        .properties(nodeService.getProperties(nodeRef)).build();
      FilerTransactionUtils.putInitialNode(nodeRef, initialNode);
    }
  }

  @Override
  public void executeAction(final FilerEvent event) {
    try {
      executeActionImpl(event);
    } catch (RuntimeException e) { // NOPMD - for logging purposes
      LOGGER.error("Could not execute action: " + event, e);
      throw e;
    }
  }

  @Override
  public boolean resolveFileable(final FilerEvent event) {
    try {
      boolean result = false;
      // Upon creation, node details may not be all set, so only perform resolution checks
      if (resolveAction(event, true)) {
        filerOperationService.setFileable(event.getNode().getNodeRef());
        result = true;
      }
      return result;
    } catch (RuntimeException e) { // NOPMD - for logging purposes
      LOGGER.error("Could not resolve fileable: " + event, e);
      throw e;
    }
  }

  private void executeActionImpl(final FilerEvent event) {
    if (resolveAction(event, false)) {
      RepositoryNode node = event.getNode();
      // Put display path for logging purposes
      if (LOGGER.isDebugEnabled()) {
        // Compute display path now, because it may not exist afterwards if a filer has been removed
        FilerNodeUtils.setDisplayPath(node,
            nodeService.getPath(node.getNodeRef()).toDisplayPath(nodeService, permissionService));
      }
      // Execute filer action
      event.setExecuted();
      filerOperationService.execute(event.getAction().get(), node);
      if (LOGGER.isDebugEnabled()) {
        String beforePath = FilerNodeUtils.getDisplayPath(node);
        String afterPath = nodeService.getPath(node.getNodeRef()).toDisplayPath(nodeService, permissionService);
        afterPath = afterPath.equals(beforePath) ? "Same location" : "It is now at " + afterPath;
        String afterName = (String) nodeService.getProperty(node.getNodeRef(), ContentModel.PROP_NAME);
        afterName = node.getName().get().equals(afterName) ? "" : " and renamed " + afterName;
        LOGGER.debug("Executed filer on {} at {}: {}{}", event, beforePath, afterPath, afterName);
      }
    }
  }

  private boolean resolveAction(final FilerEvent event, final boolean checkOnly) {
    boolean result = false;
    NodeRef nodeRef = event.getNode().getNodeRef();
    // Ensure node exists, it could have been deleted before commit (e.g. check-out/check-in working copy)
    if (nodeService.exists(nodeRef) && !isLocked(nodeRef)) {
      Optional<FilerEvent> previous = FilerTransactionUtils.getEventNode(nodeRef);
      // Retrieve information from previous event
      if (previous.isPresent()) {
        event.comesAfter(previous.get());
      }
      // Ensure action is only executed once
      if (!event.isExecuted() && initAction(event, checkOnly)) {
        FilerTransactionUtils.putEventNode(nodeRef, event);
        result = true;
      }
    }
    return result;
  }

  private boolean initAction(final FilerEvent event, final boolean checkOnly) {
    RepositoryNode node = event.getNode();
    filerRegistry.getScopeLoaders().forEach(loader -> loader.init(event));
    if (!checkOnly) {
      filerRegistry.getScopeLoaders().forEach(loader -> loader.update(event));
      // Put original node to be able to compute updates required by filer action
      // Do this before property inheritance, as it could update the node
      FilerNodeUtils.setOriginalNode(node, new RepositoryNode(node));
      // Initialize inherited aspects and associated properties from parent
      if (!(event instanceof UpdateFilerEvent)) {
        // Do not do this on update event, this would override updated inheritance properties/aspects
        propertyInheritanceService.computeAspectsAndProperties(node.getParent(), node);
      }
    }
    return putEventAction(event, checkOnly);
  }

  private boolean putEventAction(final FilerEvent event, final boolean checkOnly) {
    boolean hasAction = false;
    if (checkOnly || isUpdateEvent(event)) {
      for (FilerAction filer : filerRegistry.getActions()) {
        hasAction = filer.supportsActionResolution(event) && (checkOnly || filer.supportsActionExecution(event.getNode()));
        if (hasAction && !checkOnly) {
          event.setAction(filer);
        }
        if (hasAction) {
          break;
        }
      }
    }
    return hasAction;
  }

  private static boolean isUpdateEvent(final FilerEvent event) {
    boolean result = true;
    if (event instanceof UpdateFilerEvent) {
      RepositoryNode initialNode = FilerTransactionUtils.getInitialNode(event.getNode().getNodeRef());
      RepositoryNodeDifference difference = new RepositoryNodeDifference(initialNode, event.getNode());
      long updatedPropertiesCount = Stream.of(difference.getPropertiesToAdd().keySet(), difference.getPropertiesToRemove())
          .flatMap(set -> set.stream())
          .filter(property -> !IGNORED_PROPERTIES.contains(property))
          .count();
      long updatedAspectCount = Stream.of(difference.getAspectsToAdd(), difference.getAspectsToRemove())
          .flatMap(set -> set.stream())
          .filter(property -> !IGNORED_ASPECTS.contains(property))
          .count();
      if (updatedPropertiesCount + updatedAspectCount == 0) {
        result = false;
        LOGGER.debug("Ignoring update event without any updated property nor aspect: " + event);
      }
    }
    return result;
  }

  private boolean isLocked(final NodeRef nodeRef) {
    boolean result = false;
    try {
      lockService.checkForLock(nodeRef);
    } catch (NodeLockedException e) {
      result = true;
    }
    return result;
  }

  @Override
  public FilerOperationService operations() {
    return filerOperationService;
  }

  @Override
  public PropertyInheritanceService propertyInheritance() {
    return propertyInheritanceService;
  }

  public void setFilerRegistry(final FilerRegistry filerRegistry) {
    this.filerRegistry = filerRegistry;
  }

  public void setFilerOperationService(final FilerOperationService filerOperationService) {
    this.filerOperationService = filerOperationService;
  }

  public void setPropertyInheritanceService(final PropertyInheritanceService propertyInheritanceService) {
    this.propertyInheritanceService = propertyInheritanceService;
  }

  public void setNodeService(final NodeService nodeService) {
    this.nodeService = nodeService;
  }

  public void setPermissionService(final PermissionService permissionService) {
    this.permissionService = permissionService;
  }

  public void setLockService(final LockService lockService) {
    this.lockService = lockService;
  }
}
