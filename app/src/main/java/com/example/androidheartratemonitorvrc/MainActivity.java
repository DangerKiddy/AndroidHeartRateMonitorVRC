package com.example.androidheartratemonitorvrc;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Bundle;

import com.illposed.osc.*;
import com.illposed.osc.OSCMessage;
import com.illposed.osc.argument.ArgumentHandler;
import com.illposed.osc.argument.handler.Activator;
import com.illposed.osc.transport.OSCPortIn;
import com.illposed.osc.transport.OSCPortOut;

import androidx.activity.EdgeToEdge;
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
import java.util.UUID;

@SuppressLint("MissingPermission")
public class MainActivity extends AppCompatActivity {
    public static MainActivity instance;

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

    private static final long SCAN_PERIOD = 10000;
    private boolean isScanning = false;
    private int lastSentBpm = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        instance = this;

        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
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
        statusText.setText("Press scan button to connect device");

        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
            statusText.setText("No Bluetooth permission!");

            return;
        }

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

    public void startScan() {
        if (isScanning)
            return;

        deviceList.clear();
        layout.removeAllViews();

        handler.postDelayed(() -> {
            isScanning = false;
            scanBtn.setActivated(true);

            bluetoothLeScanner.stopScan(leScanCallback);
            updateLayoutWithDevices();
        }, SCAN_PERIOD);

        isScanning = true;
        bluetoothLeScanner.startScan(leScanCallback);
    }

    private void updateLayoutWithDevices() {
        statusText.setText("Found " + deviceList.size() + " device(s)");

        for (BluetoothDevice device : deviceList) {
            Button deviceButton = new Button(this);
            deviceButton.setText(device.getName() != null ? device.getName() : device.getAddress());

            deviceButton.setOnClickListener(view -> {
                Log.d("BLE Scan", "Connecting to " + deviceButton.getText());

                ConnectToDevice(device);
            });

            layout.addView(deviceButton);
        }
    }

    private void ConnectToDevice(BluetoothDevice device)
    {
        BluetoothGatt bluetoothGatt = device.connectGatt(this, false, new BluetoothGattCallback() {
            @Override
            public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
                if (newState == BluetoothProfile.STATE_CONNECTED) {
                    Log.d("BLE", "Connected to GATT server.");
                    gatt.discoverServices();
                } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                    Log.d("BLE", "Disconnected from GATT server.");
                }
            }

            @Override
            public void onServicesDiscovered(BluetoothGatt gatt, int status) {
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    BluetoothGattService heartRateService = gatt.getService(UUID.fromString("0000180d-0000-1000-8000-00805f9b34fb")); // Heart Rate Service
                    if (heartRateService != null) {
                        BluetoothGattCharacteristic heartRateCharacteristic = heartRateService.getCharacteristic(UUID.fromString("00002a37-0000-1000-8000-00805f9b34fb"));

                        gatt.setCharacteristicNotification(heartRateCharacteristic, true);

                        BluetoothGattDescriptor descriptor = heartRateCharacteristic.getDescriptor(UUID.fromString("00002902-0000-1000-8000-00805f9b34fb"));
                        descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                        gatt.writeDescriptor(descriptor);
                    }
                }
            }

            @Override
            public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
                if (UUID.fromString("00002a37-0000-1000-8000-00805f9b34fb").equals(characteristic.getUuid())) {
                    int flag = characteristic.getProperties();
                    int format = (flag & 0x01) != 0 ? BluetoothGattCharacteristic.FORMAT_UINT16 : BluetoothGattCharacteristic.FORMAT_UINT8;
                    int heartRate = characteristic.getIntValue(format, 1);
                    Log.d("BLE", "Heart Rate: " + heartRate);

                    bpmText.setText("BPM: " + heartRate);

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
            }
        });
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

    public static void SetStatusText(String text)
    {
        instance.statusText.setText(text);
    }

    public static void SetOSCController(String ip)
    {
        instance.oscController = new OSCController(ip, GetOSCSerializer());
        instance.oscController.SetPort(28013);

        instance.oscController.CreateSender();
    }
    public static OSCSerializerAndParserBuilder GetOSCSerializer()
    {
        return instance.oscSerializer;
    }
}