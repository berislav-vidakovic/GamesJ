package com.gamesj.Services;

import org.springframework.stereotype.Service;

import com.gamesj.Models.SudokuBoard;

@Service
public class Sudoku {
  public boolean evaluate(SudokuBoard sudokuBoard){
    String board = sudokuBoard.getBoard();
    String solution = sudokuBoard.getSolution();
    
    System.out.println(solution);
    if( solution.contains("0") )
      return false;
    
    return true;
  }
}
