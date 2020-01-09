package bot2;

import battlecode.common.*;

import static bot2.Helper.directions;
import static bot2.Helper.tryDigAway;
import static bot2.RobotPlayer.rc;
import static bot2.RobotPlayer.turnCount;

public class Landscaper {
    static MapLocation cur_loc;
    static MapLocation previous_location;
    static boolean[] blacklist = new boolean[directions.length];
    static boolean in_danger;
    static RobotInfo[] robots;
    static MapLocation destination;
    static MapLocation[] corners = new MapLocation[4];
    static int corner_i = 0;
    static int counter = 0;
    static boolean defensive = false;
    static MapLocation my_hq;

    static void runLandscaper() throws GameActionException {
        cur_loc = rc.getLocation();
        robots = rc.senseNearbyRobots();
        in_danger = false;
        for (int i = 0; i < blacklist.length; i++) {
            blacklist[i] = false;
        }

        if (turnCount == 1) {
            corners[0] = new MapLocation(0, 0);
            corners[1] = new MapLocation(rc.getMapWidth(), 0);
            corners[2] = new MapLocation(0, rc.getMapHeight());
            corners[3] = new MapLocation(rc.getMapWidth(), rc.getMapHeight());
        }

        Comms.getBlocks();

        // if near destination and its empty, set destination to null
        if (destination != null && rc.canSenseLocation(destination)) {
            RobotInfo robot = rc.senseRobotAtLocation(destination);
            // canbepicked up => unit
            if (robot == null || !robot.type.isBuilding() || robot.team == rc.getTeam()) {
                //System.out.println("killed!");
                destination = null;
            }
        }

        sense();

        if (in_danger) {
            // move in a direction such that you are not in danger
            // TODO change so that it moves towards destination
            for (int i = 0; i < directions.length; i++) {
                if (!blacklist[i] && rc.canMove(directions[i]) && !rc.senseFlooding(cur_loc.add(directions[i]))) {
                    Helper.tryMove(directions[i]);
                }
            }
        }

        if (defensive) {
            do_defense();
        } else {
            do_offense();
        }
    }

    static void do_defense() throws GameActionException {
        int dist_from_hq = cur_loc.distanceSquaredTo(my_hq);
        if (dist_from_hq <= 3) {
            // check if there exists adjacent spot with lower elevation
            // if so fill it in
            // unless im empty, in which case move do it...
            for (int i = 0; i < directions.length; i++) {
                if (rc.canMove(directions[i]) && cur_loc.add(directions[i]).distanceSquaredTo(my_hq) <= 3 &&
                        rc.senseElevation(cur_loc.add(directions[i])) < rc.senseElevation(cur_loc)) {
                    rc.move(directions[i]);
                    return;
                } else if (rc.senseElevation(cur_loc.add(directions[i])) < rc.senseElevation(cur_loc) && rc.getDirtCarrying() > 0
                        && rc.canDepositDirt(directions[i]) && cur_loc.add(directions[i]).distanceSquaredTo(my_hq) <= 3
                        && cur_loc.add(directions[i]).distanceSquaredTo(my_hq) > 0) {
                    rc.depositDirt(directions[i]);
                }
            }

            // directly adjacent, dig from away and put under
            if (rc.getDirtCarrying() > 0 && rc.canDepositDirt(Direction.CENTER)) {
                // have dirt, deposit under
                rc.depositDirt(Direction.CENTER);
            } else {
                // find direction furthest away and dig
                tryDigAway(my_hq);
            }
        } else if (dist_from_hq <= 8) {
            // first, check if there is an open spot adjacent to hq...
            for (int i = 0; i < directions.length; i++) {
                MapLocation new_loc = cur_loc.add(directions[i]);
                if (new_loc.distanceSquaredTo(my_hq) <= 3 && rc.senseRobotAtLocation(new_loc) == null) {
                    aggressive_landscaper_walk(new_loc);
                    return;
                }
            }

            // adjacent to 8 tile ring, dig from under and put closer
            if (rc.getDirtCarrying() > 0) {
                // have dirt, deposit close as possible
                Helper.tryDepositClose(my_hq);
            } else {
                // find direction furthest away and dig
                Helper.tryDigAway(my_hq);
            }
        } else {
            // move closer to hq
            aggressive_landscaper_walk(my_hq);
        }
    }

    static void aggressive_landscaper_walk(MapLocation loc) throws GameActionException {
        int least_dist = 9999999;
        int next = -1;
        for (int i = 0; i < directions.length; i++) {
            MapLocation next_loc = cur_loc.add(directions[i]);
            int temp_dist = next_loc.distanceSquaredTo(loc);
            RobotInfo r = rc.senseRobotAtLocation(next_loc);
            if (temp_dist < least_dist && !next_loc.equals(previous_location) && (r == null || (r.team != rc.getTeam() ||
                    !r.type.isBuilding()))) {
                least_dist = temp_dist;
                next = i;
            }
        }

        Direction greedy = directions[next];
        MapLocation new_loc = cur_loc.add(greedy);

        if (rc.isReady()) {
            if (rc.canMove(greedy)) {
                rc.move(greedy);
            } else {
                if (rc.senseElevation(new_loc) > rc.senseElevation(cur_loc)) {
                    // deposit at self lol
                    if (rc.getDirtCarrying() > 0) {
                        if (rc.canDepositDirt(Direction.CENTER)) {
                            rc.depositDirt(Direction.CENTER);
                        }
                    } else {
                        Helper.tryDigAway(new_loc);
                    }
                } else {
                    // deposit at dest
                    if (rc.getDirtCarrying() > 0) {
                        if (rc.canDepositDirt(greedy)) {
                            rc.depositDirt(greedy);
                        }
                    } else {
                        Helper.tryDigAway(new_loc);
                    }
                }
            }
        }
    }

