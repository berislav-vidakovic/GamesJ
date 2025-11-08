package com.gamesj.Controllers;

import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import com.gamesj.Repositories.LocalizationRepository;
import com.gamesj.Models.Localization;

import java.util.*;

@RestController
@RequestMapping("/api/localization")
public class LocalizationController {

    private final LocalizationRepository localizationRepository;

    public LocalizationController(LocalizationRepository localizationRepository) {
        this.localizationRepository = localizationRepository;
    }

    @GetMapping("/get")
    public ResponseEntity<Map<String, Object>> getLocalization() {
        List<Localization> locales = localizationRepository.findAll();
        Map<String, Object> response = Map.of("locales", locales);
        return ResponseEntity.ok(response);
    }
}
