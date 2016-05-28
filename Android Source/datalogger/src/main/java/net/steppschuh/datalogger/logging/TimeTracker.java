package net.steppschuh.datalogger.logging;

import java.util.Date;

public class TimeTracker {

    public static final String KEY_DEFAULT = "";

    private String key;
    private long startTimestamp;
    private long stopTimestamp;
    private long duration;

    private long totalDuration;
    private int trackingCount;

    private Object context;

    public TimeTracker() {
        key = KEY_DEFAULT;
        trackingCount = 0;
        totalDuration = 0;
    }

    public TimeTracker(String key) {
        this();
        this.key = key;
    }

    public TimeTracker(String key, Object context) {
        this();
        this.key = key;
        this.context = context;
    }

    public void start() {
        startTimestamp = System.currentTimeMillis();
    }

    public void stop() {
        stopTimestamp = System.currentTimeMillis();
        duration = calculateDuration();
        totalDuration += duration;
        trackingCount += 1;
    }

    public long calculateDuration() {
        if (hasStartedAndStopped()) {
            return stopTimestamp - startTimestamp;
        }
        return 0;
    }

    public long calculateAverageDuration() {
        if (trackingCount > 0) {
            return totalDuration / trackingCount;
        }
        return 0;
    }

    public boolean isTracking() {
        return startTimestamp > 0 && stopTimestamp < 1;
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
        if (trackingCount > 1) {
            sb.append("∅ ").append(calculateAverageDuration()).append("ms");
            sb.append(" (").append(trackingCount).append(" times tracked)");
        } else {
            sb.append(startTimestamp).append(" - ").append(stopTimestamp);
            sb.append(" (").append(calculateDuration()).append("ms)");
        }
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

    public long getTotalDuration() {
        return totalDuration;
    }

    public void setTotalDuration(long totalDuration) {
        this.totalDuration = totalDuration;
    }

    public int getTrackingCount() {
        return trackingCount;
    }

    public void setTrackingCount(int trackingCount) {
        this.trackingCount = trackingCount;
    }
}
