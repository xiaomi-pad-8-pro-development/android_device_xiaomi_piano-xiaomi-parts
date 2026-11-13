/*
 * Copyright (C) 2026 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.xiaomistylus

import android.app.AlertDialog
import android.app.NotificationManager
import android.bluetooth.BluetoothManager
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.PreferenceCategory
import androidx.preference.SwitchPreferenceCompat
import com.android.settingslib.widget.SettingsBasePreferenceFragment

/**
 * The pen page, laid out like HyperOS: a header card with the pen, then
 * Buttons, Gestures, Writing, Notifications and About.
 *
 * Connection, battery and firmware come from [StylusStatusStore]; the button
 * and gesture actions keep the secure settings consumed by KeyHandler, and
 * the touch-film controls keep the stock system settings in [PenControls].
 */
class StylusSettingsFragment : SettingsBasePreferenceFragment() {

    private lateinit var header: AccessoryConnectionPreference
    private lateinit var placementWarning: Preference

    private lateinit var gesturesCategory: PreferenceCategory
    private lateinit var gesturesNote: Preference
    private val gestureControls = mutableListOf<Preference>()
    private val proOnly = mutableListOf<Preference>()

    private lateinit var batteryNotification: Preference

    private lateinit var aboutCategory: PreferenceCategory
    private lateinit var model: Preference
    private lateinit var firmware: Preference

