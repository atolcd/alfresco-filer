package com.atolcd.alfresco.filer.core.test.framework;

import static java.util.UUID.randomUUID;

import java.io.Serializable;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.alfresco.model.ContentModel;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.service.cmr.security.PersonService;
import org.alfresco.service.namespace.QName;
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

public class AuthenticationExtension implements BeforeAllCallback, BeforeEachCallback, AfterEachCallback, AfterAllCallback {

  private static final Namespace NAMESPACE = Namespace.create(AuthenticationExtension.class);

  @Autowired
  private TransactionService transactionService;
  @Autowired
  private PersonService personService;

  private static ThreadLocal<String> userHolder = new ThreadLocal<>();

  @Override
  public void beforeAll(final ExtensionContext context) {
    Class<?> clazz = context.getRequiredTestClass();
    Optional<TestAuthentication> annotation = AnnotationUtils.findAnnotation(clazz, TestAuthentication.class);
    if (!annotation.isPresent()) {
      throw new IllegalStateException("Could not find TestAuthentication annotation on class: " + clazz);
    }

    String userName = annotation.map(TestAuthentication::value).filter(value -> !value.isEmpty())
        .orElseGet(() -> "user-" + randomUUID().toString());

    ApplicationContext applicationContext = SpringExtension.getApplicationContext(context);
    applicationContext.getAutowireCapableBeanFactory().autowireBean(this);

    List<String> specialNames = Arrays.asList(AuthenticationUtil.getAdminUserName(), AuthenticationUtil.getSystemUserName(),
        AuthenticationUtil.getGuestUserName());
    if (!specialNames.contains(userName)) {
      createPerson(userName);
    }
    setUser(context, userName);
  }

  @Override
  public void beforeEach(final ExtensionContext context) {
    String userName = getUser(context);
    userHolder.set(userName);
    AuthenticationUtil.setFullyAuthenticatedUser(userName);
  }

  @Override
  public void afterEach(final ExtensionContext context) {
    AuthenticationUtil.clearCurrentSecurityContext();
    userHolder.remove();
  }

  @Override
  public void afterAll(final ExtensionContext context) {
    getStore(context).remove(context.getRequiredTestClass());
  }

  public static String getUser(final ExtensionContext context) {
    Assert.notNull(context, "ExtensionContext must not be null");
    Class<?> testClass = context.getRequiredTestClass();
    return getStore(context).get(testClass, String.class);
  }

  private static void setUser(final ExtensionContext context, final String user) {
    Class<?> testClass = context.getRequiredTestClass();
    getStore(context).put(testClass, user);
  }

  private static Store getStore(final ExtensionContext context) {
    return context.getStore(NAMESPACE);
  }

  private void createPerson(final String userName) {
    // Synchronization is required to avoid race condition: multiple test classes executed in parallel trying to create the same
    // person at the same time, leading to duplicate child name errors.
    synchronized (NAMESPACE) {
      AuthenticationUtil.runAsSystem(() -> {
        transactionService.getRetryingTransactionHelper().doInTransaction(() -> {
          if (personService.getPersonOrNull(userName) == null) {
            Map<QName, Serializable> properties = new HashMap<>();
            properties.put(ContentModel.PROP_USERNAME, userName);
            personService.createPerson(properties);
          }
          return null;
        }, false);
        return null;
      });
    }
  }

  public static String getUser() {
    return userHolder.get();
  }

  public static void withUser(final String user, final Runnable task) {
    String original = getUser();
    userHolder.set(user);
    try {
      AuthenticationUtil.runAs(() -> {
        task.run();
        return null;
      }, user);
    } finally {
      if (original == null) {
        userHolder.remove();
      } else {
        userHolder.set(original);
      }
    }
  }
}
