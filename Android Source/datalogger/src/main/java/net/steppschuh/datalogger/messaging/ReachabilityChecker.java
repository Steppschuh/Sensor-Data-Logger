package net.steppschuh.datalogger.messaging;

import com.google.android.gms.wearable.MessageApi;
import com.google.android.gms.wearable.Node;

import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

import net.steppschuh.datalogger.MobileApp;
import net.steppschuh.datalogger.messaging.handler.MessageHandler;
import net.steppschuh.datalogger.messaging.handler.PingMessageHandler;
import net.steppschuh.datalogger.messaging.handler.SinglePathMessageHandler;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class ReachabilityChecker {

    public static final String TAG = ReachabilityChecker.class.getSimpleName();

    public static final long UPDATE_INTERVAL_DEFAULT = TimeUnit.SECONDS.toMillis(10);
    public static final long RECENTLY_DURATION = TimeUnit.SECONDS.toMillis(30);
    public static final long REACHABILITY_TIMEOUT_DEFAULT = TimeUnit.SECONDS.toMillis(2);

    public static final String NODE_ID_ANY = "any";

    MobileApp app;

    private Map<String, Boolean> nodeReachabilities = new HashMap<>();
    private Map<String, Long> nodeEchoTimestamps = new HashMap<>();

    private Map<String, List<NodeReachabilityUpdateReceiver>> reachabilityUpdateReceivers = new HashMap<>();
    private long updateInterval = UPDATE_INTERVAL_DEFAULT;
    private ReachabilityPingMessageHandler pingMessageHandler;
    private ReachabilityEchoMessageHandler echoMessageHandler;
    private AppClosedMessageHandler appClosedMessageHandler;

    public interface NodeReachabilityUpdateReceiver {
        void onReachabilityUpdated(String nodeId, boolean isReachable);
    }

    public ReachabilityChecker(MobileApp app) {
        this.app = app;
        pingMessageHandler = new ReachabilityPingMessageHandler(app.getGoogleApiMessenger());
        echoMessageHandler = new ReachabilityEchoMessageHandler();
        appClosedMessageHandler = new AppClosedMessageHandler();
        reachabilityUpdateReceivers.put(NODE_ID_ANY, new ArrayList<NodeReachabilityUpdateReceiver>());
    }

    public void registerMessageHandlers() {
        app.registerMessageHandler(pingMessageHandler);
        app.registerMessageHandler(echoMessageHandler);
        app.registerMessageHandler(appClosedMessageHandler);
    }

    public void unregisterMessageHandlers() {
        app.unregisterMessageHandler(pingMessageHandler);
        app.unregisterMessageHandler(echoMessageHandler);
        app.registerMessageHandler(appClosedMessageHandler);
    }

    public void registerReachabilityUpdateReceiver(String nodeId, NodeReachabilityUpdateReceiver reachabilityUpdateReceiver) {
        if (reachabilityUpdateReceiver == null) {
            return;
        }
        List<NodeReachabilityUpdateReceiver> updateReceivers = reachabilityUpdateReceivers.get(nodeId);
        if (updateReceivers == null) {
            updateReceivers = new ArrayList<>();
        }
        if (!updateReceivers.contains(reachabilityUpdateReceiver)) {
            updateReceivers.add(reachabilityUpdateReceiver);
            reachabilityUpdateReceivers.put(nodeId, updateReceivers);
        }
    }

    public void unregisterReachabilityUpdateReceiver(NodeReachabilityUpdateReceiver reachabilityUpdateReceiver) {
        if (reachabilityUpdateReceiver == null) {
            return;
        }
        for (Map.Entry<String, List<NodeReachabilityUpdateReceiver>> updateReceivers : reachabilityUpdateReceivers.entrySet()) {
            if (updateReceivers.getValue().contains(reachabilityUpdateReceiver)) {
                updateReceivers.getValue().remove(reachabilityUpdateReceiver);
            }
        }
    }

    /**
     * Returns true if the reachability for the specified node has changed
     */
    public boolean shouldNotifyReachabilityUpdateReceivers(String nodeId, boolean isReachable) {
        Boolean currentReachability = nodeReachabilities.get(nodeId);
        if (currentReachability == null) {
            return true;
        }
        return currentReachability.booleanValue() != isReachable;
    }

    /**
     * Calls all available @reachabilityUpdateReceivers and lets them know
     * the reachability of the specified node
     */
    public void notifyReachabilityUpdateReceivers(String nodeId, boolean isReachable) {
        List<NodeReachabilityUpdateReceiver> updateReceivers = reachabilityUpdateReceivers.get(NODE_ID_ANY);
        List<NodeReachabilityUpdateReceiver> specificUpdateReceivers = reachabilityUpdateReceivers.get(nodeId);
        if (specificUpdateReceivers != null) {
            updateReceivers.addAll(specificUpdateReceivers);
        }

        for (NodeReachabilityUpdateReceiver reachabilityUpdateReceiver : updateReceivers) {
            if (reachabilityUpdateReceiver != null) {
                reachabilityUpdateReceiver.onReachabilityUpdated(nodeId, isReachable);
            }
        }
    }

    /**
     * Updates the @nodeReachabilities map and notifies the @reachabilityUpdateReceivers
     * if the reachability has changed
     */
    public void setNodeReachability(String nodeId, boolean isReachable) {
        Log.v(TAG, "Reachability of node: " + nodeId + " set to: " + String.valueOf(isReachable));
        boolean reachabilityChanged = shouldNotifyReachabilityUpdateReceivers(nodeId, isReachable);
        if (reachabilityChanged) {
            nodeReachabilities.put(nodeId, isReachable);
            notifyReachabilityUpdateReceivers(nodeId, isReachable);
        }
    }

    /**
     * Sends a ping to all nearby nodes and registers the specified @NodeReachabilityUpdateReceiver
     */
    public void checkReachabilities(NodeReachabilityUpdateReceiver reachabilityUpdateReceiver) {
        try {
            Log.v(TAG, "Checking reachability of all recently connected nodes");
            for (Node node : app.getGoogleApiMessenger().getLastConnectedNearbyNodes()) {
                if (node != null) {
                    checkReachability(node.getId(), reachabilityUpdateReceiver);
                }
            }
        } catch (Exception ex) {
            Log.w(TAG, "Unable to check all nodes: " + ex.getMessage());
        }
    }

    /**
     * Sends a ping to the specified node and registers the specified @NodeReachabilityUpdateReceiver
     */
    public void checkReachability(final String nodeId, NodeReachabilityUpdateReceiver reachabilityUpdateReceiver) {
        Log.v(TAG, "Checking reachability of node: " + nodeId);
        registerReachabilityUpdateReceiver(nodeId, reachabilityUpdateReceiver);
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    // ping the node
                    byte[] data = Build.MODEL.getBytes(GoogleApiMessenger.DEFAULT_CHARSET);
                    MessageApi.SendMessageResult messageResult = app.getGoogleApiMessenger().sendMessageToNodeWithResult(MessageHandler.PATH_PING, data, nodeId);
                    if (!messageResult.getStatus().isSuccess()) {
                        throw new Exception(messageResult.getStatus().getStatusMessage());
                    }

                    // create a timeout check
                    new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            if (!hasReachedNodeSince(nodeId, System.currentTimeMillis() - REACHABILITY_TIMEOUT_DEFAULT)) {
                                Log.d(TAG, "Reachability check timed out");
                                setNodeReachability(nodeId, false);
                            }
                        }
                    }, REACHABILITY_TIMEOUT_DEFAULT);
                } catch (Exception ex) {
                    Log.w(TAG, "Unable to reach node: " + nodeId + ": " + ex.getMessage());
                    setNodeReachability(nodeId, false);
                }
            }
        }).start();
    }

    public boolean hasReachedNodeRecently(String nodeId) {
        return hasReachedNodeSince(nodeId, System.currentTimeMillis() - RECENTLY_DURATION);
    }

    public boolean hasReachedNodeSince(String nodeId, long timestamp) {
        Long lastEchoTimestamp = nodeEchoTimestamps.get(nodeId);
        if (lastEchoTimestamp == null) {
            return false;
        }
        return lastEchoTimestamp >= timestamp;
    }

    /**
     * Returns a list of node ids that were considered as reachable
     */
    public List<String> getReachableNodeIds() {
        List<String> recentlyReachedNodes = new ArrayList<>();
        for (Map.Entry<String, Boolean> nodeReachabilitiesEntry : nodeReachabilities.entrySet()) {
            if (nodeReachabilitiesEntry.getValue()) {
                recentlyReachedNodes.add(nodeReachabilitiesEntry.getKey());
            }
        }
        return recentlyReachedNodes;
    }

    /**
     * Returns a list of node ids that are connected but not reachable
     */
    public List<String> getNotReachableNodeIds() {
        List<String> reachableNodeIds = getReachableNodeIds();
        List<String> notReachableNodeIds = new ArrayList<>();

        List<Node> nearbyNodes = app.getGoogleApiMessenger().getLastConnectedNearbyNodes();
        for (Node nearbyNode : nearbyNodes) {
            if (nearbyNode == null || reachableNodeIds.contains(nearbyNode.getId())) {
                continue;
            }
            notReachableNodeIds.add(nearbyNode.getId());
        }
        return notReachableNodeIds;
    }

    public class ReachabilityPingMessageHandler extends PingMessageHandler {

        public ReachabilityPingMessageHandler(GoogleApiMessenger googleApiMessenger) {
            super(googleApiMessenger);
        }

        @Override
        public void handleMessage(Message message) {
            String sourceNode = getSourceNodeIdFromMessage(message);
            setNodeReachability(sourceNode, true);
            super.handleMessage(message);
        }
    }

    public class ReachabilityEchoMessageHandler extends SinglePathMessageHandler {

        public ReachabilityEchoMessageHandler() {
            super(PATH_ECHO);
        }

        @Override
        public void handleMessage(Message message) {
            String sourceNode = getSourceNodeIdFromMessage(message);
            Log.v(TAG, "Received an echo from: " + sourceNode);
            setNodeReachability(sourceNode, true);
            nodeEchoTimestamps.put(sourceNode, System.currentTimeMillis());
        }
    }

    public class AppClosedMessageHandler extends SinglePathMessageHandler {

        public AppClosedMessageHandler() {
            super(PATH_CLOSING);
        }

        @Override
        public void handleMessage(Message message) {
            String sourceNode = getSourceNodeIdFromMessage(message);
            Log.v(TAG, "App closed on: " + sourceNode);
            setNodeReachability(sourceNode, false);
        }
    }

    /**
     * Getter & Setter
     */
    public Map<String, Boolean> getNodeReachabilities() {
        return nodeReachabilities;
    }

    public void setNodeReachabilities(Map<String, Boolean> nodeReachabilities) {
        this.nodeReachabilities = nodeReachabilities;
    }

    public long getUpdateInterval() {
        return updateInterval;
    }

    public void setUpdateInterval(long updateInterval) {
        this.updateInterval = updateInterval;
    }
}
