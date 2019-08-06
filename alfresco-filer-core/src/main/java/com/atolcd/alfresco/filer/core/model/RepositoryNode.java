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

public class RepositoryNode implements Serializable {

  private static final long serialVersionUID = 6758895936238032221L;

  private NodeRef nodeRef;

  private NodeRef parent;
  private QName type;
  private Set<QName> aspects;
  private Map<QName, Serializable> properties;

  private Map<String, Object> extensions;

  public RepositoryNode() {
    // In case nodeRef is unknown
  }

  public RepositoryNode(final NodeRef nodeRef) {
    this();
    this.nodeRef = nodeRef;
  }

  public RepositoryNode(final NodeRef nodeRef, final NodeRef parent, final QName type, final Set<QName> aspects,
      final Map<QName, Serializable> properties, final Map<String, Object> extensions) {
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

  public NodeRef getNodeRef() {
    return nodeRef;
  }

  public void setNodeRef(final NodeRef nodeRef) {
    this.nodeRef = nodeRef;
  }

  public NodeRef getParent() {
    return parent;
  }

  public void setParent(final NodeRef parent) {
    this.parent = parent;
  }

  public QName getType() {
    return type;
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

  @SuppressWarnings("unchecked")
  public <T> T getProperty(final QName name, final Class<T> clazz) {
    return (T) getProperties().get(name);
  }

  public Optional<String> getName() {
    return Optional.ofNullable(properties).map(p -> p.get(ContentModel.PROP_NAME)).filter(Objects::nonNull).map(String::valueOf);
  }

  public Map<String, Object> getExtensions() {
    extensions = Optional.ofNullable(extensions).orElseGet(LinkedHashMap::new);
    return extensions;
  }

  @SuppressWarnings("unchecked")
  public <T> T getExtension(final String name, final Class<T> clazz) {
    return (T) getExtensions().get(name);
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
      return Objects.equals(nodeRef, other.getNodeRef());
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
