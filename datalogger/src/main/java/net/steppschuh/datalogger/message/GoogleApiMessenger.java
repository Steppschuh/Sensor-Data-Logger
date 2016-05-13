package net.steppschuh.datalogger.message;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.MessageApi;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.NodeApi;
import com.google.android.gms.wearable.Wearable;

import java.nio.charset.Charset;

public class GoogleApiMessenger implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {

    private static final String TAG = GoogleApiMessenger.class.getSimpleName();
    private final Charset UTF8_CHARSET = Charset.forName("UTF-8");

    private GoogleApiClient googleApiClient;

    public GoogleApiMessenger(Context context) {
        initialize(context);
    }

    private void initialize(Context context) {
        Log.d(TAG, "Initializing Google API client");
        googleApiClient = new GoogleApiClient.Builder(context)
                .addConnectionCallbacks(this)
                .addApiIfAvailable(Wearable.API)
                .build();
    }

    public boolean connect() {
        Log.d(TAG, "Connecting Google API client");
        if (googleApiClient != null && !googleApiClient.isConnected()) {
            googleApiClient.connect();
            return true;
        }
        return false;
    }

    public boolean disconnect() {
        Log.d(TAG, "Disconnecting Google API client");
        if (googleApiClient != null && googleApiClient.isConnected()) {
            googleApiClient.disconnect();
            return true;
        }
        return false;
    }

    @Override
    public void onConnected(Bundle bundle) {
        Log.d(TAG, "Connected");
    }

    @Override
    public void onConnectionSuspended(int i) {
        Log.d(TAG, "Connection suspended");
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        if (connectionResult.getErrorCode() == ConnectionResult.API_UNAVAILABLE) {
            // The Wearable API is unavailable
        }
        Log.w(TAG, "Google API client connection failed: " + connectionResult.getErrorMessage());
    }

    public void sendMessageToAllNodes(final String path, final String data) throws Exception {
        sendMessageToAllNodes(path, data.getBytes(UTF8_CHARSET));
    }

    public void sendMessageToAllNodes(final String path, final byte[] data) throws Exception {
        if (!googleApiClient.isConnected()) {
            throw new Exception("Google API client is not connected");
        }
        new Thread( new Runnable() {
            @Override
            public void run() {
                NodeApi.GetConnectedNodesResult nodes = Wearable.NodeApi.getConnectedNodes(googleApiClient).await();
                for (Node node : nodes.getNodes()) {
                    try {
                        sendMessageToNode(path, data, node);
                    } catch (Exception ex) {
                        Log.w(TAG, "Unable to send message to node: " + ex.getMessage());
                    }
                }
            }
        }).start();
    }

    private MessageApi.SendMessageResult sendMessageToNode(final String path, final byte[] data, Node node) throws Exception {
        if (!googleApiClient.isConnected()) {
            throw new Exception("Google API client is not connected");
        }
        Log.v(TAG, "Sending message to: " + node.getDisplayName() + " at: " + path);
        return Wearable.MessageApi.sendMessage(googleApiClient, node.getId(), path, data).await();
    }

    public GoogleApiClient getGoogleApiClient() {
        return googleApiClient;
    }

    public void setGoogleApiClient(GoogleApiClient googleApiClient) {
        this.googleApiClient = googleApiClient;
    }
}
