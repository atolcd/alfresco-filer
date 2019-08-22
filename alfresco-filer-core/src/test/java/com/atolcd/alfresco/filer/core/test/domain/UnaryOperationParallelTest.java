package com.atolcd.alfresco.filer.core.test.domain;

import static java.util.UUID.randomUUID;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.TimeUnit;

import org.alfresco.model.ContentModel;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.namespace.QName;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import com.atolcd.alfresco.filer.core.model.RepositoryNode;
import com.atolcd.alfresco.filer.core.test.domain.content.model.FilerTestConstants;
import com.atolcd.alfresco.filer.core.test.util.SiteBasedTest;

/**
 * Test multiple executions in parallel of one operation
 */
public class UnaryOperationParallelTest extends AbstractParallelTest {

  private static final Logger LOGGER = LoggerFactory.getLogger(UnaryOperationParallelTest.class);

  @Autowired
  private NodeService nodeService;

  @Test
  public void createMultipleNodes() throws InterruptedException {
    String departmentName = randomUUID().toString();
    LocalDateTime date = LocalDateTime.of(2004, 8, 12, 0, 0, 0);

    CyclicBarrier startingBarrier = new CyclicBarrier(NUM_THREAD_TO_LAUNCH);
    CountDownLatch endingLatch = new CountDownLatch(NUM_THREAD_TO_LAUNCH);
    List<RepositoryNode> results = Collections.synchronizedList(new ArrayList<>());

    for (int i = 0; i < NUM_THREAD_TO_LAUNCH; i++) {
      execute(() -> {
        RepositoryNode node = buildNode(departmentName, date).build();

        try {
          // Wait for every thread to be ready to launch parallel createNode
          startingBarrier.await(10, TimeUnit.SECONDS);

          createNode(node);
          results.add(node);
        } catch (Exception e) { //NOPMD Catch all exceptions that might occur in thread as they will not be thrown to main thread
          LOGGER.error("Could not create node", e);
        }
      }, endingLatch);
    }

    // Wait for every thread to finish job before asserting results
    endingLatch.await();

    // Assert all threads were ready for parallel createNode
    assertThat(startingBarrier.isBroken()).isFalse();

    assertThat(results.stream().map(SiteBasedTest::getPath))
        .hasSize(NUM_THREAD_TO_LAUNCH)
        .containsOnly(buildNodePath(departmentName, date));
  }

  @Test
  public void updateMultipleNodes() throws InterruptedException, BrokenBarrierException {
    String departmentName = randomUUID().toString();
    LocalDateTime sourceDate = LocalDateTime.of(2004, 8, 12, 0, 0, 0);
    LocalDateTime targetDate = LocalDateTime.of(2002, 4, 6, 0, 0, 0);

    CyclicBarrier startingBarrier = new CyclicBarrier(NUM_THREAD_TO_LAUNCH);
    CyclicBarrier preparationAssertBarrier = new CyclicBarrier(NUM_THREAD_TO_LAUNCH + 1); // Test threads plus main thread
    CountDownLatch endingLatch = new CountDownLatch(NUM_THREAD_TO_LAUNCH);
    List<RepositoryNode> results = Collections.synchronizedList(new ArrayList<>());
    Set<NodeRef> segmentAncestors = Collections.synchronizedSet(new HashSet<>());
    Set<NodeRef> closestNonSegmentAncestor = Collections.synchronizedSet(new HashSet<>());

    for (int i = 0; i < NUM_THREAD_TO_LAUNCH; i++) {
      execute(() -> {
        RepositoryNode node = buildNode(departmentName, sourceDate).build();

        try {
          createNode(node);
          results.add(node);

          preparationAssertBarrier.await(10, TimeUnit.SECONDS);
          // Wait for assertion on created nodes taking place in main thread
          preparationAssertBarrier.await(10, TimeUnit.SECONDS);

          segmentAncestors.add(node.getParent());
          NodeRef grandParentRef = nodeService.getPrimaryParent(node.getParent()).getParentRef();
          segmentAncestors.add(grandParentRef);
          NodeRef greatGrandParentRef = nodeService.getPrimaryParent(grandParentRef).getParentRef();
          closestNonSegmentAncestor.add(greatGrandParentRef);

          Map<QName, Serializable> dateProperty = Collections.singletonMap(FilerTestConstants.ImportedAspect.PROP_DATE,
              Date.from(targetDate.atZone(ZoneId.systemDefault()).toInstant()));

          // Wait for every thread to be ready to launch parallel updateNode
          startingBarrier.await(10, TimeUnit.SECONDS);

          updateNode(node, dateProperty);
        } catch (Exception e) { //NOPMD Catch all exceptions that might occur in thread as they will not be thrown to main thread
          LOGGER.error("Could not update node", e);
        }
      }, endingLatch);
    }

    // Wait for node creation to finish and then assert all nodes are well created
    preparationAssertBarrier.await();
    assertThat(results).hasSize(NUM_THREAD_TO_LAUNCH);
    for (RepositoryNode node : results) {
      assertThat(getPath(node)).isEqualTo(buildNodePath(departmentName, sourceDate));
    }
    preparationAssertBarrier.await();

    // Wait for every thread to finish job before asserting results
    endingLatch.await();

    // Assert all threads were ready for parallel updateNode
    assertThat(startingBarrier.isBroken()).isFalse();

    assertThat(results.stream().map(SiteBasedTest::getPath))
        .hasSize(NUM_THREAD_TO_LAUNCH)
        .containsOnly(buildNodePath(departmentName, targetDate));

    assertThat(segmentAncestors.stream().map(nodeService::exists)).isNotEmpty().containsOnly(false);
    assertThat(closestNonSegmentAncestor.stream().map(nodeService::exists)).isNotEmpty().containsOnly(true);
  }

