package com.atolcd.alfresco.filer.core.test.framework;

import static java.util.UUID.randomUUID;

import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import javax.annotation.PostConstruct;

import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.repo.site.SiteServiceImpl;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.security.PermissionService;
import org.alfresco.service.cmr.site.SiteService;
import org.alfresco.service.cmr.site.SiteVisibility;
import org.alfresco.service.cmr.tagging.TaggingService;
import org.alfresco.service.transaction.TransactionService;
import org.springframework.beans.factory.annotation.Autowired;

import com.atolcd.alfresco.filer.core.model.RepositoryNode;
import com.atolcd.alfresco.filer.core.model.impl.RepositoryNodeBuilder;

public abstract class SiteBasedTest extends TransactionalBasedTest {

  private static final String NODE_PATH = SiteBasedTest.class + ".NODE_PATH";

  @Autowired
  private TransactionService transactionService;
  @Autowired
  private NodeService nodeService;
  @Autowired
  private PermissionService permissionService;
  @Autowired
  private SiteService siteService;
  @Autowired
  private TaggingService taggingService;

  private NodeRef documentLibrary;

  @PostConstruct
  private void initSite() {
    String siteName = getSiteName();

    doInTransaction(() -> {
      AuthenticationUtil.setAdminUserAsFullyAuthenticatedUser();

      if (!siteService.hasSite(siteName)) {
        siteService.createSite(siteName, siteName, siteName, siteName, SiteVisibility.PUBLIC);
      }

      documentLibrary = SiteServiceImpl.getSiteContainer(siteName, SiteService.DOCUMENT_LIBRARY, true, siteService,
        transactionService, taggingService);
    });
  }

  protected String getSiteName() {
    return randomUUID().toString();
  }

  @Override
  protected void fetchNodeImpl(final RepositoryNode node) {
    super.fetchNodeImpl(node);
    setPath(node, nodeService.getPath(node.getNodeRef()).toDisplayPath(nodeService, permissionService));
  }

  protected static String getPath(final RepositoryNode node) {
    return node.getExtension(NODE_PATH, String.class);
  }

  private static void setPath(final RepositoryNode node, final String path) {
    node.getExtensions().put(NODE_PATH, path);
  }

  protected String buildSitePath() {
    return Paths
        .get(nodeService.getPath(getDocumentLibrary()).toDisplayPath(nodeService, permissionService),
            SiteService.DOCUMENT_LIBRARY)
        .toString();
  }

  protected String buildNodePath(final String departmentName, final LocalDateTime date) {
    return Paths
        .get(buildSitePath(), departmentName, Integer.toString(date.getYear()), date.format(DateTimeFormatter.ofPattern("MM")))
        .toString();
  }

  protected RepositoryNodeBuilder buildNode() {
    return RepositoryNode.builder()
        .parent(documentLibrary)
        .named(randomUUID());
  }

  protected NodeRef getDocumentLibrary() {
    return documentLibrary;
  }
}
