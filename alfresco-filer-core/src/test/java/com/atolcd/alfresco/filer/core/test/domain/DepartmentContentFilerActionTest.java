package com.atolcd.alfresco.filer.core.test.domain;

import static java.util.UUID.randomUUID;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;

import com.atolcd.alfresco.filer.core.model.RepositoryNode;
import com.atolcd.alfresco.filer.core.test.domain.content.model.FilerTestConstants;
import com.atolcd.alfresco.filer.core.test.util.SiteBasedTest;

public class DepartmentContentFilerActionTest extends SiteBasedTest {

  @Test
  public void departmentDocumentWithoutImportDate() {
    String departmentName = randomUUID().toString();

    RepositoryNode node = buildNode()
        .type(FilerTestConstants.Department.DocumentType.NAME)
        .property(FilerTestConstants.Department.Aspect.PROP_NAME, departmentName)
        .build();

    createNode(node);

    assertThat(getPath(node)).isEqualTo(buildNodePath(departmentName, LocalDateTime.now()));
  }
}
