package com.rechs.turtleapp;

import static com.rechs.turtleapp.App.CHANNEL_ID;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.os.SystemClock;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

public class SeatActivationService extends Service {
    private boolean isDestroyed = false;
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        boolean isConnected = intent.getBooleanExtra("isConnected", true);

        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this,
                0, notificationIntent, PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setOngoing(true)
                .setSmallIcon(R.drawable.ic_android)
                .setContentTitle("CHILD IN CAR")
                .setContentText("Your child is inside the car")
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_SERVICE)
                .setContentIntent(pendingIntent);

        Notification seatActivationAlertNotification = notificationBuilder.build();

        startForeground(3, seatActivationAlertNotification);

        if(!isConnected) {
            new Thread(() -> {
                while(!isDestroyed) {
                    startForeground(3, seatActivationAlertNotification);

                    SystemClock.sleep(500);
                }
            }).start();
        }


        return START_REDELIVER_INTENT;
    }

    @Override
    public void onDestroy() {
        isDestroyed = true;
        stopForeground(true);
        stopSelf();
        super.onDestroy();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
