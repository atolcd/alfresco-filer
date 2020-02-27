package com.atolcd.alfresco.filer.core.test.domain;

import static com.atolcd.alfresco.filer.core.test.framework.LibraryExtension.getLibrary;
import static java.util.UUID.randomUUID;
import static org.assertj.core.api.Assertions.assertThat;

import org.alfresco.error.AlfrescoRuntimeException;
import org.alfresco.model.ContentModel;
import org.alfresco.repo.site.SiteModel;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.atolcd.alfresco.filer.core.model.FilerException;
import com.atolcd.alfresco.filer.core.model.RepositoryNode;
import com.atolcd.alfresco.filer.core.service.FilerModelService;
import com.atolcd.alfresco.filer.core.test.framework.RepositoryNodeHelper;
import com.atolcd.alfresco.filer.core.test.framework.TestApplicationContext;
import com.atolcd.alfresco.filer.core.test.framework.TestAuthentication;
import com.atolcd.alfresco.filer.core.test.framework.TestLibrary;
import com.atolcd.alfresco.filer.core.test.framework.TestLibraryRole;

@TestApplicationContext
@TestLibrary
@TestAuthentication
@TestLibraryRole(SiteModel.SITE_CONTRIBUTOR)
public class DeniedFilerActionTest {

  @Autowired
  private FilerModelService filerModelService;

  @Autowired
  private NodeService nodeService;

  @Autowired
  private RepositoryNodeHelper repositoryNodeHelper;

  @Test
  public void withTypeContent() {
    String name = randomUUID().toString();

    RepositoryNode node = getLibrary().childNode()
        .type(ContentModel.TYPE_CONTENT)
        .aspect(filerModelService.getFileableAspect())
        .named(name)
        .build();

    NodeRef parentNodeRef = node.getParent().get();

    Throwable thrown = Assertions.catchThrowable(() -> repositoryNodeHelper.createNode(node));

    assertThat(thrown)
        .isInstanceOf(AlfrescoRuntimeException.class)
        .hasCauseInstanceOf(FilerException.class);
    assertThat(thrown.getCause())
        .hasMessageContaining("DENIED")
        .hasMessageContaining("filer.action.denied");

    assertThat(nodeService.getChildByName(parentNodeRef, ContentModel.ASSOC_CONTAINS, name)).isNull();
  }

  @Test
  public void withTypeFolder() {
    String name = randomUUID().toString();

    RepositoryNode node = getLibrary().childNode()
        .type(ContentModel.TYPE_FOLDER)
        .aspect(filerModelService.getFileableAspect())
        .named(name)
        .build();

    NodeRef parentNodeRef = node.getParent().get();

    Throwable thrown = Assertions.catchThrowable(() -> repositoryNodeHelper.createNode(node));

    assertThat(thrown)
        .isInstanceOf(AlfrescoRuntimeException.class)
        .hasCauseInstanceOf(FilerException.class);
    assertThat(thrown.getCause())
        .hasMessageContaining("DENIED")
        .hasMessageContaining("filer.action.denied");

    assertThat(nodeService.getChildByName(parentNodeRef, ContentModel.ASSOC_CONTAINS, name)).isNull();
  }
}
