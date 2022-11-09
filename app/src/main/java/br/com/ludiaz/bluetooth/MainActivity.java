package br.com.ludiaz.bluetooth;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.ContextMenu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ExpandableListView;
import android.widget.Toast;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

import br.com.ludiaz.bluetooth.adapter.ListExpandable;
import br.com.ludiaz.bluetooth.util.DeviceItem;

public class MainActivity extends AppCompatActivity {

    private BluetoothManager bluetoothManager;
    private BluetoothAdapter bluetoothAdapter;

    private ExpandableListView listDevices;
    private List<DeviceItem> paireds = new ArrayList<>();
    private List<DeviceItem> find = new ArrayList<>();

    private DeviceItem sDeviceItem = null;

    private int lastSelection = -1;

    private void showSettings() {
        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        Uri uri = Uri.fromParts("package", getPackageName(), null);
        intent.setData(uri);
        startActivityForResult(intent, 0);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (grantResults.length > 0) {
            if (grantResults[0] == PackageManager.PERMISSION_DENIED) {
                alertNeddPermissions();
            }
        } else {
            alertNeddPermissions();
        }
    }

    private void alertNeddPermissions() {
        try {
            AlertDialog.Builder builder = getAlert();
            builder.setMessage(getString(R.string.message_need_permissions));
            builder.setPositiveButton(R.string.label_yes, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    showSettings();
                }
            });
            builder.setNegativeButton(R.string.label_no, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    Toast.makeText(getApplicationContext(), R.string.label_cancel, Toast.LENGTH_SHORT).show();
                }
            });
            builder.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private AlertDialog.Builder getAlert() {
        AlertDialog.Builder builder;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            builder = new AlertDialog.Builder(MainActivity.this, android.R.style.Theme_Material_Dialog_Alert);
        } else {
            builder = new AlertDialog.Builder(MainActivity.this);
        }

        builder.setTitle(getString(R.string.app_name));

        return builder;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        /*
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH) == PackageManager.PERMISSION_DENIED) {
            requestPermissionLauncher.launch(Manifest.permission.BLUETOOTH);
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_ADMIN) == PackageManager.PERMISSION_DENIED) {
            requestPermissionLauncher.launch(Manifest.permission.BLUETOOTH_ADMIN);
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_DENIED) {
            requestPermissionLauncher.launch(Manifest.permission.BLUETOOTH_CONNECT);
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_DENIED) {
            requestPermissionLauncher.launch(Manifest.permission.BLUETOOTH_SCAN);
        }
        */

        // Use this check to determine whether Bluetooth classic is supported on the device.
        // Then you can selectively disable BLE-related features.
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH)) {
            Toast.makeText(this, R.string.bluetooth_not_supported, Toast.LENGTH_SHORT).show();
            finish();
        }

        /*
        // Use this check to determine whether BLE is supported on the device. Then
        // you can selectively disable BLE-related features.
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(this, R.string.bluetoothle_not_supported, Toast.LENGTH_SHORT).show();
            finish();
        }
         */

        bluetoothManager = getSystemService(BluetoothManager.class);
        bluetoothAdapter = bluetoothManager.getAdapter();
        if (bluetoothAdapter == null) {
            Toast.makeText(getApplicationContext(), R.string.label_doesnt_support_bt, Toast.LENGTH_LONG).show();
        } else if (!bluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            bluetoothResultLauncher.launch(enableBtIntent);
        }

        setContentView(R.layout.activity_main);
        listDevices = findViewById(R.id.listDevices);
        listDevices.setOnChildClickListener(listDevicesOnChildClickListener);
        registerForContextMenu(listDevices);

        List<String> listGroups = new ArrayList<>();
        listGroups.add(getString(R.string.label_paired));
        listGroups.add(getString(R.string.label_find));

        HashMap<String, List<DeviceItem>> devicesGroups = new HashMap<>();
        devicesGroups.put(listGroups.get(0), this.paireds);
        devicesGroups.put(listGroups.get(1), this.find);

        // cria um adaptador (BaseExpandableListAdapter) com os dados acima
        ListExpandable listExpandable = new ListExpandable(this, listGroups, devicesGroups);
        // define o adapter do ExpandableListView
        listDevices.setAdapter(listExpandable);

        pairedDevices();

        discoverDevices();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        // Don't forget to unregister the ACTION_FOUND receiver.
        unregisterReceiver(receiverDiscoverDevices);
    }

    private final ActivityResultLauncher<Intent> bluetoothResultLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            new ActivityResultCallback<ActivityResult>() {
                @Override
                public void onActivityResult(ActivityResult result) {
                    if (result.getResultCode() == Activity.RESULT_OK) {
                        Intent data = result.getData();
                    }
                }
            }
    );

    private final ActivityResultLauncher<String> requestPermissionLauncher = registerForActivityResult(
      new ActivityResultContracts.RequestPermission(),
            isGranted -> {
                if(isGranted){
                    pairedDevices();
                }else{
                    Toast.makeText(getApplicationContext(), R.string.label_doesnt_access_bt, Toast.LENGTH_LONG).show();
                }
            }
    );

    @SuppressLint("MissingPermission")
    protected void pairedDevices() {
        Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();

        Log.d("PairedDevices", "here");

        if (pairedDevices.size() > 0) {
            Log.d("PairedDevices", "> 0");
            // There are paired devices. Get the name and address of each paired device.
            for (BluetoothDevice device : pairedDevices) {
                String deviceName = device.getName();
                String deviceHardwareAddress = device.getAddress(); // MAC address

                if(deviceName != null){
                    if(!deviceName.equals("")){
                        DeviceItem deviceItem = new DeviceItem(1, deviceName, deviceHardwareAddress, device);

                        paireds.add(deviceItem);
                        setListPaireds(deviceItem);
                    }
                }
            }
        }else{
            Log.d("PairedDevices", "not exists");
        }
    }

    private void setListPaireds(DeviceItem deviceItem){
        try {
            br.com.ludiaz.bluetooth.adapter.ListExpandable adapter = (ListExpandable) listDevices.getExpandableListAdapter();
            adapter.setNewItem(0, deviceItem);
            listDevices.setFastScrollEnabled(true);

            adapter.notifyDataSetChanged();
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    @SuppressLint("MissingPermission")
    private void discoverDevices(){
        Log.d("DiscoverDevices", "IntentFilter");
        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        registerReceiver(receiverDiscoverDevices, filter);
        Log.d("DiscoverDevices", "discoverDevices");

        bluetoothAdapter.startDiscovery();
    }

    @SuppressLint("MissingPermission")
    private final BroadcastReceiver receiverDiscoverDevices = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            Log.d("DiscoverDevices", action);
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                // Discovery has found a device. Get the BluetoothDevice
                // object and its info from the Intent.
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);

                String deviceName = device.getName();
                String deviceHardwareAddress = device.getAddress(); // MAC address

                if(deviceName != null) {
                    if (!deviceName.equals("")) {

                        DeviceItem deviceItem = new DeviceItem(1, deviceName, deviceHardwareAddress, device);

                        setListDiscover(deviceItem);
                        find.add(deviceItem);
                    }
                }
            }
        }
    };


    private void setListDiscover(DeviceItem deviceItem){
        Log.d("DiscoverDevices", "setListDiscover");
        try {
            br.com.ludiaz.bluetooth.adapter.ListExpandable adapter = (ListExpandable) listDevices.getExpandableListAdapter();

            adapter.setNewItem(1, deviceItem);

            adapter.notifyDataSetChanged();

            Log.d("DiscoverDevices-setListDiscover", "ok");
        }catch (Exception e){
            Log.d("DiscoverDevices-setListDiscover", "error");
            e.printStackTrace();
        }

    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo){
        super.onCreateContextMenu(menu, v, menuInfo);

        ExpandableListView expandableListView = (ExpandableListView) v;
         /*
        ExpandableListView.ExpandableListContextMenuInfo elcmi = (ExpandableListView.ExpandableListContextMenuInfo) menuInfo;

        DeviceItem deviceItem = (DeviceItem) expandableListView.getSelectedItem();

        sDeviceItem = deviceItem;
        */
        Log.d("onCreateContextMenu", sDeviceItem.getName() + " ("+sDeviceItem.getAddress()+")");

        expandableListView.clearChoices();
        expandableListView.clearFocus();

        menu.setHeaderTitle(sDeviceItem.getName() + "("+sDeviceItem.getAddress()+")");

        if(sDeviceItem.getGroupPosition() == 0) {
            menu.add(0, 0, 0, getString(R.string.label_unpair));
        }else if(sDeviceItem.getGroupPosition() == 1){
            menu.add(0, 0, 0, getString(R.string.label_pair));
        }
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        super.onContextItemSelected(item);

        if(item.getTitle().equals(getString(R.string.label_unpair))){
            unpairDevice(sDeviceItem.getBlueoothDevice());
        }else if(item.getTitle().equals(getString(R.string.label_pair))){
            pairDevice(sDeviceItem.getBlueoothDevice());
        }else{
            return false;
        }
        return true;
    }

    private final ExpandableListView.OnChildClickListener listDevicesOnChildClickListener = new ExpandableListView.OnChildClickListener() {
        @Override
        public boolean onChildClick(ExpandableListView parent, View view, int groupPosition, int childPosition, long id) {

            //Toast.makeText(getApplicationContext(), R.string.label_child_click, Toast.LENGTH_LONG).show();

            int index = parent.getFlatListPosition(ExpandableListView.getPackedPositionForChild(groupPosition, childPosition));

            parent.setItemChecked(index, true);

            Log.d("listDevicesOnChildClickListener", "groupPosition: "+groupPosition);
            Log.d("listDevicesOnChildClickListener", "childPosition: "+childPosition);

            //Log.d("listDevicesOnChildClickListener", "index: "+index);

            sDeviceItem = (DeviceItem) parent.getExpandableListAdapter().getChild(groupPosition, childPosition);

            if(sDeviceItem == null){
                Log.d("listDevicesOnChildClickListener", "sDeviceItem is null");
            }else if(groupPosition == 0){
                unpairDevice(sDeviceItem.getBlueoothDevice());
            }else if(groupPosition == 1){
                pairDevice(sDeviceItem.getBlueoothDevice());
            }

            return true;
        }
    };

    private void pairDevice(BluetoothDevice device){
        try {
            Log.d("pairDevice()", "Start Pairing...");
            Method m = device.getClass().getMethod("createBond", (Class[]) null);
            m.invoke(device, (Object[]) null);
            Log.d("pairDevice()", "Pairing finished.");
        } catch (Exception e) {
            Log.e("pairDevice()", e.getMessage());
        }
    }

    private void unpairDevice(BluetoothDevice device) {
        try {
            Log.d("unpairDevice()", "Start Un-Pairing...");
            Method m = device.getClass().getMethod("removeBond", (Class[]) null);
            m.invoke(device, (Object[]) null);
            Log.d("unpairDevice()", "Un-Pairing finished.");
        } catch (Exception e) {
            Log.e("unpairDevice", e.getMessage());
        }
    }
}