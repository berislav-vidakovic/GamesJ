package com.gamesj.API.REST;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

public class RequestChecker {
  private static final Pattern UUID_PATTERN =
    Pattern.compile("^[0-9a-fA-F]{8}-" +
                    "[0-9a-fA-F]{4}-" +
                    "[0-9a-fA-F]{4}-" +
                    "[0-9a-fA-F]{4}-" +
                    "[0-9a-fA-F]{12}$");


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

  public static UUID parseIdParameter(String clientId) {
    System.out.println("Parsing clientId: " + clientId);

    if (clientId == null || !UUID_PATTERN.matcher(clientId).matches()) 
      return null; // invalid GUID format
    
    UUID parsedClientId;
    try {
      parsedClientId = UUID.fromString(clientId);
    } 
    catch (IllegalArgumentException e) {
      return null;
    }
    System.out.println("Parsed OK: " + parsedClientId.toString());

    return parsedClientId;
  }

  public static ResponseEntity<?> buildResponseInvalidGuid() {
    Map<String, Object> response = Map.of(
              "error", "Missing or invalid client GUID" );
    return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST); // 400  
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
