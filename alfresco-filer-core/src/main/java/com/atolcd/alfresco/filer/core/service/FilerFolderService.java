package com.atolcd.alfresco.filer.core.service;

import java.util.function.Consumer;

import org.alfresco.service.cmr.repository.NodeRef;

import com.atolcd.alfresco.filer.core.model.RepositoryNode;

public interface FilerFolderService {

  void fetchFolder(RepositoryNode node, Consumer<NodeRef> onGet);

  void fetchOrCreateFolder(RepositoryNode node, Consumer<NodeRef> onGet, Consumer<NodeRef> onCreate);

  void updateFolder(RepositoryNode node, Consumer<NodeRef> onGet, Consumer<NodeRef> onCreate);

  void lockFolder(NodeRef nodeRef);
}
