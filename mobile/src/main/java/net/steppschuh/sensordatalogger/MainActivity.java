package net.steppschuh.sensordatalogger;

import android.os.Bundle;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import com.google.android.gms.wearable.Wearable;

import net.steppschuh.datalogger.SharedConstants;
import net.steppschuh.datalogger.logging.TimeTracker;
import net.steppschuh.datalogger.message.MessageReceiver;

public class MainActivity extends AppCompatActivity implements MessageReceiver {

    private static final String TAG = MainActivity.class.getSimpleName();
    private PhoneApp app;
    private Button debugButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // get reference to the global application class
        app = (PhoneApp) getApplicationContext();

        // initialize with context activity if needed
        if (!app.isInitialized() || app.getContextActivity() == null) {
            app.initialize(this);
        }

        setupUi();
    }

    private void setupUi() {
        setContentView(R.layout.activity_main);

        debugButton = (Button) findViewById(R.id.debugButton);
        debugButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startConnectionSpeedTest();
            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        app.registerMessageReceiver(this);
        Wearable.MessageApi.addListener(app.getGoogleApiMessenger().getGoogleApiClient(), app);
    }

    @Override
    protected void onStop() {
        app.unregisterMessageReceiver(this);
        Wearable.MessageApi.removeListener(app.getGoogleApiMessenger().getGoogleApiClient(), app);
        super.onStop();
    }

    @Override
    public void onMessageReceived(Message message) {
        String path = message.getData().getString(SharedConstants.KEY_PATH);
        switch (path) {
            case SharedConstants.MESSAGE_PATH_ECHO: {
                TimeTracker tracker = app.getTrackerManager().getTracker("Connection Speed Test");
                tracker.stop();

                int trackingCount = tracker.getTrackingCount();

                if (trackingCount < 25) {
                    startConnectionSpeedTest();
                } else {
                    stopConnectionSpeedTest();
                }
                break;
            }
            default: {
                Log.w(TAG, "Unable to handle message: " + path);
                break;
            }
        }
    }

    private void startConnectionSpeedTest() {
        app.getTrackerManager().getTracker("Connection Speed Test").start();
        try {
            app.getGoogleApiMessenger().sendMessageToAllNodes(SharedConstants.MESSAGE_PATH_PING, "");
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private void stopConnectionSpeedTest() {
        TimeTracker tracker = app.getTrackerManager().getTracker("Connection Speed Test");
        Log.i(TAG, tracker.toString());
        app.getTrackerManager().getTimeTrackers().remove(tracker);
    }

}