  @Test
  public void deleteMultipleNodes() throws InterruptedException, BrokenBarrierException {
    String departmentName = randomUUID().toString();
    LocalDateTime date = LocalDateTime.of(2004, 8, 12, 0, 0, 0);

    CyclicBarrier startingBarrier = new CyclicBarrier(NUM_THREAD_TO_LAUNCH);
    CyclicBarrier preparationAssertBarrier = new CyclicBarrier(NUM_THREAD_TO_LAUNCH + 1); // Test threads plus main thread
    CountDownLatch endingLatch = new CountDownLatch(NUM_THREAD_TO_LAUNCH);
    List<RepositoryNode> results = Collections.synchronizedList(new ArrayList<>());
    Set<NodeRef> segmentAncestors = Collections.synchronizedSet(new HashSet<>());
    Set<NodeRef> closestNonSegmentAncestor = Collections.synchronizedSet(new HashSet<>());

    for (int i = 0; i < NUM_THREAD_TO_LAUNCH; i++) {
      execute(() -> {
        RepositoryNode node = buildNode(departmentName, date)
          .aspect(ContentModel.ASPECT_TEMPORARY) // Do not archive node, this could generate contention on creating user trashcan
          .build();

        try {
          createNode(node);
          results.add(node);

          preparationAssertBarrier.await(10, TimeUnit.SECONDS);
          // Wait for assertion on created nodes taking place in main thread
          preparationAssertBarrier.await(10, TimeUnit.SECONDS);

          segmentAncestors.add(node.getParent());
          NodeRef grandParentRef = nodeService.getPrimaryParent(node.getParent()).getParentRef();
          segmentAncestors.add(grandParentRef);
          NodeRef greatGrandParentRef = nodeService.getPrimaryParent(grandParentRef).getParentRef();
          closestNonSegmentAncestor.add(greatGrandParentRef);

          // Wait for every thread to be ready to launch parallel deleteNode
          startingBarrier.await(10, TimeUnit.SECONDS);

          deleteNode(node);
        } catch (Exception e) { //NOPMD Catch all exceptions that might occur in thread as they will not be thrown to main thread
          LOGGER.error("Could not delete node", e);
        }
      }, endingLatch);
    }

    // Wait for node creation to finish and then assert all nodes are well created
    preparationAssertBarrier.await();
    assertThat(results).hasSize(NUM_THREAD_TO_LAUNCH);
    for (RepositoryNode node : results) {
      assertThat(getPath(node)).isEqualTo(buildNodePath(departmentName, date));
    }
    preparationAssertBarrier.await();

    // Wait for every thread to finish job before asserting results
    endingLatch.await();

    // Assert all threads were ready for parallel deleteNode
    assertThat(startingBarrier.isBroken()).isFalse();

    assertThat(results.stream().map(RepositoryNode::getNodeRef).map(nodeService::exists))
        .hasSize(NUM_THREAD_TO_LAUNCH)
        .containsOnly(false);

    assertThat(segmentAncestors.stream().map(nodeService::exists)).isNotEmpty().containsOnly(false);
    assertThat(closestNonSegmentAncestor.stream().map(nodeService::exists)).isNotEmpty().containsOnly(true);
  }
}
