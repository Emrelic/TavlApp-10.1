package com.tavla.tavlapp

import android.os.Bundle
import android.view.View
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

class DiceActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Full screen - Status bar ve Navigation bar gizle
        window.decorView.systemUiVisibility = (
            View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
            View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or
            View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
            View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
            View.SYSTEM_UI_FLAG_FULLSCREEN or
            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
        )

        // Intent'ten parametreleri al
        val gameType = intent.getStringExtra("game_type") ?: "Modern"
        val useDiceRoller = intent.getBooleanExtra("use_dice_roller", false)
        val useTimer = intent.getBooleanExtra("use_timer", false)
        val player1Name = intent.getStringExtra("player1_name") ?: "Oyuncu 1"
        val player2Name = intent.getStringExtra("player2_name") ?: "Oyuncu 2"

        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color.Black
                ) {
                    DiceScreen(
                        gameType = gameType,
                        useDiceRoller = useDiceRoller,
                        useTimer = useTimer,
                        player1Name = player1Name,
                        player2Name = player2Name,
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
    useDiceRoller: Boolean,
    useTimer: Boolean,
    player1Name: String,
    player2Name: String,
    onBack: () -> Unit
) {
    // Zar state'leri
    var dice1Value by remember { mutableIntStateOf(1) }
    var dice2Value by remember { mutableIntStateOf(1) }
    var isRolling by remember { mutableStateOf(false) }

    // Süre state'leri
    var player1Time by remember { mutableIntStateOf(300) } // 5 dakika = 300 saniye
    var player2Time by remember { mutableIntStateOf(300) }
    var currentPlayer by remember { mutableIntStateOf(1) } // 1 veya 2
    var timerRunning by remember { mutableStateOf(false) }

    // İstatistik state'leri
    var diceStats by remember { mutableStateOf(mutableMapOf<String, Int>()) }
    var dice1Played by remember { mutableStateOf(false) }
    var dice2Played by remember { mutableStateOf(false) }


    // Süre sayacı
    LaunchedEffect(timerRunning, currentPlayer) {
        if (timerRunning && useTimer) {
            while (timerRunning) {
                delay(1000)
                if (currentPlayer == 1 && player1Time > 0) {
                    player1Time--
                } else if (currentPlayer == 2 && player2Time > 0) {
                    player2Time--
                }

                if (player1Time <= 0 || player2Time <= 0) {
                    timerRunning = false
                }
            }
        }
    }

    // Ana layout - Yatay düzen
    Row(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // Sol taraf - Player 1 (180° döndürülmüş)
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .rotate(180f)
                .background(Color(0xFF1976D2))
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            PlayerSide(
                playerName = player1Name,
                time = if (useTimer) player1Time else null,
                isActive = currentPlayer == 1,
                onDiceRoll = {
                    if (!isRolling) {
                        isRolling = true
                        // Random değerler oluştur
                        val newDice1 = (1..6).random()
                        val newDice2 = (1..6).random()
                        dice1Value = newDice1
                        dice2Value = newDice2
                        // İstatistik güncelle
                        val diceKey = if (newDice1 <= newDice2) "${newDice1}-${newDice2}" else "${newDice2}-${newDice1}"
                        diceStats = diceStats.toMutableMap().apply {
                            this[diceKey] = (this[diceKey] ?: 0) + 1
                        }
                        isRolling = false
                    }
                },
                dice1Value = dice1Value,
                dice2Value = dice2Value,
                isRolling = isRolling
            )
        }

        // Orta çizgi
        Box(
            modifier = Modifier
                .width(2.dp)
                .fillMaxHeight()
                .background(Color.White)
        )

        // Sağ taraf - Player 2 (normal)
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .background(Color(0xFFD32F2F))
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            PlayerSide(
                playerName = player2Name,
                time = if (useTimer) player2Time else null,
                isActive = currentPlayer == 2,
                onDiceRoll = {
                    if (!isRolling) {
                        isRolling = true
                        // Random değerler oluştur
                        val newDice1 = (1..6).random()
                        val newDice2 = (1..6).random()
                        dice1Value = newDice1
                        dice2Value = newDice2
                        // İstatistik güncelle
                        val diceKey = if (newDice1 <= newDice2) "${newDice1}-${newDice2}" else "${newDice2}-${newDice1}"
                        diceStats = diceStats.toMutableMap().apply {
                            this[diceKey] = (this[diceKey] ?: 0) + 1
                        }
                        isRolling = false
                    }
                },
                dice1Value = dice1Value,
                dice2Value = dice2Value,
                isRolling = isRolling
            )
        }
    }

    // İstatistik paneli (altta overlay)
    if (diceStats.isNotEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.BottomCenter
        ) {
            StatisticsPanel(
                diceStats = diceStats,
                dice1Played = dice1Played,
                dice2Played = dice2Played,
                onDice1PlayedChange = { dice1Played = it },
                onDice2PlayedChange = { dice2Played = it }
            )
        }
    }

}

