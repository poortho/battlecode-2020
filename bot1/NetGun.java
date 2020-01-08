package bot1;
import battlecode.common.*;

import static bot1.Helper.directions;
import static bot1.RobotPlayer.turnCount;
import static bot1.RobotPlayer.rc;

public class NetGun {
    static void runNetGun() throws GameActionException {
        MapLocation cur_loc = rc.getLocation();
        RobotInfo[] robots = rc.senseNearbyRobots();
        for (int i = 0; i < robots.length; i++) {
            if (robots[i].team != rc.getTeam() && robots[i].type == RobotType.DELIVERY_DRONE &&
                    robots[i].location.distanceSquaredTo(cur_loc) < GameConstants.NET_GUN_SHOOT_RADIUS_SQUARED &&
                    rc.canShootUnit(robots[i].ID)) {
                // TODO: base on distance or something to units
                //rc.shootUnit(robots[i].ID);
                break;
            }
        }
    }
}
