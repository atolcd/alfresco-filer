package com.atolcd.alfresco.filer.core.test.service;

import static com.atolcd.alfresco.filer.core.test.util.NodeRefUtils.randomNodeRef;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;

import java.util.List;

import org.alfresco.model.ContentModel;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.site.SiteService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.verification.VerificationMode;

import com.atolcd.alfresco.filer.core.service.FilerOperationService;
import com.atolcd.alfresco.filer.core.service.FilerService;
import com.atolcd.alfresco.filer.core.service.impl.FilerFolderBuilder;

@ExtendWith(MockitoExtension.class)
public class AbstractFilerBuilderTest {

  protected static final String TEST_PROP_TITLE_A = "prop_title_A";
  protected static final String TEST_PROP_TITLE_B = "prop_title_B";

  @Mock
  private FilerService filerService;
  @Mock
  private FilerOperationService filerOperationService;

  @Captor
  private ArgumentCaptor<NodeRef> nodeRefCaptor;

  @BeforeEach
  public void initMock() {
    Mockito.when(filerService.operations()).thenReturn(filerOperationService);
    Mockito.when(filerOperationService.getFolder(any(), any(), any()))
        .thenReturn(randomNodeRef());
  }

  protected void stubCreateFolder() {
    Mockito.when(filerOperationService.getOrCreateFolder(nodeRefCaptor.capture(), any(), any(), any(), any()))
        .thenReturn(randomNodeRef());
  }

  protected void verifyCreateFolder(final VerificationMode times, final String folderName) {
    Mockito.verify(filerOperationService, times).getOrCreateFolder(any(), eq(ContentModel.TYPE_FOLDER), eq(folderName), any(),
        any());
  }

  protected void verifyCreateFolder(final VerificationMode times, final String folderName, final NodeRef parent) {
    Mockito.verify(filerOperationService, times).getOrCreateFolder(eq(parent), eq(ContentModel.TYPE_FOLDER), eq(folderName),
        any(), any());
  }

  protected static FilerFolderBuilder buildDocumentLibrary(final FilerFolderBuilder filerFolderBuilder) {
    return filerFolderBuilder.folder().named().with(SiteService.DOCUMENT_LIBRARY).get();
  }

  public FilerService getFilerService() {
    return filerService;
  }

  public FilerOperationService getFilerOperationService() {
    return filerOperationService;
  }

  protected NodeRef getCaptedParentNodeRefValue() {
    return nodeRefCaptor.getValue();
  }

  protected List<NodeRef> getAllCaptedParentNodeRefValues() {
    return nodeRefCaptor.getAllValues();
  }
}
