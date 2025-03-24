package com.example.vmsobd2;

import android.animation.ValueAnimator;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
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
import com.udojava.evalex.Expression;

import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import android.app.AlertDialog;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class carDashboard extends AppCompatActivity {
    private Bluetooth bluetooth;
    private TextView connectionStatus,label1, label2, label3,moretext;

    private Button connectButton;
    private DeluxeSpeedView speedView1, speedView2, speedView3;
    private Handler handler;
    private Switch switch1;
    private Runnable obdPollingRunnable;
    private boolean switchik = false;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private DatabaseHelper dbHelper;



    private GaugeMetric gauge1Metric = GaugeMetric.RPM;
    private GaugeMetric gauge2Metric = GaugeMetric.SPEED;
    private GaugeMetric gauge3Metric = GaugeMetric.FUEL_LEVEL;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_dashboard);

        //Deklarace
        speedView1 = findViewById(R.id.speedView1);
        speedView2 = findViewById(R.id.speedView2);
        speedView3 = findViewById(R.id.speedView3);
        label1 = findViewById(R.id.textView1);
        label2 = findViewById(R.id.textView2);
        label3 = findViewById(R.id.textView3);
        moretext=findViewById(R.id.moretext);

        switch1 = findViewById(R.id.moreswitch);
        //tento switch ovlada jestli je zapnut presos dat
        switch1.setOnCheckedChangeListener((buttonView, isChecked) -> {
            switchik = isChecked;
            String message = isChecked ? "Live Data ON" : "Live Data OFF";
            Toast.makeText(carDashboard.this, message, Toast.LENGTH_SHORT).show();
        });

        //BLUETOOTH 1
        connectionStatus = findViewById(R.id.connection_status);
        connectButton = findViewById(R.id.btnConnect);

        bluetooth = new Bluetooth(this, connectionStatus);

        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        String deviceAddress = preferences.getString("selected_device_address", "");
        bluetooth.handleConnectButton(connectButton, deviceAddress);
        //BLUETOOTH 2

        //DATABASE 1

        dbHelper = new DatabaseHelper(this);

        //DATABASE 2

        //zde se v shared preferences uklada rozlozeni jednotlivych tachometru
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        gauge1Metric = GaugeMetric.valueOf(prefs.getString("gauge1Metric", GaugeMetric.RPM.name()));
        gauge2Metric = GaugeMetric.valueOf(prefs.getString("gauge2Metric", GaugeMetric.SPEED.name()));
        gauge3Metric = GaugeMetric.valueOf(prefs.getString("gauge3Metric", GaugeMetric.FUEL_LEVEL.name()));

        updateGaugeLabelsAndUnits();
        updateGaugeLabelsText();

        speedView1.setOnClickListener(v -> showMetricSelectionDialog(1));
        speedView2.setOnClickListener(v -> showMetricSelectionDialog(2));
        speedView3.setOnClickListener(v -> showMetricSelectionDialog(3));

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

                    editor.apply();
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
            double value = getMetricValue(metric);
            if (value >= 0) {
                float valueFloat = (float) value;

                runOnUiThread(() -> {
                    //view.speedTo(valueFloat, 0);
                    animateGauge(view, (int) valueFloat);
                });
            }
        });
    }

    private void animateGauge(DeluxeSpeedView view, int targetValue) {
        float currentValue = view.getSpeed();
        ValueAnimator animator = ValueAnimator.ofInt((int) currentValue, targetValue);
        animator.setDuration(300);
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
    private double getMetricValue(GaugeMetric metric) {
        String response;
        float value = -1;

        switch (metric) {
            case RPM:
                response = bluetooth.sendObdCommand("010C");
                moretext.setText("RPM Response: " + response);
                String formula = parseResponse(response,dbHelper);
                try {
                    Expression expression = new Expression(formula);
                    BigDecimal result = expression.eval();
                    value = result.floatValue();
                } catch (Expression.ExpressionException e) {
                    Log.e("ExpressionError", "Formula evaluation error: " + e.getMessage());
                }

                break;
            case SPEED:
                //response = bluetooth.sendObdCommand("010D");
                //value = parseSpeed(response);
                break;
            case FUEL_LEVEL:
                /*
                response = bluetooth.sendObdCommand("012F");
                if (response != null && response.startsWith("412F") && response.length() >= 6) {
                    try {
                        value = Integer.parseInt(response.substring(4, 6), 16) * 100 / 255;
                    } catch (Exception e) {
                        Log.e("OBD", "Fuel parse error: " + e.getMessage());
                    }
                }

                 */
                break;
            case AVG_CONSUMPTION:
                /*
                response = bluetooth.sendObdCommand("015F");
                value = parseConsumption(response);

                 */
                break;
            case CURRENT_CONSUMPTION:
                /*
                response = bluetooth.sendObdCommand("015E");
                value = parseConsumption(response);

                 */
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

     public String parseResponse(String response, DatabaseHelper dbHelper) {
        if (response == null) {
            Log.e("ParseResponseError", "Response is null.");
            return "Invalid response";
        }

        Pattern pattern = Pattern.compile("41[0-9A-Fa-f]{2}");
        Matcher matcher = pattern.matcher(response);

        if (matcher.find()) {
            String pid = matcher.group();  //tady to hleda znaky co se podobaji jakemukoliv PID
            ObdFormula formulaData = dbHelper.getFormulaByPid(pid);

            if (formulaData != null) {
                int index = response.indexOf(pid);
                String hexPart = response.substring(index + 4);  // skipnuti PID

                // na zaklade toho jestli ma PID 1 nebo 2 HEX hodnoty tak se pokracuje dal
                String A_str = null, B_str = null;
                if (formulaData.hexCount >= 1 && hexPart.length() >= 2)
                    A_str = hexPart.substring(0, 2);
                if (formulaData.hexCount == 2 && hexPart.length() >= 4)
                    B_str = hexPart.substring(2, 4);

                int A = A_str != null ? Integer.parseInt(A_str, 16) : 0;
                int B = B_str != null ? Integer.parseInt(B_str, 16) : 0;

                // zde se vymeni hodnota ze vzorce za hodnoty z OBD2
                String evalFormula = formulaData.formula
                        .replace("A", String.valueOf(A))
                        .replace("B", String.valueOf(B));

                return evalFormula;
            }
        }
        return "Unknown PID or format";
    }

    public void goBack(View view) {
        if (view.getId() == R.id.btnBack) {
            Intent intent = new Intent(carDashboard.this, MainActivity.class);
            startActivity(intent);
        }
        finish();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        bluetooth.handlePermissionsResult(requestCode, permissions, grantResults);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (bluetooth.isConnected()) {
            connectionStatus.setText("OBD2 Status: Connected");
            connectButton.setText("Disconnect");
        }
    }
    @Override
    protected void onPause() {
        super.onPause();
        handler.removeCallbacks(obdPollingRunnable);
        bluetooth.disconnect();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        bluetooth.disconnect();
        handler.removeCallbacksAndMessages(null);
        if (dbHelper != null) {
            dbHelper.close();
        }
        if (executor != null && !executor.isShutdown()) {
            executor.shutdownNow();
        }
    }

}