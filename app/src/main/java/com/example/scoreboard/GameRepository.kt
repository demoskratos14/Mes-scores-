package com.example.scoreboard

import android.content.Context
import androidx.core.content.edit
import org.json.JSONArray
import org.json.JSONObject
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
            allowNegativeScores = false
        ),
        GameRules(
            id = "builtin_skyjo",
            name = "Skyjo",
            lowestWins = true,
            allowNegativeScores = true
        ),
        GameRules(
            id = "builtin_golf",
            name = "Golf / à points (le plus petit score gagne)",
            lowestWins = true,
            allowNegativeScores = false
        )
    )

    fun customGames(): List<GameRules> {
        val json = prefs.getString(KEY_CUSTOM_GAMES, null) ?: return emptyList()
        return try {
            val array = JSONArray(json)
            (0 until array.length()).map { i ->
                val obj = array.getJSONObject(i)
                GameRules(
                    id = obj.getString("id"),
                    name = obj.getString("name"),
                    lowestWins = obj.getBoolean("lowestWins"),
                    allowNegativeScores = obj.getBoolean("allowNegativeScores")
                )
            }
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
        games.forEach { g ->
            val obj = JSONObject()
            obj.put("id", g.id)
            obj.put("name", g.name)
            obj.put("lowestWins", g.lowestWins)
            obj.put("allowNegativeScores", g.allowNegativeScores)
            array.put(obj)
        }
        prefs.edit { putString(KEY_CUSTOM_GAMES, array.toString()) }
    }
}
