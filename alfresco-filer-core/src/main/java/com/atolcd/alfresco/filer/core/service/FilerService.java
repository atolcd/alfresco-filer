package com.atolcd.alfresco.filer.core.service;

import org.alfresco.service.cmr.repository.NodeRef;

import com.atolcd.alfresco.filer.core.model.FilerEvent;

public interface FilerService {

  /**
   * Gather information on a fileable node that is being updated
   */
  void initFileable(NodeRef nodeRef);

  /**
   * Execute an action on a fileable node
   */
  void executeAction(FilerEvent event);

  /**
   * Decide whether the node associated to this event should be filed and in that case set it fileable
   */
  boolean resolveFileable(FilerEvent event);

  FilerOperationService operations();

  PropertyInheritanceService propertyInheritance();
}
