package com.francescozoccheddu.yeelightqstoggle;

import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Icon;
import android.service.quicksettings.Tile;
import android.service.quicksettings.TileService;
import android.util.Log;
import android.widget.Toast;

import java.util.HashSet;

public class ToggleTileService extends TileService {

    private enum State {
        HOME, NOT_HOME, NOT_SET
    }

    @Override
    public void onTileAdded() {
        super.onTileAdded();
        updateState();
    }

    private State state = State.NOT_HOME;

    private boolean toggling = false;

    private final Bulb.ToggleCommandListener toggleCommandListener = new Bulb.ToggleCommandListener() {
        @Override
        public void onCommandSent() {
            Log.d("ToggleTileService", "Toggle command sent");
        }

        @Override
        public void onSocketException(Exception exception) {
            Log.d("ToggleTileService", "Toggle command socket exception");
        }
    };

    private void updateIcon() {
        Tile tile = getQsTile();
        if (tile != null) {
            if (toggling) {
                tile.setState(Tile.STATE_UNAVAILABLE);
                tile.setLabel(getString(R.string.tile_label_pending));
            } else {
                switch (state) {
                    case HOME:
                        tile.setState(Tile.STATE_ACTIVE);
                        tile.setLabel(getString(R.string.tile_label_ready));
                        break;
                    case NOT_HOME:
                        tile.setState(Tile.STATE_UNAVAILABLE);
                        tile.setLabel(getString(R.string.tile_label_not_home));
                        break;
                    case NOT_SET:
                        tile.setState(Tile.STATE_INACTIVE);
                        tile.setLabel(getString(R.string.tile_label_not_set));
                        break;
                }
            }
            tile.updateTile();
        }
    }

    private void toggle() {
        if (!toggling) {
            toggling = true;
            updateIcon();
            final Bulb bulb = new Settings(this, Settings.DEFAULT_NAME).getStaticBulb();
            if (bulb != null) {
                Log.d("ToggleTileService", "Sending toggle command to static bulb '" + bulb.getAddress() + "'");
                bulb.sendToggleCommand(toggleCommandListener);
            } else {
                Log.d("ToggleTileService", "Creating new discoverer");
                Bulb.Discoverer discoverer = new Bulb.Discoverer(5000) {

                    boolean done = false;

                    @Override
                    public void onDiscover(Bulb bulb) {
                        if (!done) {
                            done = true;
                            Log.d("ToggleTileService", "Sending toggle command to dynamic bulb '" + bulb.getAddress() + "'");
                            bulb.sendToggleCommand(toggleCommandListener);
                            stopSearch();
                        }
                    }

                    @Override
                    public void onException(Exception exception) {
                        Log.d("ToggleTileService", "Discoverer exception: " + exception.getMessage());
                        toggling = false;
                        updateIcon();
                        if (!done) {
                            Toast.makeText(ToggleTileService.this, R.string.tile_toast_toggle_failed, Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onInterrupted() {
                        if (!done) {
                            Log.d("ToggleTileService", "Discoverer interrupted");
                            Toast.makeText(ToggleTileService.this, R.string.tile_toast_toggle_failed, Toast.LENGTH_SHORT).show();
                        }
                        toggling = false;
                        updateIcon();
                    }

                    @Override
                    public void onSocketTimeout() {
                        Log.d("ToggleTileService", "Discoverer socket timeout");
                        toggling = false;
                        updateIcon();
                        Toast.makeText(ToggleTileService.this, R.string.tile_toast_toggle_failed, Toast.LENGTH_SHORT).show();
                    }
                };
            }
        } else {
            Toast.makeText(this, R.string.tile_toast_already_toggling, Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onClick() {
        super.onClick();
        switch (state) {
            case HOME:
                Log.d("ToggleTileService", "Toggle requested");
                toggle();
                break;
            case NOT_HOME: {
                Log.d("ToggleTileService", "Click ignored");
                final String homeSSID = new Settings(this, Settings.DEFAULT_NAME).getWiFiSSID();
                final String text = String.format(getString(R.string.tile_toast_not_home), homeSSID);
                Toast.makeText(this, text, Toast.LENGTH_SHORT).show();
            }
            break;
            case NOT_SET:
                Log.d("ToggleTileService", "Redirected to settings activity");
                startActivityAndCollapse(new Intent(this, SettingsActivity.class));
                break;
        }
    }

    private void updateState() {
        String homeSSID = new Settings(this, Settings.DEFAULT_NAME).getWiFiSSID();
        if (homeSSID == null) {
            state = State.NOT_SET;
        } else {
            Log.d("ToggleTileService", "SSID '" + homeSSID + "'");
            switch (WiFiReceiver.getHomeState(this, homeSSID)) {
                case HOME:
                    state = State.HOME;
                    break;
                case NOT_HOME:
                    state = State.NOT_HOME;
                    break;
                case UNKNOWN:
                    break;
            }
        }
        Log.d("ToggleTileService", "State " + state.name());
        updateIcon();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);
        Log.d("ToggleTileService", "Service start command");
        updateState();
        return START_STICKY;
    }

    @Override
    public void onStartListening() {
        super.onStartListening();
        updateState();
    }

    public static void update(Context context) {
        context.startService(new Intent(context, ToggleTileService.class));
    }
}
