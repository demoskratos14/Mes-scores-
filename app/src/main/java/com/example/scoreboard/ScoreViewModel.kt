package com.example.scoreboard

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.lifecycle.ViewModel

/**
 * Gère l'état complet d'une partie :
 * - la liste des noms de joueurs
 * - la grille de scores (une SnapshotStateList par manche, un Int? par joueur)
 * - l'ajout automatique d'une nouvelle manche dès qu'un score est saisi
 *   dans la dernière manche existante
 * - le calcul des totaux et du classement
 */
class ScoreViewModel : ViewModel() {

    var players by mutableStateOf<List<String>>(emptyList())
        private set

    var gameRules by mutableStateOf(GameRules(id = "default", name = "Jeu classique"))
        private set

    private val _scores = mutableStateListOf<SnapshotStateList<Int?>>()
    val scores: List<SnapshotStateList<Int?>> get() = _scores

    companion object {
        private const val INITIAL_ROUNDS = 5
    }

    /** Démarre une nouvelle partie avec la liste de noms et les règles de score fournies. */
    fun initGame(playerNames: List<String>, rules: GameRules) {
        players = playerNames
        gameRules = rules
        _scores.clear()
        repeat(INITIAL_ROUNDS) { addRound() }
    }

    private fun addRound() {
        val row = mutableStateListOf<Int?>()
        repeat(players.size) { row.add(null) }
        _scores.add(row)
    }

    /**
     * Met à jour le score d'un joueur pour une manche donnée.
     * Si la manche modifiée est la dernière et qu'elle contient
     * désormais au moins un score, une nouvelle manche vide est ajoutée.
     */
    fun updateScore(round: Int, player: Int, value: Int?) {
        if (round !in _scores.indices || player !in players.indices) return
        _scores[round][player] = value

        if (round == _scores.lastIndex && _scores[round].any { it != null }) {
            addRound()
        }
    }

    /** Total cumulé d'un joueur sur toutes les manches. */
    fun totalFor(player: Int): Int = _scores.sumOf { it.getOrNull(player) ?: 0 }

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
