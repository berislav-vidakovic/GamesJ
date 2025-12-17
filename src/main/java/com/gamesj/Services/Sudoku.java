package com.gamesj.Services;

import java.util.HashSet;

import org.springframework.stereotype.Service;

import com.gamesj.Models.SudokuBoard;

@Service
public class Sudoku {
  public boolean evaluate(SudokuBoard sudokuBoard){
    String board = sudokuBoard.getBoard();
    String solution = sudokuBoard.getSolution();
    
    System.out.println(solution);
    
    if(  solution.contains("0") )
      return false;

    if( board.length() != 81 || solution.length() != 81 )
      return false;
    for( int i = 0; i < 81; i++ )
      if( board.charAt(i) != solution.charAt(i)  && board.charAt(i) != '0')
        return false;
      
    for( int row = 0; row < 9; row++ ){
      if( !areDigits1to9( solution.substring(row*9,row*9+9)))
        return false;
    }

    for( int col = 0; col < 9; col++ ){
      StringBuilder sbCol = new StringBuilder();
      for( int row = 0; row < 9; row++ )
        sbCol.append(solution.charAt(getStringIdx(row,col)));      
      if( !areDigits1to9( sbCol.toString() ) )
        return false;      
    }

    for( int row = 0; row < 9; row +=3 )
      for( int col = 0; col < 9; col += 3 ){
        StringBuilder sb = new StringBuilder();
        for( int r = 0; r < 3; r++ )
          for( int c =0; c < 3; c++ )
            sb.append(solution.charAt(getStringIdx(row+r, col+c)));
        if( !areDigits1to9( sb.toString() ) )
          return false;              
      }
    
    return true;
  }

  private int getStringIdx(int row, int col) { 
    return row*9+col; 
  }

  private boolean areDigits1to9(String row){
    HashSet<Character> seen = new HashSet<>();
    for( int i = 0; i < 9; i++ ){
      if( row.charAt(i) < '1' || row.charAt(i) > '9')
        return false;
      if( seen.contains(row.charAt(i) ) )
        return false;
      seen.add( row.charAt(i) );
    }
    return seen.size() == 9;
  }
}
