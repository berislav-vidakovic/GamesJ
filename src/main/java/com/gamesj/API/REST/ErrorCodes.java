package com.gamesj.API.REST;

import org.springframework.http.HttpStatus;

public class ErrorCodes {
  public static  HttpStatus toHttpStatus(String errorCode) {
    return switch (errorCode) {
      case "VALIDATION_ERROR" -> HttpStatus.BAD_REQUEST;
      case "USER_EXISTS" -> HttpStatus.CONFLICT;
      default -> HttpStatus.INTERNAL_SERVER_ERROR;
    };
  }
}
