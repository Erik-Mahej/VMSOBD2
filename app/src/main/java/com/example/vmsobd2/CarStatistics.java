package com.example.vmsobd2;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CarStatistics extends AppCompatActivity {
    private Bluetooth bluetooth;
    private TextView connectionStatus, vin, makeyear, mileage;
    private Button connectButton;
    private Handler handler;
    private Runnable legitcheck;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_carstatistics);

        connectionStatus = findViewById(R.id.connection_status);
        connectButton = findViewById(R.id.btnConnect);
        vin = findViewById(R.id.vin);
        makeyear = findViewById(R.id.makeyear);
        mileage = findViewById(R.id.mileage);

        bluetooth = Bluetooth.getInstance(getApplicationContext());

        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        String deviceAddress = preferences.getString("selected_device_address", "");
        bluetooth.handleConnectButton(connectButton, connectionStatus, deviceAddress);

        if (bluetooth.isConnected()) {
            bluetooth.updateStatusView('c', connectButton, connectionStatus);
        }

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        handler = new Handler();
        legitcheck = new Runnable() {
            @Override
            public void run() {
                if (bluetooth.isConnected()) {
                    requestVIN();
                }
                handler.postDelayed(this, 1000); // kontrola kazdou sekundu na bt connection
            }
        };
        handler.post(legitcheck);
    }
    //pozadavek na vin auta
    private void requestVIN() {
        if (!bluetooth.isConnected()) {
            vin.setText(getString(R.string.vin_btnot));
            return;
        }
        new Thread(() -> {
            String response = bluetooth.sendObdCommand("0902");
            runOnUiThread(() -> {
                if (response != null) {
                    String vinNumber = processVINResponse(response);
                    if (vinNumber != null && vinNumber.length() >= 17) {
                        vin.setText(getString(R.string.car_vin) + vinNumber);
                    }
                } else {
                    vin.setText(R.string.vin_not);
                }
            });
        }).start();
    }
    //parse vinka
    public static String processVINResponse(String response) {
        if (response == null || response.isEmpty()) {
            Log.w("VIN", "Null or empty response");
            return null;
        }

        // ocisteni response
        String cleanResponse = response.replaceAll(" ", "").toUpperCase();

        // verifikace
        if (!cleanResponse.contains("490201") || cleanResponse.length() < 12) {
            Log.w("VIN", "Invalid VIN response format: " + cleanResponse);
            return null;
        }

        try {
            // hledani response PIDu
            int headerIndex = cleanResponse.indexOf("490201");
            if (headerIndex < 0 || headerIndex + 12 > cleanResponse.length()) {
                Log.w("VIN", "Malformed VIN header");
                return null;
            }

            // extrakce vin ze stringu
            String vinHex = cleanResponse.substring(headerIndex + 6);

            // do ASCII
            StringBuilder vinBuilder = new StringBuilder();
            for (int i = 0; i < vinHex.length(); i += 2) {
                String hexPair = vinHex.substring(i, i + 2);
                try {
                    int charCode = Integer.parseInt(hexPair, 16);
                    // jenom validni ACII chars
                    if (charCode >= 32 && charCode <= 126) {
                        vinBuilder.append((char) charCode);
                    }
                } catch (NumberFormatException e) {
                    Log.e("VIN", "Invalid hex pair: " + hexPair);
                }
            }

            String fullVin = vinBuilder.toString();

            // return vinka
            if (fullVin.length() >= 17) {
                return fullVin.substring(0, 17); // standartni delka vinka
            } else if (!fullVin.isEmpty()) {
                Log.w("VIN", "Partial VIN received: " + fullVin);
                return fullVin;
            } else {
                Log.w("VIN", "No valid VIN data extracted");
                return null;
            }

        } catch (Exception e) {
            Log.e("VIN", "VIN processing error: " + e.getMessage());
            return null;
        }
    }

    public void goBack(View view) {
        if (view.getId() == R.id.btnBack) {
            Intent intentMain = new Intent(CarStatistics.this, MainActivity.class);
            startActivity(intentMain);
        }
        finish();
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
    protected void onPause() {
        super.onPause();
        handler.removeCallbacks(legitcheck);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        handler.removeCallbacks(legitcheck);
    }
}