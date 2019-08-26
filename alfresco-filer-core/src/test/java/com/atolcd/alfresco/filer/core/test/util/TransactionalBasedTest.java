package com.atolcd.alfresco.filer.core.test.util;

import java.io.Serializable;
import java.util.Map;

import org.alfresco.model.ContentModel;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.repo.transaction.AlfrescoTransactionSupport;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.namespace.NamespaceService;
import org.alfresco.service.namespace.QName;
import org.alfresco.service.transaction.TransactionService;
import org.alfresco.util.transaction.TransactionListenerAdapter;
import org.junit.jupiter.api.AfterEach;
import org.springframework.beans.factory.annotation.Autowired;

import com.atolcd.alfresco.filer.core.model.RepositoryNode;

public class TransactionalBasedTest implements ApplicationContextAwareTest {

  @Autowired
  private TransactionService transactionService;
  @Autowired
  private NodeService nodeService;

  @AfterEach
  public void clearAuthorization() {
    AuthenticationUtil.clearCurrentSecurityContext();
  }

  protected final void createNode(final RepositoryNode node) {
    doInTransaction(() -> {
      QName assocQName = QName.createQNameWithValidLocalName(NamespaceService.CONTENT_MODEL_1_0_URI, node.getName().get());
      NodeRef nodeRef = nodeService
          .createNode(node.getParent(), ContentModel.ASSOC_CONTAINS, assocQName, node.getType(), node.getProperties())
          .getChildRef();
      node.getAspects().forEach(aspect -> nodeService.addAspect(nodeRef, aspect, null));

      node.setNodeRef(nodeRef);
      bindTransactionListener(node);
    });
  }

  protected final void fetchNode(final RepositoryNode node) {
    doInTransaction(() -> {
      fetchNodeImpl(node);
    }, true);
  }

  protected void fetchNodeImpl(final RepositoryNode node) {
    node.setParent(nodeService.getPrimaryParent(node.getNodeRef()).getParentRef());
    node.setType(nodeService.getType(node.getNodeRef()));
    node.getAspects().clear();
    node.getAspects().addAll(nodeService.getAspects(node.getNodeRef()));
    node.getProperties().clear();
    node.getProperties().putAll(nodeService.getProperties(node.getNodeRef()));
  }

  protected final void updateNode(final RepositoryNode node, final Map<QName, Serializable> properties) {
    doInTransaction(() -> {
      nodeService.addProperties(node.getNodeRef(), properties);

      bindTransactionListener(node);
    });
  }

  protected final void deleteNode(final RepositoryNode node) {
    doInTransaction(() -> {
      nodeService.deleteNode(node.getNodeRef());
    });
  }

  protected final void bindTransactionListener(final RepositoryNode node) {
    AlfrescoTransactionSupport.bindListener(new TransactionListenerAdapter() {
      @Override
      public void afterCommit() {
        fetchNode(node);
      }
    });
  }

  protected <T> void doInTransaction(final Runnable callback) {
    doInTransaction(callback, false);
  }

  protected <T> void doInTransaction(final Runnable callback, final boolean readOnly) {
    transactionService.getRetryingTransactionHelper().doInTransaction(() -> {
      callback.run();
      return null;
    }, readOnly);
  }
}
