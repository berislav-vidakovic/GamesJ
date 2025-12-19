package com.gamesj.Core.Models;

import jakarta.persistence.*;

@Entity
@Table(name = "sudokuboards", uniqueConstraints = {
        @UniqueConstraint(columnNames = "board")
})
public class SudokuBoard {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "board_id")
    private Integer boardId;

    @Column(name = "board", length = 81, nullable = false, unique = true)
    private String board;

    @Column(name = "solution", length = 81, nullable = false)
    private String solution;

    @Column(name = "name", length = 20, nullable = false)
    private String name;

    @Column(name = "level", nullable = false)
    private byte level;  // TINYINT → byte

    @Column(name = "testedOK", nullable = false)
    private boolean testedOK = false;  // default value

    // Constructors
    public SudokuBoard() {}

    public SudokuBoard(String board, String solution, String name, byte level) {
        this.board = board;
        this.solution = solution;
        this.name = name;
        this.level = level;
    }

    // Getters and setters
    public boolean isTestedOK() {
        return testedOK;
    }

    public void setTestedOK(boolean testedOK) {
        this.testedOK = testedOK;
    }

    public Integer getBoardId() {
        return boardId;
    }

    public void setBoardId(Integer boardId) {
        this.boardId = boardId;
    }

    public String getBoard() {
        return board;
    }

    public void setBoard(String board) {
        this.board = board;
    }

    public String getSolution() {
        return solution;
    }

    public void setSolution(String solution) {
        this.solution = solution;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public byte getLevel() {
        return level;
    }

    public void setLevel(byte level) {
        this.level = level;
    }

    // toString (optional)
    @Override
    public String toString() {
        return "SudokuBoard{" +
                "boardId=" + boardId +
                ", board='" + board + '\'' +
                ", solution='" + solution + '\'' +
                ", name='" + name + '\'' +
                ", level=" + level +
                '}';
    }
}
