package com.francescozoccheddu.yeelightqstoggle;

import android.content.Context;
import android.content.SharedPreferences;

public class Settings {

    public static final String DEFAULT_NAME = "Settings";

    private final SharedPreferences preferences;

    public class Editor {

        private final SharedPreferences.Editor editor;

        private Editor() {
            editor = preferences.edit();
        }

        public void apply() {
            editor.commit();
        }

        public void applyAsync() {
            editor.apply();
        }

        public void setBulbAddress(String address) {
            if (address == null) {
                address = "";
            }
            editor.putString("bulb_address", address);
        }

        public void setWiFiSSID(String ssid) {
            if (ssid == null) {
                ssid = "";
            }
            editor.putString("wifi_ssid", ssid);
        }

        public void setStaticBulb(boolean staticBulb) {
            editor.putBoolean("bulb_static", staticBulb);
        }

        public void setStaticWiFi(boolean staticWiFi) {
            editor.putBoolean("wifi_static", staticWiFi);
        }

    }

    public static Settings getGlobalSettings(Context context) {
        return new Settings(context, DEFAULT_NAME);
    }

    public Settings(Context context, String name) {
        preferences = context.getSharedPreferences(name, Context.MODE_PRIVATE);
    }

    public Editor edit() {
        return new Editor();
    }

    public String getBulbAddress() {
        if (isBulbStatic()) {
            final String address = preferences.getString("bulb_address", null);
            if (address == null) {
                preferences.edit().clear().apply();
                throw new IllegalStateException("Bad settings state");
            }
            else {
                return address;
            }
        } else {
            return null;
        }
    }

    public String getWiFiSSID() {
        if (isWiFiStatic()) {
            final String ssid = preferences.getString("wifi_ssid", null);
            if (ssid == null) {
                preferences.edit().clear().apply();
                throw new IllegalStateException("Bad settings state");
            }
            else {
                return ssid;
            }
        } else {
            return null;
        }
    }

    public boolean isBulbStatic() {
        return preferences.getBoolean("bulb_static", false);
    }

    public boolean isWiFiStatic() {
        return preferences.getBoolean("wifi_static", false);
    }

}
