package com.atolcd.alfresco.filer.core.service.impl;

import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

import org.alfresco.model.ContentModel;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.namespace.QName;

import com.atolcd.alfresco.filer.core.model.FilerFolderContext;
import com.atolcd.alfresco.filer.core.model.RepositoryNode;
import com.atolcd.alfresco.filer.core.service.FilerService;

public class FilerFolderBuilder {

  private final FilerService filerService;

  private final FilerFolderContext context;

  private FilerNameBuilder<FilerFolderBuilder> nodeNameBuilder;

  public FilerFolderBuilder(final FilerService filerService, final FilerFolderContext context, final NodeRef parent) {
    this.filerService = filerService;
    this.context = new FilerFolderContext(context, parent);
  }

  public FilerFolderBuilder condition(final Predicate<RepositoryNode> condition) {
    context.enable(condition.test(context.getNode()));
    nodeNameBuilder = null; // NOPMD - reset value
    return this;
  }

  public FilerFolderBuilder conditionReverse() {
    return condition(x -> !context.isEnabled());
  }

  public FilerFolderBuilder conditionEnd() {
    return condition(x -> true);
  }

  public FilerFolderTypeBuilder with(final Function<FilerFolderBuilder, FilerFolderTypeBuilder> builder) {
    return builder.apply(this);
  }

  public FilerFolderBuilder tree(final Function<FilerFolderBuilder, FilerFolderBuilder> builder) {
    return builder.apply(this);
  }

  public FilerFolderTypeBuilder folder(final QName type) {
    return new FilerFolderTypeBuilder(filerService, context, type);
  }

  public FilerFolderTypeBuilder folder() {
    return folder(ContentModel.TYPE_FOLDER);
  }

  public FilerNameBuilder<FilerFolderBuilder> rename() {
    nodeNameBuilder = Optional.ofNullable(nodeNameBuilder).orElseGet(() -> new FilerNameBuilder<>(this, context));
    return nodeNameBuilder;
  }

  public void updateAndMove() {
    if (context.isEnabled()) {
      String name = Optional.ofNullable(rename().getName()).orElseGet(() -> context.getNode().getName().get());
      filerService.operations().updateFileable(context.getNode(), context.getParent(), name);
    }
  }

  public FilerFolderBuilder contextFrom(final Consumer<FilerFolderContext> withContext) {
    if (context.isEnabled()) {
      withContext.accept(context);
    }
    return this;
  }

  public FilerFolderContext getContext() {
    return context;
  }
}
