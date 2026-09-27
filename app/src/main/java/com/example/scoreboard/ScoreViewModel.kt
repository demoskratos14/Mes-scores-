package com.example.scoreboard

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.lifecycle.ViewModel
import java.util.UUID

/**
 * État d'une case du tableau (mode TABLE) : la magnitude saisie (toujours positive
 * côté saisie), le signe, et la règle choisie. Chaque champ est observable
 * individuellement par Compose, donc modifier l'un d'eux ne recrée pas l'objet.
 */
class CellState {
    var baseValue by mutableStateOf<Int?>(null)
    var isNegative by mutableStateOf(false)
    var multiplierId by mutableStateOf(GameRules.NORMAL_MULTIPLIER_ID)
}

/**
 * Une manche jouée en mode équipes variables (ex: Tarot) : les index des joueurs
 * de "l'équipe A" (ex: le preneur, ou preneur + appelé) pour CETTE manche, et le
 * score final déjà calculé (signe + multiplicateur + bonus appliqués). L'équipe B
 * regroupe automatiquement tous les autres joueurs et reçoit l'opposé de ce score.
 *
 * Simplification assumée : le score de l'équipe A est appliqué tel quel à chacun de
 * ses membres, et son opposé à chacun des membres de l'équipe B (pas de répartition
 * inégale entre plusieurs défenseurs comme au tarot officiel à 4 joueurs).
 */
data class TeamRound(
    val teamALabel: String,
    val teamAPlayers: Set<Int>,
    val value: Int
)

/**
 * Gère l'état complet d'une partie, quel que soit son [ScoreMode] :
 * - la liste des noms de joueurs et les règles du jeu choisi
 * - une couleur distincte assignée aléatoirement à chaque joueur
 * - mode TABLE : une grille de cases [CellState], avec ajout automatique de manche
 * - mode COUNTER : un total par joueur, modifiable par +1/-1
 * - mode VARIABLE_TEAMS : un historique de [TeamRound]
 */
class ScoreViewModel : ViewModel() {

    var players by mutableStateOf<List<String>>(emptyList())
        private set

    var gameRules by mutableStateOf(GameRules(id = "default", name = "Jeu classique"))
        private set

    /** Une couleur distincte par joueur (même index que [players]), tirée au sort à chaque nouvelle partie. */
    var playerColors by mutableStateOf<List<Color>>(emptyList())
        private set

    /**
     * Identifie la partie en cours dans le journal ([GameHistoryRepository]). Null tant
     * qu'elle n'a jamais été enregistrée ; fixé au premier appel à [snapshot], puis
     * réutilisé pour que les enregistrements suivants mettent à jour la même entrée
     * au lieu d'en créer une nouvelle à chaque fois.
     */
    var currentSaveId: String? by mutableStateOf(null)
        private set

    // --- Mode TABLE ---
    private val _scores = mutableStateListOf<SnapshotStateList<CellState>>()
    val scores: List<SnapshotStateList<CellState>> get() = _scores

    // --- Mode COUNTER ---
    private val _counters = mutableStateListOf<Int>()
    val counters: List<Int> get() = _counters

    // --- Mode VARIABLE_TEAMS ---
    private val _teamRounds = mutableStateListOf<TeamRound>()
    val teamRounds: List<TeamRound> get() = _teamRounds

    companion object {
        private const val INITIAL_ROUNDS = 5

        // Palette de couleurs suffisamment contrastées entre elles pour rester lisibles
        // une fois utilisées comme fond de case ou comme couleur de texte.
        private val COLOR_PALETTE = listOf(
            Color(0xFFE53935), Color(0xFF1E88E5), Color(0xFF43A047), Color(0xFFFB8C00),
            Color(0xFF8E24AA), Color(0xFF00897B), Color(0xFFD81B60), Color(0xFF3949AB),
            Color(0xFF6D4C41), Color(0xFF7CB342), Color(0xFFF4511E), Color(0xFF546E7A)
        )
    }

    /** Démarre une nouvelle partie avec la liste de noms et les règles de score fournies. */
    fun initGame(playerNames: List<String>, rules: GameRules) {
        players = playerNames
        gameRules = rules
        playerColors = assignColors(playerNames.size)
        currentSaveId = null
        _scores.clear()
        _counters.clear()
        _teamRounds.clear()
        when (rules.scoreMode) {
            ScoreMode.TABLE -> repeat(INITIAL_ROUNDS) { addRound() }
            ScoreMode.COUNTER -> repeat(playerNames.size) { _counters.add(0) }
            ScoreMode.VARIABLE_TEAMS -> Unit
        }
    }

