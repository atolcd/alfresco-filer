package com.atolcd.alfresco.filer.core.scope.impl;

import com.atolcd.alfresco.filer.core.model.FilerEvent;
import com.atolcd.alfresco.filer.core.scope.FilerScopeLoader;

public class EmptyFilerScopeLoader implements FilerScopeLoader {

  @Override
  public void init(final FilerEvent event) {
    // no op
  }

  @Override
  public void update(final FilerEvent event) {
    // no op
  }
}
