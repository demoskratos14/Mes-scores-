package com.example.scoreboard

import android.content.Context
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

/** Signal de fin de minuteur : vibration + bip d'alarme. Ne plante jamais si le téléphone ne sait pas faire l'un des deux. */
object TimerAlert {

    fun play(context: Context) {
        val appContext = context.applicationContext
        try {
            vibrate(appContext)
        } catch (e: Exception) {
            // Pas de vibreur ou permission refusée : on ignore.
        }
        try {
            beep()
        } catch (e: Exception) {
            // Pas de son disponible : on ignore.
        }
    }

    private fun vibrate(context: Context) {
        val vibrator: Vibrator? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            (context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager)?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
        vibrator?.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 500, 250, 500, 250, 500), -1))
    }

    private fun beep() {
        val tone = ToneGenerator(AudioManager.STREAM_ALARM, 90)
        tone.startTone(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, 1500)
        Handler(Looper.getMainLooper()).postDelayed({ tone.release() }, 2000)
    }
}
