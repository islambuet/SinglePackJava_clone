package aclusterllc.singlepack;

import org.json.JSONObject;

public interface ObserverHmiMessage {
    public void processHmiMessage(JSONObject jsonMessage,JSONObject info);
}
