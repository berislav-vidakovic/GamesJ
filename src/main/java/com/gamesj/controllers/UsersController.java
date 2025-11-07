package com.gamesj.Controllers;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.gamesj.Models.User;
import com.gamesj.Repositories.UserRepository;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/users")
public class UsersController {

    private final UserRepository userRepository;

    public UsersController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @GetMapping("/all")
    public ResponseEntity<Map<String, Object>> getUsers() {
        List<User> users = userRepository.findAll();
        //List<User> users = new ArrayList<>();
        UUID id = UUID.randomUUID();

        Map<String, Object> response = Map.of(
            "id", id.toString(),
            "users", users 
        );

        HttpStatus status = users.isEmpty() ? HttpStatus.NO_CONTENT : HttpStatus.OK;

        return new ResponseEntity<>(response, status);
    }
}
