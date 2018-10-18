package com.francescozoccheddu.yeelightqstoggle;

import android.content.Context;
import android.content.SharedPreferences;

public class Settings {

    public static final String DEFAULT_NAME = "Settings";

    private final SharedPreferences preferences;
    private final SharedPreferences.Editor preferencesEditor;

    public Settings(Context context, String name) {
        preferences = context.getSharedPreferences(name, Context.MODE_PRIVATE);
        this.preferencesEditor = preferences.edit();
    }

    public Bulb getStaticBulb() {
        if (preferences.contains("bulb_address")) {
            return Bulb.fromAddress(preferences.getString("bulb_address", null));
        }
        return null;
    }

    public String getWiFiSSID() {
        return preferences.getString("wifi_ssid", null);
    }

    public void setWiFiSSID(String ssid) {
        preferencesEditor.putString("wifi_ssid", ssid);
    }

    public void setDynamicBulb() {
        preferencesEditor.remove("bulb_address");
    }

    public void setStaticBulb(Bulb bulb) {
        preferencesEditor.putString("bulb_address", bulb.getAddress());
    }

    public void save() {
        preferencesEditor.commit();
    }

    public void saveAsync() {
        preferencesEditor.apply();
    }

}
