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
    private DeluxeSpeedView speedView2;
    private DeluxeSpeedView speedView3;
    private Handler handler;
    private Runnable rpmRequestRunnable;
    private static boolean going = false;
    private static boolean BTCN = false;


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
        speedView2 = findViewById(R.id.speedView2);
        speedView3 = findViewById(R.id.speedView3);

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


        handler = new Handler();
        rpmRequestRunnable = new Runnable() {
            @Override
            public void run() {
                if (bluetooth.isConnected()) {
                    requestEngineRPM();
                    requestCarSpeed();
                    requestEngineConsumption();
                }else{
                    speedView1.speedTo(0);
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
        int decodedResponse = decodeResponse(response);
        updateRPMDisplay(decodedResponse);
    }
    private void requestCarSpeed() {
        //tady se posila command na rychlost
        String command = "010D"; //tady se definuje PID
        bluetooth.sendCommand(command);
        String response = bluetooth.readResponse();
        int decodedResponse = decodeResponse(response);
        updateSpeedDisplay(decodedResponse);
    }
    private void requestEngineConsumption() {
        //tady se posila command na spotrebu
        String command = "012F"; //tady se definuje PID
        bluetooth.sendCommand(command);
        String response = bluetooth.readResponse();
        int decodedResponse = decodeResponse(response);
        updateConstumptionDisplay(decodedResponse);
    }
    /*
    private void handleResponse(String response) {
        Log.d("carDashboard", "Received response: " + response);
        try {
            int decodedResponse = decodeResponse(response);
            updateRPMDisplay(decodedResponse);
        } catch (Exception e) {
            Log.e("carDashboard", "Error processing response: " + e.getMessage());
        }
    }
     */

    private void updateRPMDisplay(int unit) {
        speedView1.speedTo(unit,300);
    }
    private void updateSpeedDisplay(int unit) {
        speedView2.speedTo(unit,300);
    }
    private void updateConstumptionDisplay(int unit) {
        speedView3.speedTo(unit,300);
    }
    public static int decodeResponse(String response) {
        String[] parts = response.split(" ");


        if (parts.length < 5) {
            Log.e("carDashboard", "Invalid response format. Expected at least 5 parts. Response: " + response);
            throw new IllegalArgumentException("Invalid response format. Expected at least 5 parts.");
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

    public void goBack(View view) {
        if (view.getId() == R.id.btnBack) {
            Intent intent = new Intent(carDashboard.this, MainActivity.class);
            intent.putExtra("BTCN", BTCN);
            startActivity(intent);
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
        bluetooth.disconnect(true);
    }
    @Override
    protected void onPause() {
        super.onPause();
        bluetooth.disconnect(true);
    }
}