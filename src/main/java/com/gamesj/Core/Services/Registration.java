package com.gamesj.Core.Services;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.gamesj.Core.Adapters.RegisterUserResult;
import com.gamesj.Core.Models.User;
import com.gamesj.Core.Repositories.UserRepository;

@Service
public class Registration {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public Registration(
        UserRepository userRepository,
        PasswordEncoder passwordEncoder
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public RegisterUserResult register(String login, String fullName, String password) {
      if (login == null || login.isBlank()
        || fullName == null || fullName.isBlank()
        || password == null || password.isBlank()) 
            return RegisterUserResult.failure(
                "VALIDATION_ERROR",
                "Missing login or fullname or password"
            );

      if (userRepository.existsByLoginOrFullName(login, fullName)) {
          return RegisterUserResult.failure(
              "USER_EXISTS",
              "User already exists"
          );
      }

      User user = new User();
      user.setLogin(login);
      user.setFullName(fullName);
      user.setPwd(passwordEncoder.encode(password));
      user.setIsOnline(false);
      userRepository.save(user);

      return RegisterUserResult.success(user);
  }
}
