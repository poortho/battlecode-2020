package bot2;
import battlecode.common.*;

import static bot2.RobotPlayer.rc;

public class NetGun {

    static MapLocation cur_loc;

    static void runNetGun() throws GameActionException {
        cur_loc = rc.getLocation();
        shootNetGun();
    }

    static void shootNetGun() throws GameActionException {
        RobotInfo[] robots = rc.senseNearbyRobots();
        for (int i = 0; i < robots.length; i++) {
            if (robots[i].team != rc.getTeam() && robots[i].type == RobotType.DELIVERY_DRONE &&
                    robots[i].location.distanceSquaredTo(cur_loc) < GameConstants.NET_GUN_SHOOT_RADIUS_SQUARED &&
                    rc.canShootUnit(robots[i].ID)) {
                // TODO: base on distance or something to units
                rc.shootUnit(robots[i].ID);
                break;
            }
        }        
    }
}