@Composable
fun PlayerSide(
    playerName: String,
    time: Int?,
    isActive: Boolean,
    onDiceRoll: () -> Unit,
    dice1Value: Int,
    dice2Value: Int,
    isRolling: Boolean
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Oyuncu adı
        Text(
            text = playerName,
            color = Color.White,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold
        )

        // Süre (varsa)
        if (time != null) {
            Text(
                text = "${time / 60}:${String.format("%02d", time % 60)}",
                color = if (isActive) Color.Yellow else Color.White,
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold
            )
        }

        // ZAR AT butonu
        Button(
            onClick = onDiceRoll,
            enabled = !isRolling,
            modifier = Modifier
                .size(120.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (isActive) Color.Green else Color.Gray
            )
        ) {
            Text(
                text = if (isRolling) "ATIYOR..." else "ZAR AT",
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
        }

        // Zarlar
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            DiceComponent(dice1Value, 80.dp, isRolling)
            DiceComponent(dice2Value, 80.dp, isRolling)
        }
    }
}

@Composable
fun DiceComponent(value: Int, size: androidx.compose.ui.unit.Dp, isAnimating: Boolean) {
    val animatedRotation by animateFloatAsState(
        targetValue = if (isAnimating) 360f else 0f,
        animationSpec = if (isAnimating) {
            infiniteRepeatable(
                animation = tween(200, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            )
        } else {
            tween(0)
        }
    )

    Box(
        modifier = Modifier
            .size(size)
            .rotate(animatedRotation)
            .background(Color.White, RoundedCornerShape(12.dp))
            .border(3.dp, Color.Black, RoundedCornerShape(12.dp)),
        contentAlignment = Alignment.Center
    ) {
        when (value) {
            1 -> DicePattern1()
            2 -> DicePattern2()
            3 -> DicePattern3()
            4 -> DicePattern4()
            5 -> DicePattern5()
            6 -> DicePattern6()
        }
    }
}

@Composable
fun StatisticsPanel(
    diceStats: Map<String, Int>,
    dice1Played: Boolean,
    dice2Played: Boolean,
    onDice1PlayedChange: (Boolean) -> Unit,
    onDice2PlayedChange: (Boolean) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(120.dp)
            .padding(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.9f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // İstatistikler
            Column {
                Text("Zar İstatistikleri", fontWeight = FontWeight.Bold)
                diceStats.forEach { (dice, count) ->
                    Text("$dice: $count", fontSize = 12.sp)
                }
            }

            // Checkbox'lar
            Column {
                Text("Oynanan Zarlar", fontWeight = FontWeight.Bold)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = dice1Played,
                        onCheckedChange = onDice1PlayedChange
                    )
                    Text("Zar 1")
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = dice2Played,
                        onCheckedChange = onDice2PlayedChange
                    )
                    Text("Zar 2")
                }
            }

            // Butonlar
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = {
                        onDice1PlayedChange(true)
                        onDice2PlayedChange(true)
                    }
                ) {
                    Text("Tümü Oynandı")
                }
                Button(
                    onClick = {
                        onDice1PlayedChange(false)
                        onDice2PlayedChange(false)
                    }
                ) {
                    Text("Tümü Gele")
                }
            }
        }
    }
}

// Zar desenleri
@Composable
fun DicePattern1() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Box(
            modifier = Modifier
                .size(12.dp)
                .background(Color.Black, CircleShape)
        )
    }
}

