package com.example.scoreboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp

@Composable
fun SetupScreen(
    onNext: (List<String>) -> Unit
) {
    var playerCountText by remember { mutableStateOf("2") }
    val playerCount = (playerCountText.toIntOrNull() ?: 2).coerceIn(1, 12)

    var names by remember { mutableStateOf(List(playerCount) { "" }) }

    // Ajuste la taille de la liste de noms quand le nombre de joueurs change,
    // en conservant les noms déjà saisis.
    LaunchedEffect(playerCount) {
        names = List(playerCount) { i -> names.getOrElse(i) { "" } }
    }

    AppBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = Color.White.copy(alpha = 0.93f)
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "Nouvelle partie",
                        style = MaterialTheme.typography.headlineMedium
                    )

                    OutlinedTextField(
                        value = playerCountText,
                        onValueChange = { input -> playerCountText = input.filter { it.isDigit() } },
                        label = { Text("Nombre de joueurs") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 320.dp)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        names.forEachIndexed { index, name ->
                            OutlinedTextField(
                                value = name,
                                onValueChange = { newName ->
                                    names = names.toMutableList().also { it[index] = newName }
                                },
                                label = { Text("Nom du joueur ${index + 1}") },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }

                    Button(
                        onClick = {
                            val finalNames = names.mapIndexed { i, n -> n.ifBlank { "Joueur ${i + 1}" } }
                            onNext(finalNames)
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Continuer")
                    }
                }
            }
        }
    }
}
