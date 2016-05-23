package net.steppschuh.datalogger.data;

import java.util.List;

public class SensorDataRequest extends DataRequest {

    private List<Integer> sensorTypes;

    public SensorDataRequest(List<Integer> sensorTypes) {
        super();
        this.sensorTypes = sensorTypes;
    }

    public SensorDataRequest(String sourceNodeId, List<Integer> sensorTypes) {
        super(sourceNodeId);
        this.sensorTypes = sensorTypes;
    }

    public List<Integer> getSensorTypes() {
        return sensorTypes;
    }

    public void setSensorTypes(List<Integer> sensorTypes) {
        this.sensorTypes = sensorTypes;
    }

}
