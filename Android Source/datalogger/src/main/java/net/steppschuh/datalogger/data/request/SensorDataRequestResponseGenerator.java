package net.steppschuh.datalogger.data.request;

import com.google.android.gms.wearable.Node;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import net.steppschuh.datalogger.MobileApp;
import net.steppschuh.datalogger.data.DataBatch;
import net.steppschuh.datalogger.messaging.GoogleApiMessenger;
import net.steppschuh.datalogger.messaging.handler.MessageHandler;

import java.util.ArrayList;
import java.util.List;

public class SensorDataRequestResponseGenerator {

    public static final String TAG = SensorDataRequestResponseGenerator.class.getSimpleName();

    private static final int MAXIMUM_EXCEPTION_COUNT = 10;

    private MobileApp app;
    private SensorDataRequest sensorDataRequest;
    private long lastEndTimestamp;
    private Handler updateHandler;
    private Runnable updateRunnable;

    private int exceptionCount = 0;

    public SensorDataRequestResponseGenerator(MobileApp app) {
        this.app = app;
        lastEndTimestamp = DataRequest.TIMESTAMP_NOT_SET;
        updateRunnable = getDataRequestResponseRunnable();
    }

    public void handleRequest(SensorDataRequest sensorDataRequest) {
        this.sensorDataRequest = sensorDataRequest;

        Log.v(TAG, "Handling new sensor data request from " + sensorDataRequest.getSourceNodeId());

        if (shouldStopGeneratingRequestResponses()) {
            unregisterRequiredSensorEventListeners();
            stopGeneratingRequestResponses();
        } else {
            registerRequiredSensorEventListeners();
            startGeneratingRequestResponses();
        }
    }

    public void startGeneratingRequestResponses() {
        if (isGeneratingRequestResponses()) {
            return;
        }
        Log.v(TAG, "Starting to generate request responses every " + sensorDataRequest.getUpdateInteval() + "ms");
        if (Looper.myLooper() == null) {
            Looper.prepare();
        }
        updateHandler = new Handler();
        updateHandler.postDelayed(updateRunnable, 1);
        Looper.loop();
    }

    public void stopGeneratingRequestResponses() {
        if (!isGeneratingRequestResponses()) {
            return;
        }
        Log.v(TAG, "Stopping to generate request responses");
        updateHandler.removeCallbacks(updateRunnable);
        updateHandler = null;
    }

    public boolean isGeneratingRequestResponses() {
        return updateHandler != null;
    }

    public boolean shouldStopGeneratingRequestResponses() {
        if (exceptionCount >= MAXIMUM_EXCEPTION_COUNT) {
            return true;
        }
        if (sensorDataRequest.getEndTimestamp() == DataRequest.TIMESTAMP_NOT_SET) {
            return false;
        }
        if (sensorDataRequest.getEndTimestamp() > System.currentTimeMillis()) {
            return false;
        }
        return true;
    }

    private void registerRequiredSensorEventListeners() {
        for (Integer sensorType : sensorDataRequest.getSensorTypes()) {
            app.getSensorDataManager().registerSensorEventListener(sensorType);
        }
    }

    private void unregisterRequiredSensorEventListeners() {
        for (Integer sensorType : sensorDataRequest.getSensorTypes()) {
            app.getSensorDataManager().unregisterSensorEventListener(sensorType);
        }
    }

    private Runnable getDataRequestResponseRunnable() {
        return new Runnable() {
            @Override
            public void run() {
                try {
                    if (!sensorDataRequest.getSourceNodeId().equals(GoogleApiMessenger.DEFAULT_NODE_ID)) {
                        Node sourceNode = app.getGoogleApiMessenger().getLastConnectedNodeById(sensorDataRequest.getSourceNodeId());
                        if (sourceNode == null) {
                            app.getGoogleApiMessenger().updateLastConnectedNodes();
                            throw new Exception("Source node hasn't connected recently");
                        }
                    }

                    // generate & send request response
                    DataRequestResponse dataRequestResponse = generateDataRequestResponse();
                    String json = dataRequestResponse.toString();
                    app.getGoogleApiMessenger().sendMessageToNode(MessageHandler.PATH_SENSOR_DATA_REQUEST_RESPONSE, json, sensorDataRequest.getSourceNodeId());
                    exceptionCount = 0;
                } catch (Exception ex) {
                    exceptionCount += 1;
                    Log.w(TAG, "Unable to send request response: " + ex.getMessage());
                }

                // re-invoke runnable after delay
                if (isGeneratingRequestResponses() && !shouldStopGeneratingRequestResponses()) {
                    updateHandler.postDelayed(updateRunnable, sensorDataRequest.getUpdateInteval());
                } else {
                    stopGeneratingRequestResponses();
                }
            }
        };
    }

    private DataRequestResponse generateDataRequestResponse() {
        // get all required data batches
        List<DataBatch> dataBatches = new ArrayList<>();
        for (Integer sensorType : sensorDataRequest.getSensorTypes()) {
            DataBatch existingDataBatch = app.getSensorDataManager().getDataBatch(sensorType);
            if (existingDataBatch == null) {
                continue;
            }

            DataBatch dataBatch = new DataBatch(existingDataBatch);

            // trim batch to only contain the new data
            dataBatch.setDataList(dataBatch.getDataSince(lastEndTimestamp));
            dataBatches.add(dataBatch);
        }

        // create response object
        DataRequestResponse dataRequestResponse = new DataRequestResponse(dataBatches);
        dataRequestResponse.setStartTimestamp(lastEndTimestamp);
        dataRequestResponse.setEndTimestamp(System.currentTimeMillis());

        // update last used timestamp
        lastEndTimestamp = System.currentTimeMillis();
        return dataRequestResponse;
    }

}
