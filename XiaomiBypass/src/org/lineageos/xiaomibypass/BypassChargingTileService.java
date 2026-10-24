/*
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.xiaomibypass;

import android.content.Context;
import android.content.SharedPreferences;
import android.service.quicksettings.Tile;
import android.service.quicksettings.TileService;
import android.util.Log;

import java.io.FileWriter;
import java.io.IOException;

/**
 * Bypass charging, the HyperOS "side road charge": while plugged in, power
 * the tablet straight from the charger and let the battery rest. The smart_chg
 * bypass command only lowers the charge current by temperature, so vote the
 * battery charging off in the MCA charge interface instead. The input stays on
 * and the kernel holds the vote until it is withdrawn or the tablet reboots,
 * so the state is saved and restored at boot.
 */
public class BypassChargingTileService extends TileService {

    private static final String TAG = "XiaomiBypass";

    private static final String CHARGE_ENABLE_PATH =
            "/sys/devices/platform/soc/soc:mca_charge_interface/charge_enable";
    private static final String VOTER = "bypass all ";

    private static final String PREFS_NAME = "bypass";
    private static final String KEY_ENABLED = "enabled";

    @Override
    public void onStartListening() {
        super.onStartListening();
        updateTile();
    }

    @Override
    public void onClick() {
        super.onClick();
        setEnabled(this, !isEnabled(this));
        updateTile();
    }

    private void updateTile() {
        Tile tile = getQsTile();
        if (tile == null) {
            return;
        }
        tile.setState(isEnabled(this) ? Tile.STATE_ACTIVE : Tile.STATE_INACTIVE);
        tile.updateTile();
    }

    static boolean isEnabled(Context context) {
        return getPrefs(context).getBoolean(KEY_ENABLED, false);
    }

    static void setEnabled(Context context, boolean enabled) {
        if (write(enabled)) {
            getPrefs(context).edit().putBoolean(KEY_ENABLED, enabled).apply();
        }
    }

    /** The kernel drops the vote on reboot, so vote again at startup. */
    static void restore(Context context) {
        if (isEnabled(context)) {
            write(true);
        }
    }

    private static boolean write(boolean enabled) {
        try (FileWriter writer = new FileWriter(CHARGE_ENABLE_PATH)) {
            writer.write(VOTER + (enabled ? "0" : "1"));
            return true;
        } catch (IOException e) {
            Log.e(TAG, "Failed to write charge_enable", e);
            return false;
        }
    }

    private static SharedPreferences getPrefs(Context context) {
        return context.createDeviceProtectedStorageContext()
                .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }
}
