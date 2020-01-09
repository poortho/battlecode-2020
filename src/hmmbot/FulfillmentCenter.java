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
        this.gameState = new GameState();

    }

    public void run() throws GameActionException {
        while (true) {
            this.util.waitCooldown();
            if (dronesCreated < 100) {
                if (this.util.tryBuild(RobotType.DELIVERY_DRONE) != null) {
                    dronesCreated += 1;
                    continue;
                } else {
                    this.util.log("Failed to create landscaper!");
                }
            }
            Clock.yield();
        }
    }

}
