package com.atolcd.alfresco.filer.core.test.domain;

import static com.atolcd.alfresco.filer.core.test.framework.LibraryExtension.getLibrary;
import static com.atolcd.alfresco.filer.core.util.FilerNodeUtils.getPath;
import static java.util.UUID.randomUUID;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.alfresco.model.ContentModel;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.repo.site.SiteModel;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.security.PersonService;
import org.alfresco.service.cmr.site.SiteService;
import org.alfresco.service.namespace.NamespaceService;
import org.alfresco.service.namespace.QName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.atolcd.alfresco.filer.core.model.RepositoryNode;
import com.atolcd.alfresco.filer.core.test.domain.content.model.FilerTestConstants;
import com.atolcd.alfresco.filer.core.test.domain.util.NodePathUtils;
import com.atolcd.alfresco.filer.core.test.framework.RepositoryOperations;

public class FileableAuthenticationTest extends RepositoryOperations {

  @Autowired
  private PersonService personService;
  @Autowired
  private SiteService siteService;
  @Autowired
  private NodeService nodeService;

  @Test
  public void onAddAspect() {
    AuthenticationUtil.clearCurrentSecurityContext();
    String person = randomUUID().toString();
    createPerson(person, SiteModel.SITE_CONTRIBUTOR);

    String departmentName = randomUUID().toString();

    RepositoryNode node = getLibrary().childNode()
        .type(FilerTestConstants.Department.DocumentType.NAME)
        .property(FilerTestConstants.Department.Aspect.PROP_NAME, departmentName)
        .build();

    // Trigger FileableAspect#onAddAspect behaviour
    doInTransaction(() -> {
      AuthenticationUtil.runAs(() -> {
        createNode(node);
        return null;
      }, person);
    });

    AuthenticationUtil.runAs(() -> {
      // Fetch node as the fetch done after createNode transaction commit (by binding a transaction listener) failed due
      // to missing authentication (it fail silently, only logging exception)
      fetchNode(node);

      assertThat(getPath(node)).isEqualTo(NodePathUtils.nodePath(departmentName, LocalDateTime.now()));
      assertThat(node.getProperty(ContentModel.PROP_MODIFIER, String.class)).isEqualTo(person);
      return null;
    }, person);
  }

  @Test
  public void onUpdateProperties() {
    AuthenticationUtil.clearCurrentSecurityContext();
    String personCreate = randomUUID().toString();
    String personUpdate = randomUUID().toString();
    createPerson(personCreate, SiteModel.SITE_CONTRIBUTOR);
    createPerson(personUpdate, SiteModel.SITE_COLLABORATOR);

    RepositoryNode node = getLibrary().childNode()
        .type(FilerTestConstants.Department.DocumentType.NAME)
        .property(FilerTestConstants.Department.Aspect.PROP_NAME, randomUUID())
        .build();

    AuthenticationUtil.runAs(() -> {
      createNode(node);
      return null;
    }, personCreate);

    String departmentName = randomUUID().toString();

    // Trigger FileableAspect#onUpdateProperties behaviour
    doInTransaction(() -> {
      AuthenticationUtil.runAs(() -> {
        updateNode(node, Collections.singletonMap(FilerTestConstants.Department.Aspect.PROP_NAME, departmentName));
        return null;
      }, personUpdate);
    });

    AuthenticationUtil.runAs(() -> {
      // Fetch node as the fetch done after updateNode transaction commit (by binding a transaction listener) failed due
      // to missing authentication (it fail silently, only logging exception)
      fetchNode(node);

      assertThat(getPath(node)).isEqualTo(NodePathUtils.nodePath(departmentName, LocalDateTime.now()));
      assertThat(node.getProperty(ContentModel.PROP_MODIFIER, String.class)).isEqualTo(personUpdate);
      return null;
    }, personCreate);
  }

  @Test
  public void onMoveNode() {
    AuthenticationUtil.clearCurrentSecurityContext();
    String personCreate = randomUUID().toString();
    String personMove = randomUUID().toString();
    createPerson(personCreate, SiteModel.SITE_CONTRIBUTOR);
    createPerson(personMove, SiteModel.SITE_MANAGER);

    String departmentName = randomUUID().toString();

    RepositoryNode node = getLibrary().childNode()
        .type(FilerTestConstants.Department.DocumentType.NAME)
        .property(FilerTestConstants.Department.Aspect.PROP_NAME, departmentName)
        .build();

    AuthenticationUtil.runAs(() -> {
      doInTransaction(() -> {
        createNode(node);
      });
      return null;
    }, personCreate);

    // Trigger FileableAspect#onMoveNode behaviour
    doInTransaction(() -> {
      AuthenticationUtil.runAs(() -> {
        QName assocQName = QName.createQNameWithValidLocalName(NamespaceService.CONTENT_MODEL_1_0_URI, node.getName().get());
        nodeService.moveNode(node.getNodeRef(), getLibrary().getNodeRef(), ContentModel.ASSOC_CONTAINS, assocQName);
        return null;
      }, personMove);
    });

    AuthenticationUtil.runAs(() -> {
      fetchNode(node);
      assertThat(getPath(node)).isEqualTo(NodePathUtils.nodePath(departmentName, LocalDateTime.now()));
      assertThat(node.getProperty(ContentModel.PROP_MODIFIER, String.class)).isEqualTo(personMove);
      return null;
    }, personCreate);
  }

  private void createPerson(final String userName, final String role) {
    AuthenticationUtil.runAsSystem(() -> {
      doInTransaction(() -> {
        if (personService.getPersonOrNull(userName) == null) {
          Map<QName, Serializable> properties = new HashMap<>();
          properties.put(ContentModel.PROP_USERNAME, userName);
          personService.createPerson(properties);
        }

        siteService.setMembership(getLibrary().getSiteName(), userName, role);
      });
      return null;
    });
  }
}
