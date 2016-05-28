package net.steppschuh.sensordatalogger;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Context;
import android.content.DialogInterface;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.FrameLayout;
import android.widget.ListAdapter;
import android.widget.ListView;

import com.google.android.gms.wearable.Node;

import net.steppschuh.datalogger.sensor.SensorDataManager;
import net.steppschuh.datalogger.ui.UnitHelper;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RequestBuilderDialogFragment extends DialogFragment {

    public static final String TAG = RequestBuilderDialogFragment.class.getSimpleName();

    public interface RequestBuilderDialogListener {
        public void onDialogPositiveClick(DialogFragment dialog);

        public void onDialogNegativeClick(DialogFragment dialog);
    }

    RequestBuilderDialogListener listener;

    private Map<String, Node> availableNodes = new HashMap<>();
    private Map<String, List<Sensor>> availableSensors = new HashMap<>();
    private ListAdapter multiChoiceAdapter;

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            listener = (RequestBuilderDialogListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString() + " must implement RequestBuilderDialogListener");
        }
    }

    public void showSensorSelectionForNode(String nodeId) {
        multiChoiceAdapter = createListAdabter(nodeId);
        ListView listView = ((AlertDialog) getDialog()).getListView();
        listView.setAdapter(multiChoiceAdapter);

        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
        );
        params.setMargins((int) UnitHelper.convertDpToPixel(16, getActivity()), 0, 0, 0);
        listView.setLayoutParams(params);

        listView.invalidate();
    }

    private ListAdapter createListAdabter(String nodeId) {
        SensorListAdapter listAdapter = new SensorListAdapter(availableSensors.get(nodeId), getActivity());
        return listAdapter;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(R.string.select_sensors);

        final CharSequence[] availableSensors = new CharSequence[3];
        availableSensors[0] = "Item 0";
        availableSensors[1] = "Item 1";
        availableSensors[2] = "Item 2";

        boolean[] selectedSensors = new boolean[3];
        selectedSensors[0] = false;
        selectedSensors[1] = true;
        selectedSensors[2] = false;

        builder.setMultiChoiceItems(availableSensors, null, new DialogInterface.OnMultiChoiceClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which, boolean isChecked) {
                Log.d(TAG, "Item " + which + " checked: " + String.valueOf(isChecked));
            }
        });

        builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                listener.onDialogPositiveClick(RequestBuilderDialogFragment.this);
            }
        });
        builder.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                listener.onDialogNegativeClick(RequestBuilderDialogFragment.this);
            }
        });

        new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
            @Override
            public void run() {
                SensorManager sensorManager = (SensorManager) getActivity().getSystemService(Context.SENSOR_SERVICE);
                List<Sensor> availableSensors = SensorDataManager.filterWakeUpSensors(sensorManager.getSensorList(Sensor.TYPE_ALL));
                setAvailableSensorsForNode("123", availableSensors);
                showSensorSelectionForNode("123");
            }
        }, 1000);


        return builder.create();
    }

    /**
     * Getter & Setter
     */
    public Map<String, Node> getAvailableNodes() {
        return availableNodes;
    }

    public void setAvailableNodes(Map<String, Node> availableNodes) {
        this.availableNodes = availableNodes;
    }

    public Map<String, List<Sensor>> getAvailableSensors() {
        return availableSensors;
    }

    public void setAvailableSensors(Map<String, List<Sensor>> availableSensors) {
        this.availableSensors = availableSensors;
    }

    public void setAvailableNodes(List<Node> nodes) {
        availableNodes = new HashMap<>();
        for (Node node : nodes) {
            if (node == null) {
                continue;
            }
            availableNodes.put(node.getId(), node);
        }
    }

    public void setAvailableSensorsForNode(String nodeId, List<Sensor> sensors) {
        if (nodeId == null || sensors == null) {
            return;
        }
        availableSensors.put(nodeId, sensors);
    }

}
