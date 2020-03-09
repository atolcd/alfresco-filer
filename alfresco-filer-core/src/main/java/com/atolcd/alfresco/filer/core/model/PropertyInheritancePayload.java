package com.atolcd.alfresco.filer.core.model;

import java.io.Serializable;
import java.text.MessageFormat;
import java.util.Map;
import java.util.Set;

import org.alfresco.service.namespace.QName;

public class PropertyInheritancePayload {

  private final Map<QName, Map<QName, Serializable>> added;
  private final Map<QName, Set<QName>> removed;

  public PropertyInheritancePayload(final Map<QName, Map<QName, Serializable>> added, final Map<QName, Set<QName>> removed) {
    this.added = added;
    this.removed = removed;
  }

  @Override
  public String toString() {
    return MessageFormat.format("'{'added={0}, removed={1}'}'", added, removed);
  }

  public boolean isEmpty() {
    return added.isEmpty() && removed.isEmpty();
  }

  public Map<QName, Map<QName, Serializable>> getAdded() {
    return added;
  }

  public Map<QName, Set<QName>> getRemoved() {
    return removed;
  }
}
