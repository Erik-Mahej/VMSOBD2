package com.example.vmsobd2;

import android.content.Intent;
import android.os.Bundle;
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
    private Bluetooth ConnectionManager;
    private TextView connectionStatus;
    private Button btnConnect;
    //1.1

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        //1.2
        connectionStatus = findViewById(R.id.connection_status);
        btnConnect = findViewById(R.id.btnConnect);

        ConnectionManager = new Bluetooth(this, connectionStatus);

        btnConnect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ConnectionManager.connect();
            }
        });
        //1.2

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
        }else if (view.getId() == R.id.btnStats) {
            Intent intentDashboard = new Intent(MainActivity.this, CarStatistics.class);
            startActivity(intentDashboard);
        }else if (view.getId() == R.id.btnEngineFaults) {
            Intent intentDashboard = new Intent(MainActivity.this, EngineFaults.class);
            startActivity(intentDashboard);
        }else if (view.getId() == R.id.btnSettings) {
            Intent intentDashboard = new Intent(MainActivity.this, AppSettings.class);
            startActivity(intentDashboard);
        }
    }
}