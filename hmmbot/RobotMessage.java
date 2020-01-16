package hmmbot;

import battlecode.common.GameConstants;
import sun.tools.tree.ThisExpression;


public class RobotMessage {

    public static final int MESSAGE_LENGTH = GameConstants.BLOCKCHAIN_TRANSACTION_LENGTH;
    public static final int BODY_LENGTH = MESSAGE_LENGTH - 1;

    int secret;
    int messageType;
    int[] body;


    public RobotMessage() {
        this.messageType = 0;
        this.body = new int[BODY_LENGTH];
    }



    public RobotMessage(int messageType, int[] body) {
        this.messageType = messageType;
        this.body = body.clone();
    }

    public int[] encode(int secret) {
        int[] encoded = new int[MESSAGE_LENGTH];
        encoded[0] = this.messageType + (secret << 8);
        System.arraycopy(this.body, 0, encoded, 1, BODY_LENGTH);
        System.out.println("encoded message of type " + this.messageType);

        return encoded;
    }

    public static RobotMessage decode(int secret, int[] encoded) {
        if (encoded.length != MESSAGE_LENGTH) {
            throw new RuntimeException("Attempt to decode message of incorrect length!");
        }

        int type = encoded[0] & 0xff;
        int foundSecret = encoded[0] >> 8;
        if (foundSecret != (secret & 0xffffff)) {
            return null;
        }

        int[] body = new int[BODY_LENGTH];
        System.arraycopy(encoded, 1, body, 0, BODY_LENGTH);

        System.out.println("decoded message of type " + type);
        RobotMessage message = new RobotMessage(type, body);
        return message;
    }
}
