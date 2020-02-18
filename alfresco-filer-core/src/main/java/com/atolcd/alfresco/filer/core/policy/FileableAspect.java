package com.atolcd.alfresco.filer.core.policy;

import java.io.Serializable;
import java.util.Map;

import org.alfresco.repo.node.NodeServicePolicies;
import org.alfresco.repo.policy.Behaviour.NotificationFrequency;
import org.alfresco.repo.policy.JavaBehaviour;
import org.alfresco.repo.policy.PolicyComponent;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.service.cmr.repository.ChildAssociationRef;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.namespace.QName;
import org.springframework.beans.factory.InitializingBean;

import com.atolcd.alfresco.filer.core.model.FilerEvent;
import com.atolcd.alfresco.filer.core.model.InboundFilerEvent;
import com.atolcd.alfresco.filer.core.model.UpdateFilerEvent;
import com.atolcd.alfresco.filer.core.service.FilerModelService;
import com.atolcd.alfresco.filer.core.service.FilerService;
import com.atolcd.alfresco.filer.core.util.FilerTransactionUtils;

public class FileableAspect implements InitializingBean, NodeServicePolicies.OnCreateNodePolicy,
    NodeServicePolicies.OnAddAspectPolicy, NodeServicePolicies.BeforeUpdateNodePolicy,
    NodeServicePolicies.OnUpdateNodePolicy, NodeServicePolicies.OnUpdatePropertiesPolicy,
    NodeServicePolicies.OnDeleteNodePolicy, NodeServicePolicies.OnMoveNodePolicy {

  private final FilerService filerService;
  private final FilerModelService filerModelService;
  private final PolicyComponent policyComponent;
  private final NodeService nodeService;

  public FileableAspect(final FilerService filerService, final FilerModelService filerModelService,
      final PolicyComponent policyComponent, final NodeService nodeService) {
    this.filerService = filerService;
    this.filerModelService = filerModelService;
    this.policyComponent = policyComponent;
    this.nodeService = nodeService;
  }

  @Override
  public void afterPropertiesSet() {
    QName fileableAspect = filerModelService.getFileableAspect();
    policyComponent.bindClassBehaviour(NodeServicePolicies.OnCreateNodePolicy.QNAME,
        fileableAspect, new JavaBehaviour(this, "onCreateNode"));
    // Using TRANSACTION_COMMIT ensures all properties are added/updated before applying filer
    policyComponent.bindClassBehaviour(NodeServicePolicies.OnAddAspectPolicy.QNAME,
        fileableAspect, new JavaBehaviour(this, "onAddAspect", NotificationFrequency.TRANSACTION_COMMIT));
    policyComponent.bindClassBehaviour(NodeServicePolicies.BeforeUpdateNodePolicy.QNAME,
        fileableAspect, new JavaBehaviour(this, "beforeUpdateNode", NotificationFrequency.FIRST_EVENT));
    policyComponent.bindClassBehaviour(NodeServicePolicies.OnUpdateNodePolicy.QNAME,
        fileableAspect, new JavaBehaviour(this, "onUpdateNode"));
    policyComponent.bindClassBehaviour(NodeServicePolicies.OnUpdatePropertiesPolicy.QNAME,
        fileableAspect, new JavaBehaviour(this, "onUpdateProperties", NotificationFrequency.TRANSACTION_COMMIT));
    policyComponent.bindClassBehaviour(NodeServicePolicies.OnMoveNodePolicy.QNAME,
        fileableAspect, new JavaBehaviour(this, "onMoveNode", NotificationFrequency.TRANSACTION_COMMIT));
    policyComponent.bindClassBehaviour(NodeServicePolicies.OnDeleteNodePolicy.QNAME,
        fileableAspect, new JavaBehaviour(this, "onDeleteNode"));
  }

  /**
   * Keep runAsUser in a transactionnal resource.
   * It will be used by the policy at TRANSACTION_COMMIT because the authentication might not be available at that time.
   * E.g. work trigerring the filer is done using {@link AuthenticationUtil#getRunAsUser}: at transaction commit,
   * authentication info are not available anymore.
   */
  @Override
  public void onCreateNode(final ChildAssociationRef childAssocRef) {
    FilerTransactionUtils.putUpdateUser(childAssocRef.getChildRef(), AuthenticationUtil.getRunAsUser());
  }

  @Override
  public void onAddAspect(final NodeRef nodeRef, final QName aspectTypeQName) {
    String user = FilerTransactionUtils.getUpdateUser(nodeRef);
    AuthenticationUtil.runAs(() -> {
      // Check node still exists, it might be gone at transaction commit time
      if (nodeService.exists(nodeRef)) {
        filerModelService.setOwner(nodeRef);
      }
      return null;
    }, user);

    executeAction(new InboundFilerEvent(nodeRef, false));
  }

  @Override
  public void beforeUpdateNode(final NodeRef nodeRef) {
    // Check for authentication as the policy might be triggered while unauthenticated,
    // @see DbNodeServiceImpl.AuditableTransactionListener#afterCommit
    if (AuthenticationUtil.getRunAsAuthentication() != null) {
      filerService.initFileable(nodeRef);
    }
  }

  /**
   * @see #onCreateNode
   */
  @Override
  public void onUpdateNode(final NodeRef nodeRef) {
    FilerTransactionUtils.putUpdateUser(nodeRef, AuthenticationUtil.getRunAsUser());
  }

  @Override
  public void onUpdateProperties(final NodeRef nodeRef,
      final Map<QName, Serializable> before, final Map<QName, Serializable> after) {
    // Avoid getting triggered on initial node creation
    if (!before.isEmpty()) {
      UpdateFilerEvent event = new UpdateFilerEvent(nodeRef, after);
      executeAction(event);
    }
  }

  @Override
  public void onMoveNode(final ChildAssociationRef oldChildAssocRef, final ChildAssociationRef newChildAssocRef) {
    // In a try-catch just in case, so that old parent segment can be deleted
    try {
      executeAction(new InboundFilerEvent(newChildAssocRef.getChildRef(), false));
    } finally {
      filerService.operations().deleteSegment(oldChildAssocRef.getParentRef());
    }
  }

  @Override
  public void onDeleteNode(final ChildAssociationRef childAssocRef, final boolean isNodeArchived) {
    NodeRef parent = childAssocRef.getParentRef();
    filerService.operations().deleteSegment(parent);
  }

  private void executeAction(final FilerEvent event) { //NOPMD UnusedPrivateMethod - False positive
    String user = FilerTransactionUtils.getUpdateUser(event.getNode().getNodeRef().get());
    AuthenticationUtil.runAs(() -> {
      filerService.executeAction(event);
      return null;
    }, user);
  }
}
