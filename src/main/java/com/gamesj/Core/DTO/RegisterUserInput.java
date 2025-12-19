package com.gamesj.Core.DTO;

public class RegisterUserInput {
    private String login;
    private String fullName;
    private String password;

    // getters & setters
    public String getLogin() {
      return login;
    }
    public void setLogin(String login) {
      this.login = login;
    }
    public String getFullName() {
      return fullName;
    }
    public void setFullName(String fullName) {
      this.fullName = fullName;
    }
    public String getPassword() {
      return password;
    }
    public void setPassword(String password) {
      this.password = password;
    }
}
