package com.atolcd.alfresco.filer.core.test.domain;

import static java.util.UUID.randomUUID;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.alfresco.model.ContentModel;
import org.alfresco.service.cmr.repository.NodeService;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import com.atolcd.alfresco.filer.core.model.RepositoryNode;

public class DuplicateNameParallelTest extends AbstractParallelTest {

  private static final Logger LOGGER = LoggerFactory.getLogger(DuplicateNameParallelTest.class);

  @Autowired
  private NodeService nodeService;

  @Test
  public void createAndDeleteNodes() throws InterruptedException, BrokenBarrierException {
    String departmentName = randomUUID().toString();
    LocalDateTime date = LocalDateTime.of(2004, 8, 12, 0, 0, 0);

    CyclicBarrier startingBarrier = new CyclicBarrier(NUM_THREAD_TO_LAUNCH);
    CountDownLatch endingLatch = new CountDownLatch(NUM_THREAD_TO_LAUNCH);
    List<RepositoryNode> results = Collections.synchronizedList(new ArrayList<>());
    AtomicInteger nodeNameSuffix = new AtomicInteger();

    for (int i = 0; i < NUM_THREAD_TO_LAUNCH; i++) {
      final int taskNumber = i;
      execute(() -> {
        LOGGER.debug("Task {}: task started", taskNumber);
        RepositoryNode node = buildNode(departmentName, date).build();

        try {
          // Wait for every task to be ready for launching parallel task execution
          startingBarrier.await(10, TimeUnit.SECONDS);

          LOGGER.debug("Task {}: node creation start", taskNumber);
          // Name generation and node creation must be in the same transaction as the whole transaction execution will be retried
          // when there is an attempt to create nodes with duplicate name.
          doInTransaction(() -> {
            // Generate name based on a variable shared between all test threads
            node.getProperties().put(ContentModel.PROP_NAME, "x" + Integer.toString(nodeNameSuffix.get()));
            createNodeImpl(node);
          });
          LOGGER.debug("Task {}: node creation end", taskNumber);

          results.add(node);

          // Wait before incrementing suffix to let more chance to other thread to generate duplicate node name
          TimeUnit.MILLISECONDS.sleep(250);
          nodeNameSuffix.getAndIncrement();
        } catch (Exception e) { //NOPMD Catch all exceptions that might occur in thread as they will not be thrown to main thread
          LOGGER.error("Task " + taskNumber + ": could not create node", e);
        }
      }, endingLatch);
    }

    // Wait for every task to finish job before asserting results
    endingLatch.await();

    LOGGER.debug("All tasks are done, starting assertions");

    // Assert all tasks were ready for parallel task execution
    assertThat(startingBarrier.isBroken()).isFalse();

    assertThat(results.stream().map(RepositoryNode::getName).map(Optional::get))
        .doesNotHaveDuplicates();
    assertThat(results.stream().map(RepositoryNode::getNodeRef).map(nodeService::exists))
        .hasSize(NUM_THREAD_TO_LAUNCH)
        .containsOnly(true);
  }
}
