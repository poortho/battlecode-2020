package hmmbot;

import battlecode.common.*;

import java.awt.*;

public class Miner {
    RobotController rc;
    RobotUtil util;
    GameState gameState;

    int state;

    MapLocation target;
    MapLocation targetSoupLoc;
    MapLocation refineryLoc;

    static int STATE_INIT = 0;
    static int STATE_MOVING = 1;
    static int STATE_SEARCHING = 2;
    static int STATE_DEPOSITING = 3;

    int initialMinersCreated = 0;

    public Miner(RobotController rc) {
        this.rc = rc;

        this.util = new RobotUtil(this.rc);
        this.gameState = new GameState();

        this.state = STATE_INIT;

        this.target = null;
        this.targetSoupLoc = null;
        this.refineryLoc = null;
    }

    public void run() throws GameActionException {
        if (this.gameState.hqLocation == null) {
            // We are next to the HQ, look for it.

            for (RobotInfo info : this.util.seeRobots()) {
                if (info.type != RobotType.HQ || info.team != rc.getTeam()) {
                    continue;
                }

                this.gameState.hqLocation = info.location;
            }
        }

        this.state = STATE_SEARCHING;

        this.util.log(String.format("HQ Loc: %d %d", this.gameState.hqLocation.x, this.gameState.hqLocation.y));

        this.refineryLoc = this.gameState.hqLocation;

        while (true) {
            this.util.log(String.format("State: %d", this.state));
            if (this.target != null) {
                this.util.log(String.format("Target: %d %d", this.target.x, this.target.y));
            }
            if (this.targetSoupLoc != null) {
                this.util.log(String.format("Target Soup: %d %d", this.targetSoupLoc.x, this.targetSoupLoc.y));
            }
            this.util.log(String.format("Carrying Soup: %d", rc.getSoupCarrying()));


            if (this.targetSoupLoc != null && this.rc.canSenseLocation(this.targetSoupLoc) && this.rc.senseSoup(targetSoupLoc) == 0) {
                this.targetSoupLoc = null;
            }

            MapLocation currentLocation = rc.getLocation();
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

            RobotInfo closestRefineryInfo = this.util.closestRefinery(sensedRobots);
            MapLocation closestRefinery = closestRefineryInfo != null ? closestRefineryInfo.location : this.refineryLoc;
            if (closestRefinery == null || RobotUtil.distanceLinf(currentLocation, this.refineryLoc) <= RobotUtil.distanceLinf(currentLocation, closestRefinery)) {
                closestRefinery = this.refineryLoc;
            }
            if (RobotUtil.distanceLinf(currentLocation, this.gameState.hqLocation) < RobotUtil.distanceLinf(currentLocation, closestRefinery)) {
                closestRefinery = this.gameState.hqLocation;
            }
            this.refineryLoc = closestRefinery;

            if (closestRefinery.isAdjacentTo(currentLocation) && this.rc.getSoupCarrying() > 50) {
                Direction dir = currentLocation.directionTo(closestRefinery);
                if (rc.canDepositSoup(dir)) {
                    rc.depositSoup(dir, this.rc.getSoupCarrying());
                    this.state = STATE_SEARCHING;
                    this.target = this.targetSoupLoc;
                    Clock.yield();
                    continue;
                } else {
                    this.refineryLoc = this.gameState.hqLocation;
                }
            }

            if (this.state == STATE_SEARCHING && rc.getSoupCarrying() > 80) {
                this.state = STATE_DEPOSITING;
            }

            if (this.state == STATE_DEPOSITING) {
                this.target = this.refineryLoc;
            }

            if (this.state == STATE_DEPOSITING) {
                if (RobotUtil.distanceLinf(currentLocation, this.refineryLoc) > 15 && this.rc.getTeamSoup() > RobotType.REFINERY.cost) {
                    MapLocation builtLoc = this.util.tryBuild(RobotType.REFINERY);
                    if (builtLoc != null) {
                        this.refineryLoc = builtLoc;
                        this.target = this.refineryLoc;

                        Clock.yield();
                        continue;
                    }
                }
            }

            if (this.state == STATE_SEARCHING && this.targetSoupLoc == null && this.target == null) {
                this.target = this.util.randomLocation();
                this.util.log(String.format("New Target: %d %d", this.target.x, this.target.y));
            }


            if (!this.util.moveTowards(this.target, null)) {
                this.util.log("Can't move!");

                this.target = this.util.randomLocation();
                this.util.log(String.format("New Target: %d %d", this.target.x, this.target.y));
            }
        }
    }

}
