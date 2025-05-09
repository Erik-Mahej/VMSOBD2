package com.example.vmsobd2;

import android.Manifest;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.util.Set;
    //zde logika na vybirani bluetooth zarizeni a ukladani do preferences byla udelana AI
    //prompt:okay now i want to make in the settings activity to make a button that allows the user to choose the bluetooth device to which the app will be connecting
    //pote jsem to poupravil at to zobrazuje device
public class AppSettings extends AppCompatActivity {
    private static final String TAG = "AppSettings";
    private static final int REQUEST_BLUETOOTH_CONNECT = 1;
    private BluetoothAdapter bluetoothAdapter;
    private Button chooseDeviceButton;
    private Bluetooth bluetooth;
    private TextView chosenDeviceTextView;
    private TextView connectionStatus;
    private Button connectButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_appsettings);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        //BLUETOOTH 1
        connectionStatus = findViewById(R.id.connection_status);
        connectButton = findViewById(R.id.btnConnect);

        TextView connectionStatus = findViewById(R.id.connection_status);
        Button connectButton = findViewById(R.id.btnConnect);

        bluetooth = Bluetooth.getInstance(getApplicationContext());

        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        String deviceAddress = preferences.getString("selected_device_address", "");
        bluetooth.handleConnectButton(connectButton, connectionStatus,deviceAddress);

        if (bluetooth.isConnected()) {
            bluetooth.updateStatusView('c', connectButton, connectionStatus);
        }

        //BLUETOOTH 2

        chooseDeviceButton = findViewById(R.id.chooseDeviceButton);
        chosenDeviceTextView = findViewById(R.id.chosenDeviceTextView);

        if (chooseDeviceButton == null) {
            Log.e(TAG, "chooseDeviceButton is null");
            return;
        }
        //ziskani adapteru a zjisteni jestli je bluetooth podporovano
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter == null) {
            Log.e(TAG, "Bluetooth is not supported on this device");
            return;
        }
        //on click listener ktery uzivateli da pak menu ve kterem si vybere zarizeni
        chooseDeviceButton.setOnClickListener(v -> {
            Log.d(TAG, "chooseDeviceButton clicked");
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.BLUETOOTH_CONNECT}, REQUEST_BLUETOOTH_CONNECT);
                } else {
                    showDeviceChooserDialog();
                }
            } else {
                showDeviceChooserDialog();
            }
        });

        loadChosenDevice();
    }
        //vysvetleno v MainActivity
        @Override
        public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
            bluetooth.handlePermissionsResult(requestCode, permissions, grantResults);
        }
        //vysletleno v MainActivity
        public void goBack(View view) {
            if (view.getId() == R.id.btnBack) {
                Intent intent = new Intent(AppSettings.this, MainActivity.class);
                startActivity(intent);
            }
            finish();
        }

    private void showDeviceChooserDialog() {
        Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();
        if (pairedDevices.isEmpty()) {
            Log.e(TAG, "No paired Bluetooth devices found");
            return;
        }

        String[] deviceNames = new String[pairedDevices.size()];
        String[] deviceAddresses = new String[pairedDevices.size()];
        int index = 0;

        for (BluetoothDevice device : pairedDevices) {
            deviceNames[index] = device.getName();
            deviceAddresses[index] = device.getAddress();
            index++;
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(getString(R.string.ch_bt_device));
        builder.setItems(deviceNames, (dialog, which) -> {
            String selectedDeviceName = deviceNames[which];
            String selectedDeviceAddress = deviceAddresses[which];
            Log.d(TAG, getString(R.string.seleted_device)+ ": " + selectedDeviceName + " (" + selectedDeviceAddress + ")");
            saveSelectedDevice(selectedDeviceName, selectedDeviceAddress);
        });
        builder.show();
    }

    private void saveSelectedDevice(String deviceName, String deviceAddress) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putString("selected_device_name", deviceName);
        editor.putString("selected_device_address", deviceAddress);
        editor.apply();
        Log.d(TAG, getString(R.string.device_saved) + deviceName + " (" + deviceAddress + ")");
        loadChosenDevice();
        bluetooth.handleConnectButton(connectButton, connectionStatus,deviceAddress);
    }

    private void loadChosenDevice() {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        String deviceName = preferences.getString("selected_device_name", "No device chosen");
        chosenDeviceTextView.setText( getString(R.string.chosen_device)+ ": " + deviceName);
    }

        @Override
        protected void onResume() {
            super.onResume();
            if (bluetooth.isConnected()) {
                bluetooth.updateStatusView('c', connectButton, connectionStatus);
            }else{
                bluetooth.updateStatusView('d', connectButton, connectionStatus);
            }
        }

    @Override
    protected void onPause(){
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }
}