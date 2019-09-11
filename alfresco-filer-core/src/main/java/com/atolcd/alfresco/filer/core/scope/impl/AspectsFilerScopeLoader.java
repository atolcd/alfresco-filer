package com.atolcd.alfresco.filer.core.scope.impl;

import org.alfresco.service.cmr.repository.NodeService;

import com.atolcd.alfresco.filer.core.model.FilerEvent;
import com.atolcd.alfresco.filer.core.model.RepositoryNode;

public class AspectsFilerScopeLoader extends EmptyFilerScopeLoader {

  private NodeService nodeService;

  @Override
  public void update(final FilerEvent event) {
    RepositoryNode node = event.getNode();
    // Put aspects
    node.getAspects().addAll(nodeService.getAspects(node.getNodeRef()));
  }

  public void setNodeService(final NodeService nodeService) {
    this.nodeService = nodeService;
  }
}
