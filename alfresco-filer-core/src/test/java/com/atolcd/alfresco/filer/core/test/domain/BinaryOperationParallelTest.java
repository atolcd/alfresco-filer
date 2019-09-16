package com.atolcd.alfresco.filer.core.test.domain;

import static com.atolcd.alfresco.filer.core.test.domain.util.NodePathUtils.nodePath;
import static java.util.UUID.randomUUID;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Collections;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.alfresco.model.ContentModel;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.namespace.QName;
import org.junit.jupiter.api.RepeatedTest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import com.atolcd.alfresco.filer.core.model.RepositoryNode;
import com.atolcd.alfresco.filer.core.test.domain.content.model.FilerTestConstants;

/**
 * Test parallel execution of two different operations
 *
 * <p>
 * Each test creates a thread by task to be launched exactly at the same time and waits until each task is performed before
 * checking assertions.<br>
 * All tests are repeated multiple times (10) because errors occur randomly, depending on the actual level of parallelization.
 * Therefore, even if there is a problem, the test can succeed multiple times before the issue arises.
 * </p>
 */
public class BinaryOperationParallelTest extends AbstractParallelTest {

  private static final Logger LOGGER = LoggerFactory.getLogger(BinaryOperationParallelTest.class);

  @Autowired
  private NodeService nodeService;

  /**
   * Test parallel creation and deletion of filed nodes within the same segment folder.
   *
   * <p>
   * Creates a node that is filed in the same folder as the one that is deleted. Each task is executed at the same time in its own
   * transaction.<br>
   * In this case, a newly created node could be deleted by mistake in another transaction running simultaneously that was
   * deleting, in the <strong>same</strong> filer segment, the <strong>single</strong> node at the time it was checked. This would
   * result in deleting the segment which was evaluated as empty and thus create the issue.
   * </p>
   */
  @RepeatedTest(10)
  public void createAndDeleteNodes() throws InterruptedException, BrokenBarrierException {
    createAndDeleteNodesImpl();
  }

  /**
   * Test parallel updating and deletion of filed nodes within the same segment folder.
   *
   * <p>
   * Update a node that is filled (and which filling requires the node to be moved) from the same folder as the node that is
   * deleted. Each task is executed at the same time in its own transaction.<br>
   * In this case, an empty segment may not be deleted if an update moves a node from a filer segment (source) to another and at
   * the same time another transaction is deleting, in the <strong>same</strong> source filer segment, the <strong>only other
   * remaining</strong> node. Both transactions could evaluate that there is still a node in the segment at the time it is checked
   * and would keep the segment and thus create the issue.
   * </p>
   */
  @RepeatedTest(10)
  public void updateAndDeleteNodesInSourceFolder() throws InterruptedException, BrokenBarrierException {
    String departmentName = randomUUID().toString();
    LocalDateTime sourceDate = LocalDateTime.of(2004, 8, 12, 0, 0, 0);
    LocalDateTime targetDate = LocalDateTime.of(2002, 4, 6, 0, 0, 0);

    CyclicBarrier startingBarrier = new CyclicBarrier(UPDATE_TASK + DELETE_TASK);
    CyclicBarrier preparationAssertBarrier = new CyclicBarrier(MAIN_TASK + UPDATE_TASK + DELETE_TASK);
    CountDownLatch endingLatch = new CountDownLatch(UPDATE_TASK + DELETE_TASK);
    AtomicReference<RepositoryNode> nodeToUpdate = new AtomicReference<>();
    AtomicReference<RepositoryNode> nodeToDelete = new AtomicReference<>();

    execute(endingLatch, () -> {
      LOGGER.debug("Update task: task started");
      RepositoryNode node = buildNode(departmentName, sourceDate).build();

      LOGGER.debug("Update task: creating node that will be updated");
      createNode(node);
      nodeToUpdate.set(node);

      preparationAssertBarrier.await(10, TimeUnit.SECONDS);
      // Wait for assertion on created nodes taking place in main task
      preparationAssertBarrier.await(10, TimeUnit.SECONDS);

      Map<QName, Serializable> dateProperty = Collections.singletonMap(FilerTestConstants.ImportedAspect.PROP_DATE,
          Date.from(targetDate.atZone(ZoneId.systemDefault()).toInstant()));

      // Wait for every task to be ready for launching parallel task execution
      startingBarrier.await(10, TimeUnit.SECONDS);

      LOGGER.debug("Update task: node creation start");
      updateNode(node, dateProperty);
      LOGGER.debug("Update task: node creation end");
      return null;
    });

    execute(endingLatch, () -> {
      LOGGER.debug("Delete task: task started");
      RepositoryNode node = buildNode(departmentName, sourceDate)
          // Do not archive node, this could generate contention on creating user trashcan
          .aspect(ContentModel.ASPECT_TEMPORARY)
          .build();

      LOGGER.debug("Delete task: creating node that will be deleted");
      createNode(node);
      nodeToDelete.set(node);

      preparationAssertBarrier.await(10, TimeUnit.SECONDS);
      // Wait for assertion on created nodes taking place in main task
      preparationAssertBarrier.await(10, TimeUnit.SECONDS);

      // Wait for every task to be ready for launching parallel task execution
      startingBarrier.await(10, TimeUnit.SECONDS);

      LOGGER.debug("Delete task: node deletion start");
      deleteNode(node);
      LOGGER.debug("Delete task: node deletion end");
      return null;
    });

    // Wait for node creation to finish and then assert all nodes are indeed created
    preparationAssertBarrier.await();
    assertThat(getPath(nodeToUpdate.get())).isEqualTo(nodePath(departmentName, sourceDate));
    assertThat(getPath(nodeToDelete.get())).isEqualTo(nodePath(departmentName, sourceDate));
    preparationAssertBarrier.await();

    // Wait for every task to finish job before asserting results
    endingLatch.await();

    LOGGER.debug("All tasks are done, starting assertions");

    // Assert all tasks were ready for parallel task execution
    assertThat(startingBarrier.isBroken()).isFalse();

    assertThat(getPath(nodeToUpdate.get())).isEqualTo(nodePath(departmentName, targetDate));

    assertThat(nodeService.exists(nodeToDelete.get().getNodeRef())).isFalse();
    assertThat(nodeService.exists(nodeToDelete.get().getParent())).isFalse();
  }

