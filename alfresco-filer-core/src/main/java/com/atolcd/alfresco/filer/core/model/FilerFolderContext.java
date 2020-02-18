package com.atolcd.alfresco.filer.core.model;

import java.util.Optional;

import org.alfresco.service.cmr.repository.NodeRef;

import edu.umd.cs.findbugs.annotations.CheckForNull;

public final class FilerFolderContext {

  private final RepositoryNode node;
  private final NodeRef parent;

  @CheckForNull
  private PropertyInheritance propertyInheritance;
  private boolean enabled;

  private FilerFolderContext(final RepositoryNode node, final NodeRef parent,
      final PropertyInheritance propertyInheritance, final boolean enabled) {
    this.node = node;
    this.parent = parent;
    this.propertyInheritance = Optional.ofNullable(propertyInheritance).map(PropertyInheritance::new).orElse(null);
    this.enabled = enabled;
  }

  public FilerFolderContext(final RepositoryNode node, final NodeRef parent) {
    this(node, parent, null, true);
  }

  public FilerFolderContext(final FilerFolderContext other, final NodeRef parent) {
    this(other.node, parent, other.propertyInheritance, other.enabled);
  }

  public RepositoryNode getNode() {
    return node;
  }

  public NodeRef getParent() {
    return parent;
  }

  public PropertyInheritance getPropertyInheritance() {
    propertyInheritance = Optional.ofNullable(propertyInheritance).orElseGet(PropertyInheritance::new);
    return propertyInheritance;
  }

  public boolean hasPropertyInheritance() {
    return propertyInheritance != null;
  }

  public void clearPropertyInheritance() {
    propertyInheritance = null; // NOPMD - reset value
  }

  public boolean isEnabled() {
    return enabled;
  }

  public void enable(final boolean enable) {
    this.enabled = enable;
  }
}
