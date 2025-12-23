package com.gamesj.Core.DTO;

import java.util.ArrayList;
import java.util.List;
import com.gamesj.Core.Models.User;

public class UsersAll {

  private String id;
  private List<User> users;
  private List<String> techstack;

  public UsersAll(String id, List<User> users, List<String> techstack) {
    this.id = id;
    this.users = users;
    this.techstack = new ArrayList<>(techstack);
  }

  public String getId() {
    return id;
  }

  public List<User> getUsers() {
    return users;
  }

  public List<String> getTechstack() {
    return techstack;
  }

  public void addTechStackItem(String sTechStackItem){
    techstack.add(sTechStackItem);
  }
}
