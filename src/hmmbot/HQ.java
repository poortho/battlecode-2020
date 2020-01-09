package hmmbot;

import battlecode.common.*;

public class HQ {
    RobotController rc;
    RobotUtil util;

    int initialMinersCreated = 0;

    public HQ(RobotController rc) {
        this.rc = rc;

        this.util = new RobotUtil(this.rc);
    }

    public void run() throws GameActionException {

        while (initialMinersCreated < 3) {
            this.util.waitCooldown();
            for (Direction dir : Direction.allDirections()) {
                if (this.util.tryBuild(RobotType.MINER, dir) != null) {
                    initialMinersCreated += 1;
                    break;
                }
            }

            Clock.yield();
        }

        while (true) {
            Clock.yield();
        }
    }
}
