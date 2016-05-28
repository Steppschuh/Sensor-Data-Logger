package net.steppschuh.datalogger.data;

public interface DataChangedListener {

    public void onDataChanged(DataBatch dataBatch, String sourceNodeId);

}
