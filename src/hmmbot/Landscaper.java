package hmmbot;

import battlecode.common.*;

import java.awt.*;

public class Landscaper {
    RobotController rc;
    RobotUtil util;
    GameState gameState;

    MapLocation designSchoolLoc = null;


    public Landscaper(RobotController rc) {
        this.rc = rc;

        this.util = new RobotUtil(this.rc);
        this.gameState = this.util.gameState;
    }

    public void run() throws GameActionException {
        util.preTurn();
        int startRound = rc.getRoundNum();

        for (RobotInfo info : this.util.seeRobots()) {
            // We are next to the HQ, look for it.
            if (this.gameState.hqLocation == null && info.type == RobotType.HQ && info.team == rc.getTeam()) {
                this.gameState.hqLocation = info.location;
            }
        }

        for (RobotInfo info : rc.senseNearbyRobots(2)) {
            // We are next to the Design School, look for it.
            if (this.designSchoolLoc == null && info.type == RobotType.DESIGN_SCHOOL && info.team == rc.getTeam()) {
                this.designSchoolLoc = info.location;
            }
        }

        if (util.distanceLinf(this.gameState.hqLocation, rc.getLocation()) > 2) {
            /*MapLocation[] centers = new MapLocation[3];

            Direction normal = this.gameState.hqLocation.directionTo(this.designSchoolLoc);
            Direction left = normal.rotateLeft().rotateLeft();
            Direction right = normal.rotateRight().rotateRight();
            centers[0] = this.designSchoolLoc;
            centers[1] = new MapLocation(this.designSchoolLoc.x + left.dx * 6, this.designSchoolLoc.y + left.dy * 6);
            centers[2] = new MapLocation(this.designSchoolLoc.x + right.dx * 6, this.designSchoolLoc.y + right.dy * 6);

            runCell(centers[(int) (Math.random() * 3)]);*/
            MapLocation[] centers = new MapLocation[8];

            for (int i = 0; i < 8; i++) {
                Direction dir = Direction.allDirections()[i];
                centers[i] = new MapLocation(this.gameState.hqLocation.x + dir.dx * 6, gameState.hqLocation.y + dir.dy * 6);
            }
            runCell(centers[(int) (Math.random() * 8)]);
        }

        MapLocation assignment = null;
        MapLocation tryAssignment = null;

        Direction designSchoolOrientation = this.designSchoolLoc.directionTo(this.gameState.hqLocation);
        MapLocation theCoolSpot = this.gameState.hqLocation.add(designSchoolOrientation);
        while (rc.getRoundNum() < startRound + 30) {
            this.util.waitCooldown();


            boolean[][] occupancy = new boolean[5][5];
            for (RobotInfo info : this.util.seeRobots()) {
                int dx = info.location.x - this.gameState.hqLocation.x;
                int dy = info.location.y - this.gameState.hqLocation.y;

                if (-2 <= dx && dx <= 2 && -2 <= dy && dy <= 2) {
                    occupancy[dx + 2][dy + 2] = true;
                }
            }

            MapLocation bestAssignment = rc.getLocation();
            int bestAssignmentScore = theCoolSpot.distanceSquaredTo(rc.getLocation());
            for (int i = -3; i < 3; i++) {
                for (int j = -3; j < 3; j++) {
                    MapLocation potentialAssignment = new MapLocation(this.rc.getLocation().x + i, this.rc.getLocation().y + j);
                    if (RobotUtil.distanceLinf(this.gameState.hqLocation, potentialAssignment) > 2)
                        continue;

                    int dx = potentialAssignment.x - this.gameState.hqLocation.x;
                    int dy = potentialAssignment.y - this.gameState.hqLocation.y;

                    int assignmentScore = theCoolSpot.distanceSquaredTo(potentialAssignment) * 10;
                    //assignmentScore -= this.designSchoolLoc.distanceSquaredTo(potentialAssignment) * 10;
                    assignmentScore += RobotUtil.distanceLinf(rc.getLocation(), potentialAssignment) * 2;

                    if (rc.canSenseLocation(potentialAssignment)) {
                        if (occupancy[dx + 2][dy + 2] || rc.senseFlooding(potentialAssignment)) {
                            assignmentScore += 1000;
                        }
                    }

                    if (RobotUtil.distanceLinf(this.gameState.hqLocation, potentialAssignment) == 1) {
                        assignmentScore -= 100;
                    }

                    if (this.gameState.hqLocation.distanceSquaredTo(potentialAssignment) == 2) {
                        assignmentScore -= 100;
                    }

                    assignmentScore -= RobotUtil.distanceLinf(rc.getLocation(), potentialAssignment);

                    if (assignmentScore < bestAssignmentScore) {
                        bestAssignment = potentialAssignment;
                        bestAssignmentScore = assignmentScore;
                    }
                }
            }

            this.util.log(String.format("Winner: %d %d, score: %d", bestAssignment.x, bestAssignment.y, bestAssignmentScore));

            tryAssignment = bestAssignment;

            if (tryAssignment != null) {
                this.util.moveTowardsBounded(tryAssignment, null, true, this.gameState.hqLocation, 2);
            }
            util.yield();
        }

        assignment = this.rc.getLocation();


        this.util.log("Arrived at assignment!");

        for (int i = 0; i < 100 + (RobotUtil.distanceLinf(this.gameState.hqLocation, assignment) == 2 ? 50 : 50); i++) {
            util.yield();
        }

        while (true) {
            this.util.waitCooldown();

            if (rc.getDirtCarrying() < 20 || (this.rc.getRoundNum() < startRound + 60 && rc.getDirtCarrying() != RobotType.LANDSCAPER.dirtLimit)) {
                Direction highestDir = null;
                int highest = -1 << 30;
                for (Direction dir : Direction.allDirections()) {
                    MapLocation toDig = rc.getLocation().add(dir);
                    if (rc.getRoundNum() < startRound + 100 && toDig.isAdjacentTo(this.designSchoolLoc)) {
                        continue;
                    }
                    if (toDig.equals(this.designSchoolLoc)) { // fixme: this broke
                        continue;
                    }
                    if (!rc.canDigDirt(dir)) {
                        continue;
                    }
                    if (toDig.equals(this.gameState.hqLocation) && this.rc.canSenseLocation(this.gameState.hqLocation) &&
                            this.rc.canDigDirt(rc.getLocation().directionTo(this.gameState.hqLocation))) {
                        highest = 1 << 30;
                        highestDir = rc.getLocation().directionTo(this.gameState.hqLocation);
                        break;
                    }
                    if (this.util.distanceLinf(assignment, this.gameState.hqLocation) == 2) {
                        if (RobotUtil.distanceLinf(toDig, this.gameState.hqLocation) != 3) {
                            continue;
                        }
                    } else {
                        if (RobotUtil.distanceLinf(toDig, this.gameState.hqLocation) != 1 || (this.rc.canSenseLocation(toDig) && this.rc.senseRobotAtLocation(toDig) == null)) {
                            continue;
                        }

                    }
                    if (!rc.canSenseLocation(toDig) && highestDir == null) {
                        highestDir = dir;
                    } else {
                        int height = rc.senseElevation(toDig);
                        if (height > highest) {
                            highestDir = dir;
                            highest = height;
                        }
                    }
                }
                if (highestDir != null) {
                    this.util.log(String.format("Digging: %d %d!", rc.getLocation().add(highestDir).x, rc.getLocation().add(highestDir).y));

                    rc.digDirt(highestDir);
                }
            } else {
                Direction lowestDir = null;
                int lowest = 1 << 30;
                for (Direction dir : Direction.allDirections()) {
                    MapLocation toDig = rc.getLocation().add(dir);
                    if (rc.getRoundNum() < startRound + 100 && toDig.isAdjacentTo(this.designSchoolLoc)) {
                        continue;
                    }
                    if (toDig.equals(this.designSchoolLoc)) {
                        continue;
                    }
                    if (rc.getRoundNum() < 600 && this.util.distanceLinf(assignment, this.gameState.hqLocation) == 2 || true) {
                        if (RobotUtil.distanceLinf(toDig, this.gameState.hqLocation) != 2) {
                            continue;
                        }
                    } else {
                        if (RobotUtil.distanceLinf(toDig, this.gameState.hqLocation) != 1) {
                            continue;
                        }
                    }
                    if (!rc.canSenseLocation(toDig) && lowestDir == null) {
                        lowestDir = dir;
                    } else {
                        int height = rc.senseElevation(toDig);
                        if (height < lowest) {
                            lowestDir = dir;
                            lowest = height;
                        }
                    }
                }
                if (lowestDir != null) {
                    this.util.log(String.format("Depositing: %d %d!", rc.getLocation().add(lowestDir).x, rc.getLocation().add(lowestDir).y));
                    rc.depositDirt(lowestDir);
                }
            }
            util.yield();
        }

    }

