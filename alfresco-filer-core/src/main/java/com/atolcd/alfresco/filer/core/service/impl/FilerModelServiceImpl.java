package com.atolcd.alfresco.filer.core.service.impl;

import org.alfresco.repo.policy.BehaviourFilter;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.security.OwnableService;
import org.alfresco.service.namespace.QName;

import com.atolcd.alfresco.filer.core.service.FilerModelService;

import edu.umd.cs.findbugs.annotations.Nullable;

public class FilerModelServiceImpl implements FilerModelService {

  private final OwnableService ownableService;
  private final BehaviourFilter behaviourFilter;

  @Nullable
  private QName fileableAspect;
  @Nullable
  private QName segmentAspect;
  @Nullable
  private QName subscriberAspect;
  @Nullable
  private QName propertyInheritanceAspect;
  @Nullable
  private String ownerUsername;

  public FilerModelServiceImpl(final OwnableService ownableService, final BehaviourFilter behaviourFilter) {
    this.ownableService = ownableService;
    this.behaviourFilter = behaviourFilter;
  }

  @Override
  public QName getFileableAspect() {
    return fileableAspect;
  }

  @Override
  public QName getSegmentAspect() {
    return segmentAspect;
  }

  @Override
  public QName getSubscriberAspect() {
    return subscriberAspect;
  }

  @Override
  public QName getPropertyInheritanceAspect() {
    return propertyInheritanceAspect;
  }

  @Override
  public String getOwnerUsername() {
    return ownerUsername;
  }

  @Override
  public void setOwner(final NodeRef nodeRef) {
    ownableService.setOwner(nodeRef, ownerUsername);
  }

  @Override
  public void runWithoutFileableBehaviour(final Runnable callback) {
    behaviourFilter.disableBehaviour(fileableAspect);
    try {
      callback.run();
    } finally {
      behaviourFilter.enableBehaviour(fileableAspect);
    }
  }

  @Override
  public void runWithoutFileableBehaviour(final NodeRef nodeRef, final Runnable callback) {
    runWithoutBehaviours(nodeRef, callback, fileableAspect);
  }

  @Override
  public void runWithoutSubscriberBehaviour(final NodeRef nodeRef, final Runnable callback) {
    runWithoutBehaviours(nodeRef, callback,
        segmentAspect, // Segment extends Subscriber but it is ignored... (see ALF-21992)
        subscriberAspect);
  }

  @Override
  public void runWithoutBehaviours(final NodeRef nodeRef, final Runnable callback, final QName... behaviours) {
    for (QName behaviour : behaviours) {
      behaviourFilter.disableBehaviour(nodeRef, behaviour);
    }
    try {
      callback.run();
    } finally {
      for (QName behaviour : behaviours) {
        behaviourFilter.enableBehaviour(nodeRef, behaviour);
      }
    }
  }

  public void setFileableAspectQName(final String fileableAspectQName) {
    this.fileableAspect = QName.createQName(fileableAspectQName);
  }

  public void setSegmentAspectQName(final String segmentAspectQName) {
    this.segmentAspect = QName.createQName(segmentAspectQName);
  }

  public void setSubscriberAspectQName(final String subscriberAspectQName) {
    this.subscriberAspect = QName.createQName(subscriberAspectQName);
  }

  public void setPropertyInheritanceAspectQName(final String propertyInheritanceAspectQName) {
    this.propertyInheritanceAspect = QName.createQName(propertyInheritanceAspectQName);
  }

  public void setOwnerUsername(final String ownerUsername) {
    this.ownerUsername = ownerUsername;
  }
}
