package com.gamesj.Controllers;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.gamesj.Models.User;
import com.gamesj.Repositories.UserRepository;

import java.io.Console;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;



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
      if (users.isEmpty()) 
        return new ResponseEntity<>(HttpStatus.NO_CONTENT); // 204

      UUID id = UUID.randomUUID();

      // build base URL from request
      String baseUrl = ServletUriComponentsBuilder.fromCurrentContextPath().build().toUriString(); 

      List<String> techstack = List.of(
          baseUrl + "/images/java.png",
          baseUrl + "/images/spring.png",
          baseUrl + "/images/mysql.png"
      );

      Map<String, Object> response = Map.of(
          "id", id.toString(),
          "users", users,
          "techstack", techstack
      );

      return new ResponseEntity<>(response, HttpStatus.OK); // 200
    }

     @PostMapping("/login")
    public ResponseEntity<?> login(@RequestParam("id") String clientId, @RequestBody Map<String, Object> body) {
        return handleUserStatusChange(clientId, body, true, true);
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(@RequestParam("id") String clientId, @RequestBody Map<String, Object> body) {
        return handleUserStatusChange(clientId, body, false, false);
    }

    // Common  handler for login/logout 
    private ResponseEntity<?> handleUserStatusChange(String clientId, Map<String, Object> body, boolean isOnline, boolean isLogin) {
      try {
        // Validate clientId
        UUID parsedClientId;
        try {
          parsedClientId = UUID.fromString(clientId);
        } catch (IllegalArgumentException e) {
          return ResponseEntity.badRequest().body(Map.of(
                  "acknowledged", false,
                  "error", "Missing or invalid ID"
          ));
        }
        System.out.println("Received POST /api/users/logout with valid ID: " + parsedClientId.toString());

        // Validate userId
        if (!body.containsKey("userId")) {
          return ResponseEntity.badRequest().body(Map.of(
                  "acknowledged", false,
                  "error", "Missing 'userId' field"
          ));
        }
        int userId = (Integer) body.get("userId");

        // Find user
        Optional<User> optionalUser = userRepository.findById(userId);
        if (optionalUser.isEmpty()) {
          return ResponseEntity.status(204).body(Map.of(
                  "acknowledged", false,
                  "error", "UserID Not found"
          ));
        }
        User user = optionalUser.get();
        user.setIsOnline(isOnline);
        userRepository.save(user);

        Map<String, Object> response = Map.of(
                "userId", userId,
                "isOnline", isOnline
        );

        return ResponseEntity.ok(response);
      } 
      catch (Exception e) {
        e.printStackTrace();
        return ResponseEntity.internalServerError().body(Map.of(
                "acknowledged", false,
                "error", e.getMessage()
        ));
      }
    }


  }
