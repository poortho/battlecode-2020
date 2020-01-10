package bot4;

import battlecode.common.GameActionException;
import battlecode.common.RobotInfo;
import battlecode.common.RobotType;

import static bot4.Helper.tryBuild;
import static bot4.RobotPlayer.rc;

public class DesignSchool {

    static boolean near_hq = false;

    static void runDesignSchool() throws GameActionException {
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
                        break;
                }
            }
        }

        // build when (enemies nearby & soup high scaling on nearby landscapers) | soup high
        // build more if close to HQ

        if (num_enemy_buildings > 0 && rc.getTeamSoup() >= RobotType.LANDSCAPER.cost*(1+num_landscapers*(near_hq ? 0.5 : 1))) {
            if (num_landscapers >= 8) {
                // do nothing
            } else {
                Helper.tryBuild(RobotType.LANDSCAPER);
            }
        }

        if ((num_enemy_design > 0 && num_enemy_drones == 0 && rc.getTeamSoup() >= RobotType.LANDSCAPER.cost*(1+num_landscapers*(near_hq ? 0.5 : 1)))
            || rc.getTeamSoup() >= 500*(near_hq ? 0.5 : 1)) {
            if (near_hq && num_landscapers >= 8) {
                // do nothing
            } else {
                Helper.tryBuild(RobotType.LANDSCAPER);
            }
        }
    }
}
