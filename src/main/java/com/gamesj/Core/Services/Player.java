package com.gamesj.Core.Services;

import java.util.UUID;

import org.springframework.web.socket.WebSocketSession;

public class Player {
  int userId;
  UUID clientId;
  WebSocketSession session;

  public Player(int userId, UUID clientId, WebSocketSession session){
    this.userId = userId;
    this.clientId = clientId;
    this.session = session;
  }  

  public int getUserId(){
    return this.userId;
  }

  public void setClientId(UUID clientId){
    this.clientId = clientId;
  }

  public UUID getClientId(){
    return this.clientId;
  }
}
