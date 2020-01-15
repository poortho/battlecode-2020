package copy_super_cow;

import battlecode.common.*;

import static copy_super_cow.RobotPlayer.*;

public class Helper {

  static Direction[] directions;
  static int[] distx_35 = {0, -1, 0, 0, 1, -1, -1, 1, 1, -2, 0, 0, 2, -2, -2, -1, -1, 1, 1, 2, 2, -2, -2, 2, 2, -3, 0, 0, 3, -3, -3, -1, -1, 1, 1, 3, 3, -3, -3, -2, -2, 2, 2, 3, 3, -4, 0, 0, 4, -4, -4, -1, -1, 1, 1, 4, 4, -3, -3, 3, 3, -4, -4, -2, -2, 2, 2, 4, 4, -5, -4, -4, -3, -3, 0, 0, 3, 3, 4, 4, 5, -5, -5, -1, -1, 1, 1, 5, 5, -5, -5, -2, -2, 2, 2, 5, 5, -4, -4, 4, 4, -5, -5, -3, -3, 3, 3, 5, 5};
  static int[] disty_35 = {0, 0, -1, 1, 0, -1, 1, -1, 1, 0, -2, 2, 0, -1, 1, -2, 2, -2, 2, -1, 1, -2, 2, -2, 2, 0, -3, 3, 0, -1, 1, -3, 3, -3, 3, -1, 1, -2, 2, -3, 3, -3, 3, -2, 2, 0, -4, 4, 0, -1, 1, -4, 4, -4, 4, -1, 1, -3, 3, -3, 3, -2, 2, -4, 4, -4, 4, -2, 2, 0, -3, 3, -4, 4, -5, 5, -4, 4, -3, 3, 0, -1, 1, -5, 5, -5, 5, -1, 1, -2, 2, -5, 5, -5, 5, -2, 2, -4, 4, -4, 4, -3, 3, -5, 5, -5, 5, -3, 3};
  static int[] edges_x = {2, 0, -2, 0};
  static int[] edges_y = {0, 2, 0, -2};

  static void greedy_move_adjacent_HQ(MapLocation target, MapLocation cur_loc) throws GameActionException {
    int min_dist = cur_loc.distanceSquaredTo(target);
    int next = -1;
    for (int i = 0; i < directions.length; i++) {
      MapLocation next_loc = cur_loc.add(directions[i]);
      int temp_dist = next_loc.distanceSquaredTo(target);
      if (temp_dist < min_dist && rc.canMove(directions[i]) && next_loc.distanceSquaredTo(HQ.our_hq) <= 2 && !rc.senseFlooding(next_loc)) {
        min_dist = temp_dist;
        next = i;
      }
    }

    if (next != -1)
      rc.move(directions[next]); 
  }

  static int getLevel(int r) {
    int res = (int)Math.floor(Math.exp(0.0028 * r -1.38*Math.sin(0.00157*r-1.73)+1.38*Math.sin(-1.73)) - 1);
    //System.out.println("Level: " + Integer.toString(res) + " Round: " + Integer.toString(r));
    return res;
  }

  static boolean willFlood(MapLocation loc) throws GameActionException {
    int elevation = rc.senseElevation(loc);
    return elevation <= getLevel(round + 1);
  }

  // -1 means didn't build
  // returns index in directions where robot was built
  // avoid building in places adjacent to buildings
  static int tryBuildNotAdjacentHQ(RobotType type, boolean near_hq) throws GameActionException {
    if (!rc.isReady()) {
      return -1;
    }
    MapLocation cur_loc = rc.getLocation();
    for (int i = 0; i < directions.length; i++) {
      MapLocation new_loc = cur_loc.add(directions[i]);
      if (rc.canBuildRobot(type, directions[i]) && near_hq && new_loc.distanceSquaredTo(HQ.our_hq) > 2) {
        rc.buildRobot(type, directions[i]);
        return i;
      }
    }
    return -1;
  }

  static void tryBuildToward(RobotType type, MapLocation loc) throws GameActionException {
    if (!rc.isReady()) {
      return;
    }

    int min_dist = 9999999;
    int min_dir = -1;

    MapLocation cur_loc = rc.getLocation();

    for (int i = 0; i < directions.length; i++) {
      if (rc.canBuildRobot(type, directions[i])) {
        MapLocation new_loc = cur_loc.add(directions[i]);
        int dist = new_loc.distanceSquaredTo(loc);
        if (dist < min_dist) {
          min_dist = dist;
          min_dir = i;
        }
      }
    }
    if (min_dir != -1) {
      rc.buildRobot(type, directions[min_dir]);
    }
  }

