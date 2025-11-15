package com.gamesj.WebSockets;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.*;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import org.springframework.web.util.UriComponentsBuilder;

import com.gamesj.Services.Client;
import com.gamesj.Services.UserMonitor;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class WebSocketHandler extends TextWebSocketHandler {

    @Value("${websocket.idle-timeout-minutes}")
    private long idleTimeoutMinutes;
    
    private final ConcurrentHashMap<WebSocketSession, Client> sessionActivityMap = new ConcurrentHashMap<>();

    @Autowired
    private UserMonitor userMonitor;

    public WebSocketSession getSessionByClientId(UUID clientId) {
      for (Map.Entry<WebSocketSession, Client> entry : sessionActivityMap.entrySet()) {
        WebSocketSession session = entry.getKey();
        Client client = entry.getValue();
        if (client.getClientId().equals(clientId)) 
          return session;
      }
      return null; // not found
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
      UUID clientId = null;
      try {
        String idParam = UriComponentsBuilder.fromUri(session.getUri())
                .build()
                .getQueryParams()
                .getFirst("id");
        clientId = UUID.fromString(idParam);
      } 
      catch (Exception e) {
        System.err.println("Invalid or missing clientId in WebSocket URL: " + session.getUri());
        session.close(CloseStatus.BAD_DATA); // immediately close
        return; // abort further processing
      }
      // valid UUID, proceed
      sessionActivityMap.put(session, new Client( LocalDateTime.now(), clientId) );  
      System.out.println(" *** WS Connected for clientId=" + clientId+ " WS(s): " + sessionActivityMap.size() );
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
      String payload = message.getPayload();
      System.out.println("Received message from " + session.getId() + ": " + payload);
      Client client = sessionActivityMap.get(session);
      client.setTimeStamp();
      //broadcast("Echo: " + payload);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
      Client client = sessionActivityMap.get(session);
      UUID id = client.getClientId();        
      sessionActivityMap.remove(session);
      System.out.println("WebSocket disconnected: " + id);
    }

    // Helper method to broadcast messages from anywhere
    public void broadcast(String message) {
      for (Map.Entry<WebSocketSession, Client> entry : sessionActivityMap.entrySet()) {
        WebSocketSession session = entry.getKey();
        synchronized (session) { //synchronize per session
          if (session.isOpen()) {
            try {
              session.sendMessage(new TextMessage(message));
            } 
            catch (Exception e) {
              e.printStackTrace();
            }
          }
        }
      }      
    }

    public void sendSafe(WebSocketSession session, TextMessage msg) {
    try {
      synchronized (session) {  // required for thread safety
        if (session != null && session.isOpen()) {
          session.sendMessage(msg);
        } else {
          System.err.println("Cannot send WS message: session is closed or null");
        }
      }
    } 
    catch (Exception e) {
      System.err.println("Error in sendSafe:");
      e.printStackTrace();
    }
}


    @Scheduled(fixedRateString = "${websocket.check-interval-ms}")
    public void checkIdleSessions() {
      LocalDateTime now = LocalDateTime.now();
      Duration IDLE_TIMEOUT = Duration.ofMinutes(idleTimeoutMinutes);
      // Check and collect idle WS connections  ...
      HashMap<WebSocketSession, Integer> sessionsToClose = new HashMap<>();
      for (Map.Entry<WebSocketSession, Client> entry : sessionActivityMap.entrySet()) {
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
            sessionActivityMap.remove(session);
            System.out.println("Closed session: " + session.getId());
          } catch (Exception e) {
            e.printStackTrace();
          }
        }
      }
      System.out.println(" *************** WS idle cleanup WS(s): " + sessionActivityMap.size() );

    }
}
