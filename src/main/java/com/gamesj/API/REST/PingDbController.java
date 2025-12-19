package com.gamesj.API.REST;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.Optional;
import java.util.Map;
import com.gamesj.Core.Repositories.HealthcheckRepository;
import com.gamesj.Core.Models.Healthcheck;

@RestController
@RequestMapping("/api")
public class PingDbController {

    private final HealthcheckRepository healthcheckRepository;

    public PingDbController(HealthcheckRepository healthcheckRepository) {
        this.healthcheckRepository = healthcheckRepository;
    }

    @GetMapping("/pingdb")
    public ResponseEntity<Map<String, Object>> pingDb() {
      try {
        Optional<Healthcheck> row = healthcheckRepository.findTopByOrderByIdAsc();
        if (!row.isPresent()) 
          return new ResponseEntity<>(HttpStatus.NO_CONTENT); //  204         
        Map<String, Object> response = Map.of("response", row.get().getMsg());
        return new ResponseEntity<>(response, HttpStatus.OK); // 200 
      } 
      catch (Exception ex) {
        ex.printStackTrace();
        Map<String, Object> errorResponse = Map.of("error", "Database connection failed");
        return new ResponseEntity<>(errorResponse, HttpStatus.SERVICE_UNAVAILABLE); // 503 
      }
    }
}
