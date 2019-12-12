package com.atolcd.alfresco.filer.core.scope.impl;

import java.util.Objects;

import org.springframework.beans.factory.InitializingBean;

import com.atolcd.alfresco.filer.core.scope.FilerScopeLoader;
import com.atolcd.alfresco.filer.core.service.FilerRegistry;

public class EmptyFilerScopeLoader implements FilerScopeLoader, InitializingBean {

  private FilerRegistry filerRegistry;

  @Override
  public void afterPropertiesSet() {
    Objects.requireNonNull(filerRegistry);
    filerRegistry.registerScopeLoader(this);
  }

  public void setFilerRegistry(final FilerRegistry filerRegistry) {
    this.filerRegistry = filerRegistry;
  }
}
