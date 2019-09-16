package com.atolcd.alfresco.filer.core.test.framework;

import static java.util.UUID.randomUUID;

import java.nio.file.Paths;
import java.util.Optional;
import java.util.function.Function;

import org.alfresco.service.cmr.repository.NodeRef;

import com.atolcd.alfresco.filer.core.model.RepositoryNode;
import com.atolcd.alfresco.filer.core.model.impl.RepositoryNodeBuilder;

public class DocumentLibrary {

  private final String siteName;
  private NodeRef nodeRef;
  private String path;
  private final Function<NodeRef, String> pathProvider;

  public DocumentLibrary(final String siteName, final Function<NodeRef, String> pathProvider) {
    this.siteName = siteName;
    this.pathProvider = pathProvider;
  }

  public RepositoryNodeBuilder childNode() {
    return RepositoryNode.builder()
        .parent(nodeRef)
        .named(randomUUID());
  }

  public String getSiteName() {
    return siteName;
  }

  public NodeRef getNodeRef() {
    return nodeRef;
  }

  public void setNodeRef(final NodeRef nodeRef) {
    this.nodeRef = nodeRef;
  }

  public String getPath() {
    path = Optional.ofNullable(path).orElseGet(() -> pathProvider.apply(nodeRef));
    return path;
  }

  public String childPath(final String... children) {
    return Paths.get(getPath(), children).toString();
  }
}