    private val statusListener = SharedPreferences.OnSharedPreferenceChangeListener { _, _ ->
        refreshStatus()
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        val context = requireContext()
        preferenceScreen = preferenceManager.createPreferenceScreen(context)

        // Header card.
        header = AccessoryConnectionPreference(context).apply {
            key = KEY_CONNECTION
            order = 0
            setIcon(R.drawable.accessory_stylus_show_icon)
            setTitle(R.string.pen_category_title)
        }
        preferenceScreen.addPreference(header)
        placementWarning = Preference(context).apply {
            order = 1
            setIcon(R.drawable.ic_info)
            setTitle(R.string.stylus_attachment_warning_title)
            isSelectable = false
        }
        preferenceScreen.addPreference(placementWarning)

        // Buttons.
        val buttons = category(10, R.string.stylus_category_buttons)
        buttons.addPreference(createActionPreference(
            StylusSettingsContract.PRIMARY_BUTTON_ACTION,
            R.string.stylus_primary_button_title, R.drawable.ic_button,
            StylusSettingsContract.DEFAULT_PRIMARY_BUTTON_ACTION, holdSummary = true))
        buttons.addPreference(createActionPreference(
            StylusSettingsContract.SECONDARY_BUTTON_ACTION,
            R.string.stylus_secondary_button_title, R.drawable.ic_button,
            StylusSettingsContract.DEFAULT_SECONDARY_BUTTON_ACTION, holdSummary = true))
        buttons.addPreference(Preference(context).apply {
            setIcon(R.drawable.ic_apps)
            setTitle(R.string.stylus_palette_title)
            setSummary(R.string.stylus_palette_summary_short)
            setOnPreferenceClickListener { showPaletteEditor(); true }
        })
        buttons.addPreference(SwitchPreferenceCompat(context).apply {
            key = StylusSettingsContract.SCREEN_OFF_QUICK_NOTE
            setIcon(R.drawable.ic_pen)
            setTitle(R.string.stylus_screen_off_quick_note_title)
            setSummary(R.string.stylus_screen_off_quick_note_summary)
            isPersistent = false
            isChecked = Settings.Secure.getInt(context.contentResolver,
                StylusSettingsContract.SCREEN_OFF_QUICK_NOTE,
                if (StylusSettingsContract.DEFAULT_SCREEN_OFF_QUICK_NOTE) 1 else 0) != 0
            setOnPreferenceChangeListener { _, newValue ->
                Settings.Secure.putInt(context.contentResolver,
                    StylusSettingsContract.SCREEN_OFF_QUICK_NOTE,
                    if (newValue as Boolean) 1 else 0)
            }
        })
        buttons.addPreference(createSecureSwitch(Settings.Secure.STYLUS_BUTTONS_ENABLED,
            R.string.stylus_buttons_title, R.string.stylus_pen_buttons_summary, R.drawable.ic_touch_app))

        // Gestures. Squeeze exists on every pen KeyHandler knows; the
        // touch-film gestures and their tuning only on the Focus Pen Pro.
        gesturesCategory = category(20, R.string.stylus_category_gestures)
        gesturesNote = Preference(context).apply {
            setIcon(R.drawable.ic_info)
            isSelectable = false
        }
        gesturesCategory.addPreference(gesturesNote)
        addGesture(createSystemSwitch(PenControls.DOUBLE_TAP, R.string.stylus_double_tap_enabled,
            R.string.stylus_double_tap_summary, R.drawable.ic_touch_app, 1), control = true, pro = true)
        addGesture(createActionPreference(StylusSettingsContract.DOUBLE_TAP_ACTION,
            R.string.stylus_double_tap_action, 0, StylusSettingsContract.Action.NONE).also {
            it.dependency = PenControls.DOUBLE_TAP }, control = false, pro = true)
        addGesture(createSystemSwitch(PenControls.PINCH, R.string.stylus_squeeze_enabled,
            R.string.stylus_squeeze_summary, R.drawable.ic_pen, 5), control = true, pro = true)
        addGesture(createActionPreference(StylusSettingsContract.PINCH_ACTION,
            R.string.stylus_pinch_title, 0, defaultPinchAction()), control = false, pro = false)
        addGesture(createSystemChoice(PenControls.PRESSURE, R.string.stylus_squeeze_pressure,
            R.string.stylus_squeeze_pressure_summary, R.array.stylus_pressure_levels, 0..4, 2,
            R.drawable.ic_touch_app),
            control = true, pro = true)
        addGesture(createSystemChoice(PenControls.PINCH_FEEDBACK, R.string.stylus_squeeze_feedback,
            R.string.stylus_squeeze_feedback_summary, R.array.stylus_feedback_levels, 0..5, 3),
            control = true, pro = true)
        addGesture(createSystemSwitch(PenControls.SLIDE, R.string.stylus_slide_enabled,
            R.string.stylus_slide_summary, R.drawable.ic_swap_vert, 1), control = true, pro = true)
        addGesture(createActionPreference(StylusSettingsContract.SLIDE_UP_ACTION,
            R.string.stylus_slide_up_action, 0, StylusSettingsContract.Action.NONE).also {
            it.dependency = PenControls.SLIDE }, control = false, pro = true)
        addGesture(createActionPreference(StylusSettingsContract.SLIDE_DOWN_ACTION,
            R.string.stylus_slide_down_action, 0, StylusSettingsContract.Action.NONE).also {
            it.dependency = PenControls.SLIDE }, control = false, pro = true)

        // Writing. This ROM has no palm rejection or pen refresh rate
        // setting, so neither is offered; the refresh rate row only explains
        // what PenRefreshRate does on its own.
        val writing = category(30, R.string.stylus_category_writing)
        writing.addPreference(createSecureSwitch(Settings.Secure.STYLUS_HANDWRITING_ENABLED,
            R.string.stylus_handwriting_title, R.string.stylus_handwriting_summary, R.drawable.ic_pen))
        createSystemSwitch(PenControls.HOVER, R.string.stylus_hover_enabled,
            R.string.stylus_hover_summary, R.drawable.ic_touch_app, 1).also {
            writing.addPreference(it); gestureControls.add(it); proOnly.add(it)
        }
        createSystemChoice(PenControls.WRITING_TYPE, R.string.stylus_writing_tool,
            R.string.stylus_writing_tool_summary, R.array.stylus_writing_tools, 1..4, 1,
            R.drawable.ic_pen).also {
            writing.addPreference(it); gestureControls.add(it); proOnly.add(it)
        }
        createSystemChoice(PenControls.WRITING_FEEDBACK, R.string.stylus_writing_feedback,
            R.string.stylus_writing_feedback_summary, R.array.stylus_feedback_levels, 0..5, 0).also {
            writing.addPreference(it); gestureControls.add(it); proOnly.add(it)
        }
        writing.addPreference(Preference(context).apply {
            setIcon(R.drawable.ic_speed)
            setTitle(R.string.stylus_refresh_title)
            setSummary(R.string.stylus_refresh_summary)
            isSelectable = false
        })

        // Notifications.
        val notifications = category(40, R.string.stylus_category_notifications)
        batteryNotification = Preference(context).apply {
            setIcon(R.drawable.ic_notifications)
            setTitle(R.string.stylus_battery_notification_title)
            intent = Intent(Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS)
                .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                .putExtra(Settings.EXTRA_CHANNEL_ID, BATTERY_CHANNEL)
        }
        notifications.addPreference(batteryNotification)

        // About.
        aboutCategory = category(50, R.string.stylus_category_about)
        model = Preference(context).apply {
            setIcon(R.drawable.ic_info)
            setTitle(R.string.stylus_model_title)
            isSelectable = false
        }
        aboutCategory.addPreference(model)
        firmware = Preference(context).apply {
            key = KEY_FIRMWARE
            setIcon(R.drawable.ic_system_update)
            setTitle(R.string.stylus_firmware_title)
            intent = Intent().setClassName(context.packageName,
                "${context.packageName}.StylusFirmwareActivity")
        }
        aboutCategory.addPreference(firmware)
    }

