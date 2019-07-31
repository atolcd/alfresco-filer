package com.atolcd.alfresco.filer.core.policy;

import java.util.Objects;

import org.alfresco.repo.node.NodeServicePolicies;
import org.alfresco.repo.policy.JavaBehaviour;
import org.alfresco.repo.policy.PolicyComponent;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.security.OwnableService;
import org.alfresco.service.namespace.QName;
import org.springframework.beans.factory.InitializingBean;

import com.atolcd.alfresco.filer.core.service.FilerModelService;

public class FilerSegmentAspect implements InitializingBean, NodeServicePolicies.OnAddAspectPolicy {

  private FilerModelService filerModelService;
  private PolicyComponent policyComponent;
  private OwnableService ownableService;

  private String username;

  @Override
  public void afterPropertiesSet() {
    Objects.requireNonNull(filerModelService);
    Objects.requireNonNull(policyComponent);
    Objects.requireNonNull(ownableService);
    Objects.requireNonNull(username);
    policyComponent.bindClassBehaviour(NodeServicePolicies.OnAddAspectPolicy.QNAME,
        filerModelService.getSegmentAspect(), new JavaBehaviour(this, "onAddAspect"));
  }

  @Override
  public void onAddAspect(final NodeRef nodeRef, final QName aspectTypeQName) {
    ownableService.setOwner(nodeRef, username);
  }

  public void setFilerModelService(final FilerModelService filerModelService) {
    this.filerModelService = filerModelService;
  }

  public void setPolicyComponent(final PolicyComponent policyComponent) {
    this.policyComponent = policyComponent;
  }

  public void setOwnableService(final OwnableService ownableService) {
    this.ownableService = ownableService;
  }

  public void setUsername(final String username) {
    this.username = username;
  }
}