    /** Restaure une partie précédemment enregistrée dans le journal, telle quelle. */
    fun loadFromSaved(saved: SavedGame) {
        currentSaveId = saved.id
        players = saved.players
        gameRules = saved.gameRules
        playerColors = if (saved.playerColorsArgb.size == saved.players.size) {
            saved.playerColorsArgb.map { Color(it) }
        } else {
            assignColors(saved.players.size)
        }
        _scores.clear()
        _counters.clear()
        _teamRounds.clear()
        when (saved.gameRules.scoreMode) {
            ScoreMode.TABLE -> {
                val rounds = saved.cellSnapshots
                if (rounds.isNullOrEmpty()) {
                    repeat(INITIAL_ROUNDS) { addRound() }
                } else {
                    rounds.forEach { roundSnapshot ->
                        val row = mutableStateListOf<CellState>()
                        roundSnapshot.forEach { snap ->
                            row.add(
                                CellState().apply {
                                    baseValue = snap.baseValue
                                    isNegative = snap.isNegative
                                    multiplierId = snap.multiplierId
                                }
                            )
                        }
                        _scores.add(row)
                    }
                }
            }
            ScoreMode.COUNTER -> {
                val counters = saved.counters
                if (counters.isNullOrEmpty()) {
                    repeat(saved.players.size) { _counters.add(0) }
                } else {
                    counters.forEach { _counters.add(it) }
                }
            }
            ScoreMode.VARIABLE_TEAMS -> {
                saved.teamRounds?.forEach { _teamRounds.add(it) }
            }
        }
    }

    /** Capture l'état actuel de la partie, prêt à être enregistré dans le journal via [GameHistoryRepository]. */
    fun snapshot(): SavedGame {
        val id = currentSaveId ?: UUID.randomUUID().toString().also { currentSaveId = it }
        return SavedGame(
            id = id,
            savedAt = System.currentTimeMillis(),
            gameRules = gameRules,
            players = players,
            playerColorsArgb = playerColors.map { it.toArgb() },
            cellSnapshots = if (gameRules.scoreMode == ScoreMode.TABLE) {
                _scores.map { round -> round.map { CellSnapshot(it.baseValue, it.isNegative, it.multiplierId) } }
            } else null,
            counters = if (gameRules.scoreMode == ScoreMode.COUNTER) _counters.toList() else null,
            teamRounds = if (gameRules.scoreMode == ScoreMode.VARIABLE_TEAMS) _teamRounds.toList() else null,
            isFinished = isGameOver()
        )
    }

    private fun assignColors(count: Int): List<Color> {
        if (count == 0) return emptyList()
        val shuffled = COLOR_PALETTE.shuffled()
        return List(count) { i -> shuffled[i % shuffled.size] }
    }

    // ---------- Mode TABLE ----------

    private fun addRound() {
        val row = mutableStateListOf<CellState>()
        repeat(players.size) { row.add(CellState()) }
        _scores.add(row)
    }

    /**
     * À appeler après toute modification d'une case (valeur, signe ou règle appliquée).
     * Ajoute une nouvelle manche vide si la dernière manche existante contient
     * désormais au moins un score, sauf si la partie est déjà terminée.
     */
    fun notifyCellChanged(round: Int) {
        if (isGameOver()) return
        if (round == _scores.lastIndex && _scores[round].any { it.baseValue != null }) {
            addRound()
        }
    }

    private fun effectiveValue(base: Int, isNegative: Boolean, multiplierId: String): Int {
        val rule = gameRules.multipliers.find { it.id == multiplierId } ?: GameRules.NORMAL_MULTIPLIER
        val magnitude = base * rule.factor + rule.bonus
        return if (isNegative) -magnitude else magnitude
    }

    private fun effectiveValue(cell: CellState): Int? {
        val base = cell.baseValue ?: return null
        return effectiveValue(base, cell.isNegative, cell.multiplierId)
    }

    // ---------- Mode COUNTER ----------

    fun incrementCounter(player: Int, delta: Int) {
        if (isGameOver()) return
        if (player !in _counters.indices) return
        _counters[player] = _counters[player] + delta
    }

    // ---------- Mode VARIABLE_TEAMS ----------

    /**
     * Ajoute une manche à équipes variables : [teamAPlayers] regroupe les joueurs de
     * l'équipe qui reçoit [baseValue] (signé et multiplié selon [multiplierId]) ;
     * tous les autres joueurs reçoivent l'opposé de ce score.
     */
    fun addTeamRound(teamALabel: String, teamAPlayers: Set<Int>, baseValue: Int, isNegative: Boolean, multiplierId: String) {
        val value = effectiveValue(baseValue, isNegative, multiplierId)
        _teamRounds.add(TeamRound(teamALabel, teamAPlayers, value))
    }

