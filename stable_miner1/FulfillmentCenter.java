package stable_miner1;
import battlecode.common.*;

import static stable_miner1.Helper.directions;
import static stable_miner1.RobotPlayer.turnCount;
import static stable_miner1.RobotPlayer.rc;

public class FulfillmentCenter {
  static void runFulfillmentCenter() throws GameActionException {
    for (Direction dir : Helper.directions)
      Helper.tryBuild(RobotType.DELIVERY_DRONE, dir);
  }
}
