package com.atolcd.alfresco.filer.core.test.domain;

import static com.atolcd.alfresco.filer.core.test.domain.util.NodePathUtils.nodePath;
import static com.atolcd.alfresco.filer.core.test.framework.DocumentLibraryExtension.getDocumentLibrary;
import static java.util.UUID.randomUUID;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Collections;
import java.util.Date;
import java.util.Map;

import org.alfresco.model.ContentModel;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.namespace.QName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.atolcd.alfresco.filer.core.model.RepositoryNode;
import com.atolcd.alfresco.filer.core.service.FilerModelService;
import com.atolcd.alfresco.filer.core.test.domain.content.model.FilerTestConstants;
import com.atolcd.alfresco.filer.core.test.framework.RepositoryOperations;
import com.atolcd.alfresco.filer.core.test.framework.TestDocumentLibrary;

public class DepartmentContentFilerActionTest extends RepositoryOperations {

  @Autowired
  private FilerModelService filerModelService;

  @Autowired
  private NodeService nodeService;

  @Nested
  // @TestDocumentLibrary is necessary as Spring does not find the configuration of nested class from the enclosing class
  // See https://github.com/spring-projects/spring-framework/issues/19930
  @TestDocumentLibrary
  public class DepartmentDocument {

    @Test
    public void filerAspectHierarchy() {
      RepositoryNode node = getDocumentLibrary().childNode()
          .type(FilerTestConstants.Department.DocumentType.NAME)
          .property(FilerTestConstants.Department.Aspect.PROP_NAME, randomUUID())
          .build();

      createNode(node);

      assertThat(node.getAspects()).contains(filerModelService.getFileableAspect());

      NodeRef parent = node.getParent();
      assertThat(nodeService.getAspects(parent)).contains(filerModelService.getSegmentAspect());

      NodeRef grandParent = nodeService.getPrimaryParent(parent).getParentRef();
      assertThat(nodeService.getAspects(grandParent)).contains(filerModelService.getSegmentAspect());

      NodeRef greatGrandParent = nodeService.getPrimaryParent(grandParent).getParentRef();
      assertThat(nodeService.getAspects(greatGrandParent)).contains(filerModelService.getSubscriberAspect());
    }

    @Test
    public void typeHierarchy() {
      QName type = FilerTestConstants.Department.DocumentType.NAME;

      RepositoryNode node = getDocumentLibrary().childNode()
          .type(FilerTestConstants.Department.DocumentType.NAME)
          .property(FilerTestConstants.Department.Aspect.PROP_NAME, randomUUID())
          .build();

      createNode(node);

      assertThat(node.getType()).isEqualTo(type);
      assertThat(nodeService.getType(node.getParent())).isEqualTo(ContentModel.TYPE_FOLDER);

      NodeRef grandParent = nodeService.getPrimaryParent(node.getParent()).getParentRef();
      assertThat(nodeService.getType(grandParent)).isEqualTo(ContentModel.TYPE_FOLDER);

      NodeRef greatGrandParent = nodeService.getPrimaryParent(grandParent).getParentRef();
      assertThat(nodeService.getType(greatGrandParent)).isEqualTo(FilerTestConstants.Department.FolderType.NAME);
    }

    @Test
    public void withImportDate() {
      String departmentName = randomUUID().toString();
      LocalDateTime date = LocalDateTime.of(2004, 8, 12, 0, 0, 0);

      RepositoryNode node = getDocumentLibrary().childNode()
          .type(FilerTestConstants.Department.DocumentType.NAME)
          .property(FilerTestConstants.Department.Aspect.PROP_NAME, departmentName)
          .property(FilerTestConstants.ImportedAspect.PROP_DATE, date.atZone(ZoneId.systemDefault()))
          .build();

      createNode(node);

      assertThat(getPath(node)).isEqualTo(nodePath(departmentName, date));
    }

    @Test
    public void withoutImportDate() {
      String departmentName = randomUUID().toString();

      RepositoryNode node = getDocumentLibrary().childNode()
          .type(FilerTestConstants.Department.DocumentType.NAME)
          .property(FilerTestConstants.Department.Aspect.PROP_NAME, departmentName)
          .build();

      createNode(node);

      assertThat(getPath(node)).isEqualTo(nodePath(departmentName, LocalDateTime.now()));
    }

    @Test
    public void withImportDateInWrongSegment() {
      String departmentNom = randomUUID().toString();
      LocalDateTime wrongDate = LocalDateTime.of(2004, 8, 12, 0, 0, 0);
      LocalDateTime targetDate = LocalDateTime.of(2002, 4, 6, 0, 0, 0);

      // Create a node with the wrong date to create corresponding folder
      RepositoryNode wrongSegmentNode = getDocumentLibrary().childNode()
          .type(FilerTestConstants.Department.DocumentType.NAME)
          .property(FilerTestConstants.Department.Aspect.PROP_NAME, departmentNom)
          .property(FilerTestConstants.ImportedAspect.PROP_DATE, wrongDate.atZone(ZoneId.systemDefault()))
          .build();

      createNode(wrongSegmentNode);

      // Create node with the target date in the wrong folder
      RepositoryNode node = getDocumentLibrary().childNode()
          .type(FilerTestConstants.Department.DocumentType.NAME)
          .parent(wrongSegmentNode.getParent())
          .property(FilerTestConstants.Department.Aspect.PROP_NAME, departmentNom)
          .property(FilerTestConstants.ImportedAspect.PROP_DATE, targetDate.atZone(ZoneId.systemDefault()))
          .build();

      createNode(node);

      assertThat(getPath(node)).isEqualTo(nodePath(departmentNom, targetDate));
    }

