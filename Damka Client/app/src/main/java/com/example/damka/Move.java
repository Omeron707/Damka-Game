package com.example.damka;

import java.util.Objects;

public class Move {
    private final int source;
    private final int dest;

    public Move(int s, int d) {
        this.source = s;
        this.dest = d;
    }

    // get row by index (flip for color)
    public int getSourceRow(String color) {
        if (Objects.equals(color, "white")) {
            return source / Constants.BOARD_LENGTH;
        } else {
            return Constants.BOARD_LENGTH - 1 - source / Constants.BOARD_LENGTH;
        }
    }

    // get column by index (flip for color)
    public int getSourceColumn(String color) {
        if (Objects.equals(color, "white")) {
            return source % Constants.BOARD_LENGTH;
        } else {
            return Constants.BOARD_LENGTH - 1 - source % Constants.BOARD_LENGTH;
        }
    }

    // get row by index (flip for color)
    public int getDestRow(String color) {
        if (Objects.equals(color, "white")) {
            return dest / Constants.BOARD_LENGTH;
        } else {
            return Constants.BOARD_LENGTH - 1 - dest / Constants.BOARD_LENGTH;
        }
    }

    // get column by index (flip for color)
    public int getDestColumn(String color) {
        if (Objects.equals(color, "white")) {
            return dest % Constants.BOARD_LENGTH;
        } else {
            return Constants.BOARD_LENGTH - 1 - dest % Constants.BOARD_LENGTH;
        }
    }
}
