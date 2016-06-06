package net.steppschuh.datalogger.messaging.handler;

import android.os.Message;
import android.util.Log;

import net.steppschuh.datalogger.MobileApp;
import net.steppschuh.datalogger.data.request.SensorDataRequest;
import net.steppschuh.datalogger.data.request.SensorDataRequestResponseGenerator;
import net.steppschuh.datalogger.messaging.GoogleApiMessenger;

import java.util.HashMap;
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
            if (sourceNodeId == null) {
                sourceNodeId = GoogleApiMessenger.DEFAULT_NODE_ID;
            }
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
