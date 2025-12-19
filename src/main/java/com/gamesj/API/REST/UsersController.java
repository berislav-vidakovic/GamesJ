package com.gamesj.API.REST;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gamesj.Config.JwtUtil;
import com.gamesj.Core.DTO.UsersAll;
import com.gamesj.Core.Models.RefreshToken;
import com.gamesj.Core.Models.User;
import com.gamesj.Core.Repositories.UserRepository;
import com.gamesj.Core.Services.UserMonitor;
import com.gamesj.Core.Services.UserService;
import com.gamesj.API.WebSocket.WebSocketHandler;
import com.gamesj.Core.Repositories.RefreshTokenRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

// GET /all
// POST /new
// POST /login
// POST /logout
@RestController
@RequestMapping("/api/users") 
public class UsersController {
  @Autowired
  private UserService userService;

  @Autowired
  private UserRepository userRepository;

  @Autowired
  private PasswordEncoder passwordEncoder;

  @Autowired
  private RefreshTokenRepository refreshTokenRepository;

  @Autowired
  private WebSocketHandler webSocketHandler;

  @Autowired
  private UserMonitor userMonitor;

  @Autowired
  private ObjectMapper mapper;

  public UsersController(UserRepository userRepository) {
    this.userRepository = userRepository;
  }

  @GetMapping("/all")
  public ResponseEntity<UsersAll> getUsers() {
    return new ResponseEntity<>(userService.getAllUsers(), HttpStatus.OK); // 200
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
      newUser.setPwd(hashedPwd);
      userRepository.save(newUser);
      System.out.printf("New user inserted: %s%n", login);

      Map<String, Object> response = Map.of(
              "acknowledged", true,
              "user", newUser
      );

      // Build WS message as Map
      Map<String, Object> wsMessage = Map.of(
          "type", "userRegister",
          "status", "WsStatus.OK",
          "data", response
      );
      // Convert Map to JSON string
      String wsJson = mapper.writeValueAsString(wsMessage);

      // Broadcast via WebSocket
      webSocketHandler.broadcast(wsJson);

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
    try {
      // Validate clientId
      UUID parsedClientId;
      try {
        parsedClientId = UUID.fromString(clientId);
      } 
      catch (IllegalArgumentException e) {
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

      // Extract password field
      if (!body.containsKey("password")) {
        Map<String, Object> response = Map.of(
            "acknowledged", false,
            "error", "Missing 'password' field"
          );
        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST); // 400
      }
      String password = body.get("password").toString();
      System.out.println("Password received for login: " + password);

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

      // Password validation 
      // if no hashed pwd in DB => new user, first time password hashing
      if( user.getPwd().isEmpty() ){
        // Hash the password using BCrypt
        String hashedPwd = passwordEncoder.encode(password);
        user.setPwd(hashedPwd);
        userRepository.save(user);
      }       
      else {
        boolean passwordsMatch = passwordEncoder.matches(password, user.getPwd());
        if( !passwordsMatch ) {
          Map<String, Object> response = Map.of(
                "acknowledged", false,
                "error", "Invalid password"
          );
          return new ResponseEntity<>(response, HttpStatus.UNAUTHORIZED); // 401
        }
      }
      
      // Issue access token
      String accessToken = JwtUtil.generateToken(user.getUserId(), user.getLogin());

      // Issue and store refresh token 
      String refreshToken = java.util.UUID.randomUUID().toString();
      LocalDateTime expiryDate = LocalDateTime.now().plusDays(7); // valid for 7 days
      refreshTokenRepository.deleteByUserId(user.getUserId());
      RefreshToken tokenEntity = new RefreshToken();
      tokenEntity.setUserId(user.getUserId());
      tokenEntity.setToken(refreshToken);
      tokenEntity.setExpiresAt(expiryDate);
      refreshTokenRepository.save(tokenEntity);

      // Set user online
      user.setIsOnline(true);
      userRepository.save(user);
      userMonitor.updateUserActivity(user.getUserId(), parsedClientId);

      Map<String, Object> response = Map.of(
              "userId", userId,
              "isOnline", true,
              "accessToken", accessToken,           
              "refreshToken", refreshToken
      );

      //var response = new { userId, isOnline = true };
      Map<String, Object> wsMessage = Map.of(
          "type", "userSessionUpdate",
          "status", "WsStatus.OK",
          "data", response
      );
      // Convert Map to JSON string
      String wsJson = mapper.writeValueAsString(wsMessage);
      // Broadcast via WebSocket
      webSocketHandler.broadcast(wsJson);

      return new ResponseEntity<>(response, HttpStatus.OK); // 200
    } 
    catch (Exception e) {
      e.printStackTrace();
      Map<String, Object> errorResponse = Map.of( 
        "acknowledged", false, "error", e.getMessage());
      return new ResponseEntity<>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR); // 500   
    }
  }

  @PostMapping("/logout")
  public ResponseEntity<?> logout(@RequestParam("id") String clientId, @RequestBody Map<String, Object> body) {
    try {
      // Validate clientId
      System.out.println("Received POST /api/users/logout ");
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
      user.setIsOnline(false);
      userRepository.save(user);

      // Clear refresh token from DB
      System.out.println("Deleting refresh tokens for userId: " + userId);
      refreshTokenRepository.deleteByUserId(userId);
      System.out.println("Deleting done ");

      userMonitor.removeUser(userId);       

      Map<String, Object> response = Map.of(
              "userId", userId,
              "isOnline", false
      );

      Map<String, Object> wsMessage = Map.of(
          "type", "userSessionUpdate",
          "status", "WsStatus.OK",
          "data", response
      );
      // Convert Map to JSON string
      String wsJson = mapper.writeValueAsString(wsMessage);
      // Broadcast via WebSocket
      webSocketHandler.broadcast(wsJson);

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
