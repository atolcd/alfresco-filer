package com.atolcd.alfresco.filer.core.scope.impl;

import org.alfresco.service.cmr.repository.NodeService;

import com.atolcd.alfresco.filer.core.model.FilerEvent;
import com.atolcd.alfresco.filer.core.model.RepositoryNode;

public class DefaultFilerScopeLoader extends AbstractFilerScopeLoader {

  private NodeService nodeService;

  @Override
  public void init(final FilerEvent event) {
    RepositoryNode node = event.getNode();
    // Init node type
    node.setType(nodeService.getType(node.getNodeRef()));
    // Init parent to get inherited aspects from it, but it could already be set on inbound event
    if (node.getParent() == null) {
      node.setParent(nodeService.getPrimaryParent(node.getNodeRef()).getParentRef());
    }
  }

  public void setNodeService(final NodeService nodeService) {
    this.nodeService = nodeService;
  }
}
