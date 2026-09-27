package com.example.scoreboard

import org.json.JSONArray
import org.json.JSONObject

/** Comment le tableau de score se comporte pour un jeu donné. */
enum class ScoreMode {
    /** Manches numérotées qui s'ajoutent automatiquement, une colonne par joueur. */
    TABLE,
    /** Liste des joueurs avec un total et des boutons +1/-1, sans notion de manche. */
    COUNTER,
    /** Manches où la composition des équipes peut changer à chaque fois (ex: Tarot). */
    VARIABLE_TEAMS
}

/** Type de condition qui termine la partie. */
enum class EndConditionType {
    /** La partie ne se termine jamais automatiquement. */
    NONE,
    /** La partie se termine après un nombre de manches donné. */
    ROUND_COUNT,
    /** La partie se termine dès qu'un joueur atteint (ou dépasse) un score donné. */
    SCORE_THRESHOLD
}

/**
 * Condition de fin de partie.
 *
 * @param roundCount utilisé si [type] == ROUND_COUNT : nombre de manches jouées avant la fin.
 * @param scoreThreshold utilisé si [type] == SCORE_THRESHOLD : score qui déclenche la fin
 *   dès qu'un joueur l'atteint ou le dépasse.
 */
data class EndCondition(
    val type: EndConditionType = EndConditionType.NONE,
    val roundCount: Int? = null,
    val scoreThreshold: Int? = null
)

/**
 * Une règle de score définie par l'utilisateur, ex : "Petit" ×2, "Garde" ×3 +10.
 * Appliquée au score de base saisi : (base × factor) + bonus, puis signé.
 */
data class ScoreMultiplier(
    val id: String,
    val label: String,
    val factor: Int,
    val bonus: Int = 0
)

/**
 * Règles de score propres à un jeu.
 *
 * @param lowestWins si vrai, le classement favorise le score le plus bas
 *   (ex: Skyjo). Si faux, le score le plus haut gagne (comportement par défaut).
 * @param allowNegativeScores si vrai, un joueur peut saisir un score négatif (ex: -3 à Skyjo).
 * @param multipliers règles disponibles (multiplicateur + bonus fixe). Contient toujours
 *   au moins la règle "Normal" (×1 +0). Utilisé en mode TABLE et VARIABLE_TEAMS.
 * @param scoreMode détermine l'écran de saisie utilisé pour ce jeu.
 * @param endCondition détermine quand la partie est considérée comme terminée.
 */
data class GameRules(
    val id: String,
    val name: String,
    val lowestWins: Boolean = false,
    val allowNegativeScores: Boolean = false,
    val multipliers: List<ScoreMultiplier> = listOf(NORMAL_MULTIPLIER),
    val scoreMode: ScoreMode = ScoreMode.TABLE,
    val endCondition: EndCondition = EndCondition()
) {
    /** Sérialise cette règle en JSON, pour la sauvegarde dans les SharedPreferences. */
    fun toJson(): JSONObject {
        val obj = JSONObject()
        obj.put("id", id)
        obj.put("name", name)
        obj.put("lowestWins", lowestWins)
        obj.put("allowNegativeScores", allowNegativeScores)
        obj.put("scoreMode", scoreMode.name)

        val multipliersArray = JSONArray()
        multipliers.forEach { m ->
            val mObj = JSONObject()
            mObj.put("id", m.id)
            mObj.put("label", m.label)
            mObj.put("factor", m.factor)
            mObj.put("bonus", m.bonus)
            multipliersArray.put(mObj)
        }
        obj.put("multipliers", multipliersArray)

        val endConditionObj = JSONObject()
        endConditionObj.put("type", endCondition.type.name)
        endCondition.roundCount?.let { endConditionObj.put("roundCount", it) }
        endCondition.scoreThreshold?.let { endConditionObj.put("scoreThreshold", it) }
        obj.put("endCondition", endConditionObj)

        return obj
    }

    companion object {
        const val NORMAL_MULTIPLIER_ID = "normal"
        val NORMAL_MULTIPLIER = ScoreMultiplier(NORMAL_MULTIPLIER_ID, "Normal", factor = 1, bonus = 0)

        /** Reconstruit une règle depuis le JSON produit par [toJson]. */
        fun fromJson(obj: JSONObject): GameRules {
            val multipliersJson = obj.optJSONArray("multipliers")
            val multipliers = if (multipliersJson == null || multipliersJson.length() == 0) {
                listOf(NORMAL_MULTIPLIER)
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

            val endConditionJson = obj.optJSONObject("endCondition")
            val endCondition = if (endConditionJson == null) {
                EndCondition()
            } else {
                val type = try {
                    EndConditionType.valueOf(endConditionJson.optString("type", EndConditionType.NONE.name))
                } catch (e: IllegalArgumentException) {
                    EndConditionType.NONE
                }
                EndCondition(
                    type = type,
                    roundCount = if (endConditionJson.has("roundCount") && !endConditionJson.isNull("roundCount")) {
                        endConditionJson.getInt("roundCount")
                    } else null,
                    scoreThreshold = if (endConditionJson.has("scoreThreshold") && !endConditionJson.isNull("scoreThreshold")) {
                        endConditionJson.getInt("scoreThreshold")
                    } else null
                )
            }

            return GameRules(
                id = obj.getString("id"),
                name = obj.getString("name"),
                lowestWins = obj.getBoolean("lowestWins"),
                allowNegativeScores = obj.getBoolean("allowNegativeScores"),
                multipliers = multipliers,
                scoreMode = scoreMode,
                endCondition = endCondition
            )
        }
    }
}
