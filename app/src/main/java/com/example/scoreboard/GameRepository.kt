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
            name = "Jeu classique (tableau)",
            lowestWins = false,
            allowNegativeScores = false,
            scoreMode = ScoreMode.TABLE
        ),
        GameRules(
            id = "builtin_counter",
            name = "Jeu simple (compteur +1/-1)",
            lowestWins = false,
            allowNegativeScores = false,
            scoreMode = ScoreMode.COUNTER
        ),
        GameRules(
            id = "builtin_skyjo",
            name = "Skyjo",
            lowestWins = true,
            allowNegativeScores = true,
            scoreMode = ScoreMode.TABLE
        ),
        GameRules(
            id = "builtin_tarot",
            name = "Tarot",
            lowestWins = false,
            allowNegativeScores = true,
            scoreMode = ScoreMode.VARIABLE_TEAMS,
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
            scoreMode = ScoreMode.TABLE
        ),
        GameRules(
            id = "builtin_rami",
            name = "Rami",
            lowestWins = true,
            allowNegativeScores = false,
            scoreMode = ScoreMode.TABLE
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
            (0 until array.length()).map { i -> gameFromJson(array.getJSONObject(i)) }
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

    private fun gameFromJson(obj: JSONObject): GameRules {
        val multipliersJson = obj.optJSONArray("multipliers")
        val multipliers = if (multipliersJson == null || multipliersJson.length() == 0) {
            listOf(GameRules.NORMAL_MULTIPLIER)
        } else {
            (0 until multipliersJson.length()).map { i ->
                val m = multipliersJson.getJSONObject(i)
                ScoreMultiplier(
                    id = m.getString("id"),
                    label = m.getString("label"),
                    factor = m.getInt("factor"),
                    bonus = m.optInt("bonus", 0)
                )
            }
        }
        val scoreMode = try {
            ScoreMode.valueOf(obj.optString("scoreMode", ScoreMode.TABLE.name))
        } catch (e: IllegalArgumentException) {
            ScoreMode.TABLE
        }
        return GameRules(
            id = obj.getString("id"),
            name = obj.getString("name"),
            lowestWins = obj.getBoolean("lowestWins"),
            allowNegativeScores = obj.getBoolean("allowNegativeScores"),
            multipliers = multipliers,
            scoreMode = scoreMode
        )
    }

    private fun persist(games: List<GameRules>) {
        val array = JSONArray()
        games.forEach { g ->
            val obj = JSONObject()
            obj.put("id", g.id)
            obj.put("name", g.name)
            obj.put("lowestWins", g.lowestWins)
            obj.put("allowNegativeScores", g.allowNegativeScores)
            obj.put("scoreMode", g.scoreMode.name)
            val multipliersArray = JSONArray()
            g.multipliers.forEach { m ->
                val mObj = JSONObject()
                mObj.put("id", m.id)
                mObj.put("label", m.label)
                mObj.put("factor", m.factor)
                mObj.put("bonus", m.bonus)
                multipliersArray.put(mObj)
            }
            obj.put("multipliers", multipliersArray)
            array.put(obj)
        }
        prefs.edit { putString(KEY_CUSTOM_GAMES, array.toString()) }
    }
}