@Composable
fun DicePattern2() {
    Box(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .size(12.dp)
                .background(Color.Black, CircleShape)
                .align(Alignment.TopStart)
                .offset(x = 8.dp, y = 8.dp)
        )
        Box(
            modifier = Modifier
                .size(12.dp)
                .background(Color.Black, CircleShape)
                .align(Alignment.BottomEnd)
                .offset(x = (-8).dp, y = (-8).dp)
        )
    }
}

@Composable
fun DicePattern3() {
    Box(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .size(12.dp)
                .background(Color.Black, CircleShape)
                .align(Alignment.TopStart)
                .offset(x = 8.dp, y = 8.dp)
        )
        Box(
            modifier = Modifier
                .size(12.dp)
                .background(Color.Black, CircleShape)
                .align(Alignment.Center)
        )
        Box(
            modifier = Modifier
                .size(12.dp)
                .background(Color.Black, CircleShape)
                .align(Alignment.BottomEnd)
                .offset(x = (-8).dp, y = (-8).dp)
        )
    }
}

@Composable
fun DicePattern4() {
    Box(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .size(12.dp)
                .background(Color.Black, CircleShape)
                .align(Alignment.TopStart)
                .offset(x = 8.dp, y = 8.dp)
        )
        Box(
            modifier = Modifier
                .size(12.dp)
                .background(Color.Black, CircleShape)
                .align(Alignment.TopEnd)
                .offset(x = (-8).dp, y = 8.dp)
        )
        Box(
            modifier = Modifier
                .size(12.dp)
                .background(Color.Black, CircleShape)
                .align(Alignment.BottomStart)
                .offset(x = 8.dp, y = (-8).dp)
        )
        Box(
            modifier = Modifier
                .size(12.dp)
                .background(Color.Black, CircleShape)
                .align(Alignment.BottomEnd)
                .offset(x = (-8).dp, y = (-8).dp)
        )
    }
}

@Composable
fun DicePattern5() {
    Box(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .size(12.dp)
                .background(Color.Black, CircleShape)
                .align(Alignment.TopStart)
                .offset(x = 8.dp, y = 8.dp)
        )
        Box(
            modifier = Modifier
                .size(12.dp)
                .background(Color.Black, CircleShape)
                .align(Alignment.TopEnd)
                .offset(x = (-8).dp, y = 8.dp)
        )
        Box(
            modifier = Modifier
                .size(12.dp)
                .background(Color.Black, CircleShape)
                .align(Alignment.Center)
        )
        Box(
            modifier = Modifier
                .size(12.dp)
                .background(Color.Black, CircleShape)
                .align(Alignment.BottomStart)
                .offset(x = 8.dp, y = (-8).dp)
        )
        Box(
            modifier = Modifier
                .size(12.dp)
                .background(Color.Black, CircleShape)
                .align(Alignment.BottomEnd)
                .offset(x = (-8).dp, y = (-8).dp)
        )
    }
}

@Composable
fun DicePattern6() {
    Box(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .size(12.dp)
                .background(Color.Black, CircleShape)
                .align(Alignment.TopStart)
                .offset(x = 8.dp, y = 8.dp)
        )
        Box(
            modifier = Modifier
                .size(12.dp)
                .background(Color.Black, CircleShape)
                .align(Alignment.TopEnd)
                .offset(x = (-8).dp, y = 8.dp)
        )
        Box(
            modifier = Modifier
                .size(12.dp)
                .background(Color.Black, CircleShape)
                .align(Alignment.CenterStart)
                .offset(x = 8.dp)
        )
        Box(
            modifier = Modifier
                .size(12.dp)
                .background(Color.Black, CircleShape)
                .align(Alignment.CenterEnd)
                .offset(x = (-8).dp)
        )
        Box(
            modifier = Modifier
                .size(12.dp)
                .background(Color.Black, CircleShape)
                .align(Alignment.BottomStart)
                .offset(x = 8.dp, y = (-8).dp)
        )
        Box(
            modifier = Modifier
                .size(12.dp)
                .background(Color.Black, CircleShape)
                .align(Alignment.BottomEnd)
                .offset(x = (-8).dp, y = (-8).dp)
        )
    }
}