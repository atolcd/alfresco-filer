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

import com.atolcd.alfresco.filer.core.service.FilerOperationService;
import com.atolcd.alfresco.filer.core.service.FilerUpdateService;
import com.atolcd.alfresco.filer.core.test.framework.AutowiredMockAwareMockitoExtension;

/**
 * During node creation or updating, when trying to lock the parent node, this parent one could have been already deleted in
 * another transaction running simultaneously. This would result in failing to acquire the lock and put the code out of action.
 */
@ExtendWith(AutowiredMockAwareMockitoExtension.class)
public class LockFolderNodeParallelTest extends AbstractParallelTest {

  private static final Logger LOGGER = LoggerFactory.getLogger(LockFolderNodeParallelTest.class);

  @Autowired
  private FilerOperationService filerOperationService;
  @Autowired
  private FilerUpdateService filerUpdateService;

  @BeforeEach
  public void setUpSpiedDependencies() {
    Mockito.doAnswer(invocation -> {
      LOGGER.debug("filerOperationService.deleteSegment : Waiting before call");
      TimeUnit.SECONDS.sleep(1);
      invocation.callRealMethod();
      return null;
    }).when(filerOperationService).deleteSegment(Mockito.any());

    Mockito.doAnswer(invocation -> {
      LOGGER.debug("filerUpdateService.updateAndMoveFileable : Waiting before call");
      TimeUnit.SECONDS.sleep(2);
      invocation.callRealMethod();
      return null;
    }).when(filerUpdateService).updateAndMoveFileable(Mockito.any(), Mockito.any(), Mockito.any());
  }

  @Test
  public void createAndDeleteNodes() throws InterruptedException, BrokenBarrierException {
    createAndDeleteNodesImpl();
  }
}
