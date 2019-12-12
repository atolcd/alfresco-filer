package com.atolcd.alfresco.filer.core.model;

import org.springframework.core.Ordered;

public interface FilerAction extends Ordered, Comparable<FilerAction> {

  String getName();

  boolean supportsActionResolution(FilerEvent event);

  boolean supportsActionExecution(RepositoryNode node);

  void execute(RepositoryNode node);
}
