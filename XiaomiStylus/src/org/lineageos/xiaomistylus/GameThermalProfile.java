/*
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.xiaomistylus;

import android.app.ActivityManager;
import android.app.ActivityTaskManager;
import android.app.TaskStackListener;
import android.content.ComponentName;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.os.RemoteException;
import android.os.SystemProperties;
import android.util.Log;

import java.util.List;

/**
 * Like HyperOS, switch piano-thermald to the game thermal profile while a game
 * is in front. A game is an app that declares the game category.
 */
class GameThermalProfile {

    private static final String TAG = "XiaomiStylus.GameThermal";
    private static final String PROP = "sys.piano.thermald.profile";
    private static final String GAME = "tgame";

    private final Context mContext;
    private final Handler mHandler;
    private String mProfile;

    private final Runnable mRefresh = this::refresh;

    private final TaskStackListener mListener = new TaskStackListener() {
        @Override
        public void onTaskStackChanged() {
            mHandler.removeCallbacks(mRefresh);
            mHandler.postDelayed(mRefresh, 300L);
        }
    };

    GameThermalProfile(Context context, Handler handler) {
        mContext = context.getApplicationContext();
        mHandler = handler;
    }

    void start() {
        try {
            ActivityTaskManager.getService().registerTaskStackListener(mListener);
        } catch (RemoteException | RuntimeException e) {
            Log.e(TAG, "Cannot watch the app in front", e);
            return;
        }
        refresh();
    }

    private void refresh() {
        String top = null;
        try {
            List<ActivityManager.RunningTaskInfo> tasks =
                    mContext.getSystemService(ActivityManager.class).getRunningTasks(1);
            ComponentName c = tasks.isEmpty() ? null : tasks.get(0).topActivity;
            top = c == null ? null : c.getPackageName();
        } catch (RuntimeException e) {
            Log.w(TAG, "Cannot read the app in front", e);
        }
        set(isGame(top) ? GAME : "");
    }

    private boolean isGame(String packageName) {
        if (packageName == null) return false;
        try {
            ApplicationInfo info =
                    mContext.getPackageManager().getApplicationInfo(packageName, 0);
            return info.category == ApplicationInfo.CATEGORY_GAME;
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }

    private void set(String profile) {
        if (profile.equals(mProfile)) return;
        try {
            SystemProperties.set(PROP, profile);
            mProfile = profile;
            Log.i(TAG, "Thermal profile " + (profile.isEmpty() ? "normal" : profile));
        } catch (RuntimeException e) {
            // piano-thermald is not in this build, or the property is denied.
            Log.w(TAG, "Cannot set " + PROP, e);
        }
    }
}
