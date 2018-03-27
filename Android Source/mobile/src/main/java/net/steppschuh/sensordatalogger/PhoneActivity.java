package net.steppschuh.sensordatalogger;

import com.google.android.gms.wearable.Wearable;
import com.google.firebase.analytics.FirebaseAnalytics;

import android.app.DialogFragment;
import android.content.DialogInterface;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.GridView;
import android.widget.TextView;

import net.steppschuh.datalogger.data.DataBatch;
import net.steppschuh.datalogger.data.DataChangedListener;
import net.steppschuh.datalogger.data.request.DataRequest;
import net.steppschuh.datalogger.data.request.DataRequestResponse;
import net.steppschuh.datalogger.data.request.SensorDataRequest;
import net.steppschuh.datalogger.logging.TimeTracker;
import net.steppschuh.datalogger.messaging.ReachabilityChecker;
import net.steppschuh.datalogger.messaging.handler.MessageHandler;
import net.steppschuh.datalogger.messaging.handler.SinglePathMessageHandler;
import net.steppschuh.datalogger.sensor.DeviceSensor;
import net.steppschuh.datalogger.status.ActivityStatus;
import net.steppschuh.datalogger.status.Status;
import net.steppschuh.datalogger.status.StatusUpdateHandler;
import net.steppschuh.datalogger.status.StatusUpdateReceiver;
import net.steppschuh.sensordatalogger.ui.SensorSelectionDialogFragment;
import net.steppschuh.sensordatalogger.ui.visualization.VisualizationCardData;
import net.steppschuh.sensordatalogger.ui.visualization.VisualizationCardListAdapter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class PhoneActivity extends AppCompatActivity implements DataChangedListener, ReachabilityChecker.NodeReachabilityUpdateReceiver, SensorSelectionDialogFragment.SelectedSensorsUpdatedListener {

    private static final String TAG = PhoneActivity.class.getSimpleName();

    private static final String KEY_SENSOR_DATA_REQUESTS = "sensorDataRequests";
    private static final String KEY_SELECTED_SENSORS = "selectedSensors";

    private PhoneApp app;
    private List<MessageHandler> messageHandlers;
    private ActivityStatus status = new ActivityStatus();
    private StatusUpdateHandler statusUpdateHandler;

    private FloatingActionButton floatingActionButton;
    private TextView logTextView;
    private GridView gridView;

    private VisualizationCardListAdapter cardListAdapter;
    private SensorSelectionDialogFragment sensorSelectionDialog;

    private Map<String, SensorDataRequest> sensorDataRequests = new HashMap<>();
    private Map<String, List<DeviceSensor>> selectedSensors = new HashMap<>();
    private Map<String, AlertDialog> reachabilityDialogs = new HashMap<>();

    private String lastResponseStatus;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // get reference to global application
        app = (PhoneApp) getApplicationContext();

        // initialize with context activity if needed
        if (!app.getStatus().isInitialized() || app.getContextActivity() == null) {
            app.initialize(this);
        }

        // setup stuff
        setupUi();
        setupMessageHandlers();
        setupStatusUpdates();
        setupAnalytics();
        setupTracking();

        // update status
        status.setInitialized(true);
        status.updated(statusUpdateHandler);

        // avoid empty state
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                if (sensorDataRequests.entrySet().size() == 0) {
                    // currently not requesting any data, open sensor selection
                    showSensorSelectionDialog();
                }
            }
        }, TimeUnit.SECONDS.toMillis(1));
    }

    private void setupUi() {
        setContentView(R.layout.activity_main);

        floatingActionButton = (FloatingActionButton) findViewById(R.id.floadtingActionButton);
        floatingActionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showSensorSelectionDialog();
            }
        });

        logTextView = (TextView) findViewById(R.id.logText);
        gridView = (GridView) findViewById(R.id.gridView);

        List<VisualizationCardData> visualizationCardData = new ArrayList<>();
        cardListAdapter = new VisualizationCardListAdapter(this, R.id.gridView, visualizationCardData);
        gridView.setAdapter(cardListAdapter);
    }

    private void setupMessageHandlers() {
        messageHandlers = new ArrayList<>();
        messageHandlers.add(getSetStatusMessageHandler());
        messageHandlers.add(getSensorDataRequestResponseMessageHandler());
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

        app.getGoogleApiMessenger().getStatusUpdateHandler().registerStatusUpdateReceiver(new StatusUpdateReceiver() {
            @Override
            public void onStatusUpdated(Status status) {
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        checkForConnectedButUnreachableNodes();
                    }
                }, ReachabilityChecker.REACHABILITY_TIMEOUT_DEFAULT);
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

    private void setupTracking() {
        TimeTracker tracker = app.getTrackerManager().getTracker("renderDataBatch");
        tracker.setMaximumTrackingCount(100);
        tracker.registerTrackingListener(new TimeTracker.TrackingListener() {
            @Override
            public void onTrackingFinished(TimeTracker timeTracker) {
                //Log.d(TAG, "Tracking finished: " + timeTracker);
                //Log.d(TAG, "Last response status: " + lastResponseStatus);
                timeTracker.reset();
            }

            @Override
            public void onNewDurationTracked(TimeTracker timeTracker) {

            }
        });
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        try {
            // restore sensor data requests
            sensorDataRequests = (HashMap) savedInstanceState.getSerializable(KEY_SENSOR_DATA_REQUESTS);
            if (sensorDataRequests == null) {
                sensorDataRequests = new HashMap<>();
            }

            // restore selected sensors
            selectedSensors = (HashMap) savedInstanceState.getSerializable(KEY_SELECTED_SENSORS);
            if (selectedSensors == null) {
                selectedSensors = new HashMap<>();
            }
            Log.d(TAG, "Instance state restored");

            // Update end timestamps of data requests for selected sensors.
            // This is required because all requests have been terminated when
            // the activity stopped.
            for (Map.Entry<String, SensorDataRequest> sensorDataRequestEntry : sensorDataRequests.entrySet()) {
                List<DeviceSensor> sensors = selectedSensors.get(sensorDataRequestEntry.getKey());
                if (sensors == null || sensors.size() == 0) {
                    continue;
                }
                sensorDataRequestEntry.getValue().setEndTimestamp(DataRequest.TIMESTAMP_NOT_SET);
            }

            // send all available data requests
            sendSensorEventDataRequests();
        } catch (Exception ex) {
            Log.w(TAG, "Unable to restore instance state: " + ex.getMessage());
            ex.printStackTrace();
        }
        super.onRestoreInstanceState(savedInstanceState);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putSerializable(KEY_SENSOR_DATA_REQUESTS, (HashMap) sensorDataRequests);
        outState.putSerializable(KEY_SELECTED_SENSORS, (HashMap) selectedSensors);
        Log.d(TAG, "Saved instance state");
        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onStart() {
        super.onStart();

        // register message handlers
        for (MessageHandler messageHandler : messageHandlers) {
            app.registerMessageHandler(messageHandler);
        }
        Wearable.MessageApi.addListener(app.getGoogleApiMessenger().getGoogleApiClient(), app);

        // register reachability callback
        app.getReachabilityChecker().registerReachabilityUpdateReceiver(ReachabilityChecker.NODE_ID_ANY, this);

        // update status
        status.setInForeground(true);
        status.updated(statusUpdateHandler);

        // start data request
        sendSensorEventDataRequests();
    }

    @Override
    protected void onResume() {
        super.onResume();

        // update reachabilities of nearby nodes
        app.getReachabilityChecker().checkReachabilities(this);
    }

    @Override
    protected void onStop() {
        // stop data request
        stopRequestingSensorEventData();

        // let other devices know that the app won't be reachable anymore
        app.getGoogleApiMessenger().sendMessageToNearbyNodes(MessageHandler.PATH_CLOSING, Build.MODEL);

        // unregister reachability callback
        app.getReachabilityChecker().unregisterReachabilityUpdateReceiver(this);

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
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        // update grid padding
        int verticalMargin = (int) getResources().getDimension(R.dimen.activity_vertical_margin);
        int horizontalMargin = (int) getResources().getDimension(R.dimen.activity_horizontal_margin);
        gridView.setPadding(horizontalMargin, verticalMargin, horizontalMargin, verticalMargin);
        gridView.invalidate();
    }

    /**
     * Will be called when the reachability of a connected @Node
     * (e.g. wearable device) has changed
     */
    @Override
    public void onReachabilityUpdated(String nodeId, boolean isReachable) {
        Log.d(TAG, "Reachability of " + nodeId + " changed to: " + String.valueOf(isReachable));

        // generate a readable message
        String nodeName = app.getGoogleApiMessenger().getNodeName(nodeId);
        String message = isReachable ? getString(R.string.device_connected) : getString(R.string.device_disconnected);
        message = message.replace("[DEVICENAME]", nodeName);

        // notify the user with a @Snackbar
        View parentLayout = findViewById(android.R.id.content);
        Snackbar.make(parentLayout, message, Snackbar.LENGTH_LONG)
                .setDuration(Snackbar.LENGTH_LONG)
                .show();

        if (isReachable) {
            // update reachability dialog, if any
            AlertDialog reachabilityDialog = reachabilityDialogs.get(nodeId);
            if (reachabilityDialog != null && reachabilityDialog.isShowing()) {
                reachabilityDialog.dismiss();
                showSensorSelectionDialog();
            }
        }

        // re-send available data requests
        sendSensorEventDataRequests();
    }

    /**
     * Will be called when a @Node sends (sensor) @Data to this device
     */
    @Override
    public void onDataChanged(DataBatch dataBatch, String sourceNodeId) {
        renderDataBatch(dataBatch, sourceNodeId);
    }

    /*
     * Message Handlers
     */
    private MessageHandler getSetStatusMessageHandler() {
        return new SinglePathMessageHandler(MessageHandler.PATH_SET_STATUS) {
            @Override
            public void handleMessage(Message message) {
                String sourceNodeId = MessageHandler.getSourceNodeIdFromMessage(message);
                String statusJson = MessageHandler.getDataFromMessageAsString(message);
                Log.d(TAG, "Received status from: " + sourceNodeId + ": " + statusJson);
                logTextView.setText(statusJson);
            }
        };
    }

    private MessageHandler getSensorDataRequestResponseMessageHandler() {
        return new SinglePathMessageHandler(MessageHandler.PATH_SENSOR_DATA_REQUEST_RESPONSE) {
            @Override
            public void handleMessage(final Message message) {
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            // parse response data
                            final String sourceNodeId = MessageHandler.getSourceNodeIdFromMessage(message);
                            final String responseJson = MessageHandler.getDataFromMessageAsString(message);
                            final DataRequestResponse response = DataRequestResponse.fromJson(responseJson);

                            if (response.getDataBatches().size() > 0) {
                                long transmissionDuration = System.currentTimeMillis() - response.getEndTimestamp();
                                TimeTracker tracker = app.getTrackerManager().getTracker("renderDataBatch");
                                tracker.addDuration(TimeUnit.MILLISECONDS.toNanos(transmissionDuration));

                                StringBuilder sb = new StringBuilder();
                                sb.append("First data batch items: ");
                                sb.append(response.getDataBatches().get(0).getDataList().size());
                                sb.append(" / ");
                                sb.append(response.getDataBatches().get(0).getCapacity());

                                sb.append("\nSerialized bytes: ");
                                sb.append(responseJson.getBytes().length);

                                lastResponseStatus = sb.toString();
                            }

                            // render data in UI thread
                            Runnable notifyDataChangedRunnable = new Runnable() {
                                @Override
                                public void run() {
                                    for (DataBatch dataBatch : response.getDataBatches()) {
                                        onDataChanged(dataBatch, sourceNodeId);
                                    }
                                }
                            };
                            new Handler(Looper.getMainLooper()).post(notifyDataChangedRunnable);
                        } catch (Exception ex) {
                            ex.printStackTrace();
                        }
                    }
                }).start();
            }
        };
    }

    private void requestStatusUpdateFromConnectedNodes() {
        try {
            Log.v(TAG, "Sending a status update request");
            app.getGoogleApiMessenger().sendMessageToNearbyNodes(MessageHandler.PATH_GET_STATUS, "");
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    /**
     * Will create and show a dialog that allows the user to check the @DeviceSensor
     * on each connected node that he wants to stream
     */
    private void showSensorSelectionDialog() {
        try {
            if (sensorSelectionDialog != null) {
                Log.w(TAG, "Not showing sensor selection dialog, previous dialog is still set");
                return;
            }
            Log.d(TAG, "Showing sensor selection dialog");
            sensorSelectionDialog = new SensorSelectionDialogFragment();
            sensorSelectionDialog.setPreviouslySelectedSensors(selectedSensors);
            sensorSelectionDialog.show(getFragmentManager(), SensorSelectionDialogFragment.class.getSimpleName());
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    /**
     * Will be called when the sensor selection dialog has been closed after
     * sensors from all nodes have been selected
     */
    @Override
    public void onSensorsFromAllNodesSelected(Map<String, List<DeviceSensor>> selectedSensors) {
        Log.d(TAG, "Sensors from all nodes selected");
        this.selectedSensors = selectedSensors;

        // track selected sensors in analytics
        for (Map.Entry<String, List<DeviceSensor>> selectedSensorsEntry : selectedSensors.entrySet()) {
            for (DeviceSensor deviceSensor : selectedSensorsEntry.getValue()) {
                Bundle bundle = new Bundle();
                bundle.putString(FirebaseAnalytics.Param.ITEM_ID, String.valueOf(deviceSensor.getType()));
                bundle.putString(FirebaseAnalytics.Param.ITEM_NAME, deviceSensor.getName());
                bundle.putString(FirebaseAnalytics.Param.ITEM_CATEGORY, deviceSensor.getStringType());
                app.getAnalytics().logEvent(FirebaseAnalytics.Event.VIEW_ITEM, bundle);
            }
        }
    }

    /**
     * Will be called when the sensor selection dialog has shown @DeviceSensor from
     * the specified node
     */
    @Override
    public void onSensorsFromNodeSelected(String nodeId, List<DeviceSensor> sensors) {
        StringBuilder sb = new StringBuilder("Selected sensors for " + nodeId + ":");
        for (DeviceSensor sensor : sensors) {
            sb.append("\n - " + sensor.getName());
        }
        Log.d(TAG, sb.toString());

        selectedSensors.put(nodeId, sensors);

        SensorDataRequest sensorDataRequest = SensorSelectionDialogFragment.createSensorDataRequest(sensors);
        sensorDataRequest.setSourceNodeId(app.getGoogleApiMessenger().getLocalNodeId());

        sensorDataRequests.put(nodeId, sensorDataRequest);

        sendSensorEventDataRequests();
        removeUnneededVisualizationCards();
    }

    /**
     * Will be called if the sensor selection dialog has been closed
     */
    @Override
    public void onSensorSelectionClosed(DialogFragment dialog) {
        Log.d(TAG, "Sensor selection closed");
        sensorSelectionDialog = null;
    }

    /**
     * Returns true if the app is requesting sensor data from
     * the local or any connected device
     */
    private boolean isRequestingSensorEventData() {
        for (Map.Entry<String, SensorDataRequest> sensorDataRequestEntry : sensorDataRequests.entrySet()) {
            if (sensorDataRequestEntry.getValue().getEndTimestamp() == DataRequest.TIMESTAMP_NOT_SET) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns true if the app is requesting sensor data from
     * the device with the specified node id
     */
    private boolean isRequestingSensorEventData(String nodeId) {
        SensorDataRequest request = sensorDataRequests.get(nodeId);
        if (request == null) {
            return false;
        }
        return request.getEndTimestamp() == DataRequest.TIMESTAMP_NOT_SET;
    }

    /**
     * Returns true if the app is requesting data by the specified sensor
     * from the device with the specified node id
     */
    private boolean isRequestingSensorEventData(String nodeId, String sensorName) {
        // check if the request has reached is end timestamp
        if (!isRequestingSensorEventData(nodeId)) {
            return false;
        }

        // check if the current sensor is selected
        boolean sensorIsRequested = false;
        for (DeviceSensor deviceSensor : selectedSensors.get(nodeId)) {
            if (!deviceSensor.getName().equals(sensorName)) {
                continue;
            }
            sensorIsRequested = true;
        }
        return sensorIsRequested;
    }

    /**
     * Sends all available sensor data requests to the assigned nodes
     */
    private void sendSensorEventDataRequests() {
        try {
            Log.v(TAG, "Updating sensor event data request");
            for (Map.Entry<String, SensorDataRequest> sensorDataRequestEntry : sensorDataRequests.entrySet()) {
                sendSensorEventDataRequest(sensorDataRequestEntry.getKey(), sensorDataRequestEntry.getValue());
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }


    private void sendSensorEventDataRequest(String nodeId, SensorDataRequest request) {
        try {
            StringBuilder sb = new StringBuilder("Sending sensor data request to " + nodeId);
            for (Integer sensorType : request.getSensorTypes()) {
                sb.append("\n - " + String.valueOf(sensorType));
            }
            Log.d(TAG, sb.toString());
            app.getGoogleApiMessenger().sendMessageToNode(MessageHandler.PATH_SENSOR_DATA_REQUEST, request.toJson(), nodeId);
        } catch (Exception ex) {
            Log.w(TAG, "Unable to send sensor data request: " + ex.getMessage());
            ex.printStackTrace();
        }
    }

    /**
     * Sets the end timestamps of all available sensor data requests to now
     * and sends them to the assigned nodes
     */
    private void stopRequestingSensorEventData() {
        if (!isRequestingSensorEventData()) {
            return;
        }
        try {
            Log.v(TAG, "Stopping to request sensor event data");
            for (Map.Entry<String, SensorDataRequest> sensorDataRequestEntry : sensorDataRequests.entrySet()) {
                sensorDataRequestEntry.getValue().setEndTimestamp(System.currentTimeMillis());
            }
            sendSensorEventDataRequests();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    /**
     * Creates or updates a visualization card and notifies the @cardListAdapter
     * in order to update the @ChartView with the provided @DataBatch
     */
    private void renderDataBatch(DataBatch dataBatch, String sourceNodeId) {
        try {
            // Don't render if the data isn't requested anymore.
            // This can happen if the request has been updated but data
            // has already been sent by the request receiver
            if (!isRequestingSensorEventData(sourceNodeId, dataBatch.getSource())) {
                return;
            }

            // get the visualization card
            String key = VisualizationCardData.generateKey(sourceNodeId, dataBatch.getSource());
            VisualizationCardData visualizationCardData = cardListAdapter.getVisualizationCard(key);

            // create a new card if not yet avaialable
            if (visualizationCardData == null) {
                String deviceName = app.getGoogleApiMessenger().getNodeName(sourceNodeId);
                visualizationCardData = new VisualizationCardData(key);
                visualizationCardData.setHeading(dataBatch.getSource());
                visualizationCardData.setSubHeading(deviceName);
                cardListAdapter.add(visualizationCardData);
                cardListAdapter.notifyDataSetChanged();
            }

            // update the card data
            DataBatch visualizationDataBatch = visualizationCardData.getDataBatch();
            if (visualizationDataBatch == null) {
                visualizationDataBatch = dataBatch;
                visualizationDataBatch.setCapacity(DataBatch.CAPACITY_UNLIMITED);
                visualizationCardData.setDataBatch(visualizationDataBatch);
            } else {
                visualizationDataBatch.addData(dataBatch.getDataList());
            }

            cardListAdapter.invalidateVisualization(visualizationCardData.getKey());
        } catch (Exception ex) {
            Log.w(TAG, "Unable to render data batch: " + ex.getMessage());
            ex.printStackTrace();
        }
    }

    /**
     * Removes all visualizations from the card list adapter that are currently
     * not requested
     */
    private void removeUnneededVisualizationCards() {
        Map<String, VisualizationCardData> removableVisualizationCards = new HashMap<>();

        Map<String, VisualizationCardData> visualizationCards = cardListAdapter.getVisualizationCards();
        for (Map.Entry<String, VisualizationCardData> visualizationCardDataEntry : visualizationCards.entrySet()) {
            String nodeId = visualizationCardDataEntry.getKey();
            VisualizationCardData visualizationCard = visualizationCardDataEntry.getValue();

            // check if the data that the current card holds should be rendered
            if (!isRequestingSensorEventData(nodeId, visualizationCard.getDataBatch().getSource())) {
                removableVisualizationCards.put(nodeId, visualizationCard);
                continue;
            }
        }

        for (Map.Entry<String, VisualizationCardData> visualizationCardDataEntry : removableVisualizationCards.entrySet()) {
            Log.d(TAG, "Removing unneeded visualization card: " + visualizationCardDataEntry.getValue().getHeading());
            cardListAdapter.remove(visualizationCardDataEntry.getValue());
        }
    }

    /**
     * Checks if there are connected wearables that don't have the app running.
     */
    private void checkForConnectedButUnreachableNodes() {
        Log.d(TAG, "Looking for connected but unreachable nodes");
        List<String> notReachableNodeIds = app.getReachabilityChecker().getNotReachableNodeIds();
        for (String notReachableNodeId : notReachableNodeIds) {
            DataRequest dataRequest = sensorDataRequests.get(notReachableNodeId);
            AlertDialog reachabilityDialog = reachabilityDialogs.get(notReachableNodeId);
            if (dataRequest == null && reachabilityDialog == null) {
                showAppNotRunningDialog(notReachableNodeId);
                return;
            }
        }
    }

    /**
     * Creates and shows a dialog that informs the user that a device is connected
     * but not reachable because the app is currently not running
     */
    private void showAppNotRunningDialog(String nodeId) {
        Log.d(TAG, "Showing app not running dialog");
        String nodeName = app.getGoogleApiMessenger().getNodeName(nodeId);
        AlertDialog reachabilityDialog = new AlertDialog.Builder(this)
                .setTitle(getString(R.string.android_wear_connected))
                .setMessage(getString(R.string.device_connected_but_unreachable).replace("[DEVICENAME]", nodeName))
                .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        app.getGoogleApiMessenger().updateLastConnectedNodes();
                        app.getReachabilityChecker().checkReachabilities(null);
                    }
                })
                .setIcon(R.drawable.ic_watch_black_48dp)
                .create();

        reachabilityDialogs.put(nodeId, reachabilityDialog);
        reachabilityDialog.show();
    }

}
