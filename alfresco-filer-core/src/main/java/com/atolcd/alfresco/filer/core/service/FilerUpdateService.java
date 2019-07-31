package com.atolcd.alfresco.filer.core.service;

import com.atolcd.alfresco.filer.core.model.RepositoryNode;

public interface FilerUpdateService {

  /**
   * Update and move node according to the filer operation result.
   * Node was first present with the state of initialNode and may have been updated to the state of originalNode before
   * filer operation
   * @param initialNode node at the beginning, before the request
   * @param originalNode node before applying filer
   * @param resultingNode node after applying filer
   */
  void updateAndMoveFileable(RepositoryNode initialNode, RepositoryNode originalNode, RepositoryNode resultingNode);
}
