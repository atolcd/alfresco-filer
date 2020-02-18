package com.atolcd.alfresco.filer.core.test.service.impl;

import static com.atolcd.alfresco.filer.core.test.framework.util.NodeRefUtils.randomNode;
import static com.atolcd.alfresco.filer.core.test.framework.util.NodeRefUtils.randomNodeRef;
import static java.util.UUID.randomUUID;
import static org.assertj.core.api.Assertions.assertThat;

import org.alfresco.service.cmr.repository.NodeRef;
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
import com.atolcd.alfresco.filer.core.service.impl.FilerFolderBuilder;

@Execution(ExecutionMode.CONCURRENT)
@ExtendWith(MockitoExtension.class)
public class FilerFolderBuilderTest {

  @Mock(answer = Answers.RETURNS_DEEP_STUBS)
  private FilerService filerService;

  @Test
  public void condition() {
    FilerFolderBuilder filerFolderBuilder = new FilerFolderBuilder(filerService, randomNode(), randomNodeRef());

    filerFolderBuilder.condition(x -> false);
    assertThat(filerFolderBuilder.getContext().isEnabled()).isFalse();

    filerFolderBuilder.condition(x -> true);
    assertThat(filerFolderBuilder.getContext().isEnabled()).isTrue();
  }

  @Test
  public void conditionReverse() {
    FilerFolderBuilder filerFolderBuilder = new FilerFolderBuilder(filerService, randomNode(), randomNodeRef());

    filerFolderBuilder.conditionReverse();
    assertThat(filerFolderBuilder.getContext().isEnabled()).isFalse();

    filerFolderBuilder.conditionReverse();
    assertThat(filerFolderBuilder.getContext().isEnabled()).isTrue();
  }

  @Test
  public void conditionEnd() {
    FilerFolderContext context = new FilerFolderContext(randomNode(), randomNodeRef());
    context.enable(false);
    FilerFolderBuilder filerFolderBuilder = new FilerFolderBuilder(filerService, context, randomNodeRef());

    filerFolderBuilder.conditionEnd();
    assertThat(filerFolderBuilder.getContext().isEnabled()).isTrue();
  }

  @Test
  public void updateAndMoveWithContextEnabled() {
    FilerFolderBuilder filerFolderBuilder = new FilerFolderBuilder(filerService, randomNode(), randomNodeRef());

    filerFolderBuilder.rename().with(randomUUID().toString());

    filerFolderBuilder.updateAndMove();

    Mockito.verify(filerService).operations();
    Mockito.verify(filerService.operations()).updateFileable(Mockito.any(), Mockito.any(), Mockito.any());
  }

  @Test
  public void updateAndMoveWithContextDisabled() {
    FilerFolderContext context = new FilerFolderContext(randomNode(), randomNodeRef());
    context.enable(false);
    FilerFolderBuilder filerFolderBuilder = new FilerFolderBuilder(filerService, context, randomNodeRef());

    filerFolderBuilder.updateAndMove();

    Mockito.verifyNoInteractions(filerService);
  }

  @Test
  public void contextWithContextEnabled() {
    FilerFolderBuilder filerFolderBuilder = new FilerFolderBuilder(filerService, randomNode(), randomNodeRef());

    NodeRef nodeRef = randomNodeRef();

    filerFolderBuilder.contextFrom(context -> context.getNode().setNodeRef(nodeRef));

    assertThat(filerFolderBuilder.getContext().getNode().getNodeRef()).contains(nodeRef);
  }

  @Test
  public void contextWithContextDisabled() {
    FilerFolderContext contextDisabled = new FilerFolderContext(randomNode(), randomNodeRef());
    contextDisabled.enable(false);
    FilerFolderBuilder filerFolderBuilder = new FilerFolderBuilder(filerService, contextDisabled, randomNodeRef());

    NodeRef nodeRef = randomNodeRef();

    filerFolderBuilder.contextFrom(context -> context.getNode().setNodeRef(nodeRef));

    assertThat(filerFolderBuilder.getContext().getNode().getNodeRef().get()).isNotEqualTo(nodeRef);
  }
}
