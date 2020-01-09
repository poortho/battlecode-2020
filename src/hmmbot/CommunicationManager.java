package hmmbot;

import battlecode.common.GameActionException;
import battlecode.common.RobotController;
import battlecode.common.Transaction;

import static hmmbot.RobotMessage.*;

public class CommunicationManager {

    int hq_location_x = -1, hq_location_y = -1;

    int lastBlockRead = -1;

    public CommunicationManager() {

    }

    public void processBlock(int i, Transaction[] block) {
        for (Transaction t : block) {
            RobotMessage m = RobotMessage.decode(t.getMessage());
            if (m != null) {
                processMessage(m);
            }
        }
    }

    public void processMessage(RobotMessage message) {
        switch (message.messageType) {
            case MESSAGE_TYPE_HQ_LOCATION:
                this.hq_location_x = message.body[0];
                this.hq_location_y = message.body[1];
                break;
            default:
                System.out.printf("IDK how to deal with messages of type %d\n", message.messageType);
        }
    }

    public static RobotMessage generateHqLocationMessage(int x, int y) {
        RobotMessage message = new RobotMessage();
        message.messageType = MESSAGE_TYPE_HQ_LOCATION;
        message.body[0] = x;
        message.body[1] = y;
        return message;
    }

    public void catchUp(RobotController rc) throws GameActionException {
        while (lastBlockRead < rc.getRoundNum()) {
            this.processBlock(lastBlockRead + 1, rc.getBlock(lastBlockRead + 1));
            lastBlockRead++;
        }
    }
}
