package bot1;
import battlecode.common.*;

public class Helper {

  static Direction[] directions = {Direction.NORTH, Direction.NORTHEAST, Direction.EAST, Direction.SOUTHEAST,
          Direction.SOUTH, Direction.SOUTHWEST, Direction.WEST, Direction.NORTHWEST};
  static RobotController rc;

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