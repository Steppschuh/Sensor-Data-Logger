package net.steppschuh.datalogger.logging;

import java.util.Date;

public class TimeTracker {

    public static final String KEY_DEFAULT = "";

    String key;
    long startTimestamp;
    long stopTimestamp;
    long duration;
    Object context;

    public TimeTracker() {
        key = KEY_DEFAULT;
    }

    public TimeTracker(Object context) {
        this();
        this.context = context;
    }

    public TimeTracker(String key) {
        this.key = key;
    }

    public void start() {
        startTimestamp = (new Date()).getTime();
    }

    public void stop() {
        stopTimestamp = (new Date()).getTime();
        duration = calculateDuration();
    }

    public long calculateDuration() {
        if (hasStartedAndStopped()) {
            return stopTimestamp - startTimestamp;
        }
        return 0;
    }

    public boolean hasStartedAndStopped() {
        return startTimestamp > 0 && stopTimestamp > 0;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        if (key != null && key.length() > 0) {
            sb.append(key).append(": ");
        }
        sb.append(startTimestamp).append(" - ").append(stopTimestamp);
        sb.append(" (").append(calculateDuration()).append("ms)");
        if (context != null) {
            sb.append(" ").append(context);
        }
        return sb.toString();
    }

    /**
     * Getter & Setter
     */
    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public long getStartTimestamp() {
        return startTimestamp;
    }

    public void setStartTimestamp(long startTimestamp) {
        this.startTimestamp = startTimestamp;
    }

    public long getStopTimestamp() {
        return stopTimestamp;
    }

    public void setStopTimestamp(long stopTimestamp) {
        this.stopTimestamp = stopTimestamp;
    }

    public long getDuration() {
        return duration;
    }

    public void setDuration(long duration) {
        this.duration = duration;
    }

    public Object getContext() {
        return context;
    }

    public void setContext(Object context) {
        this.context = context;
    }

}
