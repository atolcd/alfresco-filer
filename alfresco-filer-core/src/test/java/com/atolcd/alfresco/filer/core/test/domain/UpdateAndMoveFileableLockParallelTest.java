package com.atolcd.alfresco.filer.core.test.domain;

import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import com.atolcd.alfresco.filer.core.service.FilerFolderService;
import com.atolcd.alfresco.filer.core.service.FilerUpdateService;
import com.atolcd.alfresco.filer.core.test.util.AutowiredMockAwareMockitoExtension;

/**
 * This test check that the (possibly future) parent node of a fileable node is effectively locked before attempting to move or
 * rename the fileable node.<br>
 * If the parent is not locked, it could be deleted by another transaction running simultaneously. This would result in attempting
 * to move a node in a non existant folder.
 */
@ExtendWith(AutowiredMockAwareMockitoExtension.class)
public class UpdateAndMoveFileableLockParallelTest extends AbstractParallelTest {

  private static final Logger LOGGER = LoggerFactory.getLogger(UpdateAndMoveFileableLockParallelTest.class);

  @Autowired
  private FilerFolderService filerFolderService;
  @Autowired
  private FilerUpdateService filerUpdateService;

  @BeforeEach
  public void setUpSpiedDependencies() {
    Mockito.doAnswer(invocation -> {
      LOGGER.debug("filerFolderService.deleteFolder : Waiting before call");
      TimeUnit.SECONDS.sleep(2);
      invocation.callRealMethod();
      return null;
    }).when(filerFolderService).deleteFolder(Mockito.any());

    Mockito.doAnswer(invocation -> {
      LOGGER.debug("filerUpdateService.updateAndMoveFileable : Waiting before call");
      TimeUnit.SECONDS.sleep(1);
      invocation.callRealMethod();
      return null;
    }).when(filerUpdateService).updateAndMoveFileable(Mockito.any(), Mockito.any(), Mockito.any());
  }

  @Test
  public void createAndDeleteNodes() throws InterruptedException, BrokenBarrierException {
    createAndDeleteNodesImpl();
  }
}
