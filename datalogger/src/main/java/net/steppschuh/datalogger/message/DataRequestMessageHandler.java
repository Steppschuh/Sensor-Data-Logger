package net.steppschuh.datalogger.message;

import android.os.Message;
import android.util.Log;

import net.steppschuh.datalogger.MobileApp;
import net.steppschuh.datalogger.data.DataRequest;

public class DataRequestMessageHandler extends SinglePathMessageHandler {

    MobileApp app;

    public DataRequestMessageHandler(MobileApp app) {
        super(PATH_DATA_REQUEST);
        this.app = app;
    }

    @Override
    public void handleMessage(Message message) {
        try {
            String sourceNodeId = getSourceNodeIdFromMessage(message);
            String dataRequestJson = getDataFromMessageAsString(message);
            Log.v(TAG, "Received a data request from " + sourceNodeId + ": " + dataRequestJson);

            DataRequest dataRequest = DataRequest.fromJson(dataRequestJson);
            if (dataRequest == null) {
                throw new Exception("Unable to de-serialize request");
            }
        } catch (Exception ex) {
            Log.w(TAG, "Unable to handle data request: " + ex.getMessage());
        }
    }

}
