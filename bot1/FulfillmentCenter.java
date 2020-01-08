package bot1;
import battlecode.common.*;

import static bot1.Helper.directions;
import static bot1.Helper.tryBuild;
import static bot1.RobotPlayer.turnCount;
import static bot1.RobotPlayer.rc;

public class FulfillmentCenter {
    static void runFulfillmentCenter() throws GameActionException {
        RobotInfo[] robots = rc.senseNearbyRobots();
        int num_enemy_units = 0;
        int num_drones = 0;
        for (int i = 0; i < robots.length; i++) {
            if (robots[i].team != rc.getTeam() && robots[i].type.canBePickedUp()) {
                num_enemy_units++;
            }
            if (robots[i].team == rc.getTeam() && robots[i].type == RobotType.DELIVERY_DRONE) {
                num_drones++;
            }
        }

        // scale threshold based on number of drones nearby
        if (num_enemy_units > 0 && rc.getTeamSoup() >= RobotType.DELIVERY_DRONE.cost*(1+num_drones)) {
            tryBuild(RobotType.DELIVERY_DRONE);
        }
    }
}
