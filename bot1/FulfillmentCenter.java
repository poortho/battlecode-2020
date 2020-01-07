package bot1;
import battlecode.common.*;

public class FulfillmentCenter {
	static RobotController rc;
  static void runFulfillmentCenter() throws GameActionException {
    for (Direction dir : Helper.directions)
      Helper.tryBuild(RobotType.DELIVERY_DRONE, dir);
  }
}
