package com.atolcd.alfresco.filer.core.model.impl;

import java.io.Serializable;
import java.time.ZonedDateTime;
import java.util.Date;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.BiConsumer;

import org.alfresco.model.ContentModel;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.namespace.QName;

import com.atolcd.alfresco.filer.core.model.RepositoryNode;

public class RepositoryNodeBuilder {

  private final RepositoryNode node = new RepositoryNode();

  public RepositoryNodeBuilder nodeRef(final NodeRef nodeRef) {
    node.setNodeRef(nodeRef);
    return this;
  }

  public RepositoryNodeBuilder parent(final NodeRef parent) {
    node.setParent(parent);
    return this;
  }

  public RepositoryNodeBuilder type(final QName type) {
    node.setType(type);
    return this;
  }

  public RepositoryNodeBuilder named(final String name) {
    return property(ContentModel.PROP_NAME, name);
  }

  public RepositoryNodeBuilder named(final UUID name) {
    return named(name.toString());
  }

  public RepositoryNodeBuilder aspect(final QName value) {
    node.getAspects().add(value);
    return this;
  }

  public RepositoryNodeBuilder aspects(final Set<QName> aspects) {
    node.getAspects().addAll(aspects);
    return this;
  }

  public RepositoryNodeBuilder property(final QName name, final Serializable value) {
    node.getProperties().put(name, value);
    return this;
  }

  public RepositoryNodeBuilder property(final QName name, final UUID value) {
    return property(name, value.toString());
  }

  public RepositoryNodeBuilder property(final QName name, final ZonedDateTime value) {
    return property(name, Date.from(value.toInstant()));
  }

  public RepositoryNodeBuilder properties(final Map<QName, Serializable> properties) {
    node.getProperties().putAll(properties);
    return this;
  }

  public RepositoryNodeBuilder extension(final String extensionId, final Object value) {
    node.getExtensions().put(extensionId, value);
    return this;
  }

  public RepositoryNodeBuilder extensions(final Map<String, Object> extensions) {
    node.getExtensions().putAll(extensions);
    return this;
  }

  public <T> RepositoryNodeBuilder with(final BiConsumer<RepositoryNode, T> consumer, final T value) {
    consumer.accept(node, value);
    return this;
  }

  public RepositoryNode build() {
    return node;
  }
}
