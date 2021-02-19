/**
 * Basic implementation of a simple logger that writes a JSON stream to the mobile's external storage.
 * JSON might be non-trivial to parse, but the data structure that can be generated per sensor
 * are not uniform so it is probably best to stay flexible.
 *
 * Contributed by Jonathan Liebers (c) 2021, git@jo2k.de
 *
 * Thanks to `Steppschuh` (Stephan Schultz) for the great initial work.
 *
 * License: Apache License 2.0
 */

package de.jo2k.loggingmodule;

import android.os.Environment;
import android.util.Log;

import net.steppschuh.datalogger.data.DataBatch;
import net.steppschuh.sensordatalogger.BuildConfig;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static net.steppschuh.sensordatalogger.PhoneApp.TAG;

public class DataHandler {
    private boolean isRecording = false;
    private FileWriter filewriter;
    private boolean firstEntryWasWritten;
    private File file;
    private ConcurrentLinkedQueue<DataBatch> queue;

    ExecutorService es;
    private Runnable runnable;
    private volatile boolean runnableIsShutdown = false;


    /**
     * Writes newly obtained data to the logfile.
     * User should call DataHandler.startRecording() beforehand to initialize a file.
     * Returns silently if no recording was started beforehand.
     */
    public void logDataBatch(DataBatch dataBatch, String sourceNodeId) {
        if (!isRecording)
            return;

        // Log.v(TAG, sourceNodeId);
        // Log.v(TAG, dataBatch.toString());

        // add sensor reading to queue
        queue.offer(dataBatch);
    }

    /**
     * Transform all DataBatches in the queue to json objects, concatenate them and flush them to
     * file, effectively emptying the queue. Requires an active filewriter-object and should only
     * be called in the background due to processing load (e.g., from another thread).
     */
    private void flushQueue() {
        if (queue.peek() == null) {
            return;  // exit early if queue is empty
        }

        StringBuilder stringBuilder = new StringBuilder();

        // empty queue into a stringbuilder and transform all dataBatches to json
        while(!queue.isEmpty()) {
            // write the comma only if we added some entries before
            if (firstEntryWasWritten) {
                stringBuilder.append(',');
            }
            firstEntryWasWritten = true;
            stringBuilder.append(queue.poll().toJson());
        }

        // write stringbuilder to disk
        try {
            filewriter.write(stringBuilder.toString());
        } catch (IOException e) {
            Log.e(TAG, "FileWriter could not write.");
            e.printStackTrace();
        }
    }

    /**
     * Returns the status of the DataHandler, true if a recording is in progress, false otherwise.
     */
    public boolean isRecording() {
        return isRecording;
    }

    /**
     * Starts the recording of sensors to a file denoted by `filename` by initializing a new
     * file on the external storage.
     */
    public void startRecording(File file) {
        if (isRecording) {
            stopRecording();
        }
        this.file = file;
        this.queue = new ConcurrentLinkedQueue<>();

        // set recording status
        isRecording = true;
        firstEntryWasWritten = false;

        // create folder
        File folder = new File(Environment.getExternalStorageDirectory() + "/" + BuildConfig.APPLICATION_ID + "/");
        if (!folder.exists()) {
            boolean success = folder.mkdirs();
            if (success) {
                Log.v(TAG, "DataHandler created folder " + folder);
            } else {
                Log.e(TAG, "DataHandler could not create folder " + folder);
                isRecording = false;
                return;
            }
        }

        Log.v(TAG, "DataHandler records to file: " + file.getAbsolutePath());

        // Create file and filewriter
        FileWriter filewriter = null;

        // initialize filewriter
        try {
            filewriter = new FileWriter(file, true);
            filewriter.write("[");
        } catch (IOException e) {
            Log.e(TAG, "Could not create filewriter in DataHandler.startRecording()");
            e.printStackTrace();
        }

        // assign local variables
        this.filewriter = filewriter;

        // create a runnable that flushes the queue to file
        this.runnable = new Runnable(){
            @Override
            public void run(){
                // check if runnable has been requested to shut down
                while (! runnableIsShutdown) {
                    // if we are still runninig, peek at queue and see if we have something
                    // to flush. If not, sleep for 10 ms.
                    if (queue.peek() != null) {
                        flushQueue();
                    } else {
                        try {
                            Thread.sleep(10);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                }
                flushQueue();  // flush queue for last time if we should terminate
            }
        };

        // execute the created runnable in a single thread executor
        es =  Executors.newSingleThreadExecutor();
        this.runnableIsShutdown = false;
        es.execute(this.runnable);
    }

    /**
     * Stops the recording.
     * @return File that was finished after stopping the cording.
     */
    public File stopRecording () {
        // terminate early if recording was not started beforehand
        // (should not happen.)
        if (!isRecording)
            return null;

        // deactivate recording
        isRecording = false;

        // stop the flushing of the runnable
        this.runnableIsShutdown = true;

        // shutdown the executor
        es.shutdown();
        try {
            es.awaitTermination(3000, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        // flush for the very last time so we surely have not lost any readings
        flushQueue();

        // write closing bracket to json stream (it is a list) and close the filewriter
        try {
            filewriter.write("]");
            filewriter.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        Log.v(TAG, "DataHandler stopped recording.");

        // return filename of finished file to be showed in a Toast
        return this.file;
    }

    /**
     * Returns the most recently used File by this logger.
     * If stopRecording() was called first, this function returns the last written-to file.
     * If this function is called after startRecording() and before stopRecording() it returns
     * the file that is currently being written to.
     */
    public File getFile() {
        return this.file;
    }
}