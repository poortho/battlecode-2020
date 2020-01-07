package bot1;
import battlecode.common.*;

import static bot1.Helper.directions;
import static bot1.Helper.tryBuild;
import static bot1.RobotPlayer.turnCount;
import static bot1.RobotPlayer.rc;

public class FulfillmentCenter {
    static void runFulfillmentCenter() throws GameActionException {
        RobotInfo[] robots = rc.senseNearbyRobots();
        int num_enemies = 0;
        int num_drones = 0;
        for (int i = 0; i < robots.length; i++) {
            if (robots[i].team != rc.getTeam()) {
                num_enemies++;
            }
            if (robots[i].team == rc.getTeam() && robots[i].type == RobotType.DELIVERY_DRONE) {
                num_drones++;
            }
        }

        // scale threshold based on number of drones nearby
        if (num_enemies > 0 && rc.getTeamSoup() >= 150*(1+num_drones)) {
            tryBuild(RobotType.DELIVERY_DRONE);
        }
    }
}
