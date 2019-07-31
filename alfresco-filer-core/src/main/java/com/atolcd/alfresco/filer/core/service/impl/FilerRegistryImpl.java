package com.atolcd.alfresco.filer.core.service.impl;

import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import com.atolcd.alfresco.filer.core.model.FilerAction;
import com.atolcd.alfresco.filer.core.scope.FilerScopeLoader;
import com.atolcd.alfresco.filer.core.service.FilerRegistry;

public class FilerRegistryImpl implements FilerRegistry {

  private SortedSet<FilerAction> actions;
  private Set<FilerScopeLoader> scopeLoaders;

  @Override
  public void registerAction(final FilerAction action) {
    getActions().add(action);
  }

  @Override
  public void registerScopeLoader(final FilerScopeLoader scopeLoader) {
    getScopeLoaders().add(scopeLoader);
  }

  @Override
  public SortedSet<FilerAction> getActions() {
    actions = Optional.ofNullable(actions).orElseGet(TreeSet::new);
    return actions;
  }

  @Override
  public Set<FilerScopeLoader> getScopeLoaders() {
    scopeLoaders = Optional.ofNullable(scopeLoaders).orElseGet(LinkedHashSet::new);
    return scopeLoaders;
  }
}