    public void maintain(MapLocation center) throws GameActionException {
        while (true) {
            this.util.waitCooldown();

            center = util.closestCenter(rc.getLocation());

            if (rc.getDirtCarrying() < 10) {
                Direction highestDir = null;
                int highest = -1 << 30;
                for (Direction dir : Direction.allDirections()) {
                    MapLocation toDig = rc.getLocation().add(dir);
                    if (rc.getRoundNum() < this.util.birthRound + 100 && toDig.isAdjacentTo(this.designSchoolLoc)) {
                        continue;
                    }
                    if (!rc.canDigDirt(dir)) {
                        continue;
                    }
                    if (this.rc.canSenseLocation(toDig)) {
                        RobotInfo robot = rc.senseRobotAtLocation(toDig);
                        if (robot != null && robot.type.isBuilding() && robot.team == rc.getTeam() &&
                                this.rc.canDigDirt(dir)) {
                            highest = 1 << 30;
                            highestDir = rc.getLocation().directionTo(center);
                            break;
                        }
                    }
                    if (this.util.distanceLinf(rc.getLocation(), center) == 2) {
                        if (RobotUtil.distanceLinf(toDig, center) != 3) {
                            continue;
                        }
                    } else {
                        if (true || RobotUtil.distanceLinf(toDig, center) != 1 || (this.rc.canSenseLocation(toDig) && this.rc.senseRobotAtLocation(toDig) == null && rc.getRoundNum() < 600)) {
                            continue;
                        }

                    }
                    if (!rc.canSenseLocation(toDig) && highestDir == null) {
                        highestDir = dir;
                    } else {
                        int height = rc.senseElevation(toDig);
                        if (height > highest) {
                            highestDir = dir;
                            highest = height;
                        }
                    }
                }
                if (highestDir != null) {
                    this.util.log(String.format("Digging: %d %d!", rc.getLocation().add(highestDir).x, rc.getLocation().add(highestDir).y));

                    rc.digDirt(highestDir);
                }
            } else {
                Direction lowestDir = null;
                int lowest = 1 << 30;
                for (Direction dir : Direction.allDirections()) {
                    MapLocation toDig = rc.getLocation().add(dir);
                    if (toDig.equals(this.designSchoolLoc)) {
                        continue;
                    }
                    if (RobotUtil.distanceLinf(toDig, center) != 2) {
                        continue;
                    }
                    if (!rc.canSenseLocation(toDig)) {
                        if (lowestDir == null) {
                            lowestDir = dir;
                        }
                    } else {
                        int height = rc.senseElevation(toDig);
                        if (height < lowest) {
                            lowestDir = dir;
                            lowest = height;
                        }
                    }
                }
                if (lowestDir != null) {
                    this.util.log(String.format("Depositing: %d %d!", rc.getLocation().add(lowestDir).x, rc.getLocation().add(lowestDir).y));
                    rc.depositDirt(lowestDir);
                }
            }
            util.yield();
        }
    }

