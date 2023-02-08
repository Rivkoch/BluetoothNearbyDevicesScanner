package com.rivkoch.discoverbybluetooth;

import static android.bluetooth.BluetoothAdapter.ACTION_DISCOVERY_FINISHED;
import static android.bluetooth.BluetoothAdapter.ACTION_DISCOVERY_STARTED;
import static android.bluetooth.BluetoothAdapter.ACTION_SCAN_MODE_CHANGED;
import static android.bluetooth.BluetoothAdapter.ACTION_STATE_CHANGED;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity {

    private BluetoothAdapter bluetoothAdapter;
    private IntentFilter intentFilter;
    private TextView tv, tv_devices;
    private Button btn_bluetoothScan;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        findViews();

        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        createIntentFilter();
        if (ContextCompat.checkSelfPermission(MainActivity.this, android.Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_DENIED) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                ActivityCompat.requestPermissions(MainActivity.this, new String[]{android.Manifest.permission.BLUETOOTH_CONNECT}, 2);
                return;
            }
        }

        setListener();

    }

    private void setListener() {
        btn_bluetoothScan.setOnClickListener(v -> {
            if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {

                if (ContextCompat.checkSelfPermission(MainActivity.this, android.Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_DENIED) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        ActivityCompat.requestPermissions(MainActivity.this, new String[]{android.Manifest.permission.BLUETOOTH_SCAN}, 2);
                        return;
                    }
                }
            }
            bluetoothAdapter.startDiscovery();

            registerReceiver(bluetoothScanReceiver, intentFilter);
            tv.setText("receiver registered 1");
        });
    }

    private void findViews() {
        tv = findViewById(R.id.tv_broadcast);
        tv_devices = findViewById(R.id.tv_devices);
        btn_bluetoothScan = findViewById(R.id.btn_bluetoothScan);
    }

    private void createIntentFilter() {
        intentFilter = new IntentFilter();
        intentFilter.addAction(BluetoothDevice.ACTION_FOUND);
        intentFilter.addAction(ACTION_DISCOVERY_STARTED);
        intentFilter.addAction(ACTION_DISCOVERY_FINISHED);
        intentFilter.addAction(ACTION_STATE_CHANGED);
        intentFilter.addAction(ACTION_SCAN_MODE_CHANGED);
    }


    private final BroadcastReceiver bluetoothScanReceiver = new BroadcastReceiver() {
        @SuppressLint("MissingPermission")
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            if (BluetoothAdapter.ACTION_DISCOVERY_STARTED.equals(action)) {
                //discovery starts, we can show progress dialog or perform other tasks
            } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
                //discovery finishes, dismiss progress dialog
            } else if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                //bluetooth device found
                BluetoothDevice device = (BluetoothDevice) intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);

//                showToast("Found device " + device.getName());
                if(device.getName() != null) {
                    showDevices("Found device " + device.getName());
                }
            }
        }
    };

    private void showDevices(String deviceFounded) {
        tv_devices.append( "\n" + deviceFounded);
    }

    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onDestroy() {
        unregisterReceiver(bluetoothScanReceiver);
        tv.setText("unregistered -destroy");

        super.onDestroy();
    }

    @Override
    public void onResume(){
        super.onResume();
        try{
            registerReceiver(bluetoothScanReceiver,intentFilter);
            tv.setText("receiver registered 2");

        }catch (Exception e){
            // already registered
            tv.setText("Receiver is already received");
        }
    }

    protected void onPause() {
        unregisterReceiver(bluetoothScanReceiver);
        tv.setText("unregistered -pause");

        //called after unregistering
        super.onPause();
    }


}