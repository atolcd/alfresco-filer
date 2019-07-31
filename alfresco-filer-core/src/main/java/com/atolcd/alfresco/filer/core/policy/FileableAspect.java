package com.atolcd.alfresco.filer.core.policy;

import java.io.Serializable;
import java.util.Map;
import java.util.Objects;

import org.alfresco.repo.node.NodeServicePolicies;
import org.alfresco.repo.policy.Behaviour.NotificationFrequency;
import org.alfresco.repo.policy.JavaBehaviour;
import org.alfresco.repo.policy.PolicyComponent;
import org.alfresco.service.cmr.repository.ChildAssociationRef;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.namespace.QName;
import org.springframework.beans.factory.InitializingBean;

import com.atolcd.alfresco.filer.core.model.InboundFilerEvent;
import com.atolcd.alfresco.filer.core.model.UpdateFilerEvent;
import com.atolcd.alfresco.filer.core.service.FilerModelService;
import com.atolcd.alfresco.filer.core.service.FilerService;

public class FileableAspect implements InitializingBean, NodeServicePolicies.OnAddAspectPolicy,
    NodeServicePolicies.BeforeUpdateNodePolicy, NodeServicePolicies.OnUpdatePropertiesPolicy,
    NodeServicePolicies.OnDeleteNodePolicy, NodeServicePolicies.OnMoveNodePolicy {

  private FilerService filerService;
  private FilerModelService filerModelService;
  private PolicyComponent policyComponent;

  @Override
  public void afterPropertiesSet() {
    Objects.requireNonNull(filerService);
    Objects.requireNonNull(filerModelService);
    Objects.requireNonNull(policyComponent);
    QName fileableAspect = filerModelService.getFileableAspect();
    // Using TRANSACTION_COMMIT ensures all properties are added/updated before applying filer
    policyComponent.bindClassBehaviour(NodeServicePolicies.OnAddAspectPolicy.QNAME,
        fileableAspect, new JavaBehaviour(this, "onAddAspect", NotificationFrequency.TRANSACTION_COMMIT));
    policyComponent.bindClassBehaviour(NodeServicePolicies.BeforeUpdateNodePolicy.QNAME,
        fileableAspect, new JavaBehaviour(this, "beforeUpdateNode", NotificationFrequency.FIRST_EVENT));
    policyComponent.bindClassBehaviour(NodeServicePolicies.OnUpdatePropertiesPolicy.QNAME,
        fileableAspect, new JavaBehaviour(this, "onUpdateProperties", NotificationFrequency.TRANSACTION_COMMIT));
    policyComponent.bindClassBehaviour(NodeServicePolicies.OnMoveNodePolicy.QNAME,
        fileableAspect, new JavaBehaviour(this, "onMoveNode", NotificationFrequency.TRANSACTION_COMMIT));
    policyComponent.bindClassBehaviour(NodeServicePolicies.OnDeleteNodePolicy.QNAME,
        fileableAspect, new JavaBehaviour(this, "onDeleteNode"));
  }

  @Override
  public void onAddAspect(final NodeRef nodeRef, final QName aspectTypeQName) {
    filerService.executeAction(new InboundFilerEvent(nodeRef, false));
  }

  @Override
  public void beforeUpdateNode(final NodeRef nodeRef) {
    filerService.initFileable(nodeRef);
  }

  @Override
  public void onUpdateProperties(final NodeRef nodeRef,
      final Map<QName, Serializable> before, final Map<QName, Serializable> after) {
    // Avoid getting triggered on initial node creation
    if (!before.isEmpty()) {
      UpdateFilerEvent event = new UpdateFilerEvent(nodeRef, after);
      filerService.executeAction(event);
    }
  }

  @Override
  public void onMoveNode(final ChildAssociationRef oldChildAssocRef, final ChildAssociationRef newChildAssocRef) {
    // In a try-catch just in case, so that old parent segment can be deleted
    try {
      filerService.executeAction(new InboundFilerEvent(newChildAssocRef.getChildRef(), false));
    } finally {
      filerService.operations().deleteSegment(oldChildAssocRef.getParentRef());
    }
  }

  @Override
  public void onDeleteNode(final ChildAssociationRef childAssocRef, final boolean isNodeArchived) {
    NodeRef parent = childAssocRef.getParentRef();
    filerService.operations().deleteSegment(parent);
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
}
