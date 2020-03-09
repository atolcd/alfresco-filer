package com.atolcd.alfresco.filer.core.service.impl;

import org.alfresco.repo.dictionary.DictionaryDAO;
import org.alfresco.repo.dictionary.DictionaryListener;
import org.alfresco.service.namespace.QName;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationEvent;
import org.springframework.extensions.surf.util.AbstractLifecycleBean;

import com.atolcd.alfresco.filer.core.model.FilerException;

public abstract class DictionaryListenerAspect extends AbstractLifecycleBean implements DictionaryListener, InitializingBean {

  private static final Logger LOGGER = LoggerFactory.getLogger(DictionaryListenerAspect.class);

  private final DictionaryDAO dictionaryDAO;

  private boolean enabled;

  public DictionaryListenerAspect(final DictionaryDAO dictionaryDAO) {
    super();
    this.dictionaryDAO = dictionaryDAO;
  }

  @Override
  public void afterPropertiesSet() {
    dictionaryDAO.registerListener(this);
  }

  protected abstract QName getAspect();

  protected abstract void init();

  @Override
  public void afterDictionaryInit() {
    if (!enabled && dictionaryDAO.getClass(getAspect()) != null) {
      enabled = true;
      init();
    }
  }

  @Override
  protected void onBootstrap(final ApplicationEvent event) {
    if (enabled) {
      LOGGER.info("{} enabled for: {}", getClass().getSimpleName(), getAspect());
    } else {
      throw new FilerException("Could not find aspect: " + getAspect());
    }
  }

  @Override
  protected void onShutdown(final ApplicationEvent event) { // NOPMD - default empty method in abstract class
    // nothing to do on application shutdown
  }

  @Override
  public void onDictionaryInit() { // NOPMD - default empty method in abstract class
    // nothing to do on dictionary initialization
  }

  @Override
  public void afterDictionaryDestroy() { // NOPMD - default empty method in abstract class
    // nothing to do after dictionary deletion
  }
}
