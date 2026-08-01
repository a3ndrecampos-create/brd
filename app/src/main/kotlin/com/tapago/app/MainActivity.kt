package com.tapago.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import com.tapago.app.navigation.TaPagoNavHost
import com.tapago.core.designsystem.theme.TaPagoTheme
import dagger.hilt.android.AndroidEntryPoint

/**
 * Activity única do app (padrão single-activity + Navigation Compose).
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            TaPagoTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    val navController = rememberNavController()
                    TaPagoNavHost(navController = navController)
                }
            }
        }
    }
}
