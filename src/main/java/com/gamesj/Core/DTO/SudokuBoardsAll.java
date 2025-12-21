package com.gamesj.Core.DTO;

import java.util.List;

import com.gamesj.Core.Models.SudokuBoard;

public class SudokuBoardsAll {
  private final List<SudokuBoard> boards;
  private final List<String> allNames;
  private final String valid;
  private final String tested;

  public SudokuBoardsAll( List<SudokuBoard> boards, List<String> allNames,
    String valid, String tested ) {
    this.boards = boards;
    this.allNames = allNames;
    this.valid = valid;
    this.tested = tested;
  } 
 
  // getters 
  public List<SudokuBoard> getBoards() {
    return boards;
  }
  public List<String> getAllNames() {
    return allNames;
  }
  public String getValid() {
    return valid;
  } 
  public String getTested() {
    return tested;
  }
}