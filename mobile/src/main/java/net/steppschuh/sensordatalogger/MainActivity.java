package net.steppschuh.sensordatalogger;

import android.hardware.Sensor;
import android.os.Build;
import android.os.Bundle;
import android.os.Message;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.GridView;
import android.widget.TextView;

import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.Wearable;

import net.steppschuh.datalogger.data.DataBatch;
import net.steppschuh.datalogger.data.DataRequest;
import net.steppschuh.datalogger.data.DataRequestResponse;
import net.steppschuh.datalogger.data.SensorDataRequest;
import net.steppschuh.datalogger.logging.TimeTracker;
import net.steppschuh.datalogger.logging.TrackerManager;
import net.steppschuh.datalogger.message.MessageHandler;
import net.steppschuh.datalogger.message.SinglePathMessageHandler;
import net.steppschuh.datalogger.status.ActivityStatus;
import net.steppschuh.datalogger.status.Status;
import net.steppschuh.datalogger.status.StatusUpdateHandler;
import net.steppschuh.datalogger.status.StatusUpdateReceiver;
import net.steppschuh.sensordatalogger.visualization.VisualizationCardData;
import net.steppschuh.sensordatalogger.visualization.VisualizationCardListAdapter;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = MainActivity.class.getSimpleName();

    private PhoneApp app;
    private List<MessageHandler> messageHandlers;
    private ActivityStatus status = new ActivityStatus();
    private StatusUpdateHandler statusUpdateHandler;

    private FloatingActionButton floatingActionButton;
    private TextView logTextView;
    private GridView gridView;

    private SensorDataRequest sensorDataRequest;
    private VisualizationCardListAdapter cardListAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // get reference to global application
        app = (PhoneApp) getApplicationContext();

        // initialize with context activity if needed
        if (!app.getStatus().isInitialized() || app.getContextActivity() == null) {
            app.initialize(this);
        }

        setupUi();
        setupMessageHandlers();
        setupStatusUpdates();

        status.setInitialized(true);
        status.updated(statusUpdateHandler);
    }

    private void setupUi() {
        setContentView(R.layout.activity_main);

        floatingActionButton = (FloatingActionButton) findViewById(R.id.floadtingActionButton);
        floatingActionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                /*
                List<Node> lastConnectedNodes = ((GoogleApiStatus) app.getGoogleApiMessenger().getStatus()).getLastConnectedNodes();
                Log.d(TAG, "Starting connection speed test with " + lastConnectedNodes.size() + " connected node(s)");
                startConnectionSpeedTest();
                */

                //requestStatusUpdateFromConnectedNodes();
                startRequestingSensorEventData();

                //cardListAdapter.add(new VisualizationCardData("Test"));
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
        messageHandlers.add(getEchoMessageHandler());
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

        // start data request
        startRequestingSensorEventData();
    }

    @Override
    protected void onStop() {
        // stop data request
        stopRequestingSensorEventData();

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

    private MessageHandler getEchoMessageHandler() {
        return new SinglePathMessageHandler(MessageHandler.PATH_ECHO) {
            @Override
            public void handleMessage(Message message) {
                TimeTracker tracker = app.getTrackerManager().getTracker(TrackerManager.KEY_CONNECTION_SPEED_TEST);
                tracker.stop();

                int trackingCount = tracker.getTrackingCount();

                if (trackingCount < 25) {
                    startConnectionSpeedTest();
                } else {
                    stopConnectionSpeedTest();
                }
            }
        };
    }

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
            public void handleMessage(Message message) {
                String sourceNodeId = MessageHandler.getSourceNodeIdFromMessage(message);
                String responseJson = MessageHandler.getDataFromMessageAsString(message);

                DataRequestResponse response = DataRequestResponse.fromJson(responseJson);
                for (DataBatch dataBatch : response.getDataBatches()) {
                    renderDataBatch(dataBatch, sourceNodeId);
                }
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

    private SensorDataRequest createSensorDataRequest() {
        List<Integer> sensorTypes = new ArrayList<>();
        sensorTypes.add(Sensor.TYPE_ACCELEROMETER);

        String localNodeId = app.getGoogleApiMessenger().getLocalNodeId();
        SensorDataRequest sensorDataRequest = new SensorDataRequest(localNodeId, sensorTypes);
        sensorDataRequest.setUpdateInteval(50);
        return sensorDataRequest;
    }


    private boolean isRequestingSensorEventData() {
        if (sensorDataRequest == null) {
            return false;
        }
        if (sensorDataRequest.getEndTimestamp() != DataRequest.TIMESTAMP_NOT_SET) {
            return false;
        }
        return true;
    }

    private void startRequestingSensorEventData() {
        try {
            if (isRequestingSensorEventData()) {
                Log.v(TAG, "Starting to request sensor event data");
            } else {
                Log.v(TAG, "Updating sensor event data request");
            }
            sensorDataRequest = createSensorDataRequest();
            app.getGoogleApiMessenger().sendMessageToNearbyNodes(MessageHandler.PATH_SENSOR_DATA_REQUEST, sensorDataRequest.toString());
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private void stopRequestingSensorEventData() {
        if (!isRequestingSensorEventData()) {
            return;
        }
        try {
            Log.v(TAG, "Stopping to request sensor event data");
            sensorDataRequest.setEndTimestamp(System.currentTimeMillis());
            app.getGoogleApiMessenger().sendMessageToNearbyNodes(MessageHandler.PATH_SENSOR_DATA_REQUEST, sensorDataRequest.toString());
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private void renderDataBatch(DataBatch dataBatch, String sourceNodeId) {
        // get the visualization card
        Node sourceNode = app.getGoogleApiMessenger().getLastConnectedNodeById(sourceNodeId);
        String key = VisualizationCardData.generateKey(sourceNode.getDisplayName(), dataBatch.getSource());
        VisualizationCardData visualizationCardData = cardListAdapter.getVisualizationCard(key);

        // create a new card if not yet avaialable
        if (visualizationCardData == null) {
            visualizationCardData = new VisualizationCardData(key);
            visualizationCardData.setHeading(dataBatch.getSource());
            visualizationCardData.setSubHeading(sourceNode.getDisplayName());
            cardListAdapter.add(visualizationCardData);
            cardListAdapter.notifyDataSetChanged();
        }

        // update the card data
        DataBatch visualizationDataBatch = visualizationCardData.getDataBatch();
        if (visualizationDataBatch == null) {
            visualizationDataBatch = dataBatch;
            visualizationCardData.setDataBatch(visualizationDataBatch);
        } else {
            visualizationDataBatch.addData(dataBatch.getDataList());
        }
        cardListAdapter.invalidateVisualization(visualizationCardData.getKey());
    }

    private void startConnectionSpeedTest() {
        app.getTrackerManager().getTracker("Connection Speed Test").start();
        try {
            Log.v(TAG, "Sending a ping to connected nodes");
            app.getGoogleApiMessenger().sendMessageToNearbyNodes(MessageHandler.PATH_PING, Build.MODEL);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private void stopConnectionSpeedTest() {
        TimeTracker tracker = app.getTrackerManager().getTracker(TrackerManager.KEY_CONNECTION_SPEED_TEST);
        Log.d(TAG, tracker.toString());
        app.getTrackerManager().getTimeTrackers().remove(tracker);
    }

}
