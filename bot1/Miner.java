package bot1;
import battlecode.common.*;

import static bot1.Helper.directions;

public class Miner {

<<<<<<< HEAD
	static int[] distx_35 = {0, -1, 0, 0, 1, -1, -1, 1, 1, -2, 0, 0, 2, -2, -2, -1, -1, 1, 1, 2, 2, -2, -2, 2, 2, -3, 0, 0, 3, -3, -3, -1, -1, 1, 1, 3, 3, -3, -3, -2, -2, 2, 2, 3, 3, -4, 0, 0, 4, -4, -4, -1, -1, 1, 1, 4, 4, -3, -3, 3, 3, -4, -4, -2, -2, 2, 2, 4, 4, -5, -4, -4, -3, -3, 0, 3, 3, 4, 4, -5, -5, -1, 1, -5, -5, -2, 2, -4, -4, 4, 4, -5, -5, -3, 3};
	static int[] disty_35 = {0, 0, -1, 1, 0, -1, 1, -1, 1, 0, -2, 2, 0, -1, 1, -2, 2, -2, 2, -1, 1, -2, 2, -2, 2, 0, -3, 3, 0, -1, 1, -3, 3, -3, 3, -1, 1, -2, 2, -3, 3, -3, 3, -2, 2, 0, -4, 4, 0, -1, 1, -4, 4, -4, 4, -1, 1, -3, 3, -3, 3, -2, 2, -4, 4, -4, 4, -2, 2, 0, -3, 3, -4, 4, -5, -4, 4, -3, 3, -1, 1, -5, -5, -2, 2, -5, -5, -4, 4, -4, 4, -3, 3, -5, -5};

  static Direction[] directions = {Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST};
=======
>>>>>>> b4afad1315d1092a51caf63b132d380c476f1ea4
  static RobotType[] spawnedByMiner = {RobotType.REFINERY, RobotType.VAPORATOR, RobotType.DESIGN_SCHOOL, RobotType.FULFILLMENT_CENTER, RobotType.NET_GUN};
  static RobotController rc;
  static int turnCount;

	static void runMiner() throws GameActionException {

		// scan around surroundings for mine
		find_mine();

/*
		if (Helper.tryMove(Helper.randomDirection()))
			System.out.println("I moved!");
      // Helper.tryBuild(randomSpawnedByMiner(), Helper.randomDirection());
		for (Direction dir : directions)
			Helper.tryBuild(RobotType.FULFILLMENT_CENTER, dir);
		for (Direction dir : directions)
			if (tryDepositSoup(dir))
				System.out.println("I deposited soup! " + rc.getTeamSoup());
			for (Direction dir : directions)
				if (tryMine(dir))
					System.out.println("I mined soup! " + rc.getSoupCarrying());
					*/
	}

	static void find_mine() throws GameActionException {
		MapLocation cur_loc = rc.getLocation();
		for (int i = 0; i < distx_35.length; i++) {
			MapLocation next_loc = cur_loc.translate(distx_35[i], disty_35[i]);
			int count = rc.senseSoup(next_loc);
			if (count > 0) {
				System.out.println("Found mine at:" + next_loc.toString());
				return;
			}
		}
	}

	static void walk_to(MapDirection loc) {

	}

  static boolean tryMine(Direction dir) throws GameActionException {
    if (rc.isReady() && rc.canMineSoup(dir)) {
        rc.mineSoup(dir);
        return true;
    } else return false;
  }

  static boolean tryDepositSoup(Direction dir) throws GameActionException {
      if (rc.isReady() && rc.canDepositSoup(dir)) {
          rc.depositSoup(dir, rc.getSoupCarrying());
          return true;
      } else return false;
  }
}
