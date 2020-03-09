package com.atolcd.alfresco.filer.core.policy;

import org.alfresco.repo.dictionary.DictionaryDAO;
import org.alfresco.repo.node.NodeServicePolicies;
import org.alfresco.repo.policy.JavaBehaviour;
import org.alfresco.repo.policy.PolicyComponent;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.namespace.QName;

import com.atolcd.alfresco.filer.core.service.FilerModelService;
import com.atolcd.alfresco.filer.core.service.impl.DictionaryListenerAspect;

public class FilerSegmentAspect extends DictionaryListenerAspect implements NodeServicePolicies.OnAddAspectPolicy {

  private final PolicyComponent policyComponent;
  private final FilerModelService filerModelService;

  public FilerSegmentAspect(final DictionaryDAO dictionaryDAO, final PolicyComponent policyComponent,
      final FilerModelService filerModelService) {
    super(dictionaryDAO);
    this.policyComponent = policyComponent;
    this.filerModelService = filerModelService;
  }

  @Override
  protected QName getAspect() {
    return filerModelService.getSegmentAspect();
  }

  @Override
  public void init() {
    policyComponent.bindClassBehaviour(NodeServicePolicies.OnAddAspectPolicy.QNAME,
        getAspect(), new JavaBehaviour(this, "onAddAspect"));
  }

  @Override
  public void onAddAspect(final NodeRef nodeRef, final QName aspectTypeQName) {
    filerModelService.setOwner(nodeRef);
  }
}
