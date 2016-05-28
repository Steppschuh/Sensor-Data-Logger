package net.steppschuh.datalogger.logging;

import java.util.ArrayList;
import java.util.List;

public final class TrackerManager {

    public static final String KEY_CONNECTION_SPEED_TEST = "Connection Speed Test";

    List<TimeTracker> timeTrackers;

    public TrackerManager() {
        timeTrackers = new ArrayList<>();
    }

    public List<TimeTracker> getActiveTrackers() {
        return getActiveTrackers(timeTrackers);
    }

    /**
     * Returns a list of trackers that have been started but not stopped yet
     */
    public static List<TimeTracker> getActiveTrackers(List<TimeTracker> timeTrackers) {
        List<TimeTracker> activeTrackers = new ArrayList<>();
        for (TimeTracker timeTracker : timeTrackers) {
            if (timeTracker.isTracking()) {
                activeTrackers.add(timeTracker);
            }
        }
        return activeTrackers;
    }

    public TimeTracker getTracker(String key) {
        return getTracker(key, true);
    }

    public TimeTracker getTracker(String key, boolean createIfNotExisting) {
        TimeTracker tracker = getTracker(key, timeTrackers);
        if (tracker == null && createIfNotExisting) {
            tracker = new TimeTracker(key);
            timeTrackers.add(tracker);
            return tracker;
        } else {
            return tracker;
        }
    }

    public static TimeTracker getTracker(String key, List<TimeTracker> timeTrackers) {
        for (TimeTracker timeTracker : timeTrackers) {
            if (timeTracker.getKey().equals(key)) {
                return timeTracker;
            }
        }
        return null;
    }

    public void removeTracker(String key) {
        TimeTracker tracker = getTracker(key, timeTrackers);
        if (tracker != null) {
            timeTrackers.remove(tracker);
        }
    }

    /**
     * Getter & Setter
     */
    public List<TimeTracker> getTimeTrackers() {
        return timeTrackers;
    }

    public void setTimeTrackers(List<TimeTracker> timeTrackers) {
        this.timeTrackers = timeTrackers;
    }
}
