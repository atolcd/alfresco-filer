package com.atolcd.alfresco.filer.core.test.domain.action;

import com.atolcd.alfresco.filer.core.model.FilerEvent;
import com.atolcd.alfresco.filer.core.model.RepositoryNode;
import com.atolcd.alfresco.filer.core.service.impl.FilerBuilder;
import com.atolcd.alfresco.filer.core.test.domain.content.model.FilerTestConstants;

public class DepartmentFolderFilerAction extends AbstractFilerTestAction {

  @Override
  public boolean supportsActionResolution(final FilerEvent event) {
    return event.getNode().getType().equals(FilerTestConstants.Department.FolderType.NAME);
  }

  @Override
  public boolean supportsActionExecution(final RepositoryNode node) {
    return true;
  }

  @Override
  protected void execute(final FilerBuilder builder) {
    builder.with(actions()::departmentFolder).updateAndMove();
  }
}
