package net.steppschuh.sensordatalogger.service;

import android.support.v4.app.NotificationManagerCompat;
import android.support.v7.app.NotificationCompat;
import android.util.Log;

import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.WearableListenerService;

import net.steppschuh.sensordatalogger.R;

public class ListenerService extends WearableListenerService {

    @Override
    public void onMessageReceived(MessageEvent messageEvent) {
        Log.i("test", "onMessageReceived()");
        if (messageEvent.getPath().equals("asdfklajsdhflkasjhdlfkajshdlkfjahslkdjhfaklhdf")) {
            final String message = new String(messageEvent.getData());
            NotificationCompat.Builder b = new NotificationCompat.Builder(this);
            b.setContentText(message);
            b.setSmallIcon(R.mipmap.ic_launcher);
            b.setContentTitle("Test Notification");
            b.setLocalOnly(true);
            NotificationManagerCompat man = NotificationManagerCompat.from(this);
            man.notify(0, b.build());
        } else {
            super.onMessageReceived(messageEvent);
        }
    }

}
