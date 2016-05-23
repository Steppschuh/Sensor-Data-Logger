package net.steppschuh.datalogger.message;

import android.os.Message;
import android.util.Log;

import net.steppschuh.datalogger.MobileApp;
import net.steppschuh.datalogger.data.DataBatch;
import net.steppschuh.datalogger.data.DataRequest;
import net.steppschuh.datalogger.data.DataRequestResponse;
import net.steppschuh.datalogger.data.SensorDataRequest;

import java.util.ArrayList;
import java.util.List;

public class SensorDataRequestMessageHandler extends SinglePathMessageHandler {

    MobileApp app;
    private SensorDataRequest lastSensorDataRequest;
    private long lastStartTimestamp;

    public SensorDataRequestMessageHandler(MobileApp app) {
        super(PATH_SENSOR_DATA_REQUEST);
        this.app = app;
    }

    @Override
    public void handleMessage(Message message) {
        try {
            String sourceNodeId = getSourceNodeIdFromMessage(message);
            String dataRequestJson = getDataFromMessageAsString(message);
            Log.v(TAG, "Received a data request from " + sourceNodeId + ": " + dataRequestJson);

            lastSensorDataRequest = SensorDataRequest.fromJson(dataRequestJson);
            if (lastSensorDataRequest == null) {
                throw new Exception("Unable to de-serialize request");
            }
            lastStartTimestamp = lastSensorDataRequest.getStartTimestamp();
            for (Integer sensorType : lastSensorDataRequest.getSensorTypes()) {
                app.getSensorDataManager().registerSensorEventListener(sensorType);
            }
        } catch (Exception ex) {
            Log.w(TAG, "Unable to handle data request: " + ex.getMessage());
        }
    }

    private Runnable getDataRequestResponseRunnable() {
        return new Runnable() {
            @Override
            public void run() {
                try {
                    DataRequestResponse dataRequestResponse = generateDataRequestResponse();
                    String json = dataRequestResponse.toString();
                    app.getGoogleApiMessenger().sendMessageToNode(MessageHandler.PATH_SENSOR_DATA_REQUEST_RESPONSE, lastSensorDataRequest.getSourceNodeId(), json);
                    lastStartTimestamp = System.currentTimeMillis();
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        };
    }

    private DataRequestResponse generateDataRequestResponse() {
        if (lastSensorDataRequest == null) {
            return null;
        }

        List<DataBatch> dataBatches = new ArrayList<>();
        for (Integer sensorType : lastSensorDataRequest.getSensorTypes()) {
            DataBatch dataBatch = new DataBatch(app.getSensorDataManager().getDataBatch(sensorType));
            dataBatch.setDataList(dataBatch.getDataSince(lastStartTimestamp));
            dataBatches.add(dataBatch);
        }

        DataRequestResponse dataRequestResponse = new DataRequestResponse(dataBatches);
        dataRequestResponse.setStartTimestamp(lastStartTimestamp);
        dataRequestResponse.setEndTimestamp(System.currentTimeMillis());
        return dataRequestResponse;
    }

}
