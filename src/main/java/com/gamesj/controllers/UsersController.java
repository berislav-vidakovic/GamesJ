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
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController
@RequestMapping("/api/users")
public class UsersController {
  private final UserRepository userRepository;
  
  @Autowired
  private PasswordEncoder passwordEncoder;

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

  @PostMapping("/new")
  public ResponseEntity<?> registerUser(@RequestBody Map<String, Object> body) {
    try {
      // Expecting: {"register": {"login": "penny", "fullname": "Penny", "password": "pwd123"} }
      if (!body.containsKey("register")) {
        Map<String, Object> response = Map.of(
                  "acknowledged", false,
                  "error", "Missing 'register' field"
        );
        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST); // 400
      }

      @SuppressWarnings("unchecked")
      Map<String, Object> credentials = (Map<String, Object>) body.get("register");
      String login = (String) credentials.get("login");
      String fullName = (String) credentials.get("fullname");
      String password = (String) credentials.get("password");
      if (login == null || login.isBlank() || fullName == null || fullName.isBlank()
          || password == null || password.isBlank()) {
        Map<String, Object> response = Map.of(
                  "acknowledged", false,
                  "error", "Missing login or fullname or password"
          );
        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST); // 400
      }
      System.out.printf("Register request: login=%s, fullname=%s, password=%s%n", login, fullName, password);

      boolean exists = userRepository.existsByLoginOrFullName(login, fullName);
      if (exists) {
        Map<String, Object> response = Map.of(
                "acknowledged", false,
                "error", "User  already exists"
        );
        return new ResponseEntity<>(response, HttpStatus.CONFLICT); // 409
      }


      // Hash the password using BCrypt
      String hashedPwd = passwordEncoder.encode(password);

      // Create and persist user
      User newUser = new User();
      newUser.setLogin(login);
      newUser.setFullName(fullName);
      //newUser.setPwd(password);
      newUser.setPwd(hashedPwd);
      userRepository.save(newUser);
      System.out.printf("New user inserted: %s%n", login);

      Map<String, Object> response = Map.of(
              "acknowledged", true,
              "user", newUser
      );
      return new ResponseEntity<>(response, HttpStatus.CREATED); // 201
    } 
    catch (Exception e) {
        e.printStackTrace();
        Map<String, Object> errorResponse = Map.of( 
          "acknowledged", false, "error", e.getMessage());
        return new ResponseEntity<>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR); // 500   
    }
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
          Map<String, Object> response = Map.of(
                    "acknowledged", false,
                  "error", "Missing or invalid ID"
            );
          return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST); // 400
        }
        System.out.println("Received POST /api/users/logout with valid ID: " + parsedClientId.toString());

        // Validate userId
        if (!body.containsKey("userId")) {
          Map<String, Object> response = Map.of(
              "acknowledged", false,
              "error", "Missing 'userId' field"
            );
          return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST); // 400
        }
        int userId = (Integer) body.get("userId");

        // Find user
        Optional<User> optionalUser = userRepository.findById(userId);
        if (optionalUser.isEmpty()) {
          Map<String, Object> response = Map.of(
                "acknowledged", false,
                "error", "UserID Not found"
          );
          return new ResponseEntity<>(response, HttpStatus.NO_CONTENT); // 204
        }
        User user = optionalUser.get();
        user.setIsOnline(isOnline);
        userRepository.save(user);

        Map<String, Object> response = Map.of(
                "userId", userId,
                "isOnline", isOnline
        );

        return new ResponseEntity<>(response, HttpStatus.OK); // 200
      } 
      catch (Exception e) {
        e.printStackTrace();
        Map<String, Object> errorResponse = Map.of( 
          "acknowledged", false, "error", e.getMessage());
        return new ResponseEntity<>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR); // 500   
      }
    }
}
