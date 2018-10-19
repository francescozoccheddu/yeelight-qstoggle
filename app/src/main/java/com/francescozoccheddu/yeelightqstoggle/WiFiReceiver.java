package com.francescozoccheddu.yeelightqstoggle;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.wifi.SupplicantState;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.util.Log;

public class WiFiReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d("WiFiReceiver", "Broadcast received");
        ToggleTileService.update(context);
    }

    public static boolean isConnected(Context context) {
        final WifiManager wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
        if (wifiManager != null && wifiManager.isWifiEnabled()) {
            final WifiInfo wifiInfo = wifiManager.getConnectionInfo();
            if (wifiInfo != null && wifiInfo.getNetworkId() != -1) {
                if (wifiInfo.getSupplicantState() == SupplicantState.COMPLETED) {
                    Settings settings = Settings.getGlobalSettings(context);
                    if (settings.isWiFiStatic()) {
                        final String ssid = SSIDUtils.getSSID(wifiInfo);
                        return ssid != null && ssid.equals(settings.getWiFiSSID());
                    } else {
                        return true;
                    }
                }
            }
        }
        return false;
    }

}
