package bot2;

import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotInfo;
import battlecode.common.RobotType;

import java.util.Arrays;

import static bot2.Helper.distx_35;
import static bot2.Helper.disty_35;
import static bot2.RobotPlayer.*;

public class HQ {
    // used in determining which of 3 directions to send...
    static int rotation = 0;
    static MapLocation[] possible_enemy_locs = new MapLocation[6];
    static int remove_num = 0;

    static boolean queued_near = false;

    static MapLocation enemy_hq, cur_loc, our_hq;

    static void runHQ() throws GameActionException {

      cur_loc = rc.getLocation();

      RobotInfo[] robots = rc.senseNearbyRobots();
      if (turnCount == 1) {
          int width = rc.getMapWidth();
          int height = rc.getMapHeight();
          MapLocation middle = new MapLocation(width / 2, height / 2);

          // set locations
          int delta_x = middle.x + (middle.x - cur_loc.x);
          int delta_y = middle.y + (middle.y - cur_loc.y);
          possible_enemy_locs[0] = new MapLocation(delta_x, delta_y);
          possible_enemy_locs[1] = new MapLocation(middle.x, middle.y);
          possible_enemy_locs[2] = new MapLocation(delta_x, cur_loc.y);
          possible_enemy_locs[3] = new MapLocation(cur_loc.x, delta_y);
          possible_enemy_locs[4] = new MapLocation(delta_x, middle.y);
          possible_enemy_locs[5] = new MapLocation(middle.x, delta_y);
          //possible_enemy_locs[5] = new MapLocation((cur_loc.x / middle.x) * (width-1), (cur_loc.y / middle.y) * (height-1));
          System.out.println(Arrays.toString(possible_enemy_locs));
          Comms.setSeed(possible_enemy_locs);

          // find soup that's close

          queued_near = queue_close_soup();
          if (!queued_near) {
             int msg[] = {0, 0, 0, 0, 0, 0, 0};
            // initial broadcast miner request
            for (int i = 5; i >= 0; i--) {
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
          for (int i = 5; i >= 0; i--) {
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

        shootNetGun();
        handle_miners();

        if (turnCount == 80) {
          Comms.broadcast_friendly_hq(cur_loc);
        }
	    }
	  }

    static boolean queue_close_soup() throws GameActionException {
      for (int i = 0; i < distx_35.length; i++) {
        MapLocation next_loc = cur_loc.translate(distx_35[i], disty_35[i]);
        if (rc.canSenseLocation(next_loc)) {
          int count = rc.senseSoup(next_loc);
          if (count != 0) {
            Comms.broadcast_miner_request(next_loc, 2, true);
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
