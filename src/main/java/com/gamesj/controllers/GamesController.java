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
@RequestMapping("/api/games")
public class GamesController {
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

  @Autowired
  private UserRepository userRepository;

  // POST /api/games/run  - Request sent from Panel browser
  // Req: { run: "Connect Four", userId1, userId2, senderId } 
  // Resp: { game: "Connect Four", gameid, senderId }
  @PostMapping("/run")
  public ResponseEntity<?> postRunGame(@RequestBody Map<String, Object> body) {
    try {
      if (!body.containsKey("run"))
          return ResponseEntity.badRequest().body(Map.of(
                  "acknowledged", false,
                  "error", "Missing 'run' in POST request"
          ));

      Integer userId1 = (Integer) body.get("userId1");
      Integer userId2 = (Integer) body.get("userId2");
      Integer senderId = (Integer) body.get("senderId");

      if (userId1 == null || userId2 == null || senderId == null)
          return ResponseEntity.badRequest().body(Map.of(
                  "acknowledged", false,
                  "error", "Both userId1, userId2, senderId required in POST request"
          ));

      String gameId = gameManager.getGameId(userId1, userId2).toString();

      Map<String, Object> response = Map.of(
              "game", body.get("run"),
              "gameid", gameId,
              "senderId", senderId
      );

      return ResponseEntity.ok(response);

    } 
    catch (Exception ex) {
      ex.printStackTrace();
      return ResponseEntity.status(500).body(Map.of(
              "acknowledged", false,
              "error", ex.getMessage()
      ));
    }
  }

  // POST /api/games/init - Request sent from Game browser
  // Req:   { gameId, userId} 
  // Resp:  { gameId, id, userName, user2Id, user2Name}
  @PostMapping("/init")
  public ResponseEntity<?> postInitGame(@RequestBody Map<String, Object> body) {
    System.out.println("### POST /api/games/init Request received");
    try {
      if (!body.containsKey("gameId") || !body.containsKey("userId"))
          return ResponseEntity.badRequest().body(Map.of(
                  "acknowledged", false,
                  "error", "Missing keys in POST request"
          ));

      String gameId = body.get("gameId").toString();
      Integer userId = (Integer) body.get("userId");
      
      System.out.println("### POST /api/games/init Request Processing - 1");


      if (!gameManager.isGameStatePaired(UUID.fromString(gameId)))
          return ResponseEntity.badRequest().body(Map.of(
                  "acknowledged", false,
                  "error", "Invalid gameId in POST request"
          ));

      int user2Id = gameManager.getPartnerId(UUID.fromString(gameId), userId);
      System.out.println("### POST /api/games/init Request Processing - 2");


      UUID clientId = UUID.randomUUID();
      gameManager.setUserGuid(UUID.fromString(gameId), userId, clientId);

      User user1 = userRepository.findById(userId).orElse(null);
      User user2 = userRepository.findById(user2Id).orElse(null);

      System.out.println("### POST /api/games/init Request Processing - 3");


      if (user1 == null || user2 == null)
          return ResponseEntity.badRequest().body(Map.of(
                  "acknowledged", false,
                  "error", "Invalid userIds in POST request"
          ));

      System.out.println("### POST /api/games/init Request Processing - 4");


      Map<String, Object> response = Map.of(
        "gameId", gameId,
        "id", clientId,
        "userName", user1.getFullName(),
        "user2Id", user2Id,
        "user2Name", user2.getFullName()
      );

      return ResponseEntity.ok(response);
    } 
    catch (Exception ex) {
      ex.printStackTrace();
      return ResponseEntity.status(500).body(Map.of(
              "acknowledged", false,
              "error", ex.getMessage()
      ));
    }
  }
}
