package com.gamesj.Core.Adapters;

import com.gamesj.Core.Models.User;

// Common contract for user registration results
public class RegisterUserResult {

  private final boolean success;
  private final User user;
  private final String errorCode;
  private final String errorMessage;

  private RegisterUserResult(boolean success, User user, String errorCode, String errorMessage) {
    this.success = success;
    this.user = user;
    this.errorCode = errorCode;
    this.errorMessage = errorMessage;
  }

  public static RegisterUserResult success(User user) {
    return new RegisterUserResult(true, user, null, null);
  }

  public static RegisterUserResult failure(String errorCode, String errorMessage) {
    return new RegisterUserResult(false, null, errorCode, errorMessage);
  }

  // getters
  public boolean isSuccess() { return success; }
  public User getUser() { return user; }
  public String getErrorCode() { return errorCode; }
  public String getErrorMessage() { return errorMessage; }
}
  