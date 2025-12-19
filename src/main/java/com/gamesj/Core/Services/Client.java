package com.gamesj.Core.Services;

import java.time.LocalDateTime;
import java.util.UUID;

public class Client {
    private LocalDateTime timeStamp;
    private UUID clientId;

    public Client( LocalDateTime timeStamp, UUID clientId ) {
      this.timeStamp = timeStamp;
      this.clientId = clientId;
    }

    public void setTimeStamp() {
      this.timeStamp = LocalDateTime.now();
    }

    public LocalDateTime getTimeStamp() {
      return this.timeStamp; 
    }

    public UUID getClientId() {
      return this.clientId; 
    }
    
    public void setClientId(UUID clientId) {
      this.clientId = clientId; 
    }    
}
