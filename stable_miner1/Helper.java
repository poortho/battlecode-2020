package stable_miner1;
import battlecode.common.*;

import static stable_miner1.RobotPlayer.turnCount;
import static stable_miner1.RobotPlayer.rc;

public class Helper {

  static Direction[] directions = {Direction.NORTH, Direction.NORTHEAST, Direction.EAST, Direction.SOUTHEAST, Direction.SOUTH, Direction.SOUTHWEST, Direction.WEST, Direction.NORTHWEST};
  static int[] distx_35 = {0, -1, 0, 0, 1, -1, -1, 1, 1, -2, 0, 0, 2, -2, -2, -1, -1, 1, 1, 2, 2, -2, -2, 2, 2, -3, 0, 0, 3, -3, -3, -1, -1, 1, 1, 3, 3, -3, -3, -2, -2, 2, 2, 3, 3, -4, 0, 0, 4, -4, -4, -1, -1, 1, 1, 4, 4, -3, -3, 3, 3, -4, -4, -2, -2, 2, 2, 4, 4, -5, -4, -4, -3, -3, 0, 3, 3, 4, 4, -5, -5, -1, 1, -5, -5, -2, 2, -4, -4, 4, 4, -5, -5, -3, 3};
  static int[] disty_35 = {0, 0, -1, 1, 0, -1, 1, -1, 1, 0, -2, 2, 0, -1, 1, -2, 2, -2, 2, -1, 1, -2, 2, -2, 2, 0, -3, 3, 0, -1, 1, -3, 3, -3, 3, -1, 1, -2, 2, -3, 3, -3, 3, -2, 2, 0, -4, 4, 0, -1, 1, -4, 4, -4, 4, -1, 1, -3, 3, -3, 3, -2, 2, -4, 4, -4, 4, -2, 2, 0, -3, 3, -4, 4, -5, -4, 4, -3, 3, -1, 1, -5, -5, -2, 2, -5, -5, -4, 4, -4, 4, -3, 3, -5, -5};


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