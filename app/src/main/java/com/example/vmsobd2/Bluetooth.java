package com.example.vmsobd2;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.widget.TextView;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;

//zaklad teto tridy jsem si nechal vygenerovat s prompty
//i want to my android app layout add something that looks like a toolbar to the bottom where it would show obd2 connection status and a button for the connection
// can i use another java file to handle the logic for connecting to the ob2 module?
//vyhodilo mi to trochu zvlastni kod ale po uprave funguje
//jeste mi to dalo kod v activity_main.xml oznaceny 1.1 a 1.2
//dal jsem dal prompt: can we handle all the communication logic with the obd2 scanner in this file because i have 5 different activities that i use
//a doplnilo mi to logiku ale se spousty chyb
public class Bluetooth {
    public static final int REQUEST_BLUETOOTH_CONNECT = 1;
    private Context context;
    private TextView connectionStatus;
    private BluetoothAdapter bluetoothAdapter;
    private BluetoothSocket bluetoothSocket;
    private InputStream inputStream;
    private OutputStream outputStream;
    private boolean isConnected = false;

    private static final UUID OBD2_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    public Bluetooth(Context context, TextView connectionStatus) {
        this.context = context;
        this.connectionStatus = connectionStatus;
        this.bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
    }

    public void connect(String deviceAddress) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions((Activity) context, new String[]{Manifest.permission.BLUETOOTH_CONNECT}, REQUEST_BLUETOOTH_CONNECT);
            return;
        }

        BluetoothDevice device = bluetoothAdapter.getRemoteDevice(deviceAddress);
        connectionStatus.setText("OBD2 Status: Connecting...");
        try {
            bluetoothSocket = device.createRfcommSocketToServiceRecord(OBD2_UUID);
            bluetoothSocket.connect();
            inputStream = bluetoothSocket.getInputStream();
            outputStream = bluetoothSocket.getOutputStream();
            isConnected = true;
            connectionStatus.setText("OBD2 Status: Connected");
        } catch (IOException e) {
            connectionStatus.setText("OBD2 Status: Connection Failed");
            e.printStackTrace();
        }
    }

    public void disconnect() {
        try {
            if (bluetoothSocket != null) {
                bluetoothSocket.close();
            }
            isConnected = false;
            connectionStatus.setText("OBD2 Status: Disconnected");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public boolean isConnected() {
        return isConnected;
    }

    public void sendCommand(String command) {
        if (isConnected && outputStream != null) {
            try {
                outputStream.write(command.getBytes());
                outputStream.flush();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public String readResponse() {
        if (isConnected && inputStream != null) {
            try {
                byte[] buffer = new byte[1024];
                int bytesRead = inputStream.read(buffer);
                return new String(buffer, 0, bytesRead);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return null;
    }
}