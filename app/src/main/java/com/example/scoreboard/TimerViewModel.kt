package com.example.scoreboard

import android.os.SystemClock
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

enum class TimerMode { STOPWATCH, COUNTDOWN }

/**
 * État du chronomètre/minuteur, partagé entre tous les écrans via le bouton flottant.
 * Vit au niveau de l'Activity (comme [ScoreViewModel]) donc continue de tourner et
 * garde son état quand on navigue d'un écran à l'autre.
 */
class TimerViewModel : ViewModel() {

    var mode by mutableStateOf(TimerMode.STOPWATCH)
        private set

    var isRunning by mutableStateOf(false)
        private set

    /** Temps écoulé, utilisé en mode chronomètre. */
    var elapsedMillis by mutableStateOf(0L)
        private set

    /** Durée totale du minuteur, réglable avant de le démarrer. */
    var countdownDurationMillis by mutableStateOf(5 * 60 * 1000L)
        private set

    /** Temps restant, utilisé en mode minuteur. */
    var countdownRemainingMillis by mutableStateOf(countdownDurationMillis)
        private set

    /** Vrai dès que le minuteur vient d'atteindre zéro, jusqu'à la prochaine réinitialisation. */
    var justFinished by mutableStateOf(false)
        private set

    private var tickJob: Job? = null

    /** Appelée une fois quand le minuteur atteint zéro (vibration, son…). */
    var onFinished: (() -> Unit)? = null

    // Le temps est calculé à partir de l'horloge du téléphone et non en ajoutant 1 s à chaque
    // tour de boucle, pour ne pas prendre de retard avec le temps.
    private var runStartRealtime = 0L
    private var baseElapsed = 0L
    private var baseRemaining = 0L

    fun selectMode(newMode: TimerMode) {
        if (isRunning) return
        mode = newMode
    }

    fun setCountdownMinutes(minutes: Int) {
        if (isRunning) return
        val duration = minutes.coerceIn(1, 180) * 60 * 1000L
        countdownDurationMillis = duration
        countdownRemainingMillis = duration
    }

    fun start() {
        if (isRunning) return
        if (mode == TimerMode.COUNTDOWN && countdownRemainingMillis <= 0) {
            countdownRemainingMillis = countdownDurationMillis
        }
        isRunning = true
        justFinished = false
        runStartRealtime = SystemClock.elapsedRealtime()
        baseElapsed = elapsedMillis
        baseRemaining = countdownRemainingMillis
        tickJob = viewModelScope.launch {
            while (isRunning) {
                delay(TICK_MILLIS)
                syncFromClock()
                if (mode == TimerMode.COUNTDOWN && countdownRemainingMillis == 0L) {
                    isRunning = false
                    justFinished = true
                    onFinished?.invoke()
                }
            }
        }
    }

    private fun syncFromClock() {
        val spent = SystemClock.elapsedRealtime() - runStartRealtime
        when (mode) {
            TimerMode.STOPWATCH -> elapsedMillis = baseElapsed + spent
            TimerMode.COUNTDOWN -> countdownRemainingMillis = (baseRemaining - spent).coerceAtLeast(0)
        }
    }

    fun pause() {
        if (isRunning) syncFromClock()
        isRunning = false
        tickJob?.cancel()
    }

    fun reset() {
        pause()
        elapsedMillis = 0L
        countdownRemainingMillis = countdownDurationMillis
        justFinished = false
    }

    private companion object {
        const val TICK_MILLIS = 200L
    }

    override fun onCleared() {
        super.onCleared()
        tickJob?.cancel()
    }
}
