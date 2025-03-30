package com.example.vmsobd2;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class MainActivity extends AppCompatActivity {
    private Bluetooth bluetooth;
    private TextView connectionStatus;
    private Button connectButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        //BLUETOOTH 1

        bluetooth = Bluetooth.getInstance(this);

        // inicializace views
        connectButton = findViewById(R.id.btnConnect);
        connectionStatus = findViewById(R.id.connection_status);

        // Connect when ready
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        String deviceAddress = prefs.getString("selected_device_address", null);
        bluetooth.handleConnectButton(connectButton, connectionStatus, deviceAddress);
        if (bluetooth.isConnected()) {
            bluetooth.updateStatusView('c', connectButton, connectionStatus);
        }
        //BLUETOOTH 2


        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }


    //odkaz na metodu v Bluetooth tride
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        bluetooth.handlePermissionsResult(requestCode, permissions, grantResults);
    }

    //zde se pres intent prechazi do jinych aktivit
    public void Transport(View view) {
        Intent intent = null;
        int viewId = view.getId();
        if (viewId == R.id.btnDashboard) {
            intent = new Intent(MainActivity.this, carDashboard.class);
        } else if (viewId == R.id.btnStats) {
            intent = new Intent(MainActivity.this, CarStatistics.class);
        } else if (viewId == R.id.btnEngineFaults) {
            intent = new Intent(MainActivity.this, EngineFaults.class);
        } else if (viewId == R.id.btnSettings) {
            intent = new Intent(MainActivity.this, AppSettings.class);
        } else {
            return;
        }
        if (intent != null) {
            startActivity(intent);
        }
    }
    //on resume tady nastavuje status dolniho toolbaru
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
    protected void onPause(){
        super.onPause();
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
    }
}