package com.gamesj.API.GraphQL;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

import com.gamesj.Core.Services.UserService;
import com.gamesj.Core.DTO.UsersAll;

@Controller
public class UsersQueryController {
  

    @Autowired
    private UserService userService;

    @QueryMapping
    public UsersAll getAllUsers() { // Method name = Query
      UsersAll dto = userService.getAllUsers();
      userService.addTechStackItem(dto, "/images/GraphQL.png");
      return dto;
    }
}

