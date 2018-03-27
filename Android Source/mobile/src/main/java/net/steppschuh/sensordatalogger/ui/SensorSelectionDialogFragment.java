package net.steppschuh.sensordatalogger.ui;

import com.google.android.gms.wearable.Node;
import com.google.firebase.analytics.FirebaseAnalytics;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.annotation.DrawableRes;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.ListView;

import net.steppschuh.datalogger.MobileApp;
import net.steppschuh.datalogger.data.request.DataRequest;
import net.steppschuh.datalogger.data.request.SensorDataRequest;
import net.steppschuh.datalogger.messaging.GoogleApiMessenger;
import net.steppschuh.datalogger.messaging.handler.MessageHandler;
import net.steppschuh.datalogger.messaging.handler.SinglePathMessageHandler;
import net.steppschuh.datalogger.sensor.DeviceSensor;
import net.steppschuh.datalogger.sensor.DeviceSensors;
import net.steppschuh.datalogger.ui.UnitHelper;
import net.steppschuh.sensordatalogger.R;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SensorSelectionDialogFragment extends DialogFragment {

    public static final String TAG = SensorSelectionDialogFragment.class.getSimpleName();

    public interface AvailableSensorsUpdatedListener {
        void onAvailableSensorsUpdated(String nodeId, List<DeviceSensor> deviceSensors);
    }

    public interface SelectedSensorsUpdatedListener {
        void onSensorsFromAllNodesSelected(Map<String, List<DeviceSensor>> selectedSensors);

        void onSensorsFromNodeSelected(String nodeId, List<DeviceSensor> sensors);

        void onSensorSelectionClosed(DialogFragment dialog);
    }

    private MobileApp app;
    private SelectedSensorsUpdatedListener listener;

    private Map<String, Node> availableNodes = new HashMap<>();
    private Map<String, List<DeviceSensor>> availableSensors = new HashMap<>();
    private Map<String, List<DeviceSensor>> selectedSensors = new HashMap<>();
    private Map<String, List<DeviceSensor>> previouslySelectedSensors = new HashMap<>();
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
                // dismiss dialog
            }
        });
        AlertDialog dialog = builder.create();
        dialog.requestWindowFeature(Window.FEATURE_LEFT_ICON);
        return dialog;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        app = (MobileApp) activity.getApplicationContext();
        app.registerMessageHandler(setSensorsMessageHandler);
        app.getReachabilityChecker().checkReachabilities(null);
        try {
            listener = (SelectedSensorsUpdatedListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString() + " must implement " + SelectedSensorsUpdatedListener.class.getSimpleName());
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

    @Override
    public void onStop() {
        listener.onSensorSelectionClosed(this);
        super.onStop();
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

        // update icon
        if (nodeId != null && nodeId.equals(app.getGoogleApiMessenger().getLocalNodeId())) {
            setDialogIcon(R.drawable.ic_phone_android_black_48dp);
        } else {
            setDialogIcon(R.drawable.ic_watch_black_48dp);
        }

        // create & apply new list adapter
        multiChoiceAdapter = new SensorListAdapter(new ArrayList<DeviceSensor>(), getActivity());
        ListView listView = ((AlertDialog) getDialog()).getListView();
        listView.setAdapter(multiChoiceAdapter);

        // update layout params & invalidate list view
        ((ViewGroup.MarginLayoutParams) listView.getLayoutParams()).setMargins((int) UnitHelper.convertDpToPixel(16, getActivity()), 0, 0, 0);
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

        // track analytics event
        Bundle bundle = new Bundle();
        bundle.putString(FirebaseAnalytics.Param.ITEM_CATEGORY, "Device Sensors");
        bundle.putString(FirebaseAnalytics.Param.VALUE, nodeName);
        app.getAnalytics().logEvent(FirebaseAnalytics.Event.VIEW_ITEM_LIST, bundle);
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
            Log.v(TAG, "Next node (local) " + localNodeId);
            return localNodeId;
        }

        // check already requested available sensors
        for (Map.Entry<String, List<DeviceSensor>> availableSensors : this.availableSensors.entrySet()) {
            if (!hasSelectedSensorsForNode(availableSensors.getKey())) {
                Log.v(TAG, "Next node (available sensors) " + availableSensors.getKey());
                return availableSensors.getKey();
            }
        }

        // check reachable notes
        for (String nodeId : app.getReachabilityChecker().getReachableNodeIds()) {
            if (!hasSelectedSensorsForNode(nodeId)) {
                Log.v(TAG, "Next node (reachability checker) " + nodeId);
                return nodeId;
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
        List<DeviceSensor> currentlySelectedSensors;
        if (multiChoiceAdapter != null) {
            currentlySelectedSensors = multiChoiceAdapter.getSelectedSensors();
        } else {
            currentlySelectedSensors = new ArrayList<>();
        }
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
            public void onAvailableSensorsUpdated(final String nodeId, final List<DeviceSensor> deviceSensors) {
                new Handler(Looper.getMainLooper()).post(new Runnable() {
                    @Override
                    public void run() {
                        Log.d(TAG, nodeId + " updated, " + deviceSensors.size() + " sensor(s) available");
                        if (nodeId != null && nodeId != GoogleApiMessenger.DEFAULT_NODE_ID && !nodeId.equals(getNextSensorSelectionNodeId())) {
                            Log.w(TAG, "Tried to update list adapter with data wrong node: " + nodeId);
                            return;
                        }
                        if (multiChoiceAdapter != null) {
                            // update adapter with sensors
                            multiChoiceAdapter.setAvailableSensors(deviceSensors);

                            // restore previously selected sensors
                            List<DeviceSensor> selectedSensors = previouslySelectedSensors.get(nodeId);
                            if (selectedSensors != null) {
                                multiChoiceAdapter.setPreviouslySelectedSensors(selectedSensors);
                            }
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
                });
            }
        };
    }

    /**
     * Returns a readable dialog title based on the specified node id
     */
    private String getDialogTitleForAvailableSensors(String nodeId) {
        String title;
        if (nodeId == null || nodeId.equals(app.getGoogleApiMessenger().getLocalNodeId())) {
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

    private void setDialogIcon(@DrawableRes int resId) {
        getDialog().setFeatureDrawableResource(Window.FEATURE_LEFT_ICON, resId);
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

    public static SensorDataRequest createSensorDataRequest(List<DeviceSensor> selectedSensors) {
        List<Integer> sensorTypes = new ArrayList<>();
        for (DeviceSensor selectedSensor : selectedSensors) {
            sensorTypes.add(selectedSensor.getType());
        }

        SensorDataRequest sensorDataRequest = new SensorDataRequest(sensorTypes);
        sensorDataRequest.setUpdateInteval(DataRequest.UPDATE_INTERVAL_FAST);
        return sensorDataRequest;
    }

    public SensorDataRequest createSensorDataRequest(String nodeId) {
        List<DeviceSensor> sensors = selectedSensors.get(nodeId);
        SensorDataRequest sensorDataRequest = createSensorDataRequest(sensors);
        sensorDataRequest.setSourceNodeId(app.getGoogleApiMessenger().getLocalNodeId());
        return sensorDataRequest;
    }

    public Map<String, SensorDataRequest> createSensorDataRequests() {
        Map<String, SensorDataRequest> sensorDataRequests = new HashMap<>();
        for (Map.Entry<String, List<DeviceSensor>> deviceSensors : selectedSensors.entrySet()) {
            SensorDataRequest sensorDataRequest = createSensorDataRequest(deviceSensors.getKey());
            sensorDataRequests.put(deviceSensors.getKey(), sensorDataRequest);
        }
        return sensorDataRequests;
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

    public Map<String, List<DeviceSensor>> getPreviouslySelectedSensors() {
        return previouslySelectedSensors;
    }

    public void setPreviouslySelectedSensors(Map<String, List<DeviceSensor>> previouslySelectedSensors) {
        this.previouslySelectedSensors = previouslySelectedSensors;
    }
}