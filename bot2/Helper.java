package bot2;

import battlecode.common.*;
import java.lang.Math;

import static bot2.RobotPlayer.rc;
import static bot2.RobotPlayer.round;

public class Helper {

  static Direction[] directions;
  static int[] distx_35 = {0, -1, 0, 0, 1, -1, -1, 1, 1, -2, 0, 0, 2, -2, -2, -1, -1, 1, 1, 2, 2, -2, -2, 2, 2, -3, 0, 0, 3, -3, -3, -1, -1, 1, 1, 3, 3, -3, -3, -2, -2, 2, 2, 3, 3, -4, 0, 0, 4, -4, -4, -1, -1, 1, 1, 4, 4, -3, -3, 3, 3, -4, -4, -2, -2, 2, 2, 4, 4, -5, -4, -4, -3, -3, 0, 0, 3, 3, 4, 4, 5, -5, -5, -1, -1, 1, 1, 5, 5, -5, -5, -2, -2, 2, 2, 5, 5, -4, -4, 4, 4, -5, -5, -3, -3, 3, 3, 5, 5};
  static int[] disty_35 = {0, 0, -1, 1, 0, -1, 1, -1, 1, 0, -2, 2, 0, -1, 1, -2, 2, -2, 2, -1, 1, -2, 2, -2, 2, 0, -3, 3, 0, -1, 1, -3, 3, -3, 3, -1, 1, -2, 2, -3, 3, -3, 3, -2, 2, 0, -4, 4, 0, -1, 1, -4, 4, -4, 4, -1, 1, -3, 3, -3, 3, -2, 2, -4, 4, -4, 4, -2, 2, 0, -3, 3, -4, 4, -5, 5, -4, 4, -3, 3, 0, -1, 1, -5, 5, -5, 5, -1, 1, -2, 2, -5, 5, -5, 5, -2, 2, -4, 4, -4, 4, -3, 3, -5, 5, -5, 5, -3, 3};

  static int getLevel(int r) {
    int res = (int)Math.floor(Math.exp(0.0028 * r -1.38*Math.sin(0.00157*r-1.73)+1.38*Math.sin(-1.73)) - 1);
    System.out.println("Level: " + Integer.toString(res) + " Round: " + Integer.toString(r));
    return res;
  }

  static boolean willFlood(MapLocation loc) throws GameActionException{
    int elevation = rc.senseElevation(loc);
    return elevation <= getLevel(round + 1);
  }

  // -1 means didn't build
  // returns index in directions where robot was built
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

  static boolean tryBuild(RobotType type, Direction dir) throws GameActionException {
    if (rc.isReady() && rc.canBuildRobot(type, dir)) {
      rc.buildRobot(type, dir);
      return true;
    } else return false;
  }

  static int tryDigAway(MapLocation loc) throws GameActionException {
    if (!rc.isReady()) {
      return -1;
    }
    int max_dist = -1;
    int best_i = -1;
    for (int i = 0; i < directions.length; i++) {
      if (rc.canDigDirt(directions[i]) && Landscaper.cur_loc.add(directions[i]).distanceSquaredTo(loc) > max_dist) {
        max_dist = Landscaper.cur_loc.add(directions[i]).distanceSquaredTo(loc);
        best_i = i;
      }
    }

    if (best_i != -1) {
      rc.digDirt(directions[best_i]);
    }

    return best_i;
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
}