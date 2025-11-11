package com.gamesj.WebSockets;

import org.springframework.stereotype.Component;
import org.springframework.web.socket.*;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

@Component
public class WebSocketHandler extends TextWebSocketHandler {
    private final Set<WebSocketSession> sessions = Collections.synchronizedSet(new HashSet<>());

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        sessions.add(session);
        System.out.println("WebSocket connected: " + session.getId());
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        String payload = message.getPayload();
        System.out.println("Received message from " + session.getId() + ": " + payload);
        broadcast("Echo: " + payload);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        sessions.remove(session);
        System.out.println("WebSocket disconnected: " + session.getId());
    }

    // Helper method to broadcast messages from anywhere
    public void broadcast(String message) {
      synchronized (sessions) {
        for (WebSocketSession s : sessions) {
          if (s.isOpen()) {
            try {
              s.sendMessage(new TextMessage(message));
            } 
            catch (Exception e) {
              e.printStackTrace();
            }
          }
        }
      }
    }
}
