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
import androidx.core.content.ContextCompat;
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
    private Button connectButton;
    private TextView statusText;
    private LinearLayout linearLayout;
    private DatabaseHelper dbHelper;
    private Button scanButton;
    private Handler handler;
    private Runnable legitcheck;
    private boolean isScanning = false;


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

        bluetooth = Bluetooth.getInstance(getApplicationContext());

        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        String deviceAddress = preferences.getString("selected_device_address", "");
        bluetooth.handleConnectButton(connectButton, connectionStatus,deviceAddress);

        if (bluetooth.isConnected()) {
            bluetooth.updateStatusView('c', connectButton, connectionStatus);
        }

        //BLUETOOTH 2


        dbHelper = new DatabaseHelper(this);

        linearLayout = findViewById(R.id.linearLayout);
        scanButton = findViewById(R.id.btnScan);
        updateScanButtonState();


        updateStatus(getString(R.string.conn_bt_scan));
        checkBluetoothConnection();
        handler = new Handler();
        legitcheck = new Runnable() {
            @Override
            public void run() {
                updateScanButtonState();

                handler.postDelayed(this, 300);
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
            updateStatus(getString(R.string.scan_ready));
        } else {
            updateStatus(getString(R.string.conn_bt_scan));
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
        if (isScanning) {
            //ignorovani opakovaneho mackani scan tlacitka
            return;
        }

        isScanning = true; //bool na status scanovani
        scanButton.setEnabled(false); //vypnuti tlacitka pri scanu

        new Thread(() -> {
            try {
                if (bluetooth != null && bluetooth.isConnected()) {
                    runOnUiThread(() -> {
                        //odstraneni predeslych kodu
                        int childCount = linearLayout.getChildCount();
                        if (childCount > 1) {
                            linearLayout.removeViews(1, childCount - 1);
                        }
                        updateStatus(getString(R.string.scanning));
                    });

                    Thread.sleep(1000);

                    int count = requestNumberOfEngineFaults();

                    runOnUiThread(() -> updateStatus(getString(R.string.found) + " " + count + " " + getString(R.string.faults)));

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
                            String faultDescription = getString(R.string.noresponse) + reqFault;
                            runOnUiThread(() -> addCardView(faultDescription));
                        }

                        f++;
                        Thread.sleep(2000);
                    }
                } else {
                    runOnUiThread(() -> {
                        updateStatus(getString(R.string.conn_bt_scan));
                        Toast.makeText(this, getString(R.string.faults_bt_not), Toast.LENGTH_SHORT).show();
                    });
                }
            } catch (Exception e) {
                runOnUiThread(() -> {
                    Toast.makeText(this, getString(R.string.scan_error), Toast.LENGTH_SHORT).show();
                    updateStatus(getString(R.string.scan_failed));
                    Log.e("EngineFaults", "Scanning error", e);
                });
            } finally {
                runOnUiThread(() -> {
                    updateStatus(getString(R.string.scan_complete));
                    isScanning = false; // reset stavu scanovani
                    if (bluetooth != null && bluetooth.isConnected()) {
                        scanButton.setEnabled(true); // az se doscanuje tak se opet zpristupni tlacitko
                    }
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
        // vytvoreni objektu cardview
        CardView cardView = new CardView(this);

        // nastavovani vlastnosti linearlayoutu
        LinearLayout.LayoutParams cardLayoutParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        cardLayoutParams.setMargins(15, 15, 15, 15);
        cardView.setLayoutParams(cardLayoutParams);

        // nastaveni vlastnosti cardview
        cardView.setCardBackgroundColor(ContextCompat.getColor(this, R.color.cardBackgroundColor));
        cardView.setCardElevation(4);
        cardView.setRadius(15);
        cardView.setContentPadding(16, 16, 16, 16);

        // vytvoreni vnitrniho linearlayoutu
        LinearLayout innerLayout = new LinearLayout(this);
        innerLayout.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));
        innerLayout.setOrientation(LinearLayout.VERTICAL);
        innerLayout.setGravity(Gravity.CENTER_VERTICAL);

        // vytvoreni textview pro zobrazeni kodu
        TextView textView = new TextView(this);
        textView.setText(text);
        textView.setTextColor(ContextCompat.getColor(this, R.color.textcolor));
        textView.setTextSize(18);
        textView.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));
        textView.setPadding(0, 10, 0, 10);

        // pridani textview
        innerLayout.addView(textView);

        // pridani vnitrniho layoutu do cardview
        cardView.addView(innerLayout);

        // finalni pridani cardview do linear layoutu na obrazovce
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
            bluetooth.updateStatusView('c', connectButton, connectionStatus);
        }else{
            bluetooth.updateStatusView('d', connectButton, connectionStatus);
        }
        // kontrola bt
        if (handler != null && legitcheck != null) {
            handler.post(legitcheck);
        }
        updateScanButtonState();
    }

    @Override
    protected void onPause() {
        super.onPause();
        // vypnuti handleru at nebezi na pozadi
        if (handler != null && legitcheck != null) {
            handler.removeCallbacks(legitcheck);
        }

        // Stop any ongoing scan
        isScanning = false;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        // finalni ukonceni handleru
        if (handler != null) {
            if (legitcheck != null) {
                handler.removeCallbacks(legitcheck);
            }
            handler = null;
        }
        legitcheck = null;

        // zavreni pripojeni k db
        if (dbHelper != null) {
            dbHelper.close();
            dbHelper = null;
        }
    }
}