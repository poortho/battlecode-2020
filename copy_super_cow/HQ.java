package copy_super_cow;

import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotInfo;
import battlecode.common.RobotType;

import static copy_super_cow.Helper.directions;
import static copy_super_cow.Helper.distx_35;
import static copy_super_cow.Helper.disty_35;
import static copy_super_cow.RobotPlayer.*;

public class HQ {

    static boolean surrounded_by_flood = false;

    static int TOTAL_MINERS = 4;
    static boolean done_turtling = false;
    static boolean broadcast_rush = false;

    // used in determining which of 3 directions to send...
    static int rotation = 0;
    static MapLocation[] possible_enemy_locs = new MapLocation[6];
    static int remove_num = 0;

    static boolean queued_near = false;

    static MapLocation enemy_hq, cur_loc, our_hq;
    static boolean rushed = false;

    static int patrol_broadcast_round = -1;
    static int friendly_drones = 0;
    static int friendly_miner = 0;
    static int friendly_turtle = 0;
    static int miner_count = 0;
    static boolean broadcasted_patrol = false;
    static boolean turtling = false;

    static boolean gay_rush_alert = false;

    static MapLocation closest_rush_enemy = null;

    static void runHQ() throws GameActionException {
      cur_loc = rc.getLocation();

      turtling = check_turtling();

      RobotInfo[] robots = rc.senseNearbyRobots();
      friendly_drones = 0;
      friendly_miner = 0;
      for (int i = 0; i < robots.length; i++) {
        RobotInfo r = robots[i];
        if (r.team == rc.getTeam()) {
          switch(r.type) {
            case DELIVERY_DRONE:
              friendly_drones++;
              break;

            case MINER:
              friendly_miner++;
              break;

            case LANDSCAPER:
              if (r.location.distanceSquaredTo(cur_loc) <= 2) {
                friendly_turtle++;
              }
              break;
          }
        }
      }
      if (turnCount == 1) {
          int width = rc.getMapWidth();
          int height = rc.getMapHeight();
          MapLocation middle = new MapLocation(width / 2, height / 2);

          // set locations
          int delta_x = middle.x + (middle.x - cur_loc.x);
          int delta_y = middle.y + (middle.y - cur_loc.y);
          possible_enemy_locs[0] = new MapLocation(middle.x, middle.y);
          possible_enemy_locs[1] = new MapLocation(delta_x, delta_y);
          possible_enemy_locs[2] = new MapLocation(delta_x, cur_loc.y);
          possible_enemy_locs[3] = new MapLocation(cur_loc.x, delta_y);
          possible_enemy_locs[4] = new MapLocation(delta_x, middle.y);
          possible_enemy_locs[5] = new MapLocation(middle.x, delta_y);
          //possible_enemy_locs[5] = new MapLocation((cur_loc.x / middle.x) * (width-1), (cur_loc.y / middle.y) * (height-1));
          //System.out.println(Arrays.toString(possible_enemy_locs));
          Comms.setSeed(possible_enemy_locs);

          // find soup that's close

          queued_near = queue_close_soup();
          if (!queued_near) {
             int msg[] = {0, 0, 0, 0, 0, 0, 0};
            // initial broadcast miner request
            for (int i = 3; i >= 0; i--) {
              MapLocation loc = possible_enemy_locs[i];
              int val = (loc.x << 16) | (loc.y << 8) | (TOTAL_MINERS << 4) | 0x1;
              val |= 1 << 24;
              msg[i] = val;
            }
            Comms.addMessage(msg, 1, 2);
            //for (int i = 5; i >= 0; i--) {
              //Comms.broadcast_miner_request(possible_enemy_locs[i], 1, true);
            //}
          }

      }
      if (turnCount >= 1) {

        if (turnCount == 2 && queued_near) {
          int msg[] = {0, 0, 0, 0, 0, 0, 0};
          // initial broadcast miner request
          for (int i = 3; i >= 0; i--) {
            MapLocation loc = possible_enemy_locs[i];
            int val = (loc.x << 16) | (loc.y << 8) | (1 << 4) | 0x1;
            val |= 3 << 24;
            msg[3 - i] = val;
          }
          Comms.addMessage(msg, 1, 2);
          //for (int i = 5; i >= 0; i--) {
            //Comms.broadcast_miner_request(possible_enemy_locs[i], 1, true);
          //}
        }

	    	Comms.getBlocks();

        if (turnCount == 30) {
          Comms.broadcast_friendly_hq(cur_loc);
        }

        if (!broadcasted_patrol) {
            check_if_flooded();
        }

        // check for if we're being rushed
        if (!rushed) {
          rushed = checkRush();
          if (rushed) {
            Comms.broadcast_being_rushed(gay_rush_alert);
          }
        } else {
          rushed = checkRush();
          if (!rushed) {
            Comms.broadcast_end_rushed();
          }
        }

        shootNetGun();
        /*
        if (rushed && friendly_miner + friendly_turtle < 8) {
          build_defensive_miner(closest_rush_enemy);
        } else if (!turtling && miner_count < TOTAL_MINERS && rc.getTeamSoup() >= RobotType.MINER.cost * (miner_count - 3)) {
          handle_miners();
        }*/
        if (!turtling && miner_count < TOTAL_MINERS && rc.getTeamSoup() >= RobotType.MINER.cost * (miner_count - 3)) {
          if (Comms.design_school_idx != 0 || miner_count < 4) {
            handle_miners();
          }
        }

        if (!HQ.done_turtling) {
            check_turtle();
        }
        if (!HQ.surrounded_by_flood) {
            check_trapped();
        }
	    }
	  }

