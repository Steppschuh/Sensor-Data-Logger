package net.steppschuh.datalogger.sensor;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.util.Log;

import net.steppschuh.datalogger.data.Data;
import net.steppschuh.datalogger.data.DataBatch;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SensorDataManager {

    public static final String TAG = SensorDataManager.class.getSimpleName();

    private SensorManager sensorManager;

    private Map<Integer, DataBatch> sensorDataBatches;
    private Map<Integer, SensorEventListener> sensorEventListeners;

    public SensorDataManager(Context context) {
        sensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        initializeSensorEventListeners();
        initializeSensorDataBatches();
    }

    private void initializeSensorEventListeners() {
        sensorEventListeners = new HashMap<>();
    }

    private void initializeSensorDataBatches() {
        sensorDataBatches = new HashMap<>();
        List<Sensor> availableSensors = sensorManager.getSensorList(Sensor.TYPE_ALL);
        for (Sensor sensor : availableSensors) {
            sensorDataBatches.put(sensor.getType(), getDataBatch(sensor.getType()));
        }
    }

    public void registerSensorEventListener(int sensorType) {
        registerSensorEventListener(sensorManager.getDefaultSensor(sensorType));
    }

    public void registerSensorEventListener(Sensor sensor) {
        if (sensor == null) {
            Log.w(TAG, "Unable to register null sensor");
            return;
        }

        if (hasRegisteredSensorEventListener(sensor.getType())) {
            return;
        }
        Log.v(TAG, "Registering sensor event listener for " + sensor.getType() + " - " + sensor.getName());
        sensorManager.registerListener(getSensorEventListener(sensor.getType()), sensor, SensorManager.SENSOR_DELAY_FASTEST);
    }

    public void unregisterAllSensorEventListeners() {
        List<Sensor> availableSensors = sensorManager.getSensorList(Sensor.TYPE_ALL);
        for (Sensor sensor : availableSensors) {
            unregisterSensorEventListener(sensor.getType());
        }
    }

    public void unregisterSensorEventListener(int sensorType) {
        if (hasRegisteredSensorEventListener(sensorType)) {
            Log.v(TAG, "Unregistering sensor event listener for " + sensorType);
            sensorManager.unregisterListener(sensorEventListeners.get(sensorType));
            sensorEventListeners.put(sensorType, null);
        }
    }

    public boolean hasRegisteredSensorEventListener(int sensorType) {
        return sensorEventListeners.get(sensorType) != null;
    }

    public SensorEventListener getSensorEventListener(int sensorType) {
        SensorEventListener sensorEventListener = sensorEventListeners.get(sensorType);
        if (sensorEventListener == null) {
            sensorEventListener = createSensorEventListener(sensorType);
            sensorEventListeners.put(sensorType, sensorEventListener);
        }
        return sensorEventListener;
    }

    private SensorEventListener createSensorEventListener(final int sensorType) {
        return new SensorEventListener() {
            @Override
            public void onSensorChanged(SensorEvent event) {
                float[] values = new float[event.values.length];
                System.arraycopy(event.values, 0, values, 0, event.values.length);
                Data data = new Data(event.sensor.getName(), values);
                getDataBatch(sensorType).addData(data);
            }

            @Override
            public void onAccuracyChanged(Sensor sensor, int accuracy) {

            }
        };
    }

    public DataBatch getDataBatch(int sensorType) {
        DataBatch dataBatch = sensorDataBatches.get(sensorType);
        if (dataBatch == null) {
            dataBatch = createDataBatch(sensorType);
            sensorDataBatches.put(sensorType, dataBatch);
        }
        return dataBatch;
    }

    private DataBatch createDataBatch(int sensorType) {
        Sensor sensor = sensorManager.getDefaultSensor(sensorType);
        if (sensor == null) {
            return null;
        }
        String sensorName = sensor.getName();
        DataBatch dataBatch = new DataBatch(sensorName);
        dataBatch.setType(sensorType);
        return dataBatch;
    }

    /**
     * Getter & Setter
     */
    public SensorManager getSensorManager() {
        return sensorManager;
    }

    public void setSensorManager(SensorManager sensorManager) {
        this.sensorManager = sensorManager;
    }

    public Map<Integer, DataBatch> getSensorDataBatches() {
        return sensorDataBatches;
    }

    public void setSensorDataBatches(Map<Integer, DataBatch> sensorDataBatches) {
        this.sensorDataBatches = sensorDataBatches;
    }

    public Map<Integer, SensorEventListener> getSensorEventListeners() {
        return sensorEventListeners;
    }

    public void setSensorEventListeners(Map<Integer, SensorEventListener> sensorEventListeners) {
        this.sensorEventListeners = sensorEventListeners;
    }
}
