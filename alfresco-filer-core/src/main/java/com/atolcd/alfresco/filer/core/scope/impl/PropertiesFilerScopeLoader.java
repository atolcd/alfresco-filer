package com.atolcd.alfresco.filer.core.scope.impl;

import org.alfresco.service.cmr.repository.NodeService;

import com.atolcd.alfresco.filer.core.model.FilerEvent;
import com.atolcd.alfresco.filer.core.model.RepositoryNode;
import com.atolcd.alfresco.filer.core.service.FilerRegistry;

public class PropertiesFilerScopeLoader extends EmptyFilerScopeLoader {

  private final NodeService nodeService;

  public PropertiesFilerScopeLoader(final FilerRegistry filerRegistry, final NodeService nodeService) {
    super(filerRegistry);
    this.nodeService = nodeService;
  }

  @Override
  public void update(final FilerEvent event) {
    RepositoryNode node = event.getNode();
    // Put properties
    node.getProperties().putAll(nodeService.getProperties(node.getNodeRef().get()));
  }
}
