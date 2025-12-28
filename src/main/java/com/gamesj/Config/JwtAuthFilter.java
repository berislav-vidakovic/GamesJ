package com.gamesj.Config;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class JwtAuthFilter extends OncePerRequestFilter {
  @Override
  protected void doFilterInternal(HttpServletRequest request,
                                  HttpServletResponse response,
                                  FilterChain filterChain)
          throws ServletException, IOException {

    if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
        filterChain.doFilter(request, response); // let Spring handle it
        return;
    }
   // Allow endpoints without token
    String path = request.getRequestURI();
    if( path.equals("/favicon.ico") ||
        path.startsWith("/api/ping") ||
        path.startsWith("/api/pingdb") ||
        path.startsWith("/api/users/all") ||
        path.startsWith("/api/users/new") ||
        path.startsWith("/api/sudoku/board") ||
        path.startsWith("/api/sudoku/tested") ||
        path.startsWith("/api/sudoku/addgame") ||
        path.startsWith("/api/sudoku/solution") ||
        path.startsWith("/api/sudoku/setname") ||
        path.startsWith("/favicon.ico") || 
        path.startsWith("/websocket") ||
        path.startsWith("/images") ||
        path.startsWith("/api/auth/refresh") ||
        path.startsWith("/api/auth/login") ||
        path.startsWith("/api/auth/logout") ||
        path.startsWith("/api/games/init") ||
        path.startsWith("/api/localization/get") 
         || path.startsWith("/graphql")
      ) {
          filterChain.doFilter(request, response);
          return;
    }

    String header = request.getHeader("Authorization");
    if (header != null && header.startsWith("Bearer ")) {
      String token = header.substring(7);
      try {
        Claims claims = Jwts.parserBuilder()
                .setSigningKey(JwtBuilder.getSecretKey())  // from JwtBuilder class
                .build()
                .parseClaimsJws(token)
                .getBody();

        request.setAttribute("userId", claims.getSubject());
      } 
      catch (Exception e) {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.getWriter().write("Invalid or expired token");
        return;
      }
    } 
    else {
      response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
      response.getWriter().write("Missing Authorization header");
      return;
    }
    filterChain.doFilter(request, response);
  }
}
