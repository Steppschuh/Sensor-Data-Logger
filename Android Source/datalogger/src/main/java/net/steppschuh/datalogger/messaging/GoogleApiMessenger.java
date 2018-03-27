package net.steppschuh.datalogger.messaging;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.wearable.MessageApi;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.NodeApi;
import com.google.android.gms.wearable.Wearable;

import android.os.Build;
import android.os.Bundle;
import android.os.Message;
import android.support.annotation.NonNull;
import android.util.Log;

import net.steppschuh.datalogger.MobileApp;
import net.steppschuh.datalogger.messaging.handler.MessageHandler;
import net.steppschuh.datalogger.status.GoogleApiStatus;
import net.steppschuh.datalogger.status.Status;
import net.steppschuh.datalogger.status.StatusUpdateEmitter;
import net.steppschuh.datalogger.status.StatusUpdateHandler;
import net.steppschuh.datalogger.status.StatusUpdateReceiver;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

public class GoogleApiMessenger implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, StatusUpdateEmitter {

    private static final String TAG = GoogleApiMessenger.class.getSimpleName();
    public static final Charset DEFAULT_CHARSET = Charset.forName("UTF-8");
    public static final String DEFAULT_NODE_ID = "LOCAL_NODE";

    private GoogleApiStatus status = new GoogleApiStatus();
    private StatusUpdateHandler statusUpdateHandler;

    private MobileApp app;
    private GoogleApiClient googleApiClient;
    private boolean wearableApiAvailable;

    public GoogleApiMessenger(MobileApp app) {
        this.app = app;
        initialize(app);
    }

    private void initialize(MobileApp app) {
        Log.d(TAG, "Initializing Google API client");
        googleApiClient = new GoogleApiClient.Builder(app)
                .addConnectionCallbacks(this)
                .addApi(Wearable.API)
                .build();
        updateLocalNode();
        setupStatusUpdates();
    }

    public void setupStatusUpdates() {
        statusUpdateHandler = new StatusUpdateHandler();
        statusUpdateHandler.registerStatusUpdateReceiver(new StatusUpdateReceiver() {
            @Override
            public void onStatusUpdated(Status status) {
                app.getStatus().setGoogleApiStatus((GoogleApiStatus) status);
                app.getStatus().updated(app.getStatusUpdateHandler());

                // update node reachabilities
                app.getReachabilityChecker().checkReachabilities(null);
            }
        });
    }

    public boolean connect() {
        Log.d(TAG, "Connecting Google API client");
        if (googleApiClient != null && !googleApiClient.isConnected()) {
            googleApiClient.connect();
            return true;
        }
        return false;
    }

    public boolean disconnect() {
        Log.d(TAG, "Disconnecting Google API client");
        status.setLastConnectedNodes(new ArrayList<Node>());
        if (googleApiClient != null && googleApiClient.isConnected()) {
            googleApiClient.disconnect();
            return true;
        }
        return false;
    }

    @Override
    public void onConnected(Bundle bundle) {
        Log.d(TAG, "Connected");
        updateLastConnectedNodes();
        status.setConnected(true);
        status.updated(statusUpdateHandler);
    }

    @Override
    public void onConnectionSuspended(int i) {
        Log.d(TAG, "Connection suspended");
        updateLastConnectedNodes();
        status.setConnected(false);
        status.updated(statusUpdateHandler);
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        if (connectionResult.getErrorCode() == ConnectionResult.API_UNAVAILABLE) {
            wearableApiAvailable = false;
        }
        status.setConnected(false);
        status.updated(statusUpdateHandler);
        Log.e(TAG, "Google API client connection failed: " + connectionResult.getErrorMessage());
    }

    public void updateLocalNode() {
        PendingResult<NodeApi.GetLocalNodeResult> pendingResult = Wearable.NodeApi.getLocalNode(googleApiClient);
        pendingResult.setResultCallback(new ResultCallback<NodeApi.GetLocalNodeResult>() {
            @Override
            public void onResult(@NonNull NodeApi.GetLocalNodeResult getLocalNodeResult) {
                status.setLocalNode(getLocalNodeResult.getNode());
                status.updated(statusUpdateHandler);
                wearableApiAvailable = getLocalNodeResult.getNode() != null;
            }
        });
    }

    public void updateLastConnectedNodes() {
        getConnectedNodes(new ResultCallback<NodeApi.GetConnectedNodesResult>() {
            @Override
            public void onResult(@NonNull NodeApi.GetConnectedNodesResult getConnectedNodesResult) {
                status.setLastConnectedNodes(getConnectedNodesResult.getNodes());
                status.setLastConnectedNodesUpdateTimestamp(System.currentTimeMillis());
                status.updated(statusUpdateHandler);

                StringBuilder sb = new StringBuilder();
                sb.append("Connected Nodes:");
                for (Node connectedNode : status.getLastConnectedNodes()) {
                    sb.append("\n - ").append(connectedNode.getDisplayName());
                    sb.append(": ").append(connectedNode.getId());
                    if (connectedNode.isNearby()) {
                        sb.append(" (nearby)");
                    }
                }
                Log.v(TAG, sb.toString());
            }
        });
    }

