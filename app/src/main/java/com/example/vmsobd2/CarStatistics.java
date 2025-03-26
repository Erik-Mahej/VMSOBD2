package com.example.vmsobd2;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
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
    private DatabaseHelper dbHelper;
    private List<String> pidList;
    private Handler pidHandler = new Handler();
    private int pidIndex = 0;
    private TextView connectionStatus;
    private Button connectButton;
    private Map<String, TextView> pidToTextView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_carstatistics);

        dbHelper = new DatabaseHelper(this);
        connectionStatus = findViewById(R.id.connection_status);
        connectButton = findViewById(R.id.btnConnect);

        bluetooth = new Bluetooth(this, connectionStatus);

        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        String deviceAddress = preferences.getString("selected_device_address", "");
        bluetooth.handleConnectButton(connectButton, deviceAddress);


        pidList = dbHelper.getAllPIDs();




        pidToTextView = new HashMap<>();
        pidToTextView.put("0902", findViewById(R.id.vin));
        pidToTextView.put("0904", findViewById(R.id.makeyear));
        pidToTextView.put("A6", findViewById(R.id.mileage));

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        if (bluetooth.isConnected()) {
            pidIndex = 0;
            sendPIDsSequentially();
        }
    }

    private void sendPIDsSequentially() {
        if (bluetooth.isConnected() && pidList.size() > 0) {
            if (pidIndex >= pidList.size()) {
                pidIndex = 0;
            }

            String pid = pidList.get(pidIndex);
            new Thread(() -> {
                String response = bluetooth.sendObdCommand(pid);
                runOnUiThread(() -> {
                    if (response != null) {
                        updateUI(pid, response);
                    } else {
                        updateUI(pid, "No Data");
                    }
                });
            }).start();

            pidIndex++;
            pidHandler.postDelayed(this::sendPIDsSequentially, 2000);
        } else {
            pidHandler.removeCallbacksAndMessages(null);
        }
    }

    private void updateUI(String pid, String response) {
        TextView targetView = pidToTextView.get(pid);
        if (targetView != null) {
            targetView.setText(targetView.getText() + " " + response);
        }
    }

    public void goBack(View view) {
        if (view.getId() == R.id.btnBack) {
            Intent intentMain = new Intent(CarStatistics.this, MainActivity.class);
            startActivity(intentMain);
        }
        finish();
    }
}
