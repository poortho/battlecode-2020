package copy_super_cow;

import battlecode.common.*;

import static copy_super_cow.Helper.*;
import static copy_super_cow.RobotPlayer.rc;
import static copy_super_cow.RobotPlayer.turnCount;

public class Landscaper {
    static MapLocation cur_loc;
    static int lattice_elevation = 5;
    static MapLocation previous_location;
    static boolean bugpath_blocked;
    static boolean[] blacklist = new boolean[directions.length];
    static boolean in_danger;
    static RobotInfo[] robots;
    static MapLocation destination;
    static MapLocation[] corners = new MapLocation[4];
    static int corner_i = 0;
    static int counter = 0;
    static boolean defensive = false;
    static MapLocation my_hq;
    static int nearby_landscapers_not_adjacent_hq = 0;
    static int move_counter = 0;
    static MapLocation my_design = null;
    static MapLocation[] explore_locs = null;

    static void runLandscaper() throws GameActionException {
        lattice_elevation = Math.max(Helper.getLevel(rc.getRoundNum()), 5);
        move_counter++;
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

        if (explore_locs == null && HQ.our_hq != null) {
            explore_locs = new MapLocation[6];
            int width = rc.getMapWidth();
            int height = rc.getMapHeight();
            MapLocation middle = new MapLocation(width / 2, height / 2);

            // set locations
            int delta_x = middle.x + (middle.x - HQ.our_hq.x);
            int delta_y = middle.y + (middle.y - HQ.our_hq.y);
            explore_locs[0] = new MapLocation(middle.x, middle.y);
            explore_locs[1] = new MapLocation(delta_x, delta_y);
            explore_locs[2] = new MapLocation(delta_x, HQ.our_hq.y);
            explore_locs[3] = new MapLocation(HQ.our_hq.x, delta_y);
            explore_locs[4] = new MapLocation(delta_x, middle.y);
            explore_locs[5] = new MapLocation(middle.x, delta_y);
        }

        do_lattice();
        /*
        if (defensive) {
            do_defense();
        } else {
            do_offense();
        }
         */
    }

    static void lattice_walk(MapLocation loc) throws GameActionException {
        // bugpath walk but dont fall into lattice holes
        if (!rc.isReady()) {
            return;
        }
        Direction greedy;

        int least_dist = cur_loc.distanceSquaredTo(loc);
        int next = -1;
        int greedy_dist = 9999999;
        int greedy_idx = -1;
        for (int i = 0; i < directions.length; i++) {
            MapLocation next_loc = cur_loc.add(directions[i]);
            int temp_dist = next_loc.distanceSquaredTo(loc);
            if (rc.canMove(directions[i])) {
                if (temp_dist < least_dist && !rc.senseFlooding(next_loc) && !blacklist[i]) {
                    least_dist = temp_dist;
                    next = i;
                }
            }
            if (temp_dist < greedy_dist) {
                greedy_dist = temp_dist;
                greedy_idx = i;
            }
        }

        if (!bugpath_blocked && next != -1 && isLattice(cur_loc.add(directions[next]))) {
            rc.move(directions[next]);
        } else {
            if (bugpath_blocked) {
                Direction start_dir = cur_loc.directionTo(previous_location);
                for (int i = 0; i < Helper.directions.length; i++) {
                    if (Helper.directions[i] == start_dir) {
                        next = i;
                        break;
                    }
                }
            }
            bugpath_blocked = true;
            if (next == -1) {
                next = greedy_idx;
            }
            for (int i = 0; i < 7; i++) {
                next = (next + 1) % directions.length;
                Direction cw = directions[next];
                MapLocation next_loc = cur_loc.add(cw);
                if (rc.canMove(cw) && !rc.senseFlooding(next_loc) && !blacklist[next] && !Helper.willFlood(next_loc) &&
                    isLattice(cur_loc.add(directions[next]))) {
                    if (next_loc.distanceSquaredTo(loc) < cur_loc.distanceSquaredTo(loc)) {
                        bugpath_blocked = false;
                    }
                    rc.move(cw);
                    previous_location = cur_loc;
                    break;
                }
            }
        }
    }