  static int tryBuild(RobotType type) throws GameActionException {
    if (!rc.isReady()) {
      return -1;
    }
    for (int i = 0; i < directions.length; i++) {
      if (rc.canBuildRobot(type, directions[i])) {
        rc.buildRobot(type, directions[i]);
        return i;
      }
    }
    return -1;
  }

  static void try_miner_suicide(MapLocation cur_loc) throws GameActionException {
    for (int i = 0; i < directions.length; i++) {
      MapLocation next_loc = cur_loc.add(directions[i]);
      if (rc.canMove(directions[i]) && rc.senseFlooding(next_loc)) {
        rc.move(directions[i]);
      }
    }
  }

  static boolean greedy_move_away(MapLocation loc, MapLocation cur_loc) throws GameActionException {
    int max_dist = cur_loc.distanceSquaredTo(loc);
    int next = -1;
    for (int i = 0; i < directions.length; i++) {
      MapLocation next_loc = cur_loc.add(directions[i]);
      int temp_dist = next_loc.distanceSquaredTo(loc);
      if (temp_dist > max_dist && rc.canMove(directions[i]) && !rc.senseFlooding(next_loc)) {
        max_dist = temp_dist;
        next = i;
      }
    }

    if (next != -1) {
      rc.move(directions[next]);
      return true;
    }
    return false;
  }

  static boolean tryBuild(RobotType type, Direction dir) throws GameActionException {
    if (rc.isReady() && rc.canBuildRobot(type, dir)) {
      rc.buildRobot(type, dir);
      return true;
    } else return false;
  }

  static void tryDigAway(MapLocation loc) throws GameActionException {
    if (!rc.isReady()) {
      return;
    }
    int max_dist = -1;
    Direction best_dir = null;
    boolean allow_design_adjacent = true;
    for (int i = 0; i < directions.length; i++) {
      MapLocation new_loc = Landscaper.cur_loc.add(directions[i]);
      if (rc.canDigDirt(directions[i]) && new_loc.distanceSquaredTo(loc) > max_dist && (HQ.our_hq.distanceSquaredTo(new_loc) > 3)) {
        boolean adjacent = false;
        for (int j = 0; j < directions.length; j++) {
          MapLocation even_newer_loc = new_loc.add(directions[j]);
          if (rc.canSenseLocation(even_newer_loc)) {
            RobotInfo r = rc.senseRobotAtLocation(even_newer_loc);
            if (r != null && r.type == RobotType.DESIGN_SCHOOL && r.team == rc.getTeam()) {
              adjacent = true;
            }
          }
        }

        RobotInfo rob = rc.senseRobotAtLocation(new_loc);
        if (rob != null && HQ.our_hq != null && rob.type == RobotType.MINER && rob.team == rc.getTeam()) {
          continue;
        }

        if ((allow_design_adjacent && !adjacent) ||
           (!allow_design_adjacent && !adjacent && new_loc.distanceSquaredTo(loc) > max_dist) ||
           (allow_design_adjacent && new_loc.distanceSquaredTo(loc) > max_dist)) {
          max_dist = Landscaper.cur_loc.add(directions[i]).distanceSquaredTo(loc);
          best_dir = directions[i];
          if (!adjacent) {
            allow_design_adjacent = false;
          }
        }
      }
    }

    if (rc.canDigDirt(Direction.CENTER) && Landscaper.cur_loc.add(Direction.CENTER).distanceSquaredTo(loc) > max_dist) {
      max_dist = Landscaper.cur_loc.add(Direction.CENTER).distanceSquaredTo(loc);
      best_dir = Direction.CENTER;
    }

    if (best_dir != null) {
      rc.digDirt(best_dir);
    }
  }

  static int tryDepositClose(MapLocation loc) throws GameActionException {
    if (!rc.isReady()) {
      return -1;
    }
    int min_dist = 9999999;
    int best_i = -1;
    for (int i = 0; i < directions.length; i++) {
      if (rc.canDepositDirt(directions[i]) && Landscaper.cur_loc.add(directions[i]).distanceSquaredTo(loc) < min_dist) {
        min_dist = Landscaper.cur_loc.add(directions[i]).distanceSquaredTo(loc);
        best_i = i;
      }
    }

    if (best_i != -1) {
      rc.depositDirt(directions[best_i]);
    }

    return best_i;
  }

