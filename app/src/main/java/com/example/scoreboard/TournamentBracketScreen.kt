package com.example.scoreboard

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

private val ROUND_COLUMN_WIDTH = 190.dp

@Composable
fun TournamentBracketScreen(
    viewModel: TournamentViewModel,
    onBack: () -> Unit
) {
    val players = viewModel.participants
    val rounds = viewModel.rounds

    AppBackground {
        Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            TextButton(onClick = onBack) { Text("← Retour") }

            Text(
                text = "Championnat",
                style = MaterialTheme.typography.headlineSmall,
                color = Color.White,
                modifier = Modifier.padding(bottom = 4.dp)
            )

            if (viewModel.finished) {
                val podium = viewModel.podium()
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF59D)),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text("Classement final", style = MaterialTheme.typography.labelMedium)
                        podium.forEachIndexed { place, playerIndex ->
                            val name = players.getOrNull(playerIndex) ?: "?"
                            Text(
                                text = when (place) {
                                    0 -> "🏆 Champion : $name"
                                    1 -> "🥈 2e place : $name"
                                    2 -> "🥉 3e place : $name"
                                    else -> "4e place : $name"
                                },
                                fontWeight = if (place == 0) FontWeight.Bold else FontWeight.Normal,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
            } else {
                Text(
                    text = "Touche le nom du vainqueur de chaque match.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.85f),
                    modifier = Modifier.padding(bottom = 12.dp)
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                rounds.forEachIndexed { roundIndex, round ->
                    Column(
                        modifier = Modifier
                            .width(ROUND_COLUMN_WIDTH)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        val title = roundTitle(round, roundIndex)
                        if (title.isNotEmpty()) {
                            Text(
                                text = title,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                        round.forEachIndexed { matchIndex, match ->
                            MatchCard(
                                match = match,
                                players = players,
                                onPick = { winner -> viewModel.setWinner(roundIndex, matchIndex, winner) }
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun roundTitle(round: List<TournamentMatch>, roundIndex: Int): String = when {
    round.any { it.label != null } -> ""
    round.size == 1 -> "Finale"
    round.size == 2 -> "Demi-finales"
    round.size == 4 -> "Quarts de finale"
    else -> "Tour ${roundIndex + 1}"
}

@Composable
private fun MatchCard(
    match: TournamentMatch,
    players: List<String>,
    onPick: (Int) -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.93f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            match.label?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            PlayerRow(
                name = match.playerAIndex?.let { players.getOrNull(it) },
                isWinner = match.winnerIndex != null && match.winnerIndex == match.playerAIndex,
                clickable = match.winnerIndex == null && !match.isBye,
                onClick = { match.playerAIndex?.let(onPick) }
            )
            PlayerRow(
                name = match.playerBIndex?.let { players.getOrNull(it) },
                isWinner = match.winnerIndex != null && match.winnerIndex == match.playerBIndex,
                clickable = match.winnerIndex == null && !match.isBye,
                onClick = { match.playerBIndex?.let(onPick) }
            )
        }
    }
}

@Composable
private fun PlayerRow(
    name: String?,
    isWinner: Boolean,
    clickable: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(if (isWinner) Color(0xFFC8E6C9) else Color.Transparent)
            .then(if (clickable) Modifier.clickable { onClick() } else Modifier)
            .padding(horizontal = 8.dp, vertical = 6.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Text(
            text = name ?: "—",
            fontWeight = if (isWinner) FontWeight.Bold else FontWeight.Normal,
            color = if (name == null) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface
        )
    }
}
