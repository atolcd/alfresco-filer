package com.atolcd.alfresco.filer.core.test.domain;

import static java.util.UUID.randomUUID;
import static org.assertj.core.api.Assertions.assertThat;

import org.alfresco.error.AlfrescoRuntimeException;
import org.alfresco.model.ContentModel;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.atolcd.alfresco.filer.core.model.FilerException;
import com.atolcd.alfresco.filer.core.model.RepositoryNode;
import com.atolcd.alfresco.filer.core.service.FilerModelService;
import com.atolcd.alfresco.filer.core.test.framework.DocumentLibraryProvider;

public class DeniedFilerActionTest extends DocumentLibraryProvider {

  @Autowired
  private FilerModelService filerModelService;

  @Autowired
  private NodeService nodeService;

  @Test
  public void withTypeContent() {
    String name = randomUUID().toString();

    RepositoryNode node = buildNode()
        .type(ContentModel.TYPE_CONTENT)
        .aspect(filerModelService.getFileableAspect())
        .named(name)
        .build();

    NodeRef parentNodeRef = node.getParent();

    Throwable thrown = Assertions.catchThrowable(() -> createNode(node));

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

    RepositoryNode node = buildNode()
        .type(ContentModel.TYPE_FOLDER)
        .aspect(filerModelService.getFileableAspect())
        .named(name)
        .build();

    NodeRef parentNodeRef = node.getParent();

    Throwable thrown = Assertions.catchThrowable(() -> createNode(node));

    assertThat(thrown)
        .isInstanceOf(AlfrescoRuntimeException.class)
        .hasCauseInstanceOf(FilerException.class);
    assertThat(thrown.getCause())
        .hasMessageContaining("DENIED")
        .hasMessageContaining("filer.action.denied");

    assertThat(nodeService.getChildByName(parentNodeRef, ContentModel.ASSOC_CONTAINS, name)).isNull();
  }
}
