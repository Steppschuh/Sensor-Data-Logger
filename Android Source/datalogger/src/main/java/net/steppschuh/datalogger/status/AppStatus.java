package net.steppschuh.datalogger.status;

import android.os.Build;

public class AppStatus extends Status {

    private boolean initialized = false;
    private String deviceName = Build.MODEL;
    private ActivityStatus activityStatus = new ActivityStatus();
    private GoogleApiStatus googleApiStatus = new GoogleApiStatus();

    public boolean isInitialized() {
        return initialized;
    }

    public void setInitialized(boolean initialized) {
        this.initialized = initialized;
    }

    public ActivityStatus getActivityStatus() {
        return activityStatus;
    }

    public void setActivityStatus(ActivityStatus activityStatus) {
        this.activityStatus = activityStatus;
    }

    public GoogleApiStatus getGoogleApiStatus() {
        return googleApiStatus;
    }

    public void setGoogleApiStatus(GoogleApiStatus googleApiStatus) {
        this.googleApiStatus = googleApiStatus;
    }
}
