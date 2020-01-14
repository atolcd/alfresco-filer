package com.atolcd.alfresco.filer.core.test.domain;

import static com.atolcd.alfresco.filer.core.test.framework.LibraryExtension.getLibrary;
import static com.atolcd.alfresco.filer.core.util.FilerNodeUtils.getPath;
import static java.util.UUID.randomUUID;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Collections;
import java.util.Map;

import org.alfresco.repo.site.SiteModel;
import org.alfresco.service.namespace.QName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.atolcd.alfresco.filer.core.model.RepositoryNode;
import com.atolcd.alfresco.filer.core.test.domain.content.model.FilerTestConstants;
import com.atolcd.alfresco.filer.core.test.domain.util.NodePathUtils;
import com.atolcd.alfresco.filer.core.test.framework.RepositoryOperations;
import com.atolcd.alfresco.filer.core.test.framework.TestApplicationContext;
import com.atolcd.alfresco.filer.core.test.framework.TestAuthentication;
import com.atolcd.alfresco.filer.core.test.framework.TestLibrary;
import com.atolcd.alfresco.filer.core.test.framework.TestLibraryRole;

public class DepartmentFolderFilerActionTest extends RepositoryOperations {

  @Test
  public void withDepartmentName() {
    String departmentName = randomUUID().toString();

    RepositoryNode node = getLibrary().childNode()
        .type(FilerTestConstants.Department.FolderType.NAME)
        .property(FilerTestConstants.Department.Aspect.PROP_NAME, departmentName)
        .build();

    createNode(node);

    assertThat(node.getProperty(FilerTestConstants.Department.Aspect.PROP_NAME, String.class)).isEqualTo(departmentName);
    assertThat(getPath(node)).isEqualTo(getLibrary().getPath());
  }

  @Nested
  // Spring does not find the configuration of nested class from the enclosing class
  // See https://github.com/spring-projects/spring-framework/issues/19930
  @TestApplicationContext
  @TestLibrary
  @TestAuthentication
  @TestLibraryRole(SiteModel.SITE_COLLABORATOR)
  public class UpdateDepartmentName {

    @Test
    public void updateDepartmentName() {
      // Create folder node
      RepositoryNode departmentFolderNode = getLibrary().childNode()
          .type(FilerTestConstants.Department.FolderType.NAME)
          .property(FilerTestConstants.Department.Aspect.PROP_NAME, randomUUID())
          .build();

      createNode(departmentFolderNode);

      // Create document node in folder
      LocalDateTime date = LocalDateTime.of(2004, 8, 12, 0, 0, 0);
      RepositoryNode documentNode = getLibrary().childNode()
          .type(FilerTestConstants.Department.DocumentType.NAME)
          .property(FilerTestConstants.Department.Aspect.PROP_NAME,
              departmentFolderNode.getProperty(FilerTestConstants.Department.Aspect.PROP_NAME, String.class))
          .property(FilerTestConstants.ImportedAspect.PROP_DATE, date.atZone(ZoneId.systemDefault()))
          .build();

      createNode(documentNode);

      assertThat(getPath(documentNode)).isEqualTo(NodePathUtils
          .nodePath(departmentFolderNode.getProperty(FilerTestConstants.Department.Aspect.PROP_NAME, String.class), date));

      // Update folder's name
      Map<QName, Serializable> departmentNameProperty = Collections.singletonMap(FilerTestConstants.Department.Aspect.PROP_NAME,
          randomUUID().toString());

      updateNode(departmentFolderNode, departmentNameProperty);

      // Get document node and check if it has been updated automatically
      fetchNode(documentNode);

      assertThat(getPath(documentNode)).isEqualTo(NodePathUtils
          .nodePath(departmentFolderNode.getProperty(FilerTestConstants.Department.Aspect.PROP_NAME, String.class), date));
    }
  }
}
