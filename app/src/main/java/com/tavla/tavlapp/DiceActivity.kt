package com.tavla.tavlapp

import android.os.Bundle
import android.view.View
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlin.random.Random

data class GameClock(
    var reserveTimeSeconds: Int,  // Toplam rezerv süre (saniye)
    var delaySeconds: Int = 12,   // Bronstein delay (saniye)
    var isActive: Boolean = false,
    var currentDelayRemaining: Int = 12
)

enum class GameState {
    INITIAL_ROLL,    // Oyun başlangıcı - kim başlayacak
    PLAYING,         // Normal oyun
    PAUSED
}

class DiceActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Full screen
        window.decorView.systemUiVisibility = (
            View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
            View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or
            View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
            View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
            View.SYSTEM_UI_FLAG_FULLSCREEN or
            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
        )

        // Intent parametreleri
        val gameType = intent.getStringExtra("game_type") ?: "Modern"
        val useDiceRoller = intent.getBooleanExtra("use_dice_roller", false)
        val useTimer = intent.getBooleanExtra("use_timer", false)
        val player1NameText = intent.getStringExtra("player1_name") ?: "Oyuncu 1"
        val player2NameText = intent.getStringExtra("player2_name") ?: "Oyuncu 2"

        setContent {
            TavlaAppTheme {
                BackgammonClockScreen(
                    gameType = gameType,
                    useTimer = useTimer,
                    player1Name = player1NameText,
                    player2Name = player2NameText,
                    onExit = { finish() }
                )
            }
        }
    }
}

