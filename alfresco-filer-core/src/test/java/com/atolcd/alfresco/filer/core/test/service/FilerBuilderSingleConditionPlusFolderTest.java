package com.atolcd.alfresco.filer.core.test.service;

import static com.atolcd.alfresco.filer.core.test.framework.util.NodeRefUtils.randomNodeRef;
import static java.util.UUID.randomUUID;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;

import org.alfresco.model.ContentModel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.atolcd.alfresco.filer.core.model.RepositoryNode;
import com.atolcd.alfresco.filer.core.service.impl.FilerBuilder;
import com.atolcd.alfresco.filer.core.service.impl.FilerFolderBuilder;

public class FilerBuilderSingleConditionPlusFolderTest extends AbstractFilerBuilderTest {

  private String conditionFolderName;
  private String folderName;

  @BeforeEach
  public void folderPrecondition() {
    stubCreateFolder();

    conditionFolderName = randomUUID().toString();
    folderName = randomUUID().toString();
  }

  @Test
  public void conditionTrue() {
    RepositoryNode node = RepositoryNode.builder()
        .property(ContentModel.PROP_TITLE, TEST_PROP_TITLE_A)
        .build();
    FilerBuilder builder = new FilerBuilder(getFilerService(), node);

    builder.root(randomNodeRef())
        .tree(this::buildConditionPlusFolderTree);

    verifyCreateFolder(times(1), conditionFolderName, getAllCaptedParentNodeRefValues().get(0));
    verifyCreateFolder(times(1), folderName, getAllCaptedParentNodeRefValues().get(1));
  }

  @Test
  public void conditionFalse() {
    RepositoryNode node = RepositoryNode.builder()
        .property(ContentModel.PROP_TITLE, TEST_PROP_TITLE_B)
        .build();
    FilerBuilder builder = new FilerBuilder(getFilerService(), node);

    builder.root(randomNodeRef())
        .tree(this::buildConditionPlusFolderTree);

    verifyCreateFolder(never(), conditionFolderName, getCaptedParentNodeRefValue());
    verifyCreateFolder(times(1), folderName, getCaptedParentNodeRefValue());
  }

  private FilerFolderBuilder buildConditionPlusFolderTree(final FilerFolderBuilder filerFolderBuilder) {
    return filerFolderBuilder
        .tree(AbstractFilerBuilderTest::buildDocumentLibrary)
        .condition(x -> x.getProperty(ContentModel.PROP_TITLE, String.class).equals(TEST_PROP_TITLE_A))
            .folder().asSegment().named().with(conditionFolderName).getOrCreate()
        .conditionEnd()
        .folder().asSegment().named().with(folderName).getOrCreate();
  }
}
