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
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ExtensionContext.Namespace;
import org.junit.jupiter.api.extension.ExtensionContext.Store;
import org.junit.platform.commons.util.AnnotationUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.util.Assert;

public class LibraryExtension implements BeforeAllCallback, BeforeEachCallback, AfterEachCallback, AfterAllCallback {

  private static final Namespace NAMESPACE = Namespace.create(LibraryExtension.class);

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

  private static ThreadLocal<Library> libraryHolder = new ThreadLocal<>();

  @Override
  public void beforeAll(final ExtensionContext context) {
    Class<?> clazz = context.getRequiredTestClass();
    Optional<TestLibrary> annotation = AnnotationUtils.findAnnotation(clazz, TestLibrary.class);
    if (!annotation.isPresent()) {
      throw new IllegalStateException("Could not find TestLibrary annotation on class: " + clazz);
    }

    String siteName = annotation.map(TestLibrary::value).filter(value -> !value.isEmpty())
        .orElseGet(() -> "library-" + randomUUID().toString());

    ApplicationContext applicationContext = SpringExtension.getApplicationContext(context);
    applicationContext.getAutowireCapableBeanFactory().autowireBean(this);

    Library library = new Library(siteName, nodeRef -> {
      return Paths
          .get(nodeService.getPath(nodeRef).toDisplayPath(nodeService, permissionService), SiteService.DOCUMENT_LIBRARY);
    });
    initLibrary(library);
    setLibrary(context, library);
  }

  @Override
  public void beforeEach(final ExtensionContext context) {
    Library library = getLibrary(context);
    libraryHolder.set(library);
  }

  @Override
  public void afterEach(final ExtensionContext context) {
    libraryHolder.remove();
  }

  @Override
  public void afterAll(final ExtensionContext context) {
    getStore(context).remove(context.getRequiredTestClass());
  }

  public static Library getLibrary(final ExtensionContext context) {
    Assert.notNull(context, "ExtensionContext must not be null");
    Class<?> testClass = context.getRequiredTestClass();
    return getStore(context).get(testClass, Library.class);
  }

  private static void setLibrary(final ExtensionContext context, final Library library) {
    Class<?> testClass = context.getRequiredTestClass();
    getStore(context).put(testClass, library);
  }

  private static Store getStore(final ExtensionContext context) {
    return context.getStore(NAMESPACE);
  }

  private void initLibrary(final Library library) {
    // Synchronization is required to avoid race condition: multiple test classes executed in parallel trying to create the same
    // site at the same time, leading to duplicate child name errors.
    synchronized (NAMESPACE) {
      AuthenticationUtil.runAs(() -> {
        transactionService.getRetryingTransactionHelper().doInTransaction(() -> {
          String siteName = library.getSiteName();

          if (!siteService.hasSite(siteName)) {
            siteService.createSite(siteName, siteName, siteName, siteName, SiteVisibility.PRIVATE);
          }

          NodeRef documentLibrary = SiteServiceImpl.getSiteContainer(siteName, SiteService.DOCUMENT_LIBRARY, true, siteService,
              transactionService, taggingService);
          library.setNodeRef(documentLibrary);
          return null;
        }, false);
        return null;
      }, AuthenticationUtil.getAdminUserName());
    }
  }

  public static Library getLibrary() {
    return libraryHolder.get();
  }

  public static void withLibrary(final Library library, final Runnable task) {
    Library original = getLibrary();
    libraryHolder.set(library);
    try {
      task.run();
    } finally {
      if (original == null) {
        libraryHolder.remove();
      } else {
        libraryHolder.set(original);
      }
    }
  }
}
