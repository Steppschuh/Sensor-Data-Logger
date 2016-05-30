package net.steppschuh.datalogger.data.request;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import java.util.List;

public class SensorDataRequest extends DataRequest {

    private List<Integer> sensorTypes;

    public SensorDataRequest() {
    }

    public SensorDataRequest(List<Integer> sensorTypes) {
        super();
        this.sensorTypes = sensorTypes;
    }

    public SensorDataRequest(String sourceNodeId, List<Integer> sensorTypes) {
        super(sourceNodeId);
        this.sensorTypes = sensorTypes;
    }

    public static SensorDataRequest fromJson(String json) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            mapper.disable(SerializationFeature.FAIL_ON_EMPTY_BEANS);
            SensorDataRequest dataRequest = mapper.readValue(json, SensorDataRequest.class);
            return dataRequest;
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return null;
    }

    public List<Integer> getSensorTypes() {
        return sensorTypes;
    }

    public void setSensorTypes(List<Integer> sensorTypes) {
        this.sensorTypes = sensorTypes;
    }

}
