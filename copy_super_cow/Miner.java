package copy_super_cow;

import battlecode.common.*;

import static copy_super_cow.Helper.*;
import static copy_super_cow.RobotPlayer.rc;
import static copy_super_cow.RobotPlayer.round;
import static copy_super_cow.RobotPlayer.turnCount;

public class Miner {

  static MapLocation cur_loc;
  static MapLocation target_mine = null;
  static MapLocation target_explore = null;
  static MapLocation hq = null;
  static int target_idx = -1;
  static int drone_factories_built = 0;
  static MapLocation find_mine_loc;
  static boolean rush = false;

  static int timeout_mine = 0, timeout_explore = 0;
  static int TIMEOUT_THRESHOLD = 100;
  static MapLocation[] timeout_mines = new MapLocation[20];
  static int timeout_mine_idx = 0;

  static boolean new_loc = false;

  // patches already explored by other miners
  static MapLocation previous_location;
  static MapLocation[] explored = new MapLocation[Comms.arr_len];
  static int explored_count = 0;
  static boolean check_new_patch = false;
  static boolean must_reach_dest = false;
  static boolean first_target = false;
  static boolean near_refinery = false;
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
  static boolean duplicate_building, gay_rush_alert = false;
  static MapLocation gay_rush_design_school = null;

  static MapLocation closest_rush_enemy = null;
  static boolean first_miner = false;

  static MapLocation[] locs;
  static int rush_idx;


