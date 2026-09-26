package com.example.scoreboard

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
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
    // afin d'être partagé entre l'écran de configuration et le tableau de scores.
    val viewModel: ScoreViewModel = viewModel()

    NavHost(navController = navController, startDestination = "setup") {
        composable("setup") {
            SetupScreen(
                viewModel = viewModel,
                onStart = { navController.navigate("score") }
            )
        }
        composable("score") {
            ScoreScreen(viewModel = viewModel)
        }
    }
}
