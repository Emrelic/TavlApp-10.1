package com.tavla.tavlapp.ui.online

import android.content.pm.ActivityInfo
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import com.tavla.tavlapp.online.OnlineGameViewModel

class OnlineGameActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Landscape ve tam ekran
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        val roomCode = intent.getStringExtra("roomCode") ?: run {
            finish()
            return
        }
        val isWhite = intent.getBooleanExtra("isWhite", true)
        val displayName = intent.getStringExtra("displayName") ?: "Oyuncu"

        val viewModel = OnlineGameViewModel(roomCode, isWhite, displayName)

        setContent {
            MaterialTheme {
                OnlineGameScreen(
                    viewModel = viewModel,
                    onExit = { finish() }
                )
            }
        }
    }
}
