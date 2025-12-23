package com.gamesj.API.REST;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.gamesj.Core.Adapters.RegisterUserResult;
import com.gamesj.Core.DTO.UsersAll;
import com.gamesj.Core.Services.Registration;
import com.gamesj.Core.Services.UserService;
import com.gamesj.Core.Services.WebSocketService;

import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

// GET /all
// POST /new
@RestController
@RequestMapping("/api/users") 
public class UsersController {
  @Autowired
  private UserService userService;

  private final WebSocketService webSocketService;

  private final Registration userRegistrationService;

  public UsersController(Registration userRegistrationService, 
    WebSocketService webSocketService ) {
    this.userRegistrationService = userRegistrationService;
    this.webSocketService = webSocketService;
  }

  @GetMapping("/all")
  public ResponseEntity<UsersAll> getUsers() {
    UsersAll dtoUsersAll = userService.getAllUsers();
    userService.addTechStackItem(dtoUsersAll, "/images/REST.jpg");
    return new ResponseEntity<>(dtoUsersAll, HttpStatus.OK); // 200
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

}
