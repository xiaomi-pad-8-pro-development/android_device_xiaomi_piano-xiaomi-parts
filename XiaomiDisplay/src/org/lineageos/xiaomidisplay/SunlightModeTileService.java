/*
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.xiaomidisplay;

import android.service.quicksettings.Tile;
import android.service.quicksettings.TileService;

/**
 * Toggles the automatic sunlight screen boost.
 */
public class SunlightModeTileService extends TileService {

    @Override
    public void onStartListening() {
        super.onStartListening();
        updateTile();
    }

    @Override
    public void onClick() {
        super.onClick();
        DisplayApplication app = (DisplayApplication) getApplication();
        app.setSunlightMode(!app.isSunlightMode());
        updateTile();
    }

    private void updateTile() {
        Tile tile = getQsTile();
        if (tile == null) {
            return;
        }
        boolean enabled = ((DisplayApplication) getApplication()).isSunlightMode();
        tile.setState(enabled ? Tile.STATE_ACTIVE : Tile.STATE_INACTIVE);
        tile.updateTile();
    }
}
