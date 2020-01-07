package bot1;
import battlecode.common.*;

import static bot1.Helper.directions;
import static bot1.RobotPlayer.turnCount;
import static bot1.RobotPlayer.round;
import static bot1.RobotPlayer.rc;
import static bot1.RobotPlayer.waterLevel;
import static bot1.Helper.distx_35;
import static bot1.Helper.disty_35;

public class Miner {


  static RobotType[] spawnedByMiner = {RobotType.REFINERY, RobotType.VAPORATOR, RobotType.DESIGN_SCHOOL, RobotType.FULFILLMENT_CENTER, RobotType.NET_GUN};
  static int turnCount;
  static MapLocation cur_loc;
  static MapLocation target_mine = null;
  static MapLocation target_explore = null;
  static MapLocation hq = null;
  static int target_idx = -1;

	static void runMiner() throws GameActionException {
		cur_loc = rc.getLocation();

		if (hq == null) {
			hq = find_hq();
		}

		if (target_mine == null) {
			target_mine = find_mine();
			if (target_mine == null) {
				System.out.println("PENIS NO MINES");
			}
		}

		Comms.getBlocks();

		if (target_explore == null) {
			if (round == 3) {
				target_explore = Comms.explore[0];
				target_idx = 0;
				System.out.println("Target: " + target_explore.toString());
			} else if (round == 4) {
				target_explore = Comms.explore[1];
				target_idx = 1;
				System.out.println("Target: " + target_explore.toString());
			}
		}

		// scan around surroundings for mines
		if (target_mine != null) {
			// try mining it lol
			if (rc.getSoupCarrying() == RobotType.MINER.soupLimit) {
				if (cur_loc.distanceSquaredTo(hq) <= 2) {
					// deposit
					tryDepositSoup(cur_loc.directionTo(hq));
				} else {
					System.out.println("Walking Back To HQ");
					greedy_walk(hq);
				}
			} else {
				if (cur_loc.distanceSquaredTo(target_mine) <= 2) {
					// mine lmao
					boolean result = tryMine(cur_loc.directionTo(target_mine));
					int count = rc.senseSoup(target_mine);
					if (count == 0) {
						target_mine = null;
					}
				} else {
					//walk to mine
					System.out.println("Walking to " + target_mine);
					greedy_walk(target_mine);
				}
			}
		} else {
			// if i'm at destination
			if (rc.canSenseLocation(target_explore)) {
				if (target_idx == 0) {
					target_idx = 1;
				}
				target_idx++;
				if (target_idx < 6) {
					target_explore = Comms.explore[target_idx];
				}
			}

			// explore
			greedy_walk(target_explore);
		}

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

	static void mine_around_me() throws GameActionException {

	}

	static MapLocation find_hq() throws GameActionException {
		for (int i = 0; i < 9; i++) {
			MapLocation next_loc = cur_loc.translate(distx_35[i], disty_35[i]);
			RobotInfo temp = rc.senseRobotAtLocation(next_loc);
			if (temp != null && temp.type == RobotType.HQ) {
				System.out.println("FOUND HQ");
				return next_loc;
			}
		}
		System.out.println("SHOULDNT HAPPEN EVER");
		return null;
	}

	static MapLocation find_mine() throws GameActionException {
		for (int i = 0; i < distx_35.length; i++) {
			MapLocation next_loc = cur_loc.translate(distx_35[i], disty_35[i]);
			int count = rc.senseSoup(next_loc);
			if (count > 0) {
				System.out.println("Found mine at:" + next_loc.toString());
				return next_loc;
				/*
				if (rc.isReady()) {
					greedy_walk(next_loc);
				}
				return;*/
			}
		}
		return null;
	}

	static void greedy_walk(MapLocation loc) throws GameActionException {
		int least_dist = 9999999;
		int next = -1;
		for (int i = 0; i < directions.length; i++) {
			MapLocation next_loc = cur_loc.add(directions[i]);
			int temp_dist = next_loc.distanceSquaredTo(loc);
			if (temp_dist < least_dist && rc.canMove(directions[i]) && !rc.senseFlooding(next_loc)) {
				least_dist = temp_dist;
				next = i;
			}
		}

		if (next != -1)
			rc.move(directions[next]);
	}

  static boolean tryMine(Direction dir) throws GameActionException {
    if (rc.isReady() && rc.canMineSoup(dir)) {
        rc.mineSoup(dir);
        System.out.println("MINED!");
        return true;
    } else return false;
  }

  static boolean tryDepositSoup(Direction dir) throws GameActionException {
      if (rc.isReady() && rc.canDepositSoup(dir)) {
          rc.depositSoup(dir, rc.getSoupCarrying());
          System.out.println("DEPOSITED");
          return true;
      } else return false;
  }
}
