package com.gamesj.Core.Services;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class GameConnect4 extends Game {

    private String color1;   // color for player1
    private String color2;   // color for player2

    private final Object lock = new Object();

    private String board;

    public static final int STATE_INVALID = -1;
    public static final int STATE_WIN = 1;
    public static final int STATE_DRAW = 2;
    public static final int STATE_IN_PROGRESS =3;

    private static final int ROWS = 6;
    private static final int COLUMNS = 7;
    private static final int WIN = 4;

    private static final char RED = 'R';
    private static final char YELLOW = 'Y';
    private static final char EMPTY = '-';

    public GameConnect4(UUID gameId, Player player1, Player player2) {
        super(gameId, player1, player2);
        this.color1 = null;
        this.color2 = null;
        this.board = "------------------------------------------"; // 42 chars
    }

    public String resetBoard() {
        this.board = "------------------------------------------";
        return board;
    }

    public int evaluateBoard() {
        if (!board.matches("^[" + RED + YELLOW + EMPTY + "]+$")) 
            return STATE_INVALID;

        if (gameWin())
            return STATE_WIN;

        if (!board.contains("-"))
            return STATE_DRAW;

        return STATE_IN_PROGRESS;
    }

    private boolean gameWin() {
        // Convert string → matrix
        List<String> matrix = new ArrayList<>();
        for (int i = 0; i < board.length(); i += COLUMNS)
            matrix.add(board.substring(i, i + COLUMNS));
        // --- Horizontal ---
        for (String row : matrix) {
            int len = 1;
            char c = row.charAt(0);
            for (int col = 0; col < COLUMNS - 1; col++) {
                char next = row.charAt(col + 1);
                if (next == c && c != EMPTY)
                    len++;
                else {
                    c = next;
                    len = 1;
                }
                if (len == WIN) return true;
            }
        }
        // --- Vertical ---
        for (int col = 0; col < COLUMNS; col++) {
            int len = 1;
            char c = matrix.get(0).charAt(col);
            for (int row = 0; row < ROWS - 1; row++) {
                char next = matrix.get(row + 1).charAt(col);
                if (next == c && c != EMPTY)
                    len++;
                else {
                    c = next;
                    len = 1;
                }
                if (len == WIN) return true;
            }
        }
        // --- Diagonal falling \ ---
        for (int col = 0; col <= COLUMNS - WIN; col++) {
            for (int row = 0; row <= ROWS - WIN; row++) {
                char c = matrix.get(row).charAt(col);
                if (c == EMPTY) continue;

                int len = 1;
                for (int i = 1; i < WIN; i++) {
                    if (matrix.get(row + i).charAt(col + i) == c)
                        len++;
                    else break;
                    if (len == WIN) return true;
                }
            }
        }
        // --- Diagonal rising / ---
        for (int col = WIN - 1; col < COLUMNS; col++) {
            for (int row = 0; row <= ROWS - WIN; row++) {
                char c = matrix.get(row).charAt(col);
                if (c == EMPTY) continue;

                int len = 1;
                for (int i = 1; i < WIN; i++) {
                    if (matrix.get(row + i).charAt(col - i) == c)
                        len++;
                    else break;
                    if (len == WIN) return true;
                }
            }
        }
        return false;
    }

    public void insertDisk(int userId, int row, int col) {
        int idx = row * COLUMNS + col;
        char c = getUserColor(userId).charAt(0);

        StringBuilder sb = new StringBuilder(board);
        sb.setCharAt(idx, c);
        board = sb.toString();
    }

    public void setBoard(String newBoard) {
        this.board = newBoard;
    }

    public String getBoard() {
        return board;
    }

    public String getAnotherColor(String color) {
        return color.equals("Red") ? "Yellow" : "Red";
    }

    public String getUserColor(int userId) {
        synchronized (lock) {
            if (userId == player1.userId) {
                if (color2 != null)
                    color1 = getAnotherColor(color2);
                else
                    color1 = "Red";
                return color1;

            } else { // userId == player2.userId
                if (color1 != null)
                    color2 = getAnotherColor(color1);
                else
                    color2 = "Red";
                return color2;
            }
        }
    }

    public String getPartnerColor(int userId) {
      return (userId == player1.userId) ? color2 : color1;
    }

    public void swapColors() {
      synchronized (lock) {
        String temp = color1;
        color1 = color2;
        color2 = temp;
      }
    }
}
