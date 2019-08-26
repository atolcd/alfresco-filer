package com.atolcd.alfresco.filer.core.test.domain.action;

import com.atolcd.alfresco.filer.core.model.impl.AbstractFilerAction;
import com.atolcd.alfresco.filer.core.test.domain.service.FilerTestActionService;

public abstract class AbstractFilerTestAction extends AbstractFilerAction {

  private FilerTestActionService actionService;

  protected FilerTestActionService actions() {
    return actionService;
  }

  public void setActionService(final FilerTestActionService actionService) {
    this.actionService = actionService;
  }
}
