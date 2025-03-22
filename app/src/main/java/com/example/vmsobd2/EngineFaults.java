package com.example.vmsobd2;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
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
    private Button connectButton;
    private GridLayout gridLayout;
    private int cardCount = 0;
    private String faults[]  = {"nevim"};
    private static boolean going = false;
    private static boolean BTCN = false;

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
        connectionStatus = findViewById(R.id.connection_status);
        connectButton = findViewById(R.id.btnConnect);

        connectButton.setOnClickListener(v -> handleConnectButton());

        bluetooth = new Bluetooth(this, connectionStatus);

        gridLayout = findViewById(R.id.gridLayout);

        Intent intent = getIntent();
        boolean BTCN = intent.getBooleanExtra("BTCN", false);

        if (BTCN) {
            handleConnectButton();
        }
    }

    public void goBack(View view) {
        if (view.getId() == R.id.btnBack) {
            Intent intentMain = new Intent(EngineFaults.this, MainActivity.class);
            startActivity(intentMain);
        }
        finish();
    }
    public void onClick(View v) {
        addCardView("Card " + (++cardCount));
    }

    public void run() {
        if (bluetooth.isConnected()) {
            for (String e : faults){
                requestEngineFaults(e);
            }
        }else{
            //bt neni connected
        }
    }

    private void requestEngineFaults(String fault) {
        //tady se posila command na rpm
        String command = fault; //tady se definuje PID
       // bluetooth.sendCommand(command);
        //String response = bluetooth.readResponse();
        //int decodedResponse = decodeResponse(response);
        //String finalFault = assignFault(decodedResponse);
        //addCardView(finalFault);
    }
    public String assignFault(int respoceCode){

        return null;
    }
    public static int decodeResponse(String response) {
        String[] parts = response.split(" ");


        if (parts.length < 5) {
            Log.e("carDashboard", "Invalid response format. Expected at least 5 parts. Response: " + response);
            throw new IllegalArgumentException("Invalid response format. Expected at least 5 parts.");
        }

        // Extract the RPM data (the next two bytes)
        String rpmHex1 = parts[3]; // First byte
        String rpmHex2 = parts[4]; // Second byte

        // Combine the two bytes into a single hex string
        String combinedHex = (rpmHex1 + rpmHex2).trim();

        return Integer.parseInt(combinedHex, 16);
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
                Toast.makeText(this, "No device selected. Please select a device in the settings.", Toast.LENGTH_LONG).show();
            }
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
        textView.setTextColor(Color.parseColor("#0D0D0D")); //barva textu
        textView.setTextSize(18); //velikost textu
        textView.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));
        textView.setPadding(0, 10, 0, 10); //margin nahore

        innerLayout.addView(textView);

        cardView.addView(innerLayout);

        gridLayout.addView(cardView);
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
        bluetooth.disconnect(true);
    }
    @Override
    protected void onPause() {
        super.onPause();
        bluetooth.disconnect(true);
    }

}