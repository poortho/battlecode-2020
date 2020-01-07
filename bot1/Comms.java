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
	public static int blockRound = 1;
	private static int c = 0;
	public static Transaction[][] trans = new Transaction[1000][7];
	public static void getBlocks() throws GameActionException {
		while (blockRound < turnCount) {
			Transaction[] messages = rc.getBlock(blockRound);
			blockRound++;
			trans[c] = messages;
			for (int i = 0; i < messages.length; i++) {
				System.out.println("New Message: " + Arrays.toString(messages[i].getMessage()));
			}
			c++;
		}
	}

	public static void addMessage(int[] msg) {

	}

	public static void setSeed() throws GameActionException {
		int total = 0;
		MapLocation cur_loc = rc.getLocation();
		for (int i = 0; i < directions.length; i++) {
			total += rc.senseSoup(cur_loc.add(directions[i]));
		}
		int[] msg = {6969, total};
		if (rc.canSubmitTransaction(msg, INITIAL_BID)) {
			System.out.println("Submit bid");
			rc.submitTransaction(msg, INITIAL_BID);
		}
	}
}