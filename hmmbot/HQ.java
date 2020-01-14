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
        this.util.preTurn();
        this.util.comms.broadcastHqLocation(rc.getLocation().x, rc.getLocation().y);

        while (minersCreated < 3) {
            this.util.waitCooldown();
            if (this.checkDrones()) {
                util.yield();
                continue;
            }
            if (this.util.tryBuild(RobotType.MINER) != null) {
                minersCreated += 1;
                util.yield();
                continue;
            }

            util.yield();
        }

        while (rc.getTeamSoup() < 300 || rc.getRoundNum() < 20) {
            if (this.checkDrones()) {
                util.yield();
                continue;
            }
            util.yield();
        }

        // try to build a miner to build a design school!
        while (true) {
            this.util.waitCooldown();
            if (this.checkDrones()) {
                util.yield();
                continue;
            }
            if (this.util.tryBuild(RobotType.MINER) != null) {
                minersCreated += 1;
                for (int i = 0; i < 11; i++)
                    util.yield();

                break;
            }

            util.yield();
        }

        while (true) {
            this.util.waitCooldown();
            if (this.checkDrones()) {
                util.yield();
                continue;
            }
            if (minersCreated < 15 && minersCreated < rc.getRoundNum() / 10|| rc.getTeamSoup() / minersCreated > 300 - rc.getRoundNum() / 10) {
                if (this.util.tryBuild(RobotType.MINER) != null) {
                    minersCreated += 1;
                    util.yield();
                    continue;
                }
            }
            util.yield();
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
