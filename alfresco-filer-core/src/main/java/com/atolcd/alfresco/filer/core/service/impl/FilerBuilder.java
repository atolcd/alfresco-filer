package com.atolcd.alfresco.filer.core.service.impl;

import java.util.function.Function;
import java.util.function.Supplier;

import org.alfresco.service.cmr.repository.NodeRef;

import com.atolcd.alfresco.filer.core.model.FilerFolderContext;
import com.atolcd.alfresco.filer.core.model.RepositoryNode;
import com.atolcd.alfresco.filer.core.service.FilerService;

public class FilerBuilder {

  private final FilerService filerService;
  private final RepositoryNode node;

  public FilerBuilder(final FilerService filerService, final RepositoryNode node) {
    this.filerService = filerService;
    this.node = node;
  }

  public FilerFolderBuilder root(final NodeRef nodeRef) {
    return new FilerFolderBuilder(filerService, new FilerFolderContext(node), nodeRef);
  }

  public FilerFolderBuilder root(final Function<RepositoryNode, NodeRef> nodeRefExtractor) {
    return root(nodeRefExtractor.apply(node));
  }

  public FilerFolderBuilder root(final Supplier<NodeRef> nodeRefSupplier) {
    return root(nodeRefSupplier.get());
  }

  public FilerFolderTypeBuilder with(final Function<FilerBuilder, FilerFolderTypeBuilder> builder) {
    return builder.apply(this);
  }

  public RepositoryNode getNode() {
    return node;
  }
}
