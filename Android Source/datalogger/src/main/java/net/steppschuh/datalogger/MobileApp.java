package net.steppschuh.datalogger;

import com.google.android.gms.wearable.MessageApi;
import com.google.android.gms.wearable.MessageEvent;
import com.google.firebase.analytics.FirebaseAnalytics;

import android.app.Activity;
import android.os.Bundle;
import android.os.Message;
import android.support.multidex.MultiDexApplication;
import android.util.Log;

import net.steppschuh.datalogger.logging.TrackerManager;
import net.steppschuh.datalogger.messaging.GoogleApiMessenger;
import net.steppschuh.datalogger.messaging.ReachabilityChecker;
import net.steppschuh.datalogger.messaging.handler.GetAvailableSensorsMessageHandler;
import net.steppschuh.datalogger.messaging.handler.GetStatusMessageHandler;
import net.steppschuh.datalogger.messaging.handler.MessageHandler;
import net.steppschuh.datalogger.messaging.handler.SensorDataRequestMessageHandler;
import net.steppschuh.datalogger.sensor.SensorDataManager;
import net.steppschuh.datalogger.status.AppStatus;
import net.steppschuh.datalogger.status.Status;
import net.steppschuh.datalogger.status.StatusUpdateEmitter;
import net.steppschuh.datalogger.status.StatusUpdateHandler;
import net.steppschuh.datalogger.status.StatusUpdateReceiver;

import java.util.ArrayList;
import java.util.List;

public class MobileApp extends MultiDexApplication implements MessageApi.MessageListener, StatusUpdateEmitter {

    public static final String TAG = "DataLogger";

    private AppStatus status = new AppStatus();
    private StatusUpdateHandler statusUpdateHandler;

    private Activity contextActivity;

    private GoogleApiMessenger googleApiMessenger;
    List<MessageHandler> messageHandlers;

    private TrackerManager trackerManager;
    private SensorDataManager sensorDataManager;
    private ReachabilityChecker reachabilityChecker;
    private FirebaseAnalytics analytics;

    public void initialize(Activity contextActivity) {
        this.contextActivity = contextActivity;

        setupAnalytics();
        setupStatusUpdates();
        setupGoogleApis();
        setupTrackingManager();
        setupMessageHandlers();
        setupSensorDataManager();
        setupReachabilityChecker();

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
        googleApiMessenger.connect();
    }

    private void setupTrackingManager() {
        Log.d(TAG, "Setting up Tracking manager");
        trackerManager = new TrackerManager();
    }

    private void setupMessageHandlers() {
        Log.d(TAG, "Setting up Message handlers");
        messageHandlers = new ArrayList<>();
        registerMessageHandler(new GetStatusMessageHandler(this));
        registerMessageHandler(new SensorDataRequestMessageHandler(this));
        registerMessageHandler(new GetAvailableSensorsMessageHandler(this));
    }

    private void setupSensorDataManager() {
        Log.d(TAG, "Setting up Sensor Data manager");
        sensorDataManager = new SensorDataManager(this);
    }

    private void setupReachabilityChecker() {
        reachabilityChecker = new ReachabilityChecker(this);
        reachabilityChecker.registerMessageHandlers();
    }

    private void setupAnalytics() {
        analytics = FirebaseAnalytics.getInstance(contextActivity);

        //TODO: offer opt-out of analytics
        analytics.setAnalyticsCollectionEnabled(true);
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

        // create new list to avoid concurrent modification
        List<MessageHandler> currentMessageHandlers = new ArrayList<>(messageHandlers);
        for (MessageHandler messageHandler : currentMessageHandlers) {
            try {
                if (messageHandler.shouldHandleMessage(path)) {
                    messageHandler.handleMessage(message);
                    matchingHandlersCount += 1;
                }
            } catch (Exception ex) {
                Log.w(TAG, "Message handler is unable to handle message: " + ex.getMessage());
                ex.printStackTrace();
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

    public ReachabilityChecker getReachabilityChecker() {
        return reachabilityChecker;
    }

    public void setReachabilityChecker(ReachabilityChecker reachabilityChecker) {
        this.reachabilityChecker = reachabilityChecker;
    }

    public FirebaseAnalytics getAnalytics() {
        return analytics;
    }

}
