/* SPDX-License-Identifier: Apache-2.0 */
package org.lineageos.xiaomistylus;

import android.os.IBinder;
import android.os.Parcel;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.util.Log;

/** Verified .44 NDK AIDL transactions; independent of the older partial client AIDL. */
final class PenTouchClient {
    private static final String DESCRIPTOR = "vendor.xiaomi.hw.touchfeature.ITouchFeature";
    private IBinder service;

    boolean setMode(int mode, int value) {
        return transact(9, mode, value, null);
    }

    boolean setFirmware(String revision) {
        if (revision == null || revision.isEmpty()) return true;
        return transact(14, 22, 0, revision);
    }

    synchronized boolean setPressureReport(int[] report) {
        if (report == null || report.length != 15 || report[0] != 5) return false;
        Parcel data = Parcel.obtain();
        Parcel reply = Parcel.obtain();
        try {
            if (service == null || !service.isBinderAlive()) {
                service = ServiceManager.checkService(DESCRIPTOR + "/default");
            }
            if (service == null) return false;
            data.writeInterfaceToken(DESCRIPTOR);
            data.writeInt(0);
            data.writeInt(1088);
            data.writeInt(report.length);
            data.writeIntArray(report);
            // setModeLongValue is transaction 8 in the stock NDK interface.
            if (!service.transact(8, data, reply, 0)) return false;
            reply.readException();
            return reply.readInt() == 0;
        } catch (RemoteException | RuntimeException exception) {
            service = null;
            Log.w("PianoStylus", "Pressure report transaction failed", exception);
            return false;
        } finally {
            data.recycle();
            reply.recycle();
        }
    }

    private synchronized boolean transact(int transaction, int mode, int value, String text) {
        Parcel data = Parcel.obtain();
        Parcel reply = Parcel.obtain();
        try {
            if (service == null || !service.isBinderAlive()) {
                service = ServiceManager.checkService(DESCRIPTOR + "/default");
            }
            if (service == null) return false;
            data.writeInterfaceToken(DESCRIPTOR);
            data.writeInt(0);
            data.writeInt(mode);
            if (text == null) data.writeInt(value); else data.writeString(text);
            if (!service.transact(transaction, data, reply, 0)) return false;
            reply.readException();
            // .44 returns a native mode-set result. Stock ignores the package-name string result.
            if (text == null && reply.readInt() != 0) {
                Log.w("PianoStylus", "Touch controller rejected mode " + mode);
                return false;
            }
            return true;
        } catch (RemoteException | RuntimeException exception) {
            service = null;
            Log.w("PianoStylus", "Pen touch transaction failed", exception);
            return false;
        } finally {
            data.recycle();
            reply.recycle();
        }
    }
}
