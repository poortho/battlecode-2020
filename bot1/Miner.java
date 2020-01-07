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

  static int turnCount;
  static MapLocation cur_loc;
  static MapLocation target_mine = null;
  static MapLocation target_explore = null;
  static MapLocation hq = null;
  static int target_idx = -1;
  static int drone_factories_built = 0;

  // patches already explored by other miners
  static MapLocation[] explored = new MapLocation[20];
  static int explored_count = 0;
  static boolean check_new_patch = false;
  static boolean must_reach_dest = false;
  static boolean first_target = false;

	static void runMiner() throws GameActionException {
		cur_loc = rc.getLocation();

		if (hq == null) {
			hq = find_hq();
		}

		Comms.getBlocks();

		// build fulfillment center...
		if (rc.getTeamSoup() >= 150) {
			RobotInfo[] robots = rc.senseNearbyRobots();
			boolean nearby_fulfillment = false;
			int num_enemies = 0;
			for (int i = 0; i < robots.length; i++) {
				if (robots[i].team != rc.getTeam()) {
					num_enemies++;
				}
				if (robots[i].team == rc.getTeam() && robots[i].type == RobotType.FULFILLMENT_CENTER) {
					nearby_fulfillment = true;
				}
			}
			// build if none nearby and (nearby enemies or close to hq)
			if (!nearby_fulfillment) {
				if (num_enemies != 0 || rc.getLocation().distanceSquaredTo(hq) < 35) {
					int res = -1;
					if ((res = Helper.tryBuild(RobotType.FULFILLMENT_CENTER)) != -1) {
						drone_factories_built++;
					}
				}
			}
		}

		if (target_explore != null && must_reach_dest) {
			greedy_walk(target_explore);
			find_mine();

			if (cur_loc.distanceSquaredTo(target_explore) <= 10) {//rc.canSenseLocation(target_explore)) {
				// broadcast "i explored this location"
				target_explore = null;
				must_reach_dest = false;

				if (target_mine == null) {
					target_explore = get_explore_target();
				}
			}
			return;
		} else {
			if (target_mine == null){
				find_mine();
			}
			if (target_mine != null) {
				do_mine();
			} else {
				target_explore = get_explore_target();
				if (target_explore != null)
					greedy_walk(target_explore);
				else
					System.out.println("Nothing to do");
			}
		}

/*
		// get target to explore
		if (target_explore == null && target_mine == null) {
			target_explore = get_explore_target();
			System.out.println("New target: " + target_explore.toString());
			if (target_explore == null) {
				System.out.println("Done exploring");
				return;
			}
		}

		if (target_mine == null && !must_reach_dest) {
			target_mine = find_mine();
			if (target_mine == null) {
				//System.out.println("PENIS NO MINES");
			} else {
				check_new_patch = true;
			}
		}

		// build fulfillment center...
		if (rc.getTeamSoup() >= 150) {
			RobotInfo[] robots = rc.senseNearbyRobots();
			boolean nearby_fulfillment = false;
			int num_enemies = 0;
			for (int i = 0; i < robots.length; i++) {
				if (robots[i].team != rc.getTeam()) {
					num_enemies++;
				}
				if (robots[i].team == rc.getTeam() && robots[i].type == RobotType.FULFILLMENT_CENTER) {
					nearby_fulfillment = true;
				}
			}
			// build if none nearby and (nearby enemies or close to hq)
			if (!nearby_fulfillment) {
				if (num_enemies != 0 || rc.getLocation().distanceSquaredTo(hq) < 35) {
					int res = -1;
					if ((res = Helper.tryBuild(RobotType.FULFILLMENT_CENTER)) != -1) {
						drone_factories_built++;
					}
				}
			}
		}

		// scan around surroundings for mines
		if (target_mine != null && !must_reach_dest) {
			do_mine();
		} else if (target_explore != null) {
			// if i'm at destination
			if (cur_loc.distanceSquaredTo(target_explore) <= 10) {//rc.canSenseLocation(target_explore)) {
				// broadcast "i explored this location"
				target_mine = find_mine();
				target_explore = null;
				must_reach_dest = false;
				//target_explore = get_explore_target();
			}

			if (target_mine != null) {
				do_mine();
			} else if (target_explore == null) {
				target_explore = get_explore_target();
				if (target_explore != null)
					greedy_walk(target_explore);
			}
		} else {
			System.out.println("WTF NO WHERE TO GO");
		}*/
	}

	static void do_mine() throws GameActionException {
		// try mining it lol
		if (rc.getSoupCarrying() == RobotType.MINER.soupLimit) {
			if (cur_loc.distanceSquaredTo(hq) <= 2) {
				// deposit
				tryDepositSoup(cur_loc.directionTo(hq));
			} else {
				// heuristic for building refinery
				int distance = target_mine.distanceSquaredTo(hq);
				int distance2 = target_mine.distanceSquaredTo(cur_loc);
				int res = -1;
				if (distance > 80 && distance2 < 24 && (res = Helper.tryBuild(RobotType.REFINERY)) != -1) {
					// build refinery
					hq = cur_loc.add(directions[res]);
					System.out.println("New HQ: " + hq.toString());
				} else {
					System.out.println("Walking Back To HQ");
					greedy_walk(hq);
				}
			}
		} else {
			if (cur_loc.distanceSquaredTo(target_mine) <= 2) {
				// mine lmao
				boolean result = tryMine(cur_loc.directionTo(target_mine));
				int count = rc.senseSoup(target_mine);
				if (count == 0) {
					target_mine = null;
					check_new_patch = false;
				}

				if (check_new_patch) {
					broadcast_patch();
					check_new_patch = false;
				}
			} else {
				//walk to mine
				//System.out.println("Walking to " + target_mine);
				greedy_walk(target_mine);
			}
		}
	}

	static void broadcast_patch() throws GameActionException {
		// 0x00000000
		//          0 <- broadcast patch
		//         N  <- number of workers needed
		//       XY   <- patch location / 4

		// make sure this patch wasn't already broadcasted
		System.out.println("broadcast patch: " + target_mine.toString());
		for (int c = 0; c < explored_count; c++) {
			if (target_mine.distanceSquaredTo(explored[c]) <= 45) {
				return;
			}
		}

		int num = get_num_workers_needed();
		Comms.broadcast_miner_request(target_mine, num, true);
		System.out.println("Found patch at: " + target_mine);
		System.out.println("Workers needed: " + Integer.toString(num));
	}

	static int get_num_workers_needed() throws GameActionException {
		int total_soup = 0;
		for (int i = 0; i < distx_35.length; i++) {
			MapLocation next_loc = cur_loc.translate(distx_35[i], disty_35[i]);
			if (rc.canSenseLocation(next_loc)) {
				int count = rc.senseSoup(next_loc);
				total_soup += count;
			}
		}
		return total_soup / 600;
	}

	// get next target coordinate to explore to
	// this gets called when miner is created and there are no patches around miner, or when miner reaches current target and needs a new target
	static MapLocation get_explore_target() throws GameActionException {
		must_reach_dest = (Comms.miner_queue_num[Comms.poll_idx] & (1 << 16)) == 1;
		return Comms.miner_queue_peek();
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
			if (rc.canSenseLocation(next_loc)) {
				int count = rc.senseSoup(next_loc);
				if (count > 0) {
					System.out.println("Found mine at:" + next_loc.toString());
					check_new_patch = true;
					target_mine = next_loc;
					if (cur_loc.distanceSquaredTo(target_mine) <= 2) {
						broadcast_patch();
					}
					return next_loc;
					/*
					if (rc.isReady()) {
						greedy_walk(next_loc);
					}
					return;*/
				}
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
