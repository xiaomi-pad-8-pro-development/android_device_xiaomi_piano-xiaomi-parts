/* SPDX-License-Identifier: Apache-2.0 */
package org.lineageos.xiaomikeyboard;

import android.app.AlertDialog;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.media.AudioManager;
import android.media.AudioRecordingConfiguration;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.util.Log;
import android.view.ContextThemeWrapper;
import android.view.InputDevice;
import android.view.KeyEvent;
import android.view.WindowManager;
import android.widget.Toast;

import com.android.internal.os.DeviceKeyHandler;

import java.util.List;

/**
 * Handles the cover keys Android does not cover on its own, loaded into
 * system_server like the stylus KeyHandler:
 *
 * - The Do Not Disturb key, which has no key-layout equivalent.
 * - The microphone key. Android already mutes the microphone on KEYCODE_MUTE;
 *   like stock KeyboardMicKeyHelper this shows whether it is now on or off,
 *   and offers to turn it back on when an app starts recording while the
 *   key has it off.
 *
 * The cover's input devices exist only while it is attached and are disabled
 * while its keys are ignored, so no other check is needed.
 */
public final class KeyHandler implements DeviceKeyHandler {
    private static final String TAG = "XiaomiKeyboard.KeyHandler";
    private static final String PACKAGE = "org.lineageos.xiaomikeyboard";
    private static final int DND_SCAN_CODE = 193; // Linux KEY_F23.
    private static final int COVER_VENDOR = 0x15d9;
    private static final int COVER_PRODUCT = 0x00a3;
    // The mute change follows the key press within this window.
    private static final long MIC_KEY_WINDOW_MS = 1000;

    private final Context mContext;
    private final Handler mHandler = new Handler(Looper.getMainLooper());
    // PhoneWindowManager calls a DeviceKeyHandler before queueing and again
    // before dispatching; toggle once per press.
    private int mLastDevice = -1;
    private long mLastDownTime = -1;
    private volatile long mMicKeyTime = -1;
    private boolean mMutedByKey;
    private int mActiveRecordings;
    private AlertDialog mMicDialog;

    public KeyHandler(Context context) {
        mContext = context;
        try {
            context.registerReceiver(new BroadcastReceiver() {
                @Override
                public void onReceive(Context c, Intent intent) {
                    onMicrophoneMuteChanged();
                }
            }, new IntentFilter(AudioManager.ACTION_MICROPHONE_MUTE_CHANGED), null, mHandler);
            AudioManager audio = context.getSystemService(AudioManager.class);
            if (audio != null) {
                audio.registerAudioRecordingCallback(new AudioManager.AudioRecordingCallback() {
                    @Override
                    public void onRecordingConfigChanged(List<AudioRecordingConfiguration> configs) {
                        onRecordingsChanged(configs.size());
                    }
                }, mHandler);
            }
        } catch (RuntimeException e) {
            Log.w(TAG, "Microphone key feedback unavailable", e);
        }
    }

    @Override
    public KeyEvent handleKeyEvent(KeyEvent event) {
        InputDevice device = event.getDevice();
        if (device == null || device.getVendorId() != COVER_VENDOR
                || device.getProductId() != COVER_PRODUCT) {
            return event;
        }
        if (event.getKeyCode() == KeyEvent.KEYCODE_MUTE) {
            // Leave the mute itself to PhoneWindowManager.
            if (event.getAction() == KeyEvent.ACTION_DOWN && event.getRepeatCount() == 0) {
                mMicKeyTime = SystemClock.uptimeMillis();
            }
            return event;
        }
        if (event.getKeyCode() != KeyEvent.KEYCODE_BUTTON_2
                || event.getScanCode() != DND_SCAN_CODE) {
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

    private void onMicrophoneMuteChanged() {
        AudioManager audio = mContext.getSystemService(AudioManager.class);
        if (audio == null) return;
        boolean muted = audio.isMicrophoneMute();
        boolean fromKey = SystemClock.uptimeMillis() - mMicKeyTime < MIC_KEY_WINDOW_MS;
        if (fromKey) {
            mMutedByKey = muted;
            Context res = resources();
            if (res != null) {
                Toast.makeText(res, muted ? R.string.mic_muted_toast : R.string.mic_unmuted_toast,
                        Toast.LENGTH_SHORT).show();
            }
        } else if (!muted) {
            mMutedByKey = false;
        }
        if (!muted) dismissMicDialog();
    }

    private void onRecordingsChanged(int active) {
        boolean started = active > mActiveRecordings;
        mActiveRecordings = active;
        AudioManager audio = mContext.getSystemService(AudioManager.class);
        if (!started || !mMutedByKey || audio == null || !audio.isMicrophoneMute()
                || mMicDialog != null) {
            return;
        }
        Context res = resources();
        if (res == null) return;
        Context themed = new ContextThemeWrapper(res,
                android.R.style.Theme_DeviceDefault_Dialog_Alert);
        mMicDialog = new AlertDialog.Builder(themed)
                .setTitle(R.string.mic_muted_dialog_title)
                .setMessage(R.string.mic_muted_dialog_message)
                .setPositiveButton(R.string.mic_muted_dialog_turn_on, (d, w) -> {
                    AudioManager a = mContext.getSystemService(AudioManager.class);
                    if (a != null) a.setMicrophoneMute(false);
                })
                .setNegativeButton(R.string.mic_muted_dialog_keep_off, null)
                .setOnDismissListener(d -> mMicDialog = null)
                .create();
        mMicDialog.getWindow().setType(WindowManager.LayoutParams.TYPE_SYSTEM_DIALOG);
        try {
            mMicDialog.show();
        } catch (RuntimeException e) {
            Log.w(TAG, "Cannot show the microphone dialog", e);
            mMicDialog = null;
        }
    }

    private void dismissMicDialog() {
        if (mMicDialog != null) mMicDialog.dismiss();
    }

    /** This class runs in system_server; the strings live in the Parts APK. */
    private Context resources() {
        try {
            return mContext.createPackageContext(PACKAGE, 0);
        } catch (PackageManager.NameNotFoundException e) {
            Log.w(TAG, "Cannot load the keyboard strings", e);
            return null;
        }
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
