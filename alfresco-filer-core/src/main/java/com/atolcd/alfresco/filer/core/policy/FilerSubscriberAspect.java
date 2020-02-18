package com.atolcd.alfresco.filer.core.policy;

import java.util.Objects;
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

import edu.umd.cs.findbugs.annotations.Nullable;

public class FilerSubscriberAspect implements InitializingBean, NodeServicePolicies.BeforeDeleteChildAssociationPolicy,
    NodeServicePolicies.OnCreateChildAssociationPolicy {

  @Nullable
  private FilerService filerService;
  @Nullable
  private FilerModelService filerModelService;
  @Nullable
  private PolicyComponent policyComponent;

  @Override
  public void afterPropertiesSet() {
    Objects.requireNonNull(filerService);
    Objects.requireNonNull(filerModelService);
    Objects.requireNonNull(policyComponent);
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
