package com.atolcd.alfresco.filer.core.scope;

import com.atolcd.alfresco.filer.core.model.FilerEvent;

public interface FilerScopeLoader {

  default void init(final FilerEvent event) {
    // no op
  }

  default void update(final FilerEvent event) {
    // no op
  }
}
