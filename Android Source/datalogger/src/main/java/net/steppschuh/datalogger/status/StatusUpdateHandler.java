package net.steppschuh.datalogger.status;

import android.util.Log;

import java.util.ArrayList;
import java.util.List;

public class StatusUpdateHandler {

    public static final String TAG = StatusUpdateHandler.class.getSimpleName();

    private List<StatusUpdateReceiver> statusUpdateReceivers = new ArrayList<>();

    public boolean registerStatusUpdateReceiver(StatusUpdateReceiver statusUpdateReceiver) {
        if (!statusUpdateReceivers.contains(statusUpdateReceiver)) {
            return statusUpdateReceivers.add(statusUpdateReceiver);
        }
        return false;
    }

    public boolean unregisterStatusUpdateReceiver(StatusUpdateReceiver statusUpdateReceiver) {
        if (statusUpdateReceivers.contains(statusUpdateReceiver)) {
            return statusUpdateReceivers.remove(statusUpdateReceiver);
        }
        return false;
    }

    public void notifyStatusUpdateReceivers(Status status) {
        for (StatusUpdateReceiver statusUpdateReceiver : statusUpdateReceivers) {
            try {
                statusUpdateReceiver.onStatusUpdated(status);
            } catch (Exception ex) {
                Log.e(TAG, "Status update receiver is unable to handle status update: " + ex.getMessage());
                ex.printStackTrace();
            }
        }
    }

}
