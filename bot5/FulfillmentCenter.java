package bot5;

import battlecode.common.GameActionException;
import battlecode.common.RobotInfo;
import battlecode.common.RobotType;
import battlecode.common.MapLocation;

import static bot5.RobotPlayer.rc;
import static bot5.RobotPlayer.round;

public class FulfillmentCenter {

    static boolean near_hq = false;
    static int drones_produced = 0;
    static MapLocation cur_loc;

    static void runFulfillmentCenter() throws GameActionException {
        cur_loc = rc.getLocation();
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
        if (enemy_hq == 0 && enemy_net_gun == 0) {
            if ((num_enemy_units > 0 && rc.getTeamSoup() >= RobotType.DELIVERY_DRONE.cost*(1+num_drones)) ||
                    (rc.getTeamSoup() >= 450 && near_hq) ||
                    (Helper.getLevel(round) + 1 == rc.senseElevation(cur_loc) && near_hq)) {
                Helper.tryBuild(RobotType.DELIVERY_DRONE);
                drones_produced++;
            }
        }
    }
}
