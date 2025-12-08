// SudokuController.java
package com.gamesj.Controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import com.gamesj.Models.SudokuBoard;
import com.gamesj.Models.User;
import com.gamesj.Repositories.SudokuBoardRepo;

import java.io.Console;
import java.util.List;
import java.util.Map;

@RestController
public class SudokuController {

    @Autowired
    SudokuBoardRepo  boardsRepository;
    
    @GetMapping("/api/sudoku/board")
    public ResponseEntity<Map<String, Object>> getBoards() {
      List<SudokuBoard> boards = boardsRepository.findAll();
      Map<String, Object> response = Map.of( "boards", boards );
      return new ResponseEntity<>(response, HttpStatus.OK); // 200
    }
    
}
