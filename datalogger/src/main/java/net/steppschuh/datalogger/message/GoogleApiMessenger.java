package net.steppschuh.datalogger.message;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.wearable.MessageApi;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.NodeApi;
import com.google.android.gms.wearable.Wearable;

import net.steppschuh.datalogger.MobileApp;
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

    private GoogleApiStatus status = new GoogleApiStatus();
    private StatusUpdateHandler statusUpdateHandler;

    private GoogleApiClient googleApiClient;

    public GoogleApiMessenger(Context context) {
        initialize(context);
    }

    private void initialize(Context context) {
        Log.d(TAG, "Initializing Google API client");
        googleApiClient = new GoogleApiClient.Builder(context)
                .addConnectionCallbacks(this)
                .addApiIfAvailable(Wearable.API)
                .build();
        updateLocalNode();
    }

    public void setupStatusUpdates(final MobileApp app) {
        statusUpdateHandler = new StatusUpdateHandler();
        statusUpdateHandler.registerStatusUpdateReceiver(new StatusUpdateReceiver() {
            @Override
            public void onStatusUpdated(Status status) {
                app.getStatus().setGoogleApiStatus((GoogleApiStatus) status);
                app.getStatus().updated(app.getStatusUpdateHandler());
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
            // The Wearable API is unavailable
        }
        Log.w(TAG, "Google API client connection failed: " + connectionResult.getErrorMessage());
    }

    public void updateLocalNode() {
        PendingResult<NodeApi.GetLocalNodeResult> pendingResult = Wearable.NodeApi.getLocalNode(googleApiClient);
        pendingResult.setResultCallback(new ResultCallback<NodeApi.GetLocalNodeResult>() {
            @Override
            public void onResult(@NonNull NodeApi.GetLocalNodeResult getLocalNodeResult) {
                status.setLocalNode(getLocalNodeResult.getNode());
                status.updated(statusUpdateHandler);
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

    public Node getLastConnectedNodeById(String id) {
        return getNodeById(id, status.getLastConnectedNodes());
    }

    public static Node getNodeById(String id, List<Node> nodes) {
        for (Node recentNode : nodes) {
            if (recentNode.getId().equals(id)) {
                return recentNode;
            }
        }
        return null;
    }

    public void sendMessageToNearbyNodes(final String path, final String data) throws Exception {
        sendMessageToNearbyNodes(path, data.getBytes(DEFAULT_CHARSET));
    }

    public void sendMessageToNearbyNodes(final String path, final byte[] data) throws Exception {
        if (!googleApiClient.isConnected()) {
            throw new Exception("Google API client is not connected");
        }
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
                        sendMessageToNode(path, data, node);
                    } catch (Exception ex) {
                        Log.w(TAG, "Unable to send message to node: " + ex.getMessage());
                    }
                }
            }
        }).start();
    }

    public void sendMessageToNode(final String path, final String data, final String nodeId) throws Exception {
        sendMessageToNode(path, data.getBytes(DEFAULT_CHARSET), nodeId);
    }

    public void sendMessageToNode(final String path, final byte[] data, final String nodeId) throws Exception {
        if (!googleApiClient.isConnected()) {
            throw new Exception("Google API client is not connected");
        }
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Node node = getLastConnectedNodeById(nodeId);
                    if (node == null) {
                        NodeApi.GetConnectedNodesResult getConnectedNodesResult = Wearable.NodeApi.getConnectedNodes(googleApiClient).await();
                        node = getNodeById(nodeId, getConnectedNodesResult.getNodes());
                    }
                    sendMessageToNode(path, data, node);
                } catch (Exception ex) {
                    Log.w(TAG, "Unable to send message to node: " + nodeId + ": " + ex.getMessage());
                    ex.printStackTrace();
                }
            }
        }).start();
    }

    private MessageApi.SendMessageResult sendMessageToNode(final String path, final byte[] data, Node node) throws Exception {
        if (!googleApiClient.isConnected()) {
            throw new Exception("Google API client is not connected");
        }
        //Log.v(TAG, "Sending message to: " + node.getDisplayName() + " at: " + path);
        return Wearable.MessageApi.sendMessage(googleApiClient, node.getId(), path, data).await();
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
        return null;
    }

    @Override
    public Status getStatus() {
        return status;
    }

    @Override
    public StatusUpdateHandler getStatusUpdateHandler() {
        return statusUpdateHandler;
    }
}
