package net.steppschuh.sensordatalogger;

import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.Wearable;
import com.google.firebase.analytics.FirebaseAnalytics;

import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.hardware.Sensor;
import android.os.Build;
import android.os.Bundle;
import android.os.Message;
import android.support.v4.content.ContextCompat;
import android.support.wearable.activity.WearableActivity;
import android.support.wearable.view.BoxInsetLayout;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import net.steppschuh.datalogger.data.request.DataRequest;
import net.steppschuh.datalogger.data.request.SensorDataRequest;
import net.steppschuh.datalogger.messaging.handler.MessageHandler;
import net.steppschuh.datalogger.messaging.handler.SinglePathMessageHandler;
import net.steppschuh.datalogger.sensor.DeviceSensors;
import net.steppschuh.datalogger.status.ActivityStatus;
import net.steppschuh.datalogger.status.Status;
import net.steppschuh.datalogger.status.StatusUpdateEmitter;
import net.steppschuh.datalogger.status.StatusUpdateHandler;
import net.steppschuh.datalogger.status.StatusUpdateReceiver;

import java.util.ArrayList;
import java.util.List;

public class WearActivity extends WearableActivity implements StatusUpdateEmitter {

    private static final String TAG = WearActivity.class.getSimpleName();

    private WearApp app;

    private List<MessageHandler> messageHandlers;
    private ActivityStatus status = new ActivityStatus();
    private StatusUpdateHandler statusUpdateHandler;

    private BoxInsetLayout mContainerView;
    private TextView mainTextView;
    private TextView preTextView;
    private TextView postTextView;
    private TextView logTextView;

    private String lastConnectedDeviceName;
    private int lastRequestedSensorCount = -1;
    private int lastAvailableSensorCount = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // get reference to global application
        app = (WearApp) getApplicationContext();

        // initialize with context activity if needed
        if (!app.getStatus().isInitialized() || app.getContextActivity() == null) {
            app.initialize(this);
        }

        setupUi();
        setupMessageHandlers();
        setupStatusUpdates();
        setupAnalytics();

