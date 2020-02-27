package com.atolcd.alfresco.filer.core.test.framework;

import org.alfresco.service.transaction.TransactionService;
import org.springframework.beans.factory.annotation.Autowired;

public class TransactionHelper {

  @Autowired
  private TransactionService transactionService;

  public void run(final Runnable callback) {
    run(callback, false);
  }

  public void run(final Runnable callback, final boolean readOnly) {
    transactionService.getRetryingTransactionHelper().doInTransaction(() -> {
      callback.run();
      return null;
    }, readOnly);
  }
}
