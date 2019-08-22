package com.atolcd.alfresco.filer.core.test.domain;

import static java.util.UUID.randomUUID;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Collections;
import java.util.Map;

import org.alfresco.service.namespace.QName;
import org.junit.jupiter.api.Test;

import com.atolcd.alfresco.filer.core.model.RepositoryNode;
import com.atolcd.alfresco.filer.core.test.domain.content.model.FilerTestConstants;
import com.atolcd.alfresco.filer.core.test.util.SiteBasedTest;

public class DepartmentFolderFilerActionTest extends SiteBasedTest {

  @Test
  public void withDepartmentName() {
    String departmentName = randomUUID().toString();

    RepositoryNode node = buildNode()
        .type(FilerTestConstants.Department.FolderType.NAME)
        .property(FilerTestConstants.Department.Aspect.PROP_NAME, departmentName)
        .build();

    createNode(node);

    assertThat(node.getProperty(FilerTestConstants.Department.Aspect.PROP_NAME, String.class)).isEqualTo(departmentName);
    assertThat(getPath(node)).isEqualTo(buildSitePath());
  }

  @Test
  public void updateDepartmentName() {
    // Create folder node
    RepositoryNode departmentFolderNode = buildNode()
        .type(FilerTestConstants.Department.FolderType.NAME)
        .property(FilerTestConstants.Department.Aspect.PROP_NAME, randomUUID())
        .build();

    createNode(departmentFolderNode);

    // Create document node in folder
    LocalDateTime date = LocalDateTime.of(2004, 8, 12, 0, 0, 0);
    RepositoryNode documentNode = buildNode()
        .type(FilerTestConstants.Department.DocumentType.NAME)
        .property(FilerTestConstants.Department.Aspect.PROP_NAME,
            departmentFolderNode.getProperty(FilerTestConstants.Department.Aspect.PROP_NAME, String.class))
        .property(FilerTestConstants.ImportedAspect.PROP_DATE, date.atZone(ZoneId.systemDefault()))
        .build();

    createNode(documentNode);

    assertThat(getPath(documentNode)).isEqualTo(
        buildNodePath(departmentFolderNode.getProperty(FilerTestConstants.Department.Aspect.PROP_NAME, String.class), date));

    // Update folder's name
    Map<QName, Serializable> departmentNameProperty = Collections.singletonMap(FilerTestConstants.Department.Aspect.PROP_NAME,
        randomUUID().toString());

    updateNode(departmentFolderNode, departmentNameProperty);

    // Get document node and check if it has been updated automatically
    fetchNode(documentNode);

    assertThat(getPath(documentNode)).isEqualTo(
        buildNodePath(departmentFolderNode.getProperty(FilerTestConstants.Department.Aspect.PROP_NAME, String.class), date));
  }
}
