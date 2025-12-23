package com.gamesj.Core.Services;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import com.gamesj.Core.Repositories.UserRepository;
import com.gamesj.Core.DTO.UsersAll;
import com.gamesj.Core.Models.User;


@Service
public class UserService {
  private final UserRepository userRepository;

  public UserService(UserRepository userRepository) {
    this.userRepository = userRepository;
  }

  public UsersAll getAllUsers() {
   List<User> users = userRepository.findAll();

    // build base URL from request
    String baseUrl = ServletUriComponentsBuilder.fromCurrentContextPath().build().toUriString(); 

    List<String> techstack = List.of(
        baseUrl + "/images/java.png",
        baseUrl + "/images/spring.png",
        baseUrl + "/images/mysql.png"
    );

    UsersAll dtoUsersAll = new UsersAll(
      UUID.randomUUID().toString(),
      users,
      techstack
    );

    return dtoUsersAll;    
  }

  public void addTechStackItem(UsersAll users, String sTechStackItem){
    String baseUrl = ServletUriComponentsBuilder.fromCurrentContextPath().build().toUriString(); 
    users.addTechStackItem(baseUrl + sTechStackItem);
  }
}
