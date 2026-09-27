package com.example.scoreboard

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.lifecycle.ViewModel

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
 * - mode TABLE : une grille de cases [CellState], avec ajout automatique de manche
 * - mode COUNTER : un total par joueur, modifiable par +1/-1
 * - mode VARIABLE_TEAMS : un historique de [TeamRound]
 */
class ScoreViewModel : ViewModel() {

    var players by mutableStateOf<List<String>>(emptyList())
        private set

    var gameRules by mutableStateOf(GameRules(id = "default", name = "Jeu classique"))
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
    }

    /** Démarre une nouvelle partie avec la liste de noms et les règles de score fournies. */
    fun initGame(playerNames: List<String>, rules: GameRules) {
        players = playerNames
        gameRules = rules
        _scores.clear()
        _counters.clear()
        _teamRounds.clear()
        when (rules.scoreMode) {
            ScoreMode.TABLE -> repeat(INITIAL_ROUNDS) { addRound() }
            ScoreMode.COUNTER -> repeat(playerNames.size) { _counters.add(0) }
            ScoreMode.VARIABLE_TEAMS -> Unit
        }
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
     * désormais au moins un score.
     */
    fun notifyCellChanged(round: Int) {
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
}
