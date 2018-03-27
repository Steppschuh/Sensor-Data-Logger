package net.steppschuh.datalogger.logging;

import android.util.Log;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class TimeTracker {

    private static final String TAG = TimeTracker.class.getSimpleName();
    public static final String KEY_DEFAULT = "";

    public static int TRACKING_COUNT_NOT_SET = -1;

    private String key;
    private long startTimestamp;
    private long stopTimestamp;
    private long duration;

    private long totalDuration;
    private int trackingCount;
    private int maximumTrackingCount = TRACKING_COUNT_NOT_SET;

    List<TrackingListener> trackingListeners;

    private Object context;

    public interface TrackingListener {
        void onTrackingFinished(TimeTracker timeTracker);

        void onNewDurationTracked(TimeTracker timeTracker);
    }

    public TimeTracker() {
        key = KEY_DEFAULT;
        trackingCount = 0;
        totalDuration = 0;
        trackingListeners = new ArrayList<>();
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
        startTimestamp = System.nanoTime();
    }

    public void stop() {
        stopTimestamp = System.nanoTime();
        duration = calculateDuration();
        addDuration(duration);
    }

    public void reset() {
        totalDuration = 0;
        trackingCount = 0;
    }

    public void addDuration(long duration) {
        totalDuration += duration;
        trackingCount += 1;
        notifyNewDurationTracked();
        if (trackingCount == maximumTrackingCount) {
            notifyTrackingFinished();
        }
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

    public boolean registerTrackingListener(TrackingListener trackingListener) {
        if (!trackingListeners.contains(trackingListener)) {
            return trackingListeners.add(trackingListener);
        }
        return false;
    }

    public boolean unregisterTrackingListener(TrackingListener trackingListener) {
        if (trackingListeners.contains(trackingListener)) {
            return trackingListeners.remove(trackingListener);
        }
        return false;
    }

    public void notifyTrackingFinished() {
        for (TrackingListener trackingListener : trackingListeners) {
            try {
                trackingListener.onTrackingFinished(this);
            } catch (Exception ex) {
                Log.w(TAG, "Unable to notify tracking listener: " + ex.getMessage());
                ex.printStackTrace();
            }
        }
    }

    public void notifyNewDurationTracked() {
        for (TrackingListener trackingListener : trackingListeners) {
            try {
                trackingListener.onNewDurationTracked(this);
            } catch (Exception ex) {
                Log.w(TAG, "Unable to notify tracking listener: " + ex.getMessage());
                ex.printStackTrace();
            }
        }
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
            long averageDuration = calculateAverageDuration();
            long averageDurationInMillis = TimeUnit.NANOSECONDS.toMillis(averageDuration);
            sb.append("∅ ");
            sb.append(averageDuration).append(" ns (");
            sb.append(averageDurationInMillis).append(" ms ");
            sb.append(trackingCount).append(" times tracked)");
        } else {
            sb.append(startTimestamp).append(" - ").append(stopTimestamp);
            sb.append(" (").append(calculateDuration()).append("ns)");
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

    public int getMaximumTrackingCount() {
        return maximumTrackingCount;
    }

    public void setMaximumTrackingCount(int maximumTrackingCount) {
        this.maximumTrackingCount = maximumTrackingCount;
    }
}
