package net.steppschuh.datalogger;

import android.app.Activity;
import android.app.Application;
import android.os.Bundle;
import android.os.Message;
import android.util.Log;

import com.google.android.gms.wearable.MessageApi;
import com.google.android.gms.wearable.MessageEvent;

import net.steppschuh.datalogger.message.GoogleApiMessenger;
import net.steppschuh.datalogger.message.MessageReceiver;

import java.util.ArrayList;
import java.util.List;

public class MobileApp extends Application implements MessageApi.MessageListener, MessageReceiver {

    public static final String TAG = "DataLogger";

    private boolean initialized = false;
    private Activity contextActivity;

    private GoogleApiMessenger googleApiMessenger;
    List<MessageReceiver> messageReceivers;

    public void initialize(Activity contextActivity) {
        this.contextActivity = contextActivity;

        setupGoogleApis();

        messageReceivers = new ArrayList<>();
        messageReceivers.add(this);

        initialized = true;
    }

    private void setupGoogleApis() {
        Log.d(TAG, "Setting up Google APIs");
        googleApiMessenger = new GoogleApiMessenger(this);
        googleApiMessenger.connect();
    }

    public boolean registerMessageReceiver(MessageReceiver messageReceiver) {
        if (!messageReceivers.contains(messageReceiver)) {
            return messageReceivers.add(messageReceiver);
        }
        return false;
    }

    public boolean unregisterMessageReceiver(MessageReceiver messageReceiver) {
        if (!messageReceivers.contains(messageReceiver)) {
            return messageReceivers.remove(messageReceiver);
        }
        return false;
    }

    public void notifyMessageReceivers(Message message) {
        for (MessageReceiver messageReceiver : messageReceivers) {
            try {
                messageReceiver.onMessageReceived(message);
            } catch (Exception ex) {
                Log.w(TAG, "Message receiver is unable to handle message: " + ex.getMessage());
            }
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
        data.putString(SharedConstants.KEY_PATH, messageEvent.getPath());
        data.putByteArray(SharedConstants.KEY_DATA, messageEvent.getData());

        // forward message to receivers
        Message message = new Message();
        message.setData(data);
        notifyMessageReceivers(message);
    }

    @Override
    public void onMessageReceived(Message message) {
        Log.v(TAG, "onMessageReceived");
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

    public List<MessageReceiver> getMessageReceivers() {
        return messageReceivers;
    }

    public void setMessageReceivers(List<MessageReceiver> messageReceivers) {
        this.messageReceivers = messageReceivers;
    }


}
