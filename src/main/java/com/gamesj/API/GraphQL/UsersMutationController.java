package com.gamesj.API.GraphQL;

import java.util.Map;

import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.stereotype.Controller;

import com.gamesj.Core.Adapters.RegisterUserResult;
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
    public RegisterUserPayload registerUser(@Argument String login, 
                @Argument String fullName, @Argument String  password ) {

      RegisterUserResult result = userRegistrationService
        .register( login, fullName, password ); 

      if (!result.isSuccess()) {
        return new RegisterUserPayload(
          false,
          null,
          result.getErrorMessage()
        );
      }

      webSocketService.broadcastMessage(
        "userRegister",
        "WsStatus.OK",
        Map.of("acknowledged", true, "user", result.getUser())
      );

      return new RegisterUserPayload(
        true,
        result.getUser(),
        null
      );
    }
}
