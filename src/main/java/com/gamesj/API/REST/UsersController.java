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
import com.gamesj.Config.JwtBuilder;
import com.gamesj.Core.Adapters.RegisterUserResult;
import com.gamesj.Core.DTO.AuthUserDTO;
import com.gamesj.Core.DTO.UsersAll;
import com.gamesj.Core.Models.RefreshToken;
import com.gamesj.Core.Models.User;
import com.gamesj.Core.Repositories.UserRepository;
import com.gamesj.Core.Services.Authentication;
import com.gamesj.Core.Services.Registration;
import com.gamesj.Core.Services.UserMonitor;
import com.gamesj.Core.Services.UserService;
import com.gamesj.Core.Services.WebSocketService;

import com.gamesj.API.WebSocket.WebSocketHandler;
import com.gamesj.Core.Repositories.RefreshTokenRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
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

  private final WebSocketService webSocketService;

  private final Registration userRegistrationService;

  private final Authentication authService;

  @Autowired
  private UserMonitor userMonitor;

  @Autowired
  private ObjectMapper mapper;

  public UsersController(UserRepository userRepository, 
    Registration userRegistrationService, 
    WebSocketService webSocketService, 
    Authentication authService 

  ) {
    this.userRepository = userRepository;
    this.userRegistrationService = userRegistrationService;
    this.webSocketService = webSocketService;
    this.authService = authService;
  }

  @GetMapping("/all")
  public ResponseEntity<UsersAll> getUsers() {
    return new ResponseEntity<>(userService.getAllUsers(), HttpStatus.OK); // 200
  }

  @PostMapping("/new")
  public ResponseEntity<?> registerUser(@RequestBody Map<String, Object> body) {
    try {
      // Expecting: {"register": {"login": "penny", "fullname": "Penny", "password": "pwd123"} }
      if( !RequestChecker.checkMandatoryFields( List.of("register"), new ArrayList<>(body.keySet())) )
        return RequestChecker.buildResponseMissingFields();

      Map<String, Object> credentials = (Map<String, Object>) body.get("register");
      RegisterUserResult result = userRegistrationService.register(
        (String) credentials.get("login"),
        (String) credentials.get("fullname"),
        (String) credentials.get("password") );

      if (!result.isSuccess()) 
        return ResponseEntity
            .status(ErrorCodes.toHttpStatus(result.getErrorCode()))
            .body(Map.of( "error", result.getErrorMessage() ));

      webSocketService.broadcastMessage(
        "userRegister",
        "WsStatus.OK",
        Map.of("acknowledged", true, "user", result.getUser())
      );

      return ResponseEntity
            .status(HttpStatus.CREATED)
            .body(Map.of(
                "acknowledged", true,
                "user", result.getUser()
            ));
    } 
    catch (Exception e) {
        e.printStackTrace();
        Map<String, Object> errorResponse = Map.of( 
          "acknowledged", false, "error", e.getMessage());
        return new ResponseEntity<>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR); // 500   
    }
  }

  @PostMapping("/login")
  public ResponseEntity<?> login(@RequestParam("id") String clientId, @RequestBody Map<String, String> body) {
    try {
      UUID parsedClientId = RequestChecker.parseIdParameter(clientId);
      if( parsedClientId == null ) 
        return RequestChecker.buildResponseInvalidGuid();

      String password = body.get("password");
      String userId = body.get("userId");
      AuthUserDTO authUser = authService.authenticate(userId, password);
      if( !authUser.isOK() )
        return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body(Map.of("error", authUser.getErrorMsg()));

      User user = authUser.getUser();
      userMonitor.updateUserActivity(user.getUserId(), parsedClientId);
      Map<String, Object> response = Map.of(
              "userId", user.getUserId(),
              "isOnline", user.getIsOnline(),
              "accessToken", authUser.getAccessToken(),           
              "refreshToken", authUser.getRefreshToken()
      );
      webSocketService.broadcastMessage("userSessionUpdate", "WsStatus.OK", response);
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
