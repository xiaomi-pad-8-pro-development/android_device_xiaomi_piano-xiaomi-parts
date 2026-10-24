/*
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.xiaomidisplay;

import android.service.quicksettings.Tile;
import android.service.quicksettings.TileService;

/**
 * Toggles the DisplayFeature true tone, which follows the ambient colour
 * temperature from the front CCT sensor, as the HyperOS true tone does.
 */
public class TrueToneTileService extends TileService {

    @Override
    public void onStartListening() {
        super.onStartListening();
        updateTile();
    }

    @Override
    public void onClick() {
        super.onClick();
        DisplayApplication app = (DisplayApplication) getApplication();
        app.setTrueTone(!app.isTrueTone());
        updateTile();
    }

    private void updateTile() {
        Tile tile = getQsTile();
        if (tile == null) {
            return;
        }
        boolean enabled = ((DisplayApplication) getApplication()).isTrueTone();
        tile.setState(enabled ? Tile.STATE_ACTIVE : Tile.STATE_INACTIVE);
        tile.updateTile();
    }
}
