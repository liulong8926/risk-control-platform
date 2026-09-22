package com.tehang.enterpriserisk.service;

import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Supplier;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Component
public class RiskMaintenanceLock {
  private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock(true);

  public <T> T withCollection(Supplier<T> action) {
    lock.readLock().lock();
    boolean deferred = TransactionSynchronizationManager.isSynchronizationActive();
    if (deferred) TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
      @Override public void afterCompletion(int status) { lock.readLock().unlock(); }
    });
    try { return action.get(); }
    finally { if (!deferred) lock.readLock().unlock(); }
  }

  public <T> T withMaintenance(Supplier<T> action) {
    lock.writeLock().lock();
    try { return action.get(); }
    finally { lock.writeLock().unlock(); }
  }
}
