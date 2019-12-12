package com.atolcd.alfresco.filer.core.util;

import java.util.Map;
import java.util.Optional;

import org.alfresco.repo.transaction.TransactionalResourceHelper;
import org.alfresco.service.cmr.repository.NodeRef;

import com.atolcd.alfresco.filer.core.model.FilerEvent;
import com.atolcd.alfresco.filer.core.model.RepositoryNode;
import com.atolcd.alfresco.filer.core.policy.FilerSubscriberAspect;
import com.atolcd.alfresco.filer.core.service.FilerService;
import com.atolcd.alfresco.filer.core.service.FilerUpdateService;

public final class FilerTransactionUtils {

  private static final Class<?> TRANSACTION_EVENT_NODE_KEY = FilerService.class;
  private static final Class<?> TRANSACTION_INITIAL_NODE_KEY = FilerUpdateService.class;
  private static final Class<?> TRANSACTION_DELETED_ASSOC_KEY = FilerSubscriberAspect.class;

  public static Optional<FilerEvent> getEventNode(final NodeRef nodeRef) {
    return Optional.ofNullable(getEventNodeMap().get(nodeRef));
  }

  public static void putEventNode(final NodeRef nodeRef, final FilerEvent event) {
    getEventNodeMap().put(nodeRef, event);
  }

  private static Map<NodeRef, FilerEvent> getEventNodeMap() {
    return TransactionalResourceHelper.getMap(TRANSACTION_EVENT_NODE_KEY);
  }

  public static RepositoryNode getInitialNode(final NodeRef nodeRef) {
    return Optional.ofNullable(getInitialNodeMap().get(nodeRef)).orElse(new RepositoryNode(nodeRef));
  }

  public static void putInitialNode(final NodeRef nodeRef, final RepositoryNode node) {
    getInitialNodeMap().put(nodeRef, node);
  }

  private static Map<NodeRef, RepositoryNode> getInitialNodeMap() {
    return TransactionalResourceHelper.getMap(TRANSACTION_INITIAL_NODE_KEY);
  }

  public static Optional<NodeRef> getDeletedAssoc(final NodeRef childRef) {
    return Optional.ofNullable(getDeletedAssocMap().get(childRef));
  }

  public static void putDeletedAssoc(final NodeRef childRef, final NodeRef parentRef) {
    getDeletedAssocMap().put(childRef, parentRef);
  }

  private static Map<NodeRef, NodeRef> getDeletedAssocMap() {
    return TransactionalResourceHelper.getMap(TRANSACTION_DELETED_ASSOC_KEY);
  }

  private FilerTransactionUtils() {}
}
