package net.steppschuh.datalogger.messaging.handler;

import android.os.Build;
import android.os.Message;
import android.util.Log;

import net.steppschuh.datalogger.messaging.GoogleApiMessenger;

public class PingMessageHandler extends SinglePathMessageHandler {

    GoogleApiMessenger googleApiMessenger;

    public PingMessageHandler(GoogleApiMessenger googleApiMessenger) {
        super(PATH_PING);
        this.googleApiMessenger = googleApiMessenger;
    }

    @Override
    public void handleMessage(Message message) {
        try {
            Log.v(TAG, "Received a ping from " + getSourceNodeIdFromMessage(message) + ": " + getDataFromMessageAsString(message));
            googleApiMessenger.sendMessageToNearbyNodes(PATH_ECHO, Build.MODEL);
        } catch (Exception ex) {
            Log.w(TAG, "Unable to answer ping");
        }
    }

}
