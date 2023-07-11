package com.rechs.turtleapp;

import static com.rechs.turtleapp.App.CHANNEL_ID;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;
import android.os.SystemClock;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.core.app.NotificationCompat;

public class TemperatureService extends Service {
    boolean isDestroyed = false;

    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Intent fullScreenIntent = new Intent(this, MainActivity.class);
        PendingIntent fullScreenPendingIntent = PendingIntent.getActivity(this,
                0,
                fullScreenIntent,
                PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setOngoing(true)
                .setSmallIcon(R.drawable.ic_android)
                .setContentTitle("EXTREME TEMPERATURE")
                .setContentText("Your child is in the car with extreme heat")
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_SERVICE)
                .setFullScreenIntent(fullScreenPendingIntent, true);

        Notification temperatureAlertNotification = notificationBuilder.build();

        startForeground(2, temperatureAlertNotification);

        new Thread(() -> {
            while(!isDestroyed) {
                startForeground(2, temperatureAlertNotification);

                SystemClock.sleep(1000);
            }
        }).start();

        return START_REDELIVER_INTENT; // Restarts notification if system closes it
    }

    @Override
    public void onDestroy() {
        stopForeground(true);
        stopSelf();
        isDestroyed = true;
        super.onDestroy();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
