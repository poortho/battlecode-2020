package bot1;
import battlecode.common.*;

import static bot1.Helper.directions;
import static bot1.RobotPlayer.turnCount;
import static bot1.RobotPlayer.rc;

public class HQ {
  static void runHQ() throws GameActionException {
    int width = rc.getMapWidth();
    int height = rc.getMapHeight();
    MapLocation middle = new MapLocation(width / 2, height / 2);
    MapLocation myLoc = rc.getLocation();
    MapLocation[] possible_enemy_locs = new MapLocation[3];

    // set locations
    int delta_x = middle.x + (middle.x - myLoc.x);
    int delta_y = middle.y + (middle.y - myLoc.y);
    possible_enemy_locs[0] = new MapLocation(delta_x, delta_y);
    possible_enemy_locs[1] = new MapLocation(delta_x, myLoc.y);
    possible_enemy_locs[2] = new MapLocation(myLoc.x, delta_y);
    /*System.out.println(possible_enemy_locs[0]);
    System.out.println(possible_enemy_locs[1]);
    System.out.println(possible_enemy_locs[2]);*/

    for (Direction dir : Helper.directions)
      Helper.tryBuild(RobotType.MINER, dir);
  }
}
