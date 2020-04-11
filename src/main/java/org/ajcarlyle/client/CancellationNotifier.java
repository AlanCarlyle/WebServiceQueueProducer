package org.ajcarlyle.client;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

public class CancellationNotifier {

  private ExecutorService manager;
  private boolean isCanceled;
  private long timeout;
  private TimeUnit timeUnit;

  public CancellationNotifier(ExecutorService manager, long timeout, TimeUnit timeUnit) {
    this.manager = manager;
    this.timeUnit = timeUnit;
    this.timeout = timeout;
    this.isCanceled = false;
  }

  public CancellationNotifier(ExecutorService manager) {
    this(manager, 800, TimeUnit.MILLISECONDS);
  }

  public void cancel() {
    cancel(false);
  }

  public synchronized void cancel(boolean forced) {
    if (!isCanceled) {
      isCanceled = true;
      if (!(manager.isShutdown() || manager.isTerminated())) {
        manager.shutdown();
      }
      if (forced)
      forcedShutdown();
    }
  }

  private void forcedShutdown() {
    try {
      if (!manager.awaitTermination(timeout, timeUnit)) {
        manager.shutdownNow();
      }
    } catch (InterruptedException e) {
      manager.shutdownNow();
    }
  }

  /**
   * @return the manager
   */
  public ExecutorService getManager() {
    return manager;
  }

  /**
   * @return the isCanceled
   */
  public synchronized boolean isCanceled() {
    return isCanceled;
  }

  /**
   * @return the timeout
   */
  public long getTimeout() {
    return timeout;
  }

  /**
   * @return the timeUnit
   */
  public TimeUnit getTimeUnit() {
    return timeUnit;
  }
}