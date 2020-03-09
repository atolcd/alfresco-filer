package com.atolcd.alfresco.filer.core.test.service.impl;

import static com.atolcd.alfresco.filer.core.test.framework.util.NodeRefUtils.randomNode;
import static com.atolcd.alfresco.filer.core.test.framework.util.NodeRefUtils.randomNodeRef;
import static java.util.UUID.randomUUID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import org.alfresco.model.ContentModel;
import org.alfresco.service.namespace.QName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import com.atolcd.alfresco.filer.core.model.FilerFolderContext;
import com.atolcd.alfresco.filer.core.service.FilerService;
import com.atolcd.alfresco.filer.core.service.impl.FilerFolderTypeBuilder;

@Execution(ExecutionMode.CONCURRENT)
@ExtendWith(MockitoExtension.class)
public class FilerFolderTypeBuilderTest {

  @Mock(answer = Answers.RETURNS_DEEP_STUBS)
  private FilerService filerService;

  @Test
  public void checkGetOrCreateWithContextEnabled() {
    FilerFolderContext context = new FilerFolderContext(randomNode(), randomNodeRef());
    FilerFolderTypeBuilder filerFolderTypeBuilder = new FilerFolderTypeBuilder(filerService, context, ContentModel.TYPE_FOLDER);

    filerFolderTypeBuilder.named().with(randomUUID().toString());

    filerFolderTypeBuilder.getOrCreate();

    verify(filerService).operations();
    verify(filerService.operations()).getOrCreateFolder(any(), any(), any(), any(), any());
  }

  @Test
  public void checkGetOrCreateWithContextDisabled() {
    FilerFolderContext context = new FilerFolderContext(randomNode(), randomNodeRef());
    context.enable(false);
    FilerFolderTypeBuilder filerFolderTypeBuilder = new FilerFolderTypeBuilder(filerService, context, ContentModel.TYPE_FOLDER);

    filerFolderTypeBuilder.getOrCreate();

    verifyNoInteractions(filerService);
  }

  @Test
  public void checkGetWithContextEnabled() {
    FilerFolderContext context = new FilerFolderContext(randomNode(), randomNodeRef());
    FilerFolderTypeBuilder filerFolderTypeBuilder = new FilerFolderTypeBuilder(filerService, context, ContentModel.TYPE_FOLDER);

    filerFolderTypeBuilder.named().with(randomUUID().toString());

    filerFolderTypeBuilder.get();

    verify(filerService).operations();
    verify(filerService.operations()).getFolder(any(), any(), any());
  }

  @Test
  public void checkGetWithContextDisabled() {
    FilerFolderContext context = new FilerFolderContext(randomNode(), randomNodeRef());
    context.enable(false);
    FilerFolderTypeBuilder filerFolderTypeBuilder = new FilerFolderTypeBuilder(filerService, context, ContentModel.TYPE_FOLDER);

    filerFolderTypeBuilder.get();

    verifyNoInteractions(filerService);
  }

  @Test
  public void updateAndMoveWithContextEnabled() {
    FilerFolderContext context = new FilerFolderContext(randomNode(), randomNodeRef());
    FilerFolderTypeBuilder filerFolderTypeBuilder = new FilerFolderTypeBuilder(filerService, context, ContentModel.TYPE_FOLDER);

    filerFolderTypeBuilder.named().with(randomUUID().toString());

    filerFolderTypeBuilder.updateAndMove();

    verify(filerService, Mockito.times(2)).operations();
    verify(filerService.operations()).updateFileable(any(), any(), any());
    verify(filerService.operations()).updateFolder(any(), any(), any());
  }

  @Test
  public void updateAndMoveWithContextDisabled() {
    FilerFolderContext context = new FilerFolderContext(randomNode(), randomNodeRef());
    context.enable(false);
    FilerFolderTypeBuilder filerFolderTypeBuilder = new FilerFolderTypeBuilder(filerService, context, ContentModel.TYPE_FOLDER);

    filerFolderTypeBuilder.updateAndMove();

    verifyNoInteractions(filerService);
  }

