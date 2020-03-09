package com.atolcd.alfresco.filer.core.scope.impl;

import org.springframework.beans.factory.InitializingBean;

import com.atolcd.alfresco.filer.core.scope.FilerScopeLoader;
import com.atolcd.alfresco.filer.core.service.FilerRegistry;

public class EmptyFilerScopeLoader implements FilerScopeLoader, InitializingBean {

  private final FilerRegistry filerRegistry;

  protected EmptyFilerScopeLoader(final FilerRegistry filerRegistry) {
    this.filerRegistry = filerRegistry;
  }

  @Override
  public void afterPropertiesSet() {
    filerRegistry.registerScopeLoader(this);
  }
}
