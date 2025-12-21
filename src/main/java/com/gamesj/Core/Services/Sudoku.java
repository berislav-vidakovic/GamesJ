package com.gamesj.Core.Services;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.gamesj.Core.DTO.SudokuBoardsAll;
import com.gamesj.Core.DTO.SudokuGame;
import com.gamesj.Core.Models.SudokuBoard;
import com.gamesj.Core.Repositories.SudokuBoardRepo;


@Service
public class Sudoku {
  private final SudokuBoardRepo boardsRepository;

  public Sudoku(SudokuBoardRepo boardsRepository) {
    this.boardsRepository = boardsRepository;
  }

  private boolean evaluate(SudokuBoard sudokuBoard){
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

  public SudokuBoardsAll getBoardsDTO() {
    List<SudokuBoard> boardsDb = boardsRepository.findAll();
    List<SudokuBoard> boards = new ArrayList<>();
    int testedOK = 0;
    List<String> allNames = new ArrayList<>();

    for( SudokuBoard board : boardsDb ){
      if( evaluate(board) )
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

    return new SudokuBoardsAll( boards, allNames, validFormatted, testedFormatted );
  }

  public SudokuGame getDTOtested(String boardString) {
    Optional<SudokuBoard> boardOpt = boardsRepository.findByBoard(boardString);
    if( boardOpt.isEmpty() )
      return null;
   
    SudokuBoard boardEntity = boardOpt.get();
    boardEntity.setTestedOK(true);
    boardsRepository.save(boardEntity);

    return new SudokuGame( boardEntity.getName(), boardString, null);
  }

  public SudokuGame getDTOsolution(String boardString, String solution) {
    Optional<SudokuBoard> boardOpt = boardsRepository.findByBoard(boardString);
    if( boardOpt.isEmpty() )
      return null;
  
    SudokuBoard boardEntity = boardOpt.get();
    boardEntity.setSolution(solution);
    boardsRepository.save(boardEntity);

    return new SudokuGame( boardEntity.getName(), boardString, solution);
  }

  public SudokuGame getDTOname(String boardString, String name) {
    Optional<SudokuBoard> boardOpt = boardsRepository.findByBoard(boardString);
    if( boardOpt.isEmpty() )
      return null;
  
    SudokuBoard boardEntity = boardOpt.get();
    boardEntity.setName(name);
    boardsRepository.save(boardEntity);

    return new SudokuGame( boardEntity.getName(), boardString, null);
  }
  
  public SudokuGame getDTOaddGame(String boardString, String name) {
    Optional<SudokuBoard> boardOpt = boardsRepository.findByBoard(boardString);
    if( !boardOpt.isEmpty() ) // existing board
      return null;
    
    if( name == null || name.isBlank() )
      name = UUID.randomUUID().toString().substring(0,15);
    else
      if( !boardsRepository.findAllByName(name).isEmpty() ) // existing name
        return null;    
    // default level Medium = 2
    SudokuBoard boardEntity = new SudokuBoard(boardString, "", name, (byte)2);
    boardsRepository.save(boardEntity);

    return new SudokuGame( boardEntity.getName(), null, null);
  }
}
