package net.steppschuh.sensordatalogger;

import android.os.Bundle;
import android.os.Message;
import android.support.wearable.activity.WearableActivity;
import android.support.wearable.view.BoxInsetLayout;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import com.google.android.gms.wearable.Wearable;

import net.steppschuh.datalogger.SharedConstants;
import net.steppschuh.datalogger.message.MessageReceiver;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class MainActivityWear extends WearableActivity {

    private static final String TAG = MainActivityWear.class.getSimpleName();

    private WearApp app;

    private static final SimpleDateFormat AMBIENT_DATE_FORMAT = new SimpleDateFormat("HH:mm", Locale.US);

    private BoxInsetLayout mContainerView;
    private TextView mTextView;
    private TextView mClockView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // get reference to global application
        app = (WearApp) getApplicationContext();

        // initialize with context activity if needed
        if (!app.isInitialized() || app.getContextActivity() == null) {
            app.initialize(this);
        }

        setupUi();
    }

    private void setupUi() {
        setContentView(R.layout.main_activity_wear);
        setAmbientEnabled();

        mContainerView = (BoxInsetLayout) findViewById(R.id.container);
        mTextView = (TextView) findViewById(R.id.text);
        mClockView = (TextView) findViewById(R.id.clock);

        mContainerView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {

                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        Wearable.MessageApi.addListener(app.getGoogleApiMessenger().getGoogleApiClient(), app);
    }

    @Override
    protected void onStop() {
        Wearable.MessageApi.removeListener(app.getGoogleApiMessenger().getGoogleApiClient(), app);
        super.onStop();
    }

    @Override
    public void onEnterAmbient(Bundle ambientDetails) {
        super.onEnterAmbient(ambientDetails);
        updateDisplay();
    }

    @Override
    public void onUpdateAmbient() {
        super.onUpdateAmbient();
        updateDisplay();
    }

    @Override
    public void onExitAmbient() {
        updateDisplay();
        super.onExitAmbient();
    }

    private void updateDisplay() {
        if (isAmbient()) {
            mContainerView.setBackgroundColor(getResources().getColor(android.R.color.black));
            mTextView.setTextColor(getResources().getColor(android.R.color.white));
            mClockView.setVisibility(View.VISIBLE);

            mClockView.setText(AMBIENT_DATE_FORMAT.format(new Date()));
        } else {
            mContainerView.setBackground(null);
            mTextView.setTextColor(getResources().getColor(android.R.color.black));
            mClockView.setVisibility(View.GONE);
        }
    }
}
