package com.gamesj.Core.Services;

import java.time.LocalDateTime;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.*;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.WebSocketSession;

// Spring-managed singleton 
@Service
public class SessionMonitor extends IdleMonitor<WebSocketSession> {

    @Autowired
    private UserMonitor userMonitor;

    private final ConcurrentHashMap<WebSocketSession, Client> sessionMap = new ConcurrentHashMap<>();

    public SessionMonitor( 
      @Value("${websocket.timeout-mins}") short idleTimeoutMinutes,
      @Value("${websocket.check-interval-sec}") short cleanupIntervalSeconds ){
        super(idleTimeoutMinutes, cleanupIntervalSeconds);
      System.out.println(" =========== Created SessionMonitor ======="); 
    }

    @Override
    public ConcurrentHashMap<WebSocketSession, Client> getActivityMap() {
      return sessionMap;
    }

    public void addSocket(WebSocketSession session , UUID clientId ){
      sessionMap.put(session, new Client( LocalDateTime.now(), clientId) );  
      System.out.println(" *** *** WS Connected for clientId=" + clientId+ " WS(s): " + sessionMap.size() );
      if (cleanupTask == null || cleanupTask.isCancelled() || cleanupTask.isDone()) {
        //System.out.println(" *** Starting SessionMonitor timer ................");
        startTimer("WebSocketSession");
      }
    }

    public void removeSocket(WebSocketSession session ){
      sessionMap.remove(session);
      if( sessionMap.isEmpty() )
        stopTimer("WebSocketSession");
      System.out.println(" *** *** WS Disconnected WS(s): " + sessionMap.size() );
    }

    public void updateSessionActivity(WebSocketSession session) {
      Client client = sessionMap.get(session);
      client.setTimeStamp();
    }
   
    @Override
    public void cleanupIdleItems() {
      LocalDateTime now = LocalDateTime.now();
      Duration IDLE_TIMEOUT = Duration.ofMinutes(idleTimeoutMinutes);
      // Check and collect idle WS connections  ...
      HashMap<WebSocketSession, Integer> sessionsToClose = new HashMap<>();
      for (Map.Entry<WebSocketSession, Client> entry : sessionMap.entrySet()) {
        WebSocketSession session = entry.getKey();
        Client client = entry.getValue();
        System.out.println("===== WS in checkIdleSessions clientId=" + client.getClientId() );
        if (client == null || client.getTimeStamp() == null) 
          continue;
        UUID id = client.getClientId();
        if (Duration.between(client.getTimeStamp(), now).compareTo(IDLE_TIMEOUT) > 0) {
          try {
            System.out.println("===== WS CLOSING due to inactivity: clientId=" + client.getClientId() );            
            int userId = userMonitor.getUserIdByClientId(id);
            System.out.println("===== UserId= " + userId );                   
            sessionsToClose.put(session, userId);            
          } 
          catch (Exception e) {
            e.printStackTrace();
          }
        }
      }
      // AutoLogout ...
      for (Map.Entry<WebSocketSession, Integer> entry : sessionsToClose.entrySet()) {
        int userId  = entry.getValue();
        try {
          if( userId != UserMonitor.EMPTY_USERID ){
            System.out.println("===== WS CLOSING - AutoLogout ...." );
            userMonitor.autoLogout(userId);
            userMonitor.removeUser(userId);
          }     
        } catch (Exception e) {
          e.printStackTrace();
        }
      }
      // Close WS connections ...
      for (Map.Entry<WebSocketSession, Integer> entry : sessionsToClose.entrySet()) {
        WebSocketSession session = entry.getKey();
        if (session.isOpen()) {
          try {
            session.close(CloseStatus.GOING_AWAY);
            sessionMap.remove(session);
            System.out.println("Closed session: " + session.getId());
          } catch (Exception e) {
            e.printStackTrace();
          }
        }
      }
      if( sessionMap.isEmpty() )
        stopTimer("WebSocketSession");
      System.out.println(" *************** WS idle cleanup WS(s): " + sessionMap.size() );
    }    
}
