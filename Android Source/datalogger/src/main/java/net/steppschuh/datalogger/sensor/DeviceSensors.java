package net.steppschuh.datalogger.sensor;

import android.hardware.Sensor;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class DeviceSensors implements Serializable {

    List<DeviceSensor> sensors;

    public DeviceSensors() {
        sensors = new ArrayList<>();
    }

    public DeviceSensors(List<DeviceSensor> sensors) {
        this();
        this.sensors.addAll(sensors);
    }

    public DeviceSensors(List<Sensor> hardwareSensors, boolean includeWakeUpSensors) {
        this();
        for (Sensor sensor : hardwareSensors) {
            DeviceSensor deviceSensor = new DeviceSensor(sensor);
            if (!includeWakeUpSensors && deviceSensor.isWakeUpSensor()) {
                continue;
            }
            sensors.add(deviceSensor);
        }
    }

    @JsonIgnore
    public List<DeviceSensor> getNonWakeupSensors() {
        return filterWakeUpSensors(sensors);
    }

    public static List<DeviceSensor> filterWakeUpSensors(List<DeviceSensor> sensors) {
        List<DeviceSensor> nonWakeUpSensors = new ArrayList<>();
        for (DeviceSensor sensor : sensors) {
            if (sensor.isWakeUpSensor()) {
                continue;
            }
            nonWakeUpSensors.add(sensor);
        }
        return nonWakeUpSensors;
    }

    @JsonIgnore
    @Override
    public String toString() {
        return toJson();
    }

    @JsonIgnore
    public String toJson() {
        String jsonData = null;
        try {
            ObjectMapper mapper = new ObjectMapper();
            mapper.enable(SerializationFeature.INDENT_OUTPUT);
            mapper.disable(SerializationFeature.FAIL_ON_EMPTY_BEANS);
            jsonData = mapper.writeValueAsString(this);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return jsonData;
    }

    public static DeviceSensors fromJson(String json) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            mapper.disable(SerializationFeature.FAIL_ON_EMPTY_BEANS);
            DeviceSensors deviceSensors = mapper.readValue(json, DeviceSensors.class);
            return deviceSensors;
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return null;
    }

    /**
     * Getter & Setter
     */
    public List<DeviceSensor> getSensors() {
        return sensors;
    }

    public void setSensors(List<DeviceSensor> sensors) {
        this.sensors = sensors;
    }
}
