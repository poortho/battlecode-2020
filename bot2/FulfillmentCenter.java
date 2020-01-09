package bot2;

import battlecode.common.GameActionException;
import battlecode.common.RobotInfo;
import battlecode.common.RobotType;

import static bot2.Helper.tryBuild;
import static bot2.RobotPlayer.rc;

public class FulfillmentCenter {

    static boolean near_hq = false;

    static void runFulfillmentCenter() throws GameActionException {
        RobotInfo[] robots = rc.senseNearbyRobots();
        int num_enemy_units = 0;
        int num_drones = 0;
        int enemy_hq = 0;
        int enemy_net_gun = 0;
        for (int i = 0; i < robots.length; i++) {
            // if (robots[i].team != rc.getTeam() && robots[i].type.canBePickedUp()) {
            //     num_enemy_units++;
            // }
            if (robots[i].team == rc.getTeam()) {
                switch(robots[i].type) {
                    case DELIVERY_DRONE:
                        num_drones++;
                        break;
                    case HQ:
                        near_hq = true;
                        break;
                }
            } else {
                switch(robots[i].type) {
                    case HQ:
                        enemy_hq++;
                        break;
                    case NET_GUN:
                        enemy_net_gun++;
                        break;
                    case LANDSCAPER:
                        num_enemy_units++;
                        break;
                    case MINER:
                        num_enemy_units++;
                        break;
                }
            }
        }

        // scale threshold based on number of drones nearby
        if (num_enemy_units > 0 && enemy_hq == 0 && enemy_net_gun == 0 && rc.getTeamSoup() >= RobotType.DELIVERY_DRONE.cost*(1+num_drones*(near_hq ? 0.5 : 1))) {
            tryBuild(RobotType.DELIVERY_DRONE);
        }
    }
}
