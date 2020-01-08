package bot1;
import battlecode.common.*;
import java.util.Arrays;

import static bot1.Helper.directions;
import static bot1.RobotPlayer.turnCount;
import static bot1.RobotPlayer.round;
import static bot1.RobotPlayer.rc;

public class HQ {
    // used in determining which of 3 directions to send...
    static int rotation = 0;
    static MapLocation[] possible_enemy_locs = new MapLocation[6];

    static int remove_num = 0;

    static void runHQ() throws GameActionException {


      if (turnCount == 1) {
          int width = rc.getMapWidth();
          int height = rc.getMapHeight();
          MapLocation middle = new MapLocation(width / 2, height / 2);
          MapLocation myLoc = rc.getLocation();

          // set locations
          int delta_x = middle.x + (middle.x - myLoc.x);
          int delta_y = middle.y + (middle.y - myLoc.y);
          possible_enemy_locs[0] = new MapLocation(delta_x, delta_y);
          possible_enemy_locs[1] = new MapLocation(delta_x, myLoc.y);
          possible_enemy_locs[2] = new MapLocation(myLoc.x, delta_y);
          possible_enemy_locs[3] = new MapLocation(delta_x, middle.y);
          possible_enemy_locs[4] = new MapLocation(middle.x, delta_y);
          possible_enemy_locs[5] = new MapLocation((myLoc.x / middle.x) * (width-1), (myLoc.y / middle.y) * (height-1));
          System.out.println(Arrays.toString(possible_enemy_locs));
          Comms.setSeed(possible_enemy_locs);
	      	for (int i = 5; i >= 0; i--) {
	      		Comms.broadcast_miner_request(possible_enemy_locs[i], 1, true);
	      	}
      }
      if (turnCount > 1) {

	    	Comms.getBlocks();
	    	// handle building miners from queue
	    	if (Comms.miner_queue_peek() != null && Comms.miner_queue_num[Comms.poll_idx] > 0) {
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
	  }
}
