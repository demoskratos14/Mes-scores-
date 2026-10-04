package com.example.scoreboard

import android.content.Context
import androidx.core.content.edit
import org.json.JSONArray
import org.json.JSONObject

/** État figé d'une case du tableau (mode TABLE), pour la sauvegarde d'une partie en cours. */
data class CellSnapshot(
    val baseValue: Int?,
    val isNegative: Boolean,
    val multiplierId: String
) {
    fun toJson(): JSONObject {
        val obj = JSONObject()
        baseValue?.let { obj.put("baseValue", it) }
        obj.put("isNegative", isNegative)
        obj.put("multiplierId", multiplierId)
        return obj
    }

    companion object {
        fun fromJson(obj: JSONObject): CellSnapshot = CellSnapshot(
            baseValue = if (obj.has("baseValue") && !obj.isNull("baseValue")) obj.getInt("baseValue") else null,
            isNegative = obj.optBoolean("isNegative", false),
            multiplierId = obj.optString("multiplierId", GameRules.NORMAL_MULTIPLIER_ID)
        )
    }
}

private fun TeamRound.toJson(): JSONObject {
    val obj = JSONObject()
    obj.put("teamALabel", teamALabel)
    val playersArray = JSONArray()
    teamAPlayers.forEach { playersArray.put(it) }
    obj.put("teamAPlayers", playersArray)
    obj.put("value", value)
    deltas?.let { list ->
        val deltasArray = JSONArray()
        list.forEach { deltasArray.put(it) }
        obj.put("deltas", deltasArray)
    }
    return obj
}

private fun teamRoundFromJson(obj: JSONObject): TeamRound {
    val playersArray = obj.optJSONArray("teamAPlayers")
    val playersSet = if (playersArray == null) {
        emptySet()
    } else {
        (0 until playersArray.length()).map { playersArray.getInt(it) }.toSet()
    }
    return TeamRound(
        teamALabel = obj.getString("teamALabel"),
        teamAPlayers = playersSet,
        value = obj.getInt("value"),
        deltas = obj.optJSONArray("deltas")?.let { array ->
            (0 until array.length()).map { array.getInt(it) }
        }
    )
}

/**
 * Une partie enregistrée dans le journal, en cours ou terminée. [id] identifie la partie
 * de façon stable : ré-enregistrer la même partie en cours met à jour l'entrée existante
 * au lieu d'en créer une nouvelle. Seul le champ correspondant au [ScoreMode] de
 * [gameRules] est rempli ([cellSnapshots] pour TABLE, [counters] pour COUNTER,
 * [teamRounds] pour VARIABLE_TEAMS).
 */
data class SavedGame(
    val id: String,
    val savedAt: Long,
    val gameRules: GameRules,
    val players: List<String>,
    val playerColorsArgb: List<Int>,
    val cellSnapshots: List<List<CellSnapshot>>? = null,
    val counters: List<Int>? = null,
    val teamRounds: List<TeamRound>? = null,
    val isFinished: Boolean
) {
    fun toJson(): JSONObject {
        val obj = JSONObject()
        obj.put("id", id)
        obj.put("savedAt", savedAt)
        obj.put("gameRules", gameRules.toJson())
        obj.put("players", JSONArray(players))
        obj.put("playerColorsArgb", JSONArray(playerColorsArgb))
        obj.put("isFinished", isFinished)

        cellSnapshots?.let { rounds ->
            val roundsArray = JSONArray()
            rounds.forEach { round ->
                val roundArray = JSONArray()
                round.forEach { roundArray.put(it.toJson()) }
                roundsArray.put(roundArray)
            }
            obj.put("cellSnapshots", roundsArray)
        }
        counters?.let { obj.put("counters", JSONArray(it)) }
        teamRounds?.let { rounds ->
            val array = JSONArray()
            rounds.forEach { array.put(it.toJson()) }
            obj.put("teamRounds", array)
        }
        return obj
    }

    companion object {
        fun fromJson(obj: JSONObject): SavedGame {
            val playersArray = obj.getJSONArray("players")
            val players = (0 until playersArray.length()).map { playersArray.getString(it) }

            val colorsArray = obj.optJSONArray("playerColorsArgb")
            val colors = if (colorsArray == null) {
                emptyList()
            } else {
                (0 until colorsArray.length()).map { colorsArray.getInt(it) }
            }

            val cellSnapshots = obj.optJSONArray("cellSnapshots")?.let { roundsArray ->
                (0 until roundsArray.length()).map { i ->
                    val roundArray = roundsArray.getJSONArray(i)
                    (0 until roundArray.length()).map { j -> CellSnapshot.fromJson(roundArray.getJSONObject(j)) }
                }
            }

            val counters = obj.optJSONArray("counters")?.let { array ->
                (0 until array.length()).map { array.getInt(it) }
            }

            val teamRounds = obj.optJSONArray("teamRounds")?.let { array ->
                (0 until array.length()).map { i -> teamRoundFromJson(array.getJSONObject(i)) }
            }

            return SavedGame(
                id = obj.getString("id"),
                savedAt = obj.getLong("savedAt"),
                gameRules = GameRules.fromJson(obj.getJSONObject("gameRules")),
                players = players,
                playerColorsArgb = colors,
                cellSnapshots = cellSnapshots,
                counters = counters,
                teamRounds = teamRounds,
                isFinished = obj.optBoolean("isFinished", false)
            )
        }
    }
}

/**
 * Journal des parties : sauvegarde et relit la liste des parties (en cours ou terminées)
 * dans les SharedPreferences, indépendamment des jeux définis dans [GameRepository].
 */
class GameHistoryRepository(context: Context) {

    private val prefs = context.getSharedPreferences("mes_scores_history", Context.MODE_PRIVATE)

    private companion object {
        const val KEY_SAVED_GAMES = "saved_games"
    }

    /** Parties triées de la plus récente à la plus ancienne. */
    fun listGames(): List<SavedGame> {
        val json = prefs.getString(KEY_SAVED_GAMES, null) ?: return emptyList()
        return try {
            val array = JSONArray(json)
            (0 until array.length())
                .map { i -> SavedGame.fromJson(array.getJSONObject(i)) }
                .sortedByDescending { it.savedAt }
        } catch (e: Exception) {
            emptyList()
        }
    }

    /** Enregistre [game] : remplace l'entrée existante de même id si elle existe, sinon l'ajoute. */
    fun saveGame(game: SavedGame) {
        val current = listGames().filterNot { it.id == game.id }.toMutableList()
        current.add(game)
        persist(current)
    }

    fun deleteGame(id: String) {
        persist(listGames().filterNot { it.id == id })
    }

    private fun persist(games: List<SavedGame>) {
        val array = JSONArray()
        games.forEach { array.put(it.toJson()) }
        prefs.edit { putString(KEY_SAVED_GAMES, array.toString()) }
    }
}
