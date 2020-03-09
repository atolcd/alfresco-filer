package com.atolcd.alfresco.filer.core.policy;

import java.util.Optional;

import org.alfresco.repo.dictionary.DictionaryDAO;
import org.alfresco.repo.node.NodeServicePolicies;
import org.alfresco.repo.policy.JavaBehaviour;
import org.alfresco.repo.policy.PolicyComponent;
import org.alfresco.service.cmr.repository.ChildAssociationRef;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.namespace.QName;

import com.atolcd.alfresco.filer.core.model.InboundFilerEvent;
import com.atolcd.alfresco.filer.core.service.FilerModelService;
import com.atolcd.alfresco.filer.core.service.FilerService;
import com.atolcd.alfresco.filer.core.service.impl.DictionaryListenerAspect;
import com.atolcd.alfresco.filer.core.util.FilerTransactionUtils;

public class FilerSubscriberAspect extends DictionaryListenerAspect
    implements NodeServicePolicies.BeforeDeleteChildAssociationPolicy, NodeServicePolicies.OnCreateChildAssociationPolicy {

  private final PolicyComponent policyComponent;
  private final FilerModelService filerModelService;
  private final FilerService filerService;

  public FilerSubscriberAspect(final DictionaryDAO dictionaryDAO, final PolicyComponent policyComponent,
      final FilerModelService filerModelService, final FilerService filerService) {
    super(dictionaryDAO);
    this.policyComponent = policyComponent;
    this.filerModelService = filerModelService;
    this.filerService = filerService;
  }

  @Override
  protected QName getAspect() {
    return filerModelService.getSubscriberAspect();
  }

  @Override
  public void init() {
    policyComponent.bindAssociationBehaviour(NodeServicePolicies.BeforeDeleteChildAssociationPolicy.QNAME,
        getAspect(), new JavaBehaviour(this, "beforeDeleteChildAssociation"));
    policyComponent.bindAssociationBehaviour(NodeServicePolicies.OnCreateChildAssociationPolicy.QNAME,
        getAspect(), new JavaBehaviour(this, "onCreateChildAssociation"));
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
