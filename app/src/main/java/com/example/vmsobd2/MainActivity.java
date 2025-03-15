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

        connectButton.setOnClickListener(v -> {
            if (bluetooth.isConnected()) {
                bluetooth.disconnect();
                connectButton.setText("Connect");
                connectionStatus.setText("OBD2 Status: Disconnected");
            } else {
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
            }
        });

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
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
                Toast.makeText(this, "Bluetooth connection permission denied.", Toast.LENGTH_LONG).show();
            }
        }
    }

    public void Transport(View view) {
        if (view.getId() == R.id.btnDashboard) {
            Intent intentDashboard = new Intent(MainActivity.this, carDashboard.class);
            startActivity(intentDashboard);
        } else if (view.getId() == R.id.btnStats) {
            Intent intentDashboard = new Intent(MainActivity.this, CarStatistics.class);
            startActivity(intentDashboard);
        } else if (view.getId() == R.id.btnEngineFaults) {
            Intent intentDashboard = new Intent(MainActivity.this, EngineFaults.class);
            startActivity(intentDashboard);
        } else if (view.getId() == R.id.btnSettings) {
            Intent intentDashboard = new Intent(MainActivity.this, AppSettings.class);
            startActivity(intentDashboard);
        }
    }
}