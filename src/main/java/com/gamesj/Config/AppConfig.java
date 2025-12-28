package com.gamesj.Config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.filter.ForwardedHeaderFilter;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class AppConfig {

    @Bean
    public ForwardedHeaderFilter forwardedHeaderFilter() {
      return new ForwardedHeaderFilter();
    }

    @Bean
    public WebMvcConfigurer corsConfigurer() {
      return new WebMvcConfigurer() {
        @Override
        public void addCorsMappings(CorsRegistry registry) {
            registry.addMapping("/**")
                    .allowedOrigins("http://localhost:5174",
                      "http://localhost:5175", 
                      "http://localhost:5176", 
                      "https://gamesj.barryonweb.com",
                      "https://gamesjclient.barryonweb.com")
                    .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                    .allowedHeaders("*") // GraphQL required
                    .allowCredentials(true);
        }
      };
    }
}
