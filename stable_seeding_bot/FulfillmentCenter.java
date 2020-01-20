package stable_seeding_bot;

import battlecode.common.GameActionException;
import battlecode.common.RobotInfo;
import battlecode.common.RobotType;
import battlecode.common.MapLocation;

import static stable_seeding_bot.RobotPlayer.rc;
import static stable_seeding_bot.RobotPlayer.round;

public class FulfillmentCenter {

    static boolean near_hq = false;
    static int drones_produced = 0;
    static MapLocation cur_loc;

    static void runFulfillmentCenter() throws GameActionException {
        Comms.getBlocks();
        cur_loc = rc.getLocation();
        RobotInfo[] robots = rc.senseNearbyRobots();
        int num_enemy_units = 0;
        int num_drones = 0;
        int enemy_hq = 0;
        int enemy_net_gun = 0;
        int num_cows = 0;
        for (int i = 0; i < robots.length; i++) {
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
                    case COW:
                        num_cows++;
                        break;
                    case LANDSCAPER:
                    case MINER:
                        num_enemy_units++;
                        break;
                }
            }
        }

        if (HQ.rushed && Miner.gay_rush_alert && drones_produced < num_enemy_units) {
            if (HQ.our_hq != null) {
                Helper.tryBuildToward(RobotType.DELIVERY_DRONE, HQ.our_hq);
            } else {
                Helper.tryBuild(RobotType.DELIVERY_DRONE);
            }
            drones_produced++;
        }

        // scale threshold based on number of drones nearby
        if (enemy_hq == 0 && enemy_net_gun == 0) {
            if ((num_enemy_units > 0 && num_drones / 2 < num_enemy_units && rc.getTeamSoup() >= RobotType.DELIVERY_DRONE.cost*(1+num_drones))
                ||  (rc.getTeamSoup() >= 2100 && HQ.our_hq != null && cur_loc.distanceSquaredTo(HQ.our_hq) <= 40) ||
                    (rc.getTeamSoup() >= 500 && (num_enemy_units > 0 || num_cows > 0))) {
                    //(near_hq && (num_drones < num_enemy_units) && rc.getTeamSoup() >= RobotType.DELIVERY_DRONE.cost) ||
                    //(rc.getTeamSoup() >= 450 && near_hq) ||
                Helper.tryBuild(RobotType.DELIVERY_DRONE);
                drones_produced++;
            }
        }
    }
}
