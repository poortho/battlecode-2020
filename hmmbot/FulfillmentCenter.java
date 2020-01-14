package hmmbot;

import battlecode.common.*;

import java.awt.*;

public class FulfillmentCenter {
    RobotController rc;
    RobotUtil util;
    GameState gameState;

    int dronesCreated = 0;


    public FulfillmentCenter(RobotController rc) {
        this.rc = rc;

        this.util = new RobotUtil(this.rc);
        this.gameState = util.gameState;

    }

    public void run() throws GameActionException {
        util.preTurn();
        while (true) {
            this.util.waitCooldown();
            if (dronesCreated < 100 && (rc.getTeamSoup() > 1100 || rc.getRoundNum() > 400)) {
                if (this.util.tryBuild(RobotType.DELIVERY_DRONE) != null) {
                    dronesCreated += 1;

                    for (int i = 0; i < 30; i++) {
                        util.yield();
                    }

                    continue;
                } else {
                    this.util.log("Failed to create landscaper!");
                }
            }
            util.yield();
        }
    }

}
