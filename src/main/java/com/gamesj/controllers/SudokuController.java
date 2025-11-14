package com.gamesj.Controllers;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.Optional;
import java.io.Console;
import java.util.List;
import java.util.Map;
import com.gamesj.Repositories.SudokuBoardRepo;
import com.gamesj.Models.SudokuBoard;

@RestController
@RequestMapping("/api/sudoku")
public class SudokuController {

    private final SudokuBoardRepo sudokuBoardRepository;

    public SudokuController(SudokuBoardRepo sudokuBoardRepository) {
        this.sudokuBoardRepository = sudokuBoardRepository;
    }

    @GetMapping("/board")
    public ResponseEntity<?> getBoards() {
      List<SudokuBoard> boards = sudokuBoardRepository.findAll();
      if (boards.isEmpty()) 
        return new ResponseEntity<>(HttpStatus.NO_CONTENT); // 204
      return new ResponseEntity<>(Map.of( "boards", boards ), HttpStatus.OK); // 200
    }
}
