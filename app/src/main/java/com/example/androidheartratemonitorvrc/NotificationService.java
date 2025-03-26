package com.example.androidheartratemonitorvrc;

import static android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

import androidx.core.app.NotificationCompat;

import org.apache.log4j.chainsaw.Main;

import java.util.UUID;

@SuppressLint("MissingPermission")
public class NotificationService extends Service {
    private static final int NOTIFICATION_ID = 1;
    private static final String CHANNEL_ID = "HRT_NTF_CHNL";
    private NotificationManager notificationManager;

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();

        Notification notify = createNotification(0);

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            startForeground(NOTIFICATION_ID, notify);
        } else {
            startForeground(NOTIFICATION_ID, notify, FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE);
        }

        Log.i("HRTrack", "Connecting...");

        BluetoothGatt bluetoothGatt = MainActivity.instance.ConnectedDevice.connectGatt(this, false, new BluetoothGattCallback() {
            @Override
            public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
                if (newState == BluetoothProfile.STATE_CONNECTED) {
                    Log.i("BLE", "Connected to GATT server.");
                    gatt.discoverServices();
                } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                    Log.i("BLE", "Disconnected from GATT server.");
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

                    MainActivity.instance.OnBPMReceived(heartRate);
                    updateNotification(heartRate);
                }
            }
        });

    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        notificationManager.cancelAll();

        super.onDestroy();
    }

    public void updateNotification(int heartRate) {
        Notification notification = createNotification(heartRate);
        notificationManager.notify(NOTIFICATION_ID, notification);
    }

    private Notification createNotification(int heartRate) {
        String contentText = MainActivity.routerName.isEmpty() ? "Waiting for connection" : ("Connected to: " + MainActivity.routerName);

        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setAutoCancel(false)
                .setOnlyAlertOnce(true)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setOngoing(true)

                .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setCategory(NotificationCompat.CATEGORY_SERVICE)
                .setWhen(0)
                .setShowWhen(false)

                .setContentTitle("HeartRate")
                .setContentText(contentText)
                .setSubText("BPM: " + heartRate)
                .setBadgeIconType(NotificationCompat.BADGE_ICON_NONE)

                .setSmallIcon(R.drawable.ic_notification)

                .build();
    }

    private void createNotificationChannel() {
        notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                "Status and BPM",
                NotificationManager.IMPORTANCE_DEFAULT
        );
        channel.setSound(null, null);
        channel.setVibrationPattern(null);
        channel.setDescription("Displays current BPM and status connection");
        channel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
        channel.enableLights(false);
        channel.setShowBadge(false);
        channel.enableVibration(false);

        notificationManager.createNotificationChannel(channel);
    }
}