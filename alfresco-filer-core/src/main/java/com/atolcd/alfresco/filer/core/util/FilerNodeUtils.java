package com.atolcd.alfresco.filer.core.util;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;

import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.site.SiteInfo;

import com.atolcd.alfresco.filer.core.model.FilerException;
import com.atolcd.alfresco.filer.core.model.RepositoryNode;

import edu.umd.cs.findbugs.annotations.CheckForNull;

public final class FilerNodeUtils {

  private static final String PATH_KEY = "path";
  private static final String SITE_INFO_KEY = "siteInfo";
  private static final String ORIGINAL_KEY = "original";
  private static final String ORIGINAL_NODE_KEY = "originalNode";

  public static Optional<SiteInfo> getSiteInfo(final RepositoryNode node) {
    return node.getExtension(SITE_INFO_KEY, SiteInfo.class);
  }

  public static void setSiteInfo(final RepositoryNode node, final @CheckForNull SiteInfo siteInfo) {
    if (siteInfo != null) {
      node.getExtensions().put(SITE_INFO_KEY, siteInfo);
    }
  }

  /**
   * Function to return the nodeRef of the node's site that can be used in a method reference: FilerNodeUtils::getSiteNodeRef
   */
  public static NodeRef getSiteNodeRef(final RepositoryNode node) {
    return getSiteInfo(node).map(SiteInfo::getNodeRef)
        .orElseThrow(() -> new FilerException("Could not get the site of the node: " + node));
  }

  public static Boolean isOriginal(final RepositoryNode node) {
    return node.getExtension(ORIGINAL_KEY, Boolean.class).orElse(Boolean.FALSE);
  }

  public static void setOriginal(final RepositoryNode node, final Boolean original) {
    if (Boolean.TRUE.equals(original)) {
      node.getExtensions().put(ORIGINAL_KEY, original);
    }
  }

  public static RepositoryNode getOriginalNode(final RepositoryNode node) {
    return node.getExtension(ORIGINAL_NODE_KEY, RepositoryNode.class).get();
  }

  public static void setOriginalNode(final RepositoryNode node, final RepositoryNode originalNode) {
    node.getExtensions().put(ORIGINAL_NODE_KEY, originalNode);
  }

  public static Path getPath(final RepositoryNode node) {
    return node.getExtension(PATH_KEY, Path.class).get();
  }

  public static void setPath(final RepositoryNode node, final String path) {
    node.getExtensions().put(PATH_KEY, Paths.get(path));
  }

  private FilerNodeUtils() {}
}
