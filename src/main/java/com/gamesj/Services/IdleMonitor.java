package com.gamesj.Services;

import java.time.LocalDateTime;
import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import com.gamesj.Models.User;
import com.gamesj.Repositories.UserRepository;
import com.gamesj.WebSockets.WebSocketHandler;
import jakarta.annotation.PreDestroy;
import com.fasterxml.jackson.databind.ObjectMapper;

// Base class for monitoring idle users or other entities 
public abstract class IdleMonitor<TKey> {

  @Autowired
  private ApplicationContext context;

  protected short idleTimeoutMinutes;
  protected short cleanupIntervalSeconds;

  /** Child must provide the activity map. */
  protected abstract ConcurrentHashMap<TKey, Client> getActivityMap();

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

  // called from controllers 1) /api/login and 2) /auth/refresh
  public abstract void updateUserActivity(int userId, UUID clientId);    

  // called from controller /api/logout
  public abstract void removeUser(int userId);    

  protected synchronized void startTimer(){
    if (cleanupTask != null && !cleanupTask.isCancelled() && !cleanupTask.isDone()) {
      return; // already running
    }
    cleanupTask = scheduler.scheduleAtFixedRate(
      this::cleanupIdleUsers,
      cleanupIntervalSeconds,
      cleanupIntervalSeconds,
      TimeUnit.SECONDS
    );
    System.out.println(" *** Timer started " );
  }

  protected synchronized void stopTimer() {
    if (cleanupTask != null && !cleanupTask.isCancelled()) {
        cleanupTask.cancel(false);
        System.out.println(" *** Timer STOPPED");
    }
  }

  public abstract void autoLogout(int userId);
  public abstract void cleanupIdleUsers();
  
  // shutdown scheduler (when app stops)
  @PreDestroy
  public void shutdown() {
      scheduler.shutdown();
      System.out.println(" *** Scheduler SHUTDOWN");
  }
}
