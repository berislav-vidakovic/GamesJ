package com.gamesj.API.GraphQL;

import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.stereotype.Controller;

import com.gamesj.Core.DTO.RegisterUserInput;
import com.gamesj.Core.DTO.RegisterUserPayload;
import com.gamesj.Core.Models.User;
import com.gamesj.Core.Repositories.UserRepository;

@Controller
public class UsersMutationController {

    private final UserRepository userRepository;

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

        //  Return payload
        return new RegisterUserPayload(true, user, null);
    }
}
