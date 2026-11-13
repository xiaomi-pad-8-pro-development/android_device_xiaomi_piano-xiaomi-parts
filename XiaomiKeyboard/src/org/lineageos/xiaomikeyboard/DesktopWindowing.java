/*
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.xiaomikeyboard;

import android.content.Context;
import android.content.om.OverlayInfo;
import android.content.om.OverlayManager;
import android.os.Process;
import android.os.UserHandle;
import android.util.Log;

/**
 * Desktop windowing switch. Android 17 has no setting that turns desktop
 * windowing off on a device that supports it: the developer override is
 * only read while the desktop experience developer option is off, and that
 * option is forced on here. A mutable framework overlay clears
 * config_isDesktopModeSupported instead. OverlayManager keeps its state
 * across reboots, and the framework reads it once per process, so a
 * change applies after a reboot.
 */
final class DesktopWindowing {

    private static final String TAG = "XiaomiKeyboard";
    static final String OVERLAY = "org.lineageos.overlay.piano.desktopmodeoff";

    private DesktopWindowing() {}

    static boolean isAvailable(Context context) {
        return getInfo(context, UserHandle.SYSTEM) != null;
    }

    /** Desktop windowing is on while the off overlay is disabled. */
    static boolean isEnabled(Context context) {
        OverlayInfo info = getInfo(context, UserHandle.SYSTEM);
        return info == null || !info.isEnabled();
    }

    static boolean setEnabled(Context context, boolean enabled) {
        OverlayManager om = context.getSystemService(OverlayManager.class);
        if (om == null) return false;
        try {
            om.setEnabled(OVERLAY, !enabled, UserHandle.SYSTEM);
            UserHandle me = Process.myUserHandle();
            if (!UserHandle.SYSTEM.equals(me)) {
                om.setEnabled(OVERLAY, !enabled, me);
            }
            return true;
        } catch (SecurityException | IllegalStateException e) {
            Log.e(TAG, "Couldn't switch desktop windowing", e);
            return false;
        }
    }

    private static OverlayInfo getInfo(Context context, UserHandle user) {
        OverlayManager om = context.getSystemService(OverlayManager.class);
        return om != null ? om.getOverlayInfo(OVERLAY, user) : null;
    }
}
