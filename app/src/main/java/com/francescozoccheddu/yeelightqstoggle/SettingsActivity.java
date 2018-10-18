package com.francescozoccheddu.yeelightqstoggle;

import android.app.Activity;
import android.os.Bundle;

public class SettingsActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        findViewById(R.id.button).setOnClickListener((a) -> {

            Bulb.Discoverer d = new Bulb.Discoverer();
            d.startSearch(200000);
        });

    }
}
