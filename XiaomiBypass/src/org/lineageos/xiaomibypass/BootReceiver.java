/*
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.xiaomibypass;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/** The kernel drops the bypass vote on reboot, so vote again at boot. */
public class BootReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        if (Intent.ACTION_LOCKED_BOOT_COMPLETED.equals(intent.getAction())) {
            BypassChargingTileService.restore(context);
        }
    }
}
