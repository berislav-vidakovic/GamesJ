package com.gamesj.API.GraphQL;

import java.util.Map;

import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.stereotype.Controller;

import com.gamesj.Core.Adapters.RegisterUserResult;
import com.gamesj.Core.DTO.RegisterUserInput;
import com.gamesj.Core.DTO.RegisterUserPayload;
import com.gamesj.Core.Services.Registration;
import com.gamesj.Core.Services.WebSocketService;

@Controller
public class UsersMutationController {
    
    private final Registration userRegistrationService;
    private final WebSocketService webSocketService;

    public UsersMutationController(Registration userRegistrationService, WebSocketService webSocketService) {
        this.userRegistrationService = userRegistrationService; 
        this.webSocketService = webSocketService; 
    }

    @MutationMapping
    public RegisterUserPayload registerUser(@Argument RegisterUserInput input) {
      RegisterUserResult result = userRegistrationService.register(
        input.getLogin(),
        input.getFullName(),
        input.getPassword()
      );

      if (!result.isSuccess()) {
        return new RegisterUserPayload(
          false,
          null,
          result.getErrorMessage()
        );
      }

      // Use WebSocketService to broadcast
      webSocketService.broadcastMessage(
        "userRegister",
        "WsStatus.OK",
        Map.of("acknowledged", true, "user", result.getUser())
      );
      //  Return payload
      return new RegisterUserPayload(
          true,
          result.getUser(),
          null
      );
    }
}
