package com.example.vmsobd2;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class MainActivity extends AppCompatActivity {
    //1.1
    private Bluetooth bluetooth;
    private TextView connectionStatus;
    private Button connectButton;
    //1.1

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        //1.1
        connectionStatus = findViewById(R.id.connection_status);
        connectButton = findViewById(R.id.btnConnect);

        bluetooth = new Bluetooth(this, connectionStatus);

        connectButton.setOnClickListener(v -> {
            SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
            String deviceAddress = preferences.getString("selected_device_address", null);
            if (deviceAddress != null) {
                bluetooth.connect(deviceAddress);
            } else {
                connectionStatus.setText("OBD2 Status: No Device Selected");
            }

            //1.1
        });

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    public void Transport(View view) {
        if (view.getId() == R.id.btnDashboard) {
            Intent intentDashboard = new Intent(MainActivity.this, carDashboard.class);
            startActivity(intentDashboard);
        } else if (view.getId() == R.id.btnStats) {
            Intent intentDashboard = new Intent(MainActivity.this, CarStatistics.class);
            startActivity(intentDashboard);
        } else if (view.getId() == R.id.btnEngineFaults) {
            Intent intentDashboard = new Intent(MainActivity.this, EngineFaults.class);
            startActivity(intentDashboard);
        } else if (view.getId() == R.id.btnSettings) {
            Intent intentDashboard = new Intent(MainActivity.this, AppSettings.class);
            startActivity(intentDashboard);
        }
    }
}