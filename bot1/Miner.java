package bot1;
import battlecode.common.*;

public class Miner {

  static Direction[] directions = {Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST};
  static RobotType[] spawnedByMiner = {RobotType.REFINERY, RobotType.VAPORATOR, RobotType.DESIGN_SCHOOL, RobotType.FULFILLMENT_CENTER, RobotType.NET_GUN};
  static RobotController rc;
  static int turnCount;

	static void runMiner() throws GameActionException {

		Helper.tryBlockchain();
		Helper.tryMove(Helper.randomDirection());
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
	}

  static boolean tryMine(Direction dir) throws GameActionException {
    if (rc.isReady() && rc.canMineSoup(dir)) {
        rc.mineSoup(dir);
        return true;
    } else return false;
  }

  static boolean tryDepositSoup(Direction dir) throws GameActionException {
      if (rc.isReady() && rc.canDepositSoup(dir)) {
          rc.depositSoup(dir, rc.getSoupCarrying());
          return true;
      } else return false;
  }
}
