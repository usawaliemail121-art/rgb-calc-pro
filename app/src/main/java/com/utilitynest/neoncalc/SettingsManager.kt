package com.utilitynest.neoncalc

import android.content.Context

/** Persists user preferences (tap sound on/off and the chosen sound style). */
class SettingsManager(context: Context) {

    private val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    var soundEnabled: Boolean
        get() = prefs.getBoolean(KEY_SOUND_ENABLED, true)
        set(value) = prefs.edit().putBoolean(KEY_SOUND_ENABLED, value).apply()

    var soundType: Int
        get() = prefs.getInt(KEY_SOUND_TYPE, TYPE_MECHANICAL)
        set(value) = prefs.edit().putInt(KEY_SOUND_TYPE, value).apply()

    companion object {
        private const val PREFS = "rgbcalc_settings"
        private const val KEY_SOUND_ENABLED = "sound_enabled"
        private const val KEY_SOUND_TYPE = "sound_type"

        const val TYPE_MECHANICAL = 0
        const val TYPE_DIGITAL = 1
        const val TYPE_BUBBLE = 2
        const val TYPE_HAPTIC = 3
    }
}
