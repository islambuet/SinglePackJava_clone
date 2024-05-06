package aclusterllc.singlepack;

import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class ClientForSMMessageQueueHandler {
    Logger logger = LoggerFactory.getLogger(ClientForSM.class);
    private final BlockingQueue<JSONObject> messageBuffer = new LinkedBlockingQueue<JSONObject>();
    public void start(){
        new Thread(() -> {
            try {
                processMessageFromBuffer();
            }
            catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }
    public ClientForSMMessageQueueHandler(){

    }
    public void addMessageToBuffer(JSONObject jsonObject){
        try {
            messageBuffer.put(jsonObject);//put waits if buffer full. add throws exception if buffer full
        } catch (InterruptedException ex) {
            logger.error("Waiting Error.");
            logger.error(HelperCommon.getStackTraceString(ex));
        }
    }
    public void processMessageFromBuffer(){
        while (true) {
            try {
                JSONObject jsonMessage = messageBuffer.take();//waits if empty
                ClientForSM clientForSM= (ClientForSM) jsonMessage.get("object");
                clientForSM.processReceivedMessageFromSM(jsonMessage);
            }
            catch (InterruptedException ex) {
                logger.error("Error in Queue take.");
                logger.error(HelperCommon.getStackTraceString(ex));
            }
        }
    }
}
