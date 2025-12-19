package com.gamesj.Core.Services;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import com.gamesj.API.WebSocket.WebSocketHandler;
import jakarta.annotation.PreDestroy;

// Base class for monitoring idle users or other entities 
public abstract class IdleMonitor<TKey> {

  @Autowired
  private ApplicationContext context;

  protected short idleTimeoutMinutes;
  protected short cleanupIntervalSeconds;

  /** Child must provide the activity map. */
  protected abstract ConcurrentHashMap<TKey, Client> getActivityMap();

  private final AtomicBoolean timerRunning = new AtomicBoolean(false);

  // Timer functionality: ScheduledExecutorService
  protected final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
  protected ScheduledFuture<?> cleanupTask;  // to control start/stop

  protected IdleMonitor(short idleTimeoutMinutes, short cleanupIntervalSeconds) {
    this.idleTimeoutMinutes = idleTimeoutMinutes;
    this.cleanupIntervalSeconds = cleanupIntervalSeconds;
    System.out.println(" =========== Created IdleMonitor ======="); 
  }

  protected void broadcastWsMessage(String wsJson) {
    WebSocketHandler wsHandler =  context.getBean(WebSocketHandler.class);
    wsHandler.broadcast(wsJson);
  }

  protected synchronized void startTimer(String msg  ) {
    System.out.println(" *** Attempt to start Timer for " + msg);
    if (timerRunning.compareAndSet(false, true)) {
        cleanupTask = scheduler.scheduleAtFixedRate(
          this::cleanupIdleItems,
          cleanupIntervalSeconds,
          cleanupIntervalSeconds,
          TimeUnit.SECONDS
        );
        System.out.println(" *** Timer started ("+msg+")*********************************** ");
    }
  }

  protected synchronized void stopTimer(String msg) {
    if (cleanupTask != null && !cleanupTask.isCancelled()) {
      cleanupTask.cancel(false);
      timerRunning.set(false);
      System.out.println(" *** Timer STOPPED ("+msg+")*********************************** ");
    }
  }

  public abstract void cleanupIdleItems();
  
  // shutdown scheduler (when app stops)
  @PreDestroy
  public void shutdown() {
      scheduler.shutdown();
      System.out.println(" *** Scheduler SHUTDOWN");
  }
}
