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

public class FilerBuilderMultipleConditionTest extends AbstractFilerBuilderTest {

  private static final String TEST_PROP_DESCRIPTION_A = "prop_desc_A";
  private static final String TEST_PROP_DESCRIPTION_B = "prop_desc_B";

  private String firstFolderName;
  private String secondFolderName;

  @BeforeEach
  public void folderPrecondition() {
    firstFolderName = randomUUID().toString();
    secondFolderName = randomUUID().toString();
  }

  @Test
  public void firstFalseSecondFalse() {
    RepositoryNode node = RepositoryNode.builder()
        .property(ContentModel.PROP_TITLE, TEST_PROP_TITLE_B)
        .property(ContentModel.PROP_DESCRIPTION, TEST_PROP_DESCRIPTION_B)
        .build();
    FilerBuilder builder = new FilerBuilder(getFilerService(), node);

    builder.root(randomNodeRef())
        .tree(this::buildMultipleConditionTree);

    verifyCreateFolder(never(), firstFolderName);
    verifyCreateFolder(never(), secondFolderName);
  }

  @Test
  public void firstFalseSecondTrue() {
    stubCreateFolder();

    RepositoryNode node = RepositoryNode.builder()
        .property(ContentModel.PROP_TITLE, TEST_PROP_TITLE_B)
        .property(ContentModel.PROP_DESCRIPTION, TEST_PROP_DESCRIPTION_A)
        .build();
    FilerBuilder builder = new FilerBuilder(getFilerService(), node);

    builder.root(randomNodeRef())
        .tree(this::buildMultipleConditionTree);

    verifyCreateFolder(never(), firstFolderName, getCaptedParentNodeRefValue());
    verifyCreateFolder(times(1), secondFolderName, getCaptedParentNodeRefValue());
  }

  @Test
  public void firstTrueSecondFalse() {
    stubCreateFolder();

    RepositoryNode node = RepositoryNode.builder()
        .property(ContentModel.PROP_TITLE, TEST_PROP_TITLE_A)
        .property(ContentModel.PROP_DESCRIPTION, TEST_PROP_DESCRIPTION_B)
        .build();
    FilerBuilder builder = new FilerBuilder(getFilerService(), node);

    builder.root(randomNodeRef())
        .tree(this::buildMultipleConditionTree);

    verifyCreateFolder(times(1), firstFolderName, getCaptedParentNodeRefValue());
    verifyCreateFolder(never(), secondFolderName, getCaptedParentNodeRefValue());
  }

  @Test
  public void firstTrueSecondTrue() {
    stubCreateFolder();

    RepositoryNode node = RepositoryNode.builder()
        .property(ContentModel.PROP_TITLE, TEST_PROP_TITLE_A)
        .property(ContentModel.PROP_DESCRIPTION, TEST_PROP_DESCRIPTION_A)
        .build();
    FilerBuilder builder = new FilerBuilder(getFilerService(), node);

    builder.root(randomNodeRef())
        .tree(this::buildMultipleConditionTree);

    verifyCreateFolder(times(1), firstFolderName, getAllCaptedParentNodeRefValues().get(0));
    verifyCreateFolder(times(1), secondFolderName, getAllCaptedParentNodeRefValues().get(1));
  }

  private FilerFolderBuilder buildMultipleConditionTree(final FilerFolderBuilder filerFolderBuilder) {
    return filerFolderBuilder
        .tree(AbstractFilerBuilderTest::buildDocumentLibrary)
        .condition(x -> x.getProperty(ContentModel.PROP_TITLE, String.class).equals(TEST_PROP_TITLE_A))
            .folder().asSegment().named().with(firstFolderName).getOrCreate()
        .condition(x -> x.getProperty(ContentModel.PROP_DESCRIPTION, String.class).equals(TEST_PROP_DESCRIPTION_A))
            .folder().asSegment().named().with(secondFolderName).getOrCreate();
  }
}
