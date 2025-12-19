package com.gamesj.Core.Services;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.*;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import com.gamesj.API.WebSocket.WebSocketHandler;
import com.fasterxml.jackson.databind.ObjectMapper;

// Spring-managed singleton 
@Service
public class GameManager {
  private final ConcurrentHashMap<UUID, Game> games = new ConcurrentHashMap<>();

  private final ConcurrentHashMap<UUID, ScheduledFuture<?>> gameTimeoutTasks = new ConcurrentHashMap<>();


  private final ScheduledExecutorService scheduler =
        Executors.newScheduledThreadPool(1);

  @Value("${invitation.timeout-sec}")
  private int invitationTimeoutSeconds;

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

  public UUID getGameId(int callerId, int calleeId) {
    Game game = getGame(callerId, calleeId);
    if (game != null) 
      return game.getGameId();
    return null;
  }

  public void removeAll() {
    games.clear();
    System.out.println(" *** Removed ALL Games. Total Games  : " + games.size() );
    // Cancel all scheduled tasks
    for (ScheduledFuture<?> future : gameTimeoutTasks.values()) {
        future.cancel(false);
    }
    gameTimeoutTasks.clear();
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
        newGame = new GameConnect4(gameId, player1, player2);
        break;
      default:
        System.out.println("Unsupported game type: " + gameType);
    } 
    if (newGame != null){
      games.put(gameId, newGame);
      System.out.println(" *** Created Game with gameId=" + gameId + " Total Games  : " + games.size() );
      scheduleGameExpiration(newGame);
    }
    return newGame;
  }
  
  public Game getGameById(UUID gameId){
    return games.get(gameId);
  }

  public int getPartnerId(UUID gameId, int userId){
    Game game = games.get(gameId);
    if (game != null) {
      if (game.getPlayer1UserId() == userId) {
        return game.getPlayer2UserId();
      } else if (game.getPlayer2UserId() == userId) {
        return game.getPlayer1UserId();
      }
    }
    return UserMonitor.EMPTY_USERID;
  }

  public boolean isValidStateForRunAction(UUID gameId){
    Game game = games.get(gameId);
    if (game == null) 
      return false;
    int state = game.getState(); 
    return state == Game.STATE_PAIRED || state == Game.STATE_RUN1;
  }

  public void updateUserActivity(Game game) {
    if (game != null) {
      userMonitor.updateUserActivity(game.getPlayer1UserId(), game.player1.getClientId());
      userMonitor.updateUserActivity(game.getPlayer2UserId(), game.player2.getClientId());
    }
  }
  
  public boolean isGameStateReady(UUID gameId){
    Game game = games.get(gameId);
    if (game == null) 
      return false;
    int state = game.getState(); 
    return state == Game.STATE_READY;
  }

  public void setGameState( Game game, int newState ){
    //System.out.println("### Setting Game State ...");
    if (game != null) {
      //System.out.println("### Setting Game State to " + newState + " for gameId=" + game.getGameId() );
      game.setState(newState);
    }
  }

  public int getGameState( Game game ){
    //System.out.println("### Getting Game State ...");
    if (game != null) {
      //System.out.println("### Getting Game State for gameId=" + game.getGameId() );
      return game.getState();
    }
    return Game.STATE_ERROR;
  }

  public boolean updateStateOnRunAction(UUID gameId){
    Game game = games.get(gameId);
    //System.out.println(" ### updateStateOnRunAction for gameId=" + gameId );
    if (game == null) 
      return false;
    if( game.getState() == Game.STATE_PAIRED ){
      game.setState(Game.STATE_RUN1);
      return true;
    }
    if( game.getState() == Game.STATE_RUN1 ) {
      game.setState(Game.STATE_READY);
      return true;
    }
    return false;
  }

  public void setUserGuid(UUID gameId, int userId, UUID clientId){
    Game game = games.get(gameId);
    if (game != null) {
      if (game.getPlayer1UserId() == userId) {
        game.player1.setClientId(clientId);
      } else if (game.getPlayer2UserId() == userId) {
        game.player2.setClientId(clientId);
      }
    }
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
      webSocketHandler.sendWsMessage(session, new TextMessage(json));
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
          sendTimeoutMessage(session1);        
        WebSocketSession session2 = getSessionByUserId(game.getPlayer2UserId());
        if (session2 != null && session2.isOpen()) 
          sendTimeoutMessage(session2);
      }
      }, invitationTimeoutSeconds, TimeUnit.SECONDS);
    
    gameTimeoutTasks.put(game.getGameId(), future);
  }
}
