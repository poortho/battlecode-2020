package hmmbot;

import battlecode.common.*;

public strictfp class RobotPlayer {
    static RobotController rc;

    static Direction[] directions = {Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST,
            Direction.SOUTHEAST, Direction.SOUTHWEST, Direction.NORTHEAST, Direction.NORTHWEST, Direction.CENTER};
    static RobotType[] spawnedByMiner = {RobotType.REFINERY, RobotType.VAPORATOR, RobotType.DESIGN_SCHOOL,
            RobotType.FULFILLMENT_CENTER, RobotType.NET_GUN};

    static int turnCount;

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
        System.out.println("I'm a " + rc.getType() + "! Location " + rc.getLocation());

        while (true) {
            turnCount += 1;
            // Try/catch blocks stop unhandled exceptions, which cause your robot to explode
            try {
                // Here, we've separated the controls into a different method for each RobotType.
                // You can add the missing ones or rewrite this into your own control structure.
                switch (rc.getType()) {
                    case HQ:
                        HQ hq = new HQ(rc);
                        hq.run();
                        break;
                    case MINER:
                        Miner miner = new Miner(rc);
                        miner.run();
                        break;
                    case DESIGN_SCHOOL:
                        DesignSchool designSchool = new DesignSchool(rc);
                        designSchool.run();
                        break;
                    case LANDSCAPER:
                        Landscaper landscaper = new Landscaper(rc);
                        landscaper.run();
                        break;
                    case DELIVERY_DRONE:
                        Drone drone = new Drone(rc);
                        drone.run();
                        break;
                    case FULFILLMENT_CENTER:
                        FulfillmentCenter fulfillmentCenter = new FulfillmentCenter(rc);
                        fulfillmentCenter.run();
                        break;
                    case NET_GUN:
                        NetGun netGun = new NetGun(rc);
                        netGun.run();
                        break;
                    default:
                        while (true) {
                            Clock.yield();
                        }
                }
            } catch (Exception e) {
                System.out.println(rc.getType() + " Exception");
                e.printStackTrace();
                while (true) {
                    Clock.yield();
                }
            }
        }
    }
}
