package com.atolcd.alfresco.filer.core.policy;

import org.alfresco.repo.node.NodeServicePolicies;
import org.alfresco.repo.policy.JavaBehaviour;
import org.alfresco.repo.policy.PolicyComponent;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.namespace.QName;
import org.springframework.beans.factory.InitializingBean;

import com.atolcd.alfresco.filer.core.service.FilerModelService;

public class FilerSegmentAspect implements InitializingBean, NodeServicePolicies.OnAddAspectPolicy {

  private final FilerModelService filerModelService;
  private final PolicyComponent policyComponent;

  public FilerSegmentAspect(final FilerModelService filerModelService, final PolicyComponent policyComponent) {
    this.filerModelService = filerModelService;
    this.policyComponent = policyComponent;
  }

  @Override
  public void afterPropertiesSet() {
    policyComponent.bindClassBehaviour(NodeServicePolicies.OnAddAspectPolicy.QNAME,
        filerModelService.getSegmentAspect(), new JavaBehaviour(this, "onAddAspect"));
  }

  @Override
  public void onAddAspect(final NodeRef nodeRef, final QName aspectTypeQName) {
    filerModelService.setOwner(nodeRef);
  }
}
