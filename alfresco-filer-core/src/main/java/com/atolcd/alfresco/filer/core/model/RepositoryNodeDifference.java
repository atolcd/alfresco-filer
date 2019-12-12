package com.atolcd.alfresco.filer.core.model;

import java.io.Serializable;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;

import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.namespace.QName;

public class RepositoryNodeDifference {

  private QName typeToSet;
  private final Set<QName> aspectsToRemove;
  private final Set<QName> aspectsToAdd;
  private final Set<QName> propertiesToRemove;
  private final Map<QName, Serializable> propertiesToAdd;
  private NodeRef parentToMove;

  public RepositoryNodeDifference(final RepositoryNode source, final RepositoryNode target) {
    // Type
    if (source.getType() != null && !source.getType().equals(target.getType())) {
      typeToSet = target.getType();
    }
    // Compute aspects
    aspectsToRemove = new HashSet<>(source.getAspects());
    aspectsToRemove.removeAll(target.getAspects());
    aspectsToAdd = new HashSet<>(target.getAspects());
    aspectsToAdd.removeAll(source.getAspects());
    // Compute properties
    propertiesToRemove = new HashSet<>(source.getProperties().keySet());
    propertiesToRemove.removeAll(target.getProperties().keySet());
    propertiesToAdd = new HashMap<>(target.getProperties());
    removeDuplicateProperties(source.getProperties(), propertiesToAdd);
    // Parent
    if (source.getParent() != null && !source.getParent().equals(target.getParent())) {
      parentToMove = target.getParent();
    }
  }

  private static void removeDuplicateProperties(final Map<QName, Serializable> source, final Map<QName, Serializable> target) {
    for (Entry<QName, Serializable> property : source.entrySet()) {
      Serializable sourceValue = property.getValue();
      Serializable targetValue = target.get(property.getKey());
      if (targetValue == null && sourceValue == null || targetValue != null && targetValue.equals(sourceValue)) {
        target.remove(property.getKey());
      }
    }
  }

  @Override
  public String toString() {
    return MessageFormat.format("{0} type, adding {1} {2}, removing {3} {4}, {5} parent",
        Optional.ofNullable(typeToSet).map(String::valueOf).orElse("same"),
        propertiesToAdd, aspectsToAdd,
        propertiesToRemove, aspectsToRemove,
        Optional.ofNullable(parentToMove).map(String::valueOf).orElse("same"));
  }

  public boolean isEmpty() {
    return typeToSet == null && aspectsToRemove.isEmpty() && aspectsToAdd.isEmpty() && propertiesToRemove.isEmpty()
         && propertiesToAdd.isEmpty() && parentToMove == null;
  }

  public Optional<QName> getTypeToSet() {
    return Optional.ofNullable(typeToSet);
  }

  public Set<QName> getAspectsToAdd() {
    return aspectsToAdd;
  }

  public Set<QName> getAspectsToRemove() {
    return aspectsToRemove;
  }

  public Set<QName> getPropertiesToRemove() {
    return propertiesToRemove;
  }

  public Map<QName, Serializable> getPropertiesToAdd() {
    return propertiesToAdd;
  }

  public Optional<NodeRef> getParentToMove() {
    return Optional.ofNullable(parentToMove);
  }
}