	static void runMiner() throws GameActionException {
		timeout_mine++;
		timeout_explore++;
		cur_loc = rc.getLocation();
		in_danger = false;

		if (round == 3 && turnCount == 1) {
			System.out.println("FIRST MINER");
			first_miner = true;
		}

		if (hq == null) {
			hq = find_hq();
		}

		for (int i = blacklist.length; --i >= 0; ) {
			blacklist[i] = false;
		}
		Comms.getBlocks();

		robots = rc.senseNearbyRobots();

		sense();

		if (timeout_explore >= TIMEOUT_THRESHOLD) {
			target_explore = get_explore_target();
			timeout_explore = 0;
		}

		if (timeout_mine >= TIMEOUT_THRESHOLD && target_mine != null) {
			timeout_mines[timeout_mine_idx] = target_mine;
			timeout_mine_idx++;
			timeout_mine = 0;
			target_mine = null;
		}

		if (rush) {
			if (locs == null) {
				locs = new MapLocation[3];
        int width = rc.getMapWidth();
        int height = rc.getMapHeight();
        MapLocation middle = new MapLocation(width / 2, height / 2);

        // set locations
        int delta_x = middle.x + (middle.x - HQ.our_hq.x);
        int delta_y = middle.y + (middle.y - HQ.our_hq.y);
        locs[0] = new MapLocation(delta_x, HQ.our_hq.y);
        locs[1] = new MapLocation(delta_x, delta_y);
        locs[2] = new MapLocation(HQ.our_hq.x, delta_x);
        rush_idx = 0;
        target_explore = locs[rush_idx];
			}
			if (HQ.enemy_hq == null) {
				// explore lmao
				if (target_explore != null && rc.canSenseLocation(target_explore) && rc.senseFlooding(target_explore)) {
					rush_idx++;
					if (rush_idx < 3)
						target_explore = locs[rush_idx];
				}

				if (target_explore != null && cur_loc.distanceSquaredTo(target_explore) <= 10) {//rc.canSenseLocation(target_explore)) {
					// i'm at explore location
					rush_idx++;
					if (rush_idx < 3)
						target_explore = locs[rush_idx];
				}
				if (target_explore != null) {
					miner_walk(target_explore);
				} else {
					System.out.println("IDK");
				}
			} else {
				if (cur_loc.distanceSquaredTo(HQ.enemy_hq) > 8) {
					miner_walk(HQ.enemy_hq);
				} else {
					// try building a design school right next to enemy hq lmfao
					for (int i = 0; i < directions.length; i++) {
						MapLocation temp_loc = cur_loc.add(directions[i]);
						if (temp_loc.distanceSquaredTo(HQ.enemy_hq) <= 2 && rc.canBuildRobot(RobotType.DESIGN_SCHOOL, directions[i])) {
							rush = !Helper.tryBuild(RobotType.DESIGN_SCHOOL, directions[i]);
							break;
						}
					}
				}
			}
			return;
		}

		if (in_danger) {
			for (int i = directions.length; --i >= 0; ) {
				if (!blacklist[i] && rc.canMove(directions[i]) && !rc.senseFlooding(cur_loc.add(directions[i]))) {
					Helper.tryMove(directions[i]);
				}
			}
		}

		RobotType toBuild = calcBuilding();

		// if drones, build netgun
		// if landscapers, build drone
		// if buildings, build landscape

		duplicate_building = false;
		// check for duplicate

		if (toBuild != null) {
			switch (toBuild) {
				case DESIGN_SCHOOL:
					for (int i = Comms.design_school_idx; --i >= 0; ) {
						if (Comms.design_schools[i].equals(hq)) {
							duplicate_building = true;
							break;
						}
					}
					break;

				case FULFILLMENT_CENTER:
					for (int i = Comms.fulfillment_center_idx; --i >= 0; ) {
						if (Comms.fulfillment_centers[i].equals(hq)) {
							duplicate_building = true;
							break;
						}
					}
					break;
				/*
				case NET_GUN:
					for (int i = 0; i < Comms.netgun_idx; i++) {
						if (Comms.netguns[i].equals(hq)) {
							duplicate_building = true;
							break;
						}
					}
					break;*/
			}
		}

		// build thing
		if (!duplicate_building && toBuild != null && ((rc.getTeamSoup() >= toBuild.cost*1.5) ||
				rc.getTeamSoup() >= toBuild.cost*(near_hq ? 2 : 4) || (toBuild == RobotType.VAPORATOR && rc.getTeamSoup() > RobotType.VAPORATOR.cost))) {
			// build if none nearby and (nearby enemies or close to hq)
			if (toBuild == RobotType.NET_GUN || cur_loc.distanceSquaredTo(hq) <= 40) {
				for (int i = 0; i < directions.length; i++) {
					MapLocation new_loc = cur_loc.add(directions[i]);
					if (HQ.our_hq == null || new_loc.distanceSquaredTo(HQ.our_hq) > 18) {

						boolean valid = true;
						for (int j = directions.length; --j >= 0; ) {
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

		//System.out.println(Clock.getBytecodesLeft());
		mine_count = count_mine();
		//System.out.println(Clock.getBytecodesLeft());

		if (target_mine != null) {
			do_mine();
		} else if (target_explore != null && !first_miner) {
			find_mine();
			//System.out.println(Clock.getBytecodesLeft());
			if (rc.canSenseLocation(target_explore) && rc.senseFlooding(target_explore)) {
				target_explore = get_explore_target();
			}

			if (target_explore != null && cur_loc.distanceSquaredTo(target_explore) <= 10) {//rc.canSenseLocation(target_explore)) {
				// i'm at explore location
				target_explore = null;
				if (target_mine == null) {
					new_loc = false;
				}
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
				if (target_explore != null && !first_miner) {
					miner_walk(target_explore);
				} else {
					hq = HQ.our_hq;
					if (HQ.our_hq != null && HQ.our_hq.equals(hq) && cur_loc.distanceSquaredTo(hq) < 18) {
						Helper.greedy_move_away(hq, cur_loc);
					} else if (HQ.our_hq != null && HQ.our_hq.equals(hq) && cur_loc.distanceSquaredTo(hq) > 25) {
						lattice_walk(hq);
					}
				}
			}
		}
	}

	static void try_deposit_soup_without_moving() throws GameActionException {
		if (HQ.our_hq != null && cur_loc.distanceSquaredTo(HQ.our_hq) <= 2 && rc.getSoupCarrying() > 0) {
			Direction d = cur_loc.directionTo(HQ.our_hq);
			tryDepositSoup(d);
		}
	}

	static boolean check_turtling() throws GameActionException {
		if (!rc.canSenseLocation(HQ.our_hq)) {
			return false;
		}

		if (near_hq && hq.equals(HQ.our_hq)) {
			int hq_elevation = rc.senseElevation(hq);
			int blocked = 0;
			for (int i = 0; i < directions.length; i++) {
				MapLocation next_loc = hq.add(directions[i]);
				if (rc.canSenseLocation(next_loc) && rc.senseElevation(next_loc) > hq_elevation + 3) {
					blocked++;
					if (blocked == 5) {
						return true;
					}
				}
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
		near_refinery = false;
		friendy_landscapers = 0;
		int min_dist = 999999;
		for (int i = robots.length; --i >= 0; ) {
			MapLocation rob_loc = robots[i].location;
			int temp_dist = rob_loc.distanceSquaredTo(cur_loc);

			// calculations for building stuff
			if (robots[i].team != rc.getTeam()) {
				num_enemies++;
				switch (robots[i].type) {
					case DELIVERY_DRONE:
						if (cur_loc.distanceSquaredTo(robots[i].getLocation()) <= 8) {
							if (cur_loc.distanceSquaredTo(robots[i].getLocation()) <= GameConstants.DELIVERY_DRONE_PICKUP_RADIUS_SQUARED) {
								in_danger = true;
							}
							for (int j = directions.length; --j >= 0; ) {
								blacklist[j] = blacklist[j] || cur_loc.add(directions[j]).distanceSquaredTo(robots[i].getLocation())
										<= GameConstants.DELIVERY_DRONE_PICKUP_RADIUS_SQUARED;
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
								closest_rush_enemy = rob_loc;
							}
						}
						break;
					case HQ:
						if (HQ.enemy_hq == null) {
							Comms.broadcast_enemy_hq(rob_loc);
						}
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
					case REFINERY:
						if (temp_dist < hq_dist || turtling || (rc.canSenseLocation(hq) && rc.senseRobotAtLocation(hq) == null)) {
							hq = rob_loc;
							hq_dist = temp_dist;
						}
						near_refinery = true;
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
								MapLocation temp_loc = rob_loc.add(Helper.directions[a]);
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
						HQ.our_hq = rob_loc;
						near_hq = true;
						break;
					}
			}
		}
	}

	static void do_mine() throws GameActionException {
		if (target_mine != null && rc.canSenseLocation(target_mine) && rc.senseFlooding(target_mine)) {
			target_mine = find_mine();
			if (target_mine == null) {
				return;
			}
		}
		// try mining it lol
		if (rc.getSoupCarrying() == RobotType.MINER.soupLimit || target_mine == null) {
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

			int res = -1;
			if (cur_loc.distanceSquaredTo(hq) <= 2) {
				timeout_mine = 0;
				// deposit
				if (rc.canSenseLocation(hq) && rc.senseRobotAtLocation(hq) == null) {
					// refinery/hq was killed :(
					tryBuild(RobotType.REFINERY);
				}
				tryDepositSoup(cur_loc.directionTo(hq));
			} else if (((target_mine.distanceSquaredTo(hq) > 40 && target_mine.distanceSquaredTo(cur_loc) < 24
					&& mine_count > 400 && num_enemy_landscapers == 0)) &&
					(res = tryBuild(RobotType.REFINERY)) != -1) {
				// build refinery
				hq = cur_loc.add(directions[res]);
				//System.out.println("New HQ: " + hq.toString());
			} else {
				miner_walk(hq);
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
		for (int c = explored_count; --c >= 0; ) {
			if (target_mine.distanceSquaredTo(explored[c]) <= 25) {
				return;
			}
		}

		int num = get_num_workers_needed();
		Comms.broadcast_miner_request(target_mine, num, true);
		//System.out.println("Found patch at: " + target_mine);
		//System.out.println("Workers needed: " + Integer.toString(num));
	}

	static int get_num_workers_needed() throws GameActionException {
		return mine_count / 50;
	}

	// get next target coordinate to explore to
	// this gets called when miner is created and there are no patches around miner, or when miner reaches current target and needs a new target
	static MapLocation get_explore_target() throws GameActionException {
		must_reach_dest = false;//(Comms.miner_queue_num[Comms.poll_idx] & (1 << 16)) == 1;
		if (Comms.poll_idx + 1 < 20 && !new_loc && target_mine == null) {
			Comms.broadcast_miner_remove(Comms.poll_idx + 1);
			Comms.poll_idx++;
		}
		timeout_explore = 0;
		return Comms.miner_queue_peek();
	}

	static MapLocation find_hq() throws GameActionException {
		for (int i = 9; --i >= 0; ) {
			MapLocation next_loc = cur_loc.translate(distx_35[i], disty_35[i]);
			if (rc.canSenseLocation(next_loc)) {
				RobotInfo temp = rc.senseRobotAtLocation(next_loc);
				if (temp != null && temp.type == RobotType.HQ && temp.team == rc.getTeam()) {
					//System.out.println("FOUND HQ");
					return next_loc;
				}
			}
		}
		//System.out.println("SHOULDNT HAPPEN EVER");
		return null;
	}

	static int count_mine() throws GameActionException {
		int total_soup = 0;
		find_mine_loc = null;
		for (int i = 0; i < distx_35.length; i++) {
			MapLocation next_loc = cur_loc.translate(distx_35[i], disty_35[i]);
			if (rc.canSenseLocation(next_loc)) {
				int count = rc.senseSoup(next_loc);
				total_soup += count;
				if (find_mine_loc == null && count > 0) {
					boolean good = true;
					for (int j = 0; j < timeout_mine_idx; j++) {
						if (next_loc.distanceSquaredTo(timeout_mines[j]) <= 35) {
							good = false;
							break;
						}
					}
					if (good)
						find_mine_loc = next_loc;
				}
			}
		}
		return total_soup;
	}

	static MapLocation find_mine() throws GameActionException {
		if (find_mine_loc != null) {
			check_new_patch = true;
			target_mine = find_mine_loc;
			broadcast_patch();
			return target_mine;
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
		if (HQ.done_turtling) {
			lattice_walk(loc);
		} else {
			bugpath_walk(loc);
		}
	}
	static void lattice_walk(MapLocation loc) throws GameActionException {
		// bugpath walk but dont fall into lattice holes
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

		if (!bugpath_blocked && next != -1 && isLattice(cur_loc.add(directions[next]))) {
			rc.move(directions[next]);
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
				if (rc.canMove(cw) && !rc.senseFlooding(next_loc) && !blacklist[next] && !Helper.willFlood(next_loc) &&
						isLattice(cur_loc.add(directions[next]))) {
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
  	if (num_enemy_drones >= 1 && !nearby_netgun) {
  		return RobotType.NET_GUN;
  	} else if (((num_enemy_landscapers > 0)) && !nearby_fulfillment) {
		  // build fulfillment
		  return RobotType.FULFILLMENT_CENTER;
	  } else if (((num_enemy_buildings > num_enemy_drones && num_enemy_buildings > num_enemy_landscapers) || (round > 200 && near_hq)) && !nearby_design) {
		  return RobotType.DESIGN_SCHOOL;
	  } else if (num_enemy_drones > num_enemy_landscapers && num_enemy_drones > num_enemy_buildings && !nearby_netgun) {
		  return RobotType.NET_GUN;
	  }/* else if (near_hq && !nearby_design) {
	  	return RobotType.DESIGN_SCHOOL;
	  }*/
	  return RobotType.VAPORATOR;
  }
}
