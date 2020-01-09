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
            if (this.util.tryBuild(RobotType.MINER) != null) {
                minersCreated += 1;
                Clock.yield();
                continue;
            }

            Clock.yield();
        }

        while (rc.getTeamSoup() < 500) {
            Clock.yield();
        }

        while (minersCreated < rc.getTeamSoup() / 700) {
            this.util.waitCooldown();
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
            if (rc.getTeamSoup() / minersCreated > 400) {
                if (this.util.tryBuild(RobotType.MINER) != null) {
                    minersCreated += 1;
                    continue;
                }
            }
            Clock.yield();
        }
    }
}