@Composable
fun BackgammonClockScreen(
    gameType: String,
    useTimer: Boolean,
    player1Name: String,
    player2Name: String,
    onExit: () -> Unit
) {
    // Oyun durumu
    var gameState by remember { mutableStateOf(GameState.INITIAL_ROLL) }
    var currentPlayer by remember { mutableIntStateOf(1) }

    // Zarlar
    var player1Dice by remember { mutableIntStateOf(1) }
    var player2Dice by remember { mutableIntStateOf(1) }
    var leftDice1 by remember { mutableIntStateOf(1) }
    var leftDice2 by remember { mutableIntStateOf(1) }
    var rightDice1 by remember { mutableIntStateOf(1) }
    var rightDice2 by remember { mutableIntStateOf(1) }

    // Saat sistemi (FIBO kuralları: 1.5 dakika rezerv + 12sn Bronstein delay)
    var player1Clock by remember { mutableStateOf(GameClock(reserveTimeSeconds = 90)) }
    var player2Clock by remember { mutableStateOf(GameClock(reserveTimeSeconds = 90)) }

    // Timer effect
    LaunchedEffect(gameState, currentPlayer) {
        if (gameState == GameState.PLAYING && useTimer) {
            while (true) {
                delay(1000L)

                if (currentPlayer == 1 && player1Clock.isActive) {
                    if (player1Clock.currentDelayRemaining > 0) {
                        player1Clock = player1Clock.copy(currentDelayRemaining = player1Clock.currentDelayRemaining - 1)
                    } else {
                        if (player1Clock.reserveTimeSeconds > 0) {
                            player1Clock = player1Clock.copy(reserveTimeSeconds = player1Clock.reserveTimeSeconds - 1)
                        }
                    }
                } else if (currentPlayer == 2 && player2Clock.isActive) {
                    if (player2Clock.currentDelayRemaining > 0) {
                        player2Clock = player2Clock.copy(currentDelayRemaining = player2Clock.currentDelayRemaining - 1)
                    } else {
                        if (player2Clock.reserveTimeSeconds > 0) {
                            player2Clock = player2Clock.copy(reserveTimeSeconds = player2Clock.reserveTimeSeconds - 1)
                        }
                    }
                }
            }
        }
    }

    // Ana ekran - yatay layout
    Row(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        // Sol taraf - Player 1 (Uçuk mavi)
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .background(Color(0xFFE3F2FD)) // Uçuk mavi
                .clickable {
                    handlePlayerButtonPress(
                        playerNumber = 1,
                        gameState = gameState,
                        currentPlayer = currentPlayer,
                        onDiceRoll = { dice ->
                            when (gameState) {
                                GameState.INITIAL_ROLL -> {
                                    player1Dice = dice
                                    if (player2Dice != 1) {
                                        // Her iki oyuncu da attı, karşılaştır
                                        if (player1Dice > player2Dice) {
                                            currentPlayer = 1
                                            gameState = GameState.PLAYING
                                            leftDice1 = player1Dice
                                            leftDice2 = player2Dice
                                            player1Clock = player1Clock.copy(isActive = true, currentDelayRemaining = 12)
                                        } else if (player2Dice > player1Dice) {
                                            currentPlayer = 2
                                            gameState = GameState.PLAYING
                                            rightDice1 = player1Dice
                                            rightDice2 = player2Dice
                                            player2Clock = player2Clock.copy(isActive = true, currentDelayRemaining = 12)
                                        } else {
                                            // Berabere, tekrar at
                                            player1Dice = 1
                                            player2Dice = 1
                                        }
                                    }
                                }
                                GameState.PLAYING -> {
                                    // Normal zar atma
                                    leftDice1 = Random.nextInt(1, 7)
                                    leftDice2 = Random.nextInt(1, 7)
                                    // Saati durdur ve karşı tarafa geçir
                                    player1Clock = player1Clock.copy(isActive = false)
                                    player2Clock = player2Clock.copy(isActive = true, currentDelayRemaining = 12)
                                    currentPlayer = 2
                                }
                                else -> {}
                            }
                        }
                    )
                }
        ) {
            // Sol taraf içeriği
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceEvenly
            ) {
                // Oyuncu adı
                Text(
                    text = player1Name,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )

                // Süre bilgisi
                if (useTimer) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = formatTime(player1Clock.reserveTimeSeconds),
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (player1Clock.isActive) Color.Red else Color.Black
                        )
                        Text(
                            text = "Delay: ${player1Clock.currentDelayRemaining}s",
                            fontSize = 14.sp,
                            color = Color.Gray
                        )
                    }
                }

                // Zarlar
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Sol zar 1
                    Box(
                        modifier = Modifier
                            .size(60.dp)
                            .background(Color.White, RoundedCornerShape(8.dp))
                            .border(2.dp, Color.Black, RoundedCornerShape(8.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "${if (gameState == GameState.INITIAL_ROLL) player1Dice else leftDice1}",
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Red
                        )
                    }

                    // Sol zar 2
                    Box(
                        modifier = Modifier
                            .size(60.dp)
                            .background(Color.White, RoundedCornerShape(8.dp))
                            .border(2.dp, Color.Black, RoundedCornerShape(8.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "${if (gameState == GameState.INITIAL_ROLL) 1 else leftDice2}",
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Red
                        )
                    }
                }

                // Buton metni
                Text(
                    text = when (gameState) {
                        GameState.INITIAL_ROLL -> "ZAR AT\n(Kim Başlar)"
                        GameState.PLAYING -> if (currentPlayer == 1) "ZAR AT\n(Sıran)" else "BEKLE"
                        else -> "DURAKLAT"
                    },
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    color = if (currentPlayer == 1 || gameState == GameState.INITIAL_ROLL) Color.Black else Color.Gray
                )
            }
        }

        // Orta çizgi
        Box(
            modifier = Modifier
                .width(4.dp)
                .fillMaxHeight()
                .background(Color.Black)
        )

        // Sağ taraf - Player 2 (Uçuk kırmızı)
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .background(Color(0xFFFFEBEE)) // Uçuk kırmızı
                .clickable {
                    handlePlayerButtonPress(
                        playerNumber = 2,
                        gameState = gameState,
                        currentPlayer = currentPlayer,
                        onDiceRoll = { dice ->
                            when (gameState) {
                                GameState.INITIAL_ROLL -> {
                                    player2Dice = dice
                                    if (player1Dice != 1) {
                                        // Her iki oyuncu da attı, karşılaştır
                                        if (player2Dice > player1Dice) {
                                            currentPlayer = 2
                                            gameState = GameState.PLAYING
                                            rightDice1 = player1Dice
                                            rightDice2 = player2Dice
                                            player2Clock = player2Clock.copy(isActive = true, currentDelayRemaining = 12)
                                        } else if (player1Dice > player2Dice) {
                                            currentPlayer = 1
                                            gameState = GameState.PLAYING
                                            leftDice1 = player1Dice
                                            leftDice2 = player2Dice
                                            player1Clock = player1Clock.copy(isActive = true, currentDelayRemaining = 12)
                                        } else {
                                            // Berabere, tekrar at
                                            player1Dice = 1
                                            player2Dice = 1
                                        }
                                    }
                                }
                                GameState.PLAYING -> {
                                    // Normal zar atma
                                    rightDice1 = Random.nextInt(1, 7)
                                    rightDice2 = Random.nextInt(1, 7)
                                    // Saati durdur ve karşı tarafa geçir
                                    player2Clock = player2Clock.copy(isActive = false)
                                    player1Clock = player1Clock.copy(isActive = true, currentDelayRemaining = 12)
                                    currentPlayer = 1
                                }
                                else -> {}
                            }
                        }
                    )
                }
        ) {
            // Sağ taraf içeriği
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceEvenly
            ) {
                // Oyuncu adı
                Text(
                    text = player2Name,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )

                // Süre bilgisi
                if (useTimer) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = formatTime(player2Clock.reserveTimeSeconds),
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (player2Clock.isActive) Color.Red else Color.Black
                        )
                        Text(
                            text = "Delay: ${player2Clock.currentDelayRemaining}s",
                            fontSize = 14.sp,
                            color = Color.Gray
                        )
                    }
                }

                // Zarlar
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Sağ zar 1
                    Box(
                        modifier = Modifier
                            .size(60.dp)
                            .background(Color.White, RoundedCornerShape(8.dp))
                            .border(2.dp, Color.Black, RoundedCornerShape(8.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "${if (gameState == GameState.INITIAL_ROLL) player2Dice else rightDice1}",
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Red
                        )
                    }

                    // Sağ zar 2
                    Box(
                        modifier = Modifier
                            .size(60.dp)
                            .background(Color.White, RoundedCornerShape(8.dp))
                            .border(2.dp, Color.Black, RoundedCornerShape(8.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "${if (gameState == GameState.INITIAL_ROLL) 1 else rightDice2}",
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Red
                        )
                    }
                }

                // Buton metni
                Text(
                    text = when (gameState) {
                        GameState.INITIAL_ROLL -> "ZAR AT\n(Kim Başlar)"
                        GameState.PLAYING -> if (currentPlayer == 2) "ZAR AT\n(Sıran)" else "BEKLE"
                        else -> "DURAKLAT"
                    },
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    color = if (currentPlayer == 2 || gameState == GameState.INITIAL_ROLL) Color.Black else Color.Gray
                )
            }
        }
    }
}

private fun handlePlayerButtonPress(
    playerNumber: Int,
    gameState: GameState,
    currentPlayer: Int,
    onDiceRoll: (Int) -> Unit
) {
    when (gameState) {
        GameState.INITIAL_ROLL -> {
            // Başlangıç zarı at
            val dice = Random.nextInt(1, 7)
            onDiceRoll(dice)
        }
        GameState.PLAYING -> {
            if (currentPlayer == playerNumber) {
                // Sadece sırası olan oyuncu zar atabilir
                onDiceRoll(0) // Parametre kullanılmıyor, random içeride
            }
        }
        else -> {}
    }
}

private fun formatTime(seconds: Int): String {
    val minutes = seconds / 60
    val secs = seconds % 60
    return String.format("%d:%02d", minutes, secs)
}

