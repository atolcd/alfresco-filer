package com.atolcd.alfresco.filer.core.test.framework;

import static java.util.UUID.randomUUID;

import java.nio.file.Paths;
import java.util.Optional;

import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.repo.site.SiteServiceImpl;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.security.PermissionService;
import org.alfresco.service.cmr.site.SiteService;
import org.alfresco.service.cmr.site.SiteVisibility;
import org.alfresco.service.cmr.tagging.TaggingService;
import org.alfresco.service.transaction.TransactionService;
import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.platform.commons.util.AnnotationUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.junit.jupiter.SpringExtension;

public class DocumentLibraryExtension implements BeforeAllCallback, AfterAllCallback {

  @Autowired
  private TransactionService transactionService;
  @Autowired
  private SiteService siteService;
  @Autowired
  private TaggingService taggingService;
  @Autowired
  private NodeService nodeService;
  @Autowired
  private PermissionService permissionService;

  private static ThreadLocal<DocumentLibrary> documentLibraryHolder = new ThreadLocal<>();

  @Override
  public void beforeAll(final ExtensionContext context) {
    Class<?> clazz = context.getRequiredTestClass();
    Optional<TestDocumentLibrary> annotation = AnnotationUtils.findAnnotation(clazz, TestDocumentLibrary.class);
    if (!annotation.isPresent()) {
      throw new IllegalStateException("Could not find TestDocumentLibrary annotation on class: " + clazz);
    }

    String siteName = annotation.map(TestDocumentLibrary::value).filter(value -> !value.isEmpty())
        .orElseGet(() -> randomUUID().toString());

    ApplicationContext applicationContext = SpringExtension.getApplicationContext(context);
    applicationContext.getAutowireCapableBeanFactory().autowireBean(this);

    DocumentLibrary documentLibrary = new DocumentLibrary(siteName, nodeRef -> {
      return Paths
          .get(nodeService.getPath(nodeRef).toDisplayPath(nodeService, permissionService), SiteService.DOCUMENT_LIBRARY);
    });
    initDocumentLibrary(documentLibrary);
    documentLibraryHolder.set(documentLibrary);
  }

  private void initDocumentLibrary(final DocumentLibrary documentLibrary) {
    AuthenticationUtil.runAs(() -> {
      transactionService.getRetryingTransactionHelper().doInTransaction(() -> {
        String siteName = documentLibrary.getSiteName();

        if (!siteService.hasSite(siteName)) {
          siteService.createSite(siteName, siteName, siteName, siteName, SiteVisibility.PUBLIC);
        }

        NodeRef library = SiteServiceImpl.getSiteContainer(siteName, SiteService.DOCUMENT_LIBRARY, true, siteService,
            transactionService, taggingService);
        documentLibrary.setNodeRef(library);
        return null;
      }, false);
      return null;
    }, AuthenticationUtil.getAdminUserName());
  }

  @Override
  public void afterAll(final ExtensionContext context) {
    documentLibraryHolder.remove();
  }

  public static DocumentLibrary getDocumentLibrary() {
    return documentLibraryHolder.get();
  }

  public static void withDocumentLibrary(final DocumentLibrary documentLibrary, final Runnable task) {
    DocumentLibrary original = getDocumentLibrary();
    documentLibraryHolder.set(documentLibrary);
    try {
      task.run();
    } finally {
      if (original == null) {
        documentLibraryHolder.remove();
      } else {
        documentLibraryHolder.set(original);
      }
    }
  }
}
