package net.steppschuh.sensordatalogger;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;

import net.steppschuh.datalogger.sensor.DeviceSensor;

import java.util.ArrayList;
import java.util.List;

public class SensorListAdapter extends BaseAdapter {

    private Context context;
    private List<DeviceSensor> availableSensors;
    private List<DeviceSensor> selectedSensors;

    public SensorListAdapter(List<DeviceSensor> availableSensors, Context context) {
        this.availableSensors = availableSensors;
        this.selectedSensors = new ArrayList<>();
        this.context = context;
    }

    @Override
    public int getCount() {
        return availableSensors.size();
    }

    @Override
    public Object getItem(int position) {
        return availableSensors.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = new CheckBox(context);
        }
        ((CheckBox) convertView).setText(availableSensors.get(position).getName());
        ((CheckBox) convertView).setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                DeviceSensor selectedSensor = availableSensors.get(position);
                if (selectedSensors.contains(selectedSensor) && !isChecked) {
                    selectedSensors.remove(selectedSensor);
                } else if (!selectedSensors.contains(selectedSensor) && isChecked) {
                    selectedSensors.add(selectedSensor);
                }
            }
        });
        return convertView;
    }

    /**
     * Getter & Setter
     */
    public List<DeviceSensor> getAvailableSensors() {
        return availableSensors;
    }

    public void setAvailableSensors(List<DeviceSensor> availableSensors) {
        this.availableSensors = availableSensors;
    }

    public List<DeviceSensor> getSelectedSensors() {
        return selectedSensors;
    }

}
