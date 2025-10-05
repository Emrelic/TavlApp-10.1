package com.tavla.tavlapp

import android.os.Bundle
import android.view.View
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.*
import androidx.compose.ui.graphics.Color

class DiceActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.decorView.systemUiVisibility = (
            View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
            View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or
            View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
            View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
            View.SYSTEM_UI_FLAG_FULLSCREEN or
            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
        )

        val gameType = intent.getStringExtra("game_type") ?: "Modern"
        val player1Name = intent.getStringExtra("player1_name") ?: "Oyuncu 1"
        val player2Name = intent.getStringExtra("player2_name") ?: "Oyuncu 2"
        val matchLength = intent.getIntExtra("match_length", 11)

        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color(0xFF1E1E1E)
                ) {
                    DiceScreen(
                        gameType = gameType,
                        player1Name = player1Name,
                        player2Name = player2Name,
                        matchLength = matchLength,
                        onBack = { finish() }
                    )
                }
            }
        }
    }
}

@Composable
fun DiceScreen(
    gameType: String,
    player1Name: String,
    player2Name: String,
    matchLength: Int,
    onBack: () -> Unit
) {
    SimpleIntegratedScreen(
        gameType = gameType,
        player1Name = player1Name,
        player2Name = player2Name,
        matchLength = matchLength,
        onBack = onBack
    )
}