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

import edu.umd.cs.findbugs.annotations.CheckForNull;

public class RepositoryNodeDifference {

  @CheckForNull
  private NodeRef parentToMove;
  @CheckForNull
  private QName typeToSet;
  private final Set<QName> aspectsToRemove;
  private final Set<QName> aspectsToAdd;
  private final Set<QName> propertiesToRemove;
  private final Map<QName, Serializable> propertiesToAdd;

  public RepositoryNodeDifference(final RepositoryNode source, final RepositoryNode target) {
    // Parent
    if (!source.getParent().equals(target.getParent())) {
      parentToMove = target.getParent().get();
    }
    // Type
    if (!source.getType().equals(target.getType())) {
      typeToSet = target.getType().get();
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
    return MessageFormat.format("{0} parent, {1} type, adding {2} {3}, removing {4} {5}",
        getParentToMove().map(String::valueOf).orElse("same"),
        getTypeToSet().map(String::valueOf).orElse("same"),
        propertiesToAdd, aspectsToAdd, propertiesToRemove, aspectsToRemove);
  }

  public boolean isEmpty() {
    return parentToMove == null && typeToSet == null
        && aspectsToRemove.isEmpty() && aspectsToAdd.isEmpty() && propertiesToRemove.isEmpty() && propertiesToAdd.isEmpty();
  }

  public Optional<NodeRef> getParentToMove() {
    return Optional.ofNullable(parentToMove);
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
}
