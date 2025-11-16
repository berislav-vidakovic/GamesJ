package com.gamesj.Controllers;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gamesj.Config.JwtUtil;
import com.gamesj.Models.RefreshToken;
import com.gamesj.Models.User;
import com.gamesj.Repositories.UserRepository;
import com.gamesj.Services.Game;
import com.gamesj.Services.GameConnect4;
import com.gamesj.Services.GameManager;
import com.gamesj.Services.Player;
import com.gamesj.Services.UserMonitor;
import com.gamesj.WebSockets.WebSocketHandler;
import com.gamesj.Repositories.RefreshTokenRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

@RestController
@RequestMapping("/api/games/connect4")
public class Connect4Controller {
  @Autowired
  private GameManager gameManager;

  @Autowired
  private WebSocketHandler webSocketHandler;

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

      // Get Connect4 game
      GameConnect4 gameC4 = (GameConnect4) gameManager.getGameById(UUID.fromString(gameId));
      if (gameC4 == null) {
        return ResponseEntity.badRequest().body(Map.of(
          "acknowledged", false,
          "error", "Invalid Game type in POST request"
        ));
      }
      gameManager.updateUserActivity(gameC4);

      // Extract userId
      int userId = Integer.parseInt(body.get("userId").toString());
      // Get color for user
      String color = gameC4.getUserColor(userId);
      // Success response
      return ResponseEntity.ok(Map.of("color", color));
    }
    catch (Exception ex) {
      System.out.println("Error in Post Init Received: " + ex.getMessage());
      return ResponseEntity.status(500).body(Map.of(
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
        // Validate required keys ----------------------------------------------------
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

        // Swap colors ---------------------------------------------------------------
        gameC4.swapColors();
        String color1 = gameC4.getUserColor(userId);
        String color2 = gameC4.getPartnerColor(userId);

        // Send WebSocket message to partner -----------------------------------------
        UUID partnerGuid = gameC4.getPartnerGuid(userId);
        WebSocketSession partnerSession = webSocketHandler.getSessionByClientId( partnerGuid );

        Map<String, Object> wsMsg = Map.of(
            "type", "swapColors",
            "status", "WsStatus.OK",
            "data", Map.of("color", color2)
        );       

        if ( partnerSession != null && partnerSession.isOpen()) {
          ObjectMapper mapper = new ObjectMapper();
          String json = mapper.writeValueAsString(wsMsg);
          System.out.println("Sending WS: " + json);
          partnerSession.sendMessage(new TextMessage(json));
        }

        // Return HTTP response to sender --------------------------------------------
        return ResponseEntity.ok(Map.of("color", color1));
    }
    catch (Exception ex) {
        System.out.println("Error in PostSwapColors: " + ex.getMessage());
        return ResponseEntity.status(500).body(Map.of(
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
      // Validate required keys ----------------------------------------------------
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
              "error", "Invalid gameId in POST request"
          ));
      }
      // Retrieve game
      GameConnect4 gameC4 = (GameConnect4) gameManager.getGameById(UUID.fromString(gameId));
      if (gameC4 == null) {
          return ResponseEntity.badRequest().body(Map.of(
              "acknowledged", false,
              "error", "Invalid Game type in POST request"
          ));
      }

      gameManager.setGameState( gameC4, Game.STATE_RUNNING );
      System.out.println("### Game State for Start: " + gameManager.getGameState( gameC4 ) );

      gameManager.updateUserActivity(gameC4);
      UUID partnerGuid = gameC4.getPartnerGuid(userId);
      WebSocketSession partnerSession = webSocketHandler.getSessionByClientId( partnerGuid );

      // Determine user with the next move (must be Red)
      if (!gameC4.getUserColor(userId).equals("Red")) {
          userId = gameC4.getPartner(userId);
      }
      String board = gameC4.getBoard();
      // HTTP response body
      Map<String, Object> response = Map.of(
          "userId", userId,
          "board", board
      );
      // WebSocket message
      Map<String, Object> wsMsg = Map.of(
          "type", "startGame",
          "status", "WsStatus.OK",
          "data", response
      );

      if ( partnerSession != null && partnerSession.isOpen()) {
        ObjectMapper mapper = new ObjectMapper();
        String json = mapper.writeValueAsString(wsMsg);
        System.out.println("Sending WS: " + json);
        partnerSession.sendMessage(new TextMessage(json));
      }
      else 
         return ResponseEntity.status(HttpStatus.ACCEPTED).body(response);

      return ResponseEntity.ok(response);
    } catch (Exception ex) {
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
      // Validate required keys ----------------------------------------------------
      if (!body.containsKey("gameId") || !body.containsKey("userId")) {
          return ResponseEntity.badRequest().body(Map.of(
              "acknowledged", false,
              "error", "Missing keys gameId and/or userId in POST request"
          ));
      }
      String gameId = body.get("gameId").toString();
      int userId = Integer.parseInt(body.get("userId").toString());

      // Retrieve game
      GameConnect4 gameC4 = (GameConnect4) gameManager.getGameById(UUID.fromString(gameId));
      if (gameC4 == null) {
        return ResponseEntity.badRequest().body(Map.of(
          "acknowledged", false,
          "error", "Invalid Game ID in POST request"
        ));
      }
      // Validate game state ---------------------------------------------------------
      System.out.println("### Game State for Insert Disk: " + gameManager.getGameState( gameC4 ) );
      if ( gameManager.getGameState( gameC4 ) != Game.STATE_RUNNING ) {
        return ResponseEntity.badRequest().body(Map.of(
          "acknowledged", false,
          "error", "Invalid Game state for Insert Disk action"
        ));
      }
      gameManager.updateUserActivity(gameC4);

      // Get row + col -------------------------------------------------------------
      int row = Integer.parseInt(body.get("row").toString());
      int col = Integer.parseInt(body.get("col").toString());

      UUID partnerGuid = gameC4.getPartnerGuid(userId);
      WebSocketSession partnerSession = webSocketHandler.getSessionByClientId( partnerGuid );

      // Insert disk into board
      gameC4.insertDisk(userId, row, col);
      String board = gameC4.getBoard();
      String state = "inprogress";

      // Evaluate board ------------------------------------------------------------
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
        ObjectMapper mapper = new ObjectMapper();
        String json = mapper.writeValueAsString(wsGameOverMsg);
        System.out.println("Sending WS: " + json);
        if ( partnerSession != null && partnerSession.isOpen()) 
          partnerSession.sendMessage(new TextMessage(json));
        if ( senderSession != null && senderSession.isOpen()) 
          senderSession.sendMessage(new TextMessage(json));
        // Update game state
        gameManager.setGameState( gameC4, Game.STATE_OVER );
      }
      // HTTP response
      Map<String, Object> response = Map.of(
          "userId", userId,
          "board", board,
          "state", state
      );
      // WebSocket message
      Map<String, Object> wsMsg = Map.of(
          "type", "insertDisk",
          "status", "WsStatus.OK",
          "data", response
      );
      ObjectMapper mapper = new ObjectMapper();
      String json = mapper.writeValueAsString(wsMsg);
      System.out.println("Sending WS: " + json);
      if ( partnerSession != null && partnerSession.isOpen()) 
        partnerSession.sendMessage(new TextMessage(json));

      return ResponseEntity.ok(response);
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
      return ResponseEntity.ok("(not implemented)");
  }
}