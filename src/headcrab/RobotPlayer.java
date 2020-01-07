package headcrab;

import battlecode.common.*;

import java.awt.*;
import java.util.ArrayList;

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
                        runHQ();
                        break;
                    case MINER:
                        runMiner();
                        break;
                    case REFINERY:
                        runRefinery();
                        break;
                    case VAPORATOR:
                        runVaporator();
                        break;
                    case DESIGN_SCHOOL:
                        runDesignSchool();
                        break;
                    case FULFILLMENT_CENTER:
                        runFulfillmentCenter();
                        break;
                    case LANDSCAPER:
                        runLandscaper();
                        break;
                    case DELIVERY_DRONE:
                        runDeliveryDrone();
                        break;
                    case NET_GUN:
                        runNetGun();
                        break;
                }

                // Clock.yield() makes the robot wait until the next turn, then it will perform this loop again
                Clock.yield();

            } catch (Exception e) {
                System.out.println(rc.getType() + " Exception");
                e.printStackTrace();
                while (true) {
                    Clock.yield();
                }
            }
        }
    }

    static void runHQ() throws GameActionException {
        if (rc.getRobotCount() < 4) {
            for (Direction dir : directions)
                tryBuild(RobotType.MINER, dir);
        }
    }

    static Direction deltaToDirection(int dx, int dy) {
        if (dx != 0)
            dx = dx / Math.abs(dx);
        if (dy != 0)
            dy = dy / Math.abs(dy);
        switch (dx * 3 + dy) {
            case 0 * 3 + 0:
                return Direction.CENTER;
            case 0 * 3 - 1:
                return Direction.SOUTH;
            case 0 * 3 + 1:
                return Direction.NORTH;
            case -1 * 3 - 1:
                return Direction.SOUTHWEST;
            case -1 * 3 + 0:
                return Direction.WEST;
            case -1 * 3 + 1:
                return Direction.NORTHWEST;
            case 1 * 3 - 1:
                return Direction.SOUTHEAST;
            case 1 * 3 + 0:
                return Direction.EAST;
            case 1 * 3 + 1:
                return Direction.NORTHEAST;
            default:
                throw new RuntimeException("invalid dx dy" + dx + dy); // should be impossible
        }
    }

    static void runMiner() throws GameActionException {
        MapLocation targetLocation = new MapLocation((int) ((Math.random() + 1) * 64 / 2), (int) ((Math.random() + 1) * 64 / 2));
        System.out.printf("New target: %d %d\n", targetLocation.x, targetLocation.y);

        MapLocation currentSoup = null;
        MapLocation hqLoc = null;

        for (RobotInfo info : rc.senseNearbyRobots(1, rc.getTeam())) {
            if (info.type == RobotType.HQ) {
                hqLoc = info.location;
            }
        }

        if (hqLoc == null) {
            throw new RuntimeException("hqloc not found!");
        }

        while (true) {
            if (!rc.isReady())
                continue;

            MapLocation currentLocation = rc.getLocation();
            System.out.printf("currentLocation: %d %d\n", currentLocation.x, currentLocation.y);


            for (Direction direction : directions) {
                if (rc.senseFlooding(currentLocation.translate(direction.dx, direction.dy))) {
                    tryMove();
                    break;
                }
            }

            if (rc.getTeamSoup() >= RobotType.DESIGN_SCHOOL.cost && turnCount > 200 && turnCount < 400) {
                for (Direction dir : directions)
                    tryBuild(RobotType.DESIGN_SCHOOL, dir);
            }

            for (RobotInfo info : rc.senseNearbyRobots(1, rc.getTeam())) {
                if (info.type == RobotType.REFINERY) {
                    while (!rc.isReady())
                        Clock.yield();
                    if (rc.getSoupCarrying() != 0) {
                        rc.depositSoup(deltaToDirection(info.location.x - currentLocation.x, info.location.y - currentLocation.y), rc.getSoupCarrying());
                    }
                }
            }

            if (/*rc.canSenseLocation(hqLoc) && */rc.getTeamSoup() >= RobotType.REFINERY.cost && rc.getSoupCarrying() == RobotType.MINER.soupLimit) {

                for (Direction dir : directions) {
                    if (tryBuild(RobotType.REFINERY, dir)) {
                        break;
                    }
                }
            }

            int dx = 1000000, dy = 1000000;
            if (currentSoup != null) {
                System.out.println("Looking for soup!");
                if (rc.canSenseLocation(currentSoup) && rc.senseSoup(currentSoup) == 0) {
                    currentSoup = null;
                } else {
                    dx = currentSoup.x - currentLocation.x;
                    dy = currentSoup.y - currentLocation.y;
                }
            }

            if (currentSoup != null && Math.abs(dx) <= 1 && Math.abs(dy) <= 1) {
                Direction dir = deltaToDirection(dx, dy);
                if (rc.canMineSoup(dir)) {
                    rc.mineSoup(dir);
                    if (rc.senseSoup(currentSoup) == 0) {
                        currentSoup = null;
                    }
                } else {
                    if (rc.getSoupCarrying() < RobotType.MINER.soupLimit) {
                        throw new RuntimeException("should be able to mine here");
                    }
                    currentSoup = null;
                }
            }

            if (currentSoup == null) {
                dx = targetLocation.x - currentLocation.x;
                dy = targetLocation.y - currentLocation.y;
            }

            System.out.printf("Trying to move: %d %d %s\n", dx, dy, deltaToDirection(dx, dy));

            if (!tryMove(deltaToDirection(dx, dy))) {
                System.out.println("Failed to move!");
                targetLocation = new MapLocation((int) ((Math.random()) * 64), (int) ((Math.random()) * 64));
                System.out.printf("New target: %d %d\n", targetLocation.x, targetLocation.y);
            }

            /*
            if (Math.abs(dx) > Math.abs(dy)) {
                if (!tryMove(dx > 0 ? Direction.EAST : Direction.WEST)) {
                    System.out.println("Failed to move on x axis, trying y");
                    if (!tryMove(dy > 0 ? Direction.NORTH : Direction.SOUTH)) {
                        System.out.println("Failed to move on y axis too, blocking");
                    }
                }
            } else {
                if (!tryMove(dy > 0 ? Direction.NORTH : Direction.SOUTH)) {
                    System.out.println("Failed to move on y axis, trying x");
                    if (!tryMove(dx > 0 ? Direction.EAST : Direction.WEST)) {
                        System.out.println("Failed to move on x axis too, blocking");
                    }
                }
            }

             */


            if (currentSoup == null && rc.getSoupCarrying() < RobotType.MINER.soupLimit) {
                for (int i = -8; i < 8; i++) {
                    for (int j = -8; j < 8; j++) {
                        int x = currentLocation.x + i;
                        int y = currentLocation.y + j;
                        MapLocation loc = new MapLocation(x, y);
                        if (rc.canSenseLocation(loc)) {
                            if (rc.senseSoup(loc) > 0) {
                                currentSoup = loc;
                            }
                        }
                    }
                }
            }



            /*
            // tryBuild(randomSpawnedByMiner(), randomDirection());
            for (Direction dir : directions)
                tryBuild(RobotType.FULFILLMENT_CENTER, dir);
            for (Direction dir : directions)
                if (tryRefine(dir))
                    System.out.println("I refined soup! " + rc.getTeamSoup());
            for (Direction dir : directions)
                if (tryMine(dir))
                    System.out.println("I mined soup! " + rc.getSoupCarrying());

             */

            Clock.yield();
            turnCount += 1;
        }
    }

    static void runRefinery() throws GameActionException {
        // System.out.println("Pollution: " + rc.sensePollution(rc.getLocation()));
    }

    static void runVaporator() throws GameActionException {

    }

    static void runDesignSchool() throws GameActionException {
        for (Direction dir : directions)
            tryBuild(RobotType.LANDSCAPER, dir);
    }

    static void runFulfillmentCenter() throws GameActionException {
        for (Direction dir : directions)
            tryBuild(RobotType.DELIVERY_DRONE, dir);
    }

    static void runLandscaper() throws GameActionException {
        MapLocation targetLocation = new MapLocation((int) ((Math.random() + 1) * 64 / 2), (int) ((Math.random() + 1) * 64 / 2));
        System.out.printf("New target: %d %d\n", targetLocation.x, targetLocation.y);

        MapLocation focusLoc = null;

        boolean hasDirt = false;


        while (true) {
            if (!rc.isReady())
                continue;

            MapLocation currentLocation = rc.getLocation();
            System.out.printf("currentLocation: %d %d\n", currentLocation.x, currentLocation.y);
            System.out.printf("carrying: %d hasDirt: %b\n", rc.getDirtCarrying(), hasDirt);

            int dx = 1000000, dy = 1000000;
            if (focusLoc != null) {
                System.out.printf("Looking for %s!\n", hasDirt ? "dump" : "dig");
                if (rc.canSenseLocation(focusLoc) && rc.senseSoup(focusLoc) == 0 && false) {
                    focusLoc = null;
                } else {
                    dx = focusLoc.x - currentLocation.x;
                    dy = focusLoc.y - currentLocation.y;
                }
            }

            if (focusLoc != null && Math.abs(dx) <= 1 && Math.abs(dy) <= 1) {
                Direction dir = deltaToDirection(dx, dy);
                System.out.printf("Try %s!\n", hasDirt ? "dump" : "dig");
                if (hasDirt == false) {
                    if (rc.canDigDirt(dir) && rc.senseElevation(focusLoc) > 8) {
                        rc.digDirt(dir);
                        if (rc.senseElevation(focusLoc) <= 5 || rc.getDirtCarrying() == RobotType.LANDSCAPER.dirtLimit) {
                            focusLoc = null;
                        }
                    } else {
                        System.out.printf("Can't %s!\n", hasDirt ? "dump" : "dig");
                        focusLoc = null;
                    }
                } else {
                    if (rc.canDepositDirt(dir) && rc.senseElevation(focusLoc) < 0) {
                        rc.depositDirt(dir);
                        if (rc.senseElevation(focusLoc) >= 4 || rc.getDirtCarrying() == 0) {
                            focusLoc = null;
                        }
                    } else {
                        System.out.printf("Can't %s!\n", hasDirt ? "dump" : "dig");
                        focusLoc = null;
                    }

                }

            }

            if (rc.getDirtCarrying() == 0) {
                hasDirt = false;
            } else if (rc.getDirtCarrying() == RobotType.LANDSCAPER.dirtLimit) {
                hasDirt = true;
            }

            if (focusLoc == null) {
                dx = targetLocation.x - currentLocation.x;
                dy = targetLocation.y - currentLocation.y;
            }

            System.out.printf("Trying to move: %d %d %s\n", dx, dy, deltaToDirection(dx, dy));

            if (!tryMove(deltaToDirection(dx, dy))) {
                System.out.println("Failed to move!");
                targetLocation = new MapLocation((int) ((Math.random()) * 64), (int) ((Math.random()) * 64));
                System.out.printf("New target: %d %d\n", targetLocation.x, targetLocation.y);
            }

            /*
            if (Math.abs(dx) > Math.abs(dy)) {
                if (!tryMove(dx > 0 ? Direction.EAST : Direction.WEST)) {
                    System.out.println("Failed to move on x axis, trying y");
                    if (!tryMove(dy > 0 ? Direction.NORTH : Direction.SOUTH)) {
                        System.out.println("Failed to move on y axis too, blocking");
                    }
                }
            } else {
                if (!tryMove(dy > 0 ? Direction.NORTH : Direction.SOUTH)) {
                    System.out.println("Failed to move on y axis, trying x");
                    if (!tryMove(dx > 0 ? Direction.EAST : Direction.WEST)) {
                        System.out.println("Failed to move on x axis too, blocking");
                    }
                }
            }

             */


            if (focusLoc == null) {
                for (int i = -1; i < 2; i++) {
                    for (int j = -1; j < 2; j++) {
                        int x = currentLocation.x + i;
                        int y = currentLocation.y + j;
                        MapLocation loc = new MapLocation(x, y);
                        if (rc.canSenseLocation(loc)) {
                            if (hasDirt == false) {
                                if (rc.senseElevation(loc) > 10) {
                                    focusLoc = loc;
                                }
                            } else {
                                if (rc.senseElevation(loc) < 4) {
                                    focusLoc = loc;
                                }
                            }
                        }
                    }
                }
            }



            /*
            // tryBuild(randomSpawnedByMiner(), randomDirection());
            for (Direction dir : directions)
                tryBuild(RobotType.FULFILLMENT_CENTER, dir);
            for (Direction dir : directions)
                if (tryRefine(dir))
                    System.out.println("I refined soup! " + rc.getTeamSoup());
            for (Direction dir : directions)
                if (tryMine(dir))
                    System.out.println("I mined soup! " + rc.getSoupCarrying());

             */

            Clock.yield();
            turnCount += 1;
        }
    }

    static void runDeliveryDrone() throws GameActionException {
        Team enemy = rc.getTeam().opponent();
        if (!rc.isCurrentlyHoldingUnit()) {
            // See if there are any enemy robots within striking range (distance 1 from lumberjack's radius)
            RobotInfo[] robots = rc.senseNearbyRobots(GameConstants.DELIVERY_DRONE_PICKUP_RADIUS_SQUARED, enemy);

            if (robots.length > 0) {
                // Pick up a first robot within range
                rc.pickUpUnit(robots[0].getID());
                System.out.println("I picked up " + robots[0].getID() + "!");
            }
        } else {
            // No close robots, so search for robots within sight radius
            tryMove(randomDirection());
        }
    }

    static void runNetGun() throws GameActionException {

    }

    /**
     * Returns a random Direction.
     *
     * @return a random Direction
     */
    static Direction randomDirection() {
        return directions[(int) (Math.random() * directions.length)];
    }

    /**
     * Returns a random RobotType spawned by miners.
     *
     * @return a random RobotType
     */
    static RobotType randomSpawnedByMiner() {
        return spawnedByMiner[(int) (Math.random() * spawnedByMiner.length)];
    }

    static boolean tryMove() throws GameActionException {
        for (Direction dir : directions)
            if (tryMove(dir))
                return true;
        return false;
        // MapLocation loc = rc.getLocation();
        // if (loc.x < 10 && loc.x < loc.y)
        //     return tryMove(Direction.EAST);
        // else if (loc.x < 10)
        //     return tryMove(Direction.SOUTH);
        // else if (loc.x > loc.y)
        //     return tryMove(Direction.WEST);
        // else
        //     return tryMove(Direction.NORTH);
    }

    /**
     * Attempts to move in a given direction.
     *
     * @param dir The intended direction of movement
     * @return true if a move was performed
     * @throws GameActionException
     */
    static boolean tryMove(Direction dir) throws GameActionException {
        // System.out.println("I am trying to move " + dir + "; " + rc.isReady() + " " + rc.getCooldownTurns() + " " + rc.canMove(dir));
        if (rc.isReady() && rc.canMove(dir)) {
            if (rc.senseFlooding(rc.adjacentLocation(dir))) {
                return false;
            }
            if (rc.getType() == RobotType.MINER) {
                try {
                    for (Direction direction : directions) {
                        if (rc.senseFlooding(rc.adjacentLocation(dir).translate(direction.dx, direction.dy))) {
                            return false;
                        }
                    }
                } catch (Exception e) {
                }
            }
            rc.move(dir);
            return true;
        } else return false;
    }

    /**
     * Attempts to build a given robot in a given direction.
     *
     * @param type The type of the robot to build
     * @param dir  The intended direction of movement
     * @return true if a move was performed
     * @throws GameActionException
     */
    static boolean tryBuild(RobotType type, Direction dir) throws GameActionException {
        if (rc.isReady() && rc.canBuildRobot(type, dir)) {
            try {
                for (Direction direction : directions) {
                    if (rc.senseFlooding(rc.adjacentLocation(dir).translate(direction.dx, direction.dy))) {
                        return false;
                    }
                }
            } catch (Exception e) {
            }
            rc.buildRobot(type, dir);
            return true;
        } else return false;
    }

    /**
     * Attempts to mine soup in a given direction.
     *
     * @param dir The intended direction of mining
     * @return true if a move was performed
     * @throws GameActionException
     */
    static boolean tryMine(Direction dir) throws GameActionException {
        if (rc.isReady() && rc.canMineSoup(dir)) {
            rc.mineSoup(dir);
            return true;
        } else return false;
    }

    /**
     * Attempts to refine soup in a given direction.
     *
     * @param dir The intended direction of refining
     * @return true if a move was performed
     * @throws GameActionException
     */
    static boolean tryRefine(Direction dir) throws GameActionException {
        if (rc.isReady() && rc.canDepositSoup(dir)) {
            rc.depositSoup(dir, rc.getSoupCarrying());
            return true;
        } else return false;
    }


    static void tryBlockchain() throws GameActionException {
        if (turnCount < 3) {
            int[] message = new int[7];
            for (int i = 0; i < 7; i++) {
                message[i] = 123;
            }
            if (rc.canSubmitTransaction(message, 10))
                rc.submitTransaction(message, 10);
        }
        // System.out.println(rc.getRoundMessages(turnCount-1));
    }
}
