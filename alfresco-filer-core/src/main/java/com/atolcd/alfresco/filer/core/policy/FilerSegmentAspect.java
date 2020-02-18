package com.atolcd.alfresco.filer.core.policy;

import org.alfresco.repo.node.NodeServicePolicies;
import org.alfresco.repo.policy.JavaBehaviour;
import org.alfresco.repo.policy.PolicyComponent;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.security.OwnableService;
import org.alfresco.service.namespace.QName;
import org.springframework.beans.factory.InitializingBean;

import com.atolcd.alfresco.filer.core.service.FilerModelService;

public class FilerSegmentAspect implements InitializingBean, NodeServicePolicies.OnAddAspectPolicy {

  private final FilerModelService filerModelService;
  private final PolicyComponent policyComponent;
  private final OwnableService ownableService;

  private final String username;

  public FilerSegmentAspect(final FilerModelService filerModelService, final PolicyComponent policyComponent,
      final OwnableService ownableService, final String username) {
    this.filerModelService = filerModelService;
    this.policyComponent = policyComponent;
    this.ownableService = ownableService;
    this.username = username;
  }

  @Override
  public void afterPropertiesSet() {
    policyComponent.bindClassBehaviour(NodeServicePolicies.OnAddAspectPolicy.QNAME,
        filerModelService.getSegmentAspect(), new JavaBehaviour(this, "onAddAspect"));
  }

  @Override
  public void onAddAspect(final NodeRef nodeRef, final QName aspectTypeQName) {
    ownableService.setOwner(nodeRef, username);
  }
}