    static void do_lattice() throws GameActionException {
        if (cur_loc.distanceSquaredTo(HQ.our_hq) <= 8) {
            // greedy move away
            // this shouldnt be an issue later, cuz we will add turtle
            greedy_move_away(HQ.our_hq, cur_loc);
            return;
        }
        // if any tile not up to spec, find it
        for (int i = 0; i < distx_35.length; i++) {
            MapLocation new_loc = new MapLocation(cur_loc.x + distx_35[i], cur_loc.y + disty_35[i]);
            if (rc.canSenseLocation(new_loc)) {
                if (new_loc.distanceSquaredTo(HQ.our_hq) > 8 &&
                        rc.senseElevation(new_loc) < lattice_elevation &&
                        isLattice(new_loc)) {
                    RobotInfo r = rc.senseRobotAtLocation(new_loc);
                    if (r == null || !r.type.isBuilding()) {
                        if (cur_loc.distanceSquaredTo(new_loc) <= 3) {
                            // adjacent
                            if (rc.getDirtCarrying() > 0) {
                                // deposit
                                if (rc.canDepositDirt(cur_loc.directionTo(new_loc))) {
                                    rc.depositDirt(cur_loc.directionTo(new_loc));
                                }
                            } else {
                                // dig
                                Helper.digLattice(cur_loc);
                            }
                        } else {
                            // not adjacent, walk to it pepega
                            lattice_walk(new_loc);
                        }
                        return;
                    }
                }
            } else {
                break;
            }
        }
        // move in random direction i guess?
        lattice_walk(explore_locs[rc.getID() % explore_locs.length]);
    }

    static void do_defense() throws GameActionException {
        int dist_from_hq = cur_loc.distanceSquaredTo(my_hq);
        if (dist_from_hq <= 3) {
            // first things first, heal HQ
            if (rc.canDigDirt(cur_loc.directionTo(my_hq))) {
                rc.digDirt(cur_loc.directionTo(my_hq));
                return;
            }

            // check if any flood near me
            boolean near_flood = false;
            for (int i = 0; i < directions.length; i++) {
                MapLocation new_loc = cur_loc.add(directions[i]);
                if (rc.canSenseLocation(new_loc)) {
                    RobotInfo r = rc.senseRobotAtLocation(new_loc);
                    if (new_loc.distanceSquaredTo(my_hq) <= 3 && r != null && r.team != rc.getTeam() && r.type.isBuilding()) {
                        do_offense();
                    }
                    if (rc.senseFlooding(new_loc)) {
                        near_flood = true;
                    }
                }
            }

            // check if there exists adjacent spot with lower elevation
            // if so fill it in
            // unless im empty, in which case move do it...
            for (int i = 0; i < directions.length; i++) {
                MapLocation new_loc = cur_loc.add(directions[i]);
                int num_nearby_nonadjacent = 0;
                for (int j = 0; j < directions.length; j++) {
                    // check if landscaper nonadjacent to HQ is adjacent to new loc
                    if (rc.canSenseLocation(new_loc.add(directions[j]))) {
                        RobotInfo robot = rc.senseRobotAtLocation(new_loc.add(directions[j]));
                        if (robot != null && robot.location.distanceSquaredTo(my_hq) > 3 &&
                                robot.type == RobotType.LANDSCAPER &&
                                robot.team == rc.getTeam()) {
                            num_nearby_nonadjacent++;
                        }
                    }
                }
                if (near_flood) {
                    RobotInfo r = rc.senseRobotAtLocation(new_loc);
                    if (num_nearby_nonadjacent == 0 &&
                            rc.canMove(directions[i]) && new_loc.distanceSquaredTo(my_hq) <= 3 &&
                            rc.senseElevation(new_loc) < rc.senseElevation(cur_loc) && move_counter % 20 == 0) {
                        rc.move(directions[i]);
                        return;
                    } else if ((num_nearby_nonadjacent == 0 || rc.senseFlooding(new_loc))
                            && rc.senseElevation(new_loc) < rc.senseElevation(cur_loc) && rc.getDirtCarrying() > 0
                            && rc.canDepositDirt(directions[i]) && new_loc.distanceSquaredTo(my_hq) <= 3
                            && new_loc.distanceSquaredTo(my_hq) > 0 && (r == null || r.type == RobotType.LANDSCAPER || rc.senseFlooding(new_loc))) {
                        rc.depositDirt(directions[i]);
                        return;
                    }
                }
                if (rc.senseElevation(new_loc) < rc.senseElevation(my_hq) + 2 && rc.canDepositDirt(directions[i]) &&
                        new_loc.distanceSquaredTo(my_hq) <= 3 && new_loc.distanceSquaredTo(my_hq) > 0) {
                    rc.depositDirt(directions[i]);
                    return;
                }

                // if further away place from design school is movable, then sice
                if (my_design != null && new_loc.distanceSquaredTo(my_hq) <= 3 &&
                        new_loc.distanceSquaredTo(my_design) > cur_loc.distanceSquaredTo(my_design) &&
                        rc.canMove(directions[i])) {
                    rc.move(directions[i]);
                    return;
                }
            }

            // directly adjacent, dig from away and put under
            dig_and_deposit(my_hq, Direction.CENTER);
        } else if (destination != null && rc.canSenseLocation(destination)) {
            do_offense();
        } else if (dist_from_hq <= 8) {
            // first, check if there is an open spot adjacent to hq...
            // find closest...
            int min_dist = 99999;
            MapLocation best_loc = null;
            int best_dep_el = 9999999;
            Direction best_dep_dir = null;
            for (int i = 0; i < directions.length; i++) {
                MapLocation new_loc = my_hq.add(directions[i]);
                MapLocation dep_loc = cur_loc.add(directions[i]);
                if (rc.canSenseLocation(new_loc)) {
                    RobotInfo r =  rc.senseRobotAtLocation(new_loc);
                    RobotInfo r2 = rc.senseRobotAtLocation(dep_loc);
                    if (r == null) {
                        if (new_loc.distanceSquaredTo(cur_loc) < min_dist) {
                            min_dist = new_loc.distanceSquaredTo(cur_loc);
                            best_loc = new_loc;
                        }
                    }
                    if (dep_loc.distanceSquaredTo(my_hq) <= 3 && (r2 == null || r2.type == RobotType.LANDSCAPER) &&
                        best_dep_el > rc.senseElevation(dep_loc)) {
                        best_dep_dir = directions[i];
                        best_dep_el = rc.senseElevation(dep_loc);
                    }
                }
            }
            //System.out.println(best_loc);
            //System.out.println(best_dep_dir);
            if (best_loc != null) {
                if (cur_loc.distanceSquaredTo(best_loc) <= 3) {
                    aggressive_landscaper_walk(best_loc);
                } else {
                    bugpath_walk(best_loc);
                }
            } else {
                // adjacent to 8 tile ring, dig from under and put closer
                dig_and_deposit(my_hq, best_dep_dir);
            }

        } else {
            // move closer to hq
            bugpath_walk(my_hq);
        }
    }

