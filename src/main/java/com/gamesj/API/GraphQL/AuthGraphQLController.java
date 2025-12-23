package com.gamesj.API.GraphQL;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.stereotype.Controller;

import com.gamesj.API.REST.RequestChecker;
import com.gamesj.Core.DTO.AuthUserDTO;
import com.gamesj.Core.Models.User;
import com.gamesj.Core.Services.Authentication;
import com.gamesj.Core.Services.UserMonitor;
import com.gamesj.Core.Services.WebSocketService;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Controller
public class AuthGraphQLController {

    private final Authentication authService;
    private final WebSocketService webSocketService;
    private final UserMonitor userMonitor;

    public AuthGraphQLController(Authentication authService,
                                 WebSocketService webSocketService,
                                 UserMonitor userMonitor) {
        this.authService = authService;
        this.webSocketService = webSocketService;
        this.userMonitor = userMonitor;
    }

    @MutationMapping
    public Map<String, Object> refreshToken(
            @Argument String clientId,
            @Argument String refreshToken ) {
      Map<String, Object> response = new HashMap<>();
      try {
        UUID parsedClientId = RequestChecker.parseIdParameter(clientId);
        if (parsedClientId == null) {
          return Map.of("error", "Invalid clientId");
        }

        AuthUserDTO authUser = authService.authenticate(refreshToken);
        if (!authUser.isOK()) {
          // Return null tokens + error message
          response.put("accessToken", null);
          response.put("refreshToken", null);
          response.put("userId", 0);
          response.put("isOnline", false);
          response.put("error", authUser.getErrorMsg() != null ? authUser.getErrorMsg() : "Invalid refreshToken");
          return response;
          //return Map.of("error", "NULL!"+authUser.getErrorMsg());

        }

        User user = authUser.getUser();
        userMonitor.updateUserActivity(user.getUserId(), parsedClientId);
        System.out.println("updateUserActivity DONE " + user.getUserId() + " clientId=" + parsedClientId);

        response.put("accessToken", authUser.getAccessToken());
        response.put("refreshToken", authUser.getRefreshToken());
        response.put("userId", user.getUserId());
        response.put("isOnline", user.getIsOnline());
        response.put("error", null);

        webSocketService.broadcastMessage("userSessionUpdate", "WsStatus.OK", response);
        return response;
      } 
      catch (Exception e) {
          e.printStackTrace();
          return Map.of("error", e.getMessage());
      }
  }
}
