package net.steppschuh.sensordatalogger;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CheckBox;

import net.steppschuh.datalogger.sensor.DeviceSensor;

import java.util.List;

public class SensorListAdapter extends BaseAdapter {

    private Context context;
    private List<DeviceSensor> sensors;

    public SensorListAdapter(List<DeviceSensor> sensors, Context context) {
        this.sensors = sensors;
        this.context = context;
    }

    @Override
    public int getCount() {
        return sensors.size();
    }

    @Override
    public Object getItem(int position) {
        return sensors.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        convertView = new CheckBox(context);


        ((CheckBox) convertView).setText(generateStringFromSensor(sensors.get(position)));
        return convertView;
    }

    private String generateStringFromSensor(DeviceSensor sensor) {
        //return sensor.getName() + " (" + sensor.getVendor() + ")";
        return sensor.getName();
    }

    public List<DeviceSensor> getSensors() {
        return sensors;
    }

    public void setSensors(List<DeviceSensor> sensors) {
        this.sensors = sensors;
    }
}