    static void aggressive_landscaper_walk(MapLocation loc) throws GameActionException {
        int least_dist = 9999999;
        int next = -1;
        for (int i = 0; i < directions.length; i++) {
            MapLocation next_loc = cur_loc.add(directions[i]);
            if (rc.canSenseLocation(next_loc)) {
                int temp_dist = next_loc.distanceSquaredTo(loc);
                RobotInfo r = rc.senseRobotAtLocation(next_loc);
                if (temp_dist < least_dist && r == null) {
                    least_dist = temp_dist;
                    next = i;
                }
            }
        }

        if (next == -1) {
            bugpath_walk(loc);
            return;
        }

        Direction greedy = directions[next];
        MapLocation new_loc = cur_loc.add(greedy);

        if (rc.isReady()) {
            if (rc.canMove(greedy) && !rc.senseFlooding(new_loc)) {
                rc.move(greedy);
            } else {
                if (rc.senseFlooding(new_loc)) {
                    // fill flood
                    dig_and_deposit(new_loc, cur_loc.directionTo(new_loc));
                } else if (rc.senseElevation(new_loc) > rc.senseElevation(cur_loc)) {
                    // deposit at self lol
                    dig_and_deposit(new_loc, Direction.CENTER);
                } else {
                    // deposit at dest
                    dig_and_deposit(new_loc, greedy);
                }
            }
        }
    }

