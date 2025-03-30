package com.example.vmsobd2;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.ref.WeakReference;
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
    private static Bluetooth instance;
    private Context context;
    public TextView connectionStatus;
    private Button connectButton;
    private BluetoothAdapter bluetoothAdapter;
    private BluetoothSocket bluetoothSocket;
    private InputStream inputStream;
    private OutputStream outputStream;
    private boolean isConnected = false;

    private static final UUID OBD2_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    public static final int REQUEST_BLUETOOTH_CONNECT = 1;
    private WeakReference<Activity> activityRef;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    // private konstruktor
    private Bluetooth(Context context) {
        this.context = context.getApplicationContext();
        this.bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
    }

    // singleton instance getter
    public static synchronized Bluetooth getInstance(Context context) {
        if (instance == null) {
            instance = new Bluetooth(context);
        }
        // ulozeni kontextu aplikace pokud je dostupny
        if (context instanceof Activity) {
            instance.activityRef = new WeakReference<>((Activity) context);
        }
        return instance;
    }
    private void runOnUiThread(Runnable action) {
        Activity activity = activityRef.get();
        if (activity != null && !activity.isFinishing()) {
            activity.runOnUiThread(action);
        } else {
            mainHandler.post(action);
        }
    }

        //tado metoda upravuje UI na aktivite
    public void updateStatusView(char status, Button connectButton, TextView connectionStatus) {
        mainHandler.post(() -> {
            if (connectionStatus != null) {
                switch (status) {
                    case 'c': // connected
                        connectionStatus.setText(context.getString(R.string.bt_conn));
                        connectButton.setText(context.getString(R.string.disconnect));
                        break;
                    case 'd': // disconnected
                        connectionStatus.setText(context.getString(R.string.bt_disconn));
                        connectButton.setText(context.getString(R.string.connect));
                        break;
                    case 'f': // failed
                        connectionStatus.setText(context.getString(R.string.bt_conn_fail));
                        connectButton.setText(context.getString(R.string.connect));
                        break;
                    case 't': // connecting
                        connectionStatus.setText(context.getString(R.string.bt_conntg));
                        connectButton.setText(context.getString(R.string.connect));
                        break;
                    case 'n': // device adress je null
                        connectionStatus.setText(context.getString(R.string.bt_nodevice));
                        connectButton.setText(context.getString(R.string.connect));
                        showToast(context.getString(R.string.toast_nodevice));
                        break;
                }
            }
        });
    }


    public void handlePermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == REQUEST_BLUETOOTH_CONNECT) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
                String deviceAddress = preferences.getString("selected_device_address", null);
                if (deviceAddress != null) {
                    //connect(deviceAddress);
                } else {
                    showStatusMessage(context.getString(R.string.bt_nodevice));
                    showToast(context.getString(R.string.toast_nodevice));
                }
            } else {
                showStatusMessage(context.getString(R.string.bt_denied));
                showToast(context.getString(R.string.allow_in_settings));
            }
        }
    }

    public void connect(Button connectButton, TextView connectionStatus, String deviceAddress) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT)
                != PackageManager.PERMISSION_GRANTED) {

            Activity activity = activityRef.get();
            if (activity != null && !activity.isFinishing()) {
                ActivityCompat.requestPermissions(activity,
                        new String[]{Manifest.permission.BLUETOOTH_CONNECT},
                        REQUEST_BLUETOOTH_CONNECT);
            }
            return;
        }

        new Thread(() -> {
            try {
                runOnUiThread(() -> connectionStatus.setText(context.getString(R.string.bt_conntg)));

                BluetoothDevice device = bluetoothAdapter.getRemoteDevice(deviceAddress);
                bluetoothSocket = device.createRfcommSocketToServiceRecord(OBD2_UUID);
                bluetoothSocket.connect();
                inputStream = bluetoothSocket.getInputStream();
                outputStream = bluetoothSocket.getOutputStream();
                Thread.sleep(1500);
                isConnected = true;

                runOnUiThread(() -> updateStatusView('c', connectButton, connectionStatus));
                initELM327();
            } catch (Exception e) {
                isConnected = false;
                if (deviceAddress==null){
                    runOnUiThread(() -> updateStatusView('n', connectButton, connectionStatus));
                }else {
                    runOnUiThread(() -> updateStatusView('f', connectButton, connectionStatus));
                }e.printStackTrace();
            }
        }).start();
    }

    public void disconnect(Button connectButton, TextView connectionStatus) {
        new Thread(() -> {
            showStatusMessage(context.getString(R.string.bt_disconntg));
            try {
                if (bluetoothSocket != null) {
                    bluetoothSocket.close();
                }
                isConnected = false;
                updateStatusView('d',connectButton,connectionStatus);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();
    }

    private void showStatusMessage(String message) {
        if (connectionStatus != null) {
            ((Activity) connectionStatus.getContext()).runOnUiThread(() ->
                    connectionStatus.setText(message));
        }
    }

    private void showToast(String message) {
        Toast.makeText(context, message, Toast.LENGTH_LONG).show();
    }

    public boolean isConnected() {
        return isConnected;
    }

    public void handleConnectButton(Button connectButton,TextView connectionStatus, String deviceAddress) {
        connectButton.setOnClickListener(view -> {
            if (!isConnected()) {
                connect(connectButton,connectionStatus,deviceAddress);
            } else {
                disconnect(connectButton,connectionStatus);
            }
        });
    }

    public void sendCommand(String command) throws IOException {
        if (isConnected && outputStream != null) {
            clearInputBuffer();
            command = command.trim() + "\r";
            outputStream.write(command.getBytes(StandardCharsets.UTF_8));
            outputStream.flush();
        }
    }

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

    public String sendObdCommand(String command) {
        try {
            sendCommand(command);
            Thread.sleep(10); //delay
            return readResponse();
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
            return null;
        }
    }

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