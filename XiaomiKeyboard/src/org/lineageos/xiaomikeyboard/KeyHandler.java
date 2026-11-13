/* SPDX-License-Identifier: Apache-2.0 */
package org.lineageos.xiaomikeyboard;

import android.app.NotificationManager;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.InputDevice;
import android.view.KeyEvent;

import com.android.internal.os.DeviceKeyHandler;

/**
 * Handles the cover key that has no Android key-layout equivalent: the Do
 * Not Disturb key. Loaded into system_server like the stylus KeyHandler, so
 * only this key is looked at, and only from the cover. The cover's input
 * devices exist only while it is attached and are disabled while its keys
 * are ignored, so no other check is needed.
 */
public final class KeyHandler implements DeviceKeyHandler {
    private static final String TAG = "XiaomiKeyboard.KeyHandler";
    private static final int DND_SCAN_CODE = 193; // Linux KEY_F23.
    private static final int COVER_VENDOR = 0x15d9;
    private static final int COVER_PRODUCT = 0x00a3;

    private final Context mContext;
    private final Handler mHandler = new Handler(Looper.getMainLooper());
    // PhoneWindowManager calls a DeviceKeyHandler before queueing and again
    // before dispatching; toggle once per press.
    private int mLastDevice = -1;
    private long mLastDownTime = -1;

    public KeyHandler(Context context) {
        mContext = context;
    }

    @Override
    public KeyEvent handleKeyEvent(KeyEvent event) {
        if (event.getKeyCode() != KeyEvent.KEYCODE_BUTTON_2
                || event.getScanCode() != DND_SCAN_CODE) {
            return event;
        }
        InputDevice device = event.getDevice();
        if (device == null || device.getVendorId() != COVER_VENDOR
                || device.getProductId() != COVER_PRODUCT) {
            return event;
        }
        if (event.getAction() == KeyEvent.ACTION_DOWN && event.getRepeatCount() == 0) {
            synchronized (this) {
                if (event.getDeviceId() == mLastDevice
                        && event.getDownTime() == mLastDownTime) {
                    return null;
                }
                mLastDevice = event.getDeviceId();
                mLastDownTime = event.getDownTime();
            }
            mHandler.post(this::toggleDoNotDisturb);
        }
        return null;
    }

    private void toggleDoNotDisturb() {
        NotificationManager manager = mContext.getSystemService(NotificationManager.class);
        if (manager == null) return;
        int filter = manager.getCurrentInterruptionFilter();
        if (filter == NotificationManager.INTERRUPTION_FILTER_UNKNOWN) return;
        try {
            manager.setInterruptionFilter(
                    filter == NotificationManager.INTERRUPTION_FILTER_ALL
                            ? NotificationManager.INTERRUPTION_FILTER_PRIORITY
                            : NotificationManager.INTERRUPTION_FILTER_ALL,
                    true /* fromUser: physical cover key */);
        } catch (SecurityException e) {
            Log.e(TAG, "Cannot toggle Do Not Disturb", e);
        }
    }
}
