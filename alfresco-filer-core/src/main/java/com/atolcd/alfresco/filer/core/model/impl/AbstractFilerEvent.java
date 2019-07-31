package com.atolcd.alfresco.filer.core.model.impl;

import java.util.Optional;

import org.alfresco.service.cmr.repository.NodeRef;

import com.atolcd.alfresco.filer.core.model.FilerAction;
import com.atolcd.alfresco.filer.core.model.FilerEvent;
import com.atolcd.alfresco.filer.core.model.RepositoryNode;

public abstract class AbstractFilerEvent implements FilerEvent {

  private RepositoryNode node;
  private FilerAction action;
  private boolean executed;

  public AbstractFilerEvent(final NodeRef nodeRef) {
    this.node = new RepositoryNode(nodeRef);
  }

  @Override
  public boolean isExecuted() {
    return executed;
  }

  @Override
  public void setExecuted() {
    executed = true;
  }

  @Override
  public RepositoryNode getNode() {
    return node;
  }

  @Override
  public void comesAfter(final FilerEvent previous) {
    node = previous.getNode();
    action = previous.getAction().orElse(null);
    executed = previous.isExecuted();
  }

  @Override
  public Optional<FilerAction> getAction() {
    return Optional.ofNullable(action);
  }

  @Override
  public void setAction(final FilerAction action) {
    this.action = action;
  }
}
