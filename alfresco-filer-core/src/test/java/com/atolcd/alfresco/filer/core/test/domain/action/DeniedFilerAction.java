package com.atolcd.alfresco.filer.core.test.domain.action;

import com.atolcd.alfresco.filer.core.model.FilerEvent;
import com.atolcd.alfresco.filer.core.model.RepositoryNode;
import com.atolcd.alfresco.filer.core.service.impl.FilerBuilder;

public class DeniedFilerAction extends AbstractFilerTestAction {

  @Override
  public boolean supportsActionResolution(final FilerEvent event) {
    return true;
  }

  @Override
  public boolean supportsActionExecution(final RepositoryNode node) {
    return true;
  }

  @Override
  public int getOrder() {
    return LOWEST_PRECEDENCE;
  }

  @Override
  protected void execute(final FilerBuilder builder) {
    deny(builder, node -> true);
  }
}
