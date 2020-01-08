package stable_miner1;
import battlecode.common.*;

import static stable_miner1.Helper.directions;
import static stable_miner1.RobotPlayer.turnCount;
import static stable_miner1.RobotPlayer.rc;

public class DeliveryDrone {

  static void runDeliveryDrone() throws GameActionException {
    Team enemy = rc.getTeam().opponent();
    if (!rc.isCurrentlyHoldingUnit()) {
      // See if there are any enemy robots within striking range (distance 1 from lumberjack's radius)
      RobotInfo[] robots = rc.senseNearbyRobots(GameConstants.DELIVERY_DRONE_PICKUP_RADIUS_SQUARED, enemy);

      if (robots.length > 0) {
          // Pick up a first robot within range
          rc.pickUpUnit(robots[0].getID());
          System.out.println("I picked up " + robots[0].getID() + "!");
      }
    } else {
      // No close robots, so search for robots within sight radius
      Helper.tryMove(Helper.randomDirection());
    }
  }
}