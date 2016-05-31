package net.steppschuh.datalogger.sensor;

import android.hardware.Sensor;
import android.os.Build;

import java.io.Serializable;

public class DeviceSensor implements Serializable {

    private String name;
    private String vendor;
    private int version;
    private int type;
    private String stringType;
    private int reportingMode;
    private float resolution;
    private float power;
    private boolean isWakeUpSensor;

    public DeviceSensor() {
    }

    public DeviceSensor(Sensor sensor) {
        name = sensor.getName();
        vendor = sensor.getVendor();
        version = sensor.getVersion();
        type = sensor.getType();
        resolution = sensor.getResolution();
        power = sensor.getPower();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT_WATCH) {
            stringType = sensor.getStringType();
        } else {
            stringType = "SENSOR_TYPE_" + type;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            reportingMode = sensor.getReportingMode();
            isWakeUpSensor = sensor.isWakeUpSensor();
        } else {
            reportingMode = 0;
            isWakeUpSensor = sensor.getName().toLowerCase().contains("wake_up");
        }
    }

    /**
     * Getter & Setter
     */
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getVendor() {
        return vendor;
    }

    public void setVendor(String vendor) {
        this.vendor = vendor;
    }

    public boolean isWakeUpSensor() {
        return isWakeUpSensor;
    }

    public void setWakeUpSensor(boolean wakeUpSensor) {
        isWakeUpSensor = wakeUpSensor;
    }

    public int getVersion() {
        return version;
    }

    public void setVersion(int version) {
        this.version = version;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public String getStringType() {
        return stringType;
    }

    public void setStringType(String stringType) {
        this.stringType = stringType;
    }

    public int getReportingMode() {
        return reportingMode;
    }

    public void setReportingMode(int reportingMode) {
        this.reportingMode = reportingMode;
    }

    public float getResolution() {
        return resolution;
    }

    public void setResolution(float resolution) {
        this.resolution = resolution;
    }

    public float getPower() {
        return power;
    }

    public void setPower(float power) {
        this.power = power;
    }
}
