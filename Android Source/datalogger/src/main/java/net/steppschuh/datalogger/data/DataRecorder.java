package net.steppschuh.datalogger.data;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;

import net.steppschuh.datalogger.sensor.DeviceSensor;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
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
    private String recordingDirectoryName;
    private Map<String, Object> fileWriters = new ConcurrentHashMap<>();
    private Map<String, StringBuilder> dataBuffers = new ConcurrentHashMap<>();
    private Map<String, Boolean> headerWritten = new ConcurrentHashMap<>();
    private ScheduledExecutorService scheduler;
    private Context context;
    private boolean useMediaStore;

    public DataRecorder(Context context) {
        this.context = context;
        this.useMediaStore = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q;
    }

    public void start(Map<String, List<DeviceSensor>> selectedSensors) {
        if (recording) {
            return;
        }
        recording = true;

        String timestamp = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.getDefault()).format(new Date());
        recordingDirectoryName = "SensorDataLogger/" + timestamp;

        if (!useMediaStore) {
            File baseDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS);
            if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
                recordingDirectory = new File(baseDir, recordingDirectoryName);
                if (!recordingDirectory.exists()) {
                    if (!recordingDirectory.mkdirs()) {
                        Log.e(TAG, "External directory not created, falling back to internal storage");
                        recordingDirectory = context.getDir(recordingDirectoryName, Context.MODE_PRIVATE);
                        if (!recordingDirectory.exists()) {
                            if (!recordingDirectory.mkdirs()) {
                                Log.e(TAG, "Internal directory not created");
                            }
                        }
                    }
                }
            } else {
                Log.e(TAG, "External storage not available, falling back to internal storage");
                recordingDirectory = context.getDir(recordingDirectoryName, Context.MODE_PRIVATE);
                if (!recordingDirectory.exists()) {
                    if (!recordingDirectory.mkdirs()) {
                        Log.e(TAG, "Internal directory not created");
                    }
                }
            }
        }

        for (Map.Entry<String, List<DeviceSensor>> entry : selectedSensors.entrySet()) {
            for (DeviceSensor sensor : entry.getValue()) {
                String key = sensor.getName().replaceAll("[^a-zA-Z0-9_\\-]", "_");
                try {
                    if (useMediaStore) {
                        ContentResolver resolver = context.getContentResolver();
                        ContentValues values = new ContentValues();
                        values.put(MediaStore.MediaColumns.DISPLAY_NAME, key + ".csv");
                        values.put(MediaStore.MediaColumns.MIME_TYPE, "text/csv");
                        values.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOCUMENTS + "/" + recordingDirectoryName);

                        Uri uri = resolver.insert(MediaStore.Files.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY), values);
                        if (uri != null) {
                            OutputStream outputStream = resolver.openOutputStream(uri);
                            if (outputStream != null) {
                                fileWriters.put(key, new OutputStreamWriter(outputStream));
                            }
                        }
                    } else {
                        File file = new File(recordingDirectory, key + ".csv");
                        fileWriters.put(key, new FileWriter(file));
                    }
                    dataBuffers.put(key, new StringBuilder());
                    headerWritten.put(key, false);
                } catch (IOException e) {
                    Log.e(TAG, "Could not create file writer for " + key, e);
                }
            }
        }

        // schedule a task to write buffer to file every second
        scheduler = Executors.newSingleThreadScheduledExecutor();
        scheduler.scheduleWithFixedDelay(new Runnable() {
            @Override
            public void run() {
                flushBuffers();
            }
        }, 1, 1, TimeUnit.SECONDS);

        String location = useMediaStore ? "MediaStore " + Environment.DIRECTORY_DOCUMENTS + "/" + recordingDirectoryName : recordingDirectory.getAbsolutePath();
        Log.d(TAG, "Started recording to " + location);
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

        for (Object writer : fileWriters.values()) {
            try {
                if (writer instanceof FileWriter) {
                    ((FileWriter) writer).close();
                } else if (writer instanceof OutputStreamWriter) {
                    ((OutputStreamWriter) writer).close();
                }
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
        String key = dataBatch.getSource().replaceAll("[^a-zA-Z0-9_\\-]", "_");
        StringBuilder buffer = dataBuffers.get(key);
        if (buffer != null) {
            if (!headerWritten.get(key)) {
                if (!dataBatch.getDataList().isEmpty()) {
                    Data firstData = dataBatch.getDataList().get(0);
                    StringBuilder header = new StringBuilder("timestamp");
                    for (int i = 0; i < firstData.getValues().length; i++) {
                        header.append(",value_").append(i);
                    }
                    header.append('\n');
                    buffer.append(header.toString());
                    headerWritten.put(key, true);
                    Log.d(TAG, "Wrote header for " + key);
                }
            }

            for (Data data : dataBatch.getDataList()) {
                buffer.append(data.getTimestamp());
                for (float value : data.getValues()) {
                    buffer.append(',').append(value);
                }
                buffer.append('\n');
            }
            //Log.v(TAG, "Added " + dataBatch.getDataList().size() + " data points to " + key + " buffer");
        } else {
            Log.w(TAG, "No buffer found for key: " + key + " (source: " + dataBatch.getSource() + ")");
        }
    }

    private void flushBuffers() {
        for (Map.Entry<String, StringBuilder> entry : dataBuffers.entrySet()) {
            try {
                Object writer = fileWriters.get(entry.getKey());
                if (writer != null && entry.getValue().length() > 0) {
                    String data = entry.getValue().toString();
                    if (writer instanceof FileWriter) {
                        ((FileWriter) writer).write(data);
                    } else if (writer instanceof OutputStreamWriter) {
                        ((OutputStreamWriter) writer).write(data);
                        ((OutputStreamWriter) writer).flush();
                    }
                    Log.v(TAG, "Flushed " + data.length() + " characters for " + entry.getKey());
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

    public String getRecordingPath() {
        return useMediaStore ? recordingDirectoryName : (recordingDirectory != null ? recordingDirectory.getAbsolutePath() : null);
    }
}
