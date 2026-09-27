package com.example.scoreboard

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
        tickJob = viewModelScope.launch {
            while (isRunning) {
                delay(1000)
                when (mode) {
                    TimerMode.STOPWATCH -> elapsedMillis += 1000
                    TimerMode.COUNTDOWN -> {
                        countdownRemainingMillis = (countdownRemainingMillis - 1000).coerceAtLeast(0)
                        if (countdownRemainingMillis == 0L) {
                            isRunning = false
                            justFinished = true
                        }
                    }
                }
            }
        }
    }

    fun pause() {
        isRunning = false
        tickJob?.cancel()
    }

    fun reset() {
        pause()
        elapsedMillis = 0L
        countdownRemainingMillis = countdownDurationMillis
        justFinished = false
    }

    override fun onCleared() {
        super.onCleared()
        tickJob?.cancel()
    }
}
