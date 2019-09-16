package com.atolcd.alfresco.filer.core.test.framework;

import java.io.Serializable;
import java.util.Map;

import org.alfresco.model.ContentModel;
import org.alfresco.repo.transaction.AlfrescoTransactionSupport;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.security.PermissionService;
import org.alfresco.service.namespace.NamespaceService;
import org.alfresco.service.namespace.QName;
import org.alfresco.service.transaction.TransactionService;
import org.alfresco.util.transaction.TransactionListenerAdapter;
import org.springframework.beans.factory.annotation.Autowired;

import com.atolcd.alfresco.filer.core.model.RepositoryNode;
import com.atolcd.alfresco.filer.core.util.FilerNodeUtils;

@TestDocumentLibrary
public class RepositoryOperations {

  @Autowired
  private TransactionService transactionService;
  @Autowired
  private NodeService nodeService;
  @Autowired
  private PermissionService permissionService;

  protected final void createNode(final RepositoryNode node) {
    doInTransaction(() -> {
      createNodeImpl(node);
    });
  }

  protected void createNodeImpl(final RepositoryNode node) {
    QName assocQName = QName.createQNameWithValidLocalName(NamespaceService.CONTENT_MODEL_1_0_URI, node.getName().get());
    NodeRef nodeRef = nodeService
        .createNode(node.getParent(), ContentModel.ASSOC_CONTAINS, assocQName, node.getType(), node.getProperties())
        .getChildRef();
    node.getAspects().forEach(aspect -> nodeService.addAspect(nodeRef, aspect, null));

    node.setNodeRef(nodeRef);
    bindTransactionListener(node);
  }

  protected final void fetchNode(final RepositoryNode node) {
    doInTransaction(() -> {
      node.setParent(nodeService.getPrimaryParent(node.getNodeRef()).getParentRef());
      node.setType(nodeService.getType(node.getNodeRef()));
      node.getAspects().clear();
      node.getAspects().addAll(nodeService.getAspects(node.getNodeRef()));
      node.getProperties().clear();
      node.getProperties().putAll(nodeService.getProperties(node.getNodeRef()));
      FilerNodeUtils.setDisplayPath(node, nodeService.getPath(node.getNodeRef()).toDisplayPath(nodeService, permissionService));
    }, true);
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

  protected static String getPath(final RepositoryNode node) {
    return FilerNodeUtils.getDisplayPath(node);
  }

  private void bindTransactionListener(final RepositoryNode node) {
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

  protected RepositoryOperations() {
    // Prevent class instantiation
  }
}
