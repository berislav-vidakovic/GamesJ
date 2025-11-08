package com.gamesj.WebSockets;

import org.springframework.web.socket.*;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class WebSocketHandler extends TextWebSocketHandler {
    // Track connected sessions
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

      // Echo message to all connected clients
      synchronized (sessions) {
        for (WebSocketSession s : sessions) {
          if (s.isOpen()) {
            s.sendMessage(new TextMessage("Echo: " + payload));
          }
        }
      }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
      sessions.remove(session);
      System.out.println("WebSocket disconnected: " + session.getId());
    }
}
