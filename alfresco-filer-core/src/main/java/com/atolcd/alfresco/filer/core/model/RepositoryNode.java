package com.atolcd.alfresco.filer.core.model;

import java.io.Serializable;
import java.text.MessageFormat;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import org.alfresco.model.ContentModel;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.namespace.QName;

import com.atolcd.alfresco.filer.core.model.impl.RepositoryNodeBuilder;

import edu.umd.cs.findbugs.annotations.CheckForNull;

public class RepositoryNode implements Serializable {

  private static final long serialVersionUID = 6758895936238032221L;

  @CheckForNull
  private NodeRef nodeRef;

  @CheckForNull
  private NodeRef parent;
  @CheckForNull
  private QName type;
  @CheckForNull
  private Set<QName> aspects;
  @CheckForNull
  private Map<QName, Serializable> properties;

  @CheckForNull
  private Map<String, Object> extensions;

  public RepositoryNode() {
    // In case nodeRef is unknown
  }

  public RepositoryNode(final @CheckForNull NodeRef nodeRef) {
    this();
    this.nodeRef = nodeRef;
  }

  public RepositoryNode(final @CheckForNull NodeRef nodeRef, final @CheckForNull NodeRef parent,
      final @CheckForNull QName type, @CheckForNull final Set<QName> aspects,
      final @CheckForNull Map<QName, Serializable> properties, final @CheckForNull Map<String, Object> extensions) {
    this(nodeRef);
    this.parent = parent;
    this.type = type;
    this.aspects = Optional.ofNullable(aspects).map(LinkedHashSet::new).orElse(null);
    this.properties = Optional.ofNullable(properties).map(LinkedHashMap::new).orElse(null);
    this.extensions = Optional.ofNullable(extensions).map(LinkedHashMap::new).orElse(null);
  }

  public RepositoryNode(final RepositoryNode other) {
    this(other.nodeRef, other.parent, other.type, other.aspects, other.properties, other.extensions);
  }

  public static RepositoryNodeBuilder builder() {
    return new RepositoryNodeBuilder();
  }

  public Optional<NodeRef> getNodeRef() {
    return Optional.ofNullable(nodeRef);
  }

  public void setNodeRef(final NodeRef nodeRef) {
    this.nodeRef = nodeRef;
  }

  public Optional<NodeRef> getParent() {
    return Optional.ofNullable(parent);
  }

  public void setParent(final NodeRef parent) {
    this.parent = parent;
  }

  public Optional<QName> getType() {
    return Optional.ofNullable(type);
  }

  public void setType(final QName type) {
    this.type = type;
  }

  public Set<QName> getAspects() {
    aspects = Optional.ofNullable(aspects).orElseGet(LinkedHashSet::new);
    return aspects;
  }

  public Map<QName, Serializable> getProperties() {
    properties = Optional.ofNullable(properties).orElseGet(LinkedHashMap::new);
    return properties;
  }

  public <T> Optional<T> getProperty(final QName name, final Class<T> propertyType) {
    return Optional.ofNullable(properties).map(p -> p.get(name)).map(propertyType::cast);
  }

  public Optional<String> getName() {
    return getProperty(ContentModel.PROP_NAME, String.class);
  }

  public Map<String, Object> getExtensions() {
    extensions = Optional.ofNullable(extensions).orElseGet(LinkedHashMap::new);
    return extensions;
  }

  public <T> Optional<T> getExtension(final String name, final Class<T> extensionType) {
    return Optional.ofNullable(extensions).map(p -> p.get(name)).map(extensionType::cast);
  }

  @Override
  public boolean equals(final Object object) {
    if (object == null) {
      return false;
    }
    if (object == this) {
      return true;
    }
    if (object instanceof RepositoryNode) {
      RepositoryNode other = (RepositoryNode) object;
      return nodeRef != null && nodeRef.equals(other.getNodeRef().orElse(null));
    }
    return false;
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(nodeRef);
  }

  @Override
  public String toString() {
    return MessageFormat.format("{0}'{'type={1}, properties={2}, aspects={3}, parent={4}, nodeRef={5}'}'",
        getName().orElse(null), type, properties, aspects, parent, nodeRef);
  }
}
