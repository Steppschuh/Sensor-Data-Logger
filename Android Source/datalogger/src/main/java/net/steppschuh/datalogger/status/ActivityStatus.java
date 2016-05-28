package net.steppschuh.datalogger.status;

public class ActivityStatus extends Status {

    private boolean initialized = false;
    private boolean inForeground = true;
    private boolean ambientMode = false;

    public boolean isInitialized() {
        return initialized;
    }

    public void setInitialized(boolean initialized) {
        this.initialized = initialized;
    }

    public boolean isInForeground() {
        return inForeground;
    }

    public void setInForeground(boolean inForeground) {
        this.inForeground = inForeground;
    }

    public boolean isAmbientMode() {
        return ambientMode;
    }

    public void setAmbientMode(boolean ambientMode) {
        this.ambientMode = ambientMode;
    }

}
