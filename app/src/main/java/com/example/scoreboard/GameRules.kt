package com.example.scoreboard

/** Comment le tableau de score se comporte pour un jeu donné. */
enum class ScoreMode {
    /** Manches numérotées qui s'ajoutent automatiquement, une colonne par joueur. */
    TABLE,
    /** Liste des joueurs avec un total et des boutons +1/-1, sans notion de manche. */
    COUNTER,
    /** Manches où la composition des équipes peut changer à chaque fois (ex: Tarot). */
    VARIABLE_TEAMS
}

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
 */
data class GameRules(
    val id: String,
    val name: String,
    val lowestWins: Boolean = false,
    val allowNegativeScores: Boolean = false,
    val multipliers: List<ScoreMultiplier> = listOf(NORMAL_MULTIPLIER),
    val scoreMode: ScoreMode = ScoreMode.TABLE
) {
    companion object {
        const val NORMAL_MULTIPLIER_ID = "normal"
        val NORMAL_MULTIPLIER = ScoreMultiplier(NORMAL_MULTIPLIER_ID, "Normal", factor = 1, bonus = 0)
    }
}