    override fun onResume() {
        super.onResume()
        StylusStatusStore.preferences(requireContext())
            .registerOnSharedPreferenceChangeListener(statusListener)
        refreshStatus()
        requireContext().sendBroadcast(Intent(StylusController.ACTION_CHECK_FIRMWARE)
            .setPackage(requireContext().packageName))
        listOf(StylusSettingsContract.DOUBLE_TAP_ACTION, StylusSettingsContract.SLIDE_UP_ACTION,
            StylusSettingsContract.SLIDE_DOWN_ACTION).forEach {
            refreshActionPreference(it, StylusSettingsContract.Action.NONE)
        }
        refreshActionPreference(StylusSettingsContract.PRIMARY_BUTTON_ACTION,
            StylusSettingsContract.DEFAULT_PRIMARY_BUTTON_ACTION)
        refreshActionPreference(StylusSettingsContract.SECONDARY_BUTTON_ACTION,
            StylusSettingsContract.DEFAULT_SECONDARY_BUTTON_ACTION)
        refreshBatteryNotification()
    }

    override fun onPause() {
        StylusStatusStore.preferences(requireContext())
            .unregisterOnSharedPreferenceChangeListener(statusListener)
        super.onPause()
    }

    private fun category(order: Int, title: Int): PreferenceCategory =
        PreferenceCategory(requireContext()).also {
            it.order = order
            it.setTitle(title)
            preferenceScreen.addPreference(it)
        }

    private fun addGesture(preference: Preference, control: Boolean, pro: Boolean) {
        gesturesCategory.addPreference(preference)
        if (control) gestureControls.add(preference)
        if (pro) proOnly.add(preference)
    }

    private fun createSecureSwitch(key: String, title: Int, summary: Int, icon: Int) =
        SwitchPreferenceCompat(requireContext()).apply {
            this.key = key
            setTitle(title)
            setSummary(summary)
            setIcon(icon)
            isPersistent = false
            isChecked = Settings.Secure.getInt(requireContext().contentResolver, key, 1) != 0
            setOnPreferenceChangeListener { _, newValue ->
                Settings.Secure.putInt(requireContext().contentResolver, key,
                    if (newValue as Boolean) 1 else 0)
            }
        }

    private fun createSystemSwitch(key: String, title: Int, summary: Int, icon: Int,
            enabledValue: Int) = SwitchPreferenceCompat(requireContext()).apply {
        this.key = key
        setTitle(title)
        setSummary(summary)
        setIcon(icon)
        isPersistent = false
        isChecked = PenControls.value(requireContext(), key, enabledValue) != 0
        setOnPreferenceChangeListener { _, value ->
            Settings.System.putInt(requireContext().contentResolver, key,
                if (value as Boolean) enabledValue else 0)
        }
    }

    /** A level picker whose summary is "<description> · <current level>". */
    private fun createSystemChoice(
        key: String, title: Int, description: Int, entries: Int, range: IntRange, defaultValue: Int,
        icon: Int = R.drawable.ic_vibration,
    ): ListPreference = ListPreference(requireContext()).apply {
        this.key = key
        setTitle(title)
        dialogTitle = getString(title)
        setEntries(entries)
        setIcon(icon)
        entryValues = range.map(Int::toString).toTypedArray()
        isPersistent = false
        value = PenControls.value(requireContext(), key, defaultValue)
            .coerceIn(range.first, range.last).toString()
        setDescribedSummary(this, description)
        setOnPreferenceChangeListener { preference, newValue ->
            val number = (newValue as? String)?.toIntOrNull()
            if (number == null || number !in range ||
                !Settings.System.putInt(requireContext().contentResolver, key, number)
            ) return@setOnPreferenceChangeListener false
            // The stock writing-feedback command contains both tool and strength.
            if (key == PenControls.WRITING_FEEDBACK &&
                Settings.System.getString(requireContext().contentResolver, PenControls.WRITING_TYPE) == null
            ) Settings.System.putInt(requireContext().contentResolver, PenControls.WRITING_TYPE, 1)
            (preference as ListPreference).value = number.toString()
            setDescribedSummary(preference, description)
            false // Already stored and shown.
        }
    }

