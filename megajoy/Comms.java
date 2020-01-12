package megajoy;

import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotType;
import battlecode.common.Transaction;

import static megajoy.Helper.directions;
import static megajoy.RobotPlayer.rc;
import static megajoy.RobotPlayer.round;

public class Comms {

	private static final int INITIAL_BID = 2;
	public static int HARDCODE;
	public static int blockRound = 1;
	private static int c = 0;
	//public static Transaction[][] trans = new Transaction[500][7];
	private static int seed = -1;
	public static MapLocation[] explore = new MapLocation[6];
	public static boolean[] map_explore = new boolean[6];

  static MapLocation[] miner_queue = new MapLocation[20];
  static int[] miner_queue_num = new int[20];
  static boolean[] must_reach = new boolean[20];
  static int next_idx = 0, poll_idx = 0;

	public static void getBlocks() throws GameActionException {
		// received all new messages
		while (blockRound < round) {

			// miners get their first target
			if (rc.getType() == RobotType.MINER) {
				if (blockRound == round - 1 && !Miner.first_target) {
					if (miner_queue_peek() != null) {
						//System.out.println("First target: " + miner_queue_peek().toString());
						Miner.target_explore = miner_queue_peek();
						Miner.must_reach_dest = true;
					}
					Miner.first_target = true;
				}
			}

			Transaction[] messages = rc.getBlock(blockRound);

			if (seed == -1) {
				// find seed
				for (int i = 0; i < messages.length; i++) {
					int[] temp_msg = messages[i].getMessage();
					if (blockRound == 1 && (temp_msg[0] ^ HARDCODE) % 0x69696969 == temp_msg[1]) {
						seed = temp_msg[0];
						//System.out.println("Set seed: " + Integer.toString(seed));

						// unpack explore locations
						explore[0] = new MapLocation((temp_msg[2] >> 8) & 0xff, temp_msg[2] & 0xff);
						explore[1] = new MapLocation((temp_msg[2] >> 24) & 0xff, (temp_msg[2] >> 16) & 0xff);
						explore[2] = new MapLocation((temp_msg[3] >> 8) & 0xff, temp_msg[3] & 0xff);
						explore[3] = new MapLocation((temp_msg[3] >> 24) & 0xff, (temp_msg[3] >> 16) & 0xff);
						explore[4] = new MapLocation((temp_msg[4] >> 8) & 0xff, temp_msg[4] & 0xff);
						explore[5] = new MapLocation((temp_msg[4] >> 24) & 0xff, (temp_msg[4] >> 16) & 0xff);
						break;
					}
				}
			}


			for (int i = 0; i < messages.length; i++) {
				int[] temp_msg = messages[i].getMessage();
				//System.out.println(blockRound);
				//System.out.println("New Message: " + Arrays.toString(messages[i].getMessage()));
				//System.out.println("Hardcode: " + Integer.toString(HARDCODE));
				if (temp_msg.length == 7) {
					// read messages
					int key = xorKey(blockRound);
					if (temp_msg[6] != key) {
						// bad message
						continue;
					}
					for (int j = 0; j < temp_msg.length; j++) {
						// process message
						temp_msg[j] ^= key;
						// System.out.println("Received message: " + Integer.toString(temp_msg[j]));

						int opcode = temp_msg[j] & 0xf;

						if (opcode == 0x1) {
							// add to queue
							int x = (temp_msg[j] >> 16) & 0xff;
							int y = (temp_msg[j] >> 8) & 0xff;
							int n = (temp_msg[j] >> 4) & 0xf;
							int reach_dest = (temp_msg[j] >> 24) & 1;
							if (n > 0) {
								miner_queue_push(new MapLocation(x, y), n | (reach_dest << 16));
							}

							if (rc.getType() == RobotType.MINER && blockRound != 1) {
								// this is so we don't broadcast patches near locations that are already going to be explored
								Miner.explored[Miner.explored_count] = new MapLocation(x, y);
								//System.out.println("explored " + Integer.toString(blockRound) + " " + Miner.explored[Miner.explored_count]);
								Miner.explored_count++;
							}
						} else if (opcode == 0x2) {
							//int x = (temp_msg[j] >> 8) & 0xf;
							//int y = (temp_msg[j] >> 12) & 0xf;
							//int n = (temp_msg[j] >> 4) & 0xf;
							miner_queue_remove();
						} else if (opcode == 0x3) {
							// found enemy HQ
							int x = (temp_msg[j] >> 12) & 0xff;
							int y = (temp_msg[j] >> 4) & 0xff;
							HQ.enemy_hq = new MapLocation(x, y);
							//System.out.println("Received enemy HQ: " + HQ.enemy_hq.toString());
						} else if (opcode == 0x4) {
							// friendy HQ
							int x = (temp_msg[j] >> 12) & 0xff;
							int y = (temp_msg[j] >> 4) & 0xff;
							HQ.our_hq = new MapLocation(x, y);
							//System.out.println("Received our HQ: " + HQ.our_hq.toString());
						} else if (opcode == 0x5) {
							HQ.rushed = true;
						} else if (opcode == 0x6) {
							HQ.rushed = false;
						} else if (opcode == 0x7) {
							HQ.patrol_broadcast_round = blockRound;
							HQ.broadcasted_patrol = true;
						} else if (opcode == 0x8) {
							int x = (temp_msg[j] >> 12) & 0xff;
							int y = (temp_msg[j] >> 4) & 0xff;
							RobotPlayer.netgun_map[x][y] = blockRound;
						}

						temp_msg[j] ^= key;
					}
				}
			}
			blockRound++;
			c++;
		}
	}

