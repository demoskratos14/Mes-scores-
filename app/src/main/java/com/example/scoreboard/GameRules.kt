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
    /** La partie se termine dès qu'un joueur atteint un score seuil, dans un sens ou l'autre. */
    SCORE_THRESHOLD
}

/** Sens dans lequel un score doit franchir le seuil pour déclencher la fin de partie. */
enum class ThresholdDirection {
    /** La partie s'arrête dès qu'un score atteint ou dépasse le seuil (ex : Belote, Rami). */
    ABOVE,
    /** La partie s'arrête dès qu'un score atteint ou descend sous le seuil (cas plus rare). */
    BELOW
}

/**
 * Condition de fin de partie.
 *
 * @param roundCount utilisé si [type] == ROUND_COUNT : nombre de manches jouées avant la fin.
 * @param scoreThreshold utilisé si [type] == SCORE_THRESHOLD : score qui déclenche la fin.
 * @param thresholdDirection sens dans lequel le seuil doit être franchi (utilisé si
 *   [type] == SCORE_THRESHOLD). Par défaut ABOVE, pour ne rien changer au comportement existant.
 * @param stopImmediately si vrai (par défaut), la partie s'arrête dès que le seuil est franchi,
 *   même en plein milieu d'une manche. Si faux, on termine la manche en cours (pour que tous
 *   les joueurs aient joué le même nombre de tours) avant de considérer la partie terminée.
 *   Utilisé si [type] == SCORE_THRESHOLD.
 * @param tieBreakOnEqualLeaders si vrai, une manche supplémentaire est jouée si plusieurs
 *   joueurs sont à égalité en tête au moment où la partie devrait s'arrêter. Utilisé si
 *   [type] == SCORE_THRESHOLD.
 */
