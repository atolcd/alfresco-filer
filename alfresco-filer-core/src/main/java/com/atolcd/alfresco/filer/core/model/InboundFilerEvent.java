package com.atolcd.alfresco.filer.core.model;

import java.text.MessageFormat;

import org.alfresco.service.cmr.repository.NodeRef;

import com.atolcd.alfresco.filer.core.model.impl.AbstractFilerEvent;
import com.atolcd.alfresco.filer.core.util.FilerNodeUtils;

public class InboundFilerEvent extends AbstractFilerEvent {

  public InboundFilerEvent(final NodeRef nodeRef, final boolean original) {
    super(nodeRef);
    FilerNodeUtils.setOriginal(getNode(), original);
  }

  @Override
  public String toString() {
    return MessageFormat.format("Inbound'{'action={0}, name={1}, node={2}, store={3}'}'",
        getAction().orElse(null),
        getNode().getName().orElse(null),
        getNode().getNodeRef().getId(),
        getNode().getNodeRef().getStoreRef());
  }
}
