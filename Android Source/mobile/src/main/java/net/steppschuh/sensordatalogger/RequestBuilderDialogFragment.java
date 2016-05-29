package net.steppschuh.sensordatalogger;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.Button;
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
        void onSensorsFromAllNodesSelected(Map<String, List<DeviceSensor>> selectedSensors);

        void onSensorsFromNodeSelected(String nodeId, List<DeviceSensor> sensors);

        void onSensorSelectionCanceled(DialogFragment dialog);
    }

    public interface AvailableSensorsUpdatedListener {
        void onAvailableSensorsUpdated(String nodeId, List<DeviceSensor> deviceSensors);
    }

    private MobileApp app;
    private RequestBuilderDialogListener listener;

    private Map<String, Node> availableNodes = new HashMap<>();
    private Map<String, List<DeviceSensor>> availableSensors = new HashMap<>();
    private Map<String, List<DeviceSensor>> selectedSensors = new HashMap<>();
    private SensorListAdapter multiChoiceAdapter;
    private MessageHandler setSensorsMessageHandler = getSetSensorsMessageHandler();

    private List<AvailableSensorsUpdatedListener> availableSensorsUpdatedListeners = new ArrayList<>();

    /**
     * Creates the basic alert dialog with a multi-choice list.
     * It will be customized and filled with data from connected device
     * once it's shown
     */
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(R.string.loading_available_sensors);

        final CharSequence[] availableSensors = new CharSequence[0];
        builder.setMultiChoiceItems(availableSensors, null, new DialogInterface.OnMultiChoiceClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which, boolean isChecked) {
                // this will be overwritten to prevent default behaviour
            }
        });
        builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                // this will be overwritten to prevent default behaviour
            }
        });
        builder.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                listener.onSensorSelectionCanceled(RequestBuilderDialogFragment.this);
            }
        });
        return builder.create();
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        app = (MobileApp) activity.getApplicationContext();
        app.registerMessageHandler(setSensorsMessageHandler);
        try {
            listener = (RequestBuilderDialogListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString() + " must implement " + RequestBuilderDialogListener.class.getSimpleName());
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

        // override positive button click listener in order to prevent it from
        // closing the dialog if not all sensors have been selected yet
        final AlertDialog dialog = (AlertDialog) getDialog();
        if (dialog != null) {
            Button positiveButton = dialog.getButton(Dialog.BUTTON_POSITIVE);
            positiveButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    String nodeId = getNextSensorSelectionNodeId();
                    saveCurrentlySelectedSensors(nodeId);
                    listener.onSensorsFromNodeSelected(nodeId, selectedSensors.get(nodeId));

                    if (sensorsFromAllNodesSelected()) {
                        listener.onSensorsFromAllNodesSelected(selectedSensors);
                        dialog.dismiss();
                    } else {
                        showSensorSelectionForNextNode();
                    }
                }
            });
        }

        // pre-fetch available sensors from connected devices
        requestAvailableSensors();

        // load the first list of available sensors
        showSensorSelectionForNextNode();
    }

    /**
     * Updates the existing dialog to show a list of available sensors
     * from the specified node. List items will be set when they are
     * available through an @AvailableSensorsUpdatedListener.
     */
    private void showSensorSelectionForNode(String nodeId) {
        String nodeName = app.getGoogleApiMessenger().getNodeName(nodeId);
        Log.d(TAG, "Showing sensor selection for node: " + nodeName + " - " + nodeId);

        // prepare dialog for new sensor selection
        String title = getString(R.string.loading_sensors_on_device).replace("[DEVICENAME]", nodeName);
        getDialog().setTitle(title);

        // create & apply new list adapter
        multiChoiceAdapter = new SensorListAdapter(new ArrayList<DeviceSensor>(), getActivity());
        ListView listView = ((AlertDialog) getDialog()).getListView();
        listView.setAdapter(multiChoiceAdapter);

        // update layout params & invalidate list view
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
        );
        params.setMargins((int) UnitHelper.convertDpToPixel(16, getActivity()), 0, 0, 0);
        listView.setLayoutParams(params);
        listView.invalidate();

        // update available sensors
        AvailableSensorsUpdatedListener availableSensorsUpdatedListener = createAvailableSensorsUpdatedListener();
        if (availableSensors.containsKey(nodeId)) {
            // set sensors
            availableSensorsUpdatedListener.onAvailableSensorsUpdated(nodeId, availableSensors.get(nodeId));
        } else {
            // request sensors
            requestAvailableSensors(nodeId, availableSensorsUpdatedListener);
        }
    }

    /**
     * Calls @showSensorSelectionForNode with the next node id
     * that has not been shown yet
     */
    private void showSensorSelectionForNextNode() {
        String nextNodeId = getNextSensorSelectionNodeId();
        if (nextNodeId == null) {
            Log.w(TAG, "Sensors for all nodes already selected!");
            return;
        }
        showSensorSelectionForNode(nextNodeId);
    }

    /**
     * Requests a @DeviceSensors object from the specified node and passes
     * it to the specified @AvailableSensorsUpdatedListener.
     */
    private void requestAvailableSensors(String nodeId, AvailableSensorsUpdatedListener availableSensorsUpdatedListener) {
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

    /**
     * Calls @requestAvailableSensors for all connected nearby nodes
     * in order to pre-fetch the available sensors for the dialog.
     */
    private void requestAvailableSensors() {
        Log.d(TAG, "Pre-fetching available sensors from all nearby nodes");
        for (Node node : app.getGoogleApiMessenger().getLastConnectedNearbyNodes()) {
            if (!hasSelectedSensorsForNode(node.getId())) {
                requestAvailableSensors(node.getId(), new AvailableSensorsUpdatedListener() {
                    @Override
                    public void onAvailableSensorsUpdated(String nodeId, List<DeviceSensor> deviceSensors) {
                        Log.d(TAG, "Fetched available sensors on " + nodeId);
                        availableSensors.put(nodeId, deviceSensors);
                    }
                });
            }
        }
    }

    /**
     * Returns the node id of the next device for which the available
     * sensors should be shown & selected by the user.
     * Returns null if sensors for all devices have been shown already.
     */
    private String getNextSensorSelectionNodeId() {
        // check local node
        String localNodeId = app.getGoogleApiMessenger().getLocalNodeId();
        if (!hasSelectedSensorsForNode(localNodeId)) {
            return localNodeId;
        }

        // check already requested available sensors
        for (Map.Entry<String, List<DeviceSensor>> availableSensors : this.availableSensors.entrySet()) {
            if (!hasSelectedSensorsForNode(availableSensors.getKey())) {
                return availableSensors.getKey();
            }
        }

        // check nearby connected nodes
        for (Node node : app.getGoogleApiMessenger().getLastConnectedNearbyNodes()) {
            if (!hasSelectedSensorsForNode(node.getId())) {
                return node.getId();
            }
        }

        return null;
    }

    public boolean sensorsFromAllNodesSelected() {
        return getNextSensorSelectionNodeId() == null;
    }

    public boolean hasSelectedSensorsForNode(String nodeId) {
        return selectedSensors.get(nodeId) != null;
    }

    /**
     * Writes the selected sensors for the specified node id
     * into the @selectedSensors map
     */
    private void saveCurrentlySelectedSensors(String nodeId) {
        Log.d(TAG, "Saving currently selected sensors for " + nodeId);
        List<DeviceSensor> currentlySelectedSensors = multiChoiceAdapter.getSelectedSensors();
        selectedSensors.put(nodeId, currentlySelectedSensors);
    }

    /**
     * Returns an @AvailableSensorsUpdatedListener that will update the
     * dialog's @multiChoiceAdapter in order to render the checkboxes
     * for the available sensors
     */
    private AvailableSensorsUpdatedListener createAvailableSensorsUpdatedListener() {
        return new AvailableSensorsUpdatedListener() {
            @Override
            public void onAvailableSensorsUpdated(String nodeId, List<DeviceSensor> deviceSensors) {
                Log.d(TAG, nodeId + " updated, " + deviceSensors.size() + " sensor(s) available");
                if (multiChoiceAdapter != null) {
                    // update adapter with sensors
                    multiChoiceAdapter.setAvailableSensors(deviceSensors);
                    multiChoiceAdapter.notifyDataSetChanged();

                    // update dialog title
                    getDialog().setTitle(getDialogTitleForAvailableSensors(nodeId));
                } else {
                    Log.w(TAG, "Sensor selection list adapter is null");
                    getDialog().setTitle(getString(R.string.no_sensors_available));
                }

                // unregister this observer
                if (availableSensorsUpdatedListeners.contains(this)) {
                    availableSensorsUpdatedListeners.remove(this);
                }
            }
        };
    }

    /**
     * Returns a readable dialog title based on the specified node id
     */
    private String getDialogTitleForAvailableSensors(String nodeId) {
        String title;
        if (nodeId.equals(app.getGoogleApiMessenger().getLocalNodeId())) {
            // current device
            if (app.getGoogleApiMessenger().getLastConnectedNearbyNodes().size() > 0) {
                title = getString(R.string.available_sensors_on_this_device);
            } else {
                title = getString(R.string.available_sensors);
            }
        } else {
            // connected device
            String nodeName = app.getGoogleApiMessenger().getNodeName(nodeId);
            title = getString(R.string.available_sensors_on_device).replace("[DEVICENAME]", nodeName);
        }
        return title;
    }

    /**
     * Returns a @SinglePathMessageHandler that will notify @availableSensorsUpdatedListeners
     * with the @DeviceSensors parsed from the received message
     */
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

    public Map<String, List<DeviceSensor>> getSelectedSensors() {
        return selectedSensors;
    }

    public List<AvailableSensorsUpdatedListener> getAvailableSensorsUpdatedListeners() {
        return availableSensorsUpdatedListeners;
    }

}
