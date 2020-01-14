package hmmbot;

import battlecode.common.MapLocation;

public class GameState {
    MapLocation hqLocation;

    int[][] cells;

    public GameState(int width, int height) {
        this.cells = new int[(width - 1) / 6][(height - 1) / 6];
    }
}
