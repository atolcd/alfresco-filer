package com.atolcd.alfresco.filer.core.test.framework;

import java.util.Optional;

import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.service.cmr.site.SiteService;
import org.alfresco.service.transaction.TransactionService;
import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.platform.commons.util.AnnotationUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.junit.jupiter.SpringExtension;

public class LibraryRoleExtension implements BeforeAllCallback, AfterAllCallback {

  @Autowired
  private TransactionService transactionService;
  @Autowired
  private SiteService siteService;

  @Override
  public void beforeAll(final ExtensionContext context) {
    Class<?> clazz = context.getRequiredTestClass();
    Optional<TestLibraryRole> annotation = AnnotationUtils.findAnnotation(clazz, TestLibraryRole.class);
    if (!annotation.isPresent()) {
      throw new IllegalStateException("Could not find TestLibraryRole annotation on class: " + clazz);
    }

    String role = annotation.map(TestLibraryRole::value).filter(value -> !value.isEmpty())
        .orElseThrow(() -> new IllegalStateException("TestLibraryRole annotation should specify a role name on class: " + clazz));

    ApplicationContext applicationContext = SpringExtension.getApplicationContext(context);
    applicationContext.getAutowireCapableBeanFactory().autowireBean(this);

    AuthenticationUtil.runAsSystem(() -> {
      transactionService.getRetryingTransactionHelper().doInTransaction(() -> {
        siteService.setMembership(
            LibraryExtension.getLibrary(context).getSiteName(),
            AuthenticationExtension.getUser(context),
            role);
        return null;
      });
      return null;
    });
  }

  @Override
  public void afterAll(final ExtensionContext context) {
    AuthenticationUtil.runAsSystem(() -> {
      transactionService.getRetryingTransactionHelper().doInTransaction(() -> {
        siteService.removeMembership(
            LibraryExtension.getLibrary(context).getSiteName(),
            AuthenticationExtension.getUser(context));
        return null;
      });
      return null;
    });
  }
}