	  static void check_trapped() throws GameActionException {
        int[] ring_4_x = {4, 4, 4, 4, 4, 4, 4, 4, 4,
                          3, 2, 1, 0, -1, -2, -3,
                          -4, -4, -4, -4, -4, -4, -4, -4, -4,
                          -3, -2, -1, 0, 1, 2, 3};
        int[] ring_4_y = {3, 2, 1, 0, -1, -2, -3,
                          -4, -4, -4, -4, -4, -4, -4, -4, -4,
                          -3, -2, -1, 0, 1, 2, 3,
                          4, 4, 4, 4, 4, 4, 4, 4, 4};
        int non_flooded_count = 0;
        for (int i = ring_4_x.length; --i >= 0; ) {
            MapLocation new_loc = cur_loc.translate(ring_4_x[i], ring_4_y[i]);
            if (!rc.canSenseLocation(new_loc) || !rc.senseFlooding(new_loc)) {
                non_flooded_count++;
            }
        }
        if (non_flooded_count <= 10) {
            Comms.broadcast_hq_trapped();
        }
      }

	  static void check_turtle() throws GameActionException {
        boolean is_full = true;
        for (int i = 1; Math.pow(distx_35[i], 2) + Math.pow(disty_35[i], 2) <= 8; i++) {
            RobotInfo r;
            MapLocation new_loc = cur_loc.translate(distx_35[i], disty_35[i]);
            if (rc.canSenseLocation(new_loc)) {
                if (i >= 9 && i < 13 && rc.senseElevation(new_loc) <= -10) {
                    continue;
                }
                r = rc.senseRobotAtLocation(new_loc);
                is_full = is_full && r != null && r.type == RobotType.LANDSCAPER;
                //System.out.println(new_loc);
                //System.out.println(is_full);
            }
        }
        if (is_full) {
            Comms.broadcast_done_turtle();
        }
      }

    static boolean check_turtling() throws GameActionException {
      int blocked = 0;
      for (int i = 0; i < directions.length; i++) {
        MapLocation next_loc = cur_loc.add(directions[i]);
        if (rc.canSenseLocation(next_loc) && rc.senseElevation(next_loc) > rc.senseElevation(cur_loc) + 3) {
          blocked++;
        }
      }
      if (blocked >= 5) {
        return true;
      }
      return false;
    }

