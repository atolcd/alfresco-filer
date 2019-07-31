package com.atolcd.alfresco.filer.core.scope.impl;

import org.alfresco.service.cmr.repository.NodeService;

import com.atolcd.alfresco.filer.core.model.FilerEvent;
import com.atolcd.alfresco.filer.core.model.RepositoryNode;

public class PropertiesFilerScopeLoader extends AbstractFilerScopeLoader {

  private NodeService nodeService;

  @Override
  public void update(final FilerEvent event) {
    RepositoryNode node = event.getNode();
    // Put properties
    node.getProperties().putAll(nodeService.getProperties(node.getNodeRef()));
  }

  public void setNodeService(final NodeService nodeService) {
    this.nodeService = nodeService;
  }
}
