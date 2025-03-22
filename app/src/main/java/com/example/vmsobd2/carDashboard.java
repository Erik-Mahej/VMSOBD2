package com.example.vmsobd2;

import android.animation.ValueAnimator;
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
import android.app.AlertDialog;
import android.content.DialogInterface;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


public class carDashboard extends AppCompatActivity {
    private Bluetooth bluetooth;
    private TextView connectionStatus,label1, label2, label3,moretext;

    private Button connectButton;
    private DeluxeSpeedView speedView1, speedView2, speedView3;
    private Handler handler;
    private Switch switch1;
    private Runnable obdPollingRunnable;
    private boolean switchik = false;
    private static boolean BTCN = false;
    private static boolean going = false;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();


    private GaugeMetric gauge1Metric = GaugeMetric.RPM;
    private GaugeMetric gauge2Metric = GaugeMetric.SPEED;
    private GaugeMetric gauge3Metric = GaugeMetric.FUEL_LEVEL;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_dashboard);

        connectionStatus = findViewById(R.id.connection_status);
        connectButton = findViewById(R.id.btnConnect);

        speedView1 = findViewById(R.id.speedView1);
        speedView2 = findViewById(R.id.speedView2);
        speedView3 = findViewById(R.id.speedView3);
        label1 = findViewById(R.id.textView1);
        label2 = findViewById(R.id.textView2);
        label3 = findViewById(R.id.textView3);
        moretext=findViewById(R.id.moretext);

        switch1 = findViewById(R.id.moreswitch);
        switch1.setOnCheckedChangeListener((buttonView, isChecked) -> {
            switchik = isChecked;
            String message = isChecked ? "Live Data ON" : "Live Data OFF";
            Toast.makeText(carDashboard.this, message, Toast.LENGTH_SHORT).show();
        });

        bluetooth = new Bluetooth(this, connectionStatus);

        connectButton.setOnClickListener(v -> handleConnectButton());

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        gauge1Metric = GaugeMetric.valueOf(prefs.getString("gauge1Metric", GaugeMetric.RPM.name()));
        gauge2Metric = GaugeMetric.valueOf(prefs.getString("gauge2Metric", GaugeMetric.SPEED.name()));
        gauge3Metric = GaugeMetric.valueOf(prefs.getString("gauge3Metric", GaugeMetric.FUEL_LEVEL.name()));

        updateGaugeLabelsAndUnits();
        updateGaugeLabelsText();

        speedView1.setOnClickListener(v -> showMetricSelectionDialog(1));
        speedView2.setOnClickListener(v -> showMetricSelectionDialog(2));
        speedView3.setOnClickListener(v -> showMetricSelectionDialog(3));




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
                    updateGaugeAsync(speedView1, gauge1Metric);
                    updateGaugeAsync(speedView2, gauge2Metric);
                    updateGaugeAsync(speedView3, gauge3Metric);

                }
                handler.postDelayed(this, 50);
            }
        };


        handler.post(obdPollingRunnable);
    }

    private void showMetricSelectionDialog(int gaugeNumber) {
        final String[] metrics = {"Engine RPM", "Car Speed", "Fuel Level", "Avg Consumption", "Current Consumption"};
        new AlertDialog.Builder(this)
                .setTitle("Select Metric")
                .setItems(metrics, (dialog, which) -> {
                    GaugeMetric selectedMetric = GaugeMetric.values()[which];
                    SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
                    SharedPreferences.Editor editor = prefs.edit();

                    switch (gaugeNumber) {
                        case 1:
                            gauge1Metric = selectedMetric;
                            editor.putString("gauge1Metric", selectedMetric.name());
                            break;
                        case 2:
                            gauge2Metric = selectedMetric;
                            editor.putString("gauge2Metric", selectedMetric.name());
                            break;
                        case 3:
                            gauge3Metric = selectedMetric;
                            editor.putString("gauge3Metric", selectedMetric.name());
                            break;
                    }

                    editor.apply(); // Save it
                    updateGaugeLabelsAndUnits();
                    updateGaugeLabelsText();

                })
                .show();
    }

    private void updateGaugeLabelsAndUnits() {
        setGaugeUnit(speedView1, gauge1Metric);
        setGaugeUnit(speedView2, gauge2Metric);
        setGaugeUnit(speedView3, gauge3Metric);
    }
    private void updateGaugeAsync(DeluxeSpeedView view, GaugeMetric metric) {
        executor.execute(() -> {
            int value = getMetricValue(metric);
            if (value >= 0) {
                runOnUiThread(() -> {
                    animateGauge(view, value);
                });
            }
        });
    }

    private void animateGauge(DeluxeSpeedView view, int targetValue) {
        float currentValue = view.getSpeed();
        ValueAnimator animator = ValueAnimator.ofInt((int) currentValue, targetValue);
        animator.setDuration(500);
        animator.addUpdateListener(animation -> {
            int animatedValue = (int) animation.getAnimatedValue();
            view.speedTo(animatedValue, 0);
        });
        animator.start();
    }



    private void setGaugeUnit(DeluxeSpeedView view, GaugeMetric metric) {
        switch (metric) {
            case RPM:
                view.setUnit("RPM");
                view.setMaxSpeed(6000);
                view.setWithTremble(false);
                break;
            case SPEED:
                view.setUnit("km/h");
                view.setMaxSpeed(240);
                view.setWithTremble(false);
                break;
            case FUEL_LEVEL:
                view.setUnit("%");
                view.setMaxSpeed(100);
                view.setWithTremble(false);
                break;
            case AVG_CONSUMPTION:
            case CURRENT_CONSUMPTION:
                view.setUnit("L/100km");
                view.setMaxSpeed(20);
                view.setWithTremble(false);
                break;
        }
    }
    private int getMetricValue(GaugeMetric metric) {
        String response;
        int value = -1;

        switch (metric) {
            case RPM:
                response = bluetooth.sendObdCommand("010C");
                moretext.setText("RPM Response: " + response);
                value = parseRpm(response);
                break;
            case SPEED:
                response = bluetooth.sendObdCommand("010D");
                value = parseSpeed(response);
                break;
            case FUEL_LEVEL:
                response = bluetooth.sendObdCommand("012F");
                if (response != null && response.startsWith("412F") && response.length() >= 6) {
                    try {
                        value = Integer.parseInt(response.substring(4, 6), 16) * 100 / 255;
                    } catch (Exception e) {
                        Log.e("OBD", "Fuel parse error: " + e.getMessage());
                    }
                }
                break;
            case AVG_CONSUMPTION:
                response = bluetooth.sendObdCommand("015F");
                value = parseConsumption(response);
                break;
            case CURRENT_CONSUMPTION:
                response = bluetooth.sendObdCommand("015E");
                value = parseConsumption(response);
                break;
        }
        return value;
    }

    private void updateGaugeLabelsText() {
        label1.setText(getMetricLabel(gauge1Metric));
        label2.setText(getMetricLabel(gauge2Metric));
        label3.setText(getMetricLabel(gauge3Metric));
    }

    private String getMetricLabel(GaugeMetric metric) {
        switch (metric) {
            case RPM: return "Engine RPM";
            case SPEED: return "Car Speed";
            case FUEL_LEVEL: return "Fuel Level";
            case AVG_CONSUMPTION: return "Avg Consumption";
            case CURRENT_CONSUMPTION: return "Current Consumption";
            default: return "Unknown";
        }
    }


    private void requestEngineRPM() {
        String response = bluetooth.sendObdCommand("010C"); // RPM PID
        if (response != null) {
            int rpm = parseRpm(response);
            speedView1.speedTo(rpm, 50);
            //moretext.setText("RPM Response: " + response);
        } else {
            //moretext.setText("RPM: No response");
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
        if (response == null) return -1;

        response = response.replaceAll(" ", "").toUpperCase();

        int index = response.indexOf("410D");
        if (index != -1 && index + 8 <= response.length()) {
            try {
                String A_str = response.substring(index + 4, index + 6);
                String B_str = response.substring(index + 6, index + 8);
                String both = A_str+B_str;
                int Value = Integer.parseInt(both, 16);

                return Value ;
            } catch (Exception e) {
                Log.e("Bluetooth", "Speed parse failed: " + e.getMessage());
                return -1;
            }
        } else {
            Log.w("Bluetooth", "410D not found in response.");
            return -1;
        }
    }

    public int parseConsumption(String response) {
        if (response == null) return -1;

        response = response.replaceAll(" ", "").toUpperCase();

        // Example: response = "415E0A" or "415F14"
        if ((response.startsWith("415E") || response.startsWith("415F")) && response.length() >= 6) {
            try {
                String A_str = response.substring(4, 6);
                int A = Integer.parseInt(A_str, 16);

                return A; // or (int)(A * 0.1) for decimal-like representation
                //return (int)(A * 0.1); // display as 3.4 L/100km if A = 34

            } catch (Exception e) {
                Log.e("OBD", "Consumption parse error: " + e.getMessage());
            }
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
        executor.shutdownNow();
    }
    @Override
    protected void onPause() {
        super.onPause();
        bluetooth.disconnect(true);
    }
}