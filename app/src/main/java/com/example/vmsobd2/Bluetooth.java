package com.example.vmsobd2;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.preference.PreferenceManager;
import android.widget.Button;
import android.widget.TextView;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

//zaklad teto tridy jsem si nechal vygenerovat s prompty
//i want to my android app layout add something that looks like a toolbar to the bottom where it would show obd2 connection status and a button for the connection
// can i use another java file to handle the logic for connecting to the ob2 module?
//vyhodilo mi to trochu zvlastni kod ale po uprave funguje
//dal jsem dal prompt: can we handle all the communication logic with the obd2 scanner in this file because i have 5 different activities that i use
//a doplnilo mi to logiku ale se spousty chyb
//tuto tridu jsem nadale upravoval az do bodu kde z originalniho kodu zustavaji jen nazvy metod a promenych

public class Bluetooth {
    public static final int REQUEST_BLUETOOTH_CONNECT = 1;
    //private static Bluetooth instance;
    private Context context;
    private TextView connectionStatus;
    private BluetoothAdapter bluetoothAdapter;
    private BluetoothSocket bluetoothSocket;
    private InputStream inputStream;
    private OutputStream outputStream;
    private boolean isConnected = false;

    private static final UUID OBD2_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    // Konstruktor tridy
    public Bluetooth(Context context, TextView connectionStatus) {
        this.context = context;
        this.connectionStatus = connectionStatus;
        this.bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
    }

    // Pokus o jednotne pripojeni k bluetooth
    /*
    public static Bluetooth getInstance(Context context, TextView connectionStatus) {
        if (instance == null) {
            instance = new Bluetooth(context, connectionStatus);
        }
        return instance;
    }

     */
    //tato metoda zajistuje aby aplikace nespadla v pripade ze neni povelene bluetooth nebo neni zvolene zarizeni
    public void handlePermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == REQUEST_BLUETOOTH_CONNECT) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted
                SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
                String deviceAddress = preferences.getString("selected_device_address", null);
                if (deviceAddress != null) {
                    connect(deviceAddress);
                    if (isConnected) {
                        // Tady jeste patri upravovani tlacitka

                    }
                } else {
                    connectionStatus.setText("OBD2 Status: No Device Selected");
                    Toast.makeText(context, "No device selected. Please select a device in the settings.", Toast.LENGTH_LONG).show();
                }
            } else {
                // Permission denied
                connectionStatus.setText("OBD2 Status: Permission Denied");
                Toast.makeText(context, "Bluetooth permission denied. Allow it in the application settings.", Toast.LENGTH_LONG).show();
            }
        }
    }

    // Pripojeni k zarizeni
    public void connect(String deviceAddress) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions((Activity) context, new String[]{Manifest.permission.BLUETOOTH_CONNECT}, 1);
            return;
        }

        isConnected = false;

        new Thread(() -> {
            try {
                ((Activity) context).runOnUiThread(() -> connectionStatus.setText("OBD2 Status: Connecting..."));
                BluetoothDevice device = bluetoothAdapter.getRemoteDevice(deviceAddress);
                bluetoothSocket = device.createRfcommSocketToServiceRecord(OBD2_UUID);
                bluetoothSocket.connect();
                inputStream = bluetoothSocket.getInputStream();
                outputStream = bluetoothSocket.getOutputStream();
                isConnected = true;


                if (connectionStatus != null) {
                    ((Activity) context).runOnUiThread(() -> connectionStatus.setText("OBD2 Status: Connected"));
                }

                initELM327(); //inicializace ELM327 komunikace
            } catch (IOException e) {
                isConnected = false;
                if (connectionStatus != null) {
                    ((Activity) context).runOnUiThread(() -> connectionStatus.setText("OBD2 Status: Connection Failed"));
                }
                e.printStackTrace();
            }
        }).start();
    }


    // Odpojeni od zarizeni
    public void disconnect() {
        new Thread(() -> {
            if (connectionStatus != null) {
                connectionStatus.setText("OBD2 Status: Disconnecting...");
            }
            try {
                if (bluetoothSocket != null) {
                    bluetoothSocket.close();
                }
                isConnected = false;
                if (connectionStatus != null) {
                    connectionStatus.setText("OBD2 Status: Disconnected");
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();
    }

    // Kontrola pripojeni bluetooth
    public boolean isConnected() {
        return isConnected;
    }

    // Pouzivani buttonu na pripojeni/odpojeni od bluetooth zarizeni
    public void handleConnectButton(Button connectButton, String deviceAddress) {
        connectButton.setOnClickListener(view -> {
            if (!isConnected()) {
                connect(deviceAddress);
                connectButton.setText("Disconnect");
                connectionStatus.setText("OBD2 Status: Connected");
            } else {
                disconnect();
                connectButton.setText("Connect");
                connectionStatus.setText("OBD2 Status: Disconnected");
            }
        });
    }


    //Posilani prikazu OB2 zarizeni
    public void sendCommand(String command) throws IOException {
        if (isConnected && outputStream != null) {
            clearInputBuffer();
            command = command.trim() + "\r";
            outputStream.write(command.getBytes(StandardCharsets.UTF_8));
            outputStream.flush();
        }
    }

    // Cteni odpoveni od zarizeni
    public String readResponse() throws IOException {
        if (isConnected && inputStream != null) {
            StringBuilder response = new StringBuilder();
            int c;
            while ((c = inputStream.read()) != -1) {
                if ((char) c == '>') break;
                response.append((char) c);
            }
            return cleanResponse(response.toString());
        }
        return null;
    }

    // Kombinace metod pro posilani a cteni komunikace
    public String sendObdCommand(String command) {
        try {
            sendCommand(command);
            Thread.sleep(200); //delay
            return readResponse();
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
            return null;
        }
    }

    // Tato metoda pomaha cistit input
    private void clearInputBuffer() throws IOException {
        while (inputStream.available() > 0) {
            inputStream.read();
        }
    }
    private String cleanResponse(String raw) {
        return raw.replaceAll("[\r\n ]", "");
    }

    // Tyto prikazy jsou dulezite pro spravne nastaveni komunikace se zarizeni
    private void initELM327() {
        sendObdCommand("ATZ");    // Reset
        sendObdCommand("ATE0");   // Echo Off
        sendObdCommand("ATL0");   // Linefeeds Off
        sendObdCommand("ATS0");   // Spaces Off
        sendObdCommand("ATH1");   // Headers On (optional)
    }
}
