package copy_super_cow;

import battlecode.common.*;

import static copy_super_cow.Helper.*;
import static copy_super_cow.RobotPlayer.*;

public class DeliveryDrone {

    static MapLocation hq;
    static MapLocation cur_loc;
    static MapLocation[] prev_loc = new MapLocation[5];
    static int prev_loc_i = 0;
    static MapLocation nearest_flood = null;
    static int corner_i = 0;
    static MapLocation[] corners = new MapLocation[4];
    static boolean[] blacklist = new boolean[directions.length];
    static RobotInfo[] robots;
    static RobotType carried_type = null;
    static MapLocation nearest_flood_curloc;

    static MapLocation previous_location;
    static boolean bugpath_blocked = false;

    static int search_idx = 0;

    static void runDeliveryDrone() throws GameActionException {
        cur_loc = rc.getLocation();
        robots = rc.senseNearbyRobots();
        Comms.getBlocks();

        for (int i = 0; i < blacklist.length; i++) {
            blacklist[i] = false;
        }

        // compute blacklist (dont move adjacent to enemy drones, and stay out of shooting range of HQ/netgun
        for (int i = 0; i < robots.length; i++) {
            if (robots[i].team != rc.getTeam()) {
                MapLocation temp_loc = robots[i].getLocation();
                switch (robots[i].type) {
                    case HQ:
                        if (HQ.enemy_hq == null) {
                            HQ.enemy_hq = temp_loc;
                            Comms.broadcast_enemy_hq(temp_loc);
                        }
                    case NET_GUN:
                        // avoid netgun
                        if (HQ.patrol_broadcast_round == -1 || round < HQ.patrol_broadcast_round + 130) {
                            for (int j = directions.length; --j >= 0; ) {
                                blacklist[j] = blacklist[j] ||
                                        cur_loc.add(directions[j]).distanceSquaredTo(temp_loc) <= GameConstants.NET_GUN_SHOOT_RADIUS_SQUARED;
                            }
                        }
                        break;
                }
            }
        }

        if (HQ.patrol_broadcast_round == -1 || round < HQ.patrol_broadcast_round + 130) {
            // check enemy hq
            for (int i = directions.length; --i >= 0; ) {
                if (HQ.enemy_hq != null && cur_loc.add(directions[i]).distanceSquaredTo(HQ.enemy_hq) <=
                        GameConstants.NET_GUN_SHOOT_RADIUS_SQUARED) {
                    blacklist[i] = true;
                }
            }
            // now, iterate over directions within distance 25 to check for netguns lol
            int x = cur_loc.x;
            int y = cur_loc.y;
            int l = 45; // start at distance 16
            MapLocation next_loc = null;

            //System.out.println(Clock.getBytecodesLeft());
            do {
                next_loc = new MapLocation(x + distx_35[l], y + disty_35[l]);
                if (Helper.onTheMap(next_loc) && netgun_map[x + distx_35[l]][y + disty_35[l]] != 0 &&
                        netgun_map[x + distx_35[l]][y + disty_35[l]] > rc.getRoundNum() - 400) { //timeout
                    // ouo theres a netgun here, blacklist all necessary
                    for (int j = directions.length; --j >= 0; ) {
                        if (cur_loc.add(directions[j]).distanceSquaredTo(next_loc) <= GameConstants.NET_GUN_SHOOT_RADIUS_SQUARED) {
                            blacklist[j] = true;
                        }
                    }
                }
                l++;
            } while (cur_loc.distanceSquaredTo(next_loc) <= 25);
        }
        //System.out.println(Clock.getBytecodesLeft());

        if (turnCount == 1) {
            // first turn, get loc of "home"
            for (int i = 0; i < robots.length; i++) {
                if (robots[i].team == rc.getTeam() && robots[i].type == RobotType.FULFILLMENT_CENTER) {
                    hq = robots[i].getLocation();
                    break;
                }
            }

            if (hq == null) {
                if (HQ.our_hq != null) {
                    hq = HQ.our_hq;
                } else {
                    hq = cur_loc;
                }
            }

            // TODO: do this based on start?
            int width = rc.getMapWidth();
            int height = rc.getMapHeight();
            MapLocation middle = new MapLocation(width / 2, height / 2);

            int closest_x = (hq.x / middle.x) * (width - 1);
            int closest_y = (hq.y / middle.y) * (height - 1);
            corners[0] = new MapLocation(closest_x, closest_y);
            corners[1] = new MapLocation(closest_x, height - 1 - closest_y);
            corners[2] = new MapLocation(width - 1 - closest_x, closest_y);
            corners[3] = new MapLocation(width - 1 - closest_x, height - 1 - closest_y);

        }

        if (nearest_flood != null && rc.canSenseLocation(nearest_flood) && !rc.senseFlooding(nearest_flood)) {
            nearest_flood = null;
        }

        if (nearest_flood_curloc != null && rc.canSenseLocation(nearest_flood_curloc) && !rc.senseFlooding(nearest_flood_curloc)) {
            nearest_flood_curloc = null;
        }

        if (HQ.our_hq != null && rc.canSenseLocation(HQ.our_hq)) {
            // near hq, set to our hq
            hq = HQ.our_hq;
        }

        // if i'm ever next to hq, move away
        /*if (HQ.our_hq != null && hq.equals(HQ.our_hq) && cur_loc.distanceSquaredTo(hq) <= 2) {
            Helper.greedy_move_away(hq, cur_loc);
            return;
        }*/

        //System.out.println(Clock.getBytecodesLeft());
        // sense nearby deets for flood
        int k = 1;
        MapLocation new_loc = null;
        do {
            new_loc = new MapLocation(cur_loc.x + distx_35[k], cur_loc.y + disty_35[k]);
            if (rc.canSenseLocation(new_loc) && rc.senseFlooding(new_loc)) {
                if (nearest_flood_curloc == null || cur_loc.distanceSquaredTo(new_loc) < cur_loc.distanceSquaredTo(nearest_flood_curloc)) {
                    nearest_flood_curloc = new_loc;
                }
                if (hq != null && (nearest_flood == null || hq.distanceSquaredTo(new_loc) < hq.distanceSquaredTo(nearest_flood))) {
                    nearest_flood = new_loc;
                }
            }
            k++;
        } while (rc.canSenseLocation(new_loc) || (!Helper.onTheMap(new_loc)));
        //System.out.println(Clock.getBytecodesLeft());

        // if enemy HQ was never found
        if (HQ.patrol_broadcast_round != -1 && HQ.enemy_hq == null) {
            HQ.patrol_broadcast_round = round;
            // explore
            if (search_idx < 6) {
                if (cur_loc.distanceSquaredTo(Comms.explore[search_idx]) <= 10) {
                    search_idx++;
                }
                if (search_idx < 6) {
                    drone_walk(Comms.explore[search_idx]);
                }
            }
            return;
        }

        if (!rc.isCurrentlyHoldingUnit()) {
            // look for enemy units
            int num_enemies = 0;
            int closest_dist = 9999999;
            RobotInfo closest_robot = null;
            for (int i = 0; i < robots.length; i++) {
                if (robots[i].team != rc.getTeam() && robots[i].type.canBePickedUp()) {
                    num_enemies++;
                    if ((cur_loc.distanceSquaredTo(robots[i].getLocation()) < closest_dist && robots[i].type != RobotType.COW) ||
                            (closest_robot == null || closest_robot.type == RobotType.COW)) {
                        if (HQ.enemy_hq != null && robots[i].type == RobotType.COW &&
                                robots[i].getLocation().distanceSquaredTo(HQ.enemy_hq) < 25) {

                        } else {
                            closest_robot = robots[i];
                            closest_dist = cur_loc.distanceSquaredTo(closest_robot.getLocation());
                        }
                    }
                }
            }
            if (num_enemies > 0 && closest_robot != null && closest_dist <= GameConstants.DELIVERY_DRONE_PICKUP_RADIUS_SQUARED) {
                // pickup
                //System.out.println("Cur loc: " + cur_loc.toString() + " Pickup: " + closest_robot.getLocation().toString());
                if (rc.canPickUpUnit(closest_robot.ID)) {
                    rc.pickUpUnit(closest_robot.ID);
                    carried_type = closest_robot.type;
                }
            } else if (HQ.patrol_broadcast_round != -1 && HQ.enemy_hq != null && round < HQ.patrol_broadcast_round + 130) {
                //System.out.println("!!!");
                if (cur_loc.distanceSquaredTo(HQ.enemy_hq) < 50) {
                    //System.out.println("patrolling enemy hq");
                    // close-ish, start patrolling

                    int best_dst = 99999999;
                    Direction best_dir = Direction.CENTER;
                    for (int i = 0; i < directions.length; i++) {
                        if (blacklist[i]) {
                            continue;
                        }
                        int dst = cur_loc.add(directions[i]).distanceSquaredTo(HQ.enemy_hq);

                        if (rc.canMove(directions[i]) && dst > GameConstants.NET_GUN_SHOOT_RADIUS_SQUARED
                                && dst < best_dst) {
                            best_dst = dst;
                            best_dir = directions[i];
                        }
                    }

                    Helper.tryMove(best_dir);
                } else {
                    drone_walk(HQ.enemy_hq);
                }
            } else if (num_enemies > 0 && closest_robot != null) {
                //System.out.println(cur_loc.toString() + " collapse onto them " + closest_robot.getLocation());
                //System.out.println("Chase: " + closest_robot.getLocation().toString());
                if (HQ.patrol_broadcast_round != -1 && HQ.enemy_hq != null) {
                    //bugpath_ignore_blacklist(closest_robot.getLocation());
                    greedy_walk(closest_robot.getLocation());
                } else {
                    //drone_walk(closest_robot.getLocation());
                    greedy_walk(closest_robot.getLocation());
                }
            } else if (cur_loc.distanceSquaredTo(hq) < RobotType.FULFILLMENT_CENTER.sensorRadiusSquared) {
                // patrol area
                int best_dst = -1;
                Direction best_dir = Direction.CENTER;
                for (int i = 0; i < directions.length; i++) {
                    if (blacklist[i]) {
                        continue;
                    }
                    int dst = cur_loc.add(directions[i]).distanceSquaredTo(hq);

                    if (rc.canMove(directions[i]) && dst < RobotType.FULFILLMENT_CENTER.sensorRadiusSquared
                        && dst > best_dst) {
                        boolean valid = true;
                        for (int j = 0; j < prev_loc.length; j++) {
                            if (cur_loc.add(directions[i]).equals(prev_loc[j])) {
                                valid = false;
                                break;
                            }
                        }
                        if (valid) {
                            best_dst = dst;
                            best_dir = directions[i];
                        }
                    }
                }

                prev_loc[prev_loc_i % prev_loc.length] = cur_loc.add(best_dir);
                prev_loc_i++;
                Helper.tryMove(best_dir);
            } else {
                drone_walk(hq);
            }
        } else {
            // go to flood

            if (carried_type == RobotType.COW && HQ.enemy_hq != null) {
                int distance_to_hq = cur_loc.distanceSquaredTo(HQ.enemy_hq);
                if (distance_to_hq <= 25) {
                    // any closer and we'll get shot !!!
                    // drop close to them
                    int min_dist = 9999999;
                    int best_i = -1;
                    for (int i = 0; i < directions.length; i++) {
                        if (rc.canDropUnit(directions[i]) && cur_loc.add(directions[i]).distanceSquaredTo(HQ.enemy_hq) < min_dist
                            && !rc.senseFlooding(cur_loc.add(directions[i]))) {
                            min_dist = cur_loc.add(directions[i]).distanceSquaredTo(HQ.enemy_hq);
                            best_i = i;
                        }
                    }

                    if (best_i != -1) {
                        rc.dropUnit(directions[best_i]);
                    }
                } else {
                    // move towards
                    drone_walk(HQ.enemy_hq);
                }
            } else {
                // if have flood loc, move there
                if (nearest_flood_curloc != null) {
                    int temp_dist = cur_loc.distanceSquaredTo(nearest_flood_curloc);
                    if (temp_dist != 0 && temp_dist <= 2 && rc.canDropUnit(cur_loc.directionTo(nearest_flood_curloc))) {
                        //System.out.println("die chungus");
                        rc.dropUnit(cur_loc.directionTo(nearest_flood_curloc));
                    } else {
                        drone_walk(nearest_flood_curloc);
                    }
                } else if (nearest_flood != null) {
                    // adjacent, dump em
                    int temp_dist = cur_loc.distanceSquaredTo(nearest_flood);
                    if (temp_dist != 0 && temp_dist <= 2 && rc.canDropUnit(cur_loc.directionTo(nearest_flood))) {
                        //System.out.println("die chungus");
                        //TODO: uncomment once fixed
                        rc.dropUnit(cur_loc.directionTo(nearest_flood));
                    } else {
                        drone_walk(nearest_flood);
                    }
                } else {
                    // dont know where flood is, move ???
                    if (cur_loc.distanceSquaredTo(corners[corner_i % corners.length]) < RobotType.DELIVERY_DRONE.sensorRadiusSquared) {
                        // visited, inc
                        corner_i++;
                    }
                    drone_walk(corners[corner_i % corners.length]);
                }
            }
        }
    }


