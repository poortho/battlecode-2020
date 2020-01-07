package bot1;
import battlecode.common.*;
import java.util.Arrays;

import static bot1.Helper.directions;
import static bot1.RobotPlayer.turnCount;
import static bot1.RobotPlayer.rc;
import static bot1.Helper.distx_35;
import static bot1.Helper.disty_35;

public class Comms {

	private static final int INITIAL_BID = 2;
	private static final int HARDCODE = 0x358eba3;
	public static int blockRound = 1;
	private static int c = 0;
	public static Transaction[][] trans = new Transaction[1000][7];
	private static int seed;

	public static void getBlocks() throws GameActionException {
		// received all new messages
		while (blockRound < turnCount) {
			Transaction[] messages = rc.getBlock(blockRound);
			blockRound++;
			trans[c] = messages;
			for (int i = 0; i < messages.length; i++) {
				int[] temp_msg = messages[i].getMessage();
				//System.out.println("New Message: " + Arrays.toString(messages[i].getMessage()));
				if (temp_msg[0] == 6969 && (temp_msg[1] * HARDCODE) % 0x69696969 == temp_msg[2]) {
					// read seed
					seed = temp_msg[1];
					System.out.println("Seed: " + Integer.toString(seed));
				} else {
					// read messages
					int key = xorKey(blockRound);
					for (int j = 0; j < temp_msg.length; j++) {
						temp_msg[j] ^= key;
						if ((temp_msg[j] & 0xff) == 0x69) {
							// good message
							temp_msg[j] >>= 8;
						}
						System.out.println("Received message: " + Integer.toString(temp_msg[j]));
					}
				}
			}
			c++;
		}
	}

	public static int xorKey(int round) {
		// totally bad LCG
		return ((int)Math.pow(3, round) * seed) % 0x69696969;
	}

	public static void addMessage(int[] msg, int length, int bid) throws GameActionException {
		int key = xorKey(turnCount);
		for (int i = 0; i < length; i++) {
			msg[i] = ((msg[i] << 8) | 0x69) ^ key;
		}

		System.out.println("Turn key: " + Integer.toString(turnCount) + " " + Integer.toString(key));

		if (rc.canSubmitTransaction(msg, bid)) {
			System.out.println("Submit message: ");
			rc.submitTransaction(msg, bid);
		}
	}

	public static void setSeed() throws GameActionException {
		int total = 0;
		MapLocation cur_loc = rc.getLocation();
		for (int i = 0; i < directions.length; i++) {
			total += rc.senseSoup(cur_loc.add(directions[i])) << (i);
		}
		int[] msg = {6969, (total / 7), ((total / 7) * HARDCODE) % 0x69696969};
		if (rc.canSubmitTransaction(msg, INITIAL_BID)) {
			System.out.println("Submit seed bid");
			rc.submitTransaction(msg, INITIAL_BID);
		}
	}
}