    public void runCell(MapLocation center) throws GameActionException {
        while (true) {
            MapLocation[] assignments = new MapLocation[8];

            for (int i = 0; i < 8; i++) {
                Direction baseDirection = Direction.cardinalDirections()[i % 4];
                Direction auxDirection = baseDirection.rotateLeft().rotateLeft();
                if (i >= 4) {
                    auxDirection = auxDirection.opposite();
                }

                assignments[i] = center.add(baseDirection).add(baseDirection).add(auxDirection);
            }

            MapLocation assignment = assignments[(int) (Math.random() * 8)];

            util.log(String.format("Trying assignment: %d %d", assignment.x, assignment.y));


            boolean bad = false;
            while (true) {
                this.util.waitCooldown();

                if (rc.canSenseLocation(assignment)) {
                    RobotInfo robot = rc.senseRobotAtLocation(assignment);
                    if (robot != null && robot.type == RobotType.LANDSCAPER && robot.team == rc.getTeam()) {
                        bad = true;
                        break;
                    }
                }

                if (util.isGoodPosition(rc.getLocation())) {
                    break;
                }
                if (rc.getRoundNum() >= 600) {
                    break;
                }

                util.moveTowards(assignment, null, true);
                rc.setIndicatorLine(rc.getLocation(), assignment, 255, 0, 0);
                if (rc.getLocation().equals(assignment)) {
                    break;
                }
                util.yield();
            }
            if (!bad) {
                break;
            }
        }

        util.log("Arrived at assignment");

        while (rc.getRoundNum() < 600) {
            util.yield();
        }
        maintain(center);



        /*
        Direction centerDirection = center.directionTo(rc.getLocation());
        {
            while (true) {
                util.waitCooldown();

                if (rc.canMove(RobotUtil.clockwise(centerDirection))) {
                    rc.move(RobotUtil.clockwise(centerDirection));
                    util.yield();
                    break;
                }

                if (rc.canMove(RobotUtil.counterclockwise(centerDirection))) {
                    rc.move(RobotUtil.counterclockwise(centerDirection));
                    util.yield();
                    break;
                }

            }
        }
        for (int i = 0; i < 10; i++) {
            util.yield();
        }

        maintain(center);*/
    }

}
