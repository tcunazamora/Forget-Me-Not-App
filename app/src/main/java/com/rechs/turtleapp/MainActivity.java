package com.rechs.turtleapp;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.lifecycle.ViewModelProvider;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.le.ScanResult;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.rechs.turtleapp.ble.ConnectionManager;

import java.util.List;
import java.util.Locale;

import timber.log.Timber;

public class MainActivity extends AppCompatActivity {
    String macAddress = "CAE2A1911395";
    public final static int TEMPERATURE_THRESHOLD = 90;
    public static boolean IS_CONNECTED = true;

    private final Handler mainHandler = new Handler(); // Variable for handler, used to communicate from background thread to main thread to make UI changes
    private final int RESULT_SEAT_DEVICE_NAME = 101; // Return code for finding BLE device

    private volatile Context mContext;

    public BluetoothDevice seatDevice; // BLE reference for feather

    private boolean isSeatConfigured = false; // Boolean to check if BLE has been connected


    // Text views
    private TextView mTextViewTemperature;
    private TextView mTextViewActivationState;


    private List<BluetoothGattCharacteristic> seatCharList; // List that has all the BLE Characteristics references
    private int seatCharSize; // Size of characteristics list

    private Integer[] sensorArr = {0, 0}; // Array for sensor readings
    private Boolean[] isConnectedArr = {true};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if(Feather.getFeather() != null) {
            seatDevice = Feather.getFeather();
            ConnectionManager.INSTANCE.connect(seatDevice, this);
            isSeatConfigured = true;
        }


        /**
         * Initial setup things
         */
        // Set up Timber Logging
        if (BuildConfig.DEBUG) {
            Timber.plant(new Timber.DebugTree());
        }

        mContext = getApplicationContext(); // Set up context


        /**
         * Setup TextViews
         */
        mTextViewTemperature = findViewById(R.id.textview_temperature_text);
        mTextViewActivationState = findViewById(R.id.textview_activationstate_text);


        /** Background Thread to read BLE characteristics for new data **/
        new Thread(() -> {
            while (!isDestroyed()) {
                if (isSeatConfigured) {

                    sensorArr[0] = ConnectionManager.INSTANCE.readCharacteristic(seatDevice, seatCharList.get(seatCharSize - 1)); // Read temperature
                    sensorArr[1] = ConnectionManager.INSTANCE.readCharacteristic(seatDevice, seatCharList.get(seatCharSize - 2)); // Read activation state

                    if(sensorArr[0] > 500) sensorArr[0] = 500;

                    Timber.tag("IsConnected").e("%s", IS_CONNECTED);

                    Timber.tag("Main: Temperature").e("%d", sensorArr[0]);
                    Timber.tag("Activation State").e("%d", sensorArr[1]);

                    mainHandler.post(() -> {
                        mTextViewTemperature.setText(String.format(Locale.US,"%.1f°F", (sensorArr[0].floatValue() / 10)  * (9.0/5) + 32)); // Update Temperature

                        // Update ActivationState
                        if(sensorArr[1] == 0) { // 0 == OFF
                            mTextViewActivationState.setText(new StringBuilder("DEACTIVATED"));
                            mTextViewActivationState.setTextColor(Color.RED);
                        } else { // 1 == ON
                            mTextViewActivationState.setText(new StringBuilder("ACTIVATED"));
                            mTextViewActivationState.setTextColor(Color.GREEN);
                        }
                    });

                } else {
                    stopService();
                }

                SystemClock.sleep(500);
            }
        }).start();
    }


    public void startService(View v) {
        String input = "Test";

        Intent serviceIntent = new Intent(this, ExampleService.class);
        serviceIntent.putExtra("inputExtra", input);

        startService(serviceIntent);
    }

    public void startService() {
        String input = "Test";

        Intent serviceIntent = new Intent(this, ExampleService.class);
        serviceIntent.putExtra("inputExtra", input);

        startService(serviceIntent);
    }

    public void stopService(View v) {
        Intent serviceIntent = new Intent(this, ExampleService.class);
        stopService(serviceIntent);

        Intent temperatureIntent = new Intent(this, TemperatureService.class);
        stopService(temperatureIntent);

        Intent seatIntent = new Intent(this, SeatActivationService.class);
        stopService(seatIntent);
    }

    public void stopService() {
        Intent serviceIntent = new Intent(this, ExampleService.class);
        stopService(serviceIntent);

        Intent temperatureIntent = new Intent(this, TemperatureService.class);
        stopService(temperatureIntent);

        Intent seatIntent = new Intent(this, SeatActivationService.class);
        stopService(seatIntent);
    }


    // Create menu bar (3 dots, top right)
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_seat_selection, menu); // Inflate menu layout
        return true;
    }

    /** Actions for when a menu bar option is chosen
     * @param item item/option on menu, use getItemId()
     * @return returns true
     */
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()){ // Switch on item id
            case R.id.config_seat: // If "Configure Robot" option
                Intent robotIntent = new Intent(MainActivity.this, ScanActivity.class);
                    startActivityForResult(robotIntent, RESULT_SEAT_DEVICE_NAME); // Start ScanActivity to pair w/BLE Device and return device object (feather in robot)
                break;
        }
        return super.onOptionsItemSelected(item);
    }


    /** Handle results from startActivityForResult (from: onOptionsItemSelected, )
     * @param requestCode request code returned
     * @param resultCode result code returned (RESULT_OK, RESULT_CANCELLED, RESULT_FIRST_USER)
     * @param data Copy of previous Activity's Intent, usually use getExtra() to get data
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if (resultCode == RESULT_OK) { // If result code was OK
            switch (requestCode) { // Switch on request code
                case RESULT_SEAT_DEVICE_NAME: // If robot device request code
                    try {
                        Timber.tag("ScanActivity Result").d("Trying to get Device");
                        if (data != null) {
                            seatDevice = data.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE); // Assign robot BLE Device
                            Feather.setFeather(seatDevice); // Update feather for Feather class
                            seatCharList = TurtleListeners.Companion.getCharacteristicsList(seatDevice); // Set up char list
                            seatCharSize = seatCharList.size(); // Set up length of char list
                            ConnectionManager.INSTANCE.registerListener(TurtleListeners.Companion.seatListener(mContext, seatDevice, sensorArr, isConnectedArr)); // Register BLE Listener
                            isSeatConfigured = true;
                            startService();


                            Timber.tag("RSSI").e("%s", BluetoothDevice.EXTRA_RSSI);

                            Toast.makeText(this, "Seat Configured", Toast.LENGTH_SHORT).show(); // Alert user if pairing was successful
                        }
                    } catch (IllegalStateException e) {
                        Timber.tag("ScanActivity Result").e("getExtra was null");
                        Toast.makeText(this, "Pairing was not successful", Toast.LENGTH_SHORT).show(); // Alert user if paring wasn't successful
                    }
                    break;
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }
}