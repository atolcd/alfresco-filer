package com.atolcd.alfresco.filer.core.util;

import java.util.Map;
import java.util.Optional;

import org.alfresco.repo.transaction.AlfrescoTransactionSupport;
import org.alfresco.repo.transaction.TransactionalResourceHelper;
import org.alfresco.service.cmr.repository.NodeRef;

import com.atolcd.alfresco.filer.core.model.FilerEvent;
import com.atolcd.alfresco.filer.core.model.RepositoryNode;
import com.atolcd.alfresco.filer.core.policy.FileableAspect;
import com.atolcd.alfresco.filer.core.policy.FilerSubscriberAspect;
import com.atolcd.alfresco.filer.core.service.FilerModelService;
import com.atolcd.alfresco.filer.core.service.FilerService;
import com.atolcd.alfresco.filer.core.service.FilerUpdateService;

import edu.umd.cs.findbugs.annotations.CheckForNull;

public final class FilerTransactionUtils {

  private static final Class<?> TRANSACTION_EVENT_NODE_KEY = FilerService.class;
  private static final Class<?> TRANSACTION_INITIAL_NODE_KEY = FilerUpdateService.class;
  private static final Class<?> TRANSACTION_UPDATE_USER_KEY = FileableAspect.class;
  private static final Class<?> TRANSACTION_GLOBAL_USER_KEY = FilerModelService.class;
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

  public static String getUpdateUser(final NodeRef nodeRef) {
    String user = getGlobalUser();
    if (user == null) {
      user = Optional.ofNullable(getUpdateUserMap().get(nodeRef)).orElseThrow(IllegalStateException::new);
    }
    return user;
  }

  public static void putUpdateUser(final NodeRef nodeRef, final String updateUser) {
    getUpdateUserMap().put(nodeRef, updateUser);
  }

  private static Map<NodeRef, String> getUpdateUserMap() {
    return TransactionalResourceHelper.getMap(TRANSACTION_UPDATE_USER_KEY);
  }

  @CheckForNull
  private static String getGlobalUser() {
    return AlfrescoTransactionSupport.getResource(TRANSACTION_GLOBAL_USER_KEY);
  }

  public static void setGlobalUser(@CheckForNull final String globalUser) {
    AlfrescoTransactionSupport.bindResource(TRANSACTION_GLOBAL_USER_KEY, globalUser);
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
