package com.example.scoreboard

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
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

    val context = LocalContext.current
    val gameRepository = remember { GameRepository(context) }

    // Noms saisis à l'étape 1, en attente d'être associés à un jeu à l'étape 2.
    var pendingPlayerNames by remember { mutableStateOf(listOf<String>()) }

    // Route vers l'écran de score adapté au mode du jeu choisi.
    fun scoreRouteFor(rules: GameRules): String = when (rules.scoreMode) {
        ScoreMode.TABLE -> "score"
        ScoreMode.COUNTER -> "counter"
        ScoreMode.VARIABLE_TEAMS -> "teamRounds"
    }

    NavHost(navController = navController, startDestination = "setup") {
        composable("setup") {
            SetupScreen(
                onNext = { names ->
                    pendingPlayerNames = names
                    navController.navigate("chooseGame")
                }
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
            ScoreScreen(viewModel = viewModel)
        }
        composable("counter") {
            CounterScreen(viewModel = viewModel)
        }
        composable("teamRounds") {
            TeamRoundsScreen(
                viewModel = viewModel,
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
}
