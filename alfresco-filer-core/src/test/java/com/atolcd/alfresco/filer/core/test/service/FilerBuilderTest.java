package com.atolcd.alfresco.filer.core.test.service;

import static com.atolcd.alfresco.filer.core.test.framework.util.NodeRefUtils.randomNodeRef;
import static java.util.UUID.randomUUID;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.refEq;
import static org.mockito.Mockito.times;

import java.util.Locale;
import java.util.function.Consumer;

import org.alfresco.model.ContentModel;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.site.SiteService;
import org.alfresco.service.namespace.QName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;

import com.atolcd.alfresco.filer.core.model.PropertyInheritance;
import com.atolcd.alfresco.filer.core.model.RepositoryNode;
import com.atolcd.alfresco.filer.core.service.PropertyInheritanceService;
import com.atolcd.alfresco.filer.core.service.impl.FilerBuilder;

public class FilerBuilderTest extends AbstractFilerBuilderTest {

  @Mock
  private PropertyInheritanceService propertyInheritanceService;

  @Test
  public void documentLibraryFolder() {
    NodeRef rootNodeRef = randomNodeRef();
    FilerBuilder builder = new FilerBuilder(getFilerService(), new RepositoryNode());

    builder.root(rootNodeRef)
        .tree(AbstractFilerBuilderTest::buildDocumentLibrary);

    Mockito.verify(getFilerOperationService()).getFolder(eq(rootNodeRef), eq(SiteService.DOCUMENT_LIBRARY), any());
  }

  @Test
  public void nodeInDocumentLibrary() {
    RepositoryNode node = RepositoryNode.builder()
        .named(randomUUID())
        .build();
    FilerBuilder builder = new FilerBuilder(getFilerService(), node);

    NodeRef documentLibraryNode = randomNodeRef();
    Mockito.when(getFilerOperationService().getFolder(any(), any(), any())).thenReturn(documentLibraryNode);

    builder.root(randomNodeRef())
        .tree(AbstractFilerBuilderTest::buildDocumentLibrary)
        .updateAndMove();

    Mockito.verify(getFilerOperationService()).updateFileable(node, documentLibraryNode, node.getName().get());
  }

  @Test
  public void folderWithNameBasedOnNodeProperty() {
    stubCreateFolder();

    RepositoryNode node = RepositoryNode.builder()
        .property(ContentModel.PROP_TITLE, "x" + randomUUID())
        .build();
    FilerBuilder builder = new FilerBuilder(getFilerService(), node);

    builder.root(randomNodeRef())
        .tree(AbstractFilerBuilderTest::buildDocumentLibrary)
        .folder().asSegment()
            // A..Z
            .named().with(x -> {
              String name = x.getProperty(ContentModel.PROP_TITLE, String.class).get();
              return name.substring(0, 1).toUpperCase(Locale.getDefault());
            }).getOrCreate();

    verifyCreateFolder(times(1), "X", getCaptedParentNodeRefValue());
  }

  @Test
  public void multipleFolder() {
    stubCreateFolder();

    FilerBuilder builder = new FilerBuilder(getFilerService(), new RepositoryNode());

    String firstFolderName = randomUUID().toString();
    String secondFolderName = randomUUID().toString();

    builder.root(randomNodeRef())
        .tree(AbstractFilerBuilderTest::buildDocumentLibrary)
        .folder().asSegment()
            .named().with(firstFolderName).getOrCreate()
        .folder().asSegment()
            .named().with(secondFolderName).getOrCreate();

    verifyCreateFolder(times(1), firstFolderName, getAllCaptedParentNodeRefValues().get(0));
    verifyCreateFolder(times(1), secondFolderName, getAllCaptedParentNodeRefValues().get(1));
  }

  @Test
  public void propertyInheritence() {
    stubCreateFolder();

    Mockito.when(getFilerService().propertyInheritance()).thenReturn(propertyInheritanceService);

    RepositoryNode node = RepositoryNode.builder()
        .build();
    FilerBuilder builder = new FilerBuilder(getFilerService(), node);

    String name = randomUUID().toString();
    QName aspectMandatory = ContentModel.ASPECT_TAGGABLE;
    QName aspectOptional = ContentModel.ASPECT_INCOMPLETE;

    builder.root(randomNodeRef())
        .tree(AbstractFilerBuilderTest::buildDocumentLibrary)
        .folder().asSegment()
            .mandatoryPropertyInheritance(aspectMandatory)
            .optionalPropertyInheritance(aspectOptional)
            .named().with(name).getOrCreate();

    ArgumentCaptor<Consumer<NodeRef>> captor = buildNodeRefConsumerCaptor();
    Mockito.verify(getFilerOperationService()).getOrCreateFolder(eq(getCaptedParentNodeRefValue()), eq(ContentModel.TYPE_FOLDER),
        eq(name), any(), captor.capture());

    captor.getValue().accept(randomNodeRef());

    PropertyInheritance expectedPropertyInheritance = new PropertyInheritance();
    expectedPropertyInheritance.getMandatoryAspects().add(aspectMandatory);
    expectedPropertyInheritance.getOptionalAspects().add(aspectOptional);
    Mockito.verify(propertyInheritanceService).setProperties(any(), any(), refEq(expectedPropertyInheritance));
  }

  @SuppressWarnings("unchecked")
  private static ArgumentCaptor<Consumer<NodeRef>> buildNodeRefConsumerCaptor() {
    return ArgumentCaptor.forClass(Consumer.class);
  }
}
