package com.example.vmsobd2;

import android.animation.ValueAnimator;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.text.Editable;
import android.text.TextWatcher;
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
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class carDashboard extends AppCompatActivity {
    private Bluetooth bluetooth;
    private TextView connectionStatus;
    private Button connectButton;
    private TextView label1;
    private TextView label2;
    private TextView label3;
    private TextView moretext;

    private DeluxeSpeedView speedView1, speedView2, speedView3;
    private Handler handler;
    private Switch switch1;
    private Runnable obdPollingRunnable;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private DatabaseHelper dbHelper;

    private final Map<DeluxeSpeedView, ValueAnimator> gaugeAnimators = new HashMap<>();
    private boolean isPollingActive = false;


    private GaugeMetric gauge1Metric = GaugeMetric.RPM;
    private GaugeMetric gauge2Metric = GaugeMetric.SPEED;
    private GaugeMetric gauge3Metric = GaugeMetric.ENGINE_REFERENCE_TORQUE;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_dashboard);
        //info o ovladani
        Toast.makeText(carDashboard.this, getString(R.string.change_metric), Toast.LENGTH_SHORT).show();

        //Deklarace
        speedView1 = findViewById(R.id.speedView1);
        speedView2 = findViewById(R.id.speedView2);
        speedView3 = findViewById(R.id.speedView3);
        label1 = findViewById(R.id.textView1);
        label2 = findViewById(R.id.textView2);
        label3 = findViewById(R.id.textView3);
        //moretext=findViewById(R.id.moretext);

        switch1 = findViewById(R.id.moreswitch);
        //tento switch ovlada jestli je zapnut presos dat

        switch1.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (bluetooth.isConnected()) {
                isPollingActive = isChecked;
                if (isChecked) {
                    handler.post(obdPollingRunnable);
                    Toast.makeText(carDashboard.this, getString(R.string.data_on), Toast.LENGTH_SHORT).show();
                } else {
                    handler.removeCallbacks(obdPollingRunnable);
                    Toast.makeText(carDashboard.this, getString(R.string.data_off), Toast.LENGTH_SHORT).show();
                }
            } else {
                switch1.setChecked(false);
                isPollingActive = false;
                Toast.makeText(carDashboard.this, getString(R.string.bt_must_b_conn), Toast.LENGTH_SHORT).show();
            }
        });


        speedView1.setOnLongClickListener(v -> {
            if (!switch1.isChecked()) {
                showMetricSelectionDialog(1);
                return true;
            } else {
                Toast.makeText(carDashboard.this, getString(R.string.data_on_cant), Toast.LENGTH_SHORT).show();
                return false;
            }
        });

        speedView2.setOnLongClickListener(v -> {
            if (!switch1.isChecked()) {
                showMetricSelectionDialog(2);
                return true;
            } else {
                Toast.makeText(carDashboard.this, getString(R.string.data_on_cant), Toast.LENGTH_SHORT).show();
                return false;
            }
        });

        speedView3.setOnLongClickListener(v -> {
            if (!switch1.isChecked()) {
                showMetricSelectionDialog(3);
                return true;
            } else {
                Toast.makeText(carDashboard.this, getString(R.string.data_on_cant), Toast.LENGTH_SHORT).show();
                return false;
            }
        });



        //BLUETOOTH 1
        connectionStatus = findViewById(R.id.connection_status);
        connectButton = findViewById(R.id.btnConnect);

        bluetooth = Bluetooth.getInstance(getApplicationContext());

        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        String deviceAddress = preferences.getString("selected_device_address", "");
        bluetooth.handleConnectButton(connectButton, connectionStatus,deviceAddress);

        if (bluetooth.isConnected()) {
            bluetooth.updateStatusView('c', connectButton, connectionStatus);
        }

        //BLUETOOTH 2

        //DATABASE 1

        dbHelper = new DatabaseHelper(this);

        //DATABASE 2

        //zde se v shared preferences uklada rozlozeni jednotlivych tachometru
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        gauge1Metric = GaugeMetric.valueOf(prefs.getString("gauge1Metric", GaugeMetric.RPM.name()));
        gauge2Metric = GaugeMetric.valueOf(prefs.getString("gauge2Metric", GaugeMetric.SPEED.name()));
        gauge3Metric = GaugeMetric.valueOf(prefs.getString("gauge3Metric", GaugeMetric.ENGINE_REFERENCE_TORQUE.name()));

        updateGaugeLabelsAndUnits();
        updateGaugeLabelsText();


        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        //handler ktery dava hodnoty jednotlivym tachometrum
        handler = new Handler();
        obdPollingRunnable = new Runnable() {
            @Override
            public void run() {
                if (!bluetooth.isConnected() || !isPollingActive) {
                    runOnUiThread(() -> resetGauges());
                    return;
                }
                updateGaugeAsync(speedView1, gauge1Metric);
                updateGaugeAsync(speedView2, gauge2Metric);
                updateGaugeAsync(speedView3, gauge3Metric);
                if (isPollingActive) {
                    handler.postDelayed(this, 300);
                }
            }
        };
    }

    private void resetGauges() {
        speedView1.speedTo(0, 0);
        speedView2.speedTo(0, 0);
        speedView3.speedTo(0, 0);
    }


    private double getMetricValue(GaugeMetric metric) {
        String response;
        float value = -1;
        String formula;
        String request =dbHelper.getMetricPID(metric.name());
        response = bluetooth.sendObdCommand(request);
        formula = parseResponse(response, dbHelper);
        if (formula == null || formula.equals("Invalid response") || formula.equals("Unknown PID or format")) {
            Log.e("FormulaError", "Invalid formula for "+ metric.name() + " " + formula);
            return value;
        }
        try {
            Expression expression = new Expression(formula);
            BigDecimal result = expression.eval();
            value = result.floatValue();
            Log.d("OBD", "VALUE "+metric.name()+" " + value);
        } catch (Expression.ExpressionException e) {
            Log.e("ExpressionError", "Formula evaluation error: " + e.getMessage());
        }
        return value;
    }

    private void showMetricSelectionDialog(int gaugeNumber) {
        DatabaseHelper dbHelper = new DatabaseHelper(this);
        final String[] metrics = dbHelper.getAllMetricLabelsSortByID().toArray(new String[0]);
        new AlertDialog.Builder(this)
                .setTitle(getString(R.string.select_metric))
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
                    resetGauges();
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
                Log.d("OBD", "Float Value "+metric.name()+" " + valueFloat);
                runOnUiThread(() -> {
                    if (switch1.isChecked()) {
                        animateGauge(view, (int) valueFloat);
                    }
                });
            }
        });
    }

    private void animateGauge(DeluxeSpeedView view, int targetValue) {
        ValueAnimator existingAnimator = gaugeAnimators.get(view);
        if (existingAnimator != null && existingAnimator.isRunning()) {
            existingAnimator.cancel();
        }

        float currentValue = view.getSpeed();
        ValueAnimator animator = ValueAnimator.ofInt((int) currentValue, targetValue);
        animator.setDuration(150);
        animator.addUpdateListener(animation -> {
            int animatedValue = (int) animation.getAnimatedValue();
            view.speedTo(animatedValue, 300);//nastaveni jak smooth zmena hodnot je
        });
        animator.start();
    }

    private void setGaugeUnit(DeluxeSpeedView view, GaugeMetric metric) {
        DatabaseHelper dbHelper = new DatabaseHelper(this);
        GaugeSetting gaugeSetting = dbHelper.getGaugeSetting(metric.name());

        if (gaugeSetting != null) {
            view.setUnit(gaugeSetting.getUnit());
            view.setMaxSpeed(gaugeSetting.getMaxSpeed());
            view.setWithTremble(false);
        }
    }

    private void updateGaugeLabelsText() {
        label1.setText(getMetricLabel(gauge1Metric));
        label2.setText(getMetricLabel(gauge2Metric));
        label3.setText(getMetricLabel(gauge3Metric));
    }

    private String getMetricLabel(GaugeMetric metric) {
        DatabaseHelper dbHelper = new DatabaseHelper(this);
        return dbHelper.getMetricLabel(metric.name());
    }

     public String parseResponse(String response, DatabaseHelper dbHelper) {
        if (response == null) {
            Log.e("ParseResponseError", "Response is null.");
            return "Invalid response";
        }
        String evalFormula = null;
        Pattern pattern = Pattern.compile("41[0-9A-Fa-f]{2}");
        Matcher matcher = pattern.matcher(response);

        if (matcher.find()) {
            String pid = matcher.group();  //tady to hleda znaky co se podobaji jakemukoliv PID
            ObdFormula formulaData = dbHelper.getFormulaByPid(pid);
            Log.d("OBD", "PID: " + pid);
            Log.d("OBD", "Formula from DB: " + formulaData.formula);
            Log.d("OBD", "Hex Count: " + formulaData.hexCount);

            if (formulaData != null) {
                int index = response.indexOf(pid);
                String hexPart = response.substring(index + 4);  // skipnuti PID

                // na zaklade toho jestli ma PID 1 nebo 2 HEX hodnoty tak se pokracuje dal
                String A_str = null, B_str = null;

                if (formulaData.hexCount == 1 && hexPart.length() >= 2) {
                    A_str = hexPart.substring(0, 2);
                } else if (formulaData.hexCount == 2 && hexPart.length() >= 4) {
                    A_str = hexPart.substring(0, 2); // Get A
                    B_str = hexPart.substring(2, 4); // Get B
                }


                int A = A_str != null ? Integer.parseInt(A_str, 16) : 0;
                int B = B_str != null ? Integer.parseInt(B_str, 16) : 0;

                if (formulaData.hexCount == 1 && hexPart.length() >= 2) {
                    evalFormula = formulaData.formula.replace("A", String.valueOf(A));
                } else if (formulaData.hexCount == 2 && hexPart.length() >= 4) {
                    evalFormula = formulaData.formula
                            .replace("A", String.valueOf(A))
                            .replace("B", String.valueOf(B));
                }


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
            bluetooth.updateStatusView('c', connectButton, connectionStatus);
        }else{
            bluetooth.updateStatusView('d', connectButton, connectionStatus);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        isPollingActive = false;
        handler.removeCallbacks(obdPollingRunnable);
        switch1.setChecked(false);
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        isPollingActive = false;
        handler.removeCallbacksAndMessages(null);
        switch1.setChecked(false);
        if (executor != null && !executor.isShutdown()) {
            executor.shutdownNow();
        }
        if (dbHelper != null) {
            dbHelper.close();
        }
    }


}