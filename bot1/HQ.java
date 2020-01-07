package bot1;
import battlecode.common.*;

import static bot1.Helper.directions;
import static bot1.RobotPlayer.turnCount;
import static bot1.RobotPlayer.rc;

public class HQ {
  static void runHQ() throws GameActionException {
    for (Direction dir : Helper.directions)
      Helper.tryBuild(RobotType.MINER, dir);
  }
}
