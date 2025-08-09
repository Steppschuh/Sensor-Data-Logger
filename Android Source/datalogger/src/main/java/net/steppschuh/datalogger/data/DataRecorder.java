package net.steppschuh.datalogger.data;

import android.content.Context;
import android.util.Log;

import net.steppschuh.datalogger.sensor.DeviceSensor;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class DataRecorder {

    private static final String TAG = DataRecorder.class.getSimpleName();

    private boolean recording;
    private File recordingDirectory;
    private Map<String, FileWriter> fileWriters = new ConcurrentHashMap<>();
    private Map<String, StringBuilder> dataBuffers = new ConcurrentHashMap<>();
    private Map<String, Boolean> headerWritten = new ConcurrentHashMap<>();
    private ScheduledExecutorService scheduler;
    private Context context;

    public DataRecorder(Context context) {
        this.context = context;
    }

    public void start(Map<String, List<DeviceSensor>> selectedSensors) {
        if (recording) {
            return;
        }
        recording = true;

        // create directory for recordings
        String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        File baseDir = context.getExternalFilesDir(null);
        recordingDirectory = new File(baseDir, "SensorDataLogger" + File.separator + timestamp);
        if (!recordingDirectory.mkdirs()) {
            Log.e(TAG, "Directory not created");
        }

        for (Map.Entry<String, List<DeviceSensor>> entry : selectedSensors.entrySet()) {
            for (DeviceSensor sensor : entry.getValue()) {
                String key = sensor.getStringType() + "_" + sensor.getName();
                try {
                    File file = new File(recordingDirectory, key + ".csv");
                    fileWriters.put(key, new FileWriter(file));
                    dataBuffers.put(key, new StringBuilder());
                    headerWritten.put(key, false);
                } catch (IOException e) {
                    Log.e(TAG, "Could not create file writer for " + key, e);
                }
            }
        }

        // schedule a task to write buffer to file every second
        scheduler = Executors.newSingleThreadScheduledExecutor();
        scheduler.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                flushBuffers();
            }
        }, 1, 1, TimeUnit.SECONDS);

        Log.d(TAG, "Started recording to " + recordingDirectory.getAbsolutePath());
    }

    public void stop() {
        if (!recording) {
            return;
        }
        recording = false;

        if (scheduler != null) {
            scheduler.shutdown();
        }

        flushBuffers();

        for (FileWriter writer : fileWriters.values()) {
            try {
                writer.close();
            } catch (IOException e) {
                Log.e(TAG, "Could not close file writer", e);
            }
        }
        fileWriters.clear();
        dataBuffers.clear();
        headerWritten.clear();

        Log.d(TAG, "Stopped recording");
    }

    public void onDataChanged(DataBatch dataBatch) {
        if (!recording) {
            return;
        }
        String key = dataBatch.getSource() + "_" + dataBatch.getType();
        StringBuilder buffer = dataBuffers.get(key);
        if (buffer != null) {
            if (!headerWritten.get(key)) {
                if (dataBatch.getDataList().size() > 0) {
                    Data firstData = dataBatch.getDataList().get(0);
                    StringBuilder header = new StringBuilder("timestamp");
                    for (int i = 0; i < firstData.getValues().length; i++) {
                        header.append(",value_").append(i);
                    }
                    header.append('\n');
                    buffer.append(header.toString());
                    headerWritten.put(key, true);
                }
            }

            for (Data data : dataBatch.getDataList()) {
                buffer.append(data.getTimestamp());
                for (float value : data.getValues()) {
                    buffer.append(',').append(value);
                }
                buffer.append('\n');
            }
        }
    }

    private void flushBuffers() {
        for (Map.Entry<String, StringBuilder> entry : dataBuffers.entrySet()) {
            try {
                FileWriter writer = fileWriters.get(entry.getKey());
                if (writer != null && entry.getValue().length() > 0) {
                    writer.write(entry.getValue().toString());
                    entry.getValue().setLength(0);
                }
            } catch (IOException e) {
                Log.e(TAG, "Could not write to file", e);
            }
        }
    }

    public boolean isRecording() {
        return recording;
    }

    public File getRecordingDirectory() {
        return recordingDirectory;
    }
}
