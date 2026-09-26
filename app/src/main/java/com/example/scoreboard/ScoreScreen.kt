package com.example.scoreboard

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.unit.dp

// Hauteurs fixes pour que toutes les colonnes restent alignées entre elles.
private val RANK_ROW_HEIGHT = 40.dp
private val NAME_ROW_HEIGHT = 48.dp
private val SCORE_ROW_HEIGHT = 56.dp
private val TOTAL_ROW_HEIGHT = 56.dp
private val LABEL_COLUMN_WIDTH = 90.dp
private val PLAYER_COLUMN_WIDTH = 110.dp

@Composable
fun ScoreScreen(viewModel: ScoreViewModel) {
    val players = viewModel.players
    val scores = viewModel.scores
    val rankingOrder = viewModel.rankingOrder()

    AppBackground {
        Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            Text(
                text = "Feuille de scores",
                style = MaterialTheme.typography.headlineSmall,
                color = Color.White,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            Card(
                colors = CardDefaults.cardColors(
                    containerColor = Color.White.copy(alpha = 0.93f)
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .horizontalScroll(rememberScrollState())
                        .padding(8.dp)
                ) {

                    // Colonne fixe de gauche : libellés des manches + "Total"
                    Column {
                        Box(Modifier.width(LABEL_COLUMN_WIDTH).height(RANK_ROW_HEIGHT))
                        Box(Modifier.width(LABEL_COLUMN_WIDTH).height(NAME_ROW_HEIGHT))
                        scores.forEachIndexed { index, _ ->
                            Box(
                                modifier = Modifier.width(LABEL_COLUMN_WIDTH).height(SCORE_ROW_HEIGHT),
                                contentAlignment = Alignment.CenterStart
                            ) {
                                Text("Manche ${index + 1}")
                            }
                        }
                        Box(
                            modifier = Modifier.width(LABEL_COLUMN_WIDTH).height(TOTAL_ROW_HEIGHT),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            Text("Total", fontWeight = FontWeight.Bold)
                        }
                    }

                    // Une colonne par joueur
                    players.forEachIndexed { playerIndex, name ->
                        val rank = viewModel.rankOf(playerIndex)
                        Column {
                            // Rang + trophée pour le 1er
                            Box(
                                modifier = Modifier.width(PLAYER_COLUMN_WIDTH).height(RANK_ROW_HEIGHT),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    if (rank == 1 && rankingOrder.isNotEmpty()) {
                                        Icon(
                                            imageVector = Icons.Filled.EmojiEvents,
                                            contentDescription = "Premier",
                                            tint = Color(0xFFFFC107),
                                            modifier = Modifier.padding(end = 4.dp)
                                        )
                                    }
                                    Text("#$rank", fontWeight = FontWeight.Bold)
                                }
                            }

                            // Nom du joueur
                            Box(
                                modifier = Modifier.width(PLAYER_COLUMN_WIDTH).height(NAME_ROW_HEIGHT),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(name, fontWeight = FontWeight.SemiBold, maxLines = 1)
                            }

                            // Une cellule de saisie par manche
                            scores.forEachIndexed { roundIndex, round ->
                                Box(
                                    modifier = Modifier.width(PLAYER_COLUMN_WIDTH).height(SCORE_ROW_HEIGHT),
                                    contentAlignment = Alignment.Center
                                ) {
                                    ScoreCell(
                                        value = round.getOrNull(playerIndex),
                                        onValueChange = { newValue ->
                                            viewModel.updateScore(roundIndex, playerIndex, newValue)
                                        }
                                    )
                                }
                            }

                            // Total du joueur
                            Box(
                                modifier = Modifier
                                    .width(PLAYER_COLUMN_WIDTH)
                                    .height(TOTAL_ROW_HEIGHT)
                                    .background(MaterialTheme.colorScheme.surfaceVariant),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = viewModel.totalFor(playerIndex).toString(),
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/** Champ de saisie numérique pour une case du tableau. */
@Composable
private fun ScoreCell(
    value: Int?,
    onValueChange: (Int?) -> Unit
) {
    var text by remember(value) { mutableStateOf(value?.toString() ?: "") }

    OutlinedTextField(
        value = text,
        onValueChange = { newText ->
            val digitsOnly = newText.filter { it.isDigit() }
            text = digitsOnly
            onValueChange(digitsOnly.toIntOrNull())
        },
        modifier = Modifier.fillMaxWidth().padding(4.dp),
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
    )
}
