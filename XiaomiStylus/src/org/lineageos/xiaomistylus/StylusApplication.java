/*
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.xiaomistylus;

import android.app.Application;
import android.os.Handler;
import android.os.Looper;

public class StylusApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        Handler handler = new Handler(Looper.getMainLooper());
        TouchFeature touchFeature = new TouchFeature();
        new PenBatteryNotifier(this);
        new StylusController(this, handler).start();
        new StylusAppModeController(this, handler, touchFeature::setTouchMode).start();
        new StylusConnectionMode(this, handler, touchFeature::setTouchMode).start();
    }
}