    private fun setDescribedSummary(preference: ListPreference, description: Int) {
        val index = preference.findIndexOfValue(preference.value)
        val level = if (index >= 0) preference.entries[index] else null
        preference.summary = if (level == null) getString(description)
            else getString(R.string.stylus_described_value, getString(description), level)
    }

    private fun createActionPreference(
        key: String, title: Int, icon: Int, defaultValue: String, holdSummary: Boolean = false,
    ): ListPreference = ListPreference(requireContext()).apply {
        this.key = key
        setTitle(title)
        dialogTitle = getString(title)
        if (icon != 0) setIcon(icon)
        val current = StylusSettingsContract.sanitizeAction(getSecure(key), defaultValue)
        // The laser pointer needs the laser overlay; hide it where it would do nothing.
        val laser = PenLaserController.isAvailable(requireContext()) ||
            current == StylusSettingsContract.Action.LASER
        val names = resources.getTextArray(R.array.stylus_button_actions)
        val values = resources.getTextArray(R.array.stylus_button_values)
        val shown = values.indices.filter { laser || values[it] != StylusSettingsContract.Action.LASER }
        entries = shown.map { names[it] }.toTypedArray()
        entryValues = shown.map { values[it] }.toTypedArray()
        isPersistent = false
        if (holdSummary) extras.putBoolean(EXTRA_HOLD_SUMMARY, true)
        value = current
        updateActionSummary(this)
        setOnPreferenceChangeListener { preference, newValue ->
            val action = newValue as? String
                ?: return@setOnPreferenceChangeListener false
            if (action !in StylusSettingsContract.Action.SUPPORTED || !putSecure(key, action)) {
                return@setOnPreferenceChangeListener false
            }
            (preference as ListPreference).value = action
            updateActionSummary(preference)
            false // Already stored and shown.
        }
    }

    private fun refreshActionPreference(key: String, defaultValue: String) {
        val preference = findPreference<ListPreference>(key) ?: return
        preference.value = StylusSettingsContract.sanitizeAction(getSecure(key), defaultValue)
        updateActionSummary(preference)
    }

    private fun updateActionSummary(preference: ListPreference) {
        val index = preference.findIndexOfValue(preference.value)
        val action = if (index >= 0) preference.entries[index] else null
        preference.summary = when {
            action == null -> null
            preference.extras.getBoolean(EXTRA_HOLD_SUMMARY) ->
                getString(R.string.stylus_primary_button_summary_fmt, action)
            else -> action
        }
    }

    private fun refreshBatteryNotification() {
        val channel = requireContext().getSystemService(NotificationManager::class.java)
            ?.getNotificationChannel(BATTERY_CHANNEL)
        batteryNotification.summary = getString(
            if (channel != null && channel.importance == NotificationManager.IMPORTANCE_NONE)
                R.string.stylus_battery_notification_off
            else R.string.stylus_battery_notification_summary)
    }

