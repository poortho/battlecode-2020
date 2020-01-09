package hmmbot;

import battlecode.common.GameConstants;
import sun.tools.tree.ThisExpression;


public class RobotMessage {

    public static final int MESSAGE_LENGTH = GameConstants.MAX_BLOCKCHAIN_TRANSACTION_LENGTH;
    public static final int BODY_LENGTH = MESSAGE_LENGTH - 1;

    public static final int MESSAGE_TYPE_UNKNOWN = 0;
    public static final int MESSAGE_TYPE_HQ_LOCATION = 0;


    int messageType;
    int[] body;


    public RobotMessage() {
        this.messageType = MESSAGE_TYPE_UNKNOWN;
        this.body = new int[BODY_LENGTH];
    }



    public RobotMessage(int messageType, int[] body) {
        this.messageType = messageType;
        this.body = body.clone();
    }

    public int[] encode() {
        int[] encoded = new int[MESSAGE_LENGTH];
        encoded[0] = this.messageType;
        System.arraycopy(this.body, 1, encoded, 0, BODY_LENGTH);

        return encoded;
    }

    public static RobotMessage decode(int[] encoded) {
        if (encoded.length != MESSAGE_LENGTH) {
            throw new RuntimeException("Attempt to decode message of incorrect length!");
        }

        int type = encoded[0];

        int[] body = new int[BODY_LENGTH];
        System.arraycopy(encoded, 1, body, 0, BODY_LENGTH);




        RobotMessage message = new RobotMessage(type, body);
        return message;
    }
}
