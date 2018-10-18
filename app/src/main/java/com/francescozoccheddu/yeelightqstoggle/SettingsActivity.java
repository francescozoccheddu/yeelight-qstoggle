package com.francescozoccheddu.yeelightqstoggle;

import android.content.Context;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.support.design.widget.TextInputLayout;
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
    private TextInputLayout tilWiFiSSID;
    private TextInputLayout tilBulbAddress;

    private ArrayAdapter<String> bulbsAdapter;
    private HashSet<Bulb> bulbSet;
    private Bulb.Discoverer bulbDiscoverer;

    private Settings settings;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        cb_BulbStatic = findViewById(R.id.sets_cb_bulb_static);
        actvWiFiSSID = findViewById(R.id.sets_actv_wifi_ssid);
        actvBulbAddress = findViewById(R.id.sets_actv_bulb_address);
        tvBulbDynamicInfo = findViewById(R.id.sets_tv_bulb_dynamic_info);
        tilBulbAddress = findViewById(R.id.sets_til_bulb_address);
        tilWiFiSSID = findViewById(R.id.sets_til_wifi_ssid);

        settings = new Settings(this, Settings.DEFAULT_NAME);
        {
            actvWiFiSSID.setText(settings.getWiFiSSID());
            Bulb bulb = settings.getStaticBulb();
            cb_BulbStatic.setChecked(bulb != null);
            if (bulb != null) {
                actvBulbAddress.setText(bulb.getAddress());
            }
            tvBulbDynamicInfo.setVisibility(cb_BulbStatic.isChecked() ? View.GONE : View.VISIBLE);
            tilBulbAddress.setVisibility(cb_BulbStatic.isChecked() ? View.VISIBLE : View.GONE);
        }

        cb_BulbStatic.setOnCheckedChangeListener((view, isChecked) -> {
            tvBulbDynamicInfo.setVisibility(isChecked ? View.GONE : View.VISIBLE);
            tilBulbAddress.setVisibility(isChecked ? View.VISIBLE : View.GONE);
            if (isChecked) {
                validateBulbAddress(actvBulbAddress.getText().toString(), true);
            }
        });

        validateWiFiSSID(actvWiFiSSID.getText().toString(), true);

        actvWiFiSSID.setOnFocusChangeListener((view, hasFocus) -> {
            if (hasFocus) {
                actvWiFiSSID.setAdapter(getWiFiNetworksAdapter());
                actvWiFiSSID.showDropDown();
            }
        });

        actvWiFiSSID.setOnClickListener((view) -> {
            actvWiFiSSID.showDropDown();
        });

        actvWiFiSSID.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                validateWiFiSSID(s.toString(), false);
            }
        });

        validateBulbAddress(actvBulbAddress.getText().toString(), true);

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

        actvBulbAddress.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                validateBulbAddress(s.toString(), false);
            }
        });

    }

    private void runBulbDiscoverer() {
        if (bulbDiscoverer == null || !bulbDiscoverer.isSearching()) {
            Log.d("SettingsActivity", "Creating new bulb discoverer");
            bulbDiscoverer = new Bulb.Discoverer(10000) {

                @Override
                public void onDiscover(Bulb bulb) {
                    Log.d("SettingsActivity", "Discovered bulb '" + bulb.toString() + "'");
                    if (bulbSet.add(bulb)) {
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
            String[] ssids = wifis.stream().map(SettingsActivity::getSSID).toArray(String[]::new);
            return new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, ssids);
        }
        return null;
    }

    private static String getSSID(String rawSSID) {
        if (rawSSID != null && rawSSID.startsWith("\"") && rawSSID.endsWith("\"")) {
            return rawSSID.substring(1, rawSSID.length() - 1);
        }
        return null;
    }

    private static String getSSID(WifiConfiguration wifi) {
        return getSSID(wifi.SSID);
    }

    private static boolean isValidAddress(String address) {
        return Bulb.fromAddress(address) != null;
    }

    private void validateBulbAddress(String address, boolean allowEmpty) {
        if (address.isEmpty()) {
            if (allowEmpty) {
                tilBulbAddress.setError(null);
            } else {
                tilBulbAddress.setError(getString(R.string.sets_bulb_address_error_empty));
            }
        } else if (!isValidAddress(address)) {
            tilBulbAddress.setError(getString(R.string.sets_bulb_address_error_invalid));
        } else {
            tilBulbAddress.setError(null);
        }
    }

    private void validateWiFiSSID(String SSID, boolean allowEmpty) {
        if (!allowEmpty && SSID.isEmpty()) {
            tilWiFiSSID.setError(getString(R.string.sets_wifi_ssid_error_empty));
        } else {
            tilWiFiSSID.setError(null);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();

        if (cb_BulbStatic.isChecked()) {
            final Bulb bulb = Bulb.fromAddress(actvBulbAddress.getText().toString());
            if (bulb != null) {
                settings.setStaticBulb(bulb);
            }
            else {
                settings.setDynamicBulb();
            }
        }
        else {
            settings.setDynamicBulb();
        }

        settings.setWiFiSSID(actvWiFiSSID.getText().toString());

        settings.saveAsync();
    }
}
