package com.gamesj.Core.DTO;

import com.gamesj.Core.Models.User;

public class AuthUserDTO {
  private final String accessToken;
  private final String refreshToken;
  private final User user;
  private boolean isOK;
  private String errorMessage;

  public AuthUserDTO(String accessToken, String refreshToken, 
      User user) {
    this.accessToken = accessToken;
    this.refreshToken = refreshToken;
    this.user = user;
    this.isOK = true;
    this.errorMessage = null;
  }

  public AuthUserDTO(String err) {
    this.accessToken = "";
    this.refreshToken = "";
    this.user = null;
    this.isOK = false;
    this.errorMessage = err;
  }

  public String getErrorMsg(){
    return this.errorMessage;
  }

  public boolean isOK(){
    return this.isOK;
  }

  public String getAccessToken() {
    return accessToken;
  }

  public String getRefreshToken() {
    return refreshToken;
  }

  public User getUser() {
    return user;
  }
}
