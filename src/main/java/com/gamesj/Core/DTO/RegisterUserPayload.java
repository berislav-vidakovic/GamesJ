package com.gamesj.Core.DTO;

import com.gamesj.Core.Models.User;

public class RegisterUserPayload {

    private boolean acknowledged;
    private User user;
    private String error;

    public RegisterUserPayload(boolean acknowledged, User user, String error) {
      this.acknowledged = acknowledged;
      this.user = user;
      this.error = error;
    }

    public boolean isAcknowledged() { return acknowledged; }
    public User getUser() { return user; }
    public String getError() { return error; }
}
