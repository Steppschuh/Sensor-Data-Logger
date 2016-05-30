package net.steppschuh.sensordatalogger;

import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.Message;
import android.support.wearable.activity.WearableActivity;
import android.support.wearable.view.BoxInsetLayout;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import com.google.android.gms.wearable.Wearable;

import net.steppschuh.datalogger.messaging.handler.MessageHandler;
import net.steppschuh.datalogger.messaging.handler.SinglePathMessageHandler;
import net.steppschuh.datalogger.status.ActivityStatus;
import net.steppschuh.datalogger.status.Status;
import net.steppschuh.datalogger.status.StatusUpdateEmitter;
import net.steppschuh.datalogger.status.StatusUpdateHandler;
import net.steppschuh.datalogger.status.StatusUpdateReceiver;

import java.util.ArrayList;
import java.util.List;

public class MainActivityWear extends WearableActivity implements StatusUpdateEmitter {

    private static final String TAG = MainActivityWear.class.getSimpleName();

    private WearApp app;

    private List<MessageHandler> messageHandlers;
    private ActivityStatus status = new ActivityStatus();
    private StatusUpdateHandler statusUpdateHandler;

    private BoxInsetLayout mContainerView;
    private TextView mainTextView;
    private TextView preTextView;
    private TextView postTextView;
    private TextView logTextView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // get reference to global application
        app = (WearApp) getApplicationContext();

        // initialize with context activity if needed
        if (!app.getStatus().isInitialized() || app.getContextActivity() == null) {
            app.initialize(this);
        }

        setupUi();
        setupMessageHandlers();
        setupStatusUpdates();

        status.setInitialized(true);
        status.updated(statusUpdateHandler);
    }

    private void setupUi() {
        setContentView(R.layout.main_activity_wear);
        setAmbientEnabled();

        mContainerView = (BoxInsetLayout) findViewById(R.id.container);
        mainTextView = (TextView) findViewById(R.id.mainText);
        preTextView = (TextView) findViewById(R.id.preText);
        postTextView = (TextView) findViewById(R.id.postText);
        logTextView = (TextView) findViewById(R.id.logText);

        mContainerView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

            }
        });

        updateDisplay();
    }

    private void setupMessageHandlers() {
        messageHandlers = new ArrayList<>();
    }

    private void setupStatusUpdates() {
        statusUpdateHandler = new StatusUpdateHandler();
        statusUpdateHandler.registerStatusUpdateReceiver(new StatusUpdateReceiver() {
            @Override
            public void onStatusUpdated(Status status) {
                app.getStatus().setActivityStatus((ActivityStatus) status);
                app.getStatus().updated(app.getStatusUpdateHandler());
            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        // register message handlers
        for (MessageHandler messageHandler : messageHandlers) {
            app.registerMessageHandler(messageHandler);
        }
        Wearable.MessageApi.addListener(app.getGoogleApiMessenger().getGoogleApiClient(), app);

        // update status
        status.setInForeground(true);
        status.updated(statusUpdateHandler);

        // update reachabilities of nearby nodes
        app.getReachabilityChecker().checkReachabilities(null);
    }

    @Override
    protected void onStop() {
        // let other devices know that the app won't be reachable anymore
        app.getGoogleApiMessenger().sendMessageToNearbyNodes(MessageHandler.PATH_CLOSING, Build.MODEL);

        // unregister message handlers
        for (MessageHandler messageHandler : messageHandlers) {
            app.unregisterMessageHandler(messageHandler);
        }
        Wearable.MessageApi.removeListener(app.getGoogleApiMessenger().getGoogleApiClient(), app);

        // update status
        status.setInForeground(false);
        status.updated(statusUpdateHandler);
        super.onStop();
    }

    @Override
    public void onEnterAmbient(Bundle ambientDetails) {
        super.onEnterAmbient(ambientDetails);
        updateDisplay();
        status.setAmbientMode(true);
        status.updated(statusUpdateHandler);
    }

    @Override
    public void onUpdateAmbient() {
        super.onUpdateAmbient();
        updateDisplay();
    }

    @Override
    public void onExitAmbient() {
        updateDisplay();
        status.setAmbientMode(false);
        status.updated(statusUpdateHandler);
        super.onExitAmbient();
    }

    private void updateDisplay() {
        if (isAmbient()) {
            mContainerView.setBackgroundColor(Color.BLACK);

            mainTextView.setTextColor(Color.WHITE);
            preTextView.setTextColor(Color.GRAY);
            postTextView.setTextColor(Color.GRAY);
            logTextView.setTextColor(Color.GRAY);

            preTextView.setVisibility(View.GONE);
            postTextView.setVisibility(View.GONE);
            logTextView.setVisibility(View.GONE);
        } else {
            mContainerView.setBackground(null);

            mainTextView.setTextColor(Color.BLACK);
            preTextView.setTextColor(Color.BLACK);
            postTextView.setTextColor(Color.BLACK);
            logTextView.setTextColor(Color.GRAY);

            preTextView.setVisibility(View.VISIBLE);
            postTextView.setVisibility(View.VISIBLE);
            logTextView.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public Status getStatus() {
        return status;
    }

    @Override
    public StatusUpdateHandler getStatusUpdateHandler() {
        return statusUpdateHandler;
    }

}
