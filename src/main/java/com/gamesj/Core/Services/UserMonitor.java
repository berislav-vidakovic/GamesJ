package com.gamesj.Core.Services;

import java.time.LocalDateTime;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import com.gamesj.Core.Models.User;
import com.gamesj.Core.Repositories.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;


// Spring-managed singleton 
@Service
public class UserMonitor extends IdleMonitor<Integer> {
    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ObjectMapper mapper;

    private final ConcurrentHashMap<Integer, Client> userActivityMap = new ConcurrentHashMap<>();
    public static final int EMPTY_USERID = -1;

    public UserMonitor( 
      @Value("${useridle.timeout-mins}") short idleTimeoutMinutes,
      @Value("${useridle.check-interval-sec}") short cleanupIntervalSeconds ){
        super(idleTimeoutMinutes, cleanupIntervalSeconds);
      System.out.println(" =========== Created UserMonitor ======="); 

    }

    @Override
    protected ConcurrentHashMap<Integer, Client> getActivityMap() {
      return userActivityMap;
    }

    public UUID getClientIdByUserId(int userId){
      Client client = userActivityMap.get(userId);
      if( client != null )
        return client.getClientId();
      return null;
    }

    public int getUserIdByClientId(UUID clientId){
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
      // add or update userId in map  
      userActivityMap.compute(userId, (key, existingClient) -> {
        if (existingClient == null) {
          return new Client(LocalDateTime.now(), clientId);
        } 
        else {
          existingClient.setTimeStamp();
          existingClient.setClientId(clientId);
          return existingClient;
        }
      });
      //System.out.println(" *** *** UserId upd. for clientId=" + clientId + " User(s): " + userActivityMap.size() 
       // + " UserId: " + userId );
      //System.out.println(" *** updateUserActivity " + userId + " @ " + LocalDateTime.now() + " id=" + clientId);
      if (cleanupTask == null || cleanupTask.isCancelled() || cleanupTask.isDone()) {
        //System.out.println(" *** Starting UserMonitor timer ......clientId=" + clientId + ".......User(s): " + userActivityMap.size() );
        startTimer("UserMonitor");
      }
    }

      // called from controller /api/logout
    public synchronized void removeUser(int userId) {
      if (userActivityMap.remove(userId) != null) {
        System.out.println(" *** User " + userId + " removed from UserMonitor");
        // If no users remain → stop timer
        if (userActivityMap.isEmpty()) {
          System.out.println(" *** LAST User (" + userId + ") removed from UserMonitor");
          stopTimer("UserMonitor");
        }
      } 
      else 
        System.out.println(" *** removeUser: user " + userId + " not found in map");
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

          String wsJson = mapper.writeValueAsString(wsMessage);
          broadcastWsMessage(wsJson);
          System.out.println(" *** Broadcasted WS logout for user " + userId);
        }
      }
      catch (Exception ex) {
        System.err.println("Error cleaning idle user " + userId + ": " + ex.getMessage());
        ex.printStackTrace();
      }
    }

    @Override
    public void cleanupIdleItems() {
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
          stopTimer("UserMonitor");
      } 
      catch (Exception e) {
        System.err.println("Fatal error in cleanupIdleUsers: " + e.getMessage());
        e.printStackTrace();
      }
    }    
}
