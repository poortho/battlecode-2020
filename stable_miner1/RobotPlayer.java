package bot1;
import battlecode.common.*;

public strictfp class RobotPlayer {
    static RobotController rc;

    static RobotType[] spawnedByMiner = {RobotType.REFINERY, RobotType.VAPORATOR, RobotType.DESIGN_SCHOOL,
            RobotType.FULFILLMENT_CENTER, RobotType.NET_GUN};

    static int turnCount;
    static int waterLevel;
    static int round;

    /**
     * run() is the method that is called when a robot is instantiated in the Battlecode world.
     * If this method returns, the robot dies!
     **/
    @SuppressWarnings("unused")
    public static void run(RobotController rc) throws GameActionException {

        // This is the RobotController object. You use it to perform actions from this robot,
        // and to get information on its current status.
        RobotPlayer.rc = rc;

        turnCount = 0;

        while (true) {
            round = rc.getRoundNum();
            // if (round > 20) {
            //     rc.resign();
            // }
            //waterLevel = Helper.water_levels[turnCount];
            turnCount += 1;
            try {
                switch (rc.getType()) {
                    case HQ:                 HQ.runHQ();                break;
                    case MINER:              Miner.runMiner();             break;
                    case REFINERY:           Refinery.runRefinery();          break;
                    case VAPORATOR:          Vaporator.runVaporator();         break;
                    case DESIGN_SCHOOL:      DesignSchool.runDesignSchool();      break;
                    case FULFILLMENT_CENTER: FulfillmentCenter.runFulfillmentCenter(); break;
                    case LANDSCAPER:         Landscaper.runLandscaper();        break;
                    case DELIVERY_DRONE:     DeliveryDrone.runDeliveryDrone();     break;
                    case NET_GUN:            NetGun.runNetGun();            break;
                }

                Clock.yield();

            } catch (Exception e) {
                System.out.println(rc.getType() + " Exception");
                e.printStackTrace();
            }
        }
    }
}