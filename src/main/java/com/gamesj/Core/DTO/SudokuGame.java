package com.gamesj.Core.DTO;


public class SudokuGame {
  private final String name;
  private final String board;
  private final String solution;

  public SudokuGame( String name, String board, String solution ) {
    this.name = name;
    this.board = board;
    this.solution = solution;
  } 
 
  // getters 
  public String getName() {
    return name;
  }
  public String getBoard() {
    return board; 
  }
  public String getSolution() {
    return solution;
  }
}