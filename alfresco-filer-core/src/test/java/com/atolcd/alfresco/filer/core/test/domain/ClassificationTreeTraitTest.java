package com.atolcd.alfresco.filer.core.test.domain;

import static com.atolcd.alfresco.filer.core.test.framework.LibraryExtension.getLibrary;
import static java.util.UUID.randomUUID;
import static org.assertj.core.api.Assertions.assertThat;

import org.alfresco.model.ContentModel;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.namespace.QName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import com.atolcd.alfresco.filer.core.model.RepositoryNode;
import com.atolcd.alfresco.filer.core.service.FilerModelService;
import com.atolcd.alfresco.filer.core.test.domain.content.model.FilerTestConstants;
import com.atolcd.alfresco.filer.core.test.framework.RepositoryOperations;

public class ClassificationTreeTraitTest extends RepositoryOperations {

  @Autowired
  private FilerModelService filerModelService;

  @Autowired
  private NodeService nodeService;

  @Value("${filer.owner.username}")
  private String username;

  @Test
  public void filerAspect() {
    RepositoryNode node = getLibrary().childNode()
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
  public void type() {
    QName type = FilerTestConstants.Department.DocumentType.NAME;

    RepositoryNode node = getLibrary().childNode()
        .type(FilerTestConstants.Department.DocumentType.NAME)
        .property(FilerTestConstants.Department.Aspect.PROP_NAME, randomUUID())
        .build();

    createNode(node);

    assertThat(node.getType()).isEqualTo(type);

    NodeRef parent = node.getParent();
    assertThat(nodeService.getType(parent)).isEqualTo(ContentModel.TYPE_FOLDER);

    NodeRef grandParent = nodeService.getPrimaryParent(parent).getParentRef();
    assertThat(nodeService.getType(grandParent)).isEqualTo(ContentModel.TYPE_FOLDER);

    NodeRef greatGrandParent = nodeService.getPrimaryParent(grandParent).getParentRef();
    assertThat(nodeService.getType(greatGrandParent)).isEqualTo(FilerTestConstants.Department.FolderType.NAME);
  }

  @Test
  public void propertyOwner() {
    RepositoryNode node = getLibrary().childNode()
        .type(FilerTestConstants.Department.DocumentType.NAME)
        .property(FilerTestConstants.Department.Aspect.PROP_NAME, randomUUID())
        .build();

    createNode(node);

    // Fileable
    assertThat(node.getProperty(ContentModel.PROP_OWNER, String.class)).isEqualTo(username);

    // Segments
    NodeRef parent = node.getParent();
    assertThat(nodeService.getProperty(parent, ContentModel.PROP_OWNER)).isEqualTo(username);

    NodeRef grandParent = nodeService.getPrimaryParent(parent).getParentRef();
    assertThat(nodeService.getProperty(grandParent, ContentModel.PROP_OWNER)).isEqualTo(username);
  }
}
