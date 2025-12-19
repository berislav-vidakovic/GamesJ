package com.gamesj.Core.Services;
import java.util.UUID;

public class Game {
  private final java.util.UUID gameId;
  public Player player1;
  public Player player2;

  public static final int STATE_ERROR = -1;
  public static final int STATE_INVITE = 0;
  public static final int STATE_PAIRED = 1;
  public static final int STATE_RUN1 = 2;
  public static final int STATE_READY = 3;
  public static final int STATE_RUNNING = 4;
  public static final int STATE_EVAL = 5;
  public static final int STATE_OVER = 6;

  private int gameState;

  public Game(UUID gameId, Player player1, Player player2){
    this.gameId = gameId;
    this.player1 = player1;
    this.player2 = player2;
    this.gameState = STATE_INVITE;
  }
  
  public UUID getGameId(){
    return this.gameId;
  }

  public UUID getPartnerGuid(int userId){
    if (getPlayer1UserId() == userId) 
      return player2.getClientId();
    else if (getPlayer2UserId() == userId) 
      return player1.getClientId();
    
    return null;
  }

  public UUID getUserGuid(int userId){
    return  getPlayer1UserId() == userId 
          ? player1.getClientId()
          : player2.getClientId();
  }

  public int getPartner(int userId){
    if (getPlayer1UserId() == userId) 
      return player2.getUserId();
    else 
      return player1.getUserId();    
  }

  public void setPlayerClientId(int userId, UUID clientId){
    if (player1.userId == userId) {
      player1.setClientId(clientId);
    } else if (player2.userId == userId) {
      player2.setClientId(clientId);
    }
  }

  public boolean isReady(){
    return player1.userId != UserMonitor.EMPTY_USERID && player2.userId != UserMonitor.EMPTY_USERID
     && player1.session != null && player2.session != null 
     && player1.clientId != null && player2.clientId != null;
  }

  public void setState(int state){
    this.gameState = state;
  }

  public int getState(){
    return this.gameState;
  }

  public int getPlayer1UserId(){
    return this.player1.getUserId();
  }

  public int getPlayer2UserId(){
    return this.player2.getUserId();
  }   

  public boolean involvesUsers(int user1Id, int user2Id){
    return ( getPlayer1UserId() == user1Id && getPlayer2UserId() == user2Id ) ||
           ( getPlayer1UserId() == user2Id && getPlayer2UserId() == user1Id );
  }
}
