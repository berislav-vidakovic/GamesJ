package com.gamesj.WebSockets;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.WebSocketSession;

import java.time.Instant;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;


@Component
public class SessionMonitor {

    private final Map<WebSocketSession, Long> lastActivity = Collections.synchronizedMap(new HashMap<>());
    private final WebSocketHandler handler;

    public SessionMonitor(WebSocketHandler handler) {
        this.handler = handler;
    }

    public void updateActivity(WebSocketSession session) {
        lastActivity.put(session, Instant.now().toEpochMilli());
    }

    @Scheduled(fixedRate = 10_000)
    public void checkIdleSessions() {
    long now = Instant.now().toEpochMilli();
    long idleLimit = 60_000; // 1 minute

    synchronized (lastActivity) {
        for (WebSocketSession s : new HashMap<>(lastActivity).keySet()) {
            Long last = lastActivity.get(s);
            if (last != null && now - last > idleLimit) {
                try {
                    System.out.println("===== WS CLOSING: " + s.getId());
                    //handler.broadcast("{\"type\":\"autologout\",\"sessionId\":\"" + s.getId() + "\"}");
                    s.close(CloseStatus.GOING_AWAY);
                    lastActivity.remove(s);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }
}

   
}
