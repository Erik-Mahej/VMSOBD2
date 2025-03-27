package com.example.vmsobd2;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Gravity;
import android.view.View;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import android.graphics.Color;
import android.widget.Button;
import android.widget.GridLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.cardview.widget.CardView;

public class EngineFaults extends AppCompatActivity {

    private Bluetooth bluetooth;
    private TextView connectionStatus;
    private TextView statusText;
    private Button connectButton;
    private LinearLayout linearLayout;
    private DatabaseHelper dbHelper;
    private Button scanButton;
    private Handler handler;
    private Runnable legitcheck;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_engine_faults);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        statusText = findViewById(R.id.status);

        //BLUETOOTH 1
        connectionStatus = findViewById(R.id.connection_status);
        connectButton = findViewById(R.id.btnConnect);

        bluetooth = new Bluetooth(this, connectionStatus);

        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        String deviceAddress = preferences.getString("selected_device_address", "");
        bluetooth.handleConnectButton(connectButton, deviceAddress);
        //BLUETOOTH 2


        dbHelper = new DatabaseHelper(this);

        linearLayout = findViewById(R.id.linearLayout);
        scanButton = findViewById(R.id.btnScan);
        updateScanButtonState();


        updateStatus("Connect to Bluetooth to scan");
        checkBluetoothConnection();
        handler = new Handler();
        legitcheck = new Runnable() {
            @Override
            public void run() {
                updateScanButtonState();

                handler.postDelayed(this, 100);
            }
        };
        handler.post(legitcheck);



    }

    private void updateScanButtonState() {
        if (bluetooth != null && bluetooth.isConnected()) {
            scanButton.setEnabled(true);
            scanButton.setVisibility(View.VISIBLE);
        } else {
            scanButton.setEnabled(false);
        }
    }

    private void checkBluetoothConnection() {
        if (bluetooth != null && bluetooth.isConnected()) {
            updateStatus("Ready to scan");
        } else {
            updateStatus("Connect to Bluetooth to scan");
        }
        updateScanButtonState();
    }

    public void goBack(View view) {
        if (view.getId() == R.id.btnBack) {
            Intent intentMain = new Intent(EngineFaults.this, MainActivity.class);
            startActivity(intentMain);
        }
        finish();
    }

    public void run(View view) {
        new Thread(() -> {
            try {
                if (bluetooth != null && bluetooth.isConnected()) {
                    runOnUiThread(() -> {
                        int cardviewCount = linearLayout.getChildCount();
                        if (cardviewCount > 1) {
                            linearLayout.removeViews(1, cardviewCount - 1);
                        }
                        updateStatus("Scanning...");
                    });

                    Thread.sleep(1000);

                    int count = requestNumberOfEngineFaults();

                    runOnUiThread(() -> updateStatus("Found " + count + " faults"));

                    int f = 301;

                    Thread.sleep(1000);

                    for (int i = 0; i < count; i++) {
                        String reqFault = "0" + f;

                        String response = bluetooth.sendObdCommand(reqFault);

                        if (response != null && !response.isEmpty()) {
                            int decodedResponse = decodeResponse(response);

                            String faultDescription = assignFault(decodedResponse);

                            runOnUiThread(() -> {
                                addCardView(faultDescription);
                            });
                        } else {
                            String faultDescription = "No response for code " + reqFault;
                            runOnUiThread(() -> addCardView(faultDescription));
                        }

                        f++;
                        Thread.sleep(2000);
                    }
                    runOnUiThread(() -> updateStatus("Scanning completed"));
                } else {
                    runOnUiThread(() -> {
                        updateStatus("Connect to Bluetooth to scan");
                        Toast.makeText(this, "Bluetooth is not connected. Please connect your OBD2 device.", Toast.LENGTH_SHORT).show();
                    });
                }
            } catch (Exception e) {
                runOnUiThread(() -> {
                    Toast.makeText(this, "An error occurred. Please try again.", Toast.LENGTH_SHORT).show();
                    updateStatus("Scanning failed");
                });
            }
        }).start();
    }



    private void updateStatus(String status) {
        statusText.setText(status);
    }

    private int requestNumberOfEngineFaults() {
        String response = bluetooth.sendObdCommand("0300");
        if (response != null) {
            return  decodeResponseCount(response);
        } else {
            Log.e("EngineFaults", "No response received for command: " + "0300");
        }
        return 0;
    }
    public String assignFault(int responseCode) {
        return dbHelper.getFaultDescription(responseCode);
    }
    public static int decodeResponseCount(String response) {
        if (response == null) return -1;

        response = response.replaceAll(" ", "").toUpperCase();

        int index = response.indexOf("4100");
        if (index != -1 && index + 6 <= response.length()) {
            try {
                String A_str = response.substring(index + 4, index + 6);
                return Integer.parseInt(A_str);
            } catch (Exception e) {
                Log.e("Bluetooth", "Fault parse failed: " + e.getMessage());
                return -1;
            }
        } else {
            Log.w("Bluetooth", "4100 not found in response or not enough characters after 4100.");
            return -1;
        }
    }
    public static int decodeResponse(String response) {
        if (response == null) return -1;

        response = response.replaceAll(" ", "").toUpperCase();

        int index = response.indexOf("P0");
        if (index != -1 && index + 5 <= response.length()) {
            try {
                String A_str = response.substring(index + 2, index + 5);
                 return Integer.parseInt(A_str);
            } catch (Exception e) {
                Log.e("Bluetooth", "Fault parse failed: " + e.getMessage());
                return -1;
            }
        } else {
            Log.w("Bluetooth", "P0 not found in response or not enough characters after P0.");
            return -1;
        }
    }
    private void addCardView(String text) {
        CardView cardView = new CardView(this);
        GridLayout.LayoutParams cardLayoutParams = new GridLayout.LayoutParams();
        cardLayoutParams.setMargins(15, 15, 15, 15);
        cardLayoutParams.width = GridLayout.LayoutParams.MATCH_PARENT;
        cardLayoutParams.height = GridLayout.LayoutParams.MATCH_PARENT;
        cardLayoutParams.rowSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f);
        cardLayoutParams.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f);
        cardView.setLayoutParams(cardLayoutParams);
        cardView.setCardBackgroundColor(Color.parseColor("#f3f3f3"));
        cardView.setCardElevation(4);
        cardView.setRadius(15);

        LinearLayout innerLayout = new LinearLayout(this);
        innerLayout.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT));
        innerLayout.setGravity(Gravity.CENTER);
        innerLayout.setOrientation(LinearLayout.VERTICAL);

        TextView textView = new TextView(this);
        textView.setText(text);
        textView.setTextColor(Color.parseColor("#0D0D0D"));
        textView.setTextSize(18);
        textView.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));
        textView.setPadding(0, 10, 0, 10);

        innerLayout.addView(textView);

        cardView.addView(innerLayout);

        linearLayout.addView(cardView);
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
        updateScanButtonState();
    }
    @Override
    protected void onPause(){
        super.onPause();
        bluetooth.disconnect();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        bluetooth.disconnect();
    }
}