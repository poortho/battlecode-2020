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
        this.gameState = new GameState();
    }

    public void run() throws GameActionException {
        int startRound = rc.getRoundNum();


        for (RobotInfo info : this.util.seeRobots()) {
            // We are next to the HQ, look for it.
            if (this.gameState.hqLocation == null && info.type == RobotType.HQ && info.team == rc.getTeam()) {
                this.gameState.hqLocation = info.location;
            }
            // We are next to the Design School, look for it.
            if (this.designSchoolLoc == null && info.type == RobotType.DESIGN_SCHOOL && info.team == rc.getTeam()) {
                this.designSchoolLoc = info.location;
            }
        }

        MapLocation assignment = null;
        MapLocation tryAssignment = null;

        /*
        // look for assignment
        while (rc.getRoundNum() < startRound + 100) {
            this.util.waitCooldown();

            if (RobotUtil.distanceLinf(this.gameState.hqLocation, rc.getLocation()) <= 2 && (rc.getRoundNum() > startRound + 30 || !rc.getLocation().isAdjacentTo(designSchoolLoc))) {
                assignment = this.gameState.hqLocation;
                break;
            }

            boolean[][] occupancy = new boolean[5][5];
            for (RobotInfo info : this.util.seeRobots()) {
                int dx = info.location.x - this.gameState.hqLocation.x;
                int dy = info.location.y - this.gameState.hqLocation.y;

                if (-2 <= dx && dx <= 2 && -2 <= dy && dy <= 2) {
                    occupancy[dx + 2][dy + 2] = true;
                }
            }

            if (tryAssignment != null && (occupancy[tryAssignment.x - this.gameState.hqLocation.x + 2][tryAssignment.y - this.gameState.hqLocation.y + 2] || (rc.canSenseLocation(tryAssignment) && rc.senseFlooding(tryAssignment)))) {
                tryAssignment = null;
            }

            if (tryAssignment == null) {
                MapLocation bestAssignment = null;
                int bestAssignmentScore = -1;
                for (int i = -2; i < 3; i++) {
                    for (int j = -2; j < 3; j++) {
                        MapLocation potentialAssignment = new MapLocation(this.gameState.hqLocation.x + i, this.gameState.hqLocation.y + j);
                        if (RobotUtil.distanceLinf(this.gameState.hqLocation, potentialAssignment) > 2)
                            continue;

                        int dx = potentialAssignment.x - this.gameState.hqLocation.x;
                        int dy = potentialAssignment.y - this.gameState.hqLocation.y;

                        int assignmentScore;
                        if (rc.canSenseLocation(potentialAssignment)) {
                            if (!occupancy[dx + 2][dy + 2] && !rc.senseFlooding(potentialAssignment)) {
                                assignmentScore = 2;
                            } else {
                                assignmentScore = -10;
                            }
                        } else {
                            assignmentScore = 1;
                        }

                        if (rc.getRoundNum() < startRound + 50) {
                            if (!potentialAssignment.isAdjacentTo(this.designSchoolLoc)) {
                                assignmentScore += 1;
                            }
                        } else {
                            if (potentialAssignment.isAdjacentTo(this.designSchoolLoc)) {
                                assignmentScore += 1;
                            }
                        }
                        //assignmentScore += RobotUtil.distanceLinf(this.gameState.hqLocation, potentialAssignment) == 1 ? 1 : 0;

                        //RobotUtil.distanceLinf(rc.getLocation(), potentialAssignment) < RobotUtil.distanceLinf(rc.getLocation(), bestAssignment))
                        if (assignmentScore > bestAssignmentScore || (assignmentScore == bestAssignmentScore && Math.random() > 0.4)) {
                            bestAssignment = potentialAssignment;
                            bestAssignmentScore = assignmentScore;
                        }
                    }
                }

                tryAssignment = bestAssignment;
                if (tryAssignment != null) {
                    this.util.log(String.format("Try assignment: %d %d", tryAssignment.x, tryAssignment.y));
                } else {
                    this.util.log("No assignments to try F");
                }
            }

            if (tryAssignment != null) {
                this.util.moveTowards(tryAssignment, null);
            }
            Clock.yield();
        }
         */

        Direction designSchoolOrientation = this.designSchoolLoc.directionTo(this.gameState.hqLocation);
        MapLocation theCoolSpot = this.gameState.hqLocation.add(designSchoolOrientation).add(designSchoolOrientation);
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
            for (int i = -1; i < 2; i++) {
                for (int j = -1; j < 2; j++) {
                    MapLocation potentialAssignment = new MapLocation(this.rc.getLocation().x + i, this.rc.getLocation().y + j);
                    if (RobotUtil.distanceLinf(this.gameState.hqLocation, potentialAssignment) > 2)
                        continue;

                    int dx = potentialAssignment.x - this.gameState.hqLocation.x;
                    int dy = potentialAssignment.y - this.gameState.hqLocation.y;

                    int assignmentScore = theCoolSpot.distanceSquaredTo(potentialAssignment);
                    assignmentScore -= this.designSchoolLoc.distanceSquaredTo(potentialAssignment);

                    if (rc.canSenseLocation(potentialAssignment)) {
                        if (occupancy[dx + 2][dy + 2] || rc.senseFlooding(potentialAssignment)) {
                            assignmentScore += 1000;
                        }
                    }

                    if (RobotUtil.distanceLinf(this.gameState.hqLocation, potentialAssignment) == 1) {
                        assignmentScore += 2;
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
                this.util.moveTowardsBounded(tryAssignment, null, false, this.gameState.hqLocation, 2);
            }
            Clock.yield();
        }

        assignment = this.rc.getLocation();


        this.util.log("Arrived at assignment!");

        for (int i = 0; i < (RobotUtil.distanceLinf(this.gameState.hqLocation, assignment) == 2 ? 10 : 10); i++) {
            Clock.yield();
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
                    if (toDig.equals(this.designSchoolLoc)) {
                        continue;
                    }
                    if (this.util.distanceLinf(assignment, this.gameState.hqLocation) == 2) {
                        if (RobotUtil.distanceLinf(toDig, this.gameState.hqLocation) != 3) {
                            continue;
                        }
                    } else {
                        if (RobotUtil.distanceLinf(toDig, this.gameState.hqLocation) != 1) {
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
            Clock.yield();
        }

    }

}
