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
      //System.out.println(Arrays.toString(possible_enemy_locs));
      Comms.setSeed(possible_enemy_locs);
  	}
  	if (round <= 3) {
	    for (Direction dir : directions) {
	      boolean res = Helper.tryBuild(RobotType.MINER, dir);
	      if (res) {
	      	System.out.println("Created miner " + Integer.toString(round));
		      break;
	      }
	    }
	  }
    /*for (int i = 0; i < possible_enemy_locs.length; i++) {
      System.out.println(possible_enemy_locs[i]);
    }*/
    // TODO: spawn in order of distance to destination?
  }
}
