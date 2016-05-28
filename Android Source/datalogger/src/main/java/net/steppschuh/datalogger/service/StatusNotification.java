package net.steppschuh.datalogger.service;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

class StatusNotification {

    private static final String TAG = StatusNotification.class.getSimpleName();

    private Service service;
    private Class targetActivityClass;
    private PendingIntent pendingIntent;

    private NotificationManager notificationManager;
    private NotificationCompat.Builder notificationBuilder;
    private Notification ongoingNotification;
    private int ongoingNotificationId = 1;

    public StatusNotification(Service service, Class targetActivityClass) {
        this.service = service;
        this.targetActivityClass = targetActivityClass;

        notificationManager = (NotificationManager) service.getSystemService(service.NOTIFICATION_SERVICE);
        pendingIntent = getDefaultPendingIntent();
    }

    private PendingIntent getDefaultPendingIntent() {
        Intent intent = new Intent(service, targetActivityClass);
        intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
        return PendingIntent.getActivity(service, 0, intent, 0);
    }

    private void setupOngoingNotification() {
        Log.d(TAG, "Setting up ongoing notification");

        notificationBuilder = new NotificationCompat.Builder(service)
                .setContentTitle("Trust Level: Unknown")
                .setContentText("Your current trust level can't be calculated yet")
                .setAutoCancel(true)
                .setOnlyAlertOnce(true)
                .setOngoing(true)
                .setSmallIcon(android.R.mipmap.sym_def_app_icon)
                .setPriority(Notification.PRIORITY_LOW)
                .setContentIntent(pendingIntent);

        ongoingNotification = notificationBuilder.build();
        service.startForeground(ongoingNotificationId, ongoingNotification);
    }

    private void show(final String title, final String text, final long timestamp) {
        if (notificationBuilder == null) {
            setupOngoingNotification();
        }

        final Service loggingService = service;
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                notificationBuilder
                        .setContentTitle(title)
                        .setContentText(text)
                        .setWhen(timestamp);
                ongoingNotification = notificationBuilder.build();
                loggingService.startForeground(ongoingNotificationId, ongoingNotification);
            }
        });
    }

    public void hide() {
        Log.d(TAG, "Hiding ongoing notification");
        notificationManager.cancel(ongoingNotificationId);
        notificationBuilder = null;
    }

}
