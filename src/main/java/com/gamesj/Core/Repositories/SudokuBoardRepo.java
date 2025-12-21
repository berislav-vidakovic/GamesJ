package com.gamesj.Core.Repositories;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import com.gamesj.Core.Models.SudokuBoard;


public interface SudokuBoardRepo extends JpaRepository<SudokuBoard, Integer> {
  Optional<SudokuBoard> findByBoard(String board); // assume there is 0 or 1
  List<SudokuBoard> findAllByName(String name); // 0 or many
}
