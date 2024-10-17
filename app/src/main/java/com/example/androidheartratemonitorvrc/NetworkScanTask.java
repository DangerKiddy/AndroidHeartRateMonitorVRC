package com.example.androidheartratemonitorvrc;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import java.lang.ref.WeakReference;
import java.net.InetAddress;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.text.format.Formatter;

import com.illposed.osc.*;

class NetworkScanTask extends AsyncTask<Void, Void, Void> {
    private WeakReference<Context> mContextRef;
    private Context context;
    private OSCSerializerAndParserBuilder serializer;

    public NetworkScanTask(Context context) {
        mContextRef = new WeakReference<Context>(context);

        Log.d("NetworkScan", "Created task");
    }
    private static Map<String, String> deviceNetworkNames = new HashMap<String, String>();
    private static Map<String, OSCController> oscTestConnections = new HashMap<String, OSCController>();
    public static String GetNetworkName(String ip)
    {
        String name = ip;

        if (deviceNetworkNames.containsKey(ip))
            name = deviceNetworkNames.get(ip);

        return name;
    }

    public static void CloseAllTestConnection()
    {
        for (Map.Entry<String, OSCController> entry : oscTestConnections.entrySet()) {
            OSCController connection = entry.getValue();

            if (connection != null)
                connection.Close();
        }

        oscTestConnections.clear();
    }

    @Override
    protected Void doInBackground(Void... voids) {
        context = mContextRef.get();

        ScanNetwork();
        if (context != null) {
        }

        return null;
    }

    private void ScanNetwork()
    {
        Log.d("NetworkScan", "Scanning network...");
        try {
            serializer = MainActivity.GetOSCSerializer();
            WifiManager wm = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);

            WifiInfo connectionInfo = wm.getConnectionInfo();
            int ipAddress = connectionInfo.getIpAddress();
            String ipString = Formatter.formatIpAddress(ipAddress);

            String prefix = ipString.substring(0, ipString.lastIndexOf(".") + 1);

            MainActivity.SetStatusText("Searching device...");
            for (int i = 1; i < 255; i++) {
                String testIp = prefix + i;

                InetAddress address = InetAddress.getByName(testIp);
                String hostName = address.getCanonicalHostName();

                if (!testIp.equals(ipString)) {
                    Log.i("NetworkScan", "Trying access " + hostName + "(" + (testIp) + ")");

                    deviceNetworkNames.put(testIp, hostName);

                    MainActivity.SetStatusText("Searching device " + i + "/255..." + "\nTrying access " + hostName + "(" + (testIp) + ")");

                    OSCController testConnection = new OSCController(testIp, serializer);
                    oscTestConnections.put(testIp, testConnection);

                    testConnection.TryConnect();
                } else
                    Log.w("NetworkScan", "Skipping our ip");
            }
        } catch (Exception e)
        {

            e.printStackTrace();
        }
    }
}