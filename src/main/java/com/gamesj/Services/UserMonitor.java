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
    private ApplicationContext context; // inject context

    // Injected via application.properties
    @Value("${useridle.timeout-mins}")
    private long idleTimeoutMinutes;

    @Value("${useridle.cleanup-interval-sec}")
    private long cleanupIntervalSeconds;


    private final ConcurrentHashMap<Integer, Client> userActivityMap = new ConcurrentHashMap<>();
    public static final int EMPTY_USERID = -1;

    // Timer functionality: ScheduledExecutorService
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    private ScheduledFuture<?> cleanupTask;  // to control start/stop

    public UserMonitor() {
      System.out.println(" =========== Created UserMonitor ======="); 
    }

    private void broadcastWsMessage(String wsJson) {
      WebSocketHandler wsHandler =  context.getBean(WebSocketHandler.class);
      wsHandler.broadcast(wsJson);
    }

    public int getUserId(UUID clientId){
      for (Map.Entry<Integer, Client> entry : userActivityMap.entrySet()) {
        int userId  = entry.getKey();
        Client client = entry.getValue();
        if (client.getClientId().equals(clientId)) 
          return userId;
      }
      return EMPTY_USERID;
    }

    // called from controllers 1) /api/login and 2) /auth/refresh
    public void updateUserActivity(int userId, UUID clientId) {
      //userActivityMap.put(userId, new Client( LocalDateTime.now(), clientId ) );
      userActivityMap.compute(userId, (key, existingClient) -> {
        if (existingClient == null) {
          return new Client(LocalDateTime.now(), clientId);
        } else {
          existingClient.setTimeStamp();
          return existingClient;
        }
      });
      System.out.println(" *** updateUserActivity " + userId + " @ " + LocalDateTime.now() + " id=" + clientId);
      if (cleanupTask == null || cleanupTask.isCancelled() || cleanupTask.isDone()) 
        startTimer();
    }

    // called from controller /api/logout
    public synchronized void removeUser(int userId) {
      if (userActivityMap.remove(userId) != null) {
        System.out.println(" *** User " + userId + " removed from UserMonitor");
        // If no users remain → stop timer
        if (userActivityMap.isEmpty()) {
          System.out.println(" *** LAST User (" + userId + ") removed from UserMonitor");
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
        cleanupIntervalSeconds,
        cleanupIntervalSeconds,
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
          broadcastWsMessage(wsJson);
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
        Duration idleTimeout = Duration.ofMinutes(idleTimeoutMinutes);

        userActivityMap.entrySet().removeIf(entry -> {
          boolean idle = Duration.between(entry.getValue().getTimeStamp(), now).compareTo(idleTimeout) > 0;
          System.out.println( " *** cleanup curr Interval= " +  Duration.between(entry.getValue().getTimeStamp(), now) );
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
