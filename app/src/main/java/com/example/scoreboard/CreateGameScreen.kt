package com.example.scoreboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun CreateGameScreen(
    repository: GameRepository,
    onGameCreated: (GameRules) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var lowestWins by remember { mutableStateOf(false) }
    var allowNegative by remember { mutableStateOf(false) }

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
                    Text("Nouveau jeu", style = MaterialTheme.typography.headlineMedium)

                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Nom du jeu") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    SettingRow(
                        title = "Le score le plus bas gagne",
                        subtitle = "Comme au golf ou à Skyjo, au lieu du plus haut",
                        checked = lowestWins,
                        onCheckedChange = { lowestWins = it }
                    )

                    SettingRow(
                        title = "Autoriser les scores négatifs",
                        subtitle = "Permet de saisir par exemple -3",
                        checked = allowNegative,
                        onCheckedChange = { allowNegative = it }
                    )

                    Button(
                        onClick = {
                            val rules = GameRules(
                                id = repository.newId(),
                                name = name.ifBlank { "Jeu sans nom" },
                                lowestWins = lowestWins,
                                allowNegativeScores = allowNegative
                            )
                            repository.saveCustomGame(rules)
                            onGameCreated(rules)
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Créer et commencer")
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title)
            Text(subtitle, style = MaterialTheme.typography.bodySmall)
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
