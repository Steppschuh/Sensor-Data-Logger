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

import static net.steppschuh.sensordatalogger.PhoneApp.TAG;

public class DataHandler {
    private boolean isRecording = false;
    private FileWriter filewriter;
    private String filename;

    public void logDataBatch(DataBatch dataBatch, String sourceNodeId) {
        /**
         * Writes newly obtained data to the logfile.
         * User should call DataHandler.startRecording() beforehand to initialize a file.
         * Returns silently if no recording was started beforehand.
         */
        if (!isRecording)
            return;

        // Log.v(TAG, sourceNodeId);
        // Log.v(TAG, dataBatch.toString());

        try {
            filewriter.write(dataBatch.toJson());
        } catch (IOException e) {
            Log.e(TAG, "FileWriter could not write.");
            e.printStackTrace();
        }
    }

    public boolean isRecording() {
        /**
         * Returns the status of the DataHandler, true if a recording is in progress, false otherwise.
         */
        return isRecording;
    }

    public void startRecording(String filename) {
        /**
         * Starts the recording of sensors to a file denoted by `filename` by initializing a new
         * file on the external storage.
         */

        if (isRecording) {
            stopRecording();
        }

        // set recording status
        isRecording = true;

        // set path
        String path = Environment.getExternalStorageDirectory() + "/" + BuildConfig.APPLICATION_ID + "/";

        // create folder
        File folder = new File(path);
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

        // determine final path to write to
        this.filename = path + filename + ".json";
        Log.v(TAG, "DataHandler records to file: " + this.filename);

        // Create file and filewriter
        File file = new File(this.filename);
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
    }

    public String stopRecording () {
        // terminate early if recording was not started beforehand
        // (should not happen.)
        if (! isRecording)
            return null;

        // deactivate recording
        isRecording = false;

        // write closing bracket to json stream (it is a list) and close the filewriter
        try {
            filewriter.write("]");
            filewriter.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        Log.v(TAG, "DataHandler stopped recording.");

        // return filename of finished file to be showed in a Toast
        return " " + filename;
    }
}