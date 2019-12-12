package com.atolcd.alfresco.filer.core.service;

import java.util.function.Consumer;

import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.namespace.QName;

import com.atolcd.alfresco.filer.core.model.FilerAction;
import com.atolcd.alfresco.filer.core.model.RepositoryNode;

public interface FilerOperationService {

  void execute(FilerAction action, RepositoryNode node);

  void setFileable(NodeRef nodeRef);

  void setSegment(NodeRef nodeRef);

  void setSubscriber(NodeRef nodeRef);

  NodeRef getFolder(NodeRef parent, String name, Consumer<NodeRef> onGet);

  NodeRef getOrCreateFolder(NodeRef parent, QName type, String name, Consumer<NodeRef> onGet, Consumer<NodeRef> onCreate);

  void updateFileable(RepositoryNode node, NodeRef destination, String newName);

  void updateFolder(RepositoryNode node, Consumer<NodeRef> onGet, Consumer<NodeRef> onCreate);

  void deleteSegment(NodeRef nodeRef);
}
