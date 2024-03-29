package stable_miner1;
import battlecode.common.*;
import java.util.Arrays;

import static stable_miner1.Helper.directions;
import static stable_miner1.RobotPlayer.turnCount;
import static stable_miner1.RobotPlayer.round;
import static stable_miner1.RobotPlayer.rc;
import static stable_miner1.Helper.distx_35;
import static stable_miner1.Helper.disty_35;

public class Comms {

	private static final int INITIAL_BID = 2;
	private static final int HARDCODE = 0x358eba3;
	public static int blockRound = 1;
	private static int c = 0;
	//public static Transaction[][] trans = new Transaction[500][7];
	private static int seed;
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
					System.out.println("First target: " + miner_queue_peek().toString());
					Miner.target_explore = miner_queue_peek();
					Miner.first_target = true;
					Miner.must_reach_dest = true;
				}
			}

			Transaction[] messages = rc.getBlock(blockRound);
			for (int i = 0; i < messages.length; i++) {
				int[] temp_msg = messages[i].getMessage();
				//System.out.println("New Message: " + Arrays.toString(messages[i].getMessage()));
				System.out.println(blockRound);
				if (blockRound == 1 && (temp_msg[0] ^ HARDCODE) % 0x69696969 == temp_msg[1]) {
					// read seed
					seed = temp_msg[0];

					// unpack explore locations
					int c = 0;
					for (int j = 0; j < 4; j++) {
						int temp = (temp_msg[2] >> (j*8)) & 0xff;
						explore[c] = new MapLocation((temp >> 4) * 4, (temp & 0xf) * 4);
						c++;
					}
					for (int j = 4; j < 6; j++) {
						int temp = (temp_msg[3] >> (j*8)) & 0xff;
						explore[c] = new MapLocation((temp >> 4) * 4, (temp & 0xf) * 4);
						c++;
					}
				} else if (temp_msg.length == 7) {
					// read messages
					int key = xorKey(blockRound);
					if (temp_msg[6] != key) {
						// bad message
						continue;
					}
					for (int j = 0; j < temp_msg.length; j++) {
						// process message
						temp_msg[j] ^= key;
						System.out.println("Received message: " + Integer.toString(temp_msg[j]));

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
								Miner.explored[Miner.explored_count] = new MapLocation(x, y);
								System.out.println("explored " + Integer.toString(blockRound) + " " + Miner.explored[Miner.explored_count]);
								Miner.explored_count++;
							}
						} else if (opcode == 0x2) {
							//int x = (temp_msg[j] >> 8) & 0xf;
							//int y = (temp_msg[j] >> 12) & 0xf;
							//int n = (temp_msg[j] >> 4) & 0xf;
							miner_queue_remove();
						}

						temp_msg[j] ^= key;
					}
				}
			}
			blockRound++;
			c++;
		}
	}

	public static void miner_queue_push(MapLocation loc, int num) {
		miner_queue[next_idx] = loc;
		miner_queue_num[next_idx] = num;
		next_idx = (next_idx + 1) % miner_queue.length;
		System.out.println("New miner: " + Integer.toString(num));
		System.out.println("[x] Miner queue: " + Arrays.toString(miner_queue));
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

		System.out.println("Turn key: " + Integer.toString(round) + " " + Integer.toString(key));

		if (rc.canSubmitTransaction(msg, bid)) {
			System.out.println("Submit message: ");
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
		int loc1 = 0;
		for (int i = 0; i < 4; i++) {
			loc1 = loc1 | ((((locs[i].x / 4) << 4) | locs[i].y / 4) << (8*i));
		}
		int loc2 = 0;
		for (int i = 0; i < 2; i++) {
			loc2 = loc2 | ((((locs[i + 4].x / 4) << 4) | locs[i + 4].y / 4) << (8*i));
		}
		System.out.println("Locations: " + Integer.toString(loc1) + " " + Integer.toString(loc2));
		int[] msg = {(total), ((total) ^ HARDCODE) % 0x69696969, loc1, loc2};
		System.out.println("Submit seed bid");
		seed = total;
		rc.submitTransaction(msg, INITIAL_BID);
	}
}