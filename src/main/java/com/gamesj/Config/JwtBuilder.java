package com.gamesj.Config;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import java.security.Key;
import java.util.Date;

public class JwtBuilder {

    private static final Key SECRET_KEY = Keys.hmacShaKeyFor("KeyForJWTauthenticationInGamesProject".getBytes());
    private static final long EXPIRATION_TIME_MS = 60*60*1000; // 1 hour

    public static Key getSecretKey() {
        return SECRET_KEY;
    }

    public static String generateToken(int userId, String username) {
      return Jwts.builder()
              .setSubject(username)
              .claim("userId", userId)
              .setIssuedAt(new Date())
              .setExpiration(new Date(System.currentTimeMillis() + EXPIRATION_TIME_MS))
              .signWith(SECRET_KEY, SignatureAlgorithm.HS256)
              .compact();
    }
}
