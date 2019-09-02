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
import com.atolcd.alfresco.filer.core.test.framework.AutowiredMockAwareMockitoExtension;

/**
 * Verify database's transaction isolation
 *
 * <p>
 * This test were to failed when trying H2database for testing, before using PostgreSQL for this very reason.<br>
 * Transactions were not rollbacked even though an integrity violation was triggered by this test case.
 * Highest level of isolation available with H2database did not solve the problem.
 * </p>
 */
@ExtendWith(AutowiredMockAwareMockitoExtension.class)
public class DatabaseTransactionIsolationTest extends AbstractParallelTest {

  private static final Logger LOGGER = LoggerFactory.getLogger(DatabaseTransactionIsolationTest.class);

  @Autowired
  private FilerOperationService filerOperationService;

  @BeforeEach
  public void setUpSpiedDependencies() {
    Mockito.doAnswer(invocation -> {
      LOGGER.debug("filerOperationService.deleteFilerSegment : Waiting before call");
      TimeUnit.SECONDS.sleep(1);
      invocation.callRealMethod();
      return null;
    }).when(filerOperationService).deleteSegment(Mockito.any());

    Mockito.doAnswer(invocation -> {
      invocation.callRealMethod();
      LOGGER.debug("filerOperationService.execute : Waiting after call");
      TimeUnit.SECONDS.sleep(2);
      return null;
    }).when(filerOperationService).execute(Mockito.any(), Mockito.any());
  }

  @Test
  public void createAndDeleteNodes() throws InterruptedException, BrokenBarrierException {
    createAndDeleteNodesImpl();
  }
}
