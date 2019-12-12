package com.atolcd.alfresco.filer.core.model;

import java.io.Serializable;
import java.text.MessageFormat;
import java.util.Map;

import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.namespace.QName;

import com.atolcd.alfresco.filer.core.model.impl.AbstractFilerEvent;

public class UpdateFilerEvent extends AbstractFilerEvent {

  public UpdateFilerEvent(final NodeRef nodeRef, final Map<QName, Serializable> after) {
    super(nodeRef);
    // Set node properties values
    getNode().getProperties().putAll(after);
  }

  @Override
  public String toString() {
    return MessageFormat.format("Update'{'action={0}, name={1}, node={2}, store={3}'}'",
        getAction().orElse(null),
        getNode().getName().orElse(null),
        getNode().getNodeRef().getId(),
        getNode().getNodeRef().getStoreRef());
  }
}
