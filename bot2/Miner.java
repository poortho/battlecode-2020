package bot2;

import battlecode.common.*;

import static bot2.Helper.*;
import static bot2.RobotPlayer.rc;

public class Miner {

  static int turnCount;
  static MapLocation cur_loc;
  static MapLocation target_mine = null;
  static MapLocation target_explore = null;
  static MapLocation hq = null;
  static int target_idx = -1;
  static int drone_factories_built = 0;

  // patches already explored by other miners
  static MapLocation previous_location;
  static MapLocation[] explored = new MapLocation[20];
  static int explored_count = 0;
  static boolean check_new_patch = false;
  static boolean must_reach_dest = false;
  static boolean first_target = false;
  static RobotInfo[] robots;
  static boolean[] blacklist = new boolean[directions.length];
  static boolean in_danger;
  static int num_enemy_drones = 0;
  static int num_enemy_landscapers = 0;
  static int num_enemy_buildings = 0;
  static int num_enemies = 0;
  static boolean nearby_fulfillment = false;
  static boolean nearby_netgun = false;
  static boolean nearby_design = false;
  static boolean near_hq = false;

  static boolean turtling = false;
  static boolean bugpath_blocked = false;

	static void runMiner() throws GameActionException {
		cur_loc = rc.getLocation();
		in_danger = false;

		if (hq == null) {
			hq = find_hq();
		}

		for (int i = 0; i < blacklist.length; i++) {
			blacklist[i] = false;
		}
		Comms.getBlocks();
		robots = rc.senseNearbyRobots();

		sense();

		// move away from hq if turtling
		if (turtling && cur_loc.distanceSquaredTo(HQ.our_hq) <= 2) {
			Helper.greedy_move_away(HQ.our_hq, cur_loc);
			return;
		}

		if (in_danger) {
			// move in a direction such that you are not in danger
			// TODO change so that it moves towards destination
			for (int i = 0; i < directions.length; i++) {
				if (!blacklist[i] && rc.canMove(directions[i]) && !rc.senseFlooding(cur_loc.add(directions[i]))) {
					Helper.tryMove(directions[i]);
				}
			}
		}


		RobotType toBuild = calcBuilding();
		// if drones, build netgun
		// if landscapers, build drone
		// if buildings, build landscape

		// build thing
		if (toBuild != null && ((rc.getTeamSoup() >= toBuild.cost && num_enemies != 0) ||
				rc.getTeamSoup() >= toBuild.cost*(near_hq ? 1 : 4))) {
			// build if none nearby and (nearby enemies or close to hq)
			if (cur_loc.distanceSquaredTo(hq) <= 8) {
				for (int i = 0; i < directions.length; i++) {
					if (cur_loc.add(directions[i]).distanceSquaredTo(hq) < GameConstants.NET_GUN_SHOOT_RADIUS_SQUARED &&
							cur_loc.add(directions[i]).distanceSquaredTo(hq) > 3) {
						Helper.tryBuild(toBuild, directions[i]);
					}
				}
			}
		}

		if (target_explore != null && must_reach_dest) {
			// it's flooded!
			if (rc.canSenseLocation(target_explore) && rc.senseFlooding(target_explore)) {
				target_explore = null;
				must_reach_dest = false;
				if (target_mine == null) {
					target_explore = get_explore_target();
				}
			} else {
				miner_walk(target_explore);
				find_mine();
			}

			if (target_explore != null && cur_loc.distanceSquaredTo(target_explore) <= 10) {//rc.canSenseLocation(target_explore)) {
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
				if (target_explore != null) {
					miner_walk(target_explore);
				} else {
					System.out.println("Walk to enemy HQ");
					if (HQ.enemy_hq != null) {
						miner_walk(HQ.enemy_hq);
					}
				}
			}
		}
	}

	static void sense() throws GameActionException {
		int hq_dist = cur_loc.distanceSquaredTo(hq);
		num_enemy_drones = 0;
		num_enemy_landscapers = 0;
		num_enemy_buildings = 0;
		num_enemies = 0;
		nearby_fulfillment = false;
		nearby_netgun = false;
		nearby_design = false;
		for (int i = 0; i < robots.length; i++) {
			int temp_dist = robots[i].location.distanceSquaredTo(cur_loc);
			if (robots[i].type == RobotType.REFINERY && robots[i].team == rc.getTeam() && (temp_dist < hq_dist || turtling)) {
				hq = robots[i].location;
				hq_dist = temp_dist;
			} else if (HQ.enemy_hq == null && robots[i].type == RobotType.HQ && robots[i].team != rc.getTeam()) {
				// found enemy hq broadcast it
				System.out.println("Found enemy hq! " + robots[i].location);
				Comms.broadcast_enemy_hq(robots[i].location);
			} else if (robots[i].type == RobotType.HQ && robots[i].team == rc.getTeam()) {
				near_hq = true;
			}

			// calculations for building stuff
			if (robots[i].team != rc.getTeam()) {
				num_enemies++;
				switch (robots[i].type) {
					case DELIVERY_DRONE:
						if (cur_loc.distanceSquaredTo(robots[i].getLocation()) <= GameConstants.DELIVERY_DRONE_PICKUP_RADIUS_SQUARED) {
							in_danger = true;
						}
						for (int j = 0; j < directions.length; j++) {
							if (cur_loc.add(directions[j]).distanceSquaredTo(robots[i].getLocation())
									<= GameConstants.DELIVERY_DRONE_PICKUP_RADIUS_SQUARED) {
								blacklist[j] = true;
							}
						}
						num_enemy_drones++;
						break;
					case LANDSCAPER:
						num_enemy_landscapers++;
						break;
					case HQ:
					case NET_GUN:
					case REFINERY:
					case VAPORATOR:
					case DESIGN_SCHOOL:
					case FULFILLMENT_CENTER:
						num_enemy_buildings++;
						break;
				}
			}
			if (robots[i].team == rc.getTeam()) {
				switch (robots[i].type) {
					case FULFILLMENT_CENTER:
						nearby_fulfillment = true;
						break;
					case DESIGN_SCHOOL:
						nearby_design = true;
						break;
					case NET_GUN:
						nearby_netgun = true;
						break;
					case HQ:
						if (turtling) {
							for (int a = 0; a < Helper.directions.length; a++) {
								MapLocation temp_loc = robots[i].location.add(Helper.directions[a]);
								if (cur_loc.distanceSquaredTo(temp_loc) <= 2) {
									Direction d = cur_loc.directionTo(temp_loc);
									for (int j = 0; j < Helper.directions.length; j++) {
										if (Helper.directions[j] == d) {
											blacklist[j] = true;
											break;
										}
									}
								}
							}
						}
						break;
				}
			}
		}
	}

