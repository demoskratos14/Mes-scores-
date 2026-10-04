package com.example.scoreboard

import android.content.Context
import androidx.core.content.edit
import org.json.JSONArray
import java.util.UUID

/**
 * Gère les jeux disponibles : quelques jeux prédéfinis, plus les jeux
 * personnalisés créés par l'utilisateur, sauvegardés dans les SharedPreferences
 * (donc conservés d'une ouverture de l'app à l'autre, sans dépendance externe).
 */
class GameRepository(context: Context) {

    private val prefs = context.getSharedPreferences("mes_scores_games", Context.MODE_PRIVATE)

    private companion object {
        const val KEY_CUSTOM_GAMES = "custom_games"
    }

    fun builtInGames(): List<GameRules> = listOf(
        GameRules(
            id = "builtin_generic",
            name = "Jeu classique",
            lowestWins = false,
            allowNegativeScores = false,
            scoreMode = ScoreMode.TABLE
        ),
        GameRules(
            id = "builtin_skyjo",
            name = "Skyjo",
            lowestWins = true,
            allowNegativeScores = true,
            scoreMode = ScoreMode.TABLE,
            endCondition = EndCondition(type = EndConditionType.SCORE_THRESHOLD, scoreThreshold = 100, stopImmediately = false)
        ),
        GameRules(
            id = "builtin_tarot",
            name = "Tarot",
            lowestWins = false,
            allowNegativeScores = true,
            scoreMode = ScoreMode.VARIABLE_TEAMS,
            teamMode = TeamMode.VARIABLE_PER_ROUND,
            scoringFormula = ScoringFormula.MULTIPLIER,
            multipliers = listOf(
                GameRules.NORMAL_MULTIPLIER.copy(label = "Petite"),
                ScoreMultiplier(id = "tarot_garde", label = "Garde", factor = 2),
                ScoreMultiplier(id = "tarot_garde_sans", label = "Garde sans", factor = 4),
                ScoreMultiplier(id = "tarot_garde_contre", label = "Garde contre", factor = 6)
            )
        ),
        GameRules(
            id = "builtin_belote",
            name = "Belote (à 501 points)",
            lowestWins = false,
            allowNegativeScores = false,
            scoreMode = ScoreMode.TABLE,
            endCondition = EndCondition(type = EndConditionType.SCORE_THRESHOLD, scoreThreshold = 501, stopImmediately = false)
        ),
        GameRules(
            id = "builtin_rami",
            name = "Rami",
            lowestWins = true,
            allowNegativeScores = false,
            scoreMode = ScoreMode.TABLE,
            endCondition = EndCondition(type = EndConditionType.SCORE_THRESHOLD, scoreThreshold = 500, stopImmediately = false)
        ),
        GameRules(
            id = "builtin_uno",
            name = "Uno",
            lowestWins = false,
            allowNegativeScores = false,
            scoreMode = ScoreMode.TABLE
        )
    )

    fun customGames(): List<GameRules> {
        val json = prefs.getString(KEY_CUSTOM_GAMES, null) ?: return emptyList()
        return try {
            val array = JSONArray(json)
            (0 until array.length()).map { i -> GameRules.fromJson(array.getJSONObject(i)) }
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun allGames(): List<GameRules> = builtInGames() + customGames()

    fun saveCustomGame(game: GameRules) {
        val current = customGames().toMutableList()
        current.add(game)
        persist(current)
    }

    fun newId(): String = UUID.randomUUID().toString()

    private fun persist(games: List<GameRules>) {
        val array = JSONArray()
        games.forEach { array.put(it.toJson()) }
        prefs.edit { putString(KEY_CUSTOM_GAMES, array.toString()) }
    }
}
