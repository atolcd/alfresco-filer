package com.atolcd.alfresco.filer.core.test.domain.action;

import java.util.Arrays;

import com.atolcd.alfresco.filer.core.model.FilerEvent;
import com.atolcd.alfresco.filer.core.model.RepositoryNode;
import com.atolcd.alfresco.filer.core.service.impl.FilerBuilder;
import com.atolcd.alfresco.filer.core.test.domain.content.model.FilerTestConstants;

public class DepartmentManagementContentFilerAction extends AbstractFilerTestAction {

  @Override
  public boolean supportsActionResolution(final FilerEvent event) {
    return event.getNode().getType().get().equals(FilerTestConstants.Department.Management.DocumentType.NAME)
        && event.getNode().getAspects()
            .containsAll(Arrays.asList(
                FilerTestConstants.Department.Aspect.NAME, FilerTestConstants.Department.Management.Aspect.NAME));
  }

  @Override
  public boolean supportsActionExecution(final RepositoryNode node) {
    return true;
  }

  @Override
  protected void execute(final FilerBuilder builder) {
    builder.with(actions()::departmentManagementFolder).getOrCreate()
        .tree(actions()::dateSegmentation).updateAndMove();
  }
}
