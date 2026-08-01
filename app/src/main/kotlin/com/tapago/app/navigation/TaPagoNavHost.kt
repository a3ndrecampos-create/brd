package com.tapago.app.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.tapago.feature.photoshare.presentation.PhotoShareScreen
import com.tapago.feature.tracking.presentation.TrackingScreen
import kotlinx.serialization.Serializable

/**
 * Rotas tipadas do app (kotlinx.serialization), conforme padrão definido
 * na especificação técnica do agente.
 */
sealed interface TaPagoRoute {
    @Serializable
    data object Tracking : TaPagoRoute

    @Serializable
    data class PhotoShare(val sessionId: String) : TaPagoRoute
}

@Composable
fun TaPagoNavHost(navController: NavHostController = rememberNavController()) {
    NavHost(navController = navController, startDestination = "tracking") {
        composable(route = "tracking") {
            TrackingScreen(
                onRunFinished = { sessionId ->
                    navController.navigate("photo_share/$sessionId")
                },
            )
        }
        composable(route = "photo_share/{sessionId}") { backStackEntry ->
            val sessionId = backStackEntry.arguments?.getString("sessionId").orEmpty()
            PhotoShareScreen(
                sessionId = sessionId,
                onDone = { navController.popBackStack("tracking", inclusive = false) },
            )
        }
    }
}
