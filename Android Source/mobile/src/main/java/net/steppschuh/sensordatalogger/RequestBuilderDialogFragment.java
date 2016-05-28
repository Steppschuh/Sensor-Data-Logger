package net.steppschuh.sensordatalogger;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.widget.FrameLayout;
import android.widget.ListView;

import com.google.android.gms.wearable.Node;

import net.steppschuh.datalogger.MobileApp;
import net.steppschuh.datalogger.message.MessageHandler;
import net.steppschuh.datalogger.message.SinglePathMessageHandler;
import net.steppschuh.datalogger.sensor.DeviceSensor;
import net.steppschuh.datalogger.sensor.DeviceSensors;
import net.steppschuh.datalogger.ui.UnitHelper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RequestBuilderDialogFragment extends DialogFragment {

    public static final String TAG = RequestBuilderDialogFragment.class.getSimpleName();

    public interface RequestBuilderDialogListener {
        public void onDialogPositiveClick(DialogFragment dialog);

        public void onDialogNegativeClick(DialogFragment dialog);
    }

    public interface AvailableSensorsUpdatedListener {
        public void onAvailableSensorsUpdated(String nodeId, List<DeviceSensor> deviceSensors);
    }

    MobileApp app;
    RequestBuilderDialogListener listener;

    private Map<String, Node> availableNodes = new HashMap<>();
    private Map<String, List<DeviceSensor>> availableSensors = new HashMap<>();
    private SensorListAdapter multiChoiceAdapter;
    private MessageHandler setSensorsMessageHandler = getSetSensorsMessageHandler();

    private List<AvailableSensorsUpdatedListener> availableSensorsUpdatedListeners = new ArrayList<>();

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        app = (MobileApp) activity.getApplicationContext();
        app.registerMessageHandler(setSensorsMessageHandler);
        try {
            listener = (RequestBuilderDialogListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString() + " must implement RequestBuilderDialogListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        app.unregisterMessageHandler(setSensorsMessageHandler);
    }

    @Override
    public void onStart() {
        super.onStart();
        String localNodeId = app.getGoogleApiMessenger().getLocalNodeId();
        showSensorSelectionForNode(localNodeId);
    }

    public void requestAvailableSensors(String nodeId, AvailableSensorsUpdatedListener availableSensorsUpdatedListener) {
        Log.d(TAG, "Requesting available sensors on node: " + nodeId);
        try {
            if (!availableSensorsUpdatedListeners.contains(availableSensorsUpdatedListener)) {
                availableSensorsUpdatedListeners.add(availableSensorsUpdatedListener);
            }
            app.getGoogleApiMessenger().sendMessageToNode(MessageHandler.PATH_GET_SENSORS, "", nodeId);
        } catch (Exception e) {
            Log.d(TAG, "Unable to request available sensors on node: " + nodeId);
            e.printStackTrace();
        }
    }

    public void showSensorSelectionForNode(String nodeId) {
        Log.d(TAG, "Showing sensor selection for node: " + nodeId);
        getDialog().setTitle(getString(R.string.loading_available_sensors));
        List<DeviceSensor> availableDeviceSensors = availableSensors.get(nodeId);
        if (availableDeviceSensors == null) {
            availableDeviceSensors = new ArrayList<>();
        }
        multiChoiceAdapter = new SensorListAdapter(availableDeviceSensors, getActivity());
        ListView listView = ((AlertDialog) getDialog()).getListView();
        listView.setAdapter(multiChoiceAdapter);

        requestAvailableSensors(nodeId, new AvailableSensorsUpdatedListener() {
            @Override
            public void onAvailableSensorsUpdated(String nodeId, List<DeviceSensor> deviceSensors) {
                Log.d(TAG, nodeId + " updated, " + deviceSensors.size() + " sensor(s) available");
                if (multiChoiceAdapter != null) {
                    multiChoiceAdapter.setSensors(deviceSensors);
                    multiChoiceAdapter.notifyDataSetChanged();
                    getDialog().setTitle(getString(R.string.available_sensors));
                } else {
                    Log.w(TAG, "Sensor selection list adapter is null");
                    getDialog().setTitle(getString(R.string.no_sensors_available));
                }
            }
        });

        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
        );
        params.setMargins((int) UnitHelper.convertDpToPixel(16, getActivity()), 0, 0, 0);
        listView.setLayoutParams(params);
        listView.invalidate();
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(R.string.loading_available_sensors);

        final CharSequence[] availableSensors = new CharSequence[0];
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
        return builder.create();
    }

    private MessageHandler getSetSensorsMessageHandler() {
        return new SinglePathMessageHandler(MessageHandler.PATH_SET_SENSORS) {
            @Override
            public void handleMessage(Message message) {
                try {
                    String sourceNodeId = MessageHandler.getSourceNodeIdFromMessage(message);
                    String json = MessageHandler.getDataFromMessageAsString(message);
                    DeviceSensors deviceSensors = DeviceSensors.fromJson(json);
                    availableSensors.put(sourceNodeId, deviceSensors.getNonWakeupSensors());

                    for (AvailableSensorsUpdatedListener availableSensorsUpdatedListener : availableSensorsUpdatedListeners) {
                        availableSensorsUpdatedListener.onAvailableSensorsUpdated(sourceNodeId, availableSensors.get(sourceNodeId));
                    }
                } catch (Exception ex) {
                    Log.w(TAG, "Unable to set available device sensors: " + ex.getMessage());
                    ex.printStackTrace();
                }
            }
        };
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

    public Map<String, List<DeviceSensor>> getAvailableSensors() {
        return availableSensors;
    }

    public void setAvailableSensors(Map<String, List<DeviceSensor>> availableSensors) {
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

    public void setAvailableSensorsForNode(String nodeId, List<DeviceSensor> sensors) {
        if (nodeId == null || sensors == null) {
            return;
        }
        availableSensors.put(nodeId, sensors);
    }

}
