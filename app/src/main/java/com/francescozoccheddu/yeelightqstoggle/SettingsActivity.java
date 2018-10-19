package com.francescozoccheddu.yeelightqstoggle;

import android.content.Context;
import android.net.wifi.WifiConfiguration;
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
import android.widget.CheckBox;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

public class SettingsActivity extends AppCompatActivity {

    private CheckBox cb_WiFiStatic;
    private CheckBox cb_BulbStatic;
    private AutoCompleteTextView actvWiFiSSID;
    private AutoCompleteTextView actvBulbAddress;
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

        cb_WiFiStatic = findViewById(R.id.sets_cb_wifi_static);
        cb_BulbStatic = findViewById(R.id.sets_cb_bulb_static);
        actvWiFiSSID = findViewById(R.id.sets_actv_wifi_ssid);
        actvBulbAddress = findViewById(R.id.sets_actv_bulb_address);
        tilBulbAddress = findViewById(R.id.sets_til_bulb_address);
        tilWiFiSSID = findViewById(R.id.sets_til_wifi_ssid);

        cb_WiFiStatic.setOnCheckedChangeListener((view, isChecked) -> {
            if (isChecked) {
                actvWiFiSSID.setText(null);
                validateWiFiSSID(actvWiFiSSID.getText().toString(), true);
            }
            tilWiFiSSID.setVisibility(isChecked ? View.VISIBLE : View.GONE);
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

        cb_BulbStatic.setOnCheckedChangeListener((view, isChecked) -> {
            if (isChecked) {
                actvBulbAddress.setText(null);
                validateBulbAddress(actvBulbAddress.getText().toString(), true);
            }
            tilBulbAddress.setVisibility(isChecked ? View.VISIBLE : View.GONE);
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

        {
            Settings settings = Settings.getGlobalSettings(this);
            cb_WiFiStatic.setChecked(settings.isWiFiStatic());
            cb_BulbStatic.setChecked(settings.isBulbStatic());
            actvWiFiSSID.setText(settings.getWiFiSSID());
            actvBulbAddress.setText(settings.getBulbAddress());
        }

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
            if (wifis != null) {
                String[] ssids = wifis.stream().map(SSIDUtils::getSSID).toArray(String[]::new);
                return new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, ssids);
            }
        }
        return null;
    }

    private static boolean isValidBulbAddress(String address) {
        return Bulb.fromAddress(address) != null;
    }

    private void validateBulbAddress(String address, boolean allowEmpty) {
        if (address.isEmpty()) {
            if (allowEmpty) {
                tilBulbAddress.setError(null);
            } else {
                tilBulbAddress.setError(getString(R.string.sets_bulb_address_error_empty));
            }
        } else if (!isValidBulbAddress(address)) {
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

    private void saveSettings() {
        Settings.Editor settings = Settings.getGlobalSettings(this).edit();

        final String wifiSSID = actvWiFiSSID.getText().toString();
        final String bulbAddress = actvBulbAddress.getText().toString();

        settings.setWiFiSSID(wifiSSID);
        settings.setStaticWiFi(cb_WiFiStatic.isChecked() && !wifiSSID.isEmpty());
        settings.setBulbAddress(bulbAddress);
        settings.setStaticBulb(cb_BulbStatic.isChecked() && isValidBulbAddress(bulbAddress));

        settings.apply();

        Log.d("SettingsActivity", "Settings saved");

        ToggleTileService.update(this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        saveSettings();
    }
}
