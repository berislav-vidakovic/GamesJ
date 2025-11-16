package com.gamesj.Controllers;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gamesj.Services.Game;
import com.gamesj.Services.GameManager;
import com.gamesj.Services.Player;
import com.gamesj.Services.UserMonitor;
import com.gamesj.WebSockets.WebSocketHandler;
import com.gamesj.Repositories.RefreshTokenRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Map;
import java.util.UUID;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

@RestController
@RequestMapping("/api/invitations")
public class InviteController {
  @Autowired
  private GameManager gameManager;

  @Autowired
  private PasswordEncoder passwordEncoder;

  @Autowired
  private RefreshTokenRepository refreshTokenRepository;

  @Autowired
  private WebSocketHandler webSocketHandler;

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
        return ResponseEntity.badRequest().body(
              Map.of("acknowledged", false, "error", "Missing clientId") );
      }
      System.out.println("Received POST /api/invitations/invite with valid clientId");
      if (!body.has("callerId") || !body.has("calleeId")) {
          return ResponseEntity.badRequest().body(
              Map.of("acknowledged", false, "error", "No both callerId, calleeId specified")
          );
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

      // Test start
      if ( invitation.equals("send") ){
        gameManager.removeAll();
      }
      // Test end
      Game game = gameManager.getGame(callerId, calleeId);
      if ( invitation.equals("send") ){
        if( game != null  ) 
          return ResponseEntity.badRequest().body(
                Map.of("acknowledged", false, "error", "Players already play another game")); 
        gameManager.createGame(selectedGame, player1, player2);
        System.out.println(" Created Game for Invitation from " + callerId + " to " + calleeId );
      }
      else if ( invitation.equals("accept") ){
        if( game == null || game.getState() != Game.STATE_INVITE ) 
          return ResponseEntity.badRequest().body(
                Map.of("acknowledged", false, "error", "Error in pairing players")); 
        gameManager.pairPlayers(game);
        System.out.println(" Accepted Game Invitation from " + callerId + " to " + calleeId );
      }
      else {
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
      if (invitation.equals("send") && calleeSession != null && calleeSession.isOpen()) {
        ObjectMapper mapper = new ObjectMapper();
        String json = mapper.writeValueAsString(msg);
        System.out.println("Sending WS: " + json);
        calleeSession.sendMessage(new TextMessage(json));
      }
      else if (invitation.equals("accept") && callerSession != null && callerSession.isOpen()) {
        ObjectMapper mapper = new ObjectMapper();
        String json = mapper.writeValueAsString(msg);
        System.out.println("Sending WS: " + json);
        callerSession.sendMessage(new TextMessage(json));
      }
      else if (invitation.equals("cancel") && calleeSession != null && calleeSession.isOpen()) {
        ObjectMapper mapper = new ObjectMapper();
        String json = mapper.writeValueAsString(msg);
        System.out.println("Sending WS: " + json);
        calleeSession.sendMessage(new TextMessage(json));
      }
      else if (invitation.equals("reject") && callerSession != null && callerSession.isOpen()) {
        ObjectMapper mapper = new ObjectMapper();
        String json = mapper.writeValueAsString(msg);
        System.out.println("Sending WS: " + json);
        callerSession.sendMessage(new TextMessage(json));
      }


      return new ResponseEntity<>(response, HttpStatus.OK); // 200
    } 
    catch (Exception ex) {
      System.out.println("Error in Post Invite Received: " + ex.getMessage());
      return ResponseEntity.status(500).body(
              Map.of("acknowledged", false, "error", ex.getMessage())
      );
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
