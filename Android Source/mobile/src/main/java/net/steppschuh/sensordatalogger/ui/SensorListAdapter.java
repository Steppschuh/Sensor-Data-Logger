package net.steppschuh.sensordatalogger.ui;

import android.content.Context;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ListView;

import net.steppschuh.datalogger.sensor.DeviceSensor;

import java.util.ArrayList;
import java.util.List;

public class SensorListAdapter extends BaseAdapter {

    private Context context;
    private List<DeviceSensor> availableSensors;
    private List<DeviceSensor> selectedSensors;
    private List<DeviceSensor> previouslySelectedSensors;

    public SensorListAdapter(List<DeviceSensor> availableSensors, Context context) {
        this.availableSensors = availableSensors;
        this.selectedSensors = new ArrayList<>();
        this.previouslySelectedSensors = new ArrayList<>();
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

        final DeviceSensor sensor = availableSensors.get(position);
        boolean previouslySelected = wasPreviouslySelected(sensor);

        ((CheckBox) convertView).setText(sensor.getName());

        ((CheckBox) convertView).setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                DeviceSensor selectedSensor = availableSensors.get(position);
                if (selectedSensors.contains(selectedSensor) && !isChecked) {
                    Log.d("Adapter", "Removing " + sensor.getName() + " from selected sensors");
                    selectedSensors.remove(selectedSensor);
                } else if (!selectedSensors.contains(selectedSensor) && isChecked) {
                    Log.d("Adapter", "Adding " + sensor.getName() + " to selected sensors");
                    selectedSensors.add(selectedSensor);
                }
            }
        });

        ((CheckBox) convertView).setChecked(previouslySelected);
        ((ListView) parent).setItemChecked(position, previouslySelected);
        return convertView;
    }

    private boolean wasPreviouslySelected(DeviceSensor sensor) {
        for (DeviceSensor previouslySelectedSensor : previouslySelectedSensors) {
            if (previouslySelectedSensor.getType() != sensor.getType()) {
                continue;
            }
            if (!previouslySelectedSensor.getName().equals(sensor.getName())) {
                continue;
            }
            return true;
        }
        return false;
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

    public List<DeviceSensor> getPreviouslySelectedSensors() {
        return previouslySelectedSensors;
    }

    public void setPreviouslySelectedSensors(List<DeviceSensor> previouslySelectedSensors) {
        this.previouslySelectedSensors = previouslySelectedSensors;
    }
}
