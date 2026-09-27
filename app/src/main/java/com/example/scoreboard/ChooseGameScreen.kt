package com.example.scoreboard

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
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
import androidx.compose.ui.unit.dp

@Composable
fun ChooseGameScreen(
    repository: GameRepository,
    onGameChosen: (GameRules) -> Unit,
    onCreateNewGame: () -> Unit
) {
    // Recalculé à chaque recomposition de cet écran, donc un jeu tout juste
    // créé apparaît bien dans la liste au retour depuis l'écran de création.
    val games = remember { mutableStateOf(repository.allGames()) }.value
    var selected by remember { mutableStateOf<GameRules?>(null) }

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
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("Quel jeu ?", style = MaterialTheme.typography.headlineMedium)

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 320.dp)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        games.forEach { game ->
                            val isSelected = selected?.id == game.id
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { selected = game }
                                    .background(
                                        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                    .padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(selected = isSelected, onClick = { selected = game })
                                Column {
                                    Text(game.name, fontWeight = FontWeight.SemiBold)
                                    Text(
                                        text = ruleSummary(game),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }

                    OutlinedButton(
                        onClick = onCreateNewGame,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("+ Créer un nouveau jeu")
                    }

                    Button(
                        onClick = { selected?.let(onGameChosen) },
                        enabled = selected != null,
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
