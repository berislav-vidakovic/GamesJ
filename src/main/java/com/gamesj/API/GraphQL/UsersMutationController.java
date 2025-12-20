package com.gamesj.API.GraphQL;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.stereotype.Controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gamesj.API.WebSocket.WebSocketHandler;
import com.gamesj.Core.DTO.RegisterUserInput;
import com.gamesj.Core.DTO.RegisterUserPayload;
import com.gamesj.Core.Models.User;
import com.gamesj.Core.Repositories.UserRepository;

@Controller
public class UsersMutationController {

    private final UserRepository userRepository;

    @Autowired
    private WebSocketHandler webSocketHandler;

    @Autowired
    private ObjectMapper mapper;

    public UsersMutationController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @MutationMapping
    public RegisterUserPayload registerUser(@Argument RegisterUserInput input) {

        // Validate input
        if (input.getLogin() == null || input.getLogin().isBlank()
         || input.getFullName() == null || input.getFullName().isBlank()
         || input.getPassword() == null || input.getPassword().isBlank()) {

            return new RegisterUserPayload(false, null,
                    "Missing login or fullname or password");
        }

        // Check existence
        boolean exists = userRepository
                .existsByLoginOrFullName(input.getLogin(), input.getFullName());

        if (exists) {
            return new RegisterUserPayload(false, null, "User already exists");
        }

        // Create user (no hashing yet if you prefer to add later)
        User user = new User();
        user.setLogin(input.getLogin());
        user.setFullName(input.getFullName());
        user.setPwd(input.getPassword());

        userRepository.save(user);

        Map<String, Object> response = Map.of(
              "acknowledged", true,
              "user", user
        );

        // Build WS message as Map
        Map<String, Object> wsMessage = Map.of(
            "type", "userRegister",
            "status", "WsStatus.OK",
            "data", response
        );
        try {
            String wsJson = mapper.writeValueAsString(wsMessage);
            // Broadcast via WebSocket
            webSocketHandler.broadcast(wsJson);
        } catch (Exception e) {
            // Log the error but do not fail the mutation
            e.printStackTrace();
        }

        //  Return payload
        return new RegisterUserPayload(true, user, null);
    }
}
