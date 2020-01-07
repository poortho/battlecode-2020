package bot1;
import battlecode.common.*;
import java.util.Arrays;

import static bot1.Helper.directions;
import static bot1.RobotPlayer.turnCount;
import static bot1.RobotPlayer.round;
import static bot1.RobotPlayer.rc;
import static bot1.Helper.distx_35;
import static bot1.Helper.disty_35;

public class Comms {

	private static final int INITIAL_BID = 2;
	private static final int HARDCODE = 0x358eba3;
	public static int blockRound = 1;
	private static int c = 0;
	public static Transaction[][] trans = new Transaction[500][7];
	private static int seed;
	public static MapLocation[] explore = new MapLocation[6];

	public static void getBlocks() throws GameActionException {
		// received all new messages
		while (blockRound < round) {
			Transaction[] messages = rc.getBlock(blockRound);
			trans[c] = messages;
			for (int i = 0; i < messages.length; i++) {
				int[] temp_msg = messages[i].getMessage();
				System.out.println("New Message: " + Arrays.toString(messages[i].getMessage()));
				System.out.println(blockRound);
				if (blockRound == 1 && (temp_msg[0] ^ HARDCODE) % 0x69696969 == temp_msg[1]) {
					// read seed
					seed = temp_msg[0];
					System.out.println("Seed: " + Integer.toString(seed));

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
					System.out.println("New Locations: " + Arrays.toString(explore));
				} else if (temp_msg.length == 7) {
					// read messages
					int key = xorKey(blockRound);
					if (temp_msg[6] != key) {
						// bad message
						continue;
					}
					for (int j = 0; j < temp_msg.length; j++) {
						temp_msg[j] ^= key;
						System.out.println("Received message: " + Integer.toString(temp_msg[j]));
					}
				}
			}
			blockRound++;
			c++;
		}
	}

	public static int xorKey(int round) {
		// totally bad LCG
		return ((int)Math.pow(3, round) * seed) % 0x69696969;
	}

	public static void addMessage(int[] msg, int length, int bid) throws GameActionException {
		int key = xorKey(turnCount);
		msg[6] = key;
		for (int i = 0; i < length; i++) {
			msg[i] ^= key;
		}

		System.out.println("Turn key: " + Integer.toString(turnCount) + " " + Integer.toString(key));

		if (rc.canSubmitTransaction(msg, bid)) {
			System.out.println("Submit message: ");
			rc.submitTransaction(msg, bid);
		}
	}

	public static void setSeed(MapLocation[] locs) throws GameActionException {
		int total = 0;
		MapLocation cur_loc = rc.getLocation();
		for (int i = 0; i < directions.length; i++) {
			total += rc.senseSoup(cur_loc.add(directions[i])) << (i*2);
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
		rc.submitTransaction(msg, INITIAL_BID);
	}
}