    static void bugpath_walk(MapLocation loc, boolean ignore_blacklist) throws GameActionException {
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
                if (temp_dist < least_dist && (ignore_blacklist || !blacklist[i])) {
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
                if (rc.canMove(cw) && (!blacklist[next] || ignore_blacklist)) {
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

    static void bugpath_ignore_blacklist(MapLocation loc) throws GameActionException {
        bugpath_walk(loc, true);
    }

    static void drone_walk(MapLocation loc) throws GameActionException {
        bugpath_walk(loc, false);
        /*
        Direction greedy;

        int least_dist = 9999999;
        int next = -1;
        for (int i = 0; i < directions.length; i++) {
            MapLocation next_loc = cur_loc.add(directions[i]);
            int temp_dist = next_loc.distanceSquaredTo(loc);
            if (temp_dist < least_dist) {
                least_dist = temp_dist;
                next = i;
            }
        }

        greedy = directions[next];

        if (rc.canMove(greedy) && !blacklist[next]) {
            rc.move(greedy);
        } else {
            for (int i = 0; i < 7; i++) {
                next = (next + 1) % directions.length;
                Direction cw = directions[next];
                if (rc.canMove(cw) && !blacklist[next]) {
                    rc.move(cw);
                    break;
                }
            }
        }*/
    }

    static void greedy_walk(MapLocation loc) throws GameActionException {
        Direction greedy;

        int least_dist = 9999999;
        int next = -1;
        for (int i = 0; i < directions.length; i++) {
            MapLocation next_loc = cur_loc.add(directions[i]);
            int temp_dist = next_loc.distanceSquaredTo(loc);
            if (temp_dist < least_dist) {
                least_dist = temp_dist;
                next = i;
            }
        }

        greedy = directions[next];

        if (rc.canMove(greedy) && !blacklist[next]) {
            rc.move(greedy);
        } else {
            for (int i = 0; i < 7; i++) {
                next = (next + 1) % directions.length;
                Direction cw = directions[next];
                if (rc.canMove(cw) && !blacklist[next]) {
                    rc.move(cw);
                    break;
                }
            }
        }
    }

}