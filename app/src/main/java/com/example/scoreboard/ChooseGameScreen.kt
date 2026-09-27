package com.example.scoreboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChooseGameScreen(
    repository: GameRepository,
    onGameChosen: (GameRules) -> Unit,
    onCreateNewGame: () -> Unit
) {
    val games = remember { repository.allGames() }
    var selected by remember {
        mutableStateOf(games.firstOrNull { it.id == "builtin_generic" } ?: games.first())
    }
    var expanded by remember { mutableStateOf(false) }

    AppBackground {
        Column(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.93f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text("Quel jeu ?", style = MaterialTheme.typography.headlineMedium)

                    ExposedDropdownMenuBox(
                        expanded = expanded,
                        onExpandedChange = { expanded = it }
                    ) {
                        OutlinedTextField(
                            value = selected.name,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Jeu") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                            modifier = Modifier.menuAnchor().fillMaxWidth()
                        )
                        ExposedDropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false }
                        ) {
                            games.forEach { game ->
                                DropdownMenuItem(
                                    text = {
                                        Column {
                                            Text(game.name)
                                            Text(
                                                text = ruleSummary(game),
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    },
                                    onClick = {
                                        selected = game
                                        expanded = false
                                    }
                                )
                            }
                        }
                    }

                    Text(
                        text = ruleSummary(selected),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    OutlinedButton(
                        onClick = onCreateNewGame,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("+ Créer un nouveau jeu")
                    }

                    Button(
                        onClick = { onGameChosen(selected) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Continuer")
                    }
                }
            }
        }
    }
}

private fun ruleSummary(game: GameRules): String {
    val direction = if (game.lowestWins) "le plus petit score gagne" else "le plus grand score gagne"
    val negative = if (game.allowNegativeScores) "scores négatifs autorisés" else "scores positifs uniquement"
    return "$direction · $negative"
}
