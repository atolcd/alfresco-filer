package com.atolcd.alfresco.filer.core.policy;

import java.io.Serializable;
import java.util.Map;
import java.util.Objects;

import org.alfresco.repo.node.NodeServicePolicies;
import org.alfresco.repo.policy.Behaviour.NotificationFrequency;
import org.alfresco.repo.policy.JavaBehaviour;
import org.alfresco.repo.policy.PolicyComponent;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.service.cmr.repository.ChildAssociationRef;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.security.OwnableService;
import org.alfresco.service.namespace.QName;
import org.springframework.beans.factory.InitializingBean;

import com.atolcd.alfresco.filer.core.model.FilerEvent;
import com.atolcd.alfresco.filer.core.model.InboundFilerEvent;
import com.atolcd.alfresco.filer.core.model.UpdateFilerEvent;
import com.atolcd.alfresco.filer.core.service.FilerModelService;
import com.atolcd.alfresco.filer.core.service.FilerService;
import com.atolcd.alfresco.filer.core.util.FilerTransactionUtils;

import edu.umd.cs.findbugs.annotations.Nullable;

public class FileableAspect implements InitializingBean, NodeServicePolicies.OnCreateNodePolicy,
    NodeServicePolicies.OnAddAspectPolicy, NodeServicePolicies.BeforeUpdateNodePolicy,
    NodeServicePolicies.OnUpdateNodePolicy, NodeServicePolicies.OnUpdatePropertiesPolicy,
    NodeServicePolicies.OnDeleteNodePolicy, NodeServicePolicies.OnMoveNodePolicy {

  @Nullable
  private FilerService filerService;
  @Nullable
  private FilerModelService filerModelService;
  @Nullable
  private PolicyComponent policyComponent;
  @Nullable
  private NodeService nodeService;
  @Nullable
  private OwnableService ownableService;

  @Nullable
  private String username;

  @Override
  public void afterPropertiesSet() {
    Objects.requireNonNull(filerService);
    Objects.requireNonNull(filerModelService);
    Objects.requireNonNull(policyComponent);
    Objects.requireNonNull(ownableService);
    Objects.requireNonNull(username);
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
      if (nodeService.exists(nodeRef)) {
        ownableService.setOwner(nodeRef, username);
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

  public void setFilerService(final FilerService filerService) {
    this.filerService = filerService;
  }

  public void setFilerModelService(final FilerModelService filerModelService) {
    this.filerModelService = filerModelService;
  }

  public void setPolicyComponent(final PolicyComponent policyComponent) {
    this.policyComponent = policyComponent;
  }

  public void setNodeService(final NodeService nodeService) {
    this.nodeService = nodeService;
  }

  public void setOwnableService(final OwnableService ownableService) {
    this.ownableService = ownableService;
  }

  public void setUsername(final String username) {
    this.username = username;
  }
}