    public PendingResult<NodeApi.GetConnectedNodesResult> getConnectedNodes(ResultCallback<NodeApi.GetConnectedNodesResult> callback) {
        PendingResult<NodeApi.GetConnectedNodesResult> pendingResult = Wearable.NodeApi.getConnectedNodes(googleApiClient);
        pendingResult.setResultCallback(callback);
        return pendingResult;
    }

    public String getNodeName(String id) {
        Node node = getLastConnectedNodeById(id);
        if (node != null) {
            return node.getDisplayName();
        } else {
            if (id == null || id.equals(DEFAULT_NODE_ID)) {
                return Build.MODEL;
            } else {
                return id;
            }
        }
    }

    public Node getLastConnectedNodeById(String id) {
        if (id == null || id.equals(DEFAULT_NODE_ID)) {
            return null;
        }
        String localNodeId = getLocalNodeId();
        if (localNodeId != null && localNodeId.equals(id)) {
            return status.getLocalNode();
        }
        return getNodeById(id, status.getLastConnectedNodes());
    }

    public List<Node> getLastConnectedNodes() {
        if (status != null) {
            return status.getLastConnectedNodes();
        }
        return new ArrayList<>();
    }

    public List<Node> getLastConnectedNearbyNodes() {
        if (status != null) {
            return getNearbyNodes(status.getLastConnectedNodes());
        }
        return new ArrayList<>();
    }

    public static Node getNodeById(String id, List<Node> nodes) {
        for (Node recentNode : nodes) {
            if (recentNode.getId().equals(id)) {
                return recentNode;
            }
        }
        return null;
    }

    public void sendMessageToNearbyNodes(final String path, final String data) {
        sendMessageToNearbyNodes(path, data.getBytes(DEFAULT_CHARSET));
    }

    public void sendMessageToNearbyNodes(final String path, final byte[] data) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                NodeApi.GetConnectedNodesResult getConnectedNodesResult = Wearable.NodeApi.getConnectedNodes(googleApiClient).await();
                status.setLastConnectedNodes(getConnectedNodesResult.getNodes());
                status.setLastConnectedNodesUpdateTimestamp(System.currentTimeMillis());
                for (Node node : status.getLastConnectedNodes()) {
                    try {
                        if (!node.isNearby()) {
                            continue;
                        }
                        sendMessageToNodeWithResult(path, data, node.getId());
                    } catch (Exception ex) {
                        Log.w(TAG, "Unable to send message to node: " + ex.getMessage());
                    }
                }
            }
        }).start();
    }

    public void sendMessageToNode(final String path, final String data, final String nodeId) {
        sendMessageToNode(path, data.getBytes(DEFAULT_CHARSET), nodeId);
    }

    public void sendMessageToNode(final String path, final byte[] data, final String nodeId) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                if (nodeId == null || nodeId.equals(DEFAULT_NODE_ID) || nodeId.equals(getLocalNodeId())) {
                    sendMessageToLocalNode(path, data, nodeId);
                } else {
                    try {
                        sendMessageToNodeWithResult(path, data, nodeId);
                    } catch (Exception ex) {
                        Log.w(TAG, "Unable to send message to node: " + nodeId + ": " + ex.getMessage());
                        ex.printStackTrace();
                    }
                }
            }
        }).start();
    }

    public MessageApi.SendMessageResult sendMessageToNodeWithResult(final String path, final byte[] data, String nodeId) throws Exception {
        if (!googleApiClient.isConnected()) {
            throw new Exception("Google API client is not connected");
        }
        if (nodeId == null) {
            throw new Exception("Node Id is not set");
        }
        return Wearable.MessageApi.sendMessage(googleApiClient, nodeId, path, data).await();
    }

    public void sendMessageToLocalNode(final String path, final byte[] data, String nodeId) {
        try {
            if (nodeId == null) {
                nodeId = DEFAULT_NODE_ID;
            }
            Bundle bundle = new Bundle();
            bundle.putString(MessageHandler.KEY_PATH, path);
            bundle.putString(MessageHandler.KEY_SOURCE_NODE_ID, nodeId);
            bundle.putByteArray(MessageHandler.KEY_DATA, data);

            Message message = new Message();
            message.setData(bundle);

            app.notifyMessageHandlers(message);
        } catch (Exception ex) {
            Log.w(TAG, "Unable to send message to local node: " + ex.getMessage());
            ex.printStackTrace();
        }
    }

    public GoogleApiClient getGoogleApiClient() {
        return googleApiClient;
    }

    public void setGoogleApiClient(GoogleApiClient googleApiClient) {
        this.googleApiClient = googleApiClient;
    }

    public String getLocalNodeId() {
        if (status != null && status.getLocalNode() != null) {
            return status.getLocalNode().getId();
        }
        return DEFAULT_NODE_ID;
    }

    public static List<Node> getNearbyNodes(List<Node> nodes) {
        List<Node> nearbyNodes = new ArrayList<>();
        for (Node node : nodes) {
            if (node.isNearby()) {
                nearbyNodes.add(node);
            }
        }
        return nearbyNodes;
    }

    @Override
    public Status getStatus() {
        return status;
    }

    @Override
    public StatusUpdateHandler getStatusUpdateHandler() {
        return statusUpdateHandler;
    }

    public boolean isWearableApiAvailable() {
        return wearableApiAvailable;
    }
}
