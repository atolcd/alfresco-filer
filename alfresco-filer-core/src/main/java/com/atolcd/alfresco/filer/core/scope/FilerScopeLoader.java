package com.atolcd.alfresco.filer.core.scope;

import com.atolcd.alfresco.filer.core.model.FilerEvent;

public interface FilerScopeLoader {

  void init(FilerEvent event);

  void update(FilerEvent event);
}
