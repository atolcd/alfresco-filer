package com.atolcd.alfresco.filer.core.scope.impl;

import org.alfresco.service.cmr.repository.NodeService;

import com.atolcd.alfresco.filer.core.model.FilerEvent;
import com.atolcd.alfresco.filer.core.model.RepositoryNode;

import edu.umd.cs.findbugs.annotations.Nullable;

public class DefaultFilerScopeLoader extends EmptyFilerScopeLoader {

  @Nullable
  private NodeService nodeService;

  @Override
  public void init(final FilerEvent event) {
    RepositoryNode node = event.getNode();
    // Init node type
    node.setType(nodeService.getType(node.getNodeRef().get()));
    // Init parent to get inherited aspects from it, but it could already be set on inbound event
    if (!node.getParent().isPresent()) {
      node.setParent(nodeService.getPrimaryParent(node.getNodeRef().get()).getParentRef());
    }
  }

  public void setNodeService(final NodeService nodeService) {
    this.nodeService = nodeService;
  }
}
