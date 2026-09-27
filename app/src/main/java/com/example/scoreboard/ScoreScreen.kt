package com.example.scoreboard

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LocalTextStyle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

// Hauteurs fixes pour que toutes les colonnes restent alignées entre elles.
private val RANK_ROW_HEIGHT = 40.dp
private val NAME_ROW_HEIGHT = 48.dp
private val TOTAL_ROW_HEIGHT = 56.dp
private val LABEL_COLUMN_WIDTH = 90.dp
private val MIN_PLAYER_COLUMN_WIDTH = 78.dp
private val MAX_PLAYER_COLUMN_WIDTH = 128.dp

/** Renvoie du noir ou du blanc selon le fond donné, pour rester lisible. */
private fun contentColorFor(background: Color): Color {
    val luminance = 0.299 * background.red + 0.587 * background.green + 0.114 * background.blue
    return if (luminance > 0.6) Color.Black else Color.White
}

@Composable
fun ScoreScreen(viewModel: ScoreViewModel) {
    val players = viewModel.players
    val playerColors = viewModel.playerColors
    val scores = viewModel.scores
    val rankingOrder = viewModel.rankingOrder()
    val multipliers = viewModel.gameRules.multipliers
    val hasCustomMultipliers = multipliers.size > 1
    // Un peu plus de hauteur par case quand le sélecteur de règle est affiché.
    val scoreRowHeight = if (hasCustomMultipliers) 92.dp else 72.dp

    // Nom complet affiché en popup quand une case de nom tronquée est cliquée.
    var expandedNameIndex by remember { mutableStateOf<Int?>(null) }

    // Un seul état de scroll horizontal partagé entre l'en-tête fixe et le corps
    // du tableau, pour que les colonnes restent alignées quand on défile latéralement.
    val horizontalScrollState = rememberScrollState()

    AppBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .imePadding()
        ) {
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

            if (viewModel.isGameOver()) {
                val winner = viewModel.winner()
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF59D)),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                ) {
                    Text(
                        text = "Partie terminée" + (winner?.let { " · ${players[it]} gagne !" } ?: ""),
                        modifier = Modifier.padding(12.dp),
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Card(
                colors = CardDefaults.cardColors(
                    containerColor = Color.White.copy(alpha = 0.93f)
                ),
                modifier = Modifier.fillMaxWidth().weight(1f)
            ) {
                BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                    val playerColumnWidth: Dp = if (players.isEmpty()) {
                        MAX_PLAYER_COLUMN_WIDTH
                    } else {
                        val available = maxWidth - LABEL_COLUMN_WIDTH - 16.dp
                        (available / players.size).coerceIn(MIN_PLAYER_COLUMN_WIDTH, MAX_PLAYER_COLUMN_WIDTH)
                    }

                    Column(modifier = Modifier.fillMaxSize().padding(8.dp)) {
                        // ----- En-tête fixe : rang + nom (toujours visible au-dessus des manches) -----
                        Row(modifier = Modifier.horizontalScroll(horizontalScrollState)) {
                            Column {
                                Box(Modifier.width(LABEL_COLUMN_WIDTH).height(RANK_ROW_HEIGHT))
                                Box(Modifier.width(LABEL_COLUMN_WIDTH).height(NAME_ROW_HEIGHT))
                            }
                            players.forEachIndexed { playerIndex, name ->
                                val rank = viewModel.rankOf(playerIndex)
                                val color = playerColors.getOrElse(playerIndex) { Color.Gray }
                                Column {
                                    Box(
                                        modifier = Modifier.width(playerColumnWidth).height(RANK_ROW_HEIGHT),
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
                                    Box(
                                        modifier = Modifier
                                            .width(playerColumnWidth)
                                            .height(NAME_ROW_HEIGHT)
                                            .background(color)
                                            .clickable { expandedNameIndex = playerIndex },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = name,
                                            color = contentColorFor(color),
                                            fontWeight = FontWeight.SemiBold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.padding(horizontal = 4.dp)
                                        )
                                    }
                                }
                            }
                        }

                        // ----- Corps défilant : une manche par ligne, plus le total -----
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .verticalScroll(rememberScrollState())
                        ) {
                            Row(modifier = Modifier.horizontalScroll(horizontalScrollState)) {
                                Column {
                                    scores.forEachIndexed { index, _ ->
                                        Box(
                                            modifier = Modifier.width(LABEL_COLUMN_WIDTH).height(scoreRowHeight),
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

                                players.forEachIndexed { playerIndex, _ ->
                                    val color = playerColors.getOrElse(playerIndex) { Color.Black }
                                    Column {
                                        scores.forEachIndexed { roundIndex, round ->
                                            Box(
                                                modifier = Modifier.width(playerColumnWidth).height(scoreRowHeight),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                ScoreCell(
                                                    cell = round[playerIndex],
                                                    allowNegative = viewModel.gameRules.allowNegativeScores,
                                                    multipliers = multipliers,
                                                    playerColor = color,
                                                    onChanged = { viewModel.notifyCellChanged(roundIndex) }
                                                )
                                            }
                                        }
                                        Box(
                                            modifier = Modifier
                                                .width(playerColumnWidth)
                                                .height(TOTAL_ROW_HEIGHT)
                                                .background(MaterialTheme.colorScheme.surfaceVariant),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = viewModel.totalFor(playerIndex).toString(),
                                                fontWeight = FontWeight.Bold,
                                                color = color
                                            )
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

    expandedNameIndex?.let { index ->
        AlertDialog(
            onDismissRequest = { expandedNameIndex = null },
            confirmButton = {
                TextButton(onClick = { expandedNameIndex = null }) { Text("OK") }
            },
            title = { Text("Joueur") },
            text = { Text(players.getOrElse(index) { "" }) }
        )
    }
}

/**
 * Champ de saisie numérique pour une case du tableau : bouton +/− pour le signe
 * (si les scores négatifs sont autorisés), et sélecteur de règle de multiplication
 * au-dessus du champ (si le jeu en a défini plusieurs). Le texte saisi est affiché
 * dans la couleur du joueur.
 */
@Composable
private fun ScoreCell(
    cell: CellState,
    allowNegative: Boolean,
    multipliers: List<ScoreMultiplier>,
    playerColor: Color,
    onChanged: () -> Unit
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        if (multipliers.size > 1) {
            var expanded by remember { mutableStateOf(false) }
            val current = multipliers.find { it.id == cell.multiplierId } ?: multipliers.first()
            Box {
                Text(
                    text = "${current.label} ×${current.factor}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier
                        .clickable { expanded = true }
                        .padding(vertical = 2.dp)
                )
                DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                    multipliers.forEach { multiplier ->
                        DropdownMenuItem(
                            text = { Text("${multiplier.label} ×${multiplier.factor}") },
                            onClick = {
                                cell.multiplierId = multiplier.id
                                expanded = false
                                onChanged()
                            }
                        )
                    }
                }
            }
        }

        OutlinedTextField(
            value = cell.baseValue?.toString() ?: "",
            onValueChange = { newText ->
                cell.baseValue = newText.filter { it.isDigit() }.toIntOrNull()
                onChanged()
            },
            textStyle = LocalTextStyle.current.copy(color = playerColor),
            leadingIcon = if (allowNegative) {
                {
                    Text(
                        text = if (cell.isNegative) "−" else "+",
                        fontWeight = FontWeight.Bold,
                        color = if (cell.isNegative) Color(0xFFD32F2F) else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .clickable {
                                cell.isNegative = !cell.isNegative
                                onChanged()
                            }
                            .padding(horizontal = 6.dp)
                    )
                }
            } else null,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 2.dp, vertical = 4.dp),
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
        )
    }
}
