package com.rivkoch.discoverbybluetooth;

import static android.bluetooth.BluetoothAdapter.ACTION_DISCOVERY_FINISHED;
import static android.bluetooth.BluetoothAdapter.ACTION_DISCOVERY_STARTED;
import static android.bluetooth.BluetoothAdapter.ACTION_SCAN_MODE_CHANGED;
import static android.bluetooth.BluetoothAdapter.ACTION_STATE_CHANGED;

import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.view.animation.Animation;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.airbnb.lottie.LottieAnimationView;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    private RecyclerView recyclerView;
    private DeviceAdapter deviceAdapter;
    private AlertDialog alertDialog;
    private Device device;
    private BluetoothAdapter bluetoothAdapter;
    private IntentFilter intentFilter;
    private ImageView ble_img;
    private ImageButton imgbtn_settings;
    private Button btn_bluetoothScan, turnOff_btn, turnOn_btn;
    private LinearLayout ble_settings_LL, ble_onOff_LL;
    private LottieAnimationView scanningLottie;
    private TextView scanning_tv;
    private Boolean isLocationPermission;

    private static final int MANUALLY_LOCATION_PERMISSION_REQUEST_CODE = 124;
    private List<Device> bluetoothDevices;
    private List<String> listOfNames;
    private boolean isRegistred;

    //common callback for location and nearby
    ActivityResultCallback<Boolean> permissionCallBack = new ActivityResultCallback<Boolean>() {
        @Override
        public void onActivityResult(Boolean isGranted) {
            if (isLocationPermission == null) {
                requestPermissionWithRationaleCheck();
            } else {
                if (isGranted && isLocationPermission) {//location permission ok
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        requestNearby();
                    }
                } else {
                    openPermissionSettingDialog();
                }
            }
        }
    };
    ActivityResultLauncher<String> requestPermissionLauncher = registerForActivityResult(new ActivityResultContracts.RequestPermission(), permissionCallBack);



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        setData();
        findViews();
        setRecyclerView();
        setViewAdapter();

        setListeners();
    }

    private void setRecyclerView() {
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false);
        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(linearLayoutManager);
    }

    private void setViewAdapter() {
        deviceAdapter = new DeviceAdapter(this, bluetoothDevices);
        recyclerView.setAdapter(deviceAdapter);
    }

    private void setData() {
        bluetoothDevices = new ArrayList<>();
        device = new Device();
        listOfNames = new ArrayList<>();
    }

    private void setBluetoothAdapter() {
        BluetoothManager bluetoothManager = (BluetoothManager) getBaseContext().getSystemService(Context.BLUETOOTH_SERVICE);
        bluetoothAdapter = bluetoothManager.getAdapter();
    }

    private void setListeners() {

        turnOn_btn.setOnClickListener(v -> {
            if (bluetoothAdapter.isEnabled()) {
                Toast.makeText(this, "Already on", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "TURN on", Toast.LENGTH_SHORT).show();

//                Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
//                request = ENABLE;
//                someActivityResultLauncher.launch(intent);
//                requestEnableBluetooth();
            }
        });

        turnOff_btn.setOnClickListener(v -> {
            if (!bluetoothAdapter.isEnabled()) {
                Toast.makeText(this, "Already off", Toast.LENGTH_SHORT).show();
            } else {

                if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                    if (ContextCompat.checkSelfPermission(MainActivity.this, android.Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_DENIED) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                            ActivityCompat.requestPermissions(MainActivity.this, new String[]{android.Manifest.permission.BLUETOOTH_CONNECT}, 2);
                            return;
                        }
                    }
                }
                bluetoothAdapter.disable();
                    ble_img.setImageResource(R.drawable.va_bluetooth_disabled);
                    Toast.makeText(this, "Bluetooth turned off", Toast.LENGTH_SHORT).show();

                    return;



            }
        });

        imgbtn_settings.setOnClickListener(v->{
            if(ble_onOff_LL.getVisibility() == View.GONE){
                ble_onOff_LL.setVisibility(View.VISIBLE);
            }else if(ble_onOff_LL.getVisibility() == View.VISIBLE){
                ble_onOff_LL.setVisibility(View.GONE);
            }
        });

        btn_bluetoothScan.setOnClickListener(v -> {

            if (!bluetoothAdapter.isEnabled()) {
                Toast.makeText(this, "Bluetooth off, turn in on for scanning.", Toast.LENGTH_SHORT).show();
            }

            checkPermissions();

            if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {

                if (ContextCompat.checkSelfPermission(MainActivity.this, android.Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_DENIED) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        ActivityCompat.requestPermissions(MainActivity.this, new String[]{android.Manifest.permission.BLUETOOTH_SCAN}, 2);
                        return;
                    }
                }
            }
//            registerReceiver(bluetoothScanReceiver, intentFilter);
            if (!bluetoothAdapter.isDiscovering()) {
                bluetoothAdapter.startDiscovery();
            }
        });
    }

    private void checkPermissions() {

        boolean resultNearby = false;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            resultNearby = ContextCompat.checkSelfPermission(this, android.Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED;
        }
        boolean resultLocation = ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED;

        if (resultLocation) {
            requestLocation();
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (resultNearby) {
                requestNearby();
            }
        }
    }

    private void requestNearby() {
        isLocationPermission = false;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            requestPermissionLauncher.launch(android.Manifest.permission.BLUETOOTH_CONNECT);
        }
    }

    private void requestPermissionWithRationaleCheck() {
        if (ActivityCompat.shouldShowRequestPermissionRationale(MainActivity.this, android.Manifest.permission.BLUETOOTH_CONNECT)) {
            openPermissionSettingDialog();

        } else if (ActivityCompat.shouldShowRequestPermissionRationale(MainActivity.this, android.Manifest.permission.ACCESS_FINE_LOCATION)) {
            openPermissionSettingDialog();
//            requestNearby();
        }

    }

    private void requestLocation() {
        isLocationPermission = true;
        requestPermissionLauncher.launch(android.Manifest.permission.ACCESS_FINE_LOCATION);
    }

    private void openPermissionSettingDialog() {

        String message = "Location and Nearby permissions are important for app functionality. You will be transported to Setting screen because the permissions are permanently disable. Please manually allow them.";
        alertDialog =
                new AlertDialog.Builder(MainActivity.this)
                        .setMessage(message)
                        .setPositiveButton(getString(android.R.string.ok),
                                (dialog, which) -> {
                                    openSettingsManually();
                                    dialog.cancel();
                                }).show();
        alertDialog.setCanceledOnTouchOutside(true);

    }

    private final ActivityResultLauncher<Intent> manuallyPermissionResultLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK) {
                    //The broadcast is start working
                }
            });

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == MANUALLY_LOCATION_PERMISSION_REQUEST_CODE) {
            boolean result = false;
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                result = ContextCompat.checkSelfPermission(this, android.Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED;
            }
            if (result) {
                requestNearby();

            }
        }
    }

    private void openSettingsManually() {

        Intent intent = new Intent();
        intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        Uri uri = Uri.fromParts("package", getPackageName(), null);
        intent.setData(uri);
        manuallyPermissionResultLauncher.launch(intent);
    }

    private void findViews() {
        if(recyclerView==null)
            recyclerView= findViewById(R.id.activityMain_RV_recyclerView);
//        tv_devices = findViewById(R.id.tv_devices);
        btn_bluetoothScan = findViewById(R.id.btn_bluetoothScan);
        ble_img = findViewById(R.id.ble_img);
        ble_settings_LL = findViewById(R.id.ble_settings_LL);
        ble_onOff_LL = findViewById(R.id.ble_onOff_LL);
        ble_onOff_LL.setVisibility(View.GONE);
        imgbtn_settings = findViewById(R.id.imgbtn_settings);
        turnOn_btn = findViewById(R.id.turnOn_btn);
        turnOff_btn = findViewById(R.id.turnOff_btn);
        scanning_tv = findViewById(R.id.scanning_tv);
        scanning_tv.setVisibility(View.GONE);
        scanningLottie = findViewById(R.id.scanningLottie);
        scanningLottie.setVisibility(View.GONE);
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
            boolean isLoading;

            if (BluetoothAdapter.ACTION_DISCOVERY_STARTED.equals(action)) {
                // Discovery starts

                // Disable button and show animation
                btn_bluetoothScan.setVisibility(View.GONE);
                recyclerView.setVisibility(View.GONE);
                ble_settings_LL.setVisibility(View.GONE);
                scanning_tv.setVisibility(View.VISIBLE);
//                scanning_tv.animate().translationX(-2000).setDuration(10000).setStartDelay(2900);
                scanningLottie.setVisibility(View.VISIBLE);
                scanningLottie.setRepeatCount(Animation.INFINITE);

                // Clear the recycler view
                bluetoothDevices.clear();
                listOfNames.clear();
                deviceAdapter.clearList();

            } else if (ACTION_DISCOVERY_FINISHED.equals(action)) {
                // Enable button and finish animation
                btn_bluetoothScan.setVisibility(View.VISIBLE);
                recyclerView.setVisibility(View.VISIBLE);
                ble_settings_LL.setVisibility(View.VISIBLE);
                scanningLottie.setVisibility(View.GONE);
                scanning_tv.setVisibility(View.GONE);


                //discovery finishes, dismiss progress dialog
            } else if (BluetoothDevice.ACTION_FOUND.equals(action)) {

                //bluetooth device found
                int rssi = intent.getShortExtra(BluetoothDevice.EXTRA_RSSI, Short.MIN_VALUE);
                String name = intent.getStringExtra(BluetoothDevice.EXTRA_NAME);
                if (name != null) {

                    if(listOfNames.size() == 0 || !  listOfNames.contains(name)){
                        device.setName(name);
                        device.setDistance(calculateDistance(rssi));
                        // Add device to list
                        bluetoothDevices.add(device);
                        listOfNames.add(name);

                        // Put the device into recycler view that will show the devices
                        deviceAdapter.addToList(device);
                    }
                }
            }
        }
    };

    private double calculateDistance(int rssi) {
        int txPower = -59; // Hardcoded tx power value, can be obtained from the scanRecord
        return Math.pow(10d, ((double) txPower - rssi) / (10 * 2));
    }

    @Override
    public void onResume() {
        super.onResume();
        try {
            if(bluetoothAdapter==null) {
                setBluetoothAdapter();
                createIntentFilter();
                registerReceiver(bluetoothScanReceiver, intentFilter);
                isRegistred = true;

            }

        } catch (Exception e) {
            // already registered
            //   tv.setText("Receiver is already received");
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (bluetoothScanReceiver != null)
            if(isRegistred) {
                unregisterReceiver(bluetoothScanReceiver);
            }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (alertDialog != null && alertDialog.isShowing()) {
            alertDialog.dismiss();
        }
    }
}