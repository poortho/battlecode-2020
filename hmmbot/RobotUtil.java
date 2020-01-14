package hmmbot;

import battlecode.common.*;

import java.util.function.Function;

public class RobotUtil {
    RobotController rc;

    GameState gameState;
    CommunicationManager comms;

    boolean bugClockwise;

    final boolean DEBUG_MODE = true;

    int lastRound = -1;
    int birthRound = -1;

    public RobotUtil(RobotController rc) {
        this.rc = rc;
        this.gameState = new GameState(rc.getMapWidth(), rc.getMapHeight());
        this.comms = new CommunicationManager(this.rc, this.gameState);

        this.bugClockwise = rc.getID() % 2 == 0;

        this.log(this.bugClockwise ? "Clockwise!" : "Counterclockwise!");
    }

    public void waitCooldown() throws GameActionException {
        while (!this.rc.isReady()) {
            yield();
        }
    }

    public void preTurn() throws GameActionException {
        this.comms.catchUp();
        if (lastRound != -1) {
            if (rc.getRoundNum() != lastRound + 1) {
                this.log("TURN WAS SKIPPED -- BYTECODE OVERFLOW???\n");
            }
        } else {
            birthRound = lastRound;
        }
        lastRound = rc.getRoundNum();
    }

    public void yield() throws GameActionException {
        Clock.yield();
        this.preTurn();
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
            boolean floodLike = false;
            if (rc.getType() != RobotType.DELIVERY_DRONE) {
                if (rc.canSenseLocation(toLoc) && rc.senseFlooding(toLoc)) {
                    floodLike = true;
                }
                for (Direction testDir : Direction.allDirections()) {
                    if (rc.canSenseLocation(toLoc.add(dir)) && rc.senseFlooding(toLoc.add(dir))) {
                        floodLike = true;
                    }
                }
            }
            if (!rc.canMove(dir) || floodLike) {
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
        return new MapLocation((int) (Math.random() * rc.getMapWidth()), (int) (Math.random() * rc.getMapHeight()));
    }

    public MapLocation randomLocation(int w, int h, MapLocation c) {
        // todo: replace hardcode 64 with getMapWidth/getMapHeight
        return new MapLocation((int) (Math.random() * w) + c.x - w/2, (int) (Math.random() * h) + c.y - h/2);
    }

    public RobotInfo closestRobot(RobotInfo[] robots, RobotType type, boolean sameTeam) {
        RobotInfo closest = null;
        int closestDist = 1 << 30;
        for (RobotInfo info : robots) {
            if ((type == null || info.type == type) && (sameTeam == (info.team == rc.getTeam()))) {
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

    public boolean isGoodPosition(MapLocation loc) {
        if (distanceLinf(this.gameState.hqLocation, loc) <= 2) {
            return true;
        }
        for (Direction baseDir : Direction.allDirections()) {
            MapLocation center = new MapLocation(this.gameState.hqLocation.x + baseDir.dx * 6,this.gameState.hqLocation.y + baseDir.dy * 6);
            int dx = Math.abs(center.x - loc.x);
            int dy = Math.abs(center.y - loc.y);
            if (dx == 2 && dy == 1 || dx == 1 && dy == 2) {
                return true;
            }
        }
        return false;
    }

    public MapLocation closestCenter(MapLocation loc) {
        int closestDist = 1 << 30;
        MapLocation closest = null;
        for (Direction baseDir : Direction.allDirections()) {
            MapLocation center = new MapLocation(this.gameState.hqLocation.x + baseDir.dx * 6,this.gameState.hqLocation.y + baseDir.dy * 6);
            if (closest == null || distanceLinf(center, loc) < closestDist) {
                closest = center;
                closestDist = distanceLinf(center, loc);
            }
        }
        return closest;
    }
}