  /**
   * Test parallel updating and deletion of filed nodes within the same segment folder.
   *
   * <p>
   * Update a node that is filled (and which filling requires the node to be moved) from/to the same folder as the node that is
   * deleted. Each task is executed at the same time in its own transaction.<br>
   * In this case, if an update moves a node to a filer segment (target), it could be deleted by mistake in another transaction
   * running simultaneously that was deleting, in the <strong>same</strong> filer segment, the <strong>single</strong> node at the
   * time it was checked. This would result in deleting the segment which was evaluated as empty and thus create the issue.<br>
   * </p>
   */
  @RepeatedTest(10)
  public void updateAndDeleteNodesInTargetFolder() throws InterruptedException, BrokenBarrierException {
    String departmentName = randomUUID().toString();
    LocalDateTime sourceDate = LocalDateTime.of(2004, 8, 12, 0, 0, 0);
    LocalDateTime targetDate = LocalDateTime.of(2002, 4, 6, 0, 0, 0);

    CyclicBarrier startingBarrier = new CyclicBarrier(UPDATE_TASK + DELETE_TASK);
    CyclicBarrier preparationAssertBarrier = new CyclicBarrier(MAIN_TASK + UPDATE_TASK + DELETE_TASK);
    CountDownLatch endingLatch = new CountDownLatch(UPDATE_TASK + DELETE_TASK);
    AtomicReference<RepositoryNode> nodeToUpdate = new AtomicReference<>();
    AtomicReference<RepositoryNode> nodeToDelete = new AtomicReference<>();

    execute(endingLatch, () -> {
      LOGGER.debug("Update task: task started");
      RepositoryNode node = buildNode(departmentName, sourceDate).build();

      LOGGER.debug("Update task: creating node that will be updated");
      createNode(node);
      nodeToUpdate.set(node);

      preparationAssertBarrier.await(10, TimeUnit.SECONDS);
      // Wait for assertion on created nodes taking place in main task
      preparationAssertBarrier.await(10, TimeUnit.SECONDS);

      Map<QName, Serializable> dateProperty = Collections.singletonMap(FilerTestConstants.ImportedAspect.PROP_DATE,
          Date.from(targetDate.atZone(ZoneId.systemDefault()).toInstant()));

      // Wait for every task to be ready for launching parallel task execution
      startingBarrier.await(10, TimeUnit.SECONDS);

      LOGGER.debug("Update task: node creation start");
      updateNode(node, dateProperty);
      LOGGER.debug("Update task: node creation end");
      return null;
    });

    execute(endingLatch, () -> {
      LOGGER.debug("Delete task: task started");
      RepositoryNode node = buildNode(departmentName, targetDate)
          // Do not archive node, this could generate contention on creating user trashcan
          .aspect(ContentModel.ASPECT_TEMPORARY)
          .build();

      LOGGER.debug("Delete task: creating node that will be deleted");
      createNode(node);
      nodeToDelete.set(node);

      preparationAssertBarrier.await(10, TimeUnit.SECONDS);
      // Wait for assertion on created nodes taking place in main task
      preparationAssertBarrier.await(10, TimeUnit.SECONDS);

      // Wait for every task to be ready for launching parallel task execution
      startingBarrier.await(10, TimeUnit.SECONDS);

      LOGGER.debug("Delete task: node deletion start");
      deleteNode(node);
      LOGGER.debug("Delete task: node deletion end");
      return null;
    });

    // Wait for node creation to finish and then assert all nodes are indeed created
    preparationAssertBarrier.await();
    assertThat(getPath(nodeToUpdate.get())).isEqualTo(nodePath(departmentName, sourceDate));
    assertThat(getPath(nodeToDelete.get())).isEqualTo(nodePath(departmentName, targetDate));
    preparationAssertBarrier.await();

    // Wait for every task to finish job before asserting results
    endingLatch.await();

    LOGGER.debug("All tasks are done, starting assertions");

    // Assert all tasks were ready for parallel task execution
    assertThat(startingBarrier.isBroken()).isFalse();

    assertThat(getPath(nodeToUpdate.get())).isEqualTo(nodePath(departmentName, targetDate));

    assertThat(nodeService.exists(nodeToDelete.get().getNodeRef())).isFalse();
    NodeRef nodeToDeleteParent = nodeToDelete.get().getParent();
    if (nodeToDeleteParent.equals(nodeToUpdate.get().getParent())) {
      // Move occurred before deletion and target segment have not been removed
      assertThat(nodeService.exists(nodeToDeleteParent)).isTrue();
      NodeRef nodeToDeleteGrandParent = nodeService.getPrimaryParent(nodeToDeleteParent).getParentRef();
      assertThat(nodeService.exists(nodeToDeleteGrandParent)).isTrue();
    } else {
      // Deletion performed before node updating, filer have recreated new segment for target
      assertThat(nodeService.exists(nodeToDeleteParent)).isFalse();
    }
  }
}
