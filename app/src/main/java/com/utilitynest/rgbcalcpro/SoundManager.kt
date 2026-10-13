package com.utilitynest.rgbcalcpro

import android.media.AudioManager
import android.media.ToneGenerator
import android.view.HapticFeedbackConstants
import android.view.View

/**
 * Tap feedback is produced with the platform ToneGenerator plus the standard
 * view haptics, so the app declares NO permissions (not even VIBRATE).
 */
class SoundManager(private val settings: SettingsManager) {

    private var toneGenerator: ToneGenerator? = null

    init {
        toneGenerator = try {
            ToneGenerator(AudioManager.STREAM_MUSIC, 70)
        } catch (e: Exception) {
            null
        }
    }

    fun play(view: View?) {
        view?.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)

        if (!settings.soundEnabled) return
        val tone = toneGenerator ?: return
        try {
            when (settings.soundType) {
                SettingsManager.TYPE_MECHANICAL ->
                    tone.startTone(ToneGenerator.TONE_PROP_BEEP, 30)
                SettingsManager.TYPE_DIGITAL ->
                    tone.startTone(ToneGenerator.TONE_DTMF_5, 55)
                SettingsManager.TYPE_BUBBLE ->
                    tone.startTone(ToneGenerator.TONE_PROP_ACK, 80)
                else -> {
                    // Haptic only: no tone is played.
                }
            }
        } catch (e: Exception) {
            // Audio feedback is a nicety; never let it break input.
        }
    }

    fun release() {
        try {
            toneGenerator?.release()
        } catch (ignored: Exception) {
            // ignore
        }
        toneGenerator = null
    }
}
