package bot1;
import battlecode.common.*;

public class HQ {
	static RobotController rc;
	
  static void runHQ() throws GameActionException {
    for (Direction dir : Helper.directions)
      Helper.tryBuild(RobotType.MINER, dir);
  }
}
