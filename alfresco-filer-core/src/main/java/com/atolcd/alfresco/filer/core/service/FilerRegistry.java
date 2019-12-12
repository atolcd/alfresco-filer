package com.atolcd.alfresco.filer.core.service;

import java.util.Set;
import java.util.SortedSet;

import com.atolcd.alfresco.filer.core.model.FilerAction;
import com.atolcd.alfresco.filer.core.scope.FilerScopeLoader;

public interface FilerRegistry {

  /**
   * Register an action that can be executed
   */
  void registerAction(FilerAction action);

  /**
   * Register a loader that will be able to initialize required information to perform the filing
   */
  void registerScopeLoader(FilerScopeLoader scopeLoader);

  /**
   * Get actions that can be executed
   */
  SortedSet<FilerAction> getActions();

  /**
   * Get loaders to initialize the node scope
   */
  Set<FilerScopeLoader> getScopeLoaders();
}
