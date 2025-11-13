package com.gamesj.Services;

import java.time.LocalDateTime;
import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.*;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.gamesj.Models.User;
import com.gamesj.Repositories.UserRepository;
import com.gamesj.WebSockets.WebSocketHandler;
import com.fasterxml.jackson.databind.ObjectMapper;

// Spring-managed singleton 
@Service
public class UserMonitor {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private WebSocketHandler webSocketHandler;

    private static final Duration IDLE_TIMEOUT = Duration.ofMinutes(1);
    private static final long CLEANUP_INTERVAL_SECONDS = 35;

    private final ConcurrentHashMap<Integer, LocalDateTime> userActivityMap = new ConcurrentHashMap<>();

    // Timer functionality: ScheduledExecutorService
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    private ScheduledFuture<?> cleanupTask;  // to control start/stop

    public UserMonitor() {
      System.out.println(" =========== Created UserMonitor ======="); 
    }

    public void updateUserActivity(int userId) {
        userActivityMap.put(userId, LocalDateTime.now());
        System.out.println(" *** updateUserActivity " + userId + " @ " + LocalDateTime.now());

        if (cleanupTask == null || cleanupTask.isCancelled() || cleanupTask.isDone()) 
          startTimer();
    }

    public synchronized void removeUser(int userId) {
      if (userActivityMap.remove(userId) != null) {
        System.out.println(" *** User " + userId + " removed from UserMonitor manually");
        // If no users remain → stop timer
        if (userActivityMap.isEmpty()) {
          System.out.println(" *** LAST User " + userId + " removed from UserMonitor");
          stopTimer();
        }
      } 
      else 
        System.out.println(" *** removeUser: user " + userId + " not found in map");
    }

    private synchronized  void startTimer(){
      if (cleanupTask != null && !cleanupTask.isCancelled() && !cleanupTask.isDone()) {
        return; // already running
      }

      cleanupTask = scheduler.scheduleAtFixedRate(
        this::cleanupIdleUsers,
        CLEANUP_INTERVAL_SECONDS,
        CLEANUP_INTERVAL_SECONDS,
        TimeUnit.SECONDS
      );

      System.out.println(" *** Timer started " );
    }

    private synchronized void stopTimer() {
        if (cleanupTask != null && !cleanupTask.isCancelled()) {
            cleanupTask.cancel(false);
            System.out.println(" *** Timer STOPPED");
        }
    }

    public void autoLogout(int userId){
      try{
        Optional<User> optionalUser = userRepository.findById(userId);
        if (optionalUser.isPresent()) {
          User user = optionalUser.get();
          user.setIsOnline(false);
          userRepository.save(user);
          System.out.println(" *** Updated in DB");

          Map<String, Object> response = Map.of(
              "userId", userId,
              "isOnline", false,
              "automaticLogout", true
          );
          Map<String, Object> wsMessage = Map.of(
              "type", "userSessionUpdate",
              "status", "WsStatus.OK",
              "data", response
          );

          String wsJson = new ObjectMapper().writeValueAsString(wsMessage);
          webSocketHandler.broadcast(wsJson);
          System.out.println(" *** Broadcasted WS logout for user " + userId);
        }
      }
      catch (Exception ex) {
        System.err.println("Error cleaning idle user " + userId + ": " + ex.getMessage());
        ex.printStackTrace();
      }
    }

    public void cleanupIdleUsers() {
      try {
        LocalDateTime now = LocalDateTime.now();
        System.out.println(" *** cleanupIdleUsers-START, Count = " + userActivityMap.size());

        userActivityMap.entrySet().removeIf(entry -> {
          boolean idle = Duration.between(entry.getValue(), now).compareTo(IDLE_TIMEOUT) > 0;
          System.out.println( " *** cleanup curr Interval= " +  Duration.between(entry.getValue(), now) );
          if (idle) {
            int userId = entry.getKey();
            System.out.println(" *** Removing idle user: " + userId);
            autoLogout(userId);
          }
          return idle;
        });
        System.out.println(" *** cleanupIdleUsers-END, Count = " + userActivityMap.size());
        if( userActivityMap.isEmpty() ) // Stop timer if no active users remain
          stopTimer();
      } 
      catch (Exception e) {
        System.err.println("Fatal error in cleanupIdleUsers: " + e.getMessage());
        e.printStackTrace();
      }
    }

    // shutdown scheduler (when app stops)
    public void shutdown() {
        scheduler.shutdown();
        System.out.println(" *** Scheduler SHUTDOWN");
    }
}
