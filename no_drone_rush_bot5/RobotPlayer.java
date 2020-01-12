package no_drone_rush_bot5;
import battlecode.common.*;

import java.sql.SQLOutput;

import static no_drone_rush_bot5.RobotPlayer.rc;

public strictfp class RobotPlayer {
    static RobotController rc;

    static RobotType[] spawnedByMiner = {RobotType.REFINERY, RobotType.VAPORATOR, RobotType.DESIGN_SCHOOL,
            RobotType.FULFILLMENT_CENTER, RobotType.NET_GUN};

    static int turnCount;
    static int waterLevel;
    static int round;
    static int[][] netgun_map;

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
        if (rc.getTeam() == Team.A) {
            Comms.HARDCODE = 0xdeadbeef;
        } else {
            Comms.HARDCODE = 0x358eba3;
        }

        while (true) {
            round = rc.getRoundNum();
            // if (round > 20) {
            //     rc.resign();
            // }
            //waterLevel = Helper.water_levels[turnCount];
            turnCount += 1;
            if (turnCount == 1) {
                // set directions array to point outwards
                MapLocation middle = new MapLocation(rc.getMapWidth() / 2, rc.getMapHeight() / 2);
                MapLocation my_loc = rc.getLocation();
                if (my_loc.x < middle.x) {
                    if (my_loc.y < middle.y) {
                        // bottom left
                        Helper.directions = new Direction[]{Direction.NORTH, Direction.NORTHEAST, Direction.EAST, Direction.SOUTHEAST, Direction.SOUTH, Direction.SOUTHWEST, Direction.WEST, Direction.NORTHWEST};
                    } else {
                        // top left
                        Helper.directions = new Direction[]{Direction.SOUTH, Direction.SOUTHEAST, Direction.EAST, Direction.NORTHEAST, Direction.NORTH, Direction.NORTHWEST, Direction.WEST, Direction.SOUTHWEST};
                    }
                } else {
                    if (my_loc.y < middle.y) {
                        // bottom right
                        Helper.directions = new Direction[]{Direction.NORTH, Direction.NORTHWEST, Direction.WEST, Direction.SOUTHWEST, Direction.SOUTH, Direction.SOUTHEAST, Direction.EAST, Direction.NORTHEAST};
                    } else {
                        // top right
                        Helper.directions = new Direction[]{Direction.SOUTH, Direction.SOUTHWEST, Direction.WEST, Direction.NORTHWEST, Direction.NORTH, Direction.NORTHEAST, Direction.EAST, Direction.SOUTHEAST};
                    }
                }

                netgun_map = new int[rc.getMapWidth()][rc.getMapHeight()];
            }
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

                Helper.check_netguns();

                if (rc.getRoundNum() != round) {
                    System.out.println("Ran out of bytecode on turn " + turnCount + ", took " + (rc.getRoundNum() - round) + " turns.");
                }

                Clock.yield();

            } catch (Exception e) {
                System.out.println(rc.getType() + " Exception");
                e.printStackTrace();
            }
        }
    }
}