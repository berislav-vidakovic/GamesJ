package com.gamesj.API.REST;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gamesj.Core.Services.Game;
import com.gamesj.Core.Services.GameManager;
import com.gamesj.Core.Services.Player;
import com.gamesj.Core.Services.UserMonitor;
import com.gamesj.API.WebSocket.WebSocketHandler;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Map;
import java.util.UUID;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

// POST /invite
// POST /accept
// POST /cancel
// POST /reject
@RestController
@RequestMapping("/api/invitations")
public class InviteController {
  @Autowired
  private GameManager gameManager;

  @Autowired
  private WebSocketHandler webSocketHandler;

  @Autowired
  private ObjectMapper mapper;

  @Autowired
  private UserMonitor userMonitor;

  public InviteController(UserMonitor userMonitor, WebSocketHandler webSocketHandler) {
      this.userMonitor = userMonitor;
      this.webSocketHandler = webSocketHandler;
  }

  public ResponseEntity<?> handleRequestSendWs(JsonNode body, String invitation, String clientId) {
    // Request { "callerId": int, "calleeId": int, "selectedGame": "panel.game.connect4" } 
    try {
      UUID parsedClientId;
      try {
        parsedClientId = UUID.fromString(clientId);
      } 
      catch (Exception e) {
        return new ResponseEntity<>(
          Map.of("acknowledged", false, "error", "Missing clientId"), 
          HttpStatus.BAD_REQUEST);
      }
      System.out.println("Received POST /api/invitations/invite with valid clientId");
      if (!body.has("callerId") || !body.has("calleeId")) {
        return new ResponseEntity<>(
          Map.of("acknowledged", false, "error", "No both callerId, calleeId specified"),
          HttpStatus.BAD_REQUEST );
      }
      System.out.println("Received POST /api/invitations/invite with valid callerId and calleeId");
      int callerId = body.get("callerId").asInt();
      int calleeId = body.get("calleeId").asInt();

      String selectedGame = body.has("selectedGame") ? body.get("selectedGame").asText() : null;
      // "panel.game.connect4"
      
      UUID callerClientId = userMonitor.getClientIdByUserId(callerId);
      System.out.println(" Caller ClientId : " + callerClientId + " Parsed ClientId : " + parsedClientId  );
      WebSocketSession callerSession = webSocketHandler.getSessionByClientId( callerClientId);
      Player player1 = new Player( callerId, callerClientId, callerSession );

      UUID calleeClientId = userMonitor.getClientIdByUserId(calleeId);  
      WebSocketSession calleeSession = webSocketHandler.getSessionByClientId( calleeClientId);
      Player player2 = new Player( calleeId, calleeClientId, calleeSession );

      Game game = gameManager.getGame(callerId, calleeId);
      if ( invitation.equals("send") ){
        if( game != null  ) 
          gameManager.removeGame(game);
        gameManager.createGame(selectedGame, player1, player2);
        System.out.println(" Created Game for Invitation from " + callerId + " to " + calleeId );
      }
      else if ( invitation.equals("accept") ){
        if( game == null || game.getState() != Game.STATE_INVITE ) 
          return new ResponseEntity<>(
            Map.of("acknowledged", false, "error", "Error in pairing players"),
            HttpStatus.BAD_REQUEST); 
        gameManager.pairPlayers(game);
        System.out.println(" Accepted Game Invitation from " + callerId + " to " + calleeId );
      }
      else { // cancel or reject
        gameManager.removeGame(game);
      }

      Map<String, Object> response = Map.of(
          "invitation", invitation,
          "callerId", callerId,
          "calleeId", calleeId,
          "selectedGame", selectedGame
      );

      Map<String, Object> msg = Map.of(
          "type", "invitation",
          "status", "WsStatus.OK",
          "data", response
      );
      // send and cancel are sent by caller / accept and reject are sent by callee      
      if (invitation.equals("send") )
        sendWsMessageToSession(calleeSession, msg); 
      else if (invitation.equals("accept") )
        sendWsMessageToSession(callerSession, msg);
      else if (invitation.equals("cancel") )
        sendWsMessageToSession(calleeSession, msg);
      else if (invitation.equals("reject") )
        sendWsMessageToSession(callerSession, msg);

      return new ResponseEntity<>(response, HttpStatus.OK); // 200
    } 
    catch (Exception ex) {
      System.out.println("Error in Post Invite Received: " + ex.getMessage());      
      return new ResponseEntity<>(
        Map.of( "acknowledged", false, "error", ex.getMessage() ), 
        HttpStatus.INTERNAL_SERVER_ERROR);      
    }
  }

  private void sendWsMessageToSession(WebSocketSession session, Map<String, Object> msg) {
    try {
      if (session != null && session.isOpen()) {
        String json = mapper.writeValueAsString(msg);
        System.out.println("Sending WS: " + json);
        webSocketHandler.sendWsMessage( session, new TextMessage(json));
      }
    } 
    catch (Exception ex) {
      System.out.println("Error sending WS message: " + ex.getMessage());
    }
  }

  // Request { callerId, calleeId, selectedGame }  
  // Response { "invitation": "send" | "cancel" | "accept" | "reject", callerId, calleeId, selectedGame }
  @PostMapping("/invite")
  public ResponseEntity<?> postInvitationSend(
          @RequestBody JsonNode body,
          @RequestParam(name = "id") String clientId) {

    System.out.println("POST /api/invitations/invite called");

    ResponseEntity<?> res = handleRequestSendWs(body, "send", clientId);
    return res;
  }

  @PostMapping("/accept")
  public ResponseEntity<?> postInvitationAccept(
          @RequestBody JsonNode body,
          @RequestParam(name = "id") String clientId) {

    System.out.println("POST /api/invitations/accept called");

    ResponseEntity<?> res = handleRequestSendWs(body, "accept", clientId);
    return res;
  }

  @PostMapping("/cancel")
  public ResponseEntity<?> postInvitationCancel(
          @RequestBody JsonNode body,
          @RequestParam(name = "id") String clientId) {

    System.out.println("POST /api/invitations/cancel called");

    ResponseEntity<?> res = handleRequestSendWs(body, "cancel", clientId);
    return res;
  }

  @PostMapping("/reject")
  public ResponseEntity<?> postInvitationReject(
          @RequestBody JsonNode body,
          @RequestParam(name = "id") String clientId) {

    System.out.println("POST /api/invitations/reject called");

    ResponseEntity<?> res = handleRequestSendWs(body, "reject", clientId);
    return res;
  }
}
