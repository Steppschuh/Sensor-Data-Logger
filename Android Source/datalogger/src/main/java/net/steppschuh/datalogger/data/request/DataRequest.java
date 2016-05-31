package net.steppschuh.datalogger.data.request;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import java.io.Serializable;
import java.util.concurrent.TimeUnit;

public class DataRequest implements Serializable {

    public static final long UPDATE_INTERVAL_DEFAULT = TimeUnit.SECONDS.toMillis(1);
    public static final long UPDATE_INTERVAL_FAST = 50;
    public static final long UPDATE_INTERVAL_NORMAL = 100;
    public static final long UPDATE_INTERVAL_SLOW = 500;
    public static final int TIMESTAMP_NOT_SET = -1;

    private String sourceNodeId;
    private String dataSource;
    private long updateInteval;
    private long startTimestamp;
    private long endTimestamp;

    public DataRequest() {
        updateInteval = UPDATE_INTERVAL_DEFAULT;
        startTimestamp = System.currentTimeMillis();
        endTimestamp = TIMESTAMP_NOT_SET;
    }

    public DataRequest(String sourceNodeId) {
        this();
        this.sourceNodeId = sourceNodeId;
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

    public static DataRequest fromJson(String json) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            mapper.disable(SerializationFeature.FAIL_ON_EMPTY_BEANS);
            DataRequest dataRequest = mapper.readValue(json, DataRequest.class);
            return dataRequest;
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return null;
    }

    /**
     * Getter & Setter
     */
    public String getDataSource() {
        return dataSource;
    }

    public void setDataSource(String dataSource) {
        this.dataSource = dataSource;
    }

    public long getUpdateInteval() {
        return updateInteval;
    }

    public void setUpdateInteval(long updateInteval) {
        this.updateInteval = updateInteval;
    }

    public long getStartTimestamp() {
        return startTimestamp;
    }

    public void setStartTimestamp(long startTimestamp) {
        this.startTimestamp = startTimestamp;
    }

    public long getEndTimestamp() {
        return endTimestamp;
    }

    public void setEndTimestamp(long endTimestamp) {
        this.endTimestamp = endTimestamp;
    }

    public String getSourceNodeId() {
        return sourceNodeId;
    }

    public void setSourceNodeId(String sourceNodeId) {
        this.sourceNodeId = sourceNodeId;
    }
}