  @Test
  public void addingPropertyInheritanceWithContextEnabled() {
    FilerFolderContext context = new FilerFolderContext(randomNode(), randomNodeRef());
    FilerFolderTypeBuilder filerFolderTypeBuilder = new FilerFolderTypeBuilder(filerService, context, ContentModel.TYPE_FOLDER);

    QName aspect = ContentModel.ASPECT_WORKING_COPY;

    filerFolderTypeBuilder.mandatoryPropertyInheritance(aspect);
    filerFolderTypeBuilder.optionalPropertyInheritance(aspect);

    // FilerFolderTypeBuilder does not provide a method to directly get context.
    // We use get() method to get a filerFolderBuilder and get context from it.
    filerFolderTypeBuilder.named().with(randomUUID().toString());
    context = filerFolderTypeBuilder.get().getContext();

    assertThat(context.getPropertyInheritance().getMandatoryAspects()).contains(aspect);
    assertThat(context.getPropertyInheritance().getOptionalAspects()).contains(aspect);
  }

  @Test
  public void addingPropertyInheritanceWithContextDisabled() {
    FilerFolderContext context = new FilerFolderContext(randomNode(), randomNodeRef());
    context.enable(false);
    FilerFolderTypeBuilder filerFolderTypeBuilder = new FilerFolderTypeBuilder(filerService, context, ContentModel.TYPE_FOLDER);

    QName aspect = ContentModel.ASPECT_WORKING_COPY;

    filerFolderTypeBuilder.mandatoryPropertyInheritance(aspect);
    filerFolderTypeBuilder.optionalPropertyInheritance(aspect);

    // FilerFolderTypeBuilder does not provide a method to directly get context.
    // We use get() method to get a filerFolderBuilder and get context from it.
    context = filerFolderTypeBuilder.get().getContext();

    assertThat(context.getPropertyInheritance().getMandatoryAspects()).doesNotContain(aspect);
    assertThat(context.getPropertyInheritance().getOptionalAspects()).doesNotContain(aspect);
  }

  @Test
  public void clearingPropertyInheritanceWithContextEnabled() {
    QName aspect = ContentModel.ASPECT_WORKING_COPY;

    FilerFolderContext initialContext = new FilerFolderContext(randomNode(), randomNodeRef());
    initialContext.getPropertyInheritance().getMandatoryAspects().add(aspect);
    initialContext.getPropertyInheritance().getOptionalAspects().add(aspect);

    FilerFolderTypeBuilder filerFolderTypeBuilder = new FilerFolderTypeBuilder(filerService, initialContext,
        ContentModel.TYPE_FOLDER);

    filerFolderTypeBuilder.clearPropertyInheritance();

    // FilerFolderTypeBuilder does not provide a method to directly get context.
    // We use get() method to get a filerFolderBuilder and get context from it.
    filerFolderTypeBuilder.named().with(randomUUID().toString());
    FilerFolderContext context = filerFolderTypeBuilder.get().getContext();

    assertThat(context.getPropertyInheritance().getMandatoryAspects()).isEmpty();
    assertThat(context.getPropertyInheritance().getOptionalAspects()).isEmpty();
  }

  @Test
  public void clearingPropertyInheritanceWithContextDisabled() {
    QName aspect = ContentModel.ASPECT_WORKING_COPY;

    FilerFolderContext initialContext = new FilerFolderContext(randomNode(), randomNodeRef());
    initialContext.getPropertyInheritance().getMandatoryAspects().add(aspect);
    initialContext.getPropertyInheritance().getOptionalAspects().add(aspect);
    initialContext.enable(false);

    FilerFolderTypeBuilder filerFolderTypeBuilder = new FilerFolderTypeBuilder(filerService, initialContext,
        ContentModel.TYPE_FOLDER);

    // FilerFolderTypeBuilder does not provide a method to directly get context.
    // We use get() method to get a filerFolderBuilder and get context from it.
    FilerFolderContext context = filerFolderTypeBuilder.get().getContext();

    assertThat(context.getPropertyInheritance().getMandatoryAspects()).contains(aspect);
    assertThat(context.getPropertyInheritance().getOptionalAspects()).contains(aspect);
  }
}
