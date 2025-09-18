package net.steppschuh.datalogger.status;

public interface StatusUpdateEmitter {

    public Status getStatus();

    public StatusUpdateHandler getStatusUpdateHandler();

}
