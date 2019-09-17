package com.atolcd.alfresco.filer.core.test.framework;

import static java.util.UUID.randomUUID;

import java.nio.file.Path;
import java.util.Optional;
import java.util.function.Function;

import org.alfresco.service.cmr.repository.NodeRef;

import com.atolcd.alfresco.filer.core.model.RepositoryNode;
import com.atolcd.alfresco.filer.core.model.impl.RepositoryNodeBuilder;

public class DocumentLibrary {

  private final String siteName;
  private NodeRef nodeRef;
  private Path path;
  private final Function<NodeRef, Path> pathProvider;

  public DocumentLibrary(final String siteName, final Function<NodeRef, Path> pathProvider) {
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

  public Path getPath() {
    path = Optional.ofNullable(path).orElseGet(() -> pathProvider.apply(nodeRef));
    return path;
  }

  public Path childPath(final String... children) {
    Path childPath = getPath();
    for (String child : children) {
      childPath = childPath.resolve(child);
    }
    return childPath;
  }
}
