package hmmbot;

import battlecode.common.*;

import javax.security.auth.login.AccountLockedException;
import java.awt.*;
import java.util.Map;

public class Miner {
    RobotController rc;
    RobotUtil util;
    GameState gameState;

    int state;
    int birthRound;

    MapLocation target;
    MapLocation targetSoupLoc;
    MapLocation refineryLoc;

    boolean foundDesignSchool = false;
    boolean builtDesignSchool = false;
    boolean builtFulfillmentCenter = false;
    boolean colonizer = false;

    static int STATE_INIT = 0;
    static int STATE_MOVING = 1;
    static int STATE_SEARCHING = 2;
    static int STATE_DEPOSITING = 3;

    int initialMinersCreated = 0;

    public Miner(RobotController rc) {
        this.rc = rc;

        this.util = new RobotUtil(this.rc);
        this.gameState = new GameState(rc.getMapWidth(), rc.getMapHeight());

        this.state = STATE_INIT;

        this.target = null;
        this.targetSoupLoc = null;
        this.refineryLoc = null;

        this.foundDesignSchool = false;
        this.colonizer = rc.getID() % 3 == 1;
        if (colonizer)
            this.util.log("I'm a colonizer!");

    }

    public void run() throws GameActionException {
        this.birthRound = rc.getRoundNum();

        int seenLandscapers = 0;
        for (RobotInfo info : this.util.seeRobots()) {
            // We are next to the HQ, look for it.
            if (this.gameState.hqLocation == null && info.type == RobotType.HQ && info.team == rc.getTeam()) {
                this.gameState.hqLocation = info.location;
            }

            if (info.type == RobotType.LANDSCAPER && info.team == rc.getTeam()) {
                seenLandscapers += 1;
            }
        }

        if (rc.getRoundNum() > 20) {
            // ensure design school
            while (true) {
                int minerCount = 0;
                for (RobotInfo info : this.util.seeRobots()) {
                    // look for landscaper house
                    if (info.type == RobotType.DESIGN_SCHOOL && info.team == rc.getTeam()) {
                        this.foundDesignSchool = true;
                    }
                    if (info.type == RobotType.MINER && info.team == rc.getTeam() && info.location.isAdjacentTo(this.gameState.hqLocation)) {
                        minerCount += 1;
                    }
                }

                if (minerCount > 3) {
                    if (Math.random() > 0.6) {
                        break;
                    }
                }

                if (foundDesignSchool)
                    break;

                for (Direction dir : Direction.cardinalDirections()) {
                    MapLocation toBuild = this.gameState.hqLocation.add(dir);
                    Direction dirToBuild = rc.getLocation().directionTo(toBuild);
                    if (!rc.getLocation().isAdjacentTo(toBuild) || !rc.canBuildRobot(RobotType.DESIGN_SCHOOL, dirToBuild)) {
                        continue;
                    }
                    rc.buildRobot(RobotType.DESIGN_SCHOOL, dirToBuild);



                    foundDesignSchool = true;
                    builtDesignSchool = true;
                }

                if (foundDesignSchool)
                    break;

                Clock.yield();
            }

            this.state = STATE_MOVING;
            this.target = this.util.randomLocation();
            this.util.log(String.format("Initial migration: %d %d", this.target.x, this.target.y));

        } else {
            this.state = STATE_SEARCHING;
        }

        if (rc.getRoundNum() > 100 && rc.getTeamSoup() > 400 && colonizer) {
            util.log("Going to colonize!");

            Direction colonizeDirection = Direction.allDirections()[(int) (Math.random() * (Direction.allDirections().length - 1))]; // fixme: random can return 1 f.
            //colonizeDirection = this.gameState.hqLocation.directionTo(rc.getLocation());
            this.colonize(new MapLocation(this.gameState.hqLocation.x + colonizeDirection.dx * 6, this.gameState.hqLocation.y + colonizeDirection.dy * 6));
        }

        this.util.log(String.format("HQ Loc: %d %d", this.gameState.hqLocation.x, this.gameState.hqLocation.y));

        this.refineryLoc = this.gameState.hqLocation;

        while (true) {
            this.util.waitCooldown();


            this.util.log(String.format("State: %d", this.state));
            if (this.target != null) {
                this.util.log(String.format("Target: %d %d", this.target.x, this.target.y));
            }
            if (this.targetSoupLoc != null) {
                this.util.log(String.format("Target Soup: %d %d", this.targetSoupLoc.x, this.targetSoupLoc.y));
            }
            this.util.log(String.format("Carrying Soup: %d", rc.getSoupCarrying()));

            MapLocation currentLocation = rc.getLocation();

            if (false && builtDesignSchool && rc.getRoundNum() > 300 && rc.getTeamSoup() > 1000 && !this.builtFulfillmentCenter) {
                if (this.util.tryBuild(RobotType.FULFILLMENT_CENTER) != null) {
                    builtFulfillmentCenter = true;
                    Clock.yield();
                    continue;
                }
            }


            if (rc.getRoundNum() > 100 && rc.getTeamSoup() > 400 && colonizer && rc.getSoupCarrying() < 20) {
                util.log("Going to colonize!");

                Direction colonizeDirection = Direction.allDirections()[(int) (Math.random() * (Direction.allDirections().length - 1))]; // fixme: random can return 1 f.
                //colonizeDirection = this.gameState.hqLocation.directionTo(rc.getLocation());
                this.colonize(new MapLocation(this.gameState.hqLocation.x + colonizeDirection.dx * 6, this.gameState.hqLocation.y + colonizeDirection.dy * 6));
            }

            if (this.state == STATE_MOVING && (this.target.equals(currentLocation) || rc.getRoundNum() > this.birthRound + 80)) {
                this.state = STATE_SEARCHING;
            }


            if (this.targetSoupLoc != null && this.rc.canSenseLocation(this.targetSoupLoc) && this.rc.senseSoup(targetSoupLoc) == 0) {
                this.targetSoupLoc = null;
            }

            MapLocation soupLoc = this.util.seeSoup();

            RobotInfo[] sensedRobots = this.util.seeRobots();


            if (soupLoc != null && (this.targetSoupLoc == null || RobotUtil.distanceLinf(currentLocation, soupLoc) < RobotUtil.distanceLinf(currentLocation, this.targetSoupLoc))) {
                this.targetSoupLoc = soupLoc;
            }

            if (this.state == STATE_SEARCHING) {
                this.target = targetSoupLoc;
            }

            if (soupLoc != null && currentLocation.isAdjacentTo(soupLoc)) {
                Direction soupDir = currentLocation.directionTo(soupLoc);
                if (rc.canMineSoup(soupDir)) {
                    rc.mineSoup(soupDir);
                    Clock.yield();
                    continue;
                }
            }

            if (this.rc.getRoundNum() >= 200 && this.refineryLoc == this.gameState.hqLocation) {
                this.refineryLoc = null;
            }

            RobotInfo closestRefineryInfo = this.util.closestRobot(sensedRobots, RobotType.REFINERY, true);
            MapLocation closestRefinery = closestRefineryInfo != null ? closestRefineryInfo.location : this.refineryLoc;
            if (closestRefinery == null || this.refineryLoc != null && RobotUtil.distanceLinf(currentLocation, this.refineryLoc) <= RobotUtil.distanceLinf(currentLocation, closestRefinery)) {
                closestRefinery = this.refineryLoc;
            }
            if (this.rc.getRoundNum() < 200) {
                if (RobotUtil.distanceLinf(currentLocation, this.gameState.hqLocation) < RobotUtil.distanceLinf(currentLocation, closestRefinery)) {
                    closestRefinery = this.gameState.hqLocation;
                }
            }
            this.refineryLoc = closestRefinery;

            if (closestRefinery != null && closestRefinery.isAdjacentTo(currentLocation) && this.rc.getSoupCarrying() > 80) {
                Direction dir = currentLocation.directionTo(closestRefinery);
                if (rc.canDepositSoup(dir)) {
                    rc.depositSoup(dir, this.rc.getSoupCarrying());
                    this.state = STATE_SEARCHING;
                    this.target = this.targetSoupLoc;
                    Clock.yield();
                    continue;
                } else {
                    if (rc.getRoundNum() < 200) {
                        this.refineryLoc = this.gameState.hqLocation;
                    } else {
                        this.refineryLoc = null;
                    }
                }
            }

            if (this.state == STATE_SEARCHING && rc.getSoupCarrying() > 80) {
                this.state = STATE_DEPOSITING;
            }

            if (this.state == STATE_DEPOSITING) {
                if (this.refineryLoc == null || RobotUtil.distanceLinf(currentLocation, this.refineryLoc) > 10 && this.rc.getTeamSoup() > RobotType.REFINERY.cost) {
                    MapLocation builtLoc = null;

                    for (Direction dir : Direction.allDirections()) {
                        if (currentLocation.add(dir).isAdjacentTo(this.gameState.hqLocation)) {
                            continue;
                        }
                        MapLocation tryLoc = this.util.tryBuild(RobotType.REFINERY, dir);
                        if (tryLoc != null) {
                            builtLoc = tryLoc;
                            break;
                        }
                    }

                    if (builtLoc != null) {
                        this.refineryLoc = builtLoc;

                        Clock.yield();
                        continue;
                    }
                }
            }


            if (this.state == STATE_DEPOSITING) {
                this.target = this.refineryLoc;
            }

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

    public void colonize(MapLocation cellCenter) throws GameActionException {
        util.log(String.format("Colonizing %d %d", cellCenter.x, cellCenter.y));

        while (true) {
            this.util.waitCooldown();

            MapLocation currentLocation = rc.getLocation();
            if (currentLocation.equals(cellCenter)) {
                break;
            }

            if (rc.canSenseLocation(cellCenter) && rc.senseRobotAtLocation(cellCenter) != null) {
                return;
            }

            util.moveTowards(cellCenter, null, true);
            util.yield();
        }

        int type;
        if (this.gameState.hqLocation.distanceSquaredTo(cellCenter) == 36) {
            if (this.gameState.hqLocation.x == cellCenter.x) {
                type = 0;
            } else {
                type = 1;
            }
        } else {
            type = 2;
        }

        {
            boolean done = false;
            Direction[] dirs;
            if (type < 2) {
                dirs = new Direction[2];
                dirs[0] = this.gameState.hqLocation.directionTo(cellCenter).rotateLeft();
                dirs[1] = this.gameState.hqLocation.directionTo(cellCenter).rotateRight();
            } else {
                dirs = new Direction[3];
                dirs[0] = this.gameState.hqLocation.directionTo(cellCenter).rotateLeft().rotateLeft();
                dirs[1] = this.gameState.hqLocation.directionTo(cellCenter).rotateRight().rotateRight();
                dirs[2] = this.gameState.hqLocation.directionTo(cellCenter);
            }
            while (!done) {
                this.util.waitCooldown();
                done = true;
                boolean moved = false;
                for (Direction dir : dirs) {
                    Direction realDir = dir;
                    MapLocation toBuild = cellCenter.add(realDir);
                    if (!rc.canSenseLocation(toBuild)) {
                        continue;
                    }
                    RobotInfo robot = rc.senseRobotAtLocation(toBuild);
                    if (robot != null) {
                        if (robot.type != RobotType.NET_GUN) {
                            done = false;
                        }
                        continue;
                    }

                    if (util.tryBuild(RobotType.NET_GUN, realDir) != null) {
                        util.yield();
                        moved = true;
                        done = false;
                        break;
                    }
                }
                if (!moved)
                    util.yield();
            }
        }

        if (type == 2) {
            int vapesMade = 0;
            while (true) {
                this.util.waitCooldown();
                if (rc.getTeamSoup() < 1000 + vapesMade * 550) {
                    continue;
                }

                for (Direction dir : Direction.cardinalDirections()) {
                    if (util.tryBuild(RobotType.VAPORATOR, dir) != null) {
                        vapesMade++;
                        util.yield();
                        break;
                    }
                }
                util.yield();
            }
        } else {
            {
                boolean done = false;
                while (!done) {
                    this.util.waitCooldown();
                    for (Direction dir : Direction.cardinalDirections()) {
                        if (!rc.canMove(dir)) {
                            continue;
                        }

                        rc.move(dir);
                        done = true;
                        util.yield();
                        break;
                    }

                    util.yield();
                }
            }

            {
                while (true) {
                    this.util.waitCooldown();

                    if (util.tryBuild(type == 0 ? RobotType.DESIGN_SCHOOL : RobotType.FULFILLMENT_CENTER, rc.getLocation().directionTo(cellCenter)) != null) {
                        util.yield();
                        break;
                    }
                    util.yield();
                }
            }
        }



    }

}
