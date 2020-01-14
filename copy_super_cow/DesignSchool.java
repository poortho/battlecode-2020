package copy_super_cow;

import battlecode.common.GameActionException;
import battlecode.common.RobotInfo;
import battlecode.common.RobotType;
import battlecode.common.MapLocation;

import static copy_super_cow.RobotPlayer.rc;
import static copy_super_cow.Helper.distx_35;
import static copy_super_cow.Helper.disty_35;

public class DesignSchool {

    static boolean near_hq = false;
    static boolean seen_enemy_drone = false;
    static int seen_drone_timeout = 0;
    static int mine_count;

    static void runDesignSchool() throws GameActionException {
        /*
        mine_count = count_mine();
        RobotInfo[] robots = rc.senseNearbyRobots();
        int num_enemy_buildings = 0;
        int num_landscapers = 0;
        int num_enemy_drones = 0;
        int num_enemy_fulfill = 0;
        int num_enemy_design = 0;
        for (int i = 0; i < robots.length; i++) {
            if (robots[i].team != rc.getTeam() && robots[i].type.isBuilding()) {
                num_enemy_buildings++;
            }
            if (robots[i].team == rc.getTeam()) {
                switch(robots[i].type) {
                    case LANDSCAPER:
                        num_landscapers++;
                        break;
                    case HQ:
                        near_hq = true;
                        break;
                }
            } else {
                switch(robots[i].type) {
                    case FULFILLMENT_CENTER:
                        num_enemy_fulfill++;
                        break;
                    case DESIGN_SCHOOL:
                        num_enemy_design++;
                        break;
                    case DELIVERY_DRONE:
                        num_enemy_drones++;
                        seen_enemy_drone = true;
                        seen_drone_timeout = 0;
                        break;
                }
            }
        }

        seen_drone_timeout++;
        if (seen_drone_timeout == 50) {
            seen_drone_timeout = 0;
            seen_enemy_drone = false;
        }

        // build when (enemies nearby & soup high scaling on nearby landscapers) | soup high
        // build more if close to HQ

        if (mine_count > 200 && num_enemy_buildings > 0 && !seen_enemy_drone && num_enemy_fulfill == 0 && num_landscapers == 0 && rc.getTeamSoup() >= RobotType.LANDSCAPER.cost*(1+num_landscapers*(near_hq ? 0.5 : 1))) {
            Helper.tryBuild(RobotType.LANDSCAPER);
        }

        if (rc.getTeamSoup() >= 1000) {
            Helper.tryBuild(RobotType.LANDSCAPER);
        }

        if ((num_enemy_design > 0 && num_enemy_drones == 0 && rc.getTeamSoup() >= RobotType.LANDSCAPER.cost*(1+num_landscapers*(near_hq ? 0.5 : 1)))
            || rc.getTeamSoup() >= 500*(near_hq ? 0.5 : 1)) {
            if (near_hq && num_landscapers >= 8) {
                // do nothing
            } else {
                if (near_hq && HQ.our_hq != null) {
                    Helper.tryBuildToward(RobotType.LANDSCAPER, HQ.our_hq);
                } else {
                    Helper.tryBuild(RobotType.LANDSCAPER);
                }
            }
        }
*/
    }

    static int count_mine() throws GameActionException {
        MapLocation cur_loc = rc.getLocation();
        int total_soup = 0;
        for (int i = 0; i < distx_35.length; i++) {
            MapLocation next_loc = cur_loc.translate(distx_35[i], disty_35[i]);
            if (rc.canSenseLocation(next_loc)) {
                int count = rc.senseSoup(next_loc);
                total_soup += count;
            }
        }
        return total_soup;
    }
}
