package com.gamesj.Core.Services;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import com.gamesj.Core.Repositories.UserRepository;
import com.gamesj.Core.Repositories.RefreshTokenRepository;
import com.gamesj.Core.DTO.AuthUserDTO;
import com.gamesj.Config.JwtBuilder;
import com.gamesj.Core.Models.RefreshToken;
import com.gamesj.Core.Models.User;

import com.fasterxml.jackson.databind.ObjectMapper;

/* Middleware Service for user authentication
  Usage:
    AuthUserDTO authResult = authentication.authenticate(userId, password);
    if( !authResult.isOK() ){
       // handle auth failure
    }
    // proceed with authResult.getUser(), authResult.getAccessToken(), authResult.getRefreshToken()
  Methods:
    authenticate(userId, password) 
      - Find User entity in UserRepository by provided userId
      - Validate provided password with hashed password in UserRepository
      - on Error return new AuthUserDTO(errMsg)
      - Provide User entity argument to buildAuthUser
    authenticate(refreshToken) 
      - Find RefreshToken entity in RefreshTokenRepository by provided refreshToken
      - Find User entity in UserRepository by userId from RefreshToken
      - on Error return new AuthUserDTO(errMsg)
      - Delete found RefreshToken from RefreshTokenRepository
      - Provide User entity argument to buildAuthUser
    common private method buildAuthUser(user) 
      - generate new JWT access token
      - generate new refreshToken
      - create new RefreshToken entity
          - new refreshToken string
          - userId from provided user entity parameter
          - new expiry date
          - save to Repository
      - set user online and save to UserRepository
      - return new AuthUserDTO(accessToken, refreshToken, user)
    logout(userId)
      - Find User entity in UserRepository by provided userId
      - set user offline and save to UserRepository
      - delete all RefreshToken entities for userId from RefreshTokenRepository
      - return new AuthUserDTO(null,null,userID) for success
*/

// Spring-managed singleton 
@Service
public class Authentication  {
    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    public AuthUserDTO authenticate(String userIdStr, String password){
      if( userIdStr == null || password == null || password.isEmpty() )
        return new AuthUserDTO("Missing userId or password");

      Integer userId;
      try {
        userId = Integer.valueOf(userIdStr);
      } catch (NumberFormatException e) {
        return new AuthUserDTO("Invalid userId");
      }
      
      // Find user
      Optional<User> optionalUser = userRepository.findById(userId);
      if (optionalUser.isEmpty()) 
        return new AuthUserDTO("UserID Not found");
      User user = optionalUser.get();

      // Password validation 
      // if no hashed pwd in DB => new user, first time password hashing
      if( user.getPwd().isEmpty() ){
        // Hash the password using BCrypt
        String hashedPwd = passwordEncoder.encode(password);
        user.setPwd(hashedPwd);
        userRepository.save(user);
      }       
      else {
        boolean passwordsMatch = passwordEncoder.matches(password, user.getPwd());
        if( !passwordsMatch )
          return new AuthUserDTO( "Invalid password" );
      }
      System.out.println("authenticate OK"); 
      return buildAuthUser(user);
    }

    public AuthUserDTO authenticate(String refreshToken){
      RefreshToken refTokenEntity = checkReceivedToken(refreshToken);
      if( refTokenEntity == null ) 
        return new AuthUserDTO("Refresh token missing, invalid or expired");
      // valid token - get user by userId from refreshToken 
      var userOpt = userRepository.findById(refTokenEntity.getUserId());
      if (userOpt.isEmpty()) 
        return new AuthUserDTO("User not found for the provided refresh token");
      
      // delete existing refreshToken
      //refreshTokenRepository.delete(refTokenEntity);
      return buildAuthUser(userOpt.get());
    }

    public AuthUserDTO logout(String userIdStr){
      if( userIdStr == null || userIdStr.isEmpty() )
        return new AuthUserDTO("Missing userId");
      Integer userId;
      try {
        userId = Integer.valueOf(userIdStr);
      } catch (NumberFormatException e) {
        return new AuthUserDTO("Invalid userId");
      }

      // Find user
      Optional<User> optionalUser = userRepository.findById(userId);
      if (optionalUser.isEmpty()) 
        return new AuthUserDTO("User not found");
      
      User user = optionalUser.get();
      user.setIsOnline(false);
      userRepository.save(user);

      // Clear refresh token from DB
      System.out.println("Deleting refresh tokens for userId: " + userId);
      refreshTokenRepository.deleteByUserId(userId);
      System.out.println("Deleting done ");

      return new AuthUserDTO(null,null,user);
    }

    // Private helper methods
    private AuthUserDTO buildAuthUser(User user) {
      String accessToken = JwtBuilder.generateToken(
              user.getUserId(), user.getLogin() );

      refreshTokenRepository.deleteByUserId(user.getUserId());      
      RefreshToken tokenEntity = new RefreshToken(user.getUserId());
      String refreshToken = renewAndStoreRefreshToken(tokenEntity);
      // Set user online
      user.setIsOnline(true);
      userRepository.save(user);
      return new AuthUserDTO(accessToken, refreshToken, user);
    }

    private RefreshToken checkReceivedToken(String refreshToken) {
      if (refreshToken == null || refreshToken.isEmpty()) 
          return null; // Missing token
        
      Optional<RefreshToken> tokenOpt = refreshTokenRepository.findByToken(refreshToken);
      if (tokenOpt.isEmpty()) 
          return null; // Token not found in DB

      RefreshToken token = tokenOpt.get();
      if (token.getExpiresAt().isBefore(LocalDateTime.now())) 
          return null; // Token expired

      return token; // Valid Token
    }

    private String renewAndStoreRefreshToken(RefreshToken tokenEntity){
      String refreshToken = java.util.UUID.randomUUID().toString();
      LocalDateTime expiresAt = LocalDateTime.now().plusDays(7);
      tokenEntity.setToken(refreshToken);
      tokenEntity.setExpiresAt(expiresAt);
      refreshTokenRepository.save(tokenEntity);
      return refreshToken;
    }



}
