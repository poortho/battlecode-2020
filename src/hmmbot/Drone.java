package hmmbot;

import battlecode.common.*;

import java.awt.*;

public class Drone {
    RobotController rc;
    RobotUtil util;
    GameState gameState;

    MapLocation target;


    public Drone(RobotController rc) {
        this.rc = rc;

        this.util = new RobotUtil(this.rc);
        this.gameState = new GameState();


        this.target = null;
    }

    public void run() throws GameActionException {
        while (true) {
            this.util.waitCooldown();

            RobotInfo enemy = this.util.closestRobot(this.util.seeRobots(), RobotType.COW, false);
            if (enemy != null && this.rc.canPickUpUnit(enemy.ID)) {
                this.util.log(String.format("Try to pick up: %d", enemy.ID));

                this.rc.pickUpUnit(enemy.ID);
                Clock.yield();
                continue;
            }


            if (this.target != null) {
                this.util.log(String.format("Target: %d %d", this.target.x, this.target.y));
            }

            MapLocation currentLocation = rc.getLocation();

            if (this.target == null) {
                this.target = this.util.randomLocation();
                this.util.log(String.format("New Target: %d %d", this.target.x, this.target.y));
            }

            if (!this.util.moveTowards(this.target, null)) {
                this.util.log("Can't move!");

                this.target = this.util.randomLocation();
                this.util.log(String.format("New Target: %d %d", this.target.x, this.target.y));
            }
            Clock.yield();
        }
    }

}
