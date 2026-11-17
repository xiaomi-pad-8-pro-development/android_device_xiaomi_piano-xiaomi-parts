/*
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.xiaomistylus

import android.app.ActivityManager
import android.app.WindowConfiguration
import android.content.ComponentName
import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager

/**
 * The per-app pen rules of stock MiuiStylusShortcutManager and
 * MiuiStylusTouchFilmManager:
 *
 * - In short-video apps (stylus_page_key_bw_config.json) the pen buttons and
 *   slide move to the next or previous video, by a swipe on the screen.
 * - In reading and office apps (NEED_TOUCHFLIM_KEY_APP) the pen slide turns
 *   the page.
 * - While a game is in front (stock: Game Turbo's gb_boosting) the pen keys
 *   go to the game and no pen shortcut runs.
 */
internal object PenAppRules {
    private val REMOTE_CONTROL_APPS = setOf(
        "com.tencent.mm",
        "com.ss.android.ugc.aweme",
        "com.tencent.mobileqq",
        "com.smile.gifmaker",
        "com.xingin.xhs",
        "com.kuaishou.nebula",
        "com.ss.android.ugc.aweme.lite",
        "com.sina.weibo",
        "com.ss.android.article.video",
        "com.ss.android.article.news",
    )

    private val REMOTE_CONTROL_BLOCKED_ACTIVITIES = setOf(
        "com.tencent.mm.ui.LauncherUI",
        "com.tencent.mm.ui.EmptyActivity",
        "com.tencent.mm.plugin.sns.ui.SnsTimeLineUI",
        "com.tencent.mobileqq.activity.ChatActivity",
    )

    private val SLIDE_PAGE_APPS = setOf(
        "com.tencent.weread",
        "cn.wps.moffice_eng",
        "com.xiaomi.wpslauncher",
        "cn.wps.moffice_eng.xiaomi.lite",
        "com.microsoft.office.powerpoint",
        "com.tencent.docs",
        "com.miui.gallery",
        "com.android.browser",
        "com.ss.android.lark",
        "com.ss.android.lark.saxmsa667",
    )

    data class Foreground(val activity: ComponentName, val fullscreen: Boolean)

    fun foreground(context: Context): Foreground? = try {
        val task = context.getSystemService(ActivityManager::class.java)
            ?.getRunningTasks(1)?.firstOrNull()
        task?.topActivity?.let {
            Foreground(it, task.windowingMode == WindowConfiguration.WINDOWING_MODE_FULLSCREEN)
        }
    } catch (_: RuntimeException) { null }

    /** Stock skips the swipe in split screen and free-form windows. */
    fun isRemoteControl(top: Foreground): Boolean =
        top.fullscreen && top.activity.packageName in REMOTE_CONTROL_APPS &&
            top.activity.className !in REMOTE_CONTROL_BLOCKED_ACTIVITIES

    fun isSlidePage(top: Foreground): Boolean = top.activity.packageName in SLIDE_PAGE_APPS

    fun isGame(context: Context, top: Foreground): Boolean = try {
        context.packageManager.getApplicationInfoAsUser(top.activity.packageName, 0,
            ActivityManager.getCurrentUser()).category == ApplicationInfo.CATEGORY_GAME
    } catch (_: PackageManager.NameNotFoundException) {
        false
    } catch (_: RuntimeException) {
        false
    }
}
