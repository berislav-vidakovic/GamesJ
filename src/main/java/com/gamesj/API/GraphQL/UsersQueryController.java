package com.gamesj.API.GraphQL;

import java.util.List;
import java.util.UUID;

import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import com.gamesj.Core.Models.User;
import com.gamesj.Core.Repositories.UserRepository;
import com.gamesj.Core.DTO.UsersAll;

@Controller
public class UsersQueryController {

    private final UserRepository userRepository;

    public UsersQueryController(UserRepository userRepository) {
      this.userRepository = userRepository;
    }

    @QueryMapping
    public UsersAll getAllUsers() { // Method name = Query
      List<User> users = userRepository.findAll();

      String baseUrl = ServletUriComponentsBuilder
              .fromCurrentContextPath()
              .build()
              .toUriString();

      List<String> techstack = List.of(
          baseUrl + "/images/java.png",
          baseUrl + "/images/spring.png",
          baseUrl + "/images/mysql.png"
      );

      return new UsersAll(
        UUID.randomUUID().toString(),
        users,
        techstack
      );
    }
}


/*
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
*/ 
