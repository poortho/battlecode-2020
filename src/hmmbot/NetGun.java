package hmmbot;

import battlecode.common.*;

public class NetGun {
    RobotController rc;
    RobotUtil util;

    int minersCreated = 0;

    public NetGun(RobotController rc) {
        this.rc = rc;

        this.util = new RobotUtil(this.rc);
    }

    public void run() throws GameActionException {
        this.util.preTurn();

        while (true) {
            this.checkDrones();
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
