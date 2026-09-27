package com.example.scoreboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val historyDateFormat = SimpleDateFormat("d MMM yyyy 'à' HH:mm", Locale.FRENCH)

/**
 * Journal des parties : liste, triée de la plus récente à la plus ancienne, des parties
 * enregistrées via le bouton "Enregistrer" des écrans de score. Une partie en cours peut
 * être reprise là où elle en était ; une partie terminée peut être revue.
 */
@Composable
fun GameHistoryScreen(
    repository: GameHistoryRepository,
    onResumeGame: (SavedGame) -> Unit,
    onBack: () -> Unit
) {
    var games by remember { mutableStateOf(repository.listGames()) }
    var gameToDelete by remember { mutableStateOf<SavedGame?>(null) }

    AppBackground {
        Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            TextButton(onClick = onBack) { Text("← Retour") }

            Text(
                text = "Journal des parties",
                style = MaterialTheme.typography.headlineSmall,
                color = Color.White,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            if (games.isEmpty()) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.93f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Aucune partie enregistrée pour l'instant. Utilise le bouton " +
                            "\"Enregistrer\" pendant une partie pour la retrouver ici.",
                        modifier = Modifier.padding(20.dp)
                    )
                }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(games, key = { it.id }) { game ->
                        GameHistoryRow(
                            game = game,
                            onResume = { onResumeGame(game) },
                            onDelete = { gameToDelete = game }
                        )
                    }
                }
            }
        }
    }

    gameToDelete?.let { game ->
        AlertDialog(
            onDismissRequest = { gameToDelete = null },
            confirmButton = {
                TextButton(onClick = {
                    repository.deleteGame(game.id)
                    games = repository.listGames()
                    gameToDelete = null
                }) { Text("Supprimer") }
            },
            dismissButton = {
                TextButton(onClick = { gameToDelete = null }) { Text("Annuler") }
            },
            title = { Text("Supprimer cette partie ?") },
            text = { Text("${game.gameRules.name} · ${game.players.joinToString(", ")}") }
        )
    }
}

@Composable
private fun GameHistoryRow(
    game: SavedGame,
    onResume: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.93f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = game.gameRules.name,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = if (game.isFinished) "Terminée" else "En cours",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = if (game.isFinished) {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    } else {
                        MaterialTheme.colorScheme.primary
                    }
                )
            }
            Text(
                text = historyDateFormat.format(Date(game.savedAt)),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = game.players.joinToString(", "),
                style = MaterialTheme.typography.bodyMedium
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 4.dp)) {
                Button(onClick = onResume) {
                    Text(if (game.isFinished) "Revoir" else "Reprendre")
                }
                OutlinedButton(onClick = onDelete) {
                    Icon(Icons.Filled.Delete, contentDescription = "Supprimer cette partie")
                }
            }
        }
    }
}
