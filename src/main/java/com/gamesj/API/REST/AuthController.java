package com.gamesj.API.REST;

import java.util.Map;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.gamesj.Core.DTO.AuthUserDTO;
import com.gamesj.Core.Models.User;
import com.gamesj.Core.Services.Authentication;
import com.gamesj.Core.Services.UserMonitor;
import com.gamesj.Core.Services.WebSocketService;

// POST /api/auth/refresh
@RestController
@RequestMapping("/api/auth")
public class AuthController {
    @Autowired
    private UserMonitor userMonitor;

    private final Authentication authService;
    private final WebSocketService webSocketService;

    public AuthController(Authentication authenticationService, WebSocketService webSocketService) {
        this.authService = authenticationService;
        this.webSocketService = webSocketService; 
    }

    @PostMapping("/refresh")
    public ResponseEntity<?> refreshToken(@RequestParam("id") String clientId, @RequestBody Map<String, String> body) {
      // Request: ?id=guid body: { refreshToken }
      try {        
        // Validate clientId
        UUID parsedClientId;
        try {
          parsedClientId = UUID.fromString(clientId);
        } 
        catch (IllegalArgumentException e) {
          Map<String, Object> response = Map.of(
              "error", "Missing or invalid ID"
            );
          return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST); // 400
        }
        System.out.println("Received POST /auth/refresh with valid ID: " + parsedClientId.toString());
        String refreshToken = body.get("refreshToken");

        AuthUserDTO authUser = authService.authenticate(refreshToken);
        if( !authUser.isOK() )
          return ResponseEntity
                  .status(HttpStatus.UNAUTHORIZED)
                  .body(Map.of("error", authUser.getErrorMsg()));

        User user = authUser.getUser();
        userMonitor.updateUserActivity(user.getUserId(), parsedClientId);
        System.out.println(" -----------updateUserActivity DONE " + user.getUserId() + " clientId=" + parsedClientId);

        // Return new tokens
        Map<String, Object> response = Map.of(
                "accessToken", authUser.getAccessToken(),
                "refreshToken", authUser.getRefreshToken(),
                "userId", user.getUserId(),
                "isOnline", user.getIsOnline()
        );

        webSocketService.broadcastMessage("userSessionUpdate", "WsStatus.OK", response);

        return ResponseEntity.ok(response);
      } 
      catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }
}
