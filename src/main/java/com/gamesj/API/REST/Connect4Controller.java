package com.gamesj.API.REST;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gamesj.Core.Services.Game;
import com.gamesj.Core.Services.GameConnect4;
import com.gamesj.Core.Services.GameManager;
import com.gamesj.API.WebSocket.WebSocketHandler;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Map;
import java.util.UUID;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

// POST /init
// POST /swapcolors
// POST /start
// POST /insertdisk
// POST /newgame
@RestController
@RequestMapping("/api/games/connect4")
public class Connect4Controller {
  @Autowired
  private GameManager gameManager;

  @Autowired
  private WebSocketHandler webSocketHandler;

  @Autowired
  private ObjectMapper mapper;

  // POST /api/games/connect4/init - Request sent from Game browser
  // Req: { gameId, userId } Auth Bearer: accessToken
  // Resp: { color }
  @PostMapping("/init")
  public ResponseEntity<?> postInitConnect4(@RequestBody Map<String, Object> body) {
    try {
      // Validate gameId
      if (!body.containsKey("gameId") || !body.containsKey("userId")) {
        return ResponseEntity.badRequest().body(Map.of(
          "acknowledged", false,
          "error", "Missing keys gameId and/or userId in POST request"
        ));
      }

      String gameId = body.get("gameId").toString();

      if (!gameManager.updateStateOnRunAction(UUID.fromString(gameId)))
        return ResponseEntity.badRequest().body(Map.of(
          "acknowledged", false,
          "error", "Invalid gameId in POST request or wrong game State for Run action"
        ));

      GameConnect4 gameC4 = (GameConnect4) gameManager.getGameById(UUID.fromString(gameId));
      if (gameC4 == null) {
        return ResponseEntity.badRequest().body(Map.of(
          "acknowledged", false,
          "error", "Invalid Game type in POST request"
        ));
      }
      gameManager.updateUserActivity(gameC4);
      int userId = Integer.parseInt(body.get("userId").toString());
      String color = gameC4.getUserColor(userId);
      return ResponseEntity.status(HttpStatus.OK).body(Map.of("color", color)); // 200  
    }
    catch (Exception ex) {
      System.out.println("Error in Post Init Received: " + ex.getMessage());
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
        "acknowledged", false,
        "error", ex.getMessage()
      ));
    }
  }
  
  // POST /api/games/connect4/swapcolors - Request sent from Game browser
  // Req: { gameId, userId } Auth Bearer: accessToken
  // Resp: { color }
  @PostMapping("/swapcolors")
  public ResponseEntity<?> postSwapColors(@RequestBody Map<String, Object> body) {
    try {
      if (!body.containsKey("gameId") || !body.containsKey("userId")) {
        return ResponseEntity.badRequest().body(Map.of(
          "acknowledged", false,
          "error", "Missing keys gameId and/or userId in POST request"
        ));
      }

      String gameId = body.get("gameId").toString();
      int userId = Integer.parseInt(body.get("userId").toString());

      // Validate game state -------------------------------------------------------
      if (!gameManager.isGameStateReady(UUID.fromString(gameId))) {
          return ResponseEntity.badRequest().body(Map.of(
              "acknowledged", false,
              "error", "Invalid gameId in POST request or wrong game State for SwapColors action"
          ));
      }

      GameConnect4 gameC4 = (GameConnect4) gameManager.getGameById(UUID.fromString(gameId));
      if (gameC4 == null) {
          return ResponseEntity.badRequest().body(Map.of(
              "acknowledged", false,
              "error", "Invalid Game type in POST request"
          ));
      }
      gameManager.updateUserActivity(gameC4);

      gameC4.swapColors();
      String color1 = gameC4.getUserColor(userId);
      String color2 = gameC4.getPartnerColor(userId);

      UUID partnerGuid = gameC4.getPartnerGuid(userId);
      WebSocketSession partnerSession = webSocketHandler.getSessionByClientId( partnerGuid );

      Map<String, Object> wsMsg = Map.of(
          "type", "swapColors",
          "status", "WsStatus.OK",
          "data", Map.of("color", color2)
      );       

      if ( partnerSession != null && partnerSession.isOpen()) {
        String json = mapper.writeValueAsString(wsMsg);
        System.out.println("Sending WS: " + json);
        webSocketHandler.sendWsMessage(partnerSession, new TextMessage(json));
      }

      return new ResponseEntity<>(Map.of("color", color1), HttpStatus.OK); // 200
    }
    catch (Exception ex) {
      System.out.println("Error in PostSwapColors: " + ex.getMessage());
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
        "acknowledged", false,
        "error", ex.getMessage()
      ));
    }
    
  }

  // POST /api/games/connect4/start - Request sent from Game browser
  // Req: { gameId, userId } Auth Bearer: accessToken
  // Resp: { userId, board } - userId with first move (Red)
  @PostMapping("/start")
  public ResponseEntity<?> postStartGame(@RequestBody Map<String, Object> body){
    try {
      if (!body.containsKey("gameId") || !body.containsKey("userId")) {
        return ResponseEntity.badRequest().body(Map.of(
          "acknowledged", false,
          "error", "Missing keys gameId and/or userId in POST request"
        ));
      }
      String gameId = body.get("gameId").toString();
      int userId = Integer.parseInt(body.get("userId").toString());
      if (!gameManager.isGameStateReady(UUID.fromString(gameId))) {
        return ResponseEntity.badRequest().body(Map.of(
          "acknowledged", false,
          "error", "Invalid gameId in POST request"
        ));
      }
      GameConnect4 gameC4 = (GameConnect4) gameManager.getGameById(UUID.fromString(gameId));
      if (gameC4 == null) {
        return ResponseEntity.badRequest().body(Map.of(
          "acknowledged", false,
          "error", "Invalid Game type in POST request"
        ));
      }

      gameManager.setGameState( gameC4, Game.STATE_RUNNING );
      gameManager.updateUserActivity(gameC4);
      UUID partnerGuid = gameC4.getPartnerGuid(userId);
      WebSocketSession partnerSession = webSocketHandler.getSessionByClientId( partnerGuid );

      if (!gameC4.getUserColor(userId).equals("Red")) {
          userId = gameC4.getPartner(userId);
      }
      String board = gameC4.getBoard();
      Map<String, Object> response = Map.of(
        "userId", userId,
        "board", board
      );
      Map<String, Object> wsMsg = Map.of(
        "type", "startGame",
        "status", "WsStatus.OK",
        "data", response
      );

      if ( partnerSession != null && partnerSession.isOpen()) {
        String json = mapper.writeValueAsString(wsMsg);
        System.out.println("Sending WS: " + json);
        webSocketHandler.sendWsMessage( partnerSession, new TextMessage(json));
      }
      else 
         return ResponseEntity.status(HttpStatus.ACCEPTED).body(response);

      return new ResponseEntity<>(response, HttpStatus.OK); // 200
    } 
    catch (Exception ex) {
      System.out.println("Error in PostStartGame: " + ex.getMessage());
      return ResponseEntity.status(500).body(Map.of(
        "acknowledged", false,
        "error", ex.getMessage()
      ));
    }
  }

  // POST /api/games/connect4/insertdisk - Request sent from Game browser
  // Req: { gameId, userId, row, col } Auth Bearer: accessToken
  // Resp: { userId, board, state }
  @PostMapping("/insertdisk")
  public ResponseEntity<?> postInsertDisk(@RequestBody Map<String, Object> body){
    try {
      if (!body.containsKey("gameId") || !body.containsKey("userId")) {
        return ResponseEntity.badRequest().body(Map.of(
          "acknowledged", false,
          "error", "Missing keys gameId and/or userId in POST request"
        ));
      }
      String gameId = body.get("gameId").toString();
      int userId = Integer.parseInt(body.get("userId").toString());

      GameConnect4 gameC4 = (GameConnect4) gameManager.getGameById(UUID.fromString(gameId));
      if (gameC4 == null) {
        return ResponseEntity.badRequest().body(Map.of(
          "acknowledged", false,
          "error", "Invalid Game ID in POST request"
        ));
      }
      if ( gameManager.getGameState( gameC4 ) != Game.STATE_RUNNING ) {
        return ResponseEntity.badRequest().body(Map.of(
          "acknowledged", false,
          "error", "Invalid Game state for Insert Disk action"
        ));
      }
      gameManager.updateUserActivity(gameC4);

      int row = Integer.parseInt(body.get("row").toString());
      int col = Integer.parseInt(body.get("col").toString());

      UUID partnerGuid = gameC4.getPartnerGuid(userId);
      WebSocketSession partnerSession = webSocketHandler.getSessionByClientId( partnerGuid );

      gameC4.insertDisk(userId, row, col);
      String board = gameC4.getBoard();
      String state = "inprogress";

      int res = gameC4.evaluateBoard();
      if (res == GameConnect4.STATE_DRAW || res == GameConnect4.STATE_WIN) {
        state = "gameover";
        String result = (res == GameConnect4.STATE_DRAW ? "draw" : "win");
        UUID senderGuid = gameC4.getUserGuid(userId);
        WebSocketSession senderSession = webSocketHandler.getSessionByClientId( senderGuid );
        Map<String, Object> wsGameOverMsg = Map.of(
          "type", "gameOver",
          "status", "WsStatus.OK",
          "data", Map.of(
            "userId", userId,
            "result", result
          )
        );
        // Send game-over to both users
        String json = mapper.writeValueAsString(wsGameOverMsg);
        System.out.println("Sending WS: " + json);
        if ( partnerSession != null && partnerSession.isOpen()) 
          webSocketHandler.sendWsMessage( partnerSession, new TextMessage(json));
        if ( senderSession != null && senderSession.isOpen()) 
          webSocketHandler.sendWsMessage( senderSession, new TextMessage(json));
        gameManager.setGameState( gameC4, Game.STATE_OVER );
      }
      else
        gameManager.setGameState( gameC4, Game.STATE_RUNNING );
       
      Map<String, Object> response = Map.of(
        "userId", userId,
        "board", board,
        "state", state // inprogress or gameover
      );
      Map<String, Object> wsMsg = Map.of(
        "type", "insertDisk",
        "status", "WsStatus.OK",
        "data", response
      );
      String json = mapper.writeValueAsString(wsMsg);
      System.out.println("Sending WS: " + json);
      if ( partnerSession != null && partnerSession.isOpen()) 
        webSocketHandler.sendWsMessage( partnerSession, new TextMessage(json));

      return new ResponseEntity<>(response, HttpStatus.OK); // 200

    } 
    catch (Exception ex) {
      System.out.println("Error in PostInsertDisk: " + ex.getMessage());
      return ResponseEntity.status(500).body(Map.of(
        "acknowledged", false,
        "error", ex.getMessage()
      ));
    }
  }

  // POST /api/games/connect4/newgame - Request sent from Game browser
  // Req: { gameId, userId, user2Id } Auth Bearer: accessToken
  // Resp: { board }
  @PostMapping("/newgame")
  public ResponseEntity<?> postNewGame(@RequestBody Map<String, Object> body){
    try {
      if (!body.containsKey("gameId") || !body.containsKey("userId")) {
        return ResponseEntity.badRequest().body(Map.of(
          "acknowledged", false,
          "error", "Missing keys gameId and/or userId in POST request"
        ));
      }
      String gameId = body.get("gameId").toString();
      int userId = Integer.parseInt(body.get("userId").toString());
      GameConnect4 gameC4 = (GameConnect4) gameManager.getGameById(UUID.fromString(gameId));
      if (gameC4 == null) {
        return ResponseEntity.badRequest().body(Map.of(
          "acknowledged", false,
          "error", "Invalid Game type in POST request"
        ));
      }
      if ( gameManager.getGameState( gameC4 ) != Game.STATE_OVER ) {
        return ResponseEntity.badRequest().body(Map.of(
          "acknowledged", false,
          "error", "Invalid Game state for New Game action"
        ));
      }
      gameManager.updateUserActivity(gameC4);
      gameManager.setGameState( gameC4, Game.STATE_READY );

      UUID partnerGuid = gameC4.getPartnerGuid(userId);
      WebSocketSession partnerSession = webSocketHandler.getSessionByClientId(partnerGuid);

      if (!gameC4.getUserColor(userId).equals("Red")) {
          userId = gameC4.getPartner(userId);
      }

      String board = gameC4.resetBoard();

      Map<String, Object> response = Map.of(
        "userId", userId,
        "board", board
      );

      Map<String, Object> wsMsg = Map.of(
        "type", "newGame",
        "status", "WsStatus.OK",
        "data", response
      );

      if (partnerSession != null && partnerSession.isOpen()) {
        String json = mapper.writeValueAsString(wsMsg);
        webSocketHandler.sendWsMessage( partnerSession, new TextMessage(json));
      }

      return new ResponseEntity<>(response, HttpStatus.OK); // 200
    }
    catch (Exception ex) {
      System.out.println("Error in PostNewGame: " + ex.getMessage());
      return ResponseEntity.status(500).body(Map.of(
          "acknowledged", false,
          "error", ex.getMessage()
      ));
    }
  }
}