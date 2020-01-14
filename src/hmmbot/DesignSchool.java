package hmmbot;

import battlecode.common.*;

public class DesignSchool {
    RobotController rc;
    RobotUtil util;
    GameState gameState;

    int landscapersCreated = 0;

    public DesignSchool(RobotController rc) {
        this.rc = rc;

        this.util = new RobotUtil(this.rc);
        this.gameState = util.gameState;
    }

    void run() throws GameActionException {
        util.preTurn();

        for (RobotInfo info : this.util.seeRobots()) {
            // We are next to the HQ, look for it.
            if (this.gameState.hqLocation == null && info.type == RobotType.HQ && info.team == rc.getTeam()) {
                this.gameState.hqLocation = info.location;
            }
        }


        if (util.distanceLinf(this.gameState.hqLocation, rc.getLocation()) > 2) {
            runCell();
        }

        while (true) {
            this.util.waitCooldown();
            if (landscapersCreated < 6 && rc.getTeamSoup() > RobotType.LANDSCAPER.cost + RobotType.REFINERY.cost) {
                if (this.util.tryBuild(RobotType.LANDSCAPER) != null) {
                    landscapersCreated += 1;
                    util.yield();
                    continue;
                } else {
                    this.util.log("Failed to create landscaper!");
                }
            }
            util.yield();
        }
    }

    public void runCell() throws GameActionException {
        while (true) {
            this.util.waitCooldown();
            for (Direction dir : Direction.allDirections()) {
                if (rc.getTeamSoup() > 1500 || rc.getRoundNum() > 500) {
                    if (this.util.tryBuild(RobotType.LANDSCAPER, dir) != null) {
                        util.yield();
                        continue;
                    } else {
                        this.util.log("Failed to create landscaper!");
                    }
                }
            }
            util.yield();
        }
/*
            int[] built = new int[4];
            while (true) {
            this.util.waitCooldown();
            for (int i = 0; i < 4; i++) {
                if (built[i] >= 2) {
                    continue;
                }

                Direction dir = Direction.cardinalDirections()[i];
                if (rc.getTeamSoup() > RobotType.LANDSCAPER.cost + RobotType.REFINERY.cost) {
                    if (this.util.tryBuild(RobotType.LANDSCAPER, dir) != null) {
                        built[i] += 1;
                        util.yield();
                        continue;
                    } else {
                        this.util.log("Failed to create landscaper!");
                    }
                }
            }
            util.yield();
        }
        */
    }
}