    @Test
    public void updateImportDate() {
      String departmentNom = randomUUID().toString();
      LocalDateTime sourceDate = LocalDateTime.of(2004, 8, 12, 0, 0, 0);
      LocalDateTime targetDate = LocalDateTime.of(2002, 4, 6, 0, 0, 0);

      // Create node that will be updated
      RepositoryNode node = getDocumentLibrary().childNode()
          .type(FilerTestConstants.Department.DocumentType.NAME)
          .property(FilerTestConstants.Department.Aspect.PROP_NAME, departmentNom)
          .property(FilerTestConstants.ImportedAspect.PROP_DATE, sourceDate.atZone(ZoneId.systemDefault()))
          .build();

      createNode(node);

      assertThat(getPath(node)).isEqualTo(nodePath(departmentNom, sourceDate));

      // Get ancestors before updating node as they will change
      NodeRef oldParent = node.getParent();
      NodeRef oldGrandParent = nodeService.getPrimaryParent(node.getParent()).getParentRef();

      // Set updated property
      Map<QName, Serializable> dateProperty = Collections.singletonMap(FilerTestConstants.ImportedAspect.PROP_DATE,
          Date.from(targetDate.atZone(ZoneId.systemDefault()).toInstant()));

      updateNode(node, dateProperty);

      assertThat(nodeService.exists(oldParent)).isFalse();
      assertThat(nodeService.exists(oldGrandParent)).isFalse();
      assertThat(getPath(node)).isEqualTo(nodePath(departmentNom, targetDate));
    }

    @Test
    public void deleteDocument() {
      String departmentNom = randomUUID().toString();
      LocalDateTime date = LocalDateTime.of(2004, 8, 12, 0, 0, 0);

      // Create node that will be deleted
      RepositoryNode node = getDocumentLibrary().childNode()
          .type(FilerTestConstants.Department.DocumentType.NAME)
          .property(FilerTestConstants.Department.Aspect.PROP_NAME, departmentNom)
          .property(FilerTestConstants.ImportedAspect.PROP_DATE, date.atZone(ZoneId.systemDefault()))
          .build();

      createNode(node);

      assertThat(getPath(node)).isEqualTo(nodePath(departmentNom, date));

      // Get ancestors before deleting node
      NodeRef grandParent = nodeService.getPrimaryParent(node.getParent()).getParentRef();
      NodeRef greatGrandParent = nodeService.getPrimaryParent(grandParent).getParentRef();

      deleteNode(node);

      assertThat(nodeService.exists(node.getNodeRef())).isFalse();
      assertThat(nodeService.exists(node.getParent())).isFalse();
      assertThat(nodeService.exists(grandParent)).isFalse();
      assertThat(nodeService.exists(greatGrandParent)).isTrue();
    }

    @Test
    public void multipleDocumentWithSameImportDate() {
      String departmentName = randomUUID().toString();
      LocalDateTime date = LocalDateTime.of(2004, 8, 12, 0, 0, 0);

      RepositoryNode firstNode = getDocumentLibrary().childNode()
          .type(FilerTestConstants.Department.DocumentType.NAME)
          .property(FilerTestConstants.Department.Aspect.PROP_NAME, departmentName)
          .property(FilerTestConstants.ImportedAspect.PROP_DATE, date.atZone(ZoneId.systemDefault()))
          .build();

      RepositoryNode secondNode = getDocumentLibrary().childNode()
          .type(FilerTestConstants.Department.DocumentType.NAME)
          .property(FilerTestConstants.Department.Aspect.PROP_NAME, departmentName)
          .property(FilerTestConstants.ImportedAspect.PROP_DATE, date.atZone(ZoneId.systemDefault()))
          .build();

      createNode(firstNode);
      createNode(secondNode);

      String nodePath = nodePath(departmentName, date);

      assertThat(getPath(firstNode)).isEqualTo(nodePath);
      assertThat(getPath(secondNode)).isEqualTo(nodePath);
    }
  }

  @Test
  public void specialDocumentTypeWithoutImportDate() {
    QName type = FilerTestConstants.SpecialDocumentType.NAME;
    String departmentName = randomUUID().toString();

    RepositoryNode node = getDocumentLibrary().childNode()
        .type(type)
        .property(FilerTestConstants.Department.Aspect.PROP_NAME, departmentName)
        .build();

    createNode(node);

    assertThat(node.getType()).isEqualTo(type);
    assertThat(getPath(node)).isEqualTo(nodePath(departmentName, LocalDateTime.now()));
  }
}
