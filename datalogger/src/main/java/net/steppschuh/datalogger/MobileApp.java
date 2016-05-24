package net.steppschuh.datalogger;

import android.app.Activity;
import android.app.Application;
import android.os.Bundle;
import android.os.Message;
import android.util.Log;

import com.google.android.gms.wearable.MessageApi;
import com.google.android.gms.wearable.MessageEvent;

import net.steppschuh.datalogger.logging.TrackerManager;
import net.steppschuh.datalogger.message.SensorDataRequestMessageHandler;
import net.steppschuh.datalogger.message.GetStatusMessageHandler;
import net.steppschuh.datalogger.message.GoogleApiMessenger;
import net.steppschuh.datalogger.message.MessageHandler;
import net.steppschuh.datalogger.message.PingMessageHandler;
import net.steppschuh.datalogger.sensor.SensorDataManager;
import net.steppschuh.datalogger.status.AppStatus;
import net.steppschuh.datalogger.status.Status;
import net.steppschuh.datalogger.status.StatusUpdateEmitter;
import net.steppschuh.datalogger.status.StatusUpdateHandler;
import net.steppschuh.datalogger.status.StatusUpdateReceiver;

import java.util.ArrayList;
import java.util.List;

public class MobileApp extends Application implements MessageApi.MessageListener, StatusUpdateEmitter {

    public static final String TAG = "DataLogger";

    private AppStatus status = new AppStatus();
    private StatusUpdateHandler statusUpdateHandler;

    private Activity contextActivity;

    private GoogleApiMessenger googleApiMessenger;
    List<MessageHandler> messageHandlers;

    private TrackerManager trackerManager;
    private SensorDataManager sensorDataManager;

    public void initialize(Activity contextActivity) {
        this.contextActivity = contextActivity;

        setupStatusUpdates();
        setupGoogleApis();
        setupTrackingManager();
        setupMessageHandlers();
        setupSensorDataManager();

        status.setInitialized(true);
    }

    private void setupStatusUpdates() {
        statusUpdateHandler = new StatusUpdateHandler();
        statusUpdateHandler.registerStatusUpdateReceiver(new StatusUpdateReceiver() {
            @Override
            public void onStatusUpdated(Status status) {
                //Log.v(TAG, "App Status updated: " + ((AppStatus) status).toString());
            }
        });
    }

    private void setupGoogleApis() {
        Log.d(TAG, "Setting up Google APIs");
        googleApiMessenger = new GoogleApiMessenger(this);
        googleApiMessenger.setupStatusUpdates(this);
        googleApiMessenger.connect();
    }

    private void setupTrackingManager() {
        Log.d(TAG, "Setting up Tracking manager");
        trackerManager = new TrackerManager();
    }

    private void setupMessageHandlers() {
        Log.d(TAG, "Setting up Message handlers");
        messageHandlers = new ArrayList<>();
        registerMessageHandler(new PingMessageHandler(googleApiMessenger));
        registerMessageHandler(new GetStatusMessageHandler(this));
        registerMessageHandler(new SensorDataRequestMessageHandler(this));
    }

    private void setupSensorDataManager() {
        Log.d(TAG, "Setting up Sensor Data manager");
        sensorDataManager = new SensorDataManager(this);
    }

    public boolean registerMessageHandler(MessageHandler messageHandler) {
        if (!messageHandlers.contains(messageHandler)) {
            return messageHandlers.add(messageHandler);
        }
        return false;
    }

    public boolean unregisterMessageHandler(MessageHandler messageHandler) {
        if (messageHandlers.contains(messageHandler)) {
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
    public AppStatus getStatus() {
        return status;
    }

    @Override
    public StatusUpdateHandler getStatusUpdateHandler() {
        return statusUpdateHandler;
    }

    public void setStatus(AppStatus status) {
        this.status = status;
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

    public SensorDataManager getSensorDataManager() {
        return sensorDataManager;
    }

    public void setSensorDataManager(SensorDataManager sensorDataManager) {
        this.sensorDataManager = sensorDataManager;
    }
}
