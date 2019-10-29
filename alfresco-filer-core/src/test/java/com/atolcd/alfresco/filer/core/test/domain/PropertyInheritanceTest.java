package com.atolcd.alfresco.filer.core.test.domain;

import static com.atolcd.alfresco.filer.core.test.framework.LibraryExtension.getLibrary;
import static java.util.UUID.randomUUID;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.Serializable;
import java.util.Collections;
import java.util.Map;

import org.alfresco.model.ContentModel;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.namespace.QName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.atolcd.alfresco.filer.core.model.RepositoryNode;
import com.atolcd.alfresco.filer.core.test.domain.content.model.FilerTestConstants;
import com.atolcd.alfresco.filer.core.test.framework.RepositoryOperations;

public class PropertyInheritanceTest extends RepositoryOperations {

  @Autowired
  private NodeService nodeService;

  @Test
  public void createNodeInDepartmentFolder() {
    RepositoryNode folderNode = getLibrary().childNode()
        .type(FilerTestConstants.Department.FolderType.NAME)
        .property(FilerTestConstants.Department.Aspect.PROP_NAME, randomUUID())
        .property(FilerTestConstants.Department.Aspect.PROP_ID, randomUUID())
        .build();

    createNode(folderNode);

    // Create node in department folder and check if properties have been inheritated from department folder
    RepositoryNode testNode = getLibrary().childNode()
        .type(FilerTestConstants.Department.DocumentType.NAME)
        .parent(folderNode.getNodeRef())
        .build();

    createNode(testNode);

    assertThat(testNode.getProperty(FilerTestConstants.Department.Aspect.PROP_NAME, String.class))
        .isEqualTo(folderNode.getProperty(FilerTestConstants.Department.Aspect.PROP_NAME, String.class));
    assertThat(testNode.getProperty(FilerTestConstants.Department.Aspect.PROP_ID, String.class))
        .isEqualTo(folderNode.getProperty(FilerTestConstants.Department.Aspect.PROP_ID, String.class));
  }

  // Test fails. TODO Tested code need correction.
//  @Test
//  public void setWrongValuePropertyOnDocumentNode() {
//    RepositoryNode folderNode = getDocumentLibrary().childNode()
//        .type(FilerTestConstants.Department.FolderType.NAME)
//        .property(FilerTestConstants.Department.Aspect.PROP_NAME, randomUUID())
//        .property(FilerTestConstants.Department.Aspect.PROP_ID, randomUUID())
//        .build();
//
//    createNode(folderNode);
//
//    RepositoryNode testNode = getDocumentLibrary().childNode()
//        .type(FilerTestConstants.Department.DocumentType.NAME)
//        .parent(folderNode.getNodeRef())
//        .build();
//
//    createNode(testNode);
//
//    // Change property of document node and check that inheritance override that changement
//    Map<QName, Serializable> property = Collections.singletonMap(FilerTestConstants.Department.Aspect.PROP_ID,
//        randomUUID().toString());
//    updateNode(testNode, property);
//
//    assertThat(testNode.getProperty(FilerTestConstants.Department.Aspect.PROP_ID, String.class))
//        .isNotEqualTo(property.get(FilerTestConstants.Department.Aspect.PROP_ID));
//    assertThat(testNode.getProperty(FilerTestConstants.Department.Aspect.PROP_ID, String.class))
//        .isEqualTo(folderNode.getProperty(FilerTestConstants.Department.Aspect.PROP_ID, String.class));
//  }

  @Test
  public void changePropertyOfDepartmentFolder() {
    RepositoryNode testNode = getLibrary().childNode()
        .type(FilerTestConstants.Department.DocumentType.NAME)
        .property(FilerTestConstants.Department.Aspect.PROP_NAME, randomUUID())
        .property(FilerTestConstants.Department.Aspect.PROP_ID, randomUUID())
        .build();

    createNode(testNode);

    // Change property of department folder
    RepositoryNode folderNode = new RepositoryNode(getDepartmentFolder(testNode));

    Map<QName, Serializable> property = Collections.singletonMap(FilerTestConstants.Department.Aspect.PROP_ID,
        randomUUID().toString());
    updateNode(folderNode, property);

    assertThat(folderNode.getProperty(FilerTestConstants.Department.Aspect.PROP_ID, String.class))
        .isEqualTo(property.get(FilerTestConstants.Department.Aspect.PROP_ID));

    // Get document node and check if property change have been inherited
    fetchNode(testNode);

    assertThat(folderNode.getProperty(FilerTestConstants.Department.Aspect.PROP_NAME, String.class))
        .isEqualTo(testNode.getProperty(FilerTestConstants.Department.Aspect.PROP_NAME, String.class));
    assertThat(testNode.getProperty(FilerTestConstants.Department.Aspect.PROP_ID, String.class))
        .isEqualTo(folderNode.getProperty(FilerTestConstants.Department.Aspect.PROP_ID, String.class));
  }

