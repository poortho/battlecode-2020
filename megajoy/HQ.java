package megajoy;

import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotInfo;
import battlecode.common.RobotType;

import static megajoy.Helper.directions;
import static megajoy.Helper.distx_35;
import static megajoy.Helper.disty_35;
import static megajoy.RobotPlayer.*;

public class HQ {
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
              int val = (loc.x << 16) | (loc.y << 8) | (1 << 4) | 0x1;
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
            val |= 1 << 24;
            msg[i] = val;
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
        if (rushed && friendly_miner + friendly_turtle < 8) {
          build_defensive_miner(closest_rush_enemy);
        } else if (!turtling) {
          handle_miners();
        }
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
                  System.out.println("FUCK YOU BATTLEGAODE");
                }
              }
            }
        }
      }
      return (enemy_land >= 1 || enemy_design >= 1) && friendly_drones < (enemy_land + enemy_design);
    }

    static boolean queue_close_soup() throws GameActionException {
      for (int i = 0; i < distx_35.length; i++) {
        MapLocation next_loc = cur_loc.translate(distx_35[i], disty_35[i]);
        if (rc.canSenseLocation(next_loc)) {
          int count = rc.senseSoup(next_loc);
          if (count != 0) {
            Comms.broadcast_miner_request(next_loc, 3, true);
            return true;
          }
        }
      }
      return false;
    }

    static void handle_miners() throws GameActionException {
      // handle building miners from queue
      if (Comms.miner_queue_peek() != null && Comms.miner_queue_num[Comms.poll_idx] > 0 && (round <= 150 || rc.getTeamSoup() > 270)) {
        int res = Helper.tryBuild(RobotType.MINER);
        if (res != -1) {
          Comms.miner_queue_num[Comms.poll_idx] -= 1;
          if ((Comms.miner_queue_num[Comms.poll_idx] & 0xff) == 0) {
            remove_num++;
          }
        }
      }
      if (remove_num > 0) {
        boolean res = Comms.broadcast_miner_remove();
        if (res) {
          remove_num--;
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
