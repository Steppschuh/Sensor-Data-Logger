package net.steppschuh.datalogger.data;

public class Data {

    private long timestamp;
    private String source;
    private float[] values;

    public Data() {
        timestamp = System.currentTimeMillis();
    }

    public Data(String source, float[] values) {
        this();
        this.source = source;
        this.values = values;
    }

    /**
     * Getter & Setter
     */
    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public float[] getValues() {
        return values;
    }

    public void setValues(float[] values) {
        this.values = values;
    }

}