data class EndCondition(
    val type: EndConditionType = EndConditionType.NONE,
    val roundCount: Int? = null,
    val scoreThreshold: Int? = null,
    val thresholdDirection: ThresholdDirection = ThresholdDirection.ABOVE,
    val stopImmediately: Boolean = true,
    val tieBreakOnEqualLeaders: Boolean = false
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

/** Comment les joueurs sont regroupés pour le calcul des scores d'une manche. */
enum class TeamMode {
    /** Chaque joueur marque pour lui-même (ex : Skyjo, Rami, Uno). */
    INDIVIDUAL,
    /** Des équipes fixes tout au long de la partie (ex : Belote coinchée, Bridge classique). */
    FIXED_TEAMS,
    /** La composition des équipes peut changer à chaque manche (ex : Tarot). */
    VARIABLE_PER_ROUND
}

/** Comment le score d'une manche est calculé à partir de la saisie. */
enum class ScoringFormula {
    /** Le score saisi est utilisé tel quel (ex : Skyjo, Rami, Uno). */
    DIRECT_ENTRY,
    /** Le score saisi est multiplié par une règle choisie, plus un bonus fixe (ex : Tarot). */
    MULTIPLIER,
    /** Le score dépend d'un contrat annoncé, réussi ou chuté (ex : Belote coinchée, Bridge). */
    CONTRACT_CONDITIONAL
}

/** À qui s'applique un [BonusRule] au moment où il est coché pour une manche. */
enum class BonusScope {
    /** Le(s) gagnant(s) de la manche. */
    ROUND_WINNER,
    /** Le joueur ou l'équipe qui a annoncé le contrat (preneur). */
    DECLARER,
    /** L'équipe ou les joueurs adverses au preneur. */
    OPPOSING_TEAM,
    /** Tous les joueurs concernés, sans distinction. */
    ALL_PLAYERS
}

/**
 * Un bonus optionnel qu'on peut cocher pour une manche, réutilisable par n'importe quel jeu
 * (ex : "Petit au bout", "Chien", "Belote-rebelote", "Capot", "Poignée").
 *
 * @param points valeur ajoutée (ou retranchée si négative) quand le bonus est coché.
 * @param appliesTo à qui ce bonus profite quand il est coché.
 */
data class BonusRule(
    val id: String,
    val label: String,
    val points: Int,
    val appliesTo: BonusScope
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
 * @param teamMode comment les joueurs sont regroupés pour marquer (individuel, équipes fixes,
 *   équipes variables par manche). Purement descriptif pour l'instant : ne change encore rien
 *   au comportement, qui reste piloté par [scoreMode].
 * @param scoringFormula comment le score d'une manche est calculé (saisie directe, multiplicateur,
 *   contrat). Purement descriptif pour l'instant, comme [teamMode].
 * @param roundBonuses bonus optionnels proposés à la cochée pour ce jeu (ex : "Petit au bout"
 *   pour le Tarot). Non encore appliqués au calcul des scores.
 */
data class GameRules(
    val id: String,
    val name: String,
    val lowestWins: Boolean = false,
    val allowNegativeScores: Boolean = false,
    val multipliers: List<ScoreMultiplier> = listOf(NORMAL_MULTIPLIER),
    val scoreMode: ScoreMode = ScoreMode.TABLE,
    val endCondition: EndCondition = EndCondition(),
    val teamMode: TeamMode = TeamMode.INDIVIDUAL,
    val scoringFormula: ScoringFormula = ScoringFormula.DIRECT_ENTRY,
    val roundBonuses: List<BonusRule> = emptyList()
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

        obj.put("teamMode", teamMode.name)
        obj.put("scoringFormula", scoringFormula.name)
        val bonusesArray = JSONArray()
        roundBonuses.forEach { b ->
            val bObj = JSONObject()
            bObj.put("id", b.id)
            bObj.put("label", b.label)
            bObj.put("points", b.points)
            bObj.put("appliesTo", b.appliesTo.name)
            bonusesArray.put(bObj)
        }
        obj.put("roundBonuses", bonusesArray)

        val endConditionObj = JSONObject()
        endConditionObj.put("type", endCondition.type.name)
        endCondition.roundCount?.let { endConditionObj.put("roundCount", it) }
        endCondition.scoreThreshold?.let { endConditionObj.put("scoreThreshold", it) }
        endConditionObj.put("thresholdDirection", endCondition.thresholdDirection.name)
        endConditionObj.put("stopImmediately", endCondition.stopImmediately)
        endConditionObj.put("tieBreakOnEqualLeaders", endCondition.tieBreakOnEqualLeaders)
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
                    } else null,
                    thresholdDirection = try {
                        ThresholdDirection.valueOf(
                            endConditionJson.optString("thresholdDirection", ThresholdDirection.ABOVE.name)
                        )
                    } catch (e: IllegalArgumentException) {
                        ThresholdDirection.ABOVE
                    },
                    stopImmediately = endConditionJson.optBoolean("stopImmediately", true),
                    tieBreakOnEqualLeaders = endConditionJson.optBoolean("tieBreakOnEqualLeaders", false)
                )
            }

            // Déduit des valeurs cohérentes pour les jeux déjà sauvegardés (Tarot compris),
            // qui n'ont pas encore ces champs dans leur JSON, plutôt qu'un défaut générique faux.
            val teamMode = if (obj.has("teamMode")) {
                try {
                    TeamMode.valueOf(obj.getString("teamMode"))
                } catch (e: IllegalArgumentException) {
                    TeamMode.INDIVIDUAL
                }
            } else if (scoreMode == ScoreMode.VARIABLE_TEAMS) {
                TeamMode.VARIABLE_PER_ROUND
            } else {
                TeamMode.INDIVIDUAL
            }

            val scoringFormula = if (obj.has("scoringFormula")) {
                try {
                    ScoringFormula.valueOf(obj.getString("scoringFormula"))
                } catch (e: IllegalArgumentException) {
                    ScoringFormula.DIRECT_ENTRY
                }
            } else if (multipliers.size > 1) {
                ScoringFormula.MULTIPLIER
            } else {
                ScoringFormula.DIRECT_ENTRY
            }

            val roundBonuses = obj.optJSONArray("roundBonuses")?.let { array ->
                (0 until array.length()).map { i ->
                    val b = array.getJSONObject(i)
                    BonusRule(
                        id = b.getString("id"),
                        label = b.getString("label"),
                        points = b.getInt("points"),
                        appliesTo = try {
                            BonusScope.valueOf(b.optString("appliesTo", BonusScope.ALL_PLAYERS.name))
                        } catch (e: IllegalArgumentException) {
                            BonusScope.ALL_PLAYERS
                        }
                    )
                }
            } ?: emptyList()

            return GameRules(
                id = obj.getString("id"),
                name = obj.getString("name"),
                lowestWins = obj.getBoolean("lowestWins"),
                allowNegativeScores = obj.getBoolean("allowNegativeScores"),
                multipliers = multipliers,
                scoreMode = scoreMode,
                endCondition = endCondition,
                teamMode = teamMode,
                scoringFormula = scoringFormula,
                roundBonuses = roundBonuses
            )
        }
    }
}
