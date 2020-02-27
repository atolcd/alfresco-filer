package com.atolcd.alfresco.filer.core.test.domain;

import static com.atolcd.alfresco.filer.core.test.framework.LibraryExtension.getLibrary;
import static java.util.UUID.randomUUID;
import static org.assertj.core.api.Assertions.assertThat;

import org.alfresco.model.ContentModel;
import org.alfresco.repo.site.SiteModel;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.namespace.QName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.atolcd.alfresco.filer.core.model.RepositoryNode;
import com.atolcd.alfresco.filer.core.service.FilerModelService;
import com.atolcd.alfresco.filer.core.test.domain.content.model.FilerTestConstants;
import com.atolcd.alfresco.filer.core.test.framework.RepositoryNodeHelper;
import com.atolcd.alfresco.filer.core.test.framework.TestApplicationContext;
import com.atolcd.alfresco.filer.core.test.framework.TestAuthentication;
import com.atolcd.alfresco.filer.core.test.framework.TestLibrary;
import com.atolcd.alfresco.filer.core.test.framework.TestLibraryRole;

@TestApplicationContext
@TestLibrary
@TestAuthentication
@TestLibraryRole(SiteModel.SITE_CONTRIBUTOR)
public class ClassificationTreeTraitTest {

  @Autowired
  private FilerModelService filerModelService;

  @Autowired
  private NodeService nodeService;

  @Autowired
  private RepositoryNodeHelper repositoryNodeHelper;

  @Test
  public void filerAspect() {
    RepositoryNode node = getLibrary().childNode()
        .type(FilerTestConstants.Department.DocumentType.NAME)
        .property(FilerTestConstants.Department.Aspect.PROP_NAME, randomUUID())
        .build();

    repositoryNodeHelper.createNode(node);

    assertThat(node.getAspects()).contains(filerModelService.getFileableAspect());

    NodeRef parent = node.getParent().get();
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

    repositoryNodeHelper.createNode(node);

    assertThat(node.getType()).contains(type);

    NodeRef parent = node.getParent().get();
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

    repositoryNodeHelper.createNode(node);

    // Fileable
    assertThat(node.getProperty(ContentModel.PROP_OWNER, String.class)).contains(filerModelService.getOwnerUsername());

    // Segments
    NodeRef parent = node.getParent().get();
    assertThat(nodeService.getProperty(parent, ContentModel.PROP_OWNER)).isEqualTo(filerModelService.getOwnerUsername());

    NodeRef grandParent = nodeService.getPrimaryParent(parent).getParentRef();
    assertThat(nodeService.getProperty(grandParent, ContentModel.PROP_OWNER)).isEqualTo(filerModelService.getOwnerUsername());
  }
}
