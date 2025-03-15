package com.example.vmsobd2;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import androidx.activity.EdgeToEdge;
import com.github.anastr.speedviewlib.DeluxeSpeedView;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.util.Arrays;

public class carDashboard extends AppCompatActivity {
    private Bluetooth bluetooth;
    private TextView connectionStatus;
    private Button connectButton;
    private DeluxeSpeedView speedView1;
    private Handler handler;
    private Runnable rpmRequestRunnable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_dashboard);

        connectionStatus = findViewById(R.id.connection_status);
        connectButton = findViewById(R.id.btnConnect);

        bluetooth = new Bluetooth(this, connectionStatus);

        connectButton.setOnClickListener(v -> handleConnectButton());

        speedView1 = findViewById(R.id.speedView);



        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });


        handler = new Handler();
        rpmRequestRunnable = new Runnable() {
            @Override
            public void run() {
                if (bluetooth.isConnected()) {
                    requestEngineRPM();
                }
                //jak casto se posila dotaz
                handler.postDelayed(this, 100);
            }
        };

        //tady se zapne posilani dotazu
        handler.post(rpmRequestRunnable);
    }


    private void requestEngineRPM() {
        //tady se posila command na rpm
        String command = "010C"; //tady se definuje PID
        bluetooth.sendCommand(command);
        String response = bluetooth.readResponse();
        handleResponse(response);
    }

    private void handleResponse(String response) {
        Log.d("carDashboard", "Received response: " + response);
        try {
            int rpm = decodeRPM(response);
            updateRPMDisplay(rpm);
        } catch (Exception e) {
            Log.e("carDashboard", "Error processing response: " + e.getMessage());
        }
    }

    private void updateRPMDisplay(int rpm) {
        speedView1.speedTo(rpm,300);
    }

    public static int decodeRPM(String response) {
        String[] parts = response.split(" ");


        if (parts.length < 5) {
            Log.e("carDashboard", "Invalid response format. Expected at least 5 parts. Response: " + response);
            throw new IllegalArgumentException("Invalid response format. Expected at least 5 parts.");
        }
        if (!parts[1].equals("41") || !parts[2].equals("0C")) {
            Log.e("carDashboard", "Response is not for RPM request. Response: " + response);
            throw new IllegalArgumentException("Response is not for RPM request.");
        }

        // Extract the RPM data (the next two bytes)
        String rpmHex1 = parts[3]; // First byte
        String rpmHex2 = parts[4]; // Second byte

        // Combine the two bytes into a single hex string
        String combinedHex = (rpmHex1 + rpmHex2).trim();

        return Integer.parseInt(combinedHex, 16);
    }

    private void handleConnectButton() {
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
    }

    public void goBack(View view) {
        if (view.getId() == R.id.btnBack) {
            Intent intentMain = new Intent(carDashboard.this, MainActivity.class);
            startActivity(intentMain);
        }
        finish();
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

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Stop the handler when the activity is destroyed
        handler.removeCallbacks(rpmRequestRunnable);
        bluetooth.disconnect();
    }
}