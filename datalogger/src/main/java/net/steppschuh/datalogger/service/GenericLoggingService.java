package net.steppschuh.datalogger.service;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Message;
import android.os.Messenger;
import android.util.Log;

import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.WearableListenerService;

import net.steppschuh.datalogger.SharedConstants;
import net.steppschuh.datalogger.message.GoogleApiMessenger;
import net.steppschuh.datalogger.message.MessageHandler;
import net.steppschuh.datalogger.message.MessageReceiver;

public abstract class GenericLoggingService extends WearableListenerService implements MessageReceiver {

    protected final String TAG = GenericLoggingService.class.getSimpleName();

    // Messengers
    private Messenger serviceToAppMessenger;
    private MessageHandler appToServiceMessageHandler;
    protected GoogleApiMessenger googleApiMessenger;

    private StatusNotification statusNotification;

    @Override
    public void onCreate() {
        super.onCreate();

        appToServiceMessageHandler = new MessageHandler(this);
        statusNotification = new StatusNotification(this, getTargetActivityClass());

        setupGoogleApis();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        setMessengersFromIntent(intent);
        onServiceInvoked();
        return Service.START_NOT_STICKY;
    }

    private void setMessengersFromIntent(Intent intent) {
        Bundle extras = intent.getExtras();
        if (extras != null) {
            if (extras.containsKey(SharedConstants.KEY_MESSENGER)) {
                Log.d(TAG, "Service to app messenger set");

                // App has set a service -> app messenger, save it for later
                serviceToAppMessenger = (Messenger) extras.get(SharedConstants.KEY_MESSENGER);

                // Send a message back and include a app -> service messenger
                Bundle messageBundle = new Bundle();
                messageBundle.putString(SharedConstants.KEY_PATH, SharedConstants.MESSAGE_PATH_SET_MESSENGER);
                messageBundle.putParcelable(SharedConstants.KEY_MESSENGER, new Messenger(appToServiceMessageHandler));
                sendMessageToApp(messageBundle);
            }
        }
    }

    protected void onServiceInvoked() {
        Log.d(TAG, "Logging service invoked on " + android.os.Build.MODEL);
    }

    @Override
    public void onDestroy() {
        Log.w(TAG, "Service destroyed");

        statusNotification.hide();

        super.onDestroy();
    }

    /*
        Setup
    */
    private void setupGoogleApis() {
        Log.d(TAG, "Setting up Google APIs");
        googleApiMessenger = new GoogleApiMessenger(this);
        googleApiMessenger.connect();
    }

    /*
        Messaging
    */
    private void sendMessageToApp(final String path, String data) {
        try {
            Bundle messageBundle = new Bundle();
            messageBundle.putString(SharedConstants.KEY_PATH, path);
            messageBundle.putString(SharedConstants.KEY_DATA, data);
            sendMessageToApp(messageBundle);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private void sendMessageToApp(Bundle messageBundle) {
        Log.v(TAG, "Sending message to app: " + messageBundle.getString(SharedConstants.KEY_PATH));
        try {
            Message message = Message.obtain();
            message.setData(messageBundle);
            if (serviceToAppMessenger != null) {
                serviceToAppMessenger.send(message);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /*
        Handles messages from googleApiMessenger
    */
    @Override
    public void onMessageReceived(MessageEvent messageEvent) {
        Bundle data = new Bundle();
        data.putByteArray(SharedConstants.KEY_DATA, messageEvent.getData());
        data.putBoolean(SharedConstants.KEY_FROM_NODE, true);

        String path = messageEvent.getPath();
        handleMessage(path, data);

        super.onMessageReceived(messageEvent);
    }

    /*
        Handles messages from appToServiceMessageHandler
    */
    @Override
    public void onMessageReceived(Message message) {
        Bundle data = message.getData();
        data.putBoolean(SharedConstants.KEY_FROM_NODE, false);

        String path = data.getString(SharedConstants.KEY_PATH);
        handleMessage(path, data);
    }

    protected void handleMessage(String path, Bundle data) {
        Log.v(TAG, "Received message: " + path);
        switch (path) {
            case SharedConstants.MESSAGE_PATH_GET_LATEST_STATES: {

                break;
            }
            default: {
                Log.w(TAG, "Unable to handle message: " + path);
                break;
            }
        }
    }

    /**
     * Helper
     */
    protected abstract Class getTargetActivityClass();

    private static String getTag(Context context) {
        String tag = "Service";
        if (isRunningOnWear(context)) {
            return "Wear" + tag;
        } else {
            return "Mobile" + tag;
        }
    }

    public static boolean isRunningOnWear(Context context) {
        return context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_WATCH);
    }

}
