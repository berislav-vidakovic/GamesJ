package com.gamesj.WebSockets;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.*;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.time.Instant;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@Component
public class WebSocketHandler extends TextWebSocketHandler {
    private final Set<WebSocketSession> sessions = Collections.synchronizedSet(new HashSet<>());
    private final Map<WebSocketSession, Long> lastActivity = Collections.synchronizedMap(new HashMap<>());

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        sessions.add(session);
        lastActivity.put(session, Instant.now().toEpochMilli());
        System.out.println("WebSocket connected: " + session.getId());
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        String payload = message.getPayload();
        System.out.println("Received message from " + session.getId() + ": " + payload);
        lastActivity.put(session, Instant.now().toEpochMilli());
        broadcast("Echo: " + payload);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        sessions.remove(session);
        lastActivity.remove(session);
        System.out.println("WebSocket disconnected: " + session.getId());
    }

    // Helper method to broadcast messages from anywhere
    public void broadcast(String message) {
      synchronized (sessions) {
        for (WebSocketSession s : sessions) {
          if (s.isOpen()) {
            try {
              s.sendMessage(new TextMessage(message));
              lastActivity.put(s, Instant.now().toEpochMilli());
            } 
            catch (Exception e) {
              e.printStackTrace();
            }
          }
        }
      }
    }

    // Scheduled task to close idle sessions
    @Scheduled(fixedRate = 10_000)
    public void checkIdleSessions() {
      long now = Instant.now().toEpochMilli();
      long idleLimit = 5*60*1000; // millisec

      synchronized (lastActivity) {
        for (WebSocketSession s : new HashSet<>(lastActivity.keySet())) {
          Long last = lastActivity.get(s);
          if (last != null && now - last > idleLimit) {
            try {
              //broadcast("{\"type\":\"autologout\",\"sessionId\":\"" + s.getId() + "\"}");
              s.close(CloseStatus.GOING_AWAY);
              lastActivity.remove(s);
              sessions.remove(s);
              System.out.println("===== WS CLOSING due to inactivity: " + s.getId());
            } catch (Exception e) {
              e.printStackTrace();
            }
          }
        }
      }
    }

}
