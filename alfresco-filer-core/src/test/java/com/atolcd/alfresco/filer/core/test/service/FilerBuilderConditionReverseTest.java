package com.atolcd.alfresco.filer.core.test.service;

import static com.atolcd.alfresco.filer.core.test.util.NodeRefUtils.randomNodeRef;
import static java.util.UUID.randomUUID;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;

import org.alfresco.model.ContentModel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.atolcd.alfresco.filer.core.model.RepositoryNode;
import com.atolcd.alfresco.filer.core.service.impl.FilerBuilder;
import com.atolcd.alfresco.filer.core.service.impl.FilerFolderBuilder;

public class FilerBuilderConditionReverseTest extends AbstractFilerBuilderTest {

  private String conditionFolderName;
  private String reverseFolderName;

  @BeforeEach
  public void folderPrecondition() {
    stubCreateFolder();

    conditionFolderName = randomUUID().toString();
    reverseFolderName = randomUUID().toString();
  }

  @Test
  public void conditionTrue() {
    RepositoryNode node = RepositoryNode.builder()
        .property(ContentModel.PROP_TITLE, TEST_PROP_TITLE_A)
        .build();
    FilerBuilder builder = new FilerBuilder(getFilerService(), node);

    builder.root(randomNodeRef())
        .tree(this::buildConditionTree);

    verifyCreateFolder(times(1), conditionFolderName, getCaptedParentNodeRefValue());
    verifyCreateFolder(never(), reverseFolderName, getCaptedParentNodeRefValue());
  }

  @Test
  public void conditionFalse() {
    RepositoryNode node = RepositoryNode.builder()
        .property(ContentModel.PROP_TITLE, TEST_PROP_TITLE_B)
        .build();
    FilerBuilder builder = new FilerBuilder(getFilerService(), node);

    builder.root(randomNodeRef())
        .tree(this::buildConditionTree);

    verifyCreateFolder(never(), conditionFolderName, getCaptedParentNodeRefValue());
    verifyCreateFolder(times(1), reverseFolderName, getCaptedParentNodeRefValue());
  }

  private FilerFolderBuilder buildConditionTree(final FilerFolderBuilder filerFolderBuilder) {
    return filerFolderBuilder
        .tree(AbstractFilerBuilderTest::buildDocumentLibrary)
        .condition(x -> x.getProperty(ContentModel.PROP_TITLE, String.class).equals(TEST_PROP_TITLE_A))
            .folder().asSegment().named().with(conditionFolderName).getOrCreate()
        .conditionReverse()
            .folder().asSegment().named().with(reverseFolderName).getOrCreate();
  }
}
