package com.gamesj.Controllers;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.Optional;
import java.util.Map;
import com.gamesj.Repositories.HealthcheckRepository;
import com.gamesj.Models.Healthcheck;

@RestController
@RequestMapping("/api")
public class PingDbController {

    private final HealthcheckRepository healthcheckRepository;

    public PingDbController(HealthcheckRepository healthcheckRepository) {
        this.healthcheckRepository = healthcheckRepository;
    }

    @GetMapping("/pingdb")
    public Map<String, String> pingDb() {
        Optional<Healthcheck> row = healthcheckRepository.findById(1L);
        String message = row.map(Healthcheck::getMsg).orElse("No record found");
        return Map.of("response", message);
    }
}
