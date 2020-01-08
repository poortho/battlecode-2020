package bot1;
import battlecode.common.*;

import static bot1.Helper.directions;
import static bot1.RobotPlayer.turnCount;
import static bot1.RobotPlayer.rc;

public class FulfillmentCenter {
  static void runFulfillmentCenter() throws GameActionException {
    for (Direction dir : Helper.directions)
      Helper.tryBuild(RobotType.DELIVERY_DRONE, dir);
  }
}