    private fun refreshStatus() {
        val status = StylusStatusStore.snapshot(requireContext())
        refreshActionPreference(StylusSettingsContract.PINCH_ACTION, defaultPinchAction())
        // Unlike pen state persisted across process/device restarts, the
        // Bluetooth adapter is live. Never tell the user Bluetooth is off
        // merely because the pen controller has an old state snapshot.
        val bluetoothEnabled = requireContext()
            .getSystemService(BluetoothManager::class.java)?.adapter?.isEnabled == true
        val connectionState = when {
            !bluetoothEnabled -> StylusConnectionState.BLUETOOTH_OFF
            status.state == StylusConnectionState.BLUETOOTH_OFF -> StylusConnectionState.DISCONNECTED
            else -> status.state
        }
        val connected = connectionState == StylusConnectionState.CONNECTED
        val touchFilm = PenControls.isTouchFilm(status)

        // The page used to say "Focus Pen" for every pen, the Pro included.
        val modelRes = XiaomiStylusDevice.modelName(status.vendorId, status.productId)
        val modelName = getString(modelRes ?: R.string.pen_category_title)
        header.title = modelName
        activity?.title = modelName

        var summary = getString(when (connectionState) {
            StylusConnectionState.CONNECTED -> R.string.stylus_connected
            StylusConnectionState.CONNECTING -> R.string.stylus_connecting
            StylusConnectionState.PAIRING -> R.string.stylus_pairing
            StylusConnectionState.BLUETOOTH_OFF -> R.string.stylus_bluetooth_off
            StylusConnectionState.CONNECTION_FAILED -> R.string.stylus_connection_failed
            StylusConnectionState.DISCONNECTED -> R.string.stylus_header_disconnected_guide
        })
        if (connected && !status.penInitialized && touchFilm) {
            summary = getString(R.string.stylus_initializing)
        }
        if (status.penError != 0) {
            summary = getString(when (status.penError) {
                1 -> R.string.stylus_battery_error
                2 -> R.string.stylus_temperature_error
                3 -> R.string.stylus_current_error
                else -> R.string.stylus_setting_rejected
            })
        }
        header.summary = summary
        val batteryLabel = when {
            status.battery == null -> null
            status.docked -> getString(R.string.stylus_charging_battery, status.battery)
            else -> getString(R.string.stylus_battery_percent, status.battery)
        }
        header.setBattery(if (connected) status.battery else null, batteryLabel,
            connected && !status.penInitialized)

        placementWarning.isVisible = status.placementError != 0
        placementWarning.summary = getString(R.string.stylus_attachment_warning,
            status.placementError)

        // Gestures: hide what this pen cannot do, and say why the rest is
        // locked instead of only greying it out.
        proOnly.forEach { it.isVisible = touchFilm }
        val controlsReady = connected && status.penInitialized
        gestureControls.forEach { it.isEnabled = controlsReady }
        gesturesNote.isVisible = touchFilm && !controlsReady
        gesturesNote.summary = getString(if (connected) R.string.stylus_gestures_initializing
            else R.string.stylus_gestures_not_ready)

        aboutCategory.isVisible = connected || modelRes != null
        model.summary = modelName
        firmware.isVisible = connected
        val revision = status.firmwareRevision
        firmware.summary = when {
            revision == null -> getString(R.string.stylus_unavailable)
            status.updateState == "available" ->
                getString(R.string.stylus_firmware_available, status.updateVersion)
            status.updateState == "updating" ->
                getString(R.string.stylus_firmware_progress, status.updateProgress)
            else -> getString(R.string.stylus_firmware_summary_fmt, revision)
        }
    }

    private fun getSecure(key: String): String? =
        Settings.Secure.getString(requireContext().contentResolver, key)

    private fun putSecure(key: String, value: String): Boolean =
        Settings.Secure.putString(requireContext().contentResolver, key, value)

    private fun showPaletteEditor() {
        val context = requireContext()
        val choices = PenPalette.candidates(context)
        val available = choices.map { it.id }.toSet()
        val selected = PenPalette.selected(context).filter(available::contains).toMutableList()
        val checked = BooleanArray(choices.size) { choices[it].id in selected }
        val dialog = AlertDialog.Builder(context)
            .setTitle(R.string.stylus_palette_title)
            .setMultiChoiceItems(choices.map { it.label }.toTypedArray(), checked) { dialog, index, enabled ->
                val id = choices[index].id
                if (enabled && selected.size >= PenPalette.MAX_ENTRIES) {
                    (dialog as AlertDialog).listView.setItemChecked(index, false)
                    Toast.makeText(context, R.string.stylus_palette_limit, Toast.LENGTH_SHORT).show()
                } else if (enabled) selected.add(id) else selected.remove(id)
            }
            .setNegativeButton(android.R.string.cancel, null)
            .setNeutralButton(R.string.stylus_palette_reset) { _, _ -> PenPalette.reset(context) }
            .setPositiveButton(android.R.string.ok, null)
            .create()
        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                if (selected.isEmpty()) Toast.makeText(context, R.string.stylus_palette_empty, Toast.LENGTH_SHORT).show()
                else if (PenPalette.save(context, selected)) dialog.dismiss()
            }
        }
        dialog.show()
    }

    private fun defaultPinchAction(): String =
        if (PenControls.isTouchFilm(StylusStatusStore.snapshot(requireContext())))
            StylusSettingsContract.DEFAULT_PINCH_ACTION else StylusSettingsContract.Action.RECENTS

    private companion object {
        const val KEY_CONNECTION = "stylus_connection"
        const val KEY_FIRMWARE = "stylus_firmware"
        const val BATTERY_CHANNEL = "pen_battery"
        const val EXTRA_HOLD_SUMMARY = "hold_summary"
    }
}
