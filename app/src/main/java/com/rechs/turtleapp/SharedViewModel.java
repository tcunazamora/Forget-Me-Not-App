package com.rechs.turtleapp;

import android.bluetooth.BluetoothDevice;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public class SharedViewModel extends ViewModel {

    // Live Data that will keep track of sensor things
    private MutableLiveData<BluetoothDevice> feather = new MutableLiveData<>();

    /**
     * Sensor Things
     */

    public void setFeather(BluetoothDevice input) {feather.setValue(input);}

    public LiveData<BluetoothDevice> getFeather() {return feather;}
}

