// SudokuController.java
package com.gamesj.API.REST;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.gamesj.Core.Services.Sudoku;

import com.gamesj.Core.DTO.SudokuBoardsAll;
import com.gamesj.Core.DTO.SudokuGame;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

// GET /board
// POST /tested
// POST /solution
// POST /setname
// POST /addgame
@RestController
@RequestMapping("/api/sudoku")
public class SudokuController {

    @Autowired
    Sudoku  sudokuService;
    
    @GetMapping("/board")
    public ResponseEntity<Map<String, Object>> getBoards() {
      SudokuBoardsAll dto = sudokuService.getBoardsDTO();
      Map<String, Object> response = Map.of( 
        "boards", dto.getBoards(), 
        "valid", dto.getValid(),
        "tested", dto.getTested(),
        "allNames", dto.getAllNames()
      );
      return new ResponseEntity<>(response, HttpStatus.OK); // 200
    }

    @PostMapping("/tested")
    public ResponseEntity<?> setTestedOK(@RequestParam("id") String clientId, @RequestBody Map<String, String> body) {
      // Request { board }
      if( !RequestChecker.checkMandatoryFields(List.of("board"), new ArrayList<>(body.keySet())) )
        return RequestChecker.buildResponseMissingFields();
      SudokuGame dtoGame = sudokuService.getDTOtested( body.get("board") );
      if( dtoGame == null )
        return RequestChecker.buildResponseNotFound();
      Map<String, Object> response = Map.of( 
        "board", dtoGame.getBoard(), "name", dtoGame.getName()  );
      return new ResponseEntity<>(response, HttpStatus.OK); // 200
    }

    @PostMapping("/solution")
    public ResponseEntity<?> setSolution(@RequestParam("id") String clientId, @RequestBody Map<String, String> body) {
      // Request { board, solution }
      if( !RequestChecker.checkMandatoryFields(List.of("board", "solution"), new ArrayList<>(body.keySet())) )
        return RequestChecker.buildResponseMissingFields();
      SudokuGame dtoGame = sudokuService.getDTOsolution( body.get("board"), body.get("solution") );
      if( dtoGame == null )
        return RequestChecker.buildResponseNotFound();
      Map<String, Object> response = Map.of( 
          "board", dtoGame.getBoard(),
          "solution", dtoGame.getSolution(), 
          "name", dtoGame.getName() );
      return new ResponseEntity<>(response, HttpStatus.OK); // 200
    }

    @PostMapping("/setname")
    public ResponseEntity<?> setName(@RequestParam("id") String clientId, @RequestBody Map<String, String> body) {
      // Request { board, name }
      if( !RequestChecker.checkMandatoryFields(List.of("board", "name"), new ArrayList<>(body.keySet())) )
        return RequestChecker.buildResponseMissingFields();
      SudokuGame dtoGame = sudokuService.getDTOname( body.get("board"), body.get("name") );
      if( dtoGame == null )
        return RequestChecker.buildResponseNotFound();
      Map<String, Object> response = Map.of( 
        "board", dtoGame.getBoard(), "name", dtoGame.getName() );
      return new ResponseEntity<>(response, HttpStatus.OK); // 200
    }

    @PostMapping("/addgame")
    public ResponseEntity<?> addNewGame(@RequestParam("id") String clientId, @RequestBody Map<String, String> body) {
      // Request { board, name } - name field is optional
      if( !RequestChecker.checkMandatoryFields(List.of("board"), new ArrayList<>(body.keySet())) )
        return RequestChecker.buildResponseMissingFields();
      SudokuGame dtoGame = sudokuService.getDTOaddGame( body.get("board"), body.get("name") );
      if( dtoGame == null )
        return RequestChecker.buildResponseConflict();
      Map<String, Object> response = Map.of( "name", dtoGame.getName() );
      return new ResponseEntity<>(response, HttpStatus.CREATED); // 201
    }
}
