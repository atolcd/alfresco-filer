package com.atolcd.alfresco.filer.core.test.framework;

import java.io.Serializable;
import java.util.Map;

import org.alfresco.model.ContentModel;
import org.alfresco.repo.site.SiteModel;
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

@TestApplicationContext
@TestLibrary
@TestAuthentication
@TestLibraryRole(SiteModel.SITE_CONTRIBUTOR)
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
        .createNode(node.getParent().get(), ContentModel.ASSOC_CONTAINS, assocQName, node.getType().get(), node.getProperties())
        .getChildRef();
    node.getAspects().forEach(aspect -> nodeService.addAspect(nodeRef, aspect, null));

    node.setNodeRef(nodeRef);
    bindTransactionListener(node);
  }

  protected final void fetchNode(final RepositoryNode node) {
    doInTransaction(() -> {
      node.setParent(nodeService.getPrimaryParent(node.getNodeRef().get()).getParentRef());
      node.setType(nodeService.getType(node.getNodeRef().get()));
      node.getAspects().clear();
      node.getAspects().addAll(nodeService.getAspects(node.getNodeRef().get()));
      node.getProperties().clear();
      node.getProperties().putAll(nodeService.getProperties(node.getNodeRef().get()));
      FilerNodeUtils.setPath(node, nodeService.getPath(node.getNodeRef().get()).toDisplayPath(nodeService, permissionService));
    }, true);
  }

  protected final void updateNode(final RepositoryNode node, final Map<QName, Serializable> properties) {
    doInTransaction(() -> {
      nodeService.addProperties(node.getNodeRef().get(), properties);

      bindTransactionListener(node);
    });
  }

  protected final void deleteNode(final RepositoryNode node) {
    doInTransaction(() -> {
      nodeService.deleteNode(node.getNodeRef().get());
    });
  }

  private void bindTransactionListener(final RepositoryNode node) {
    AlfrescoTransactionSupport.bindListener(new TransactionListenerAdapter() {
      @Override
      public void afterCommit() {
        fetchNode(node);
      }
    });
  }

  protected void doInTransaction(final Runnable callback) {
    doInTransaction(callback, false);
  }

  protected void doInTransaction(final Runnable callback, final boolean readOnly) {
    transactionService.getRetryingTransactionHelper().doInTransaction(() -> {
      callback.run();
      return null;
    }, readOnly);
  }

  protected RepositoryOperations() {
    // Prevent class instantiation
  }
}
