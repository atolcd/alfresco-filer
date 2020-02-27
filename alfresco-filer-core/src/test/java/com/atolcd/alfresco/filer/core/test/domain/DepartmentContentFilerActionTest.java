package com.atolcd.alfresco.filer.core.test.domain;

import static com.atolcd.alfresco.filer.core.test.framework.LibraryExtension.getLibrary;
import static com.atolcd.alfresco.filer.core.util.FilerNodeUtils.getPath;
import static java.util.UUID.randomUUID;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.Serializable;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Collections;
import java.util.Date;
import java.util.Map;

import org.alfresco.repo.site.SiteModel;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.namespace.QName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.atolcd.alfresco.filer.core.model.RepositoryNode;
import com.atolcd.alfresco.filer.core.test.domain.content.model.FilerTestConstants;
import com.atolcd.alfresco.filer.core.test.domain.util.NodePathUtils;
import com.atolcd.alfresco.filer.core.test.framework.RepositoryNodeHelper;
import com.atolcd.alfresco.filer.core.test.framework.TestApplicationContext;
import com.atolcd.alfresco.filer.core.test.framework.TestAuthentication;
import com.atolcd.alfresco.filer.core.test.framework.TestLibrary;
import com.atolcd.alfresco.filer.core.test.framework.TestLibraryRole;

@TestApplicationContext
@TestLibrary
@TestAuthentication
@TestLibraryRole(SiteModel.SITE_CONTRIBUTOR)
public class DepartmentContentFilerActionTest {

  @Autowired
  private NodeService nodeService;
  @Autowired
  private RepositoryNodeHelper repositoryNodeHelper;

  @Nested
  // The annotations below are necessary as Spring does not find the configuration of nested class from the enclosing class
  // See https://github.com/spring-projects/spring-framework/issues/19930
  @TestApplicationContext
  @TestLibrary
  @TestAuthentication
  @TestLibraryRole(SiteModel.SITE_CONTRIBUTOR)
  public class DepartmentDocumentCreateOp {

    @Test
    public void withImportDate() {
      String departmentName = randomUUID().toString();
      LocalDateTime date = LocalDateTime.of(2004, 8, 12, 0, 0, 0);

      RepositoryNode node = getLibrary().childNode()
          .type(FilerTestConstants.Department.DocumentType.NAME)
          .property(FilerTestConstants.Department.Aspect.PROP_NAME, departmentName)
          .property(FilerTestConstants.ImportedAspect.PROP_DATE, date.atZone(ZoneId.systemDefault()))
          .build();

      repositoryNodeHelper.createNode(node);

      assertThat(getPath(node)).isEqualTo(NodePathUtils.nodePath(departmentName, date));
    }

    @Test
    public void withoutImportDate() {
      String departmentName = randomUUID().toString();

      RepositoryNode node = getLibrary().childNode()
          .type(FilerTestConstants.Department.DocumentType.NAME)
          .property(FilerTestConstants.Department.Aspect.PROP_NAME, departmentName)
          .build();

      repositoryNodeHelper.createNode(node);

      assertThat(getPath(node)).isEqualTo(NodePathUtils.nodePath(departmentName, LocalDateTime.now()));
    }

    @Test
    public void withImportDateInWrongSegment() {
      String departmentNom = randomUUID().toString();
      LocalDateTime wrongDate = LocalDateTime.of(2004, 8, 12, 0, 0, 0);
      LocalDateTime targetDate = LocalDateTime.of(2002, 4, 6, 0, 0, 0);

      // Create a node with the wrong date to create corresponding folder
      RepositoryNode wrongSegmentNode = getLibrary().childNode()
          .type(FilerTestConstants.Department.DocumentType.NAME)
          .property(FilerTestConstants.Department.Aspect.PROP_NAME, departmentNom)
          .property(FilerTestConstants.ImportedAspect.PROP_DATE, wrongDate.atZone(ZoneId.systemDefault()))
          .build();

      repositoryNodeHelper.createNode(wrongSegmentNode);

      // Create node with the target date in the wrong folder
      RepositoryNode node = getLibrary().childNode()
          .type(FilerTestConstants.Department.DocumentType.NAME)
          .parent(wrongSegmentNode.getParent())
          .property(FilerTestConstants.Department.Aspect.PROP_NAME, departmentNom)
          .property(FilerTestConstants.ImportedAspect.PROP_DATE, targetDate.atZone(ZoneId.systemDefault()))
          .build();

      repositoryNodeHelper.createNode(node);

      assertThat(getPath(node)).isEqualTo(NodePathUtils.nodePath(departmentNom, targetDate));
    }

