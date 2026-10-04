package com.example.scoreboard

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import kotlin.math.abs

private val POINTS_REQUIRED_BY_BOUTS = listOf(56, 51, 41, 36)

/** Total des points d'un jeu de Tarot : avoir 91 points signifie avoir remporté tous les plis. */
private const val TOTAL_POINTS = 91

/**
 * Prime accordée aux défenseurs quand ils remportent tous les plis (chelem de la défense).
 * Ce n'est pas une prime du règlement officiel mais une règle maison courante : changer
 * cette valeur pour l'ajuster (0 pour la désactiver).
 */
private const val DEFENSE_SLAM_PRIZE = 200

private fun signed(value: Int): String = when {
    value > 0 -> "+$value"
    value < 0 -> "−${-value}"
    else -> "0"
}

/** Nombre d'atouts à montrer pour une poignée simple / double / triple, selon le nombre de joueurs. */
private fun handfulThresholds(playerCount: Int): String? = when (playerCount) {
    3 -> "13 / 15 / 18"
    4 -> "10 / 13 / 15"
    5 -> "8 / 10 / 13"
    else -> null
}

/** Rangée de choix exclusifs, défilante horizontalement si elle ne tient pas à l'écran. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ChoiceRow(options: List<String>, selected: Int?, onSelect: (Int) -> Unit) {
    Row(
        modifier = Modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        options.forEachIndexed { index, label ->
            FilterChip(
                selected = selected == index,
                onClick = { onSelect(index) },
                label = { Text(label) }
            )
        }
    }
}

/**
 * Saisie d'une manche de Tarot avec calcul automatique du score selon les règles officielles :
 * contrat, bouts, points réalisés, petit au bout, poignée et chelem. Le résultat est réparti
 * entre le preneur, son éventuel partenaire (à 5) et les défenseurs, de façon à ce que la somme
 * des scores d'une manche soit toujours nulle.
 */
