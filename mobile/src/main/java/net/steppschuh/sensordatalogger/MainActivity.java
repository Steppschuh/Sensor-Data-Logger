package net.steppschuh.sensordatalogger;

import android.os.Bundle;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import net.steppschuh.datalogger.SharedConstants;
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
                try {
                    app.getGoogleApiMessenger().sendMessageToAllNodes(SharedConstants.MESSAGE_PATH_GET_STATUS, "Test");
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        app.registerMessageReceiver(this);
    }

    @Override
    protected void onStop() {
        app.unregisterMessageReceiver(this);
        super.onStop();
    }

    @Override
    public void onMessageReceived(Message message) {
        Log.v(TAG, "onMessageReceived");
    }
}
