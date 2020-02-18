package com.atolcd.alfresco.filer.core.test.domain;

import static com.atolcd.alfresco.filer.core.test.domain.util.NodePathUtils.nodePath;
import static com.atolcd.alfresco.filer.core.util.FilerNodeUtils.getPath;
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
import java.util.Optional;
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
import org.springframework.beans.factory.annotation.Autowired;

import com.atolcd.alfresco.filer.core.model.RepositoryNode;
import com.atolcd.alfresco.filer.core.test.domain.content.model.FilerTestConstants;
import com.atolcd.alfresco.filer.core.util.FilerNodeUtils;

/**
 * Test multiple executions in parallel of one operation
 */
public class UnaryOperationParallelTest extends AbstractParallelTest {

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
      execute(endingLatch, () -> {
        RepositoryNode node = buildNode(departmentName, date).build();

        // Wait for every thread to be ready to launch parallel createNode
        startingBarrier.await(10, TimeUnit.SECONDS);

        createNode(node);
        results.add(node);
        return null;
      });
    }

    // Wait for every thread to finish job before asserting results
    endingLatch.await();

    // Assert all threads were ready for parallel createNode
    assertThat(startingBarrier.isBroken()).isFalse();

    assertThat(results.stream().map(FilerNodeUtils::getPath))
        .hasSize(NUM_THREAD_TO_LAUNCH)
        .containsOnly(nodePath(departmentName, date));
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
      execute(endingLatch, () -> {
        RepositoryNode node = buildNode(departmentName, sourceDate).build();

        createNode(node);
        results.add(node);

        preparationAssertBarrier.await(10, TimeUnit.SECONDS);
        // Wait for assertion on created nodes taking place in main thread
        preparationAssertBarrier.await(10, TimeUnit.SECONDS);

        segmentAncestors.add(node.getParent().get());
        NodeRef grandParentRef = nodeService.getPrimaryParent(node.getParent().get()).getParentRef();
        segmentAncestors.add(grandParentRef);
        NodeRef greatGrandParentRef = nodeService.getPrimaryParent(grandParentRef).getParentRef();
        closestNonSegmentAncestor.add(greatGrandParentRef);

        Map<QName, Serializable> dateProperty = Collections.singletonMap(FilerTestConstants.ImportedAspect.PROP_DATE,
            Date.from(targetDate.atZone(ZoneId.systemDefault()).toInstant()));

        // Wait for every thread to be ready to launch parallel updateNode
        startingBarrier.await(10, TimeUnit.SECONDS);

        updateNode(node, dateProperty);
        return null;
      });
    }

    // Wait for node creation to finish and then assert all nodes are well created
    preparationAssertBarrier.await();
    assertThat(results).hasSize(NUM_THREAD_TO_LAUNCH);
    for (RepositoryNode node : results) {
      assertThat(getPath(node)).isEqualTo(nodePath(departmentName, sourceDate));
    }
    preparationAssertBarrier.await();

    // Wait for every thread to finish job before asserting results
    endingLatch.await();

    // Assert all threads were ready for parallel updateNode
    assertThat(startingBarrier.isBroken()).isFalse();

    assertThat(results.stream().map(FilerNodeUtils::getPath))
        .hasSize(NUM_THREAD_TO_LAUNCH)
        .containsOnly(nodePath(departmentName, targetDate));

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
      execute(endingLatch, () -> {
        RepositoryNode node = buildNode(departmentName, date)
          .aspect(ContentModel.ASPECT_TEMPORARY) // Do not archive node, this could generate contention on creating user trashcan
          .build();

        createNode(node);
        results.add(node);

        preparationAssertBarrier.await(10, TimeUnit.SECONDS);
        // Wait for assertion on created nodes taking place in main thread
        preparationAssertBarrier.await(10, TimeUnit.SECONDS);

        segmentAncestors.add(node.getParent().get());
        NodeRef grandParentRef = nodeService.getPrimaryParent(node.getParent().get()).getParentRef();
        segmentAncestors.add(grandParentRef);
        NodeRef greatGrandParentRef = nodeService.getPrimaryParent(grandParentRef).getParentRef();
        closestNonSegmentAncestor.add(greatGrandParentRef);

        // Wait for every thread to be ready to launch parallel deleteNode
        startingBarrier.await(10, TimeUnit.SECONDS);

        deleteNode(node);
        return null;
      });
    }

    // Wait for node creation to finish and then assert all nodes are well created
    preparationAssertBarrier.await();
    assertThat(results).hasSize(NUM_THREAD_TO_LAUNCH);
    for (RepositoryNode node : results) {
      assertThat(getPath(node)).isEqualTo(nodePath(departmentName, date));
    }
    preparationAssertBarrier.await();

    // Wait for every thread to finish job before asserting results
    endingLatch.await();

    // Assert all threads were ready for parallel deleteNode
    assertThat(startingBarrier.isBroken()).isFalse();

    assertThat(results.stream().map(RepositoryNode::getNodeRef).map(Optional::get).map(nodeService::exists))
        .hasSize(NUM_THREAD_TO_LAUNCH)
        .containsOnly(false);

    assertThat(segmentAncestors.stream().map(nodeService::exists)).isNotEmpty().containsOnly(false);
    assertThat(closestNonSegmentAncestor.stream().map(nodeService::exists)).isNotEmpty().containsOnly(true);
  }
}
