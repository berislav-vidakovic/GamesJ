package com.gamesj.Controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import java.util.Optional;
import java.util.UUID;
import java.io.Console;
import java.util.List;
import java.util.Map;
import com.gamesj.Repositories.SudokuBoardRepo;
import com.gamesj.Services.UserMonitor;
import com.gamesj.Models.SudokuBoard;

@RestController
@RequestMapping("/api/sudoku")
public class SudokuController {

  @Autowired
  private UserMonitor userMonitor;

  private final SudokuBoardRepo sudokuBoardRepository;

  public SudokuController(SudokuBoardRepo sudokuBoardRepository) {
      this.sudokuBoardRepository = sudokuBoardRepository;
  }

  @GetMapping("/board")
  public ResponseEntity<?> getBoards(@RequestParam("id") String clientId) {
    // Validate clientId
    UUID parsedClientId = new UUID(0L, 0L);
    try {
      parsedClientId = UUID.fromString(clientId);
      System.out.println("Received POST /api/sudoku/board with valid ID: " + parsedClientId.toString());

    } 
    catch (IllegalArgumentException e) {
      parsedClientId = UUID.randomUUID();  
      System.out.println("Received POST /api/sudoku/board - NEW ID: " + parsedClientId.toString());

    }
  
    List<SudokuBoard> boards = sudokuBoardRepository.findAll();
    if (boards.isEmpty()) 
      return new ResponseEntity<>(HttpStatus.NO_CONTENT); // 204
    return new ResponseEntity<>(Map.of( "boards", boards, "clientId", parsedClientId ), HttpStatus.OK); // 200
  }
}
