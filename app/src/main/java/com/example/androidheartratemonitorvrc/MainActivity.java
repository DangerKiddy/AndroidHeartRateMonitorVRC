package com.example.androidheartratemonitorvrc;

import android.Manifest;
import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;

import com.illposed.osc.*;
import com.illposed.osc.argument.ArgumentHandler;
import com.illposed.osc.argument.handler.Activator;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import android.os.Handler;
import android.util.Log;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

@SuppressLint("MissingPermission")
public class MainActivity extends AppCompatActivity {
    public static MainActivity instance;

    public static String routerName = "";
    public BluetoothDevice ConnectedDevice;
    private Intent notificationService;

    private List<BluetoothDevice> deviceList = new ArrayList<>();

    private BluetoothAdapter bluetoothAdapter;
    private BluetoothLeScanner bluetoothLeScanner;
    private ScanCallback leScanCallback;
    private boolean isLookingForDevice = false;
    private OSCController oscController = null;
    private OSCSerializerAndParserBuilder oscSerializer;

    private Handler handler = new Handler();
    private LinearLayout layout;
    private Button scanBtn;
    private TextView statusText;
    private TextView bpmText;

    private static final long SCAN_PERIOD = 20000;
    private boolean isScanning = false;
    private int lastSentBpm = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        instance = this;

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        MainActivity activity = this;
        layout = (LinearLayout)findViewById(R.id.DeviceLayout);
        bpmText = (TextView) findViewById(R.id.BPMText);

        statusText = (TextView) findViewById(R.id.StatusText);
        statusText.setText("");

        TryInitBluetooth();
    }

    public void TryInitBluetooth()
    {
        Log.i("HRTrack", "Trying to init Bluetooth");
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
            Log.i("HRTrack", "No permission for bluetooth, requesting");

            statusText.setText("No permission!");

            ActivityCompat.requestPermissions(MainActivity.this,
                    new String[]{
                            Manifest.permission.BLUETOOTH_SCAN,
                            Manifest.permission.BLUETOOTH_CONNECT,
                            Manifest.permission.POST_NOTIFICATIONS,
                            Manifest.permission.ACCESS_NETWORK_STATE,
                            Manifest.permission.ACCESS_WIFI_STATE,
                            Manifest.permission.ACCESS_NETWORK_STATE,
                            Manifest.permission.ACCESS_FINE_LOCATION,
                    }, 1);

            return;
        }
        statusText.setText("Press scan button to search for pulseoximeter");

        SetupOSCSerializer();

        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        bluetoothLeScanner = bluetoothAdapter.getBluetoothLeScanner();

        leScanCallback = new ScanCallback() {
            @Override
            public void onScanResult(int callbackType, ScanResult result) {
                BluetoothDevice device = result.getDevice();

                if (!deviceList.contains(device)) {
                    deviceList.add(device);
                    Log.d("BLE", "Device found: " + device.getName() + ", " + device.getAddress());

                    Button deviceButton = new Button(MainActivity.this);
                    deviceButton.setText(device.getName() != null ? device.getName() : device.getAddress());

                    deviceButton.setOnClickListener(view -> {
                        Log.d("BLE Scan", "Connecting to " + deviceButton.getText());

                        ConnectToDevice(device);
                    });

                    layout.addView(deviceButton);
                }
            }
            @Override
            public void onScanFailed(int errorCode) {
                Log.e("BLE Scan", "Scan failed with error: " + errorCode);
            }
        };

        scanBtn = (Button)findViewById(R.id.ScanButton);
        scanBtn.setOnClickListener(view -> {
            statusText.setText("Scanning...");
            startScan();

            scanBtn.setActivated(false);
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == 1) {
            if (grantResults.length > 0)

            if (grantResults.length > 0
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                TryInitBluetooth();
            }
        }
    }

    public void startScan() {
        if (isScanning)
            return;

        deviceList.clear();
        layout.removeAllViews();

        handler.postDelayed(() -> {
            isScanning = false;
            scanBtn.setActivated(true);

            bluetoothLeScanner.stopScan(leScanCallback);
        }, SCAN_PERIOD);

        isScanning = true;
        bluetoothLeScanner.startScan(leScanCallback);
    }

    private void ConnectToDevice(BluetoothDevice device)
    {
        ConnectedDevice = device;

        InitNotificationManager();
    }

    /// Used to keep BPM tracker alive even when phone is in sleep mode (screen locked)
    private void InitNotificationManager()
    {
        notificationService = new Intent(this, NotificationService.class);
        startForegroundService(notificationService);
    }

    public void OnBPMReceived(int heartRate)
    {
        Log.d("BLE", "Heart Rate: " + heartRate);

        StringBuilder zeros = new StringBuilder();
        String bpmStr = heartRate + "";
        for (int i = 0; i < 3 - bpmStr.length(); i++)
        {
            zeros.append('0');
        }

        bpmText.setText(zeros.toString() + heartRate);

        if (!isLookingForDevice)
        {
            isLookingForDevice = true;
            LookForRouter();
        }

        if (oscController != null && heartRate != lastSentBpm)
        {
            lastSentBpm = heartRate;

            oscController.Send("/heartrate", heartRate);
        }
    }

    private void SetupOSCSerializer()
    {
        oscSerializer = new OSCSerializerAndParserBuilder();
        oscSerializer.setUsingDefaultHandlers(false);

        List<ArgumentHandler> defaultParserTypes = Activator.createSerializerTypes();
        defaultParserTypes.remove(16);

        char typeChar = 'a';
        for (ArgumentHandler argumentHandler:defaultParserTypes) {
            oscSerializer.registerArgumentHandler(argumentHandler, typeChar);
            typeChar++;
        }
    }

    private void LookForRouter() {
        Log.d("BLE", "Looking for router app");

        Context context = instance.getApplicationContext();
        new NetworkScanTask(context).execute();
    }

    public void FailedSearchingRouter()
    {
        statusText.setText("Failed to find PC! Retrying in 5 seconds");

        handler.postDelayed(() -> {
            if (oscController == null)
                isLookingForDevice = false;

        }, 5000);
    }

    public static void SetStatusText(String text)
    {
        instance.statusText.setText(text);
    }

    public static void SetOSCController(String ip, String name)
    {
        instance.oscController = new OSCController(ip, GetOSCSerializer());
        instance.oscController.SetPort(28013);

        instance.oscController.CreateSender();

        String finalName = name;
        if (name.isEmpty())
            finalName = ip;

        routerName = finalName;
    }

    public static OSCSerializerAndParserBuilder GetOSCSerializer()
    {
        return instance.oscSerializer;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (notificationService != null)
            stopService(notificationService);
    }
}