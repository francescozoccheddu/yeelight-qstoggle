package com.francescozoccheddu.yeelightqstoggle;

import android.content.Context;
import android.content.Intent;
import android.service.quicksettings.Tile;
import android.service.quicksettings.TileService;
import android.util.Log;
import android.widget.Toast;

public class ToggleTileService extends TileService {

    private boolean connected = false;
    private boolean toggling = false;

    private final Bulb.ToggleCommandListener toggleCommandListener = new Bulb.ToggleCommandListener() {
        @Override
        public void onCommandSent() {
            Log.d("ToggleTileService", "Toggle command sent");
            toggling = false;
            updateIcon();
        }

        @Override
        public void onSocketException(Exception exception) {
            Log.d("ToggleTileService", "Toggle command socket exception");
            Toast.makeText(ToggleTileService.this, R.string.tile_toast_toggle_failed, Toast.LENGTH_SHORT).show();
            toggling = false;
            updateIcon();
        }
    };

    private void updateIcon() {
        Tile tile = getQsTile();
        if (tile != null) {
            if (toggling) {
                tile.setState(Tile.STATE_UNAVAILABLE);
                tile.setLabel(getString(R.string.tile_label_toggling));
            } else {
                if (connected) {
                    tile.setState(Tile.STATE_ACTIVE);
                    tile.setLabel(getString(R.string.tile_label_connected));
                }
                else {
                    tile.setState(Tile.STATE_UNAVAILABLE);
                    tile.setLabel(getString(R.string.tile_label_disconnected));
                }
            }
            tile.updateTile();
        }
    }

    private void toggle() {
        if (!toggling) {
            toggling = true;
            updateIcon();
            Settings settings = Settings.getGlobalSettings(this);
            if (settings.isBulbStatic()) {
                final Bulb bulb = Bulb.fromAddress(settings.getBulbAddress());
                if (bulb == null) {
                    throw new RuntimeException("Bad bulb address '" + settings.getBulbAddress() + "'");
                }
                Log.d("ToggleTileService", "Sending toggle command to static bulb '" + bulb.getAddress() + "'");
                bulb.sendToggleCommand(toggleCommandListener);
            } else {
                Log.d("ToggleTileService", "Creating new discoverer");
                new Bulb.Discoverer(5000) {

                    boolean done = false;

                    @Override
                    public void onDiscover(Bulb bulb) {
                        if (!done) {
                            Log.d("ToggleTileService", "Sending toggle command to dynamic bulb '" + bulb.getAddress() + "'");
                            done = true;
                            bulb.sendToggleCommand(toggleCommandListener);
                            stopSearch();
                        }
                    }

                    @Override
                    public void onInterrupted() {
                        if (!done) {
                            Log.d("ToggleTileService", "Discoverer interrupted");
                            Toast.makeText(ToggleTileService.this, R.string.tile_toast_discovery_failed, Toast.LENGTH_SHORT).show();
                            toggling = false;
                        }
                        updateIcon();
                    }

                };
            }
        } else {
            Toast.makeText(this, R.string.tile_toast_already_toggling, Toast.LENGTH_SHORT).show();
        }
    }

    private void updateConnectedState() {
        connected = WiFiReceiver.isConnected(this);
        Log.d("ToggleTileService", "State " + (connected ? "connected" : "disconnected"));
        updateIcon();
    }

    public static void update(Context context) {
        context.startService(new Intent(context, ToggleTileService.class));
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);
        Log.d("ToggleTileService", "Service start command");
        updateConnectedState();
        return START_STICKY;
    }

    @Override
    public void onStartListening() {
        super.onStartListening();
        updateConnectedState();
    }

    @Override
    public void onTileAdded() {
        super.onTileAdded();
        updateConnectedState();
    }

    @Override
    public void onClick() {
        super.onClick();
        if (connected) {
            toggle();
        }
    }

}
