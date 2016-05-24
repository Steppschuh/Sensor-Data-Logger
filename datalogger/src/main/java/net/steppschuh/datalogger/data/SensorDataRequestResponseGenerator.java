package net.steppschuh.datalogger.data;

import android.os.Handler;
import android.util.Log;

import net.steppschuh.datalogger.MobileApp;
import net.steppschuh.datalogger.message.MessageHandler;

import java.util.ArrayList;
import java.util.List;

public class SensorDataRequestResponseGenerator {

    public static final String TAG = SensorDataRequestResponseGenerator.class.getSimpleName();

    private MobileApp app;
    private SensorDataRequest sensorDataRequest;
    private long lastEndTimestamp;
    private Handler updateHandler;
    private Runnable updateRunnable;

    public SensorDataRequestResponseGenerator(MobileApp app) {
        this.app = app;
        lastEndTimestamp = DataRequest.TIMESTAMP_NOT_SET;
        updateRunnable = getDataRequestResponseRunnable();
    }

    public void handleRequest(SensorDataRequest sensorDataRequest) {
        this.sensorDataRequest = sensorDataRequest;

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
        updateHandler = new Handler();
        updateHandler.postDelayed(updateRunnable, 1);
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
        return sensorDataRequest.getEndTimestamp() <= System.currentTimeMillis();
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
                    // generate & send request response
                    DataRequestResponse dataRequestResponse = generateDataRequestResponse();
                    String json = dataRequestResponse.toString();
                    app.getGoogleApiMessenger().sendMessageToNode(MessageHandler.PATH_SENSOR_DATA_REQUEST_RESPONSE, sensorDataRequest.getSourceNodeId(), json);
                } catch (Exception ex) {
                    ex.printStackTrace();
                }

                // re-invoke runnable after delay
                if (isGeneratingRequestResponses() && !shouldStopGeneratingRequestResponses()) {
                    updateHandler.postDelayed(updateRunnable, sensorDataRequest.getUpdateInteval());
                }
            }
        };
    }

    private DataRequestResponse generateDataRequestResponse() {
        Log.v(TAG, "Generating request response for " + sensorDataRequest.getSensorTypes().size() + " sensor(s)");

        // get all required data batches
        List<DataBatch> dataBatches = new ArrayList<>();
        for (Integer sensorType : sensorDataRequest.getSensorTypes()) {
            DataBatch dataBatch = new DataBatch(app.getSensorDataManager().getDataBatch(sensorType));

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
