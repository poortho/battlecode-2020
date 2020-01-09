package bot1;
import battlecode.common.*;

import java.util.Map;

import static bot1.Helper.directions;
import static bot1.Helper.distx_35;
import static bot1.Helper.disty_35;
import static bot1.RobotPlayer.turnCount;
import static bot1.RobotPlayer.rc;

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

    static void runDeliveryDrone() throws GameActionException {
        cur_loc = rc.getLocation();
        robots = rc.senseNearbyRobots();

        for (int i = 0; i < blacklist.length; i++) {
            blacklist[i] = false;
        }

        // compute blacklist (dont move adjacent to enemy drones, and stay out of shooting range of HQ/netgun
        for (int i = 0; i < robots.length; i++) {
            if (robots[i].team != rc.getTeam()) {
                switch (robots[i].type) {
                    case NET_GUN:
                    case HQ:
                        // avoid netgun
                        for (int j = 0; j < directions.length; j++) {
                            if (cur_loc.add(directions[j]).distanceSquaredTo(robots[i].getLocation())
                                    <= GameConstants.NET_GUN_SHOOT_RADIUS_SQUARED) {
                                blacklist[j] = true;
                            }
                        }
                        break;
                    case DELIVERY_DRONE:
                        // dont move into enemy drone pickup
                        for (int j = 0; j < directions.length; j++) {
                            if (cur_loc.add(directions[j]).distanceSquaredTo(robots[i].getLocation())
                                    <= GameConstants.DELIVERY_DRONE_PICKUP_RADIUS_SQUARED) {
                                blacklist[j] = true;
                            }
                        }
                        break;
                }
            }
        }

        if (turnCount == 1) {
            // first turn, get loc of "home"
            for (int i = 0; i < robots.length; i++) {
                if (robots[i].team == rc.getTeam() && robots[i].type == RobotType.FULFILLMENT_CENTER) {
                    hq = robots[i].getLocation();
                    break;
                }
            }
            // TODO: do this based on start?
            corners[0] = new MapLocation(0, 0);
            corners[1] = new MapLocation(rc.getMapWidth(), 0);
            corners[2] = new MapLocation(0, rc.getMapHeight());
            corners[3] = new MapLocation(rc.getMapWidth(), rc.getMapHeight());
        }
        if (!rc.isCurrentlyHoldingUnit()) {
            // look for enemy units
            int num_enemies = 0;
            int closest_dist = 9999999;
            RobotInfo closest_robot = null;
            for (int i = 0; i < robots.length; i++) {
                if (robots[i].team != rc.getTeam() && robots[i].type.canBePickedUp()) {
                    num_enemies++;
                    if (cur_loc.distanceSquaredTo(robots[i].getLocation()) < closest_dist) {
                        closest_robot = robots[i];
                        closest_dist = cur_loc.distanceSquaredTo(closest_robot.getLocation());
                    }
                }
            }

            // move towards them...
            if (num_enemies > 0) {
                if (closest_dist <= GameConstants.DELIVERY_DRONE_PICKUP_RADIUS_SQUARED) {
                    // pickup
                    if (rc.canPickUpUnit(closest_robot.ID)) {
                        rc.pickUpUnit(closest_robot.ID);
                    }
                } else {
                    drone_walk(closest_robot.getLocation());
                }
            } else if (cur_loc.distanceSquaredTo(hq) < RobotType.FULFILLMENT_CENTER.sensorRadiusSquared) {
                // patrol area
                int best_dst = -1;
                Direction best_dir = Direction.CENTER;
                for (int i = 0; i < directions.length; i++) {
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
                // just dropped a unit, move back to hq
                drone_walk(hq);
            }
        } else {
            // go to flood

            // sense nearby deets for flood
            int i = 0;
            while (Math.pow(distx_35[i], 2) + Math.pow(disty_35[i], 2) <= RobotType.DELIVERY_DRONE.sensorRadiusSquared) {
                MapLocation new_loc = new MapLocation(cur_loc.x + distx_35[i], cur_loc.y + disty_35[i]);
                if (rc.canSenseLocation(new_loc) && rc.senseFlooding(new_loc)) {
                    if (nearest_flood == null || hq.distanceSquaredTo(new_loc) < hq.distanceSquaredTo(nearest_flood)) {
                        nearest_flood = new_loc;
                    }
                }
                i++;
            }

            // if have flood loc, move there
            if (nearest_flood != null) {
                // adjacent, dump em
                if (cur_loc.distanceSquaredTo(nearest_flood) <= 2 && rc.canDropUnit(cur_loc.directionTo(nearest_flood))) {
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

    static void drone_walk(MapLocation loc) throws GameActionException {
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