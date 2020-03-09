package com.atolcd.alfresco.filer.core.service.impl;

import java.util.Arrays;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.namespace.QName;

import com.atolcd.alfresco.filer.core.model.FilerFolderContext;
import com.atolcd.alfresco.filer.core.model.RepositoryNode;
import com.atolcd.alfresco.filer.core.service.FilerService;

import edu.umd.cs.findbugs.annotations.CheckForNull;

public class FilerFolderTypeBuilder {

  private final FilerService filerService;
  private final FilerFolderContext context;
  private final QName filerType;

  @CheckForNull
  private FilerNameBuilder<FilerFolderTypeBuilder> nameBuilder;
  @CheckForNull
  private Consumer<NodeRef> onFilerGet;
  @CheckForNull
  private Consumer<NodeRef> onFilerCreate;

  public FilerFolderTypeBuilder(final FilerService filerService, final FilerFolderContext context, final QName filerType) {
    this.filerService = filerService;
    this.context = new FilerFolderContext(context, context.getParent());
    this.filerType = filerType;
  }

  private Consumer<NodeRef> onCreate() {
    return Optional.ofNullable(onFilerCreate).orElse(x -> {});
  }

  public FilerFolderTypeBuilder onCreate(final Consumer<NodeRef> onCreate) {
    onFilerCreate = onCreate().andThen(onCreate);
    return this;
  }

  public FilerFolderTypeBuilder onCreate(final BiConsumer<NodeRef, RepositoryNode> onCreate) {
    return onCreate(nodeRef -> onCreate.accept(nodeRef, context.getNode()));
  }

  public FilerFolderTypeBuilder asSegment() {
    return onCreate(filerService.operations()::setSegment);
  }

  public FilerFolderTypeBuilder asSubscriber() {
    return onCreate(filerService.operations()::setSubscriber);
  }

  public FilerFolderTypeBuilder asFileable() {
    return onCreate(filerService.operations()::setFileable);
  }

  public FilerFolderTypeBuilder mandatoryPropertyInheritance(final QName... aspects) {
    if (context.isEnabled()) {
      context.getPropertyInheritance().getMandatoryAspects().addAll(Arrays.asList(aspects));
    }
    return this;
  }

  public FilerFolderTypeBuilder optionalPropertyInheritance(final QName... aspects) {
    if (context.isEnabled()) {
      context.getPropertyInheritance().getOptionalAspects().addAll(Arrays.asList(aspects));
    }
    return this;
  }

  public FilerFolderTypeBuilder clearPropertyInheritance() {
    if (context.isEnabled()) {
      context.clearPropertyInheritance();
    }
    return this;
  }

  private Consumer<NodeRef> onGet() {
    return Optional.ofNullable(onFilerGet).orElse(x -> {});
  }

  public FilerFolderTypeBuilder onGet(final Consumer<NodeRef> onGet) {
    onFilerGet = onGet().andThen(onGet);
    return this;
  }

  public FilerFolderTypeBuilder onGet(final BiConsumer<NodeRef, RepositoryNode> onGet) {
    return onGet(nodeRef -> onGet.accept(nodeRef, context.getNode()));
  }

  public FilerNameBuilder<FilerFolderTypeBuilder> named() {
    nameBuilder = Optional.ofNullable(nameBuilder).orElseGet(() -> new FilerNameBuilder<>(this, context));
    return nameBuilder;
  }

  public FilerFolderBuilder getOrCreate() {
    NodeRef child = context.getParent();
    if (context.isEnabled()) {
      String name = named().getName().get();
      if (context.hasPropertyInheritance()) {
        // Apply property inheritance on the folder
        onCreate(nodeRef -> filerService.propertyInheritance()
            .setProperties(nodeRef, context.getNode(), context.getPropertyInheritance()));
      }
      child = filerService.operations().getOrCreateFolder(child, filerType, name, onGet(), onCreate());
    }
    return new FilerFolderBuilder(filerService, context, child);
  }

  public FilerFolderBuilder get() {
    NodeRef child = context.getParent();
    if (context.isEnabled()) {
      String name = named().getName().get();
      child = filerService.operations().getFolder(child, name, onGet());
    }
    return new FilerFolderBuilder(filerService, context, child);
  }

  public FilerFolderBuilder get(final boolean createIfAbsent) {
    return createIfAbsent ? getOrCreate() : get();
  }

  public void updateAndMove() {
    if (context.isEnabled()) {
      RepositoryNode node = context.getNode();
      // Update type
      node.setType(filerType);
      String name = named().getName().get();
      // Update node, which will apply property inheritance
      filerService.operations().updateFileable(node, context.getParent(), name);
      // Apply node get/create functions if this is required, property inheritance is already applied
      filerService.operations().updateFolder(node, onGet(), onCreate());
    }
  }
}
