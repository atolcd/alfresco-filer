package com.atolcd.alfresco.filer.core.scope.impl;

import java.util.Objects;

import org.springframework.beans.factory.InitializingBean;

import com.atolcd.alfresco.filer.core.scope.FilerScopeLoader;
import com.atolcd.alfresco.filer.core.service.FilerRegistry;

import edu.umd.cs.findbugs.annotations.Nullable;

public class EmptyFilerScopeLoader implements FilerScopeLoader, InitializingBean {

  @Nullable
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
