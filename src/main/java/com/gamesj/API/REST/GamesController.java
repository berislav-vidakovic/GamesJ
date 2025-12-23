package com.gamesj.API.REST;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.gamesj.Config.JwtBuilder;
import com.gamesj.Core.Models.RefreshToken;
import com.gamesj.Core.Models.User;
import com.gamesj.Core.Repositories.UserRepository;
import com.gamesj.Core.Services.GameManager;
import com.gamesj.Core.Services.UserMonitor;
import com.gamesj.Core.Repositories.RefreshTokenRepository;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

// POST /run
// POST /init
@RestController
@RequestMapping("/api/games")
public class GamesController {
  @Autowired
  private GameManager gameManager;

  @Autowired
  private RefreshTokenRepository refreshTokenRepository;

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

  // POST /api/games/run  - Request sent from Panel browser
  // Req: { run: "panel.game.connect4", userId1, userId2, senderId, refreshToken }
  // Resp: { game: "panel.game.connect4", gameid, senderId, refreshToken, accessToken }
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

      String refreshToken = (String) body.get("refreshToken");

      // get token entity from refreshTokenRepository and check expiry
      RefreshToken tokenEntity = getTokenEntity(refreshToken); // {id, userId, token, expiresAt }
      if( tokenEntity == null ) 
        return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body(Map.of("error", "Refresh token missing, invalid or expired"));

      // Load user and issue new tokens
      User user = userRepository.findById(tokenEntity.getUserId())
              .orElseThrow(() -> new RuntimeException("User not found"));
      // Issue new Access Token
      String newAccessToken = JwtBuilder.generateToken(user.getUserId(), user.getLogin());
      // Issue new Refresh Token and save it in DB
      String newRefreshToken = UUID.randomUUID().toString();
      LocalDateTime newExpiry = LocalDateTime.now().plusDays(7);
      tokenEntity.setToken(newRefreshToken);
      tokenEntity.setExpiresAt(newExpiry);
      refreshTokenRepository.save(tokenEntity);

      Map<String, Object> response = Map.of(
        "game", body.get("run"),
        "gameid", gameId,
        "senderId", senderId,
        "refreshToken", newRefreshToken,
        "accessToken", newAccessToken
      );

      return new ResponseEntity<>(response, HttpStatus.OK); // 200
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
  // Req:   { gameId, userId} Auth Bearer: accessToken
  // Resp:  { gameId, id, userName, user2Id, user2Name}
  @PostMapping("/init")
  public ResponseEntity<?> postInitGame(@RequestBody Map<String, Object> body) {
    System.out.println("### POST /api/games/init Request received");
    try {
      if (!body.containsKey("gameId") || !body.containsKey("userId"))
        return new ResponseEntity<>(
          Map.of("acknowledged", false,"error", "Missing keys in POST request"), 
          HttpStatus.BAD_REQUEST);
      String gameId = body.get("gameId").toString();
      Integer userId = (Integer) body.get("userId");

      if (!gameManager.isValidStateForRunAction(UUID.fromString(gameId)))
        return new ResponseEntity<>(Map.of(
          "acknowledged", false,
          "error", "Invalid gameId in POST request"
        ), HttpStatus.BAD_REQUEST);

      int user2Id = gameManager.getPartnerId(UUID.fromString(gameId), userId);

      UUID clientId = UUID.randomUUID();
      userMonitor.updateUserActivity(userId, clientId);

      gameManager.setUserGuid(UUID.fromString(gameId), userId, clientId);
      User user1 = userRepository.findById(userId).orElse(null);
      User user2 = userRepository.findById(user2Id).orElse(null);

      if (user1 == null || user2 == null)
        return new ResponseEntity<>(
          Map.of( "acknowledged", false, "error", "Invalid userIds in POST request"), 
          HttpStatus.BAD_REQUEST);

      Map<String, Object> response = Map.of(
        "gameId", gameId,
        "id", clientId,
        "userName", user1.getFullName(),
        "user2Id", user2Id,
        "user2Name", user2.getFullName()
      );

      return new ResponseEntity<>(response, HttpStatus.OK); // 200

    } 
    catch (Exception ex) {
      ex.printStackTrace();
      return new ResponseEntity<>(
        Map.of( "acknowledged", false, "error", ex.getMessage()), 
        HttpStatus.INTERNAL_SERVER_ERROR);
    }
  }

}