package com.rechs.turtleapp;

import android.bluetooth.BluetoothDevice;

public class Feather {
    private static BluetoothDevice feather = null;

    public static void setFeather(BluetoothDevice input) {feather = input;}

    public static BluetoothDevice getFeather() {return feather;}
}
