package com.rechs.turtleapp;

import static com.rechs.turtleapp.App.CHANNEL_ID;
import static com.rechs.turtleapp.MainActivity.IS_CONNECTED;
import static com.rechs.turtleapp.MainActivity.TEMPERATURE_THRESHOLD;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGattCharacteristic;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.os.SystemClock;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.core.app.NotificationCompat;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModelProvider;
import androidx.lifecycle.ViewModelStoreOwner;

import com.rechs.turtleapp.ble.ConnectionManager;

import org.jetbrains.anko.commons.BuildConfig;

import java.util.List;
import java.util.Locale;

import timber.log.Timber;

public class ExampleService extends Service {
    boolean isDestroyed = false;

    @Override
    public void onCreate() {
        if (BuildConfig.DEBUG) {
            Timber.plant(new Timber.DebugTree());
        }

        super.onCreate();
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        /*
        if(Feather.getFeather() == null) { // Kill Service if feather isn't connected
            stopForeground(true);
            stopSelf();
            return START_NOT_STICKY;
        }*/

        BluetoothDevice seatDevice = Feather.getFeather();
        List<BluetoothGattCharacteristic> seatCharList = TurtleListeners.Companion.getCharacteristicsList(seatDevice);
        int seatCharSize = seatCharList.size();

        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this,
                0, notificationIntent, PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setOngoing(true)
                .setContentTitle("Temperature Reading")
                .setContentText(String.valueOf(0))
                .setSmallIcon(R.drawable.ic_android)
                .setOnlyAlertOnce(true)
                .setContentIntent(pendingIntent);

        Notification notification = builder.build();

        startForeground(1, notification);

        NotificationManager notificationManager = getSystemService(NotificationManager.class);

        SystemClock.sleep(2000);
        new Thread(() -> {
            boolean isTempAlertOn = false;
            boolean isSeatAlertOn = false;
            boolean isConstantSeatAlertOn = false;
            while(!isDestroyed) {
                double temperature = ConnectionManager.INSTANCE.readCharacteristic(seatDevice, seatCharList.get(seatCharSize - 1)); // Read temp
                temperature = (temperature / 10.0) * (9.0 / 5) + 32; // Convert to F

                int seatActivationState = ConnectionManager.INSTANCE.readCharacteristic(seatDevice, seatCharList.get(seatCharSize - 2)); // Read seat state

                Timber.tag("Temperature").d("%.1f", temperature);
                Timber.tag("Seat State").d("%s", seatActivationState);

                builder.setContentText(String.format(Locale.US, "%.1f", temperature)); // Update notification for temp

                notificationManager.notify(1, builder.build());


                /**
                 * Service Handling
                 */

                if(seatActivationState == 1 && temperature >= TEMPERATURE_THRESHOLD && !isTempAlertOn) { // Start temp service
                    showTemperatureAlert(); // If temp threshold is reached, show alert
                    isTempAlertOn = true;
                } else if(isTempAlertOn && (seatActivationState == 0 || temperature < TEMPERATURE_THRESHOLD)) { // Stop service
                    Intent tempService = new Intent(this, TemperatureService.class);
                    stopService(tempService);

                    isTempAlertOn = false;
                }


                if(seatActivationState == 1 && isSeatAlertOn && !isConstantSeatAlertOn && !IS_CONNECTED) {
                    Intent seatService = new Intent(this, SeatActivationService.class); // Stop service
                    stopService(seatService);

                    showSeatActivationAlert(false); // start new service with constant notifications

                    isConstantSeatAlertOn = true;
                }
                else if(seatActivationState == 1 && !isSeatAlertOn) {
                    showSeatActivationAlert(IS_CONNECTED); // If seat is activated, show alert
                    isSeatAlertOn = true;
                }
                else if (seatActivationState == 0 && isSeatAlertOn){ // Else stop notification
                    Intent seatService = new Intent(this, SeatActivationService.class);
                    stopService(seatService);

                    isSeatAlertOn = false;
                    isConstantSeatAlertOn = false;
                }

                SystemClock.sleep(500);
            }
        }).start();

        return START_REDELIVER_INTENT; // Restart service when killed
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

    private void showTemperatureAlert() {
        Intent serviceIntent = new Intent(this, TemperatureService.class);
        startService(serviceIntent); // Start temp notification
    }

    private void showSeatActivationAlert(boolean isConnected) {
        Intent serviceIntent = new Intent(this, SeatActivationService.class);

        serviceIntent.putExtra("isConnected", isConnected); // Send boolean
        startService(serviceIntent);
    }

}
