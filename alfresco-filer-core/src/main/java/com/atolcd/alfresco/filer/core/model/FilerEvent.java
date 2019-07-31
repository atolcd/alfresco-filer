package com.atolcd.alfresco.filer.core.model;

import java.util.Optional;

public interface FilerEvent {

  boolean isExecuted();

  void setExecuted();

  RepositoryNode getNode();

  Optional<FilerAction> getAction();

  void setAction(FilerAction action);

  void comesAfter(FilerEvent previous);
}
