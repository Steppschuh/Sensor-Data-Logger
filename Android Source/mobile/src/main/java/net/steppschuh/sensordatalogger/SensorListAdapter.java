package net.steppschuh.sensordatalogger;

import android.content.Context;
import android.hardware.Sensor;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.FrameLayout;
import android.widget.LinearLayout;

import net.steppschuh.datalogger.ui.UnitHelper;

import java.util.List;

public class SensorListAdapter extends BaseAdapter {

    private Context context;
    private List<Sensor> sensors;

    public SensorListAdapter(List<Sensor> sensors, Context context) {
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

    private String generateStringFromSensor(Sensor sensor) {
        //return sensor.getName() + " (" + sensor.getVendor() + ")";
        return sensor.getName();
    }
}