@Composable
fun NewTarotRoundScreen(
    viewModel: ScoreViewModel,
    onDone: () -> Unit
) {
    val players = viewModel.players
    val multipliers = viewModel.gameRules.multipliers

    var takerIndex by remember { mutableStateOf<Int?>(null) }
    // Index dans la liste des "autres joueurs" ; null = pas de partenaire (preneur seul).
    var partnerIndex by remember { mutableStateOf<Int?>(null) }
    var multiplierIndex by remember { mutableStateOf(0) }
    var bouts by remember { mutableStateOf(0) }
    var pointsText by remember { mutableStateOf("") }
    var petitAuBout by remember { mutableStateOf(0) } // 0 aucun, 1 preneur, 2 défense
    var handful by remember { mutableStateOf(0) }     // 0 aucune, 1 simple, 2 double, 3 triple
    var slamAnnounced by remember { mutableStateOf(false) } // chelem annoncé par le preneur
    var defenseSlam by remember { mutableStateOf(false) }   // la défense a fait tous les plis

    val taker = takerIndex
    val points = pointsText.toIntOrNull()?.takeIf { it in 0..91 }
    val hasPartnerChoice = players.size >= 5
    val partner = if (hasPartnerChoice && partnerIndex != taker) partnerIndex else null
    val multiplier = multipliers.getOrElse(multiplierIndex) { multipliers.first() }

    // ----- Calcul -----
    val required = POINTS_REQUIRED_BY_BOUTS[bouts]
    // Le preneur ne peut faire 91 points que s'il a remporté tous les plis : le chelem est
    // donc déduit automatiquement des points saisis, ce qui évite toute incohérence.
    val slamSucceeded = points == TOTAL_POINTS
    val contractFailed = points != null && points < required
    val defenseSlamActive = defenseSlam && contractFailed
    val slamScore = when {
        slamSucceeded -> if (slamAnnounced) 400 else 200
        defenseSlamActive -> -DEFENSE_SLAM_PRIZE
        slamAnnounced && points != null -> -200 // annoncé mais raté
        else -> 0
    }
    var perDefender = 0
    var success = true
    var difference = 0
    var deltas: List<Int>? = null
    if (taker != null && points != null) {
        difference = points - required
        success = difference >= 0
        val contractScore = (25 + abs(difference)) * multiplier.factor * (if (success) 1 else -1)
        val petitScore = when (petitAuBout) {
            1 -> 10 * multiplier.factor
            2 -> -10 * multiplier.factor
            else -> 0
        }
        val handfulScore = listOf(0, 20, 30, 40)[handful] * (if (success) 1 else -1)
        perDefender = contractScore + petitScore + handfulScore + slamScore

        val attackers = setOfNotNull(taker, partner)
        val defenderCount = players.size - attackers.size
        deltas = players.indices.map { player ->
            when {
                player == taker -> perDefender * (defenderCount - if (partner != null) 1 else 0)
                player == partner -> perDefender
                else -> -perDefender
            }
        }
    }
    val canValidate = taker != null && points != null && deltas != null

    AppBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(start = 16.dp, end = 16.dp, top = 72.dp, bottom = 24.dp)
                .imePadding()
        ) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.93f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text("Nouvelle manche", style = MaterialTheme.typography.headlineMedium)

                    Text("Preneur", style = MaterialTheme.typography.titleSmall)
                    ChoiceRow(players, takerIndex) { takerIndex = it }

                    if (hasPartnerChoice) {
                        Text("Joueur appelé (partenaire)", style = MaterialTheme.typography.titleSmall)
                        val others = players.indices.filter { it != taker }
                        ChoiceRow(
                            options = listOf("Aucun (seul)") + others.map { players[it] },
                            selected = partner?.let { others.indexOf(it) + 1 } ?: 0
                        ) { choice -> partnerIndex = if (choice == 0) null else others[choice - 1] }
                    }

                    Text("Contrat", style = MaterialTheme.typography.titleSmall)
                    ChoiceRow(
                        options = multipliers.map { "${it.label} ×${it.factor}" },
                        selected = multiplierIndex
                    ) { multiplierIndex = it }

                    Text("Bouts du preneur", style = MaterialTheme.typography.titleSmall)
                    ChoiceRow(listOf("0 bout", "1 bout", "2 bouts", "3 bouts"), bouts) { bouts = it }
                    Text(
                        "Contrat à réaliser : $required points",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    OutlinedTextField(
                        value = pointsText,
                        onValueChange = { pointsText = it.filter { c -> c.isDigit() }.take(2) },
                        label = { Text("Points réalisés par le preneur (0 à 91)") },
                        isError = pointsText.isNotEmpty() && points == null,
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Text("Petit au bout", style = MaterialTheme.typography.titleSmall)
                    ChoiceRow(listOf("Aucun", "Preneur", "Défense"), petitAuBout) { petitAuBout = it }

                    Text("Poignée", style = MaterialTheme.typography.titleSmall)
                    ChoiceRow(
                        listOf("Aucune", "Simple 20", "Double 30", "Triple 40"),
                        handful
                    ) { handful = it }
                    handfulThresholds(players.size)?.let {
                        Text(
                            "Atouts à montrer (simple / double / triple) : $it",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Text("Chelem", style = MaterialTheme.typography.titleSmall)
                    ChoiceRow(
                        options = listOf("Non annoncé", "Annoncé par le preneur"),
                        selected = if (slamAnnounced) 1 else 0
                    ) { slamAnnounced = it == 1 }
                    if (contractFailed) {
                        ChoiceRow(
                            options = listOf("La défense n'a pas fait tous les plis", "Chelem de la défense"),
                            selected = if (defenseSlam) 1 else 0
                        ) { defenseSlam = it == 1 }
                    }
                    val slamHint = when {
                        slamSucceeded && slamAnnounced -> "91 points : chelem annoncé réussi (+400)"
                        slamSucceeded -> "91 points : chelem réussi non annoncé (+200)"
                        defenseSlamActive -> "Chelem de la défense : +$DEFENSE_SLAM_PRIZE pour les défenseurs (règle maison)"
                        slamAnnounced && points != null -> "Chelem annoncé mais raté (−200)"
                        else -> "Le chelem réussi est ajouté automatiquement à 91 points."
                    }
                    Text(
                        slamHint,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    // ----- Aperçu du résultat -----
                    val preview = deltas
                    if (taker != null && points != null && preview != null) {
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = if (success) Color(0xFFE8F5E9) else Color(0xFFFFEBEE)
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = if (success) {
                                        "Contrat réussi de $difference point(s)"
                                    } else {
                                        "Contrat chuté de ${-difference} point(s)"
                                    },
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    "Score par défenseur : ${signed(perDefender)}",
                                    style = MaterialTheme.typography.bodySmall
                                )
                                players.forEachIndexed { index, name ->
                                    Text("$name : ${signed(preview[index])}")
                                }
                            }
                        }
                    }

                    Button(
                        onClick = {
                            val result = deltas ?: return@Button
                            val attackers = setOfNotNull(taker, partner)
                            val team = attackers.joinToString(" + ") { players[it] }
                            val label = "$team · ${multiplier.label} · " +
                                "$bouts bout${if (bouts > 1) "s" else ""} · $points pts " +
                                (if (success) "(réussi)" else "(chuté)") +
                                when {
                                    slamSucceeded -> " · chelem"
                                    defenseSlamActive -> " · chelem de la défense"
                                    else -> ""
                                }
                            viewModel.addDetailedTeamRound(
                                teamALabel = label,
                                teamAPlayers = attackers,
                                value = perDefender,
                                deltas = result
                            )
                            onDone()
                        },
                        enabled = canValidate,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Valider la manche")
                    }
                }
            }
        }
    }
}
