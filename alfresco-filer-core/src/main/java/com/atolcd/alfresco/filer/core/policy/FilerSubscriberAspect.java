package com.atolcd.alfresco.filer.core.policy;

import java.util.Optional;

import org.alfresco.repo.node.NodeServicePolicies;
import org.alfresco.repo.policy.JavaBehaviour;
import org.alfresco.repo.policy.PolicyComponent;
import org.alfresco.service.cmr.repository.ChildAssociationRef;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.namespace.QName;
import org.springframework.beans.factory.InitializingBean;

import com.atolcd.alfresco.filer.core.model.InboundFilerEvent;
import com.atolcd.alfresco.filer.core.service.FilerModelService;
import com.atolcd.alfresco.filer.core.service.FilerService;
import com.atolcd.alfresco.filer.core.util.FilerTransactionUtils;

public class FilerSubscriberAspect implements InitializingBean, NodeServicePolicies.BeforeDeleteChildAssociationPolicy,
    NodeServicePolicies.OnCreateChildAssociationPolicy {

  private final FilerService filerService;
  private final FilerModelService filerModelService;
  private final PolicyComponent policyComponent;

  public FilerSubscriberAspect(final FilerService filerService, final FilerModelService filerModelService,
      final PolicyComponent policyComponent) {
    this.filerService = filerService;
    this.filerModelService = filerModelService;
    this.policyComponent = policyComponent;
  }

  @Override
  public void afterPropertiesSet() {
    QName subscriberAspect = filerModelService.getSubscriberAspect();
    policyComponent.bindAssociationBehaviour(NodeServicePolicies.BeforeDeleteChildAssociationPolicy.QNAME,
        subscriberAspect, new JavaBehaviour(this, "beforeDeleteChildAssociation"));
    policyComponent.bindAssociationBehaviour(NodeServicePolicies.OnCreateChildAssociationPolicy.QNAME,
        subscriberAspect, new JavaBehaviour(this, "onCreateChildAssociation"));
  }

  @Override
  public void beforeDeleteChildAssociation(final ChildAssociationRef childAssocRef) {
    if (childAssocRef.isPrimary()) {
      // Store previous parent to determine later if it is a move on the same parent
      FilerTransactionUtils.putDeletedAssoc(childAssocRef.getChildRef(), childAssocRef.getParentRef());
    }
  }

  @Override
  public void onCreateChildAssociation(final ChildAssociationRef childAssocRef, final boolean isNewNode) {
    // Ignore node rename, it is not a move nor a creation, the node was there before
    if (childAssocRef.isPrimary() && !isRename(childAssocRef)) {
      // Renaming a Segment triggers this policy
      filerService.resolveFileable(new InboundFilerEvent(childAssocRef.getChildRef(), isNewNode));
    }
  }

  private boolean isRename(final ChildAssociationRef childAssocRef) {
    Optional<NodeRef> oldParent = FilerTransactionUtils.getDeletedAssoc(childAssocRef.getChildRef());
    // It is in fact a rename if a previous deletion of the child happened on the same parent
    return oldParent.isPresent() && oldParent.get().equals(childAssocRef.getParentRef());
  }
}
