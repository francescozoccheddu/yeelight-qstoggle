package com.francescozoccheddu.yeelightqstoggle;

import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;

public final class SSIDUtils {

    private SSIDUtils() {}

    public static String getSSID(String rawSSID) {
        if (rawSSID != null && !rawSSID.isEmpty() && rawSSID.startsWith("\"") && rawSSID.endsWith("\"")) {
            return rawSSID.substring(1, rawSSID.length() - 1);
        }
        return null;
    }

    public static String getSSID(WifiConfiguration wifi) {
        return getSSID(wifi.SSID);
    }

    public static String getSSID(WifiInfo wifi) {
        return getSSID(wifi.getSSID());
    }

}
