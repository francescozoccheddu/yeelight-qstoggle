package com.francescozoccheddu.yeelightqstoggle;

import android.graphics.drawable.Icon;
import android.service.quicksettings.Tile;
import android.service.quicksettings.TileService;

public class ToggleTileService extends TileService {

    private void updateTile(boolean active) {
        Tile tile = getQsTile();
        if (tile != null) {
            tile.setIcon(Icon.createWithResource(getApplicationContext(), R.drawable.ic_bulb));
            tile.setState(active ? Tile.STATE_ACTIVE : Tile.STATE_UNAVAILABLE);
            tile.updateTile();
        }
    }

}
