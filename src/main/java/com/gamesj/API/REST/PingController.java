// PingController.java
package com.gamesj.API.REST;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.Map;

@RestController
public class PingController {

    @GetMapping("/api/ping")
    public ResponseEntity<Map<String, Object>> ping() {
      Map<String, Object> response = Map.of( "response", "pong" );
      return new ResponseEntity<>(response, HttpStatus.OK); // 200
    }
}
