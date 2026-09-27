package com.example.scoreboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun TeamRoundsScreen(
    viewModel: ScoreViewModel,
    onAddRound: () -> Unit
) {
    val players = viewModel.players
    val rankingOrder = viewModel.rankingOrder()
    val rounds = viewModel.teamRounds

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

            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.93f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Totaux et classement
                    players.forEachIndexed { index, name ->
                        val rank = viewModel.rankOf(index)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
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
                            Text("#$rank  $name", modifier = Modifier.weight(1f), fontWeight = FontWeight.SemiBold)
                            Text(viewModel.totalFor(index).toString(), fontWeight = FontWeight.Bold)
                        }
                    }

                    Button(onClick = onAddRound, modifier = Modifier.fillMaxWidth()) {
                        Text("+ Nouvelle manche")
                    }

                    if (rounds.isNotEmpty()) {
                        Text("Historique des manches", style = MaterialTheme.typography.titleSmall)
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 260.dp)
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            rounds.forEachIndexed { index, round ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Manche ${index + 1} — ${round.teamALabel} : ${if (round.value >= 0) "+" else ""}${round.value}",
                                        modifier = Modifier.weight(1f),
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                    IconButton(onClick = { viewModel.removeTeamRound(index) }) {
                                        Icon(Icons.Filled.Close, contentDescription = "Supprimer cette manche")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