	  static void check_if_flooded() throws GameActionException {
        MapLocation next_loc = null;
        int index = 1;
        int flooded_count = 0;
        int nonflooded_count = 0;
        do {
            next_loc = new MapLocation(cur_loc.x + distx_35[index], cur_loc.y + disty_35[index]);
            if (rc.canSenseLocation(next_loc)) {
                if (rc.senseFlooding(next_loc)) {
                    flooded_count++;
                } else {
                    nonflooded_count++;
                }
            }
            index++;
        } while (next_loc.distanceSquaredTo(cur_loc) <= 15);
        if (flooded_count > nonflooded_count) {
            //System.out.println("broadcasted!");
            Comms.broadcast_patrol_enemy_hq();
        }
      }

    static void build_defensive_miner(MapLocation closest_enemy) throws GameActionException {
      Helper.tryBuildToward(RobotType.MINER, closest_enemy);
    }

    static boolean checkRush() throws GameActionException {
      gay_rush_alert = false;
      RobotInfo[] nearby = rc.senseNearbyRobots();
      int enemy_land = 0;
      int friendly_land = 0;
      int enemy_design = 0;

      int min_dist = 999999;
      for (int i = 0; i < nearby.length; i++) {
        switch (nearby[i].type) {
          case LANDSCAPER:
            if (nearby[i].team != rc.getTeam()) {
              enemy_land++;
              int dist = nearby[i].location.distanceSquaredTo(cur_loc);
              if (dist < min_dist) {
                min_dist = dist;
                closest_rush_enemy = nearby[i].location;
              }
            } else {
              friendly_land++;
            }
            break;
          case DESIGN_SCHOOL:
            if (nearby[i].team != rc.getTeam() && cur_loc.distanceSquaredTo(nearby[i].location) <= 8) {
              enemy_design++;
              int dist = nearby[i].location.distanceSquaredTo(cur_loc);
              if (dist < min_dist) {
                min_dist = dist;
                closest_rush_enemy = nearby[i].location;
                if (dist <= 2) {
                  gay_rush_alert = true;
                }
              }
            }
          case MINER:
            if (nearby[i].team != rc.getTeam() && round < 200) {
              gay_rush_alert = true;
            }
        }
      }
      return ((enemy_land > friendly_land || enemy_design >= 1) && friendly_drones < (enemy_land + enemy_design)) || gay_rush_alert;
    }

    static boolean queue_close_soup() throws GameActionException {
      MapLocation[] soup = rc.senseNearbySoup();
      int max_dist = -1;
      int idx = -1;
      for (int i = 0; i < soup.length; i++) {
        int temp_dist = soup[i].distanceSquaredTo(cur_loc);
        if (temp_dist > max_dist) {
          max_dist = temp_dist;
          idx = i;
        }
      }

      if (idx != -1) {
        //System.out.println("FOUND SOUP " + soup[idx].toString());
        Comms.broadcast_miner_request_double(soup[idx], TOTAL_MINERS, true);
        return true;
      }
      return false;
    }

    static void handle_miners() throws GameActionException {
      // handle building miners from queue
      if (Comms.miner_queue_peek() != null || miner_count == 0) {
        int res = Helper.tryBuild(RobotType.MINER);
        if (res != -1) {
          //System.out.println("PRODUCED");
          miner_count++;
          if (miner_count == 1) {
            Comms.broadcast_rushing_miner();
          }
        }
      }
    }

    static void shootNetGun() throws GameActionException {
        RobotInfo[] robots = rc.senseNearbyRobots();
        for (int i = 0; i < robots.length; i++) {
            if (robots[i].team != rc.getTeam() && robots[i].type == RobotType.DELIVERY_DRONE &&
                    rc.canShootUnit(robots[i].ID)) {
                // TODO: base on distance or something to units
                rc.shootUnit(robots[i].ID);
                break;
            }
        }        
    }
}
