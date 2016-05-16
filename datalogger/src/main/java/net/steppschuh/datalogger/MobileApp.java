package net.steppschuh.datalogger;

import android.app.Activity;
import android.app.Application;
import android.os.Build;
import android.os.Bundle;
import android.os.Message;
import android.util.Log;

import com.google.android.gms.wearable.MessageApi;
import com.google.android.gms.wearable.MessageEvent;

import net.steppschuh.datalogger.logging.TrackerManager;
import net.steppschuh.datalogger.message.GoogleApiMessenger;
import net.steppschuh.datalogger.message.MessageHandler;
import net.steppschuh.datalogger.message.MessageReceiver;
import net.steppschuh.datalogger.message.PingMessageHandler;

import java.util.ArrayList;
import java.util.List;

public class MobileApp extends Application implements MessageApi.MessageListener {

    public static final String TAG = "DataLogger";

    private boolean initialized = false;
    private Activity contextActivity;

    private GoogleApiMessenger googleApiMessenger;
    List<MessageHandler> messageHandlers;

    private TrackerManager trackerManager;

    public void initialize(Activity contextActivity) {
        this.contextActivity = contextActivity;

        setupGoogleApis();
        setupTrackingManager();

        messageHandlers = new ArrayList<>();
        registerMessageHandler(new PingMessageHandler(googleApiMessenger));

        initialized = true;
    }

    private void setupGoogleApis() {
        Log.d(TAG, "Setting up Google APIs");
        googleApiMessenger = new GoogleApiMessenger(this);
        googleApiMessenger.connect();
    }

    private void setupTrackingManager() {
        Log.d(TAG, "Setting up Tracking manager");
        trackerManager = new TrackerManager();
    }

    public boolean registerMessageHandler(MessageHandler messageHandler) {
        if (!messageHandlers.contains(messageHandler)) {
            return messageHandlers.add(messageHandler);
        }
        return false;
    }

    public boolean unregisterMessageHandler(MessageHandler messageHandler) {
        if (!messageHandlers.contains(messageHandler)) {
            return messageHandlers.remove(messageHandler);
        }
        return false;
    }

    public void notifyMessageHandlers(Message message) {
        int matchingHandlersCount = 0;
        String path = MessageHandler.getPathFromMessage(message);
        for (MessageHandler messageHandler : messageHandlers) {
            try {
                if (messageHandler.shouldHandleMessage(path)) {
                    messageHandler.handleMessage(message);
                    matchingHandlersCount += 1;
                }
            } catch (Exception ex) {
                Log.w(TAG, "Message handler is unable to handle message: " + ex.getMessage());
            }
        }
        if (matchingHandlersCount == 0) {
            Log.w(TAG, "No message handler available that can handle message: " + path);
        }
    }

    /**
     * Will be called from the Message API if a connected Google API node
     * sent a message to this device
     */
    @Override
    public void onMessageReceived(MessageEvent messageEvent) {
        // convert MessageEvent to data bundle
        Bundle data = new Bundle();
        data.putString(MessageHandler.KEY_PATH, messageEvent.getPath());
        data.putString(MessageHandler.KEY_SOURCE_NODE_ID, messageEvent.getSourceNodeId());
        data.putByteArray(MessageHandler.KEY_DATA, messageEvent.getData());

        // forward message to handlers
        Message message = new Message();
        message.setData(data);

        notifyMessageHandlers(message);
    }

    /**
     * Getter & Setter
     */
    public boolean isInitialized() {
        return initialized;
    }

    public void setInitialized(boolean initialized) {
        this.initialized = initialized;
    }

    public Activity getContextActivity() {
        return contextActivity;
    }

    public void setContextActivity(Activity contextActivity) {
        this.contextActivity = contextActivity;
    }

    public GoogleApiMessenger getGoogleApiMessenger() {
        return googleApiMessenger;
    }

    public void setGoogleApiMessenger(GoogleApiMessenger googleApiMessenger) {
        this.googleApiMessenger = googleApiMessenger;
    }

    public List<MessageHandler> getMessageHandlers() {
        return messageHandlers;
    }

    public void setMessageHandlers(List<MessageHandler> messageHandlers) {
        this.messageHandlers = messageHandlers;
    }

    public TrackerManager getTrackerManager() {
        return trackerManager;
    }

    public void setTrackerManager(TrackerManager trackerManager) {
        this.trackerManager = trackerManager;
    }
}
