package com.atolcd.alfresco.filer.core.test.framework.util;

import java.util.UUID;

import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.StoreRef;

public final class NodeRefUtils {

  public static NodeRef randomNodeRef() {
    return randomWorkspaceSpacesStoreNodeRef();
  }

  public static NodeRef randomWorkspaceSpacesStoreNodeRef() {
    return new NodeRef(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE, UUID.randomUUID().toString());
  }

  public static NodeRef randomArchiveSpacesStoreNodeRef() {
    return new NodeRef(StoreRef.STORE_REF_ARCHIVE_SPACESSTORE, UUID.randomUUID().toString());
  }

  private NodeRefUtils() {}
}
