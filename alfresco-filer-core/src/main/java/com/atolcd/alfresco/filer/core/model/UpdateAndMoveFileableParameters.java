package com.atolcd.alfresco.filer.core.model;

import edu.umd.cs.findbugs.annotations.Nullable;

public final class UpdateAndMoveFileableParameters {
  @Nullable
  private RepositoryNode initialNode;
  @Nullable
  private RepositoryNode originalNode;
  @Nullable
  private RepositoryNode resultingNode;

  public RepositoryNode getInitialNode() {
    return initialNode;
  }

  public void setInitialNode(final RepositoryNode initialNode) {
    this.initialNode = initialNode;
  }

  public RepositoryNode getOriginalNode() {
    return originalNode;
  }

  public void setOriginalNode(final RepositoryNode originalNode) {
    this.originalNode = originalNode;
  }

  public RepositoryNode getResultingNode() {
    return resultingNode;
  }

  public void setResultingNode(final RepositoryNode resultingNode) {
    this.resultingNode = resultingNode;
  }
}
