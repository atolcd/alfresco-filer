package com.atolcd.alfresco.filer.core.test.domain.action;

import java.util.Arrays;

import com.atolcd.alfresco.filer.core.model.FilerEvent;
import com.atolcd.alfresco.filer.core.model.RepositoryNode;
import com.atolcd.alfresco.filer.core.service.impl.FilerBuilder;
import com.atolcd.alfresco.filer.core.test.domain.content.model.FilerTestConstants;

public class DepartmentContentFilerAction extends AbstractFilerTestAction {

  @Override
  public boolean supportsActionResolution(final FilerEvent event) {
    return event.getNode().getAspects().contains(FilerTestConstants.Department.Aspect.NAME)
        && Arrays.asList(FilerTestConstants.Department.DocumentType.NAME, FilerTestConstants.SpecialDocumentType.NAME)
            .contains(event.getNode().getType());
  }

  @Override
  public boolean supportsActionExecution(final RepositoryNode node) {
    return true;
  }

  @Override
  protected void execute(final FilerBuilder builder) {
    builder.with(actions()::departmentFolder).getOrCreate()
        .tree(actions()::dateSegmentation).updateAndMove();
  }
}
