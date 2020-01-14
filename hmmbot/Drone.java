package hmmbot;

import battlecode.common.*;

import java.awt.*;

public class Drone {
    RobotController rc;
    RobotUtil util;
    GameState gameState;

    MapLocation target;

    boolean holdingFriend = false;
    boolean domestic = false;
    MapLocation pickedUpLocation;


    public Drone(RobotController rc) {
        this.rc = rc;

        this.util = new RobotUtil(this.rc);
        this.gameState = util.gameState;


        this.target = null;

        this.domestic = rc.getID() % 2 == 1;
        if (domestic) {
            util.log("I'm domestic!");
        }
    }

    public void run() throws GameActionException {
        while (true) {
            this.util.waitCooldown();

            RobotInfo enemy = this.util.closestRobot(this.util.seeRobots(), null, false);
            if (enemy != null && this.rc.canPickUpUnit(enemy.ID)) {
                this.util.log(String.format("Try to pick up: %d", enemy.ID));

                this.rc.pickUpUnit(enemy.ID);
                holdingFriend = false;
                util.yield();
                continue;
            }

            if (domestic) {
                boolean pickedUp = false;
                for (RobotInfo robot : rc.senseNearbyRobots(2, rc.getTeam())) {
                    if (robot != null && robot.type == RobotType.LANDSCAPER && this.rc.canPickUpUnit(robot.ID)) {
                        this.util.log(String.format("Found friend: %d", robot.ID));
                        if (!util.isGoodPosition(robot.location)) {
                            this.util.log(String.format("Try to pick up friend: %d", robot.ID));

                            this.rc.pickUpUnit(robot.ID);
                            holdingFriend = true;
                            pickedUp = true;
                            util.yield();
                            break;
                        }
                    }
                }
                if (pickedUp) {
                    continue;
                }
            } else {
                boolean pickedUp = false;
                for (RobotInfo robot : rc.senseNearbyRobots(2, rc.getTeam())) {
                    if (robot != null && robot.type == RobotType.MINER && Math.random() > 0.5 && this.rc.canPickUpUnit(robot.ID)) {
                        this.util.log(String.format("Found friend miner: %d", robot.ID));
                        if (!util.isGoodPosition(robot.location)) {
                            this.util.log(String.format("Try to pick up friend miner: %d", robot.ID));

                            this.rc.pickUpUnit(robot.ID);
                            pickedUpLocation = robot.location;
                            holdingFriend = true;
                            pickedUp = true;
                            util.yield();
                            break;
                        }
                    }
                }
                if (pickedUp) {
                    continue;
                }

            }

            if (this.rc.isCurrentlyHoldingUnit() && !holdingFriend) {
                boolean dropped = false;
                for (Direction dir : Direction.allDirections()) {
                    MapLocation toDropLoc = rc.getLocation().add(dir);
                    if (rc.canSenseLocation(toDropLoc) && rc.senseFlooding(toDropLoc) && rc.canDropUnit(dir)) {
                        rc.dropUnit(dir);
                        util.yield();
                        dropped = true;
                        break;
                    }
                }

                if (dropped) {
                    continue;
                }
            }

            if (domestic) {
                if (this.rc.isCurrentlyHoldingUnit() && holdingFriend) {
                    boolean dropped = false;
                    for (Direction dir : Direction.allDirections()) {
                        MapLocation toDropLoc = rc.getLocation().add(dir);
                        if (rc.canSenseLocation(toDropLoc) && util.isGoodPosition(toDropLoc) && rc.canDropUnit(dir)) {
                            rc.dropUnit(dir);
                            util.yield();
                            dropped = true;
                            break;
                        }
                    }

                    if (dropped) {
                        continue;
                    }

                }
            } else {
                if (this.rc.isCurrentlyHoldingUnit() && holdingFriend) {
                    boolean dropped = false;
                    for (Direction dir : Direction.allDirections()) {
                        MapLocation toDropLoc = rc.getLocation().add(dir);
                        if (rc.canSenseLocation(toDropLoc) && pickedUpLocation.distanceSquaredTo(toDropLoc) > 5 && Math.random() > 0.9 && rc.canDropUnit(dir) && rc.senseSoup(toDropLoc) != 0) {
                            rc.dropUnit(dir);
                            util.yield();
                            dropped = true;
                            break;
                        }
                    }

                    if (dropped) {
                        continue;
                    }

                }
            }


            if (this.target != null) {
                this.util.log(String.format("Target: %d %d", this.target.x, this.target.y));
            }

            MapLocation currentLocation = rc.getLocation();

            if (this.target == null) {
                if (domestic) {
                    this.target = this.util.randomLocation();
                } else {
                    this.target = this.util.randomLocation(rc.getMapWidth() / 2, rc.getMapHeight() / 2, this.gameState.hqLocation);
                }
                this.util.log(String.format("New Target: %d %d", this.target.x, this.target.y));
            }

            if (!this.util.moveTowards(this.target, null, false)) {
                this.util.log("Can't move!");

                if (domestic) {
                    this.target = this.util.randomLocation();
                } else {
                    this.target = this.util.randomLocation(rc.getMapWidth() / 2, rc.getMapHeight() / 2, this.gameState.hqLocation);
                }
                this.util.log(String.format("New Target: %d %d", this.target.x, this.target.y));
            }
            util.yield();
        }
    }

}
