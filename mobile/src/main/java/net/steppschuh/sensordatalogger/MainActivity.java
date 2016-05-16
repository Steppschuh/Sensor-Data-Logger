package net.steppschuh.sensordatalogger;

import android.os.Build;
import android.os.Bundle;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import com.google.android.gms.wearable.Wearable;

import net.steppschuh.datalogger.logging.TimeTracker;
import net.steppschuh.datalogger.logging.TrackerManager;
import net.steppschuh.datalogger.message.MessageHandler;
import net.steppschuh.datalogger.message.MessageReceiver;
import net.steppschuh.datalogger.message.SinglePathMessageHandler;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = MainActivity.class.getSimpleName();

    private PhoneApp app;
    private List<MessageHandler> messageHandlers;

    private Button debugButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // get reference to global application
        app = (PhoneApp) getApplicationContext();

        // initialize with context activity if needed
        if (!app.isInitialized() || app.getContextActivity() == null) {
            app.initialize(this);
        }

        setupUi();
        setupMessageHandlers();
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

    private void setupMessageHandlers() {
        messageHandlers = new ArrayList<>();
        messageHandlers.add(getEchoMessageHandler());
    }

    @Override
    protected void onStart() {
        super.onStart();
        for (MessageHandler messageHandler : messageHandlers) {
            app.registerMessageHandler(messageHandler);
        }
        Wearable.MessageApi.addListener(app.getGoogleApiMessenger().getGoogleApiClient(), app);
    }

    @Override
    protected void onStop() {
        for (MessageHandler messageHandler : messageHandlers) {
            app.unregisterMessageHandler(messageHandler);
        }
        Wearable.MessageApi.removeListener(app.getGoogleApiMessenger().getGoogleApiClient(), app);
        super.onStop();
    }

    private MessageHandler getEchoMessageHandler() {
        return new SinglePathMessageHandler(MessageHandler.PATH_ECHO) {
            @Override
            public void handleMessage(Message message) {
                TimeTracker tracker = app.getTrackerManager().getTracker(TrackerManager.KEY_CONNECTION_SPEED_TEST);
                tracker.stop();

                int trackingCount = tracker.getTrackingCount();

                if (trackingCount < 25) {
                    startConnectionSpeedTest();
                } else {
                    stopConnectionSpeedTest();
                }
            }
        };
    }

    private void startConnectionSpeedTest() {
        app.getTrackerManager().getTracker("Connection Speed Test").start();
        try {
            app.getGoogleApiMessenger().sendMessageToAllNodes(MessageHandler.PATH_PING, Build.MODEL);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private void stopConnectionSpeedTest() {
        TimeTracker tracker = app.getTrackerManager().getTracker(TrackerManager.KEY_CONNECTION_SPEED_TEST);
        Log.i(TAG, tracker.toString());
        app.getTrackerManager().getTimeTrackers().remove(tracker);
    }

}
