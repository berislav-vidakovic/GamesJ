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
import com.gamesj.Services.Sudoku;

import java.io.Console;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestController
public class SudokuController {

    @Autowired
    SudokuBoardRepo  boardsRepository;

    @Autowired
    Sudoku  sudokuService;
    
    @GetMapping("/api/sudoku/board")
    public ResponseEntity<Map<String, Object>> getBoards() {
      List<SudokuBoard> boardsDb = boardsRepository.findAll();
      List<SudokuBoard> boards = new ArrayList<>();

      for( SudokuBoard board : boardsDb ){
        if( sudokuService.evaluate(board) )
          boards.add(board);
      }

      System.out.println("Valid " + boards.size() + " out of " + boardsDb.size() + "board(s)" );

      Map<String, Object> response = Map.of( "boards", boards );
      return new ResponseEntity<>(response, HttpStatus.OK); // 200
    }

}
