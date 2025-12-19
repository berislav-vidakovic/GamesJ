package com.gamesj.API.WebSocket;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.*;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import org.springframework.web.util.UriComponentsBuilder;
import com.gamesj.Core.Services.Client;
import com.gamesj.Core.Services.SessionMonitor;
import java.util.Map;
import java.util.UUID;

@Component
public class WebSocketHandler extends TextWebSocketHandler {

    @Autowired
    private SessionMonitor sessionMonitor;

    public WebSocketSession getSessionByClientId(UUID clientId) {
      for (Map.Entry<WebSocketSession, Client> entry : sessionMonitor.getActivityMap().entrySet()) {
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
      sessionMonitor.addSocket(session, clientId);  
      System.out.println(" *** WS Connected for clientId=" + clientId );
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
      String payload = message.getPayload();
      System.out.println("Received message from " + session.getId() + ": " + payload);
      sessionMonitor.updateSessionActivity(session);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
      sessionMonitor.removeSocket(session);
      System.out.println("WebSocket disconnected: ");
    }

    // Helper method to broadcast messages from anywhere
    public void broadcast(String message) {
      for (Map.Entry<WebSocketSession, Client> entry : sessionMonitor.getActivityMap().entrySet()) {
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

    public void sendWsMessage(WebSocketSession session, TextMessage msg) {
      try {
        synchronized (session) {  // required for thread safety
        if (session != null && session.isOpen()) 
          session.sendMessage(msg);
        else 
          System.err.println("Cannot send WS message: session is closed or null");
        }
      } 
      catch (Exception e) {
        System.err.println("Error in sendSafe:");
        e.printStackTrace();
      } 
    }   
}
