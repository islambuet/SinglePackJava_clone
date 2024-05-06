package aclusterllc.singlepack;

import org.json.JSONObject;

public interface ObserverSMMessage {
    public void processSMMessage(JSONObject jsonMessage,JSONObject info);
}
