package com.atolcd.alfresco.filer.core.util;

import java.util.Objects;
import java.util.Optional;

import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.site.SiteInfo;

import com.atolcd.alfresco.filer.core.model.RepositoryNode;

public final class FilerNodeUtils {

  private static final String DISPLAY_PATH_KEY = "displayPath";
  private static final String SITE_INFO_KEY = "siteInfo";
  private static final String ORIGINAL_KEY = "original";
  private static final String ORIGINAL_NODE_KEY = "originalNode";

  public static Optional<SiteInfo> getSiteInfo(final RepositoryNode node) {
    return Optional.ofNullable(node.getExtension(SITE_INFO_KEY, SiteInfo.class));
  }

  public static void setSiteInfo(final RepositoryNode node, final SiteInfo siteInfo) {
    if (siteInfo != null) {
      node.getExtensions().put(SITE_INFO_KEY, siteInfo);
    }
  }

  public static NodeRef getSiteNodeRef(final RepositoryNode node) {
    return getSiteInfo(node).get().getNodeRef();
  }

  public static Boolean isOriginal(final RepositoryNode node) {
    return Optional.ofNullable(node.getExtension(ORIGINAL_KEY, Boolean.class)).orElse(Boolean.FALSE);
  }

  public static void setOriginal(final RepositoryNode node, final Boolean original) {
    if (Boolean.TRUE.equals(original)) {
      node.getExtensions().put(ORIGINAL_KEY, original);
    }
  }

  public static RepositoryNode getOriginalNode(final RepositoryNode node) {
    RepositoryNode originalNode = node.getExtension(ORIGINAL_NODE_KEY, RepositoryNode.class);
    Objects.requireNonNull(originalNode);
    return originalNode;
  }

  public static void setOriginalNode(final RepositoryNode node, final RepositoryNode originalNode) {
    node.getExtensions().put(ORIGINAL_NODE_KEY, originalNode);
  }

  public static String getDisplayPath(final RepositoryNode node) {
    return Optional.ofNullable(node.getExtension(DISPLAY_PATH_KEY, String.class)).orElse("");
  }

  public static void setDisplayPath(final RepositoryNode node, final String path) {
    node.getExtensions().put(DISPLAY_PATH_KEY, path);
  }

  private FilerNodeUtils() {}
}
