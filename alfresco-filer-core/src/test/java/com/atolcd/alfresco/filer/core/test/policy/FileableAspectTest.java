package com.atolcd.alfresco.filer.core.test.policy;

import static com.atolcd.alfresco.filer.core.test.framework.LibraryExtension.getLibrary;
import static java.util.UUID.randomUUID;
import static org.assertj.core.api.Assertions.assertThat;

import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.repo.security.permissions.AccessDeniedException;
import org.alfresco.repo.site.SiteModel;
import org.alfresco.service.cmr.security.PermissionService;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.atolcd.alfresco.filer.core.model.RepositoryNode;
import com.atolcd.alfresco.filer.core.test.domain.content.model.FilerTestConstants;
import com.atolcd.alfresco.filer.core.test.framework.RepositoryNodeHelper;
import com.atolcd.alfresco.filer.core.test.framework.TestApplicationContext;
import com.atolcd.alfresco.filer.core.test.framework.TestAuthentication;
import com.atolcd.alfresco.filer.core.test.framework.TestLibrary;
import com.atolcd.alfresco.filer.core.test.framework.TestLibraryRole;
import com.atolcd.alfresco.filer.core.test.framework.TransactionHelper;
import com.atolcd.alfresco.filer.core.util.FilerTransactionUtils;

@TestApplicationContext
@TestLibrary
@TestAuthentication
@TestLibraryRole(SiteModel.SITE_CONTRIBUTOR)
public class FileableAspectTest {

  @Autowired
  private PermissionService permissionService;
  @Autowired
  private TransactionHelper transactionHelper;
  @Autowired
  private RepositoryNodeHelper repositoryNodeHelper;

  @Test
  public void noUpdateUserWithGlobalUser() {
    Throwable thrown = noUpdateUser(() -> FilerTransactionUtils.setGlobalUser(AuthenticationUtil.getSystemUserName()));

    // No problem encountered
    assertThat(thrown).isNull();
  }

  @Test
  public void noUpdateUserWithoutGlobalUser() {
    Throwable thrown = noUpdateUser(() -> {});

    assertThat(thrown.getCause()).isInstanceOf(AccessDeniedException.class);
  }

  private Throwable noUpdateUser(final Runnable task) {
    RepositoryNode node = getLibrary().childNode()
        .type(FilerTestConstants.Department.DocumentType.NAME)
        .property(FilerTestConstants.Department.Aspect.PROP_NAME, randomUUID().toString())
        .build();

    return Assertions.catchThrowable(() -> {
      transactionHelper.run(() -> {
        repositoryNodeHelper.createNode(node);

        task.run();

        permissionService.setInheritParentPermissions(node.getNodeRef().get(), false);
      });
    });
  }
}