  static int tryDig() throws GameActionException {
    if (!rc.isReady()) {
      return -1;
    }
    // dig evenly...
    int highest_el = -99999999;
    Direction best = null;
    for (int i = 0; i < directions.length; i++) {
      if (rc.canDigDirt(directions[i]) && rc.senseElevation(Landscaper.cur_loc.add(directions[i])) > highest_el) {
        highest_el = rc.senseElevation(Landscaper.cur_loc.add(directions[i]));
        best = directions[i];
      }
    }
    if (rc.canDigDirt(Direction.CENTER) && rc.senseElevation(Landscaper.cur_loc.add(Direction.CENTER)) > highest_el) {
      highest_el = rc.senseElevation(Landscaper.cur_loc.add(Direction.CENTER));
      best = Direction.CENTER;
    }

    if (best != null) {
      rc.digDirt(best);
    }
    return -1;
  }

  static boolean tryDigEdges() throws GameActionException {
    for (int i = 0; i < edges_x.length; i++) {
      MapLocation loc = new MapLocation(Landscaper.my_hq.x + edges_x[i], Landscaper.my_hq.y + edges_y[i]);
      if (Landscaper.cur_loc.distanceSquaredTo(loc) <= 3 && rc.canDigDirt(Landscaper.cur_loc.directionTo(loc))) {
        rc.digDirt(Landscaper.cur_loc.directionTo(loc));
        return true;
      }
    }
    return false;
  }

  static boolean tryDig(Direction dir) throws GameActionException {
    if (rc.isReady() && rc.canDigDirt(dir)) {
      rc.digDirt(dir);
      return true;
    } else return false;
  }

  static boolean tryMove(Direction dir) throws GameActionException {
    // System.out.println("I am trying to move " + dir + "; " + rc.isReady() + " " + rc.getCooldownTurns() + " " + rc.canMove(dir));
    if (rc.isReady() && rc.canMove(dir)) {
      rc.move(dir);
      return true;
    }
    return false;
  }
 
  static Direction randomDirection() {
    return directions[(int) (Math.random() * directions.length)];
  }

  static void tryBlockchain() throws GameActionException {
    if (RobotPlayer.turnCount < 3) {
      int[] message = new int[10];
      for (int i = 0; i < 10; i++) {
        message[i] = 123;
      }
      if (rc.canSubmitTransaction(message, 10))
        rc.submitTransaction(message, 10);
    }
    // System.out.println(rc.getRoundMessages(turnCount-1));
  }

  static void check_netguns() throws GameActionException {
    // sense robots after moving, broadcast if netgun
    RobotInfo[] robots = rc.senseNearbyRobots(-1, rc.getTeam() == Team.A ? Team.B : Team.A);
    for (int i = 0; i < robots.length; i++) {
      if ((robots[i].type == RobotType.NET_GUN || robots[i].type == RobotType.HQ) &&
              RobotPlayer.netgun_map[robots[i].location.x][robots[i].location.y] >= Math.max(rc.getRoundNum() - 100, 0)) {
        // netgun, broadcast it :O
        Comms.broadcast_enemy_netgun(robots[i].location);
      }
    }
  }

  static boolean onTheMap(MapLocation loc) {
    return loc.x >= 0 && loc.y >= 0 && loc.x < rc.getMapWidth() && loc.y < rc.getMapHeight();
  }

  static boolean digLattice(MapLocation cur_loc) throws GameActionException {
    for (int i = 0; i < directions.length; i++) {
      MapLocation new_loc = cur_loc.add(directions[i]);
      if (!isLattice(new_loc) && rc.canDigDirt(directions[i])) {
        rc.digDirt(directions[i]);
        return true;
      }
    }
    return false;
  }

  static boolean isLattice(MapLocation loc) {
    return Math.abs(loc.x - HQ.our_hq.x) % 2 != 1 || Math.abs(loc.y - HQ.our_hq.y) % 2 != 1;
  }

  static Direction oppositeDirection(Direction d) {
    switch (d) {
      case NORTH:
        return Direction.SOUTH;
      case SOUTH:
        return Direction.NORTH;
      case WEST:
        return Direction.EAST;
      case EAST:
        return Direction.WEST;
      case NORTHEAST:
        return Direction.SOUTHWEST;
      case NORTHWEST:
        return Direction.SOUTHEAST;
      case SOUTHEAST:
        return Direction.NORTHWEST;
      case SOUTHWEST:
        return Direction.NORTHEAST;
    }
    return null;
  }

}