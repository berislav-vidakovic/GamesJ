package com.gamesj.Repositories;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import com.gamesj.Models.SudokuBoard;


public interface SudokuBoardRepo extends JpaRepository<SudokuBoard, Integer> {
  Optional<SudokuBoard> findByBoard(String board);
}
