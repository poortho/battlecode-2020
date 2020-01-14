package hmmbot;

import battlecode.common.*;

import java.util.Map;

import static hmmbot.RobotMessage.*;

public class CommunicationManager {
    RobotController rc;
    GameState gameState;

    public static final int MESSAGE_TYPE_UNKNOWN = 0;
    public static final int MESSAGE_TYPE_HQ_LOCATION = 1;
    public static final int MESSAGE_TYPE_CELL_CLAIM = 2;

    int hq_location_x = -1, hq_location_y = -1;

    int lastBlockRead = 0;

    public CommunicationManager(RobotController rc, GameState gameState) {
        this.rc = rc;
        this.gameState = gameState;
    }

    public void processBlock(int i, Transaction[] block) {
        for (Transaction t : block) {
            RobotMessage m = RobotMessage.decode(getSecret(), t.getMessage());
            if (m != null) {
                processMessage(m);
            }
        }
    }

    public void processMessage(RobotMessage message) {
        switch (message.messageType) {
            case MESSAGE_TYPE_HQ_LOCATION:
                this.gameState.hqLocation = new MapLocation(message.body[0], message.body[1]);
                break;
            case MESSAGE_TYPE_CELL_CLAIM:
                if (this.gameState.cells[message.body[0]][message.body[1]] == 0) {
                    this.gameState.cells[message.body[0]][message.body[1]] = message.body[2];
                }
            default:
                System.out.printf("IDK how to deal with messages of type %d\n", message.messageType);
        }
    }

    public boolean catchUp() throws GameActionException {
        while (lastBlockRead < rc.getRoundNum() - 1) {
            if (Clock.getBytecodesLeft() < 500) {
                return false;
            }
            this.processBlock(lastBlockRead + 1, rc.getBlock(lastBlockRead + 1));
            lastBlockRead++;
        }
        return true;
    }

    public RobotMessage generateHqLocationMessage(int x, int y) {
        RobotMessage message = new RobotMessage();
        message.messageType = MESSAGE_TYPE_HQ_LOCATION;
        message.body[0] = x;
        message.body[1] = y;
        return message;
    }

    public boolean broadcastHqLocation(int x, int y) throws GameActionException {
        if (rc.getTeamSoup() <= 3) {
            return false;
        }

        rc.submitTransaction(this.generateHqLocationMessage(x, y).encode(getSecret()), Math.min(rc.getTeamSoup(), 7));
        return true;
    }

    public RobotMessage generateCellClaimMessage(int x, int y) {
        RobotMessage message = new RobotMessage();
        message.messageType = MESSAGE_TYPE_CELL_CLAIM;
        message.body[0] = x;
        message.body[1] = y;
        message.body[2] = this.rc.getID();
        return message;
    }

    public boolean broadcastCellClaim(int x, int y) throws GameActionException {
        if (rc.getTeamSoup() <= 3) {
            return false;
        }

        rc.submitTransaction(this.generateCellClaimMessage(x, y).encode(getSecret()), Math.min(rc.getTeamSoup(), 7));
        return true;

    }

    public int getSecret() {
        return rc.getTeam() == Team.A ? 1 : 0;
    }
}
