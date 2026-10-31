/*
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.xiaomistylus;

import android.content.Context;
import android.hardware.input.InputManager;
import android.os.Handler;
import android.util.SparseIntArray;
import android.view.InputDevice;

/**
 * Tells the touch panel which Xiaomi stylus is connected, as the stock
 * MiuiStylusDeviceListener does. The touch service turns on its pen palm
 * rejection only while a stylus is reported.
 */
final class StylusConnectionMode implements InputManager.InputDeviceListener {

    // Stock: -1 at start, the stylus type | 16 when one connects and the bare
    // type when it disconnects.
    private static final int MODE_STYLUS = 20;
    private static final int CONNECTED = 16;

    interface Setter {
        boolean setTouchMode(int mode, int value);
    }

    private final InputManager mInputManager;
    private final Handler mHandler;
    private final Setter mSetter;
    private final SparseIntArray mStyluses = new SparseIntArray();

    StylusConnectionMode(Context context, Handler handler, Setter setter) {
        mInputManager = context.getSystemService(InputManager.class);
        mHandler = handler;
        mSetter = setter;
    }

    void start() {
        mHandler.post(() -> {
            mSetter.setTouchMode(MODE_STYLUS, -1);
            mInputManager.registerInputDeviceListener(this, mHandler);
            // A stylus connected before this app started sends no add event.
            for (int id : mInputManager.getInputDeviceIds()) {
                onInputDeviceAdded(id);
            }
        });
    }

    @Override
    public void onInputDeviceAdded(int id) {
        int type = XiaomiStylusDevice.INSTANCE.typeOf(mInputManager.getInputDevice(id));
        if (type == 0 || mStyluses.indexOfKey(id) >= 0) {
            return;
        }
        mStyluses.put(id, type);
        mSetter.setTouchMode(MODE_STYLUS, type | CONNECTED);
    }

    @Override
    public void onInputDeviceRemoved(int id) {
        int index = mStyluses.indexOfKey(id);
        if (index < 0) {
            return;
        }
        int type = mStyluses.valueAt(index);
        mStyluses.removeAt(index);
        mSetter.setTouchMode(MODE_STYLUS, type);
    }

    @Override
    public void onInputDeviceChanged(int id) {
    }
}