    @Test
    public void multipleDocumentWithSameImportDate() {
      String departmentName = randomUUID().toString();
      LocalDateTime date = LocalDateTime.of(2004, 8, 12, 0, 0, 0);

      RepositoryNode firstNode = getLibrary().childNode()
          .type(FilerTestConstants.Department.DocumentType.NAME)
          .property(FilerTestConstants.Department.Aspect.PROP_NAME, departmentName)
          .property(FilerTestConstants.ImportedAspect.PROP_DATE, date.atZone(ZoneId.systemDefault()))
          .build();

      RepositoryNode secondNode = getLibrary().childNode()
          .type(FilerTestConstants.Department.DocumentType.NAME)
          .property(FilerTestConstants.Department.Aspect.PROP_NAME, departmentName)
          .property(FilerTestConstants.ImportedAspect.PROP_DATE, date.atZone(ZoneId.systemDefault()))
          .build();

      repositoryNodeHelper.createNode(firstNode);
      repositoryNodeHelper.createNode(secondNode);

      Path nodePath = NodePathUtils.nodePath(departmentName, date);

      assertThat(getPath(firstNode)).isEqualTo(nodePath);
      assertThat(getPath(secondNode)).isEqualTo(nodePath);
    }
  }

  @Nested
  // Spring does not find the configuration of nested class from the enclosing class
  // See https://github.com/spring-projects/spring-framework/issues/19930
  @TestApplicationContext
  @TestLibrary
  @TestAuthentication
  @TestLibraryRole(SiteModel.SITE_COLLABORATOR)
  public class DepartmentDocumentUpdateOp {

    @Test
    public void updateImportDate() {
      String departmentNom = randomUUID().toString();
      LocalDateTime sourceDate = LocalDateTime.of(2004, 8, 12, 0, 0, 0);
      LocalDateTime targetDate = LocalDateTime.of(2002, 4, 6, 0, 0, 0);

      // Create node that will be updated
      RepositoryNode node = getLibrary().childNode()
          .type(FilerTestConstants.Department.DocumentType.NAME)
          .property(FilerTestConstants.Department.Aspect.PROP_NAME, departmentNom)
          .property(FilerTestConstants.ImportedAspect.PROP_DATE, sourceDate.atZone(ZoneId.systemDefault()))
          .build();

      repositoryNodeHelper.createNode(node);

      assertThat(getPath(node)).isEqualTo(NodePathUtils.nodePath(departmentNom, sourceDate));

      // Get ancestors before updating node as they will change
      NodeRef oldParent = node.getParent().get();
      NodeRef oldGrandParent = nodeService.getPrimaryParent(oldParent).getParentRef();

      // Set updated property
      Map<QName, Serializable> dateProperty = Collections.singletonMap(FilerTestConstants.ImportedAspect.PROP_DATE,
          Date.from(targetDate.atZone(ZoneId.systemDefault()).toInstant()));

      repositoryNodeHelper.updateNode(node, dateProperty);

      assertThat(nodeService.exists(oldParent)).isFalse();
      assertThat(nodeService.exists(oldGrandParent)).isFalse();
      assertThat(getPath(node)).isEqualTo(NodePathUtils.nodePath(departmentNom, targetDate));
    }
  }

  @Nested
  // Spring does not find the configuration of nested class from the enclosing class
  // See https://github.com/spring-projects/spring-framework/issues/19930
  @TestApplicationContext
  @TestLibrary
  @TestAuthentication
  @TestLibraryRole(SiteModel.SITE_MANAGER)
  public class DepartmentDocumentDeleteOp {

    @Test
    public void deleteDocument() {
      String departmentNom = randomUUID().toString();
      LocalDateTime date = LocalDateTime.of(2004, 8, 12, 0, 0, 0);

      // Create node that will be deleted
      RepositoryNode node = getLibrary().childNode()
          .type(FilerTestConstants.Department.DocumentType.NAME)
          .property(FilerTestConstants.Department.Aspect.PROP_NAME, departmentNom)
          .property(FilerTestConstants.ImportedAspect.PROP_DATE, date.atZone(ZoneId.systemDefault()))
          .build();

      repositoryNodeHelper.createNode(node);

      assertThat(getPath(node)).isEqualTo(NodePathUtils.nodePath(departmentNom, date));

      // Get ancestors before deleting node
      NodeRef grandParent = nodeService.getPrimaryParent(node.getParent().get()).getParentRef();
      NodeRef greatGrandParent = nodeService.getPrimaryParent(grandParent).getParentRef();

      repositoryNodeHelper.deleteNode(node);

      assertThat(nodeService.exists(node.getNodeRef().get())).isFalse();
      assertThat(nodeService.exists(node.getParent().get())).isFalse();
      assertThat(nodeService.exists(grandParent)).isFalse();
      assertThat(nodeService.exists(greatGrandParent)).isTrue();
    }
  }

  @Test
  public void specialDocumentTypeWithoutImportDate() {
    QName type = FilerTestConstants.SpecialDocumentType.NAME;
    String departmentName = randomUUID().toString();

    RepositoryNode node = getLibrary().childNode()
        .type(type)
        .property(FilerTestConstants.Department.Aspect.PROP_NAME, departmentName)
        .build();

    repositoryNodeHelper.createNode(node);

    assertThat(node.getType()).contains(type);
    assertThat(getPath(node)).isEqualTo(NodePathUtils.nodePath(departmentName, LocalDateTime.now()));
  }
}