    static void do_offense() throws GameActionException {
        if (destination == null && HQ.enemy_hq != null) {
            destination = HQ.enemy_hq;
        }

        // adjacent, cuck em asap
        if (destination != null && cur_loc.distanceSquaredTo(destination) <= 2) {
            if (rc.getDirtCarrying() > 0) {
                // cuck em
                //System.out.println("tryna dump");
                //System.out.println(rc.getDirtCarrying());
                //System.out.println(rc.isReady());
                if (rc.canDepositDirt(cur_loc.directionTo(destination))) {
                    //System.out.println("dumped");
                    rc.depositDirt(cur_loc.directionTo(destination));
                }
            } else {
                // succ
                //System.out.println("Tryna dig");
                Helper.tryDig();
            }
        } else if (rc.getDirtCarrying() < RobotType.LANDSCAPER.dirtLimit) {
            // not full and not adjacent, fill up on dirt
            if (counter % 2 == 1) {
                Helper.tryDig();
            }
            counter++;
        }

        if (rc.canSenseLocation(destination)) {
            aggressive_landscaper_walk(destination);
        } else {
            walk_to_dest();
        }
    }

    static void walk_to_dest() throws GameActionException {
        // walk if possible
        if (destination != null) {
            //System.out.println("Destination: " + destination.toString());
            // move to destination
            bugpath_walk(destination);
        } else {
            // no known destination & full, wander map
            //System.out.println("no destination, walking to corner " + corners[corner_i % corners.length].toString());
            if (cur_loc.distanceSquaredTo(corners[corner_i % corners.length]) < RobotType.LANDSCAPER.sensorRadiusSquared) {
                // visited, inc
                corner_i++;
            }
            bugpath_walk(corners[corner_i % corners.length]);
        }
    }

    static void sense() throws GameActionException {
        for (int i = 0; i < robots.length; i++) {
            if (HQ.enemy_hq == null && robots[i].type == RobotType.HQ && robots[i].team != rc.getTeam()) {
                // found enemy hq broadcast it
                System.out.println("Found enemy hq! " + robots[i].location);
                Comms.broadcast_enemy_hq(robots[i].location);
            }
            if (!defensive && robots[i].type == RobotType.HQ && robots[i].team == rc.getTeam()) {
                // defensive
                defensive = true;
                my_hq = robots[i].location;
            }
            if (robots[i].team != rc.getTeam()) {
                switch (robots[i].type) {
                    case DELIVERY_DRONE:
                        if (cur_loc.distanceSquaredTo(robots[i].getLocation()) <= GameConstants.DELIVERY_DRONE_PICKUP_RADIUS_SQUARED) {
                            in_danger = true;
                        }
                        for (int j = 0; j < directions.length; j++) {
                            if (cur_loc.add(directions[j]).distanceSquaredTo(robots[i].getLocation())
                                    <= GameConstants.DELIVERY_DRONE_PICKUP_RADIUS_SQUARED) {
                                blacklist[j] = true;
                            }
                        }
                        break;
                    case HQ:
                    case NET_GUN:
                    case REFINERY:
                    case VAPORATOR:
                    case DESIGN_SCHOOL:
                    case FULFILLMENT_CENTER:
                        // enemy building, go fk it
                        if (destination == null ||
                                robots[i].location.distanceSquaredTo(cur_loc) < destination.distanceSquaredTo(cur_loc)) {
                            destination = robots[i].location;
                        }
                        break;
                }
            }
        }
    }

    static void bugpath_walk(MapLocation loc) throws GameActionException {
        Direction greedy;

        int least_dist = 9999999;
        int next = -1;
        for (int i = 0; i < directions.length; i++) {
            MapLocation next_loc = cur_loc.add(directions[i]);
            int temp_dist = next_loc.distanceSquaredTo(loc);
            if (temp_dist < least_dist && !next_loc.equals(previous_location)) {
                least_dist = temp_dist;
                next = i;
            }
        }

        greedy = directions[next];
        MapLocation greedy_loc = cur_loc.add(greedy);

        if (rc.canMove(greedy) && !rc.senseFlooding(greedy_loc) && !blacklist[next]) {
            rc.move(greedy);
        } else {
            for (int i = 0; i < 7; i++) {
                next = (next + 1) % directions.length;
                Direction cw = directions[next];
                MapLocation next_loc = cur_loc.add(cw);
                if (rc.canMove(cw) && !rc.senseFlooding(next_loc) && !blacklist[next]) {
                    rc.move(cw);
                    break;
                }
            }
        }
        previous_location = cur_loc;
    }
}
