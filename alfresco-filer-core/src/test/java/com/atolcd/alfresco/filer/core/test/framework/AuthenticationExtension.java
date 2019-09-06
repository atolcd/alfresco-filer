package com.atolcd.alfresco.filer.core.test.framework;

import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

public class AuthenticationExtension implements BeforeEachCallback, AfterEachCallback {

  @Override
  public void beforeEach(final ExtensionContext context) {
    AuthenticationUtil.setFullyAuthenticatedUser(AuthenticationUtil.getAdminUserName());
  }

  @Override
    public void afterEach(final ExtensionContext context) {
    AuthenticationUtil.clearCurrentSecurityContext();
  }
}
