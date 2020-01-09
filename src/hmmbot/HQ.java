package hmmbot;

import battlecode.common.*;

public class HQ {
    RobotController rc;
    RobotUtil util;

    int minersCreated = 0;

    public HQ(RobotController rc) {
        this.rc = rc;

        this.util = new RobotUtil(this.rc);
    }

    public void run() throws GameActionException {

        while (minersCreated < 3) {
            this.util.waitCooldown();
            if (this.checkDrones()) {
                Clock.yield();
                continue;
            }
            if (this.util.tryBuild(RobotType.MINER) != null) {
                minersCreated += 1;
                Clock.yield();
                continue;
            }

            Clock.yield();
        }

        while (rc.getTeamSoup() < 500) {
            if (this.checkDrones()) {
                Clock.yield();
                continue;
            }
            Clock.yield();
        }

        while (minersCreated < rc.getTeamSoup() / 700) {
            this.util.waitCooldown();
            if (this.checkDrones()) {
                Clock.yield();
                continue;
            }
            if (this.util.tryBuild(RobotType.MINER) != null) {
                minersCreated += 1;
                for (int i = 0; i < 20; i++)
                    Clock.yield();

                continue;
            }

            Clock.yield();
        }

        while (true) {
            this.util.waitCooldown();
            if (this.checkDrones()) {
                Clock.yield();
                continue;
            }
            if (rc.getTeamSoup() / minersCreated > 400) {
                if (this.util.tryBuild(RobotType.MINER) != null) {
                    minersCreated += 1;
                    continue;
                }
            }
            Clock.yield();
        }
    }

    public boolean checkDrones() throws GameActionException {
        RobotInfo closestDrone = this.util.closestRobot(this.util.seeRobots(), RobotType.DELIVERY_DRONE, false);
        if (closestDrone == null) {
            return false;
        }

        // todo: try to shoot drones that weve seen in the past
        if (rc.canShootUnit(closestDrone.ID)) {
            rc.shootUnit(closestDrone.ID);
            return true;
        }
        return false;

    }
}
