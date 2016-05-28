package net.steppschuh.datalogger.message;

import android.hardware.Sensor;
import android.os.Message;
import android.util.Log;

import net.steppschuh.datalogger.MobileApp;
import net.steppschuh.datalogger.data.SensorDataRequest;
import net.steppschuh.datalogger.data.SensorDataRequestResponseGenerator;
import net.steppschuh.datalogger.sensor.DeviceSensors;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GetAvailableSensorsMessageHandler extends SinglePathMessageHandler {

    MobileApp app;

    public GetAvailableSensorsMessageHandler(MobileApp app) {
        super(PATH_GET_SENSORS);
        this.app = app;
    }

    @Override
    public void handleMessage(Message message) {
        try {
            // parse message
            String sourceNodeId = getSourceNodeIdFromMessage(message);
            Log.v(TAG, "Received a sensor request from " + sourceNodeId);

            // get available sensors
            List<Sensor> hardwareSensors = app.getSensorDataManager().getSensorManager().getSensorList(Sensor.TYPE_ALL);
            DeviceSensors deviceSensors = new DeviceSensors(hardwareSensors, true);

            // respond with device sensors
            String deviceSensorsJson = deviceSensors.toJson();
            app.getGoogleApiMessenger().sendMessageToNode(PATH_SET_SENSORS, deviceSensorsJson, sourceNodeId);
        } catch (Exception ex) {
            Log.w(TAG, "Unable to handle sensor request: " + ex.getMessage());
            ex.printStackTrace();
        }
    }

}
