package com.example.scoreboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun CounterScreen(viewModel: ScoreViewModel) {
    val players = viewModel.players
    val rankingOrder = viewModel.rankingOrder()

    AppBackground {
        Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            Text(
                text = "Feuille de scores",
                style = MaterialTheme.typography.headlineSmall,
                color = Color.White
            )
            Text(
                text = viewModel.gameRules.name,
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.85f),
                modifier = Modifier.padding(bottom = 12.dp)
            )

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                players.forEachIndexed { index, name ->
                    val rank = viewModel.rankOf(index)
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.93f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (rank == 1 && rankingOrder.isNotEmpty()) {
                                Icon(
                                    imageVector = Icons.Filled.EmojiEvents,
                                    contentDescription = "Premier",
                                    tint = Color(0xFFFFC107),
                                    modifier = Modifier.padding(end = 6.dp)
                                )
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(name, fontWeight = FontWeight.SemiBold)
                                Text(
                                    "#$rank",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            FilledIconButton(onClick = { viewModel.incrementCounter(index, -1) }) {
                                Icon(Icons.Filled.Remove, contentDescription = "Retirer un point")
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            Text(
                                text = viewModel.totalFor(index).toString(),
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.width(48.dp),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            FilledIconButton(onClick = { viewModel.incrementCounter(index, 1) }) {
                                Icon(Icons.Filled.Add, contentDescription = "Ajouter un point")
                            }
                        }
                    }
                }
            }
        }
    }
}
