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
}
