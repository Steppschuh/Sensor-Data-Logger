package net.steppschuh.datalogger.messaging.handler;

import android.os.Message;
import android.util.Log;

import net.steppschuh.datalogger.MobileApp;

public class GetStatusMessageHandler extends SinglePathMessageHandler {

    MobileApp app;

    public GetStatusMessageHandler(MobileApp app) {
        super(PATH_GET_STATUS);
        this.app = app;
    }

    @Override
    public void handleMessage(Message message) {
        try {
            String sourceNodeId = getSourceNodeIdFromMessage(message);
            Log.v(TAG, "Received a status request from " + sourceNodeId + ": " + getDataFromMessageAsString(message));
            String statusJson = app.getStatus().toString();
            app.getGoogleApiMessenger().sendMessageToNode(PATH_SET_STATUS, statusJson, sourceNodeId);
        } catch (Exception ex) {
            Log.w(TAG, "Unable to send status: " + ex.getMessage());
        }
    }

}
