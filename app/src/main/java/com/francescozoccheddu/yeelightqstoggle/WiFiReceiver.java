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

    public static WiFiHomeState getHomeState (WifiInfo wifiInfo, String homeSSID) {
        if (wifiInfo != null) {
            if (wifiInfo.getSupplicantState() == SupplicantState.COMPLETED) {
                final String ssid = SSIDUtils.getSSID(wifiInfo);
                if (ssid != null && ssid.equals(homeSSID)) {
                    return WiFiHomeState.HOME;
                }
            }
            return WiFiHomeState.NOT_HOME;
        } else {
            return WiFiHomeState.UNKNOWN;
        }
    }

    public static WiFiHomeState getHomeState(Context context, String homeSSID) {
        final WifiManager wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
        if (wifiManager != null) {
            final WifiInfo wifiInfo = wifiManager.getConnectionInfo();
            return getHomeState(wifiInfo, homeSSID);
        } else {
            return WiFiHomeState.UNKNOWN;
        }
    }

    public enum WiFiHomeState {
        HOME, NOT_HOME, UNKNOWN
    }

}
