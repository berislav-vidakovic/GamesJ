package com.gamesj.API.REST;

import org.springframework.web.bind.annotation.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import com.gamesj.Core.Services.Locales;
import com.gamesj.Core.Models.Localization;

import java.util.*;

// GET /get
@RestController
@RequestMapping("/api/localization")
public class LocalizationController {

  private final Locales localesService;

  public LocalizationController(Locales localesService) {
      this.localesService = localesService;
  }

  @GetMapping("/get")
  public ResponseEntity<Map<String, Object>> getLocalization(@RequestParam("id") String clientId) {
    try {
      UUID parsedClientId = RequestChecker.parseIdParameter(clientId);
      if( parsedClientId == null )
        return new ResponseEntity<>(HttpStatus.BAD_REQUEST); 

      List<Localization> locales = localesService.getLocales();        
      if ( locales.isEmpty() ) 
        return new ResponseEntity<>(HttpStatus.NO_CONTENT); //  204 
      Map<String, Object> response = Map.of("locales", locales);

      return new ResponseEntity<>(response, HttpStatus.OK); // 200          
    } 
    catch (Exception ex) {
      ex.printStackTrace();
      Map<String, Object> errorResponse = Map.of("error", "Database connection failed");
      return new ResponseEntity<>(errorResponse, HttpStatus.SERVICE_UNAVAILABLE); // 503 
    }
  }
}
