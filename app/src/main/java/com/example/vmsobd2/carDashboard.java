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
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.activity.EdgeToEdge;

import com.github.anastr.speedviewlib.DeluxeSpeedView;

import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;


public class carDashboard extends AppCompatActivity {
    private Bluetooth bluetooth;
    private TextView connectionStatus, moretext;
    private Button connectButton;
    private DeluxeSpeedView speedView1, speedView2, speedView3;
    private Handler handler;
    private Switch switch1;
    private Runnable obdPollingRunnable;
    private boolean switchik = false;
    private static boolean BTCN = false;
    private static boolean going = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_dashboard);

        connectionStatus = findViewById(R.id.connection_status);
        connectButton = findViewById(R.id.btnConnect);
        moretext = findViewById(R.id.moretext);

        speedView1 = findViewById(R.id.speedView);   // RPM
        speedView2 = findViewById(R.id.speedView2);  // Speed
        speedView3 = findViewById(R.id.speedView3);  // Fuel level

        switch1 = findViewById(R.id.moreswitch);
        switch1.setOnCheckedChangeListener((buttonView, isChecked) -> {
            switchik = isChecked;
            String message = isChecked ? "Live Data ON" : "Live Data OFF";
            Toast.makeText(carDashboard.this, message, Toast.LENGTH_SHORT).show();
        });

        bluetooth = new Bluetooth(this, connectionStatus);

        connectButton.setOnClickListener(v -> handleConnectButton());

        Intent intent = getIntent();
        BTCN = intent.getBooleanExtra("BTCN", false);
        if (BTCN) handleConnectButton();

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        handler = new Handler();
        obdPollingRunnable = new Runnable() {
            @Override
            public void run() {
                if (bluetooth.isConnected() && switchik) {
                    requestEngineRPM();
                    requestCarSpeed();
                    requestFuelLevel();
                }
                handler.postDelayed(this, 3000);
            }
        };

        handler.post(obdPollingRunnable);
    }

    private void requestEngineRPM() {
        String response = bluetooth.sendObdCommand("010C"); // RPM PID
        if (response != null) {
            int rpm = parseRpm(response);
            speedView1.speedTo(rpm, 300);
            moretext.setText("RPM Response: " + response);
        } else {
            moretext.setText("RPM: No response");
        }
    }

    private void requestCarSpeed() {
        String response = bluetooth.sendObdCommand("010D"); // Speed PID
        if (response != null) {
            int speed = parseSpeed(response);
            speedView2.speedTo(speed, 300);
        }
    }

    private void requestFuelLevel() {
        String response = bluetooth.sendObdCommand("012F"); // Fuel Level PID
        if (response != null && response.startsWith("412F") && response.length() >= 6) {
            try {
                int fuelPercent = Integer.parseInt(response.substring(4, 6), 16) * 100 / 255;
                speedView3.speedTo(fuelPercent, 300);
            } catch (Exception e) {
                Log.e("OBD", "Fuel parse error: " + e.getMessage());
            }
        }
    }
    public int parseRpm(String response) {
        if (response == null) return -1;

        response = response.replaceAll(" ", "").toUpperCase();

        // Look for 410C in the response
        int index = response.indexOf("410C");
        if (index != -1 && index + 8 <= response.length()) {
            try {
                String A_str = response.substring(index + 4, index + 6);
                String B_str = response.substring(index + 6, index + 8);
                String both = A_str+B_str;
                int Value = Integer.parseInt(both, 16);

                return Value / 4;
            } catch (Exception e) {
                Log.e("Bluetooth", "RPM parse failed: " + e.getMessage());
                return -1;
            }
        } else {
            Log.w("Bluetooth", "410C not found in response.");
            return -1;
        }
    }
    public int parseSpeed(String response) {
        if (response != null && response.startsWith("410D") && response.length() >= 6) {
            return Integer.parseInt(response.substring(4, 6), 16);
        }
        return -1;
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
                Toast.makeText(this, "Please select a device in settings.", Toast.LENGTH_LONG).show();
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
        handler.removeCallbacks(obdPollingRunnable);
        bluetooth.disconnect(true);
    }
    @Override
    protected void onPause() {
        super.onPause();
        bluetooth.disconnect(true);
    }
}