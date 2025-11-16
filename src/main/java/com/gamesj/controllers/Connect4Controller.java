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
  private PasswordEncoder passwordEncoder;

  @Autowired
  private RefreshTokenRepository refreshTokenRepository;

  @Autowired
  private WebSocketHandler webSocketHandler;

  @Autowired
  private UserMonitor userMonitor;

  @Autowired
  private UserRepository userRepository;

  
  private RefreshToken getTokenEntity(String refreshToken) {
    if( refreshToken == null || refreshToken.isEmpty() )
      return null;

    Optional<RefreshToken> tokenEntityOpt = refreshTokenRepository.findByToken(refreshToken);
    if( tokenEntityOpt.isEmpty() )
      return null;
    
    RefreshToken tokenEntity = tokenEntityOpt.get();
    if (tokenEntity.getExpiresAt().isBefore(LocalDateTime.now()) )
      return null;

    return tokenEntity;
  }

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
      GameConnect4 gamec4 = (GameConnect4) gameManager.getGameById(UUID.fromString(gameId));
      if (gamec4 == null) {
        return ResponseEntity.badRequest().body(Map.of(
          "acknowledged", false,
          "error", "Invalid Game type in POST request"
        ));
      }

      // Extract userId
      int userId = Integer.parseInt(body.get("userId").toString());

      // Get color for user
      String color = gamec4.getUserColor(userId);

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

}
