package com.atolcd.alfresco.filer.core.service;

import org.alfresco.service.cmr.repository.NodeRef;

import com.atolcd.alfresco.filer.core.model.PropertyInheritance;
import com.atolcd.alfresco.filer.core.model.PropertyInheritancePayload;
import com.atolcd.alfresco.filer.core.model.RepositoryNode;
import com.atolcd.alfresco.filer.core.model.RepositoryNodeDifference;

/**
 * Service responsible for dealing with property inheritance (mandatory or optional)
 * that has to be enforced when a node is created or updated
 */
public interface PropertyInheritanceService {

  /**
   * Retrieve the aspects and properties that are inherited from the parent node identified by its {@link NodeRef}
   * and stores them in the resulting {@link RepositoryNode}
   */
  void computeAspectsAndProperties(NodeRef nodeRef, RepositoryNode result);

  /**
   * Update the node identified by its {@link NodeRef} using the {@link PropertyInheritance} definition to decide
   * which aspects and properties should be applied from the {@code payload}
   */
  void setProperties(NodeRef nodeRef, RepositoryNode payload, PropertyInheritance inheritance);

  /**
   * Generate the {@link PropertyInheritancePayload} based on the {@link RepositoryNodeDifference} which is needed to
   * know which aspects and properties are to be added or removed on the children recursively
   */
  PropertyInheritancePayload getPayload(RepositoryNodeDifference difference);

  /**
   * Update recursively a tree view identified by its root {@link NodeRef} using the {@link PropertyInheritancePayload}
   * content to know which aspects and properties to add or remove
   */
  void setInheritance(NodeRef root, PropertyInheritancePayload payload);
}
