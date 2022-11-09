package br.com.ludiaz.bluetooth.util;

import android.bluetooth.BluetoothDevice;

public class DeviceItem {

    private int mGroupPosition;
    private String mName;
    private String mAddress;
    private BluetoothDevice mBluetoothDevice;

    public DeviceItem(int groupPosition, String name, String address, BluetoothDevice bluetoothDevice){
        this.mGroupPosition = groupPosition;
        this.mName = name;
        this.mAddress = address;
        this.mBluetoothDevice = bluetoothDevice;
    }

    public int getGroupPosition(){
        return this.mGroupPosition;
    }

    public String getName(){
        return this.mName;
    }

    public String getAddress(){
        return this.mAddress;
    }

    public BluetoothDevice getBlueoothDevice(){
        return this.mBluetoothDevice;
    }

}
