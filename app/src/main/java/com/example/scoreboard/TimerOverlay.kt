package com.example.scoreboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

private fun formatMillis(ms: Long): String {
    val totalSeconds = (ms / 1000).coerceAtLeast(0)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%02d:%02d".format(minutes, seconds)
}

/**
 * Bouton flottant affiché en superposition sur tous les écrans de l'app (voir MainActivity.kt).
 * Ouvre une fenêtre proposant un chronomètre (temps qui monte) ou un minuteur (compte à rebours).
 * Quand il tourne, un petit badge au-dessus du bouton affiche le temps sans avoir à ouvrir la fenêtre.
 */
@Composable
fun TimerOverlay(viewModel: TimerViewModel) {
    var showDialog by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        contentAlignment = Alignment.BottomEnd
    ) {
        Column(horizontalAlignment = Alignment.End) {
            if (viewModel.isRunning) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.75f)),
                    modifier = Modifier.padding(bottom = 8.dp)
                ) {
                    Text(
                        text = if (viewModel.mode == TimerMode.STOPWATCH) {
                            formatMillis(viewModel.elapsedMillis)
                        } else {
                            formatMillis(viewModel.countdownRemainingMillis)
                        },
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }
            FloatingActionButton(onClick = { showDialog = true }) {
                Icon(Icons.Filled.Timer, contentDescription = "Chronomètre / minuteur")
            }
        }
    }

    if (showDialog) {
        TimerDialog(viewModel = viewModel, onDismiss = { showDialog = false })
    }
}

@Composable
private fun TimerDialog(viewModel: TimerViewModel, onDismiss: () -> Unit) {
    var minutesText by remember { mutableStateOf((viewModel.countdownDurationMillis / 60000).toString()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            OutlinedButton(onClick = onDismiss) { Text("Fermer") }
        },
        title = { Text("Chronomètre") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = viewModel.mode == TimerMode.STOPWATCH,
                        onClick = { viewModel.selectMode(TimerMode.STOPWATCH) },
                        label = { Text("Chronomètre") }
                    )
                    FilterChip(
                        selected = viewModel.mode == TimerMode.COUNTDOWN,
                        onClick = { viewModel.selectMode(TimerMode.COUNTDOWN) },
                        label = { Text("Minuteur") }
                    )
                }

                if (viewModel.mode == TimerMode.COUNTDOWN && !viewModel.isRunning) {
                    OutlinedTextField(
                        value = minutesText,
                        onValueChange = { input ->
                            minutesText = input.filter { it.isDigit() }
                            minutesText.toIntOrNull()?.let { viewModel.setCountdownMinutes(it) }
                        },
                        label = { Text("Durée (minutes)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                val displayMillis = if (viewModel.mode == TimerMode.STOPWATCH) {
                    viewModel.elapsedMillis
                } else {
                    viewModel.countdownRemainingMillis
                }

                Text(
                    text = formatMillis(displayMillis),
                    style = MaterialTheme.typography.displayMedium,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    color = if (viewModel.justFinished) Color(0xFFD32F2F) else MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.fillMaxWidth()
                )

                if (viewModel.justFinished) {
                    Text(
                        text = "Temps écoulé !",
                        color = Color(0xFFD32F2F),
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { if (viewModel.isRunning) viewModel.pause() else viewModel.start() }) {
                        Icon(
                            imageVector = if (viewModel.isRunning) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                            contentDescription = null
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(if (viewModel.isRunning) "Pause" else "Démarrer")
                    }
                    OutlinedButton(onClick = { viewModel.reset() }) {
                        Icon(Icons.Filled.Refresh, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Réinitialiser")
                    }
                }
            }
        }
    )
}
