package com.example.vmsobd2;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    public void Transport(View view) {
        if (view.getId() == R.id.btnDashboard) {
            Intent intentDashboard = new Intent(MainActivity.this, cardashboard.class);
            startActivity(intentDashboard);
        }else if (view.getId() == R.id.btnStats) {
            Intent intentDashboard = new Intent(MainActivity.this, CarStatistics.class);
            startActivity(intentDashboard);
        }else if (view.getId() == R.id.btnEngineFaults) {
            Intent intentDashboard = new Intent(MainActivity.this, EngineFaults.class);
            startActivity(intentDashboard);
        }else if (view.getId() == R.id.btnSettings) {
            Intent intentDashboard = new Intent(MainActivity.this, appSettings.class);
            startActivity(intentDashboard);
        }
    }
}