  @Test
  public void createNodeInDepartmentFolderWithWrongProperty() {
    RepositoryNode folderNode = getLibrary().childNode()
        .type(FilerTestConstants.Department.FolderType.NAME)
        .property(FilerTestConstants.Department.Aspect.PROP_NAME, randomUUID())
        .property(FilerTestConstants.Department.Aspect.PROP_ID, randomUUID())
        .build();

    createNode(folderNode);

    // Create node in department folder with wrong property and check if correct property is inherited from folder
    RepositoryNode testNode = getLibrary().childNode()
        .type(FilerTestConstants.Department.DocumentType.NAME)
        .parent(folderNode.getNodeRef())
        .property(FilerTestConstants.Department.Aspect.PROP_ID, randomUUID())
        .build();

    createNode(testNode);

    assertThat(testNode.getProperty(FilerTestConstants.Department.Aspect.PROP_NAME, String.class))
        .isEqualTo(folderNode.getProperty(FilerTestConstants.Department.Aspect.PROP_NAME, String.class));
    assertThat(testNode.getProperty(FilerTestConstants.Department.Aspect.PROP_ID, String.class))
        .isEqualTo(folderNode.getProperty(FilerTestConstants.Department.Aspect.PROP_ID, String.class));
  }

  // Test fails. TODO Tested code need correction.
//  @Test
//  public void createNodeInDocumentLibraryWithPropertiesForFiler() {
//    String departmentName = randomUUID().toString();
//
//    RepositoryNode fixtureNode = getDocumentLibrary().childNode()
//        .type(FilerTestConstants.Department.DocumentType.NAME)
//        .property(FilerTestConstants.Department.Aspect.PROP_NAME, departmentName)
//        .property(FilerTestConstants.Department.Aspect.PROP_ID, randomUUID())
//        .build();
//
//    createNode(fixtureNode);
//
//    // Create node in document library with all required properties to be filed
//    // and check if it has been filed correctly with property inheritance applied
//    RepositoryNode testNode = getDocumentLibrary().childNode()
//        .type(FilerTestConstants.Department.DocumentType.NAME)
//        .property(FilerTestConstants.Department.Aspect.PROP_NAME, departmentName)
//        .build();
//
//    createNode(testNode);
//
//    assertThat(getPath(testNode)).isEqualTo(getPath(fixtureNode));
//    assertThat(testNode.getProperty(FilerTestConstants.Department.Aspect.PROP_NAME, String.class))
//        .isEqualTo(fixtureNode.getProperty(FilerTestConstants.Department.Aspect.PROP_NAME, String.class));
//    assertThat(testNode.getProperty(FilerTestConstants.Department.Aspect.PROP_ID, String.class))
//        .isEqualTo(fixtureNode.getProperty(FilerTestConstants.Department.Aspect.PROP_ID, String.class));
//  }

  @Test
  public void createNodeInFolderHierarchyWithMultipleLevelInheritance() {

    RepositoryNode departmentFolderNode = getLibrary().childNode()
        .type(FilerTestConstants.Department.FolderType.NAME)
        .property(FilerTestConstants.Department.Aspect.PROP_NAME, randomUUID())
        .property(FilerTestConstants.Department.Aspect.PROP_ID, randomUUID())
        .build();

    createNode(departmentFolderNode);

    // Create management folder in department folder and check if department properties are inherited
    RepositoryNode managementFolderNode = getLibrary().childNode()
        .type(FilerTestConstants.Department.Management.DocumentType.NAME)
        .parent(departmentFolderNode.getNodeRef())
        .property(FilerTestConstants.Department.Management.Aspect.PROP_ID, randomUUID())
        .build();

    createNode(managementFolderNode);

    assertThat(managementFolderNode.getProperty(FilerTestConstants.Department.Aspect.PROP_NAME, String.class))
        .isEqualTo(departmentFolderNode.getProperty(FilerTestConstants.Department.Aspect.PROP_NAME, String.class));
    assertThat(managementFolderNode.getProperty(FilerTestConstants.Department.Aspect.PROP_ID, String.class))
        .isEqualTo(departmentFolderNode.getProperty(FilerTestConstants.Department.Aspect.PROP_ID, String.class));

    // Create node in management folder and check if department and management properties are inherited
    RepositoryNode testNode = getLibrary().childNode()
        .type(FilerTestConstants.Department.Management.DocumentType.NAME)
        .parent(managementFolderNode.getParent())
        .build();

    createNode(testNode);

    assertThat(testNode.getProperty(FilerTestConstants.Department.Aspect.PROP_NAME, String.class))
        .isEqualTo(departmentFolderNode.getProperty(FilerTestConstants.Department.Aspect.PROP_NAME, String.class));
    assertThat(testNode.getProperty(FilerTestConstants.Department.Aspect.PROP_ID, String.class))
        .isEqualTo(departmentFolderNode.getProperty(FilerTestConstants.Department.Aspect.PROP_ID, String.class));
    assertThat(testNode.getProperty(FilerTestConstants.Department.Management.Aspect.PROP_ID, String.class))
        .isEqualTo(managementFolderNode.getProperty(FilerTestConstants.Department.Management.Aspect.PROP_ID, String.class));
  }

  private NodeRef getDepartmentFolder(final RepositoryNode node) {
    String departmentName = node.getProperty(FilerTestConstants.Department.Aspect.PROP_NAME, String.class);
    return nodeService.getChildByName(getLibrary().getNodeRef(), ContentModel.ASSOC_CONTAINS, departmentName);
  }
}
