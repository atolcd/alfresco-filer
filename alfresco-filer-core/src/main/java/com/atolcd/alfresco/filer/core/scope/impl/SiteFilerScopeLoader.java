package com.atolcd.alfresco.filer.core.scope.impl;

import org.alfresco.service.cmr.site.SiteInfo;
import org.alfresco.service.cmr.site.SiteService;

import com.atolcd.alfresco.filer.core.model.FilerEvent;
import com.atolcd.alfresco.filer.core.model.RepositoryNode;
import com.atolcd.alfresco.filer.core.service.FilerRegistry;
import com.atolcd.alfresco.filer.core.util.FilerNodeUtils;

public class SiteFilerScopeLoader extends EmptyFilerScopeLoader {

  private final SiteService siteService;

  public SiteFilerScopeLoader(final FilerRegistry filerRegistry, final SiteService siteService) {
    super(filerRegistry);
    this.siteService = siteService;
  }

  @Override
  public void init(final FilerEvent event) {
    RepositoryNode node = event.getNode();
    // Init site information
    SiteInfo siteInfo = siteService.getSite(node.getNodeRef().get());
    FilerNodeUtils.setSiteInfo(node, siteInfo);
  }
}
