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

    // Add CORS headers
    response.setHeader("Access-Control-Allow-Origin", "http://localhost:5174");
    response.setHeader("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
    response.setHeader("Access-Control-Allow-Headers", "Authorization, Content-Type");
    response.setHeader("Access-Control-Allow-Credentials", "true");

    // Allow preflight requests (CORS) to pass through
    if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
        response.setStatus(HttpServletResponse.SC_OK);
        filterChain.doFilter(request, response); // continue chain
        return;
    }

    String header = request.getHeader("Authorization");

    // Allow endpoints without token
    String path = request.getRequestURI();
    if( path.startsWith("/api/ping") ||
        path.startsWith("/api/pingdb") ||
        path.startsWith("/api/users/all") ||
        path.startsWith("/api/users/new") ||
        path.startsWith("/favicon.ico") || 
        path.startsWith("/websocket") ||
        path.startsWith("/images") ||
        path.startsWith("/api/auth/refresh") ||
        path.startsWith("/api/users/login") ||
        path.startsWith("/api/users/logout") ||
        path.startsWith("/api/localization/get") ) {
          filterChain.doFilter(request, response);
          return;
    }
    if (header != null && header.startsWith("Bearer ")) {
      String token = header.substring(7);
      try {
        Claims claims = Jwts.parserBuilder()
                .setSigningKey(JwtUtil.getSecretKey())  // from JwtUtil class
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