	public static boolean broadcast_patrol_enemy_hq() throws GameActionException {
		// 0x00000000
		//          3 <- opcode
		//      XXYY  <- patch location / 4
		int val = 0x7;
		int[] msg = {val, 0, 0, 0, 0, 0, 0};

		return addMessage(msg, 1, 2);
	}

	public static void broadcast_end_rushed() throws GameActionException {
		// 0x00000000
		//          3 <- opcode
		//      XXYY  <- patch location / 4	
		int val = 0x6;
		int[] msg = {val, 0, 0, 0, 0, 0, 0};

		addMessage(msg, 1, 2);
	}

	public static void broadcast_being_rushed() throws GameActionException {
		// 0x00000000
		//          3 <- opcode
		//      XXYY  <- patch location / 4	
		int val = 0x5;
		int[] msg = {val, 0, 0, 0, 0, 0, 0};

		addMessage(msg, 1, 2);
	}

	public static void broadcast_enemy_netgun(MapLocation loc) throws GameActionException {
		broadcast_enemy_netgun(loc, rc.getRoundNum());
	}

	public static void broadcast_enemy_netgun(MapLocation loc, int round) throws GameActionException {
		// 0x00000000
		//          3 <- opcode
		//      XXYY  <- patch location / 4
		int val = (loc.x << 12) | (loc.y << 4) | 8;
		int[] msg = {val, 0, 0, 0, 0, 0, 0};

		addMessage(msg, 1, 2);
	}

	public static void broadcast_enemy_hq(MapLocation loc) throws GameActionException {
		// 0x00000000
		//          3 <- opcode
		//      XXYY  <- patch location / 4	
		int val = (loc.x << 12) | (loc.y << 4) | 0x3;
		int[] msg = {val, 0, 0, 0, 0, 0, 0};

		addMessage(msg, 1, 2);
	}

	public static void broadcast_friendly_hq(MapLocation loc) throws GameActionException {
		// 0x00000000
		//          4 <- opcode
		//      XXYY  <- patch location / 4	
		int val = (loc.x << 12) | (loc.y << 4) | 0x4;
		int[] msg = {val, 0, 0, 0, 0, 0, 0};

		addMessage(msg, 1, 2);
	}

	public static void miner_queue_push(MapLocation loc, int num) {
		miner_queue[next_idx] = loc;
		miner_queue_num[next_idx] = num;
		next_idx = (next_idx + 1) % miner_queue.length;
		//System.out.println("New miner: " + Integer.toString(num));
		//System.out.println("[x] Miner queue: " + Arrays.toString(miner_queue));
	}

	public static MapLocation miner_queue_peek() {
		return miner_queue[poll_idx];
	}

	public static void miner_queue_remove() {
		miner_queue[poll_idx] = null;
		miner_queue_num[poll_idx] = 0;
		poll_idx = (poll_idx + 1) % miner_queue.length;
	}

	public static boolean broadcast_miner_request(MapLocation loc, int num, boolean must) throws GameActionException {
		// 0x00000000
		//          1 <- broadcast miner request
		//         N  <- number of miners needed
		//     XXYY   <- patch location / 4	
		int val = (loc.x << 16) | (loc.y << 8) | (num << 4) | 0x1;
		if (must) {
			val |= 1 << 24;
		}
		int msg[] = {val, 0, 0, 0, 0, 0, 0};
		return addMessage(msg, 1, 2);
	}

	public static boolean broadcast_miner_remove() throws GameActionException {
		// 0x00000000
		//          2 <- broadcast miner remove
		//         N  <- number of miners
		//     XXYY   <- patch location / 4	
		int val = 0x2;
		int msg[] = {val, 0, 0, 0, 0, 0, 0};
		return addMessage(msg, 1, 2);	
	}

	public static int xorKey(int round) {
		// totally bad LCG
		return (((int)Math.pow(3, round) % 0x69696969) ^ seed);
	}

	public static boolean addMessage(int[] msg, int length, int bid) throws GameActionException {
		int key = xorKey(round);
		for (int i = 0; i < 7; i++) {
			msg[i] ^= key;
		}

		//System.out.println("Turn key: " + Integer.toString(round) + " " + Integer.toString(key));

		if (rc.canSubmitTransaction(msg, bid)) {
			//System.out.println("Submit message: ");
			rc.submitTransaction(msg, bid);
			return true;
		}
		return false;
	}

	public static void setSeed(MapLocation[] locs) throws GameActionException {
		int total = 0;
		MapLocation cur_loc = rc.getLocation();
		for (int i = 0; i < directions.length; i++) {
			total += rc.senseElevation(cur_loc.add(directions[i])) << (i);
		}
		total += cur_loc.x*69 + cur_loc.y*69;

    //System.out.println(Comms.HARDCODE);
		//System.out.println("Locations: " + Integer.toString(loc1) + " " + Integer.toString(loc2));
		int[] msg = {(total), ((total) ^ HARDCODE), (locs[1].x << 24) | (locs[1].y << 16) | (locs[0].x << 8) | (locs[0].y), (locs[3].x << 24) | (locs[3].y << 16) | (locs[2].x << 8) | (locs[2].y), (locs[5].x << 24) | (locs[5].y << 16) | (locs[4].x << 8) | (locs[4].y)};
		//System.out.println("Submit seed bid");
		seed = total;
		rc.submitTransaction(msg, INITIAL_BID);
	}
}