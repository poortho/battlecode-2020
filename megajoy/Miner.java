package megajoy;

import battlecode.common.*;

import static megajoy.Helper.*;
import static megajoy.RobotPlayer.rc;

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
  static int num_enemy_miners = 0;
  static int friendy_landscapers = 0;
  static boolean nearby_fulfillment = false;
  static boolean nearby_netgun = false;
  static boolean nearby_design = false;
  static boolean near_hq = false;

  static boolean turtling = false;
  static boolean bugpath_blocked = false;
  static boolean[] turtle_blocked = new boolean[directions.length];
  static int blocked = 0;
  static int mine_count = -1;
  static boolean duplicate_building;

  static MapLocation closest_rush_enemy = null;

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

		mine_count = count_mine();

		sense();

		if (!turtling)
			turtling = check_turtling();

		// don't do anything if being rushed and i'm next to HQ
		if (HQ.rushed && near_hq) {
			if (cur_loc.distanceSquaredTo(hq) > 2) {
				greedy_walk(hq);
			} else if (closest_rush_enemy != null) {
				Helper.greedy_move_adjacent_HQ(closest_rush_enemy, cur_loc);
			}
		}

		boolean dont_move = HQ.rushed && near_hq;

		if (HQ.rushed && near_hq) {
			// try to check if miners already surrounded HQ
			int c = 0;
			for (int i = 0; i < directions.length; i++) {
				MapLocation next_loc = HQ.our_hq.add(directions[i]);
				RobotInfo rob = rc.senseRobotAtLocation(next_loc);
				if (rob.team == rc.getTeam()) {
					c++;
				}
			}
			if (c == 8 && cur_loc.distanceSquaredTo(HQ.our_hq) > 2) {
				dont_move = false;
			}
		}

		// move away from hq if turtling
		if (!dont_move && turtling && cur_loc.distanceSquaredTo(HQ.our_hq) <= 8) {
			boolean res = Helper.greedy_move_away(HQ.our_hq, cur_loc);
			if (!res && cur_loc.distanceSquaredTo(HQ.our_hq) <= 2) {
				Helper.try_miner_suicide(cur_loc);
			}
			return;
		}

		if ((dont_move) && in_danger) {
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

		// check for duplicate
		duplicate_building = false;
		if (toBuild != null) {
			switch (toBuild) {
				case DESIGN_SCHOOL:
					for (int i = 0; i < Comms.design_school_idx; i++) {
						if (Comms.design_schools[i].equals(hq)) {
							duplicate_building = true;
							break;
						}
					}
					break;

				case FULFILLMENT_CENTER:
					for (int i = 0; i < Comms.fulfillment_center_idx; i++) {
						if (Comms.fulfillment_centers[i].equals(hq)) {
							duplicate_building = true;
							break;
						}
					}
					break;
			}
		}

		// build thing
		if (!duplicate_building && toBuild != null && ((rc.getTeamSoup() >= (int)toBuild.cost*1.5 && num_enemies != 0) ||
				rc.getTeamSoup() >= toBuild.cost*(near_hq ? 2 : 4))) {
			// build if none nearby and (nearby enemies or close to hq)
			if (cur_loc.distanceSquaredTo(hq) <= 13) {
				for (int i = 0; i < directions.length; i++) {
					MapLocation new_loc = cur_loc.add(directions[i]);
					if (new_loc.distanceSquaredTo(hq) < GameConstants.NET_GUN_SHOOT_RADIUS_SQUARED &&
						new_loc.distanceSquaredTo(hq) > 3) {

						boolean valid = true;
						for (int j = 0; j < directions.length; j++) {
							if (rc.canSenseLocation(new_loc.add(directions[j]))) {
								RobotInfo robot = rc.senseRobotAtLocation(new_loc.add(directions[j]));
								if (robot != null && robot.type.isBuilding()) {
									valid = false;
									break;
								}
							}
						}
						if (!valid) {
							continue;
						}
						boolean res = Helper.tryBuild(toBuild, directions[i]);
						if (res) {
							Comms.broadcast_building(hq, toBuild);
						}
					}
				}
			}
		}

		if (dont_move) {
			// don't want to do exploring or mining if we're being rushed and we're near hq
			return;
		}

		if (target_explore != null) {
			find_mine();
			if (rc.canSenseLocation(target_explore) && rc.senseFlooding(target_explore)) {
				target_explore = get_explore_target();
			}

			if (target_explore != null && cur_loc.distanceSquaredTo(target_explore) <= 10) {//rc.canSenseLocation(target_explore)) {
				// i'm at explore location
				target_explore = null;
			}
			if (target_explore != null) {
				miner_walk(target_explore);
			}
			else if (target_mine != null) {
				do_mine();
			}
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
					hq = HQ.our_hq;
					if (HQ.our_hq != null && HQ.our_hq.equals(hq) && cur_loc.distanceSquaredTo(hq) < 8) {
						Helper.greedy_move_away(hq, cur_loc);
					} else if (HQ.our_hq != null && HQ.our_hq.equals(hq) && cur_loc.distanceSquaredTo(hq) > 13) {
						miner_walk(hq);
					}
				}
			}
		}
	}

	static boolean check_turtling() throws GameActionException {
		if (!rc.canSenseLocation(HQ.our_hq)) {
			return false;
		}
		if (near_hq && hq.equals(HQ.our_hq)) {
			int blocked = 0;
			for (int i = 0; i < directions.length; i++) {
				MapLocation next_loc = hq.add(directions[i]);
				if (rc.canSenseLocation(next_loc) && rc.senseElevation(next_loc) > rc.senseElevation(hq) + 3) {
					blocked++;
				}
			}
			if (blocked >= 5) {
				return true;
			}
		}
		return false;
	}

	static void sense() throws GameActionException {
		int hq_dist = cur_loc.distanceSquaredTo(hq);
		num_enemy_drones = 0;
		num_enemy_landscapers = 0;
		num_enemy_buildings = 0;
		num_enemies = 0;
		num_enemy_miners = 0;
		nearby_fulfillment = false;
		nearby_netgun = false;
		nearby_design = false;
		friendy_landscapers = 0;
		int min_dist = 999999;
		for (int i = 0; i < robots.length; i++) {
			int temp_dist = robots[i].location.distanceSquaredTo(cur_loc);
			if (robots[i].type == RobotType.REFINERY && robots[i].team == rc.getTeam() && (temp_dist < hq_dist || turtling)) {
				hq = robots[i].location;
				hq_dist = temp_dist;
			} else if (HQ.enemy_hq == null && robots[i].type == RobotType.HQ && robots[i].team != rc.getTeam()) {
				// found enemy hq broadcast it
				//System.out.println("Found enemy hq! " + robots[i].location);
				Comms.broadcast_enemy_hq(robots[i].location);
			} else if (robots[i].type == RobotType.HQ && robots[i].team == rc.getTeam()) {
				HQ.our_hq = robots[i].location;
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
						if (HQ.our_hq != null) {
							int tmp_dist = robots[i].location.distanceSquaredTo(HQ.our_hq);
							if (tmp_dist < min_dist) {
								min_dist = tmp_dist;
								closest_rush_enemy = robots[i].location;
							}
						}
						break;
					case HQ:
					case NET_GUN:
					case REFINERY:
					case VAPORATOR:
					case DESIGN_SCHOOL:
						num_enemy_landscapers++; //count this as landscaper
						num_enemy_drones--; // for when it falls through
					case FULFILLMENT_CENTER:
						num_enemy_drones++; // count this as drone
						num_enemy_buildings++;
						break;
					case MINER:
						num_enemy_miners++;
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
				target_explore = get_explore_target();
				if (target_explore != null) {
					miner_walk(target_explore);
				}
				return;
			}
		}
		// try mining it lol
		if (rc.getSoupCarrying() == RobotType.MINER.soupLimit) {
			// check if HQ has landscapers around it
			/*
			if (!turtling && HQ.our_hq != null && hq.equals(HQ.our_hq) && rc.canSenseLocation(hq)) {
				turtling = true;
				for (int i = 0; i < Helper.directions.length; i++) {
					MapLocation temp_loc = hq.add(Helper.directions[i]);
					if (rc.canSenseLocation(temp_loc)) {
						RobotInfo rob = rc.(temp_loc);
						if (rob != null && rob.type == RobotType.LANDSCAPER) {
							turtling = true;
							break;
						}
					}
				}
			}*/

			if (turtling && hq.equals(HQ.our_hq)) {
				if (cur_loc.distanceSquaredTo(target_mine) <= 5 && mine_count > 300) {
					// try build refinery
					int res = Helper.tryBuildNotAdjacentHQ(RobotType.REFINERY, near_hq);
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
				if (distance > 80 && distance2 < 24 && mine_count > 600 && num_enemy_landscapers == 0 && num_enemy_buildings == 0 && (res = Helper.tryBuildNotAdjacentHQ(RobotType.REFINERY, near_hq)) != -1) {
					// build refinery
					hq = cur_loc.add(directions[res]);
					//System.out.println("New HQ: " + hq.toString());
				} else if (distance < 800) {
					//System.out.println("Walking Back To HQ");
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
		//System.out.println("broadcast patch: " + target_mine.toString());
		for (int c = 0; c < explored_count; c++) {
			if (target_mine.distanceSquaredTo(explored[c]) <= 45) {
				return;
			}
		}

		int num = get_num_workers_needed();
		Comms.broadcast_miner_request(target_mine, num, true);
		//System.out.println("Found patch at: " + target_mine);
		//System.out.println("Workers needed: " + Integer.toString(num));
	}

	static int get_num_workers_needed() throws GameActionException {
		int total_soup = mine_count;
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
				//System.out.println("FOUND HQ");
				return next_loc;
			}
		}
		//System.out.println("SHOULDNT HAPPEN EVER");
		return null;
	}

	static int count_mine() throws GameActionException {
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

	static MapLocation find_mine() throws GameActionException {
		for (int i = 0; i < distx_35.length; i++) {
			MapLocation next_loc = cur_loc.translate(distx_35[i], disty_35[i]);
			if (rc.canSenseLocation(next_loc) && !rc.senseFlooding(next_loc)) {
				int count = rc.senseSoup(next_loc);
				if (count > 0) {
					//System.out.println("Found mine at:" + next_loc.toString());
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
		if (!rc.isReady()) {
			return;
		}
		Direction greedy;

		int least_dist = cur_loc.distanceSquaredTo(loc);
		int next = -1;
		int greedy_dist = 9999999;
		int greedy_idx = -1;
		for (int i = 0; i < directions.length; i++) {
			MapLocation next_loc = cur_loc.add(directions[i]);
			int temp_dist = next_loc.distanceSquaredTo(loc);
			if (rc.canMove(directions[i])) {
				if (temp_dist < least_dist && !rc.senseFlooding(next_loc) && !blacklist[i]) {
					least_dist = temp_dist;
					next = i;
				}
			}
			if (temp_dist < greedy_dist) {
				greedy_dist = temp_dist;
				greedy_idx = i;
			}
		}

		if (!bugpath_blocked && next != -1) {
			rc.move(directions[next]);
			previous_location = cur_loc;
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

			if (next == -1) {
				next = greedy_idx;
			}

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
		int least_dist = loc.distanceSquaredTo(cur_loc);
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
        rc.mineSoup(dir); //System.out.println("MINED!"); return true;
        return true;
    } else {
    	return false;
    }
  }

  static boolean tryDepositSoup(Direction dir) throws GameActionException {
      if (rc.isReady() && rc.canDepositSoup(dir)) {
          rc.depositSoup(dir, rc.getSoupCarrying());
          //System.out.println("DEPOSITED");
          return true;
      } else {
      	return false;
      }
  }

  static RobotType calcBuilding() {
	  if (((num_enemy_landscapers > num_enemy_drones && num_enemy_landscapers > num_enemy_buildings) || near_hq) && !nearby_fulfillment) {
		  // build fulfillment
		  return RobotType.FULFILLMENT_CENTER;
	  } else if (((num_enemy_buildings > num_enemy_drones && num_enemy_buildings > num_enemy_landscapers) || num_enemy_miners > 0) && !nearby_design) {
		  return RobotType.DESIGN_SCHOOL;
	  } else if (num_enemy_drones > num_enemy_landscapers && num_enemy_drones > num_enemy_buildings && !nearby_netgun && !near_hq) {
		  return RobotType.NET_GUN;
	  } else if (!nearby_fulfillment && near_hq && hq.equals(HQ.our_hq) && rc.getTeamSoup() > 300) {
		  return RobotType.FULFILLMENT_CENTER;
	  } else if (!nearby_design && near_hq && hq.equals(HQ.our_hq) && rc.getTeamSoup() > 300) {
		  return RobotType.DESIGN_SCHOOL;
		}
	  return null;
  }
}
