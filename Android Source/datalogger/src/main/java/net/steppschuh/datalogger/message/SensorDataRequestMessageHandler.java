package net.steppschuh.datalogger.message;

import android.os.Message;
import android.util.Log;

import net.steppschuh.datalogger.MobileApp;
import net.steppschuh.datalogger.data.DataBatch;
import net.steppschuh.datalogger.data.DataRequest;
import net.steppschuh.datalogger.data.DataRequestResponse;
import net.steppschuh.datalogger.data.SensorDataRequest;
import net.steppschuh.datalogger.data.SensorDataRequestResponseGenerator;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SensorDataRequestMessageHandler extends SinglePathMessageHandler {

    MobileApp app;
    private Map<String, SensorDataRequestResponseGenerator> requestResponseGenerators;

    public SensorDataRequestMessageHandler(MobileApp app) {
        super(PATH_SENSOR_DATA_REQUEST);
        this.app = app;
        requestResponseGenerators = new HashMap<>();
    }

    @Override
    public void handleMessage(Message message) {
        try {
            // parse message
            String sourceNodeId = getSourceNodeIdFromMessage(message);
            String dataRequestJson = getDataFromMessageAsString(message);
            Log.v(TAG, "Received a data request from " + sourceNodeId + ": " + dataRequestJson);

            // parse sensor data request
            SensorDataRequest sensorDataRequest = SensorDataRequest.fromJson(dataRequestJson);
            if (sensorDataRequest == null) {
                throw new Exception("Unable to de-serialize request");
            }

            // let a response generator handle the sensor data request
            if (!requestResponseGenerators.containsKey(sourceNodeId)) {
                requestResponseGenerators.put(sourceNodeId, new SensorDataRequestResponseGenerator(app));
            }
            requestResponseGenerators.get(sourceNodeId).handleRequest(sensorDataRequest);
        } catch (Exception ex) {
            Log.w(TAG, "Unable to handle data request: " + ex.getMessage());
            ex.printStackTrace();
        }
    }

}
