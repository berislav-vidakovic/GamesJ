package com.gamesj.Services;

import java.time.LocalDateTime;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.*;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import com.gamesj.Models.User;
import com.gamesj.Repositories.UserRepository;
import com.gamesj.WebSockets.WebSocketHandler;
import com.fasterxml.jackson.databind.ObjectMapper;

// Spring-managed singleton 
@Service
public class GameManager {
  private final ConcurrentHashMap<UUID, Game> games = new ConcurrentHashMap<>();

  private final ConcurrentHashMap<UUID, ScheduledFuture<?>> gameTimeoutTasks = new ConcurrentHashMap<>();


  private final ScheduledExecutorService scheduler =
        Executors.newScheduledThreadPool(1);

  private final long GAME_EXPIRE_SECONDS = 5; 

  @Autowired
  private ObjectMapper mapper;

  // wire socket handler
  @Autowired
  private WebSocketHandler webSocketHandler;

  // wire user monitor  
  @Autowired
  private UserMonitor userMonitor;

  WebSocketSession getSessionByUserId(int userId) {
    UUID clientId = userMonitor.getClientIdByUserId(userId);
    if (clientId != null) {
      return webSocketHandler.getSessionByClientId(clientId);
    }
    return null; // not found
  }

  public GameManager() {
    System.out.println(" =========== Created GameManager ======="); 
  }

  public Game getGame(int callerId, int calleeId) {
    for (Game game : games.values()) 
      if( game.involvesUsers(callerId, calleeId))
        return game;
    return null;
  }

  public void removeGame( Game game ) {
    if( game != null ){
      games.remove(game.getGameId());
      System.out.println(" *** Removed Game with gameId=" + game.getGameId() + " Total Games  : " + games.size() );

      ScheduledFuture<?> future = gameTimeoutTasks.remove(game.getGameId());
      if (future != null) {
          future.cancel(false);
      }
    }
  }

  public Game createGame(String gameType, Player player1, Player player2) {
    UUID gameId = UUID.randomUUID();
    Game newGame = null;

    switch(gameType) {
      case "panel.game.connect4":
        newGame = new Game(gameId, player1, player2);
        break;
      default:
        System.out.println("Unsupported game type: " + gameType);
    } 
    if (newGame != null){
      games.put(gameId, newGame);
      System.out.println(" *** Created Game with gameId=" + gameId + " Total Games  : " + games.size() );
      // TO DO timer start
      scheduleGameExpiration(newGame);
    }

    return newGame;
  }    

  public void pairPlayers(Game game) {
    if (game != null) {
      game.setState(Game.STATE_PAIRED);

      // Cancel the timeout task
      ScheduledFuture<?> future = gameTimeoutTasks.remove(game.getGameId());
      if (future != null) {
          future.cancel(false);
          System.out.println("### Timer canceled for paired gameId=" + game.getGameId());
      }
    }
  }

  private void sendTimeoutMessage(WebSocketSession session){
    System.out.println("*** Sending timeout message to session " + session.getId());
    Map<String, Object> response = new HashMap<>();
    response.put("invitation", "timeout");
    response.put("callerId", null);
    response.put("calleeId", null);
    response.put("selectedGame", "");
    // From the JDK docs: "All keys and values in Map.of must be non-null.
    // If a null key or value is used, a NullPointerException is thrown."
   
    Map<String, Object> msg = Map.of(
        "type", "invitation",
        "status", "WsStatus.OK",
        "data", response
    );
    try{
      String json = mapper.writeValueAsString(msg);
      System.out.println("Sending WS: " + json);
      webSocketHandler.sendSafe(session, new TextMessage(json));
    } 
    catch (Exception e){
      System.err.println("Error sending timeout message: " + e.getMessage());
      e.printStackTrace();
    }
  } 

  private void scheduleGameExpiration(Game game) {
    ScheduledFuture<?> future = scheduler.schedule(() -> {
      // Only remove if still present (i.e., not already finished)
      if (games.containsKey(game.getGameId())) {
        System.out.println("### Game timer expired for gameId=" + game.getGameId());
        removeGame(game);
        WebSocketSession session1 = getSessionByUserId(game.getPlayer1UserId());
        if (session1 != null && session1.isOpen()) 
          //System.out.println(" ### Session 1 is ready");  
          sendTimeoutMessage(session1);        
        WebSocketSession session2 = getSessionByUserId(game.getPlayer2UserId());
        if (session2 != null && session2.isOpen()) 
          sendTimeoutMessage(session2);
          //System.out.println(" ### Session 2 is ready");  
      }
      }, GAME_EXPIRE_SECONDS, TimeUnit.SECONDS);
    
    gameTimeoutTasks.put(game.getGameId(), future);
  }

}