	static void do_mine() throws GameActionException {
		if (rc.canSenseLocation(target_mine) && rc.senseFlooding(target_mine)) {
			target_mine = find_mine();
			if (target_mine == null) {
				return;
			}
		}
		// try mining it lol
		if (rc.getSoupCarrying() == RobotType.MINER.soupLimit) {
			// check if HQ has landscapers around it
			if (!turtling && HQ.our_hq != null && hq.equals(HQ.our_hq) && rc.canSenseLocation(hq)) {
				for (int i = 0; i < Helper.directions.length; i++) {
					MapLocation temp_loc = hq.add(Helper.directions[i]);
					if (rc.canSenseLocation(temp_loc)) {
						RobotInfo rob = rc.senseRobotAtLocation(temp_loc);
						if (rob != null && rob.type == RobotType.LANDSCAPER) {
							turtling = true;
							break;
						}
					}
				}
			}

			if (turtling && hq.equals(HQ.our_hq)) {
				if (cur_loc.distanceSquaredTo(target_mine) <= 5 && HQ.our_hq.equals(hq)) {
					// try build refinery
					int res = Helper.tryBuild(RobotType.REFINERY);
					if (res != -1) {
						hq = cur_loc.add(directions[res]);
						return;
					}
				} else {
					miner_walk(target_mine);
				}
				return;
			}

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
				} else if (distance < 800) {
					System.out.println("Walking Back To HQ");
					miner_walk(hq);
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
				miner_walk(target_mine);
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
		if (total_soup > 900) {
			return 1;
		} else if (total_soup > 2500) {
			return 2;
		} else if (total_soup > 5000) {
			return 3;
		} else {
			return 0;
		}
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
			if (temp != null && temp.type == RobotType.HQ && temp.team == rc.getTeam()) {
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
			if (rc.canSenseLocation(next_loc) && !rc.senseFlooding(next_loc)) {
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
						miner_walk(next_loc);
					}
					return;*/
				}
			}
		}
		return null;
	}

	static void bugpath_walk(MapLocation loc) throws GameActionException {
		Direction greedy;

		int least_dist = 9999999;
		int next = -1;
		for (int i = 0; i < directions.length; i++) {
			MapLocation next_loc = cur_loc.add(directions[i]);
			int temp_dist = next_loc.distanceSquaredTo(loc);
			if (temp_dist < least_dist && !next_loc.equals(previous_location)) {
				least_dist = temp_dist;
				next = i;
			}
		}

		greedy = directions[next];
		MapLocation greedy_loc = cur_loc.add(greedy);

		if (!bugpath_blocked && rc.canMove(greedy) && !rc.senseFlooding(greedy_loc) && !blacklist[next] && !Helper.willFlood(greedy_loc)) {
			rc.move(greedy);
		} else {
			if (bugpath_blocked) {
				Direction start_dir = cur_loc.directionTo(previous_location);
				for (int i = 0; i < Helper.directions.length; i++) {
					if (Helper.directions[i] == start_dir) {
						next = i;
						break;
					}
				}
			}
			bugpath_blocked = true;
			for (int i = 0; i < 7; i++) {
				next = (next + 1) % directions.length;
				Direction cw = directions[next];
				MapLocation next_loc = cur_loc.add(cw);
				if (rc.canMove(cw) && !rc.senseFlooding(next_loc) && !blacklist[next] && !Helper.willFlood(next_loc)) {
					if (next_loc.distanceSquaredTo(loc) < cur_loc.distanceSquaredTo(loc)) {
						bugpath_blocked = false;
					}
					rc.move(cw);
					previous_location = cur_loc;
					break;
				}
			}	
		}
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

	static void miner_walk(MapLocation loc) throws GameActionException {
		bugpath_walk(loc);
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

  static RobotType calcBuilding() {
	  if (num_enemy_landscapers >= num_enemy_drones && num_enemy_landscapers >= num_enemy_buildings && !nearby_fulfillment) {
		  // build fulfillment
		  return RobotType.FULFILLMENT_CENTER;
	  } else if (num_enemy_buildings >= num_enemy_drones && num_enemy_buildings >= num_enemy_landscapers && !nearby_design) {
		  return RobotType.DESIGN_SCHOOL;
	  } else if (num_enemy_drones >= num_enemy_landscapers && num_enemy_drones >= num_enemy_buildings && !nearby_netgun) {
		  return RobotType.NET_GUN;
	  }
	  return null;
  }
}
