package bot1;
import battlecode.common.*;

import static bot1.Helper.directions;
import static bot1.Helper.tryBuild;
import static bot1.RobotPlayer.turnCount;
import static bot1.RobotPlayer.rc;

public class DesignSchool {

    static void runDesignSchool() throws GameActionException {
        RobotInfo[] robots = rc.senseNearbyRobots();
        int num_enemy_buildings = 0;
        int num_landscapers = 0;
        for (int i = 0; i < robots.length; i++) {
            if (robots[i].team != rc.getTeam() && !robots[i].type.canBePickedUp()) {
                num_enemy_buildings++;
            }
            if (robots[i].team == rc.getTeam() && robots[i].type == RobotType.LANDSCAPER) {
                num_landscapers++;
            }
        }

        // build when (enemies nearby & soup high scaling on nearby landscapers) | soup high
        if ((num_enemy_buildings > 0 && rc.getTeamSoup() >= RobotType.LANDSCAPER.cost*(1+num_landscapers))
            || rc.getTeamSoup() >= 500) {
            tryBuild(RobotType.LANDSCAPER);
        }
    }
}
