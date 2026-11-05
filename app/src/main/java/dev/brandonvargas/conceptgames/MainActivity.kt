package dev.brandonvargas.conceptgames

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import dev.brandonvargas.conceptgames.ui.navigation.ConceptGamesNavHost
import dev.brandonvargas.conceptgames.ui.theme.ConceptGamesTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        val container = (application as ConceptGamesApp).container
        setContent {
            CompositionLocalProvider(LocalAppContainer provides container) {
                ConceptGamesTheme {
                    // A root Surface gives every screen the theme's background and readable content color,
                    // including screens without a Scaffold (onboarding, generation progress).
                    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                        ConceptGamesNavHost()
                    }
                }
            }
        }
    }
}
