package com.francescozoccheddu.yeelightqstoggle;

import android.content.Context;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.net.InetAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

public class SettingsActivity extends AppCompatActivity {

    private CheckBox cb_BulbStatic;
    private AutoCompleteTextView actvWiFiSSID;
    private AutoCompleteTextView actvBulbAddress;
    private TextView tvBulbDynamicInfo;
    private LinearLayout tilBulbAddress;

    private ArrayAdapter<String> bulbsAdapter;
    private HashSet<Bulb> bulbSet;
    private Bulb.Discoverer bulbDiscoverer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        cb_BulbStatic = findViewById(R.id.sets_cb_bulb_static);
        actvWiFiSSID = findViewById(R.id.sets_actv_wifi_ssid);
        actvBulbAddress = findViewById(R.id.sets_actv_bulb_address);
        tvBulbDynamicInfo = findViewById(R.id.sets_tv_bulb_dynamic_info);
        tilBulbAddress = findViewById(R.id.sets_til_bulb_address);

        cb_BulbStatic.setOnCheckedChangeListener((view, isChecked) -> {
            tvBulbDynamicInfo.setVisibility(isChecked ? View.GONE : View.VISIBLE);
            tilBulbAddress.setVisibility(isChecked ? View.VISIBLE : View.GONE);
        });

        actvWiFiSSID.setOnFocusChangeListener((view, hasFocus) -> {
            if (hasFocus) {
                actvWiFiSSID.setAdapter(getWiFiNetworksAdapter());
                actvWiFiSSID.showDropDown();
            }
        });

        actvWiFiSSID.setOnClickListener((view) -> {
            actvWiFiSSID.showDropDown();
        });

        bulbsAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, new ArrayList<>());
        bulbSet = new HashSet<>();

        actvBulbAddress.setAdapter(bulbsAdapter);

        actvBulbAddress.setOnFocusChangeListener((view, hasFocus) -> {
            if (hasFocus) {
                runBulbDiscoverer();
                actvBulbAddress.showDropDown();
            }
        });

        actvBulbAddress.setOnClickListener((view) -> {
            runBulbDiscoverer();
            actvBulbAddress.showDropDown();
        });

    }

    private void runBulbDiscoverer() {
        if (bulbDiscoverer == null || !bulbDiscoverer.isSearching()) {
            Log.d("SettingsActivity", "Creating new bulb discoverer");
            bulbDiscoverer = new Bulb.Discoverer(10000) {

                @Override
                public void onDiscover(Bulb bulb) {
                    Log.d("SettingsActivity", "Discovered bulb '" + bulb.toString() + "'");
                    if (bulb.hasAddress() && bulbSet.add(bulb)) {
                        bulbsAdapter.add(bulb.toString());
                        if (actvBulbAddress.hasFocus()) {
                            actvBulbAddress.showDropDown();
                        }
                        Log.d("SettingsActivity", "Bulb added");
                    }
                }
            };
        }
    }

    private ArrayAdapter<String> getWiFiNetworksAdapter() {
        Log.d("SettingsActivity", "Getting configured wifi networks");
        WifiManager wifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);
        if (wifiManager != null) {
            List<WifiConfiguration> wifis = wifiManager.getConfiguredNetworks();
            String[] ssids = wifis.stream().map(this::getSSID).toArray(String[]::new);
            return new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, ssids);
        }
        return null;
    }

    private String getSSID(String rawSSID) {
        if (rawSSID != null && rawSSID.startsWith("\"") && rawSSID.endsWith("\"")) {
            return rawSSID.substring(1, rawSSID.length() - 1);
        }
        return null;
    }

    private String getSSID(WifiConfiguration wifi) {
        return getSSID(wifi.SSID);
    }

}
