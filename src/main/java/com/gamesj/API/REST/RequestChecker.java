package com.gamesj.API.REST;

import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

public class RequestChecker {
  public static ResponseEntity<?> buildResponseNotFound() {
    return new ResponseEntity<>(
      Map.of("error", "Invalid board - not found in DB"), 
      HttpStatus.NOT_FOUND);   
  }

  public static boolean checkMandatoryFields(List<String> mandatoryFields, List<String> bodyKeys) {
    for( String field : mandatoryFields )
      if (!bodyKeys.contains(field)  ) 
        return false;
    return true;
  }

  public static ResponseEntity<?> buildResponseMissingFields() {
    return new ResponseEntity<>(
      Map.of("error", "Invalid request - missing fields"), 
      HttpStatus.BAD_REQUEST);   
  }

  public static ResponseEntity<?> buildResponseConflict() {
    return new ResponseEntity<>(
      Map.of("error", "Invalid request - item already exists in DB"), 
      HttpStatus.CONFLICT);   
  }
}
