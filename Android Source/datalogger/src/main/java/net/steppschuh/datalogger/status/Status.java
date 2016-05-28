package net.steppschuh.datalogger.status;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

public class Status {

    long lastUpdateTimestamp = -1;

    public void updated() {
        lastUpdateTimestamp = System.currentTimeMillis();
    }

    public void updated(StatusUpdateHandler statusUpdateHandler) {
        updated();
        statusUpdateHandler.notifyStatusUpdateReceivers(this);
    }

    @JsonIgnore
    public boolean hasBeenUpdatedSince(long timestamp) {
        return lastUpdateTimestamp > timestamp;
    }

    @JsonIgnore
    @Override
    public String toString() {
        return toJson();
    }

    @JsonIgnore
    public String toJson() {
        String jsonData = null;
        try {
            ObjectMapper mapper = new ObjectMapper();
            mapper.enable(SerializationFeature.INDENT_OUTPUT);
            mapper.disable(SerializationFeature.FAIL_ON_EMPTY_BEANS);
            jsonData = mapper.writeValueAsString(this);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return jsonData;
    }

    public long getLastUpdateTimestamp() {
        return lastUpdateTimestamp;
    }

    public void setLastUpdateTimestamp(long lastUpdateTimestamp) {
        this.lastUpdateTimestamp = lastUpdateTimestamp;
    }

}
