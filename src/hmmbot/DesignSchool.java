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
        this.gameState = new GameState();
    }

    void run() throws GameActionException {
        for (RobotInfo info : this.util.seeRobots()) {
            // We are next to the HQ, look for it.
            if (this.gameState.hqLocation == null && info.type == RobotType.HQ && info.team == rc.getTeam()) {
                this.gameState.hqLocation = info.location;
            }
        }

        while (true) {
            this.util.waitCooldown();
            if (landscapersCreated < 25) {
                if (this.util.tryBuild(RobotType.LANDSCAPER) != null) {
                    landscapersCreated += 1;
                    continue;
                } else {
                    this.util.log("Failed to create landscaper!");
                }
            }
            Clock.yield();
        }
    }
}
