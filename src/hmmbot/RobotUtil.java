package hmmbot;

import battlecode.common.*;

import java.util.function.Function;

public class RobotUtil {
    RobotController rc;
    boolean bugClockwise;

    final boolean DEBUG_MODE = true;

    public RobotUtil(RobotController rc) {
        this.rc = rc;
        this.bugClockwise = rc.getID() % 2 == 0;

        this.log(this.bugClockwise ? "Clockwise!" : "Counterclockwise!");
    }

    public void waitCooldown() {
        while (!this.rc.isReady()) {
            Clock.yield();
        }
    }

    public MapLocation tryBuild(RobotType type, Direction dir) throws GameActionException {
        MapLocation buildLoc = rc.getLocation().add(dir);
        if (this.rc.canBuildRobot(type, dir) && (!this.rc.canSenseLocation(buildLoc) || !this.rc.senseFlooding(buildLoc))) {
            this.rc.buildRobot(type, dir);
            return buildLoc;
        }
        return null;
    }

    public MapLocation tryBuild(RobotType type) throws GameActionException {
        for (Direction dir : Direction.allDirections()) {
            MapLocation tryLoc = this.tryBuild(type, dir);
            if (tryLoc != null) {
                return tryLoc;
            }
        }
        return null;
    }

    public boolean broadcast(RobotMessage message, int txfee) throws GameActionException {
        int[] encodedMessage = message.encode();
        if (!this.rc.canSubmitTransaction(encodedMessage, txfee))
            return false;
        this.rc.submitTransaction(encodedMessage, txfee);
        return true;
    }

    public RobotInfo[] seeRobots() {
        return rc.senseNearbyRobots();
    }

    public MapLocation seeSoup() throws GameActionException {
        MapLocation bestSoupLoc = null;
        int bestSoupDist = 1 << 30;
        for (int i = -5; i < 6; i++) {
            for (int j = -5; j < 6; j++) {
                int x = rc.getLocation().x + i;
                int y = rc.getLocation().y + j;
                MapLocation loc = new MapLocation(x, y);
                int dist = RobotUtil.distanceLinf(rc.getLocation(), loc);
                if (dist >= bestSoupDist) {
                    continue;
                }
                if (rc.canSenseLocation(loc)) {
                    if (rc.senseSoup(loc) > 0) {
                        bestSoupDist = dist;
                        bestSoupLoc = loc;
                    }
                }
            }
        }
        return bestSoupLoc;
    }

    public static int distanceLinf(MapLocation from, MapLocation to) {
        return Math.max(Math.abs(to.x - from.x), Math.abs(to.y - from.y));
    }

    public static Direction clockwise(Direction dir) {
        switch (dir) {
            case NORTH:
                return Direction.NORTHEAST;
            case NORTHEAST:
                return Direction.EAST;
            case EAST:
                return Direction.SOUTHEAST;
            case SOUTHEAST:
                return Direction.SOUTH;
            case SOUTH:
                return Direction.SOUTHWEST;
            case SOUTHWEST:
                return Direction.WEST;
            case WEST:
                return Direction.NORTHWEST;
            case NORTHWEST:
                return Direction.NORTH;
            default:
                return Direction.CENTER;
        }
    }

    public static Direction counterclockwise(Direction dir) {
        switch (dir) {
            case NORTHEAST:
                return Direction.NORTH;
            case EAST:
                return Direction.NORTHEAST;
            case SOUTHEAST:
                return Direction.EAST;
            case SOUTH:
                return Direction.SOUTHEAST;
            case SOUTHWEST:
                return Direction.SOUTH;
            case WEST:
                return Direction.SOUTHWEST;
            case NORTHWEST:
                return Direction.WEST;
            case NORTH:
                return Direction.NORTHWEST;
            default:
                return Direction.CENTER;
        }
    }

    public boolean moveTowards(MapLocation loc, Function<Void, Void> callback, boolean bugPath) throws GameActionException {
        MapLocation currentLocation = rc.getLocation();
        if (loc.x == currentLocation.x && loc.y == currentLocation.y)
            return false;
        Direction origDir = deltaToDirection(loc.x - currentLocation.x, loc.y - currentLocation.y);
        Direction dir = origDir;
        do {
            MapLocation toLoc = currentLocation.add(dir);
            if (!rc.canMove(dir) || (rc.canSenseLocation(toLoc) && rc.senseFlooding(toLoc) && rc.getType() != RobotType.DELIVERY_DRONE)) {
                if (!bugPath) {
                    break;
                }
                if (bugClockwise) {
                    dir = clockwise(dir);
                } else {
                    dir = counterclockwise(dir);
                }
                continue;
            }
            rc.move(dir);
            return true;
        } while (dir != origDir);
        return false;
    }
    public boolean moveTowards(MapLocation loc, Function<Void, Void> callback) throws GameActionException {
        return moveTowards(loc, callback, true);
    }

    public boolean moveTowardsBounded(MapLocation loc, Function<Void, Void> callback, boolean bugPath, MapLocation center, int distance) throws GameActionException {
        MapLocation currentLocation = rc.getLocation();
        if (loc.x == currentLocation.x && loc.y == currentLocation.y)
            return false;
        Direction origDir = deltaToDirection(loc.x - currentLocation.x, loc.y - currentLocation.y);
        Direction dir = origDir;
        do {
            MapLocation toLoc = currentLocation.add(dir);
            if (!rc.canMove(dir) || (rc.canSenseLocation(toLoc) && rc.senseFlooding(toLoc)) || distanceLinf(center, toLoc) > distance) {
                if (!bugPath) {
                    break;
                }
                if (bugClockwise) {
                    dir = clockwise(dir);
                } else {
                    dir = counterclockwise(dir);
                }
                continue;
            }
            rc.move(dir);
            return true;
        } while (dir != origDir);
        return false;
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

    public MapLocation randomLocation() {
        // todo: replace hardcode 64 with getMapWidth/getMapHeight
        return new MapLocation((int) (Math.random() * 64), (int) (Math.random() * 64));
    }

    public RobotInfo closestRobot(RobotInfo[] robots, RobotType type, boolean sameTeam) {
        RobotInfo closest = null;
        int closestDist = 1 << 30;
        for (RobotInfo info : robots) {
            if (info.type == type && (!sameTeam || info.team == rc.getTeam())) {
                int dist = distanceLinf(rc.getLocation(), info.location);
                if (dist < closestDist) {
                    closest = info;
                    closestDist = dist;
                }
            }
        }
        return closest;
    }

    public void log(String s) {
        if (DEBUG_MODE)
            System.out.println(s);
    }
}
