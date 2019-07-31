package com.atolcd.alfresco.filer.core.scope.impl;

import java.util.Objects;

import org.springframework.beans.factory.InitializingBean;

import com.atolcd.alfresco.filer.core.model.FilerEvent;
import com.atolcd.alfresco.filer.core.scope.FilerScopeLoader;
import com.atolcd.alfresco.filer.core.service.FilerRegistry;

public abstract class AbstractFilerScopeLoader implements FilerScopeLoader, InitializingBean {

  private FilerRegistry filerRegistry;

  @Override
  public void afterPropertiesSet() {
    Objects.requireNonNull(filerRegistry);
    filerRegistry.registerScopeLoader(this);
  }

  @Override
  public void init(final FilerEvent event) { // NOPMD - default empty method, in case init is not required
    // no op
  }

  @Override
  public void update(final FilerEvent event) { // NOPMD - default empty method, in case update is not required
    // no op
  }

  public void setFilerRegistry(final FilerRegistry filerRegistry) {
    this.filerRegistry = filerRegistry;
  }
}
