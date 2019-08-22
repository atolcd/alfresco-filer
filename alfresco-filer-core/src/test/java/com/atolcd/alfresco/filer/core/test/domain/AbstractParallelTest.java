package com.atolcd.alfresco.filer.core.test.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

import com.atolcd.alfresco.filer.core.model.impl.RepositoryNodeBuilder;
import com.atolcd.alfresco.filer.core.test.domain.content.model.FilerTestConstants;
import com.atolcd.alfresco.filer.core.test.util.SiteBasedTest;

/**
 * Provide base class for parallel tests of {@linkplain com.atolcd.alfresco.filer.core.model.FilerAction Filer actions}. Assert
 * parallelism is available on test platform.
 *
 * <p>
 * Parallel tests are not executed concurrently to ensure most resources are available for the test, including multiple physical
 * threads to be sure parallelism is achieved during test.
 * </p>
 */
@Execution(ExecutionMode.SAME_THREAD)
public abstract class AbstractParallelTest extends SiteBasedTest {

  protected static final int NUM_THREAD_TO_LAUNCH = Runtime.getRuntime().availableProcessors() * 2;

  private final ExecutorService executor = Executors.newFixedThreadPool(NUM_THREAD_TO_LAUNCH);

  @BeforeAll
  public static void assertParallelismIsAvailable() {
    assertThat(Runtime.getRuntime().availableProcessors()).isGreaterThan(1);
  }

  protected void execute(final Runnable task, final CountDownLatch endingLatch) {
    executor.submit(() -> {
      AuthenticationUtil.setRunAsUserSystem();
      try {
        task.run();
      } finally {
        endingLatch.countDown();
        AuthenticationUtil.clearCurrentSecurityContext();
      }
    });
  }

  protected RepositoryNodeBuilder buildNode(final String departmentName, final LocalDateTime date) {
    return buildNode()
        .type(FilerTestConstants.Department.DocumentType.NAME)
        .property(FilerTestConstants.Department.Aspect.PROP_NAME, departmentName)
        .property(FilerTestConstants.ImportedAspect.PROP_DATE, date.atZone(ZoneId.systemDefault()));
  }
}
