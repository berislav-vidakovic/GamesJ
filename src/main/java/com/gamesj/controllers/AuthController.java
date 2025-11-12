package com.gamesj.Controllers;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.gamesj.Config.JwtUtil;
import com.gamesj.Models.RefreshToken;
import com.gamesj.Models.User;
import com.gamesj.Repositories.RefreshTokenRepository;
import com.gamesj.Repositories.UserRepository;
import com.gamesj.Services.UserMonitor;
import com.gamesj.WebSockets.WebSocketHandler;


@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final RefreshTokenRepository refreshTokenRepository;
    private final UserRepository userRepository;

    @Autowired
    private WebSocketHandler webSocketHandler;

    @Autowired
    private UserMonitor userMonitor;

    public AuthController(RefreshTokenRepository refreshTokenRepository,
                          UserRepository userRepository) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.userRepository = userRepository;
    }

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

    @PostMapping("/refresh")
    public ResponseEntity<?> refreshToken(@RequestParam("id") String clientId, @RequestBody Map<String, String> body) {
      try {        
        System.out.println("1-AuthController.refreshToken called with body: ");
        String refreshToken = body.get("refreshToken");
        System.out.println("2-AuthController.refreshToken called with body: " );

        // get token entity from refreshTokenRepository and check expiry
        RefreshToken tokenEntity = getTokenEntity(refreshToken); // {id, userId, token, expiresAt }
        if( tokenEntity == null ) 
          return ResponseEntity
                  .status(HttpStatus.UNAUTHORIZED)
                  .body(Map.of("error", "Refresh token missing, invalid or expired"));

        // Load user and issue new tokens
        User user = userRepository.findById(tokenEntity.getUserId())
                .orElseThrow(() -> new RuntimeException("User not found"));

        String newAccessToken = JwtUtil.generateToken(user.getUserId(), user.getLogin());
        String newRefreshToken = UUID.randomUUID().toString();
        LocalDateTime newExpiry = LocalDateTime.now().plusDays(7);

        // Update token in DB
        tokenEntity.setToken(newRefreshToken);
        tokenEntity.setExpiresAt(newExpiry);
        refreshTokenRepository.save(tokenEntity);

        // Set user online
        user.setIsOnline(true);
        userRepository.save(user);
        userMonitor.updateUserActivity(user.getUserId());


        // Return new tokens
        Map<String, Object> response = Map.of(
                "accessToken", newAccessToken,
                "refreshToken", newRefreshToken,
                "userId", tokenEntity.getUserId(),
                "isOnline", true
        );

        Map<String, Object> wsMessage = Map.of(
            "type", "userSessionUpdate",
            "status", "WsStatus.OK",
            "data", response
        );
        // Convert Map to JSON string
        String wsJson = new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(wsMessage);
        // Broadcast via WebSocket
        webSocketHandler.broadcast(wsJson);

        return ResponseEntity.ok(response);

      } 
      catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }
}