        status.setInitialized(true);
        status.updated(statusUpdateHandler);
    }

    private void setupUi() {
        setContentView(R.layout.main_activity_wear);
        setAmbientEnabled();

        mContainerView = (BoxInsetLayout) findViewById(R.id.container);
        mainTextView = (TextView) findViewById(R.id.mainText);
        preTextView = (TextView) findViewById(R.id.preText);
        postTextView = (TextView) findViewById(R.id.postText);
        logTextView = (TextView) findViewById(R.id.logText);

        mContainerView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                updateStatusTexts();
            }
        });

        updateDisplay();
    }

    private void setupMessageHandlers() {
        messageHandlers = new ArrayList<>();
        messageHandlers.add(getSensorDataRequestHandler());
    }

    private void setupStatusUpdates() {
        statusUpdateHandler = new StatusUpdateHandler();
        statusUpdateHandler.registerStatusUpdateReceiver(new StatusUpdateReceiver() {
            @Override
            public void onStatusUpdated(Status status) {
                app.getStatus().setActivityStatus((ActivityStatus) status);
                app.getStatus().updated(app.getStatusUpdateHandler());
            }
        });
    }

    private void setupAnalytics() {
        Bundle bundle = new Bundle();
        try {
            PackageInfo packageInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
            bundle.putString("version_code", String.valueOf(packageInfo.versionCode));
            bundle.putString("version_name", String.valueOf(packageInfo.versionName));
        } catch (PackageManager.NameNotFoundException e) {
            Log.w(TAG, "Unable to get package info");
        }
        app.getAnalytics().logEvent(FirebaseAnalytics.Event.APP_OPEN, bundle);
    }

    @Override
    protected void onStart() {
        super.onStart();
        // register message handlers
        for (MessageHandler messageHandler : messageHandlers) {
            app.registerMessageHandler(messageHandler);
        }
        Wearable.MessageApi.addListener(app.getGoogleApiMessenger().getGoogleApiClient(), app);

        // update status
        status.setInForeground(true);
        status.updated(statusUpdateHandler);

        // update reachabilities of nearby nodes
        app.getReachabilityChecker().checkReachabilities(null);
    }

    @Override
    protected void onStop() {
        // let other devices know that the app won't be reachable anymore
        app.getGoogleApiMessenger().sendMessageToNearbyNodes(MessageHandler.PATH_CLOSING, Build.MODEL);

        // unregister message handlers
        for (MessageHandler messageHandler : messageHandlers) {
            app.unregisterMessageHandler(messageHandler);
        }
        Wearable.MessageApi.removeListener(app.getGoogleApiMessenger().getGoogleApiClient(), app);

        // update status
        status.setInForeground(false);
        status.updated(statusUpdateHandler);
        super.onStop();
    }

    @Override
    public void onEnterAmbient(Bundle ambientDetails) {
        super.onEnterAmbient(ambientDetails);
        updateDisplay();
        status.setAmbientMode(true);
        status.updated(statusUpdateHandler);
    }

    @Override
    public void onUpdateAmbient() {
        super.onUpdateAmbient();
        updateDisplay();
    }

    @Override
    public void onExitAmbient() {
        updateDisplay();
        status.setAmbientMode(false);
        status.updated(statusUpdateHandler);
        super.onExitAmbient();
    }

    private void updateDisplay() {
        if (isAmbient()) {
            mContainerView.setBackgroundColor(Color.BLACK);

            preTextView.setVisibility(View.GONE);
            postTextView.setVisibility(View.GONE);
            logTextView.setVisibility(View.GONE);
        } else {
            mContainerView.setBackgroundColor(ContextCompat.getColor(this, R.color.colorPrimary));

            preTextView.setVisibility(View.VISIBLE);
            postTextView.setVisibility(View.VISIBLE);
            logTextView.setVisibility(View.VISIBLE);
        }
        updateStatusTexts();
    }

    private void updateStatusTexts() {
        String connectedDeviceName = getConnectedDeviceName();
        int availableSensorsCount = getAvailableSensorsCount();
        int registeredSensorsCount = getRegisteredSensorsCount();

        String preText;
        String mainText;
        String postText;
        String logText = String.valueOf(app.getStatus().getLastUpdateTimestamp());

        if (isSendingRequestResponses()) {
            preText = getString(R.string.status_connected_pre);
            preText = preText.replace("[REGISTERED_SENSORS]", String.valueOf(registeredSensorsCount));
            preText = preText.replace("[AVAILABLE_SENSORS]", String.valueOf(availableSensorsCount));
            mainText = getString(R.string.status_connected_main);
            postText = getString(R.string.status_connected_post);
            postText = postText.replace("[DEVICENAME]", connectedDeviceName);
        } else {
            preText = getString(R.string.status_disconnected_pre);
            preText = preText.replace("[AVAILABLE_SENSORS]", String.valueOf(availableSensorsCount));
            mainText = getString(R.string.status_disconnected_main);
            postText = getString(R.string.status_disconnected_post);
        }

        preTextView.setText(preText);
        mainTextView.setText(mainText);
        postTextView.setText(postText);
        logTextView.setText(logText);
    }

    private boolean isSendingRequestResponses() {
        if (!app.getGoogleApiMessenger().getGoogleApiClient().isConnected()) {
            return false;
        }
        if (app.getReachabilityChecker().getReachableNodeIds().size() < 1) {
            return false;
        }
        if (app.getSensorDataManager().getSensorEventListeners().entrySet().size() < 1) {
            return false;
        }
        return true;
    }

    private int getAvailableSensorsCount() {
        if (lastAvailableSensorCount < 0) {
            List<Sensor> availableSensors = app.getSensorDataManager().getSensorManager().getSensorList(Sensor.TYPE_ALL);
            lastAvailableSensorCount = new DeviceSensors(availableSensors, false).getSensors().size();
        }
        return lastAvailableSensorCount;
    }

    private int getRegisteredSensorsCount() {
        if (lastRequestedSensorCount < 0) {
            lastRequestedSensorCount = app.getSensorDataManager().getSensorEventListeners().entrySet().size();
        }
        return lastRequestedSensorCount;
    }

    private String getConnectedDeviceName() {
        if (lastConnectedDeviceName == null) {
            List<Node> lastConnectedNodes = app.getGoogleApiMessenger().getLastConnectedNearbyNodes();
            if (lastConnectedNodes != null && lastConnectedNodes.size() > 0) {
                lastConnectedDeviceName = app.getGoogleApiMessenger().getNodeName(lastConnectedNodes.get(0).getId());
            }
        }
        return lastConnectedDeviceName;
    }

    private MessageHandler getSensorDataRequestHandler() {
        return new SinglePathMessageHandler(MessageHandler.PATH_SENSOR_DATA_REQUEST) {
            @Override
            public void handleMessage(Message message) {
                // parse message
                String sourceNodeId = getSourceNodeIdFromMessage(message);
                String dataRequestJson = getDataFromMessageAsString(message);
                Log.v(TAG, "Received a data request from " + sourceNodeId + ": " + dataRequestJson);

                // update status from sensor data request
                SensorDataRequest sensorDataRequest = SensorDataRequest.fromJson(dataRequestJson);
                if (sensorDataRequest != null) {
                    if (sensorDataRequest.getEndTimestamp() == DataRequest.TIMESTAMP_NOT_SET) {
                        lastRequestedSensorCount = sensorDataRequest.getSensorTypes().size();
                    } else {
                        lastRequestedSensorCount = 0;
                    }
                    lastConnectedDeviceName = app.getGoogleApiMessenger().getNodeName(sensorDataRequest.getSourceNodeId());
                }

                updateStatusTexts();
                logTextView.setText(String.valueOf(System.currentTimeMillis()));
            }
        };
    }

    @Override
    public Status getStatus() {
        return status;
    }

    @Override
    public StatusUpdateHandler getStatusUpdateHandler() {
        return statusUpdateHandler;
    }

}
