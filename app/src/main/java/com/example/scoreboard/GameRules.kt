package com.example.scoreboard

/**
 * Règles de score propres à un jeu.
 *
 * @param lowestWins si vrai, le classement favorise le score le plus bas
 *   (ex: Skyjo, golf). Si faux, le score le plus haut gagne (comportement par défaut).
 * @param allowNegativeScores si vrai, un joueur peut saisir un score négatif (ex: -3 à Skyjo).
 */
data class GameRules(
    val id: String,
    val name: String,
    val lowestWins: Boolean = false,
    val allowNegativeScores: Boolean = false
)
