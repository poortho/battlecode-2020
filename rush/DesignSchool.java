package rush;

import battlecode.common.GameActionException;
import battlecode.common.RobotInfo;
import battlecode.common.RobotType;
import battlecode.common.MapLocation;

import static rush.RobotPlayer.rc;
import static rush.Helper.distx_35;
import static rush.Helper.disty_35;

public class DesignSchool {

    static boolean near_hq = false;
    static boolean seen_enemy_drone = false;
    static boolean near_enemy_hq = false;
    static int seen_drone_timeout = 0;
    static MapLocation enemy_hq = null;
    static int mine_count;
    static int produce_attack = 0;
    static int total_produced = 0;
    static int produce_defense = 0;

    static void runDesignSchool() throws GameActionException {
        Comms.getBlocks();
        mine_count = count_mine();
        RobotInfo[] robots = rc.senseNearbyRobots();
        int num_enemy_buildings = 0;
        int num_landscapers = 0;
        int num_enemy_drones = 0;
        int num_enemy_fulfill = 0;
        int num_enemy_design = 0;
        int num_netguns = 0;
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
                    case NET_GUN:
                        num_netguns++;
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
                    case HQ:
                        near_enemy_hq = true;
                        enemy_hq = robots[i].location;
                        break;
                }
            }
        }

        seen_drone_timeout++;
        if (seen_drone_timeout == 50) {
            seen_drone_timeout = 0;
            seen_enemy_drone = false;
        }

        if (Miner.gay_rush_alert && Miner.all_in) {
            Miner.gay_rush_alert = false;
        }

        if (HQ.rushed && Miner.gay_rush_alert && near_hq && produce_defense < 6) {
            if (HQ.our_hq != null) {
                if (Helper.tryBuildToward(RobotType.LANDSCAPER, HQ.our_hq)) {
                    produce_defense++;
                    total_produced++;
                }
            } else {
                if (Helper.tryBuild(RobotType.LANDSCAPER) != -1) {
                    produce_defense++;
                    total_produced++;
                }
            }
            /*
            if (produce_defense == 2) {
                Comms.broadcast_all_in();
            }*/
        }

        // build when (enemies nearby & soup high scaling on nearby landscapers) | soup high
        // build more if close to HQ

        if (near_enemy_hq && ((!seen_enemy_drone && num_enemy_fulfill == 0)|| num_netguns > 0)
            && (!Miner.gay_rush_alert || (rc.getTeamSoup() > RobotType.DESIGN_SCHOOL.cost + RobotType.LANDSCAPER.cost) || Comms.design_school_idx > 0)) {
            Helper.tryBuildToward(RobotType.LANDSCAPER, enemy_hq);
            produce_attack++;
        }

        if (mine_count > 200 && num_enemy_buildings > 0 && !seen_enemy_drone && num_enemy_fulfill == 0 &&
                num_landscapers == 0 && rc.getTeamSoup() >= RobotType.LANDSCAPER.cost*(1+num_landscapers*(near_hq ? 0.5 : 1))) {
            if (Helper.tryBuild(RobotType.LANDSCAPER) != -1) {
                total_produced++;
            }

        }

        if (rc.getTeamSoup() >= 700 || (!HQ.done_turtling && rc.getRoundNum() > 400 && HQ.our_hq != null && rc.getLocation().distanceSquaredTo(HQ.our_hq) < 100 && total_produced < 20)) {
            if (Helper.tryBuildToward(RobotType.LANDSCAPER, HQ.our_hq)) {
                total_produced++;
            }
        }

        if (total_produced >= 20 && !HQ.done_turtling) {
            Comms.broadcast_done_turtle();
        }

        /*if ((num_enemy_design > 0 && num_enemy_drones == 0 && rc.getTeamSoup() >= RobotType.LANDSCAPER.cost*(1+num_landscapers*(near_hq ? 0.5 : 1)))
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
        }*/
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
