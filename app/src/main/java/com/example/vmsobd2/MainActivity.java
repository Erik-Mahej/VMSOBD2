package com.example.vmsobd2;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class MainActivity extends AppCompatActivity {
    private static boolean BTCN = false;
    private static boolean going = false;
    private Bluetooth bluetooth;
    private TextView connectionStatus;
    private Button connectButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        connectionStatus = findViewById(R.id.connection_status);
        connectButton = findViewById(R.id.btnConnect);

        bluetooth = new Bluetooth(this, connectionStatus);

        connectButton.setOnClickListener(v -> handleConnectButton());

        Intent intent = getIntent();
        boolean BTCN = intent.getBooleanExtra("BTCN", false);

        if (BTCN) {
            handleConnectButton();
        }

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    private void handleConnectButton() {
        if (bluetooth.isConnected()) {
            bluetooth.disconnect(going);
            connectButton.setText("Connect");
            connectionStatus.setText("OBD2 Status: Disconnected");
            BTCN = false;
        } else {
            SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
            String deviceAddress = preferences.getString("selected_device_address", null);
            if (deviceAddress != null) {
                bluetooth.connect(deviceAddress);
                if (bluetooth.isConnected()) {
                    BTCN = true;
                    connectButton.setText("Disconnect");
                }
            } else {
                connectionStatus.setText("OBD2 Status: No Device Selected");
                Toast.makeText(this, "No device selected. Please select a device in the settings.", Toast.LENGTH_LONG).show();
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == Bluetooth.REQUEST_BLUETOOTH_CONNECT) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
                String deviceAddress = preferences.getString("selected_device_address", null);
                if (deviceAddress != null) {
                    bluetooth.connect(deviceAddress);
                    if (bluetooth.isConnected()) {
                        connectButton.setText("Disconnect");
                    }
                } else {
                    connectionStatus.setText("OBD2 Status: No Device Selected");
                    Toast.makeText(this, "No device selected. Please select a device in the settings.", Toast.LENGTH_LONG).show();
                }
            } else {
                connectionStatus.setText("OBD2 Status: Permission Denied");
                Toast.makeText(this, "Bluetooth permission denied. Allow it in the aplication settings.", Toast.LENGTH_LONG).show();
            }
        }
    }

    public void Transport(View view) {
        Intent intent = null;
        int viewId = view.getId(); // Store the view ID in a variable

        if (viewId == R.id.btnDashboard) {
            intent = new Intent(MainActivity.this, carDashboard.class);
        } else if (viewId == R.id.btnStats) {
            intent = new Intent(MainActivity.this, CarStatistics.class);
        } else if (viewId == R.id.btnEngineFaults) {
            intent = new Intent(MainActivity.this, EngineFaults.class);
        } else if (viewId == R.id.btnSettings) {
            intent = new Intent(MainActivity.this, AppSettings.class);
        } else {
            // Handle the case where the view ID does not match any known button
            return; // Exit the method if no valid button was pressed
        }

        // Put the extra and start the activity if intent is not null
        if (intent != null) {
            intent.putExtra("BTCN", BTCN);
            startActivity(intent);
        }
    }
    @Override
    protected void onDestroy() {
        super.onDestroy();
        bluetooth.disconnect(true);
    }
    @Override
    protected void onPause() {
        super.onPause();
        bluetooth.disconnect(true);
    }
}