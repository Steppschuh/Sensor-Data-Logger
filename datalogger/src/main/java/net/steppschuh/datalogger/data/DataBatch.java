package net.steppschuh.datalogger.data;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class DataBatch {

    public static final int CAPACITY_UNLIMITED = -1;
    public static final int CAPACITY_DEFAULT = 200;

    private String source;
    private List<Data> dataList;
    private int capacity;

    public DataBatch() {
        dataList = new ArrayList<>();
        capacity = CAPACITY_DEFAULT;
    }

    public DataBatch(DataBatch dataBatch) {
        source = dataBatch.getSource();
        dataList = new ArrayList<>(dataBatch.getDataList().size());
        dataList.addAll(dataBatch.getDataList());
        capacity = dataBatch.capacity;
    }

    public DataBatch(List<Data> dataList) {
        this();
        this.dataList = dataList;
    }

    public DataBatch(String source) {
        this();
        this.source = source;
    }

    private void trimDataToCapacity() {
        // check if there's a capacity limit
        if (capacity == CAPACITY_UNLIMITED) {
            return;
        }

        // check if trimming is needed
        if (dataList == null || dataList.size() < capacity) {
            return;
        }

        // remove oldest data
        while (dataList.size() > capacity) {
            dataList.remove(0);
        }
    }

    public void addData(Data data) {
        dataList.add(data);
        trimDataToCapacity();
    }

    public Data getNewestData() {
        if (dataList == null || dataList.size() < 1) {
            return null;
        }
        return dataList.get(dataList.size() - 1);
    }

    public Data getOldestData() {
        if (dataList == null || dataList.size() < 1) {
            return null;
        }
        return dataList.get(0);
    }

    public List<Data> getDataSince(long timestamp) {
        List<Data> dataSince = new ArrayList<>();
        for (int i = dataList.size() - 1; i >= 0; i--) {
            if (dataList.get(i).getTimestamp() > timestamp) {
                dataSince.add(dataList.get(i));
            } else {
                break;
            }
        }
        Collections.reverse(dataSince);
        return dataSince;
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

    /**
     * Getter & Setter
     */
    public List<Data> getDataList() {
        return dataList;
    }

    public void setDataList(List<Data> dataList) {
        this.dataList = dataList;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public int getCapacity() {
        return capacity;
    }

    public void setCapacity(int capacity) {
        this.capacity = capacity;
    }
}
