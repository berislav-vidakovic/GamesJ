package com.gamesj.Core.Services;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.gamesj.API.WebSocket.WebSocketHandler;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;


// Spring-managed singleton 
@Service
public class WebSocketService {
    @Autowired
    private WebSocketHandler webSocketHandler;

    @Autowired
    private ObjectMapper mapper;

    public void broadcastMessage(String type, String status, Map<String, Object> content) {
      try{
        // Build WS message as Map 
        Map<String, Object> wsMessage = Map.of(
          "type", type,
          "status", status,
          "data", content
        );
        // Convert Map to JSON string
        String wsJson = mapper.writeValueAsString(wsMessage);

        // Broadcast via WebSocket
        webSocketHandler.broadcast(wsJson);
      } 
      catch (JsonProcessingException e) {
        e.printStackTrace();
      }
    }
}