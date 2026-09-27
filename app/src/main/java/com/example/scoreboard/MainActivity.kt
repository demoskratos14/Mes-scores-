package com.example.scoreboard

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Nécessaire pour que Modifier.imePadding() fonctionne correctement et que le
        // contenu remonte automatiquement au-dessus du clavier au lieu d'être masqué.
        enableEdgeToEdge()
        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    ScoreApp()
                }
            }
        }
    }
}

@Composable
fun ScoreApp() {
    val navController = rememberNavController()
    // Le ViewModel est créé ici, au niveau du graphe de navigation,
    // afin d'être partagé entre tous les écrans.
    val viewModel: ScoreViewModel = viewModel()
    // Vit au même niveau que ScoreViewModel pour continuer de tourner sur tous les écrans.
    val timerViewModel: TimerViewModel = viewModel()
    val tournamentViewModel: TournamentViewModel = viewModel()

    val context = LocalContext.current
    val gameRepository = remember { GameRepository(context) }
    val gameHistoryRepository = remember { GameHistoryRepository(context) }

    // Noms saisis à l'étape 1, en attente d'être associés à un jeu à l'étape 2.
    var pendingPlayerNames by remember { mutableStateOf(listOf<String>()) }

    // Route vers l'écran de score adapté au mode du jeu choisi.
    fun scoreRouteFor(rules: GameRules): String = when (rules.scoreMode) {
        ScoreMode.TABLE -> "score"
        ScoreMode.COUNTER -> "counter"
        ScoreMode.VARIABLE_TEAMS -> "teamRounds"
    }

    Box(modifier = Modifier.fillMaxSize()) {
        NavHost(navController = navController, startDestination = "setup") {
            composable("setup") {
                SetupScreen(
                    onNext = { names ->
                        pendingPlayerNames = names
                        navController.navigate("chooseGame")
                    },
                    onOpenJournal = {
                        navController.navigate("journal")
                    },
                    onOpenTournament = {
                        navController.navigate("tournamentSetup")
                    }
                )
            }
            composable("tournamentSetup") {
                TournamentSetupScreen(
                    onNext = { names ->
                        tournamentViewModel.startTournament(names)
                        navController.navigate("tournamentBracket")
                    },
                    onBack = { navController.popBackStack() }
                )
            }
            composable("tournamentBracket") {
                TournamentBracketScreen(
                    viewModel = tournamentViewModel,
                    onBack = { navController.popBackStack("setup", inclusive = false) }
                )
            }
            composable("journal") {
                GameHistoryScreen(
                    repository = gameHistoryRepository,
                    onResumeGame = { saved ->
                        viewModel.loadFromSaved(saved)
                        navController.navigate(scoreRouteFor(saved.gameRules)) {
                            popUpTo("setup")
                        }
                    },
                    onBack = { navController.popBackStack() }
                )
            }
            composable("chooseGame") {
                ChooseGameScreen(
                    repository = gameRepository,
                    onGameChosen = { rules ->
                        viewModel.initGame(pendingPlayerNames, rules)
                        navController.navigate(scoreRouteFor(rules))
                    },
                    onCreateNewGame = {
                        navController.navigate("createGame")
                    }
                )
            }
            composable("createGame") {
                CreateGameScreen(
                    repository = gameRepository,
                    onGameCreated = { rules ->
                        viewModel.initGame(pendingPlayerNames, rules)
                        navController.navigate(scoreRouteFor(rules)) {
                            popUpTo("chooseGame") { inclusive = true }
                        }
                    }
                )
            }
            composable("score") {
                ScoreScreen(viewModel = viewModel, historyRepository = gameHistoryRepository)
            }
            composable("counter") {
                CounterScreen(viewModel = viewModel, historyRepository = gameHistoryRepository)
            }
            composable("teamRounds") {
                TeamRoundsScreen(
                    viewModel = viewModel,
                    historyRepository = gameHistoryRepository,
                    onAddRound = { navController.navigate("newTeamRound") }
                )
            }
            composable("newTeamRound") {
                NewTeamRoundScreen(
                    viewModel = viewModel,
                    onDone = { navController.popBackStack() }
                )
            }
        }

        // Superposé à toutes les pages ci-dessus.
        TimerOverlay(viewModel = timerViewModel)
    }
}
