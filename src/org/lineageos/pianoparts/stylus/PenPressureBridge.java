/*
 * SPDX-FileCopyrightText: 2026 Yet Another AOSP Project
 * SPDX-License-Identifier: Apache-2.0
 */
package org.lineageos.pianoparts.stylus;

import android.content.Context;
import android.hardware.input.InputManager;
import android.os.Handler;
import android.system.ErrnoException;
import android.system.Os;
import android.system.OsConstants;
import android.system.StructPollfd;
import android.util.Log;
import android.view.InputDevice;
import java.io.File;
import java.io.FileDescriptor;
import java.io.IOException;
import java.nio.file.Files;

/** Forward Focus Pen Pro report 5 without consuming its Android input events. */
final class PenPressureBridge implements InputManager.InputDeviceListener, AutoCloseable {
    private static final String TAG = "PianoPenPressure";
    private static final String HID_ID = "0005:0022:5081.";
    private final InputManager inputs;
    private final Handler handler;
    private final PenTouchClient touch;
    private final Runnable refresh = this::refresh;
    private boolean started;
    private int retries;
    private Reader reader;

    PenPressureBridge(Context context, Handler handler, PenTouchClient touch) {
        inputs = context.getSystemService(InputManager.class);
        this.handler = handler;
        this.touch = touch;
    }

    void start() {
        if (started) return;
        started = true;
        inputs.registerInputDeviceListener(this, handler);
        refresh();
    }

    @Override public void onInputDeviceAdded(int id) { changed(); }
    @Override public void onInputDeviceChanged(int id) { changed(); }
    @Override public void onInputDeviceRemoved(int id) { changed(); }

    private void changed() {
        retries = 0;
        refresh();
    }

    private boolean connected() {
        for (int id : inputs.getInputDeviceIds()) {
            InputDevice device = inputs.getInputDevice(id);
            if (device != null && device.getVendorId() == 0x0022
                    && device.getProductId() == 0x5081) return true;
        }
        return false;
    }

    private void refresh() {
        handler.removeCallbacks(refresh);
        if (!started || !connected()) {
            if (reader != null) reader.cancelled = true;
            reader = null;
            return;
        }
        if (reader != null) return;
        File[] nodes = new File("/sys/class/hidraw").listFiles();
        if (nodes != null) {
            for (File node : nodes) {
                try {
                    if (!Files.readSymbolicLink(node.toPath()).toString().contains(HID_ID)) continue;
                    reader = new Reader("/dev/" + node.getName());
                    new Thread(reader, "PianoPenPressure").start();
                    return;
                } catch (IOException | SecurityException e) {
                    Log.w(TAG, "Cannot identify " + node, e);
                }
            }
        }
        retry();
    }

    private void retry() {
        if (started && retries++ < 10) handler.postDelayed(refresh, 1000);
    }

    @Override public void close() {
        if (!started) return;
        started = false;
        inputs.unregisterInputDeviceListener(this);
        handler.removeCallbacks(refresh);
        if (reader != null) reader.cancelled = true;
        reader = null;
    }

    private final class Reader implements Runnable {
        private final String node;
        volatile boolean cancelled;
        Reader(String node) { this.node = node; }

        @Override public void run() {
            FileDescriptor fd = null;
            try {
                fd = Os.open(node, OsConstants.O_RDONLY | OsConstants.O_NONBLOCK
                        | OsConstants.O_CLOEXEC, 0);
                StructPollfd poll = new StructPollfd();
                poll.fd = fd;
                poll.events = (short) OsConstants.POLLIN;
                byte[] buffer = new byte[64];
                while (!cancelled) {
                    if (Os.poll(new StructPollfd[]{poll}, 250) == 0) continue;
                    if ((poll.revents & (OsConstants.POLLHUP | OsConstants.POLLERR
                            | OsConstants.POLLNVAL)) != 0) break;
                    int count;
                    try {
                        count = Os.read(fd, buffer, 0, buffer.length);
                    } catch (ErrnoException e) {
                        if (e.errno == OsConstants.EAGAIN || e.errno == OsConstants.EINTR) continue;
                        throw e;
                    }
                    if (count <= 0) break;
                    if (cancelled || count != 15 || (buffer[0] & 255) != 5) continue;
                    int[] report = new int[count];
                    for (int i = 0; i < count; i++) report[i] = buffer[i] & 255;
                    if (!touch.setPressureReport(report)) break;
                }
            } catch (ErrnoException | IOException | SecurityException e) {
                Log.w(TAG, "Pressure reader stopped: " + node, e);
            } finally {
                if (fd != null) {
                    try { Os.close(fd); } catch (ErrnoException ignored) { }
                }
                handler.post(() -> {
                    if (reader == this) {
                        reader = null;
                        retry();
                    }
                });
            }
        }
    }
}
