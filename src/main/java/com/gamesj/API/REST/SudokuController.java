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

import com.gamesj.Core.Models.SudokuBoard;
import com.gamesj.Core.Repositories.SudokuBoardRepo;
import com.gamesj.Core.Services.Sudoku;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

// GET /board
// POST /tested
// POST /solution
// POST /setname
// POST /addgame
@RestController
@RequestMapping("/api/sudoku")
public class SudokuController {

    @Autowired
    SudokuBoardRepo  boardsRepository;

    @Autowired
    Sudoku  sudokuService;
    
    @GetMapping("/board")
    public ResponseEntity<Map<String, Object>> getBoards() {
      List<SudokuBoard> boardsDb = boardsRepository.findAll();
      List<SudokuBoard> boards = new ArrayList<>();
      int testedOK = 0;
      List<String> allNames = new ArrayList<>();

      for( SudokuBoard board : boardsDb ){
        if( sudokuService.evaluate(board) )
          boards.add(board);
        if( board.isTestedOK() )
          ++testedOK;
        allNames.add( board.getName());
      }

      System.out.println("Valid " + boards.size() + " out of " + boardsDb.size() + "board(s)" );
      float valid = (float) boards.size() / boardsDb.size() * 100;
      String validFormatted = String.format("%d%% (%d/%d)", (int)valid, boards.size(), boardsDb.size()); 
      float tested = (float) testedOK / boardsDb.size() * 100;
      String testedFormatted = String.format("%d%% (%d/%d)", (int)tested, testedOK, boardsDb.size()); 

      Map<String, Object> response = Map.of( 
        "boards", boards, 
        "valid", validFormatted,
        "tested", testedFormatted,
        "allNames", allNames
      );

      return new ResponseEntity<>(response, HttpStatus.OK); // 200
    }

    @PostMapping("/tested")
    public ResponseEntity<?> setTestedOK(@RequestParam("id") String clientId, @RequestBody Map<String, String> body) {
      // Request { board, name }
      if (!body.containsKey("board") || !body.containsKey("name") ) 
        return new ResponseEntity<>(
          Map.of("error", "Invalid request - missing board and/or name"), 
          HttpStatus.BAD_REQUEST);         
      else
        System.out.println(" **** **** *** Key(s) contained");

      String boardString = body.get("board");
      Optional<SudokuBoard> boardOpt = boardsRepository.findByBoard(boardString);
      if( boardOpt.isEmpty() )
        return new ResponseEntity<>(
          Map.of("error", "Invalid board - not found in DB"), 
          HttpStatus.NOT_FOUND);       
      else
        System.out.println(" **** **** *** Board found");   

      String name = body.get("name");

      SudokuBoard board = boardOpt.get();
      board.setName(name);
      board.setTestedOK(true);
      boardsRepository.save(board);

      Map<String, Object> response = Map.of( 
        "board", board.getBoard(), "name", name  );
      return new ResponseEntity<>(response, HttpStatus.OK); // 200
    }


    @PostMapping("/solution")
    public ResponseEntity<?> setSolution(@RequestParam("id") String clientId, @RequestBody Map<String, String> body) {
      // Request { board, solution, name }
      if (!body.containsKey("board") || !body.containsKey("solution") 
          || !body.containsKey("name") ) 
        return new ResponseEntity<>(
          Map.of("error", "Invalid request - missing board, name and/or solution"), 
          HttpStatus.BAD_REQUEST);         
      else
        System.out.println(" **** **** *** Key(s) contained");

      String boardString = body.get("board");
      Optional<SudokuBoard> boardOpt = boardsRepository.findByBoard(boardString);
      if( boardOpt.isEmpty() )
        return new ResponseEntity<>(
          Map.of("error", "Invalid board - not found in DB"), 
          HttpStatus.NOT_FOUND);       
      else
        System.out.println(" **** **** *** Board found");   

      SudokuBoard board = boardOpt.get();
      String solution = body.get("solution");
      String name = body.get("name");
      board.setSolution(solution);
      board.setName(name);
      boardsRepository.save(board);

      Map<String, Object> response = Map.of( 
          "board", board.getBoard(),
          "solution", solution, 
          "name", name );
      return new ResponseEntity<>(response, HttpStatus.OK); // 200
    }

    @PostMapping("/setname")
    public ResponseEntity<?> setName(@RequestParam("id") String clientId, @RequestBody Map<String, String> body) {
      // Request { board, name }
      if (!body.containsKey("board") || !body.containsKey("name") ) 
        return new ResponseEntity<>(
          Map.of("error", "Invalid request - missing board and/or name"), 
          HttpStatus.BAD_REQUEST);         
      else
        System.out.println(" **** **** *** Key(s) contained");

      String boardString = body.get("board");
      Optional<SudokuBoard> boardOpt = boardsRepository.findByBoard(boardString);
      if( boardOpt.isEmpty() )
        return new ResponseEntity<>(
          Map.of("error", "Invalid board - not found in DB"), 
          HttpStatus.NOT_FOUND);       
      else
        System.out.println(" **** **** *** Board found");   

      SudokuBoard board = boardOpt.get();
      String name = body.get("name");
      board.setName(name);
      boardsRepository.save(board);

      Map<String, Object> response = Map.of( "board", board.getBoard(), "name", name );
      return new ResponseEntity<>(response, HttpStatus.OK); // 200
    }

    @PostMapping("/addgame")
    public ResponseEntity<?> addNewGame(@RequestParam("id") String clientId, @RequestBody Map<String, String> body) {
      // Request { board, name }
      if (!body.containsKey("board") ) {
        System.out.println(" **** **** *** Invalid request - missing board");

        return new ResponseEntity<>(
          Map.of("error", "Invalid request - missing board"), 
          HttpStatus.BAD_REQUEST);         
        }
      else
        System.out.println(" **** **** *** Key contained");

      String boardString = body.get("board");
      Optional<SudokuBoard> boardOpt = boardsRepository.findByBoard(boardString);
      if( !boardOpt.isEmpty() )
        return new ResponseEntity<>(
          Map.of("error", "Invalid request - board exists"), 
          HttpStatus.CONFLICT);       
      else
        System.out.println(" **** **** *** Board does not exist");   

      
      String name = body.containsKey("name")
        ? body.get("name")
        : UUID.randomUUID().toString().substring(0,15);

      SudokuBoard board = new SudokuBoard(boardString, "", name, (byte)2);
      boardsRepository.save(board);

      Map<String, Object> response = Map.of( "newGame", name );
      return new ResponseEntity<>(response, HttpStatus.CREATED); // 201
    }

}
