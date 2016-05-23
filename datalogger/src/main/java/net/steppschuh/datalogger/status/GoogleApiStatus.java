package net.steppschuh.datalogger.status;

import com.google.android.gms.wearable.Node;

import java.util.ArrayList;
import java.util.List;

public class GoogleApiStatus extends Status {

    private boolean connected = true;
    private List<Node> lastConnectedNodes = new ArrayList<>();
    private long lastConnectedNodesUpdateTimestamp = -1;

    public boolean isConnected() {
        return connected;
    }

    public void setConnected(boolean connected) {
        this.connected = connected;
    }

    public List<Node> getLastConnectedNodes() {
        return lastConnectedNodes;
    }

    public void setLastConnectedNodes(List<Node> lastConnectedNodes) {
        this.lastConnectedNodes = lastConnectedNodes;
    }

    public long getLastConnectedNodesUpdateTimestamp() {
        return lastConnectedNodesUpdateTimestamp;
    }

    public void setLastConnectedNodesUpdateTimestamp(long lastConnectedNodesUpdateTimestamp) {
        this.lastConnectedNodesUpdateTimestamp = lastConnectedNodesUpdateTimestamp;
    }
}
