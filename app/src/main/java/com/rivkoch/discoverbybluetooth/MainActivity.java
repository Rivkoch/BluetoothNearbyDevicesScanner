package com.rivkoch.discoverbybluetooth;

import static android.bluetooth.BluetoothAdapter.ACTION_DISCOVERY_FINISHED;
import static android.bluetooth.BluetoothAdapter.ACTION_DISCOVERY_STARTED;
import static android.bluetooth.BluetoothAdapter.ACTION_SCAN_MODE_CHANGED;
import static android.bluetooth.BluetoothAdapter.ACTION_STATE_CHANGED;

import androidx.activity.result.ActivityResult;
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
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    private RecyclerView recyclerView;
    private DeviceAdapter deviceAdapter;
    private AlertDialog alertDialog;
    private Device device;
    private BluetoothAdapter bluetoothAdapter;
    private IntentFilter intentFilter;
    private ImageView main_img_bluetoothStatus;
    private ImageButton main_imgbtn_settings;
    private Button main_btn_bluetoothScan;
    private Switch main_switch_onOffBluetooth;
    private LinearLayout main_LL_settingsLayout, main_LL_onOffLayout;
    private TextView main_tv_scanning, main_tv_dots;
    private Boolean isLocationPermission;

    private static final int MANUALLY_LOCATION_PERMISSION_REQUEST_CODE = 124;
    private List<Device> bluetoothDevices;
    private List<String> listOfNames;
    private boolean isRegistred, afterDiscover;

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
        if (bluetoothAdapter == null) {
            setBluetoothAdapter();
            getBleData();
        }
        createIntentFilter();
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

        main_switch_onOffBluetooth.setOnClickListener(v -> {
            if (main_switch_onOffBluetooth.isChecked()) {

                if (bluetoothAdapter.isEnabled()) {
                    Toast.makeText(this, "Already on", Toast.LENGTH_SHORT).show();
                } else {
                    Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                    someActivityResultLauncher.launch(intent);
                }
            }

            else {
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
                    main_img_bluetoothStatus.setImageResource(R.drawable.va_bluetooth_disabled);
                    Toast.makeText(this, "Bluetooth turned off", Toast.LENGTH_SHORT).show();
                }
            }
        });

        main_imgbtn_settings.setOnClickListener(v -> {
            if (main_LL_onOffLayout.getVisibility() == View.GONE) {
                main_LL_onOffLayout.setVisibility(View.VISIBLE);
            } else if (main_LL_onOffLayout.getVisibility() == View.VISIBLE) {
                main_LL_onOffLayout.setVisibility(View.GONE);
            }
        });

        main_btn_bluetoothScan.setOnClickListener(v -> {
            if (!bluetoothAdapter.isEnabled()) {
                Toast.makeText(this, "Bluetooth off, turn it on for scanning.", Toast.LENGTH_LONG).show();
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

            if(!isRegistred) {
                registerReceiver(bluetoothScanReceiver, intentFilter);
                isRegistred = true;
            }

            if (!bluetoothAdapter.isDiscovering()) {
                bluetoothAdapter.startDiscovery();
                afterDiscover = false;
            }

            if(bluetoothDevices.isEmpty() && afterDiscover){
                main_tv_scanning.setTextSize(14);
                main_tv_scanning.setVisibility(View.VISIBLE);
                main_tv_scanning.setText("No devices were found.");
            }
        });
    }

    ActivityResultLauncher<Intent> someActivityResultLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            new ActivityResultCallback<ActivityResult>() {
                @Override
                public void onActivityResult(ActivityResult result) {
                    if (result.getResultCode() == Activity.RESULT_OK) {
                        // There are no request codes
                        main_img_bluetoothStatus.setImageResource(R.drawable.va_bluetooth_enabled);
                        Toast.makeText(MainActivity.this, "Bluetooth turned on", Toast.LENGTH_LONG).show();
                    } else {
                        Toast.makeText(MainActivity.this, "The user decided to deny bluetooth access", Toast.LENGTH_LONG).show();
                    }
                }
            });

    private void checkPermissions() {
        boolean resultNearby = false;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            resultNearby = ContextCompat.checkSelfPermission(this, android.Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED;
        }
        boolean resultLocation = ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED;

        if (resultLocation) {
            requestLocation();
            if (bluetoothAdapter != null) {
                getBleData();
            }
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (resultNearby) {
                requestNearby();
            }
        }
    }

    private void getBleData() {
            if(bluetoothAdapter.isEnabled()){
                main_img_bluetoothStatus.setImageResource(R.drawable.va_bluetooth_enabled);
                main_switch_onOffBluetooth.setChecked(true);
            }else {
                main_img_bluetoothStatus.setImageResource(R.drawable.va_bluetooth_disabled);
                main_switch_onOffBluetooth.setChecked(false);
        }
    }

    private void requestNearby() {
        isLocationPermission = false;
        requestPermissionLauncher.launch(android.Manifest.permission.BLUETOOTH_CONNECT);
    }

    private void requestPermissionWithRationaleCheck() {
        if (ActivityCompat.shouldShowRequestPermissionRationale(MainActivity.this, android.Manifest.permission.BLUETOOTH_CONNECT)) {
            openPermissionSettingDialog();
        } else if (ActivityCompat.shouldShowRequestPermissionRationale(MainActivity.this, android.Manifest.permission.ACCESS_FINE_LOCATION)) {
            openPermissionSettingDialog();
        }
    }

    private void requestLocation() {
        isLocationPermission = true;
        requestPermissionLauncher.launch(android.Manifest.permission.ACCESS_FINE_LOCATION);
    }

    private void openPermissionSettingDialog() {
        String message="";
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            message = "Location and Nearby permissions are important for app functionality. You will be transported to Setting screen because the permissions are permanently disable. Please manually allow them.";
        }else {
            message = "DiscoverByBluetooth permission are important for app functionality. You will be transported to Setting screen because access to the device's location is permanently disable. Please manually allow it.";
        }
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
            result = ContextCompat.checkSelfPermission(this, android.Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED;
            if (result && android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
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
        if (recyclerView == null)
            recyclerView = findViewById(R.id.activityMain_RV_recyclerView);
        main_btn_bluetoothScan = findViewById(R.id.main_btn_bluetoothScan);
        main_img_bluetoothStatus = findViewById(R.id.main_img_bluetoothStatus);
        main_LL_settingsLayout = findViewById(R.id.main_LL_settingsLayout);
        main_switch_onOffBluetooth = findViewById(R.id.main_switch_onOffBluetooth);

        main_LL_onOffLayout = findViewById(R.id.main_LL_onOffLayout);
        main_imgbtn_settings = findViewById(R.id.main_imgbtn_settings);
        main_tv_scanning = findViewById(R.id.main_tv_scanning);
        main_tv_dots = findViewById(R.id.main_tv_dots);

        // set visibility
        main_LL_onOffLayout.setVisibility(View.GONE);
        main_tv_scanning.setVisibility(View.GONE);
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
            afterDiscover  = true;

            /* Discovery starts */
            if (BluetoothAdapter.ACTION_DISCOVERY_STARTED.equals(action)) {

                // Disable button and view while showing "animation" only
                main_btn_bluetoothScan.setVisibility(View.GONE);
                main_LL_settingsLayout.setVisibility(View.GONE);
                main_tv_scanning.setVisibility(View.VISIBLE);
                main_tv_scanning.setTextSize(24);
                main_tv_scanning.setText("S c a n n i n g\n");
                main_tv_dots.setVisibility(View.VISIBLE);

                // Clear recycler view data
                bluetoothDevices.clear();
                listOfNames.clear();
                deviceAdapter.clearList();

                /* Discovery finishes */
            } else if (ACTION_DISCOVERY_FINISHED.equals(action)) {

                // Enable view and finish animation
                main_LL_settingsLayout.setVisibility(View.VISIBLE);
                main_tv_scanning.setVisibility(View.GONE);
                main_btn_bluetoothScan.setVisibility(View.VISIBLE);
                main_tv_dots.setText("");
                main_tv_dots.setVisibility(View.GONE);

                /* Bluetooth device found */
            } else if (BluetoothDevice.ACTION_FOUND.equals(action)) {

                int rssi = intent.getShortExtra(BluetoothDevice.EXTRA_RSSI, Short.MIN_VALUE);
                String name = intent.getStringExtra(BluetoothDevice.EXTRA_NAME);
                main_tv_dots.append(" . ");

                if (name != null) {

                    if (listOfNames.size() == 0 || !listOfNames.contains(name)) {

                        device.setName(name);
                        device.setDistance(calculateDistance(rssi));

                        // Add device to list
                        bluetoothDevices.add(device);
                        listOfNames.add(name);

                        // Put the device into recycler view that will show the devices
                        deviceAdapter.addToList(device);
                    }
                    else{
                        // The "scanning animation"
                        main_tv_dots.append(" . ");
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
            if (bluetoothAdapter == null) {
                setBluetoothAdapter();
                getBleData();
                createIntentFilter();
                if(!isRegistred) {
                    registerReceiver(bluetoothScanReceiver, intentFilter);
                    isRegistred = true;
                }
            }
        } catch (Exception e) {
            // already registered
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (bluetoothScanReceiver != null)
            if (isRegistred) {
                unregisterReceiver(bluetoothScanReceiver);
                isRegistred = false;
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