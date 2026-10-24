/*
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.xiaomikeyboard;

import android.app.Application;

public class KeyboardApplication extends Application {

    private TouchpadFlickManager mTouchpadFlickManager;

    @Override
    public void onCreate() {
        super.onCreate();
        mTouchpadFlickManager = new TouchpadFlickManager(this);
        mTouchpadFlickManager.start();
        PadKeyboardManager.get(this).start();
    }
}
