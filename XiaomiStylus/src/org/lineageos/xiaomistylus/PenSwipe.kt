/*
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.xiaomistylus

import android.content.Context
import android.graphics.Point
import android.hardware.input.InputManager
import android.os.Handler
import android.os.HandlerThread
import android.os.SystemClock
import android.util.Log
import android.view.Display
import android.view.InputDevice
import android.view.InputEvent
import android.view.KeyCharacterMap
import android.view.KeyEvent
import android.view.MotionEvent
import android.hardware.display.DisplayManager

/**
 * Injects what stock sends for the per-app pen rules: a vertical swipe for
 * the next or previous short video (stock swipeGenerator, 60 ms), and a page
 * key for the slide in reading apps. Runs in system_server, which may inject.
 */
internal class PenSwipe(private val context: Context) {
    private val thread = HandlerThread("PianoPenSwipe").apply { start() }
    private val handler = Handler(thread.looper)

    /** next: swipe up to the next video; otherwise down to the previous one. */
    fun video(next: Boolean) = handler.post {
        val display = context.getSystemService(DisplayManager::class.java)
            ?.getDisplay(Display.DEFAULT_DISPLAY) ?: return@post
        // Injected touches use the rotated logical size of the display.
        val size = Point().also { @Suppress("DEPRECATION") display.getRealSize(it) }
        val x = size.x / 2f
        val h = size.y.toFloat()
        val from = h * if (next) 0.7f else 0.3f
        val to = h * if (next) 0.3f else 0.7f
        val down = SystemClock.uptimeMillis()
        inject(motion(down, down, MotionEvent.ACTION_DOWN, x, from))
        for (step in 1..STEPS) {
            SystemClock.sleep(SWIPE_MS / STEPS)
            val y = from + (to - from) * step / STEPS
            inject(motion(down, SystemClock.uptimeMillis(), MotionEvent.ACTION_MOVE, x, y))
        }
        inject(motion(down, SystemClock.uptimeMillis(), MotionEvent.ACTION_UP, x, to))
    }

    fun pageKey(keyCode: Int) = handler.post {
        val now = SystemClock.uptimeMillis()
        for (action in intArrayOf(KeyEvent.ACTION_DOWN, KeyEvent.ACTION_UP)) {
            inject(KeyEvent(now, now, action, keyCode, 0, 0, KeyCharacterMap.VIRTUAL_KEYBOARD,
                0, KeyEvent.FLAG_FROM_SYSTEM, InputDevice.SOURCE_KEYBOARD))
        }
    }

    private fun motion(down: Long, time: Long, action: Int, x: Float, y: Float) =
        MotionEvent.obtain(down, time, action, x, y, 0).apply {
            source = InputDevice.SOURCE_TOUCHSCREEN
        }

    private fun inject(event: InputEvent) {
        try {
            context.getSystemService(InputManager::class.java)?.injectInputEvent(event,
                InputManager.INJECT_INPUT_EVENT_MODE_ASYNC)
        } catch (exception: RuntimeException) {
            Log.w(TAG, "Cannot inject the pen gesture", exception)
        }
    }

    companion object {
        private const val TAG = "XiaomiStylus.PenSwipe"
        private const val SWIPE_MS = 60L
        private const val STEPS = 6
    }
}