    fun removeTeamRound(index: Int) {
        if (index in _teamRounds.indices) _teamRounds.removeAt(index)
    }

    // ---------- Commun ----------

    /** Total cumulé d'un joueur, quel que soit le mode de jeu. */
    fun totalFor(player: Int): Int = when (gameRules.scoreMode) {
        ScoreMode.TABLE ->
            _scores.sumOf { round -> round.getOrNull(player)?.let { effectiveValue(it) } ?: 0 }
        ScoreMode.COUNTER ->
            _counters.getOrNull(player) ?: 0
        ScoreMode.VARIABLE_TEAMS ->
            _teamRounds.sumOf { round -> if (player in round.teamAPlayers) round.value else -round.value }
    }

    /** Index des joueurs triés du meilleur score au moins bon, selon les règles du jeu. */
    fun rankingOrder(): List<Int> =
        if (gameRules.lowestWins) {
            players.indices.sortedBy { totalFor(it) }
        } else {
            players.indices.sortedByDescending { totalFor(it) }
        }

    /** Rang (1 = premier) d'un joueur donné. */
    fun rankOf(player: Int): Int = rankingOrder().indexOf(player) + 1

    /** Nombre de manches réellement jouées (au moins une case remplie), pour TABLE et VARIABLE_TEAMS. */
    fun roundsPlayed(): Int = when (gameRules.scoreMode) {
        ScoreMode.TABLE -> _scores.count { round -> round.any { it.baseValue != null } }
        ScoreMode.VARIABLE_TEAMS -> _teamRounds.size
        ScoreMode.COUNTER -> 0
    }

    /** Vrai si la condition de fin de partie définie par les règles du jeu est atteinte. */
    fun isGameOver(): Boolean {
        val condition = gameRules.endCondition
        val rawStop = when (condition.type) {
            EndConditionType.NONE -> false
            EndConditionType.ROUND_COUNT -> {
                val limit = condition.roundCount ?: return false
                roundsPlayed() >= limit
            }
            EndConditionType.SCORE_THRESHOLD -> {
                if (!thresholdReached()) {
                    false
                } else if (condition.stopImmediately || gameRules.scoreMode != ScoreMode.TABLE) {
                    // Hors mode TABLE (pas de notion de "manche en cours" à terminer), ou si
                    // demandé explicitement, on s'arrête dès que le seuil est franchi.
                    true
                } else {
                    // On attend que la manche en cours soit complète (tous les joueurs ont
                    // saisi un score) avant de considérer la partie terminée.
                    activeRoundComplete()
                }
            }
        }
        if (!rawStop) return false
        // Manche décisive : si plusieurs joueurs sont à égalité en tête, on ne s'arrête pas.
        if (condition.type == EndConditionType.SCORE_THRESHOLD && condition.tieBreakOnEqualLeaders && hasTiedLeaders()) {
            return false
        }
        return true
    }

    /** Vrai si le seuil de score des règles est franchi par au moins un joueur, dans le sens configuré. */
    private fun thresholdReached(): Boolean {
        val condition = gameRules.endCondition
        val threshold = condition.scoreThreshold ?: return false
        return when (condition.thresholdDirection) {
            ThresholdDirection.ABOVE -> players.indices.any { totalFor(it) >= threshold }
            ThresholdDirection.BELOW -> players.indices.any { totalFor(it) <= threshold }
        }
    }

    /** Vrai si la dernière manche entamée (mode TABLE) a été remplie par tous les joueurs. */
    private fun activeRoundComplete(): Boolean {
        val activeIndex = _scores.indexOfLast { round -> round.any { it.baseValue != null } }
        if (activeIndex == -1) return false
        return _scores[activeIndex].all { it.baseValue != null }
    }

    /** Vrai si plusieurs joueurs sont à égalité à la meilleure place du classement actuel. */
    private fun hasTiedLeaders(): Boolean {
        if (players.isEmpty()) return false
        val totals = players.indices.map { totalFor(it) }
        val best = if (gameRules.lowestWins) totals.min() else totals.max()
        return totals.count { it == best } > 1
    }

    /** Index du joueur en tête si la partie est terminée, sinon null. */
    fun winner(): Int? = if (isGameOver() && players.isNotEmpty()) rankingOrder().firstOrNull() else null
}
