/*
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.xiaomistylus;

import android.os.IBinder;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.util.Log;

import vendor.xiaomi.hw.touchfeature.ITouchFeature;

/** Sets touch panel modes through the stock TouchFeature HAL. */
final class TouchFeature {

    private static final String TAG = "XiaomiStylus";
    private static final String SERVICE = "vendor.xiaomi.hw.touchfeature.ITouchFeature/default";
    private static final int TOUCH_ID = 0;

    private ITouchFeature mService;

    boolean setTouchMode(int mode, int value) {
        ITouchFeature service = getService();
        if (service == null) {
            return false;
        }
        try {
            service.setTouchMode(TOUCH_ID, mode, value);
            return true;
        } catch (RemoteException | RuntimeException e) {
            Log.e(TAG, "Failed to set touch mode " + mode, e);
            mService = null;
            return false;
        }
    }

    private ITouchFeature getService() {
        if (mService == null) {
            IBinder binder = ServiceManager.waitForDeclaredService(SERVICE);
            if (binder == null) {
                Log.e(TAG, "TouchFeature service is not available");
                return null;
            }
            mService = ITouchFeature.Stub.asInterface(binder);
        }
        return mService;
    }
}
