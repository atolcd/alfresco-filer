package com.atolcd.alfresco.filer.core.service;

import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.namespace.QName;

public interface FilerModelService {

  QName getFileableAspect();

  QName getSegmentAspect();

  QName getSubscriberAspect();

  QName getPropertyInheritanceAspect();

  String getOwnerUsername();

  void setOwner(NodeRef nodeRef);

  void runWithoutFileableBehaviour(Runnable callback);

  void runWithoutFileableBehaviour(NodeRef nodeRef, Runnable callback);

  void runWithoutSubscriberBehaviour(NodeRef nodeRef, Runnable callback);

  void runWithoutBehaviours(NodeRef nodeRef, Runnable callback, QName... behaviours);
}
