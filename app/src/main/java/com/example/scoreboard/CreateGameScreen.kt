package com.example.scoreboard

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp

/** Brouillon d'une règle en cours de saisie dans le formulaire (multiplicateur + bonus). */
private data class RuleDraft(val label: String, val factorText: String, val bonusText: String)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateGameScreen(
    repository: GameRepository,
    onGameCreated: (GameRules) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var scoreMode by remember { mutableStateOf(ScoreMode.TABLE) }
    var lowestWins by remember { mutableStateOf(false) }
    var allowNegative by remember { mutableStateOf(false) }
    var ruleDrafts by remember { mutableStateOf(listOf<RuleDraft>()) }
    var endConditionType by remember { mutableStateOf(EndConditionType.NONE) }
    var endValueText by remember { mutableStateOf("") }

    AppBackground {
        Column(
            modifier = Modifier.fillMaxSize().padding(24.dp).imePadding(),
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

                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("Mode de score", style = MaterialTheme.typography.titleSmall)
                        Row(
                            modifier = Modifier.horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            FilterChip(
                                selected = scoreMode == ScoreMode.TABLE,
                                onClick = { scoreMode = ScoreMode.TABLE },
                                label = { Text("Tableau (manches)") }
                            )
                            FilterChip(
                                selected = scoreMode == ScoreMode.COUNTER,
                                onClick = { scoreMode = ScoreMode.COUNTER },
                                label = { Text("Compteur (+1/-1)") }
                            )
                            FilterChip(
                                selected = scoreMode == ScoreMode.VARIABLE_TEAMS,
                                onClick = { scoreMode = ScoreMode.VARIABLE_TEAMS },
                                label = { Text("Équipes variables") }
                            )
                        }
                        Text(
                            text = when (scoreMode) {
                                ScoreMode.TABLE -> "Manches numérotées, une colonne par joueur (comme aujourd'hui)."
                                ScoreMode.COUNTER -> "Chaque joueur a un total, avec des boutons +1/-1. Pas de manches."
                                ScoreMode.VARIABLE_TEAMS -> "À chaque manche, tu choisis qui est dans quelle équipe (ex: Tarot avec appel du roi)."
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    if (scoreMode != ScoreMode.COUNTER) {
                        SettingRow(
                            title = "Le score le plus bas gagne",
                            subtitle = "Comme à Skyjo, au lieu du plus haut",
                            checked = lowestWins,
                            onCheckedChange = { lowestWins = it }
                        )

                        SettingRow(
                            title = "Autoriser les scores négatifs",
                            subtitle = "Permet de saisir par exemple -3",
                            checked = allowNegative,
                            onCheckedChange = { allowNegative = it }
                        )

                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Règles de score (optionnel)", style = MaterialTheme.typography.titleSmall)
                            Text(
                                "Ex. pour le Tarot : \"Garde\" ×2. Ex. pour la Belote : \"Capot\" ×1 +250. " +
                                    "Un sélecteur apparaîtra ensuite pour choisir la règle à appliquer.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            ruleDrafts.forEachIndexed { index, draft ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    OutlinedTextField(
                                        value = draft.label,
                                        onValueChange = { newLabel ->
                                            ruleDrafts = ruleDrafts.toMutableList().also {
                                                it[index] = draft.copy(label = newLabel)
                                            }
                                        },
                                        label = { Text("Nom") },
                                        modifier = Modifier.weight(1f)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    OutlinedTextField(
                                        value = draft.factorText,
                                        onValueChange = { newFactor ->
                                            val digitsOnly = newFactor.filter { it.isDigit() }
                                            ruleDrafts = ruleDrafts.toMutableList().also {
                                                it[index] = draft.copy(factorText = digitsOnly)
                                            }
                                        },
                                        label = { Text("×") },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        modifier = Modifier.width(60.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    OutlinedTextField(
                                        value = draft.bonusText,
                                        onValueChange = { newBonus ->
                                            val digitsOnly = newBonus.filter { it.isDigit() }
                                            ruleDrafts = ruleDrafts.toMutableList().also {
                                                it[index] = draft.copy(bonusText = digitsOnly)
                                            }
                                        },
                                        label = { Text("+") },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        modifier = Modifier.width(60.dp)
                                    )
                                    IconButton(onClick = {
                                        ruleDrafts = ruleDrafts.toMutableList().also { it.removeAt(index) }
                                    }) {
                                        Icon(Icons.Filled.Close, contentDescription = "Supprimer cette règle")
                                    }
                                }
                            }

                            TextButton(onClick = { ruleDrafts = ruleDrafts + RuleDraft("", "", "") }) {
                                Text("+ Ajouter une règle")
                            }
                        }
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("Fin de partie", style = MaterialTheme.typography.titleSmall)
                        Row(
                            modifier = Modifier.horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            FilterChip(
                                selected = endConditionType == EndConditionType.NONE,
                                onClick = { endConditionType = EndConditionType.NONE },
                                label = { Text("Aucune") }
                            )
                            FilterChip(
                                selected = endConditionType == EndConditionType.ROUND_COUNT,
                                onClick = { endConditionType = EndConditionType.ROUND_COUNT },
                                label = { Text("Nombre de manches") }
                            )
                            FilterChip(
                                selected = endConditionType == EndConditionType.SCORE_THRESHOLD,
                                onClick = { endConditionType = EndConditionType.SCORE_THRESHOLD },
                                label = { Text("Score atteint") }
                            )
                        }
                        if (endConditionType != EndConditionType.NONE) {
                            OutlinedTextField(
                                value = endValueText,
                                onValueChange = { endValueText = it.filter { c -> c.isDigit() } },
                                label = {
                                    Text(
                                        if (endConditionType == EndConditionType.ROUND_COUNT) {
                                            "Nombre de manches"
                                        } else {
                                            "Score à atteindre"
                                        }
                                    )
                                },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }

                    Button(
                        onClick = {
                            val customRules = ruleDrafts.mapIndexedNotNull { index, draft ->
                                val factor = draft.factorText.toIntOrNull() ?: 1
                                val bonus = draft.bonusText.toIntOrNull() ?: 0
                                if (draft.label.isBlank() || (factor == 1 && bonus == 0)) {
                                    null
                                } else {
                                    ScoreMultiplier(id = "custom_$index", label = draft.label, factor = factor, bonus = bonus)
                                }
                            }
                            val endValue = endValueText.toIntOrNull()
                            val endCondition = when (endConditionType) {
                                EndConditionType.NONE -> EndCondition()
                                EndConditionType.ROUND_COUNT -> EndCondition(type = EndConditionType.ROUND_COUNT, roundCount = endValue)
                                EndConditionType.SCORE_THRESHOLD -> EndCondition(type = EndConditionType.SCORE_THRESHOLD, scoreThreshold = endValue)
                            }
                            val rules = GameRules(
                                id = repository.newId(),
                                name = name.ifBlank { "Jeu sans nom" },
                                lowestWins = lowestWins,
                                allowNegativeScores = allowNegative,
                                multipliers = listOf(GameRules.NORMAL_MULTIPLIER) + customRules,
                                scoreMode = scoreMode,
                                endCondition = endCondition
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