    static void do_offense() throws GameActionException {
        if (HQ.enemy_hq != null && (destination == null || cur_loc.distanceSquaredTo(HQ.enemy_hq) < cur_loc.distanceSquaredTo(destination))) {
            destination = HQ.enemy_hq;
        }

        // adjacent, cuck em asap
        if (destination != null && cur_loc.distanceSquaredTo(destination) <= 3) {
            if (rc.getDirtCarrying() > 0) {
                // cuck em
                //System.out.println("tryna dump");
                //System.out.println(rc.getDirtCarrying());
                //System.out.println(rc.isReady());
                if (rc.canDepositDirt(cur_loc.directionTo(destination))) {
                    //System.out.println("dumped");
                    RobotInfo r = rc.senseRobotAtLocation(destination);
                    rc.depositDirt(cur_loc.directionTo(destination));
                    if (r != null && !rc.canSenseRobot(r.ID) && r.type == RobotType.NET_GUN) {
                        // killed a netgun :O
                        Comms.broadcast_enemy_netgun(destination, 0);
                    }
                }
            } else {
                // succ
                //System.out.println("Tryna dig");
                //Helper.tryDig();
                Helper.tryDigAway(destination);
            }
        } else if (rc.getDirtCarrying() < RobotType.LANDSCAPER.dirtLimit) {
            // not full and not adjacent, fill up on dirt
            if (counter % 2 == 1) {
                Helper.tryDig();
            }
            counter++;
        }
        if (destination != null && rc.canSenseLocation(destination)) {
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
        nearby_landscapers_not_adjacent_hq = 0;
        for (int i = 0; i < robots.length; i++) {
            if (robots[i].team == rc.getTeam()) {
                switch(robots[i].type) {
                    case HQ:
                        // defensive
                        defensive = true;
                        my_hq = robots[i].location;
                        break;
                    case LANDSCAPER:
                        if (defensive && robots[i].location.distanceSquaredTo(my_hq) > 3) {
                            nearby_landscapers_not_adjacent_hq++;
                        }
                        break;
                    case DESIGN_SCHOOL:
                        if (turnCount == 1) {
                            my_design = robots[i].location;
                        }
                        break;
                }
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
                        if (HQ.enemy_hq == null) {
                            Comms.broadcast_enemy_hq(robots[i].location);
                        }
                    case NET_GUN:
                    case REFINERY:
                    case VAPORATOR:
                    case DESIGN_SCHOOL:
                    case FULFILLMENT_CENTER:
                        // enemy building, go fk it
                        if (destination == null || (robots[i].type != RobotType.REFINERY &&
                                 robots[i].location.distanceSquaredTo(cur_loc) < destination.distanceSquaredTo(cur_loc))) {
                            destination = robots[i].location;
                        }
                        break;
                }
            }
        }
    }

    static void bugpath_walk(MapLocation loc) throws GameActionException {
        if (!rc.isReady()) {
            return;
        }
        Direction greedy;

        int least_dist = cur_loc.distanceSquaredTo(loc);
        int next = -1;
        int greedy_dist = 9999999;
        int greedy_idx = -1;
        for (int i = 0; i < directions.length; i++) {
            MapLocation next_loc = cur_loc.add(directions[i]);
            int temp_dist = next_loc.distanceSquaredTo(loc);
            if (rc.canMove(directions[i])) {
                if (temp_dist < least_dist && !rc.senseFlooding(next_loc) && !blacklist[i]) {
                    least_dist = temp_dist;
                    next = i;
                }
            }
            if (temp_dist < greedy_dist) {
                greedy_dist = temp_dist;
                greedy_idx = i;
            }
        }

        if (!bugpath_blocked && next != -1) {
            rc.move(directions[next]);
        } else {
            if (bugpath_blocked) {
                Direction start_dir = cur_loc.directionTo(previous_location);
                for (int i = 0; i < Helper.directions.length; i++) {
                    if (Helper.directions[i] == start_dir) {
                        next = i;
                        break;
                    }
                }
            }
            bugpath_blocked = true;
            if (next == -1) {
                next = greedy_idx;
            }
            for (int i = 0; i < 7; i++) {
                next = (next + 1) % directions.length;
                Direction cw = directions[next];
                MapLocation next_loc = cur_loc.add(cw);
                if (rc.canMove(cw) && !rc.senseFlooding(next_loc) && !blacklist[next] && !Helper.willFlood(next_loc)) {
                    if (next_loc.distanceSquaredTo(loc) < cur_loc.distanceSquaredTo(loc)) {
                        bugpath_blocked = false;
                    }
                    rc.move(cw);
                    previous_location = cur_loc;
                    break;
                }
            }   
        }
    }

    static void dig_and_deposit(MapLocation loc, Direction dir) throws GameActionException {
        if (rc.getDirtCarrying() > 0 && dir != null) {
            if (rc.canDepositDirt(dir)) {
                rc.depositDirt(dir);
            }
        } else {
            if (loc.equals(my_hq)) {
                // dig edges
                Helper.tryDigEdges();
            } else {
                Helper.tryDigAway(loc);
            }
        }
    }
}
