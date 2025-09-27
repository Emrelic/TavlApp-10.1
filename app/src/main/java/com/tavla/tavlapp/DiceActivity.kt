package com.tavla.tavlapp

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tavla.tavlapp.data.BackgammonTimeControl
import kotlin.random.Random
import kotlin.time.Duration
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class DiceActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Fullscreen mode - status bar ve navigation bar gizle
        WindowCompat.setDecorFitsSystemWindows(window, false)
        val windowInsetsController = WindowInsetsControllerCompat(window, window.decorView)
        windowInsetsController.hide(WindowInsetsCompat.Type.systemBars())
        windowInsetsController.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE

        // Ekranı açık tut
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        // Intent'den süre kullanım bilgisini al
        val useTimer = intent.getBooleanExtra("use_timer", false)

        try {
            setContent {
                MaterialTheme {
                    NewDiceScreen(useTimer = useTimer) { finish() }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            finish()
        }
    }
}

@Composable
fun NewDiceScreen(useTimer: Boolean = false, onExit: () -> Unit) {
    // Zar durumları
    var leftDice1 by remember { mutableIntStateOf(1) }
    var leftDice2 by remember { mutableIntStateOf(2) }
    var rightDice1 by remember { mutableIntStateOf(3) }
    var rightDice2 by remember { mutableIntStateOf(4) }
    
    var isAnimating by remember { mutableStateOf(false) }
    
    // Süre kontrol durumları
    var player1ReserveTime by remember { mutableStateOf(Duration.ZERO) }
    var player1MoveTime by remember { mutableStateOf(Duration.ZERO) }
    var player1IsActive by remember { mutableStateOf(false) }
    
    var player2ReserveTime by remember { mutableStateOf(Duration.ZERO) }
    var player2MoveTime by remember { mutableStateOf(Duration.ZERO) }
    var player2IsActive by remember { mutableStateOf(false) }
    
    // Süre kontrol sistemi
    val timeControl = remember {
        if (useTimer) {
            BackgammonTimeControl(
                onTimeUpdate = { player, reserveTime, currentMoveTime, isActive ->
                    when (player) {
                        BackgammonTimeControl.Player.PLAYER1 -> {
                            player1ReserveTime = reserveTime
                            player1MoveTime = currentMoveTime
                            player1IsActive = isActive
                        }
                        BackgammonTimeControl.Player.PLAYER2 -> {
                            player2ReserveTime = reserveTime
                            player2MoveTime = currentMoveTime
                            player2IsActive = isActive
                        }
                    }
                },
                onTimeExpired = { player ->
                    // Süre dolduğunda buraya gelir
                    // TODO: Oyun sonu işlemleri
                }
            )
        } else null
    }
    
    // Oyun durumu
    var gamePhase by remember { mutableIntStateOf(0) } // 0: Başlangıç zarı, 1: Normal oyun
    
    // Başlangıç zar değerleri
    var leftStartDice by remember { mutableIntStateOf(1) }
    var rightStartDice by remember { mutableIntStateOf(1) }
    var showStartDiceResult by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        Row(modifier = Modifier.fillMaxSize()) {
            // Sol taraf - açık mavi
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .background(Color(0xFFE3F2FD))
            )
            
            // Orta çizgi - siyah
            Box(
                modifier = Modifier
                    .width(8.dp)
                    .fillMaxHeight()
                    .background(Color.Black)
            )
            
            // Sağ taraf - açık kırmızı
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .background(Color(0xFFFFEBEE))
            )
        }

        // Sol süre başlat butonu - tam yükseklik
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .width(100.dp)
                .align(Alignment.CenterStart)
                .clickable(enabled = !isAnimating) {
                    if (!isAnimating) {
                        isAnimating = true
                        val scope = CoroutineScope(Dispatchers.Main)
                        scope.launch {
                            if (gamePhase == 0) {
                                // Başlangıç zarı - tek zar
                                repeat(6) { i ->
                                    leftStartDice = Random.nextInt(1, 7)
                                    delay(100)
                                }
                                showStartDiceResult = true
                            } else {
                                // Normal oyun - çift zar
                                repeat(6) { i ->
                                    leftDice1 = Random.nextInt(1, 7)
                                    leftDice2 = Random.nextInt(1, 7)
                                    delay(100)
                                }
                                
                                // Süre yönetimi - Player1'e geç
                                timeControl?.switchToPlayer(BackgammonTimeControl.Player.PLAYER1)
                            }
                            isAnimating = false
                        }
                    }
                }
                .background(Color(0xFF1976D2).copy(alpha = 0.8f)),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "SÜRE\nBAŞLAT",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.graphicsLayer(rotationZ = 0f)
                )
                
                if (useTimer) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Rezerv:\n${(player1ReserveTime.inWholeMinutes)}:${(player1ReserveTime.inWholeSeconds % 60).toString().padStart(2, '0')}",
                        fontSize = 12.sp,
                        color = Color.White,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = "Hamle:\n${player1MoveTime.inWholeSeconds}s",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (player1IsActive) Color.Yellow else Color.White,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        // Sağ süre başlat butonu - tam yükseklik
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .width(100.dp)
                .align(Alignment.CenterEnd)
                .clickable(enabled = !isAnimating) {
                    if (!isAnimating) {
                        isAnimating = true
                        val scope = CoroutineScope(Dispatchers.Main)
                        scope.launch {
                            if (gamePhase == 0) {
                                // Başlangıç zarı - tek zar
                                repeat(6) { i ->
                                    rightStartDice = Random.nextInt(1, 7)
                                    delay(100)
                                }
                                showStartDiceResult = true
                            } else {
                                // Normal oyun - çift zar
                                repeat(6) { i ->
                                    rightDice1 = Random.nextInt(1, 7)
                                    rightDice2 = Random.nextInt(1, 7)
                                    delay(100)
                                }
                                
                                // Süre yönetimi - Player2'ye geç
                                timeControl?.switchToPlayer(BackgammonTimeControl.Player.PLAYER2)
                            }
                            isAnimating = false
                        }
                    }
                }
                .background(Color(0xFFD32F2F).copy(alpha = 0.8f)),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "SÜRE\nBAŞLAT",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.graphicsLayer(rotationZ = 180f)
                )
                
                if (useTimer) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Rezerv:\n${(player2ReserveTime.inWholeMinutes)}:${(player2ReserveTime.inWholeSeconds % 60).toString().padStart(2, '0')}",
                        fontSize = 12.sp,
                        color = Color.White,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.graphicsLayer(rotationZ = 180f)
                    )
                    Text(
                        text = "Hamle:\n${player2MoveTime.inWholeSeconds}s",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (player2IsActive) Color.Yellow else Color.White,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.graphicsLayer(rotationZ = 180f)
                    )
                }
            }
        }

        // Ana içerik - ortadan zarlar
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = 100.dp, end = 100.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {

            // Merkez zar alanı
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Sol zarlar
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    if (gamePhase == 0) {
                        if (showStartDiceResult) {
                            Text(
                                "Sol Oyuncu",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.Blue
                            )
                            DiceView(value = leftStartDice)
                        } else {
                            Text(
                                "Başlangıç\nZarı At",
                                fontSize = 14.sp,
                                textAlign = TextAlign.Center,
                                color = Color.Blue
                            )
                            DiceView(value = 1)
                        }
                    } else {
                        DiceView(value = leftDice1)
                        DiceView(value = leftDice2)
                    }
                }
                
                // Orta çizgi görsel
                Box(
                    modifier = Modifier
                        .width(8.dp)
                        .height(120.dp)
                        .background(Color.Black)
                )
                
                // Sağ zarlar
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    if (gamePhase == 0) {
                        if (showStartDiceResult) {
                            Text(
                                "Sağ Oyuncu",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.Red
                            )
                            DiceView(value = rightStartDice)
                        } else {
                            Text(
                                "Başlangıç\nZarı At",
                                fontSize = 14.sp,
                                textAlign = TextAlign.Center,
                                color = Color.Red
                            )
                            DiceView(value = 1)
                        }
                    } else {
                        DiceView(value = rightDice1)
                        DiceView(value = rightDice2)
                    }
                }
            }
            
            // Başlangıç sonuç ve kontroller
            if (gamePhase == 0 && showStartDiceResult) {
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    text = when {
                        leftStartDice > rightStartDice -> "Sol Oyuncu Başlıyor!"
                        rightStartDice > leftStartDice -> "Sağ Oyuncu Başlıyor!"
                        else -> "Berabere! Tekrar Atın!"
                    },
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = when {
                        leftStartDice > rightStartDice -> Color.Blue
                        rightStartDice > leftStartDice -> Color.Red
                        else -> Color.Black
                    }
                )
                
                if (leftStartDice != rightStartDice) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = {
                            gamePhase = 1
                            if (useTimer) {
                                timeControl?.startGame(
                                    if (leftStartDice > rightStartDice) 
                                        BackgammonTimeControl.Player.PLAYER1 
                                    else 
                                        BackgammonTimeControl.Player.PLAYER2
                                )
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Green)
                    ) {
                        Text("Oyuna Başla", fontSize = 16.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
            
            // FIBO kuralları bilgisi
            if (useTimer) {
                Text(
                    text = "FIBO Kuralları: 90s rezerv + 12s hamle",
                    fontSize = 12.sp,
                    color = Color.Gray,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(16.dp))
            }
            
            // Kapatma ve kontrol butonları
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (useTimer && gamePhase == 1) {
                    Button(
                        onClick = { timeControl?.pauseGame() },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF57C00))
                    ) {
                        Text("DURAKLAT", fontSize = 14.sp)
                    }
                    
                    Button(
                        onClick = { timeControl?.resumeGame() },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Green)
                    ) {
                        Text("DEVAM", fontSize = 14.sp)
                    }
                }
                
                Button(
                    onClick = {
                        timeControl?.stopGame()
                        onExit()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Gray)
                ) {
                    Text(text = "KAPAT", fontSize = 16.sp)
                }
            }
        }
    }
}

@Composable
fun DiceView(value: Int) {
    Box(
        modifier = Modifier
            .size(80.dp)
            .background(
                Color.White,
                androidx.compose.foundation.shape.RoundedCornerShape(12.dp)
            )
            .padding(8.dp),
        contentAlignment = Alignment.Center
    ) {
        when (value) {
            1 -> DiceOne()
            2 -> DiceTwo()
            3 -> DiceThree()
            4 -> DiceFour()
            5 -> DiceFive()
            6 -> DiceSix()
        }
    }
}

@Composable
fun DiceOne() {
    Box(modifier = Modifier.fillMaxSize()) {
        DiceDot(Modifier.align(Alignment.Center))
    }
}

@Composable
fun DiceTwo() {
    Box(modifier = Modifier.fillMaxSize()) {
        DiceDot(Modifier.align(Alignment.TopStart))
        DiceDot(Modifier.align(Alignment.BottomEnd))
    }
}

@Composable
fun DiceThree() {
    Box(modifier = Modifier.fillMaxSize()) {
        DiceDot(Modifier.align(Alignment.TopStart))
        DiceDot(Modifier.align(Alignment.Center))
        DiceDot(Modifier.align(Alignment.BottomEnd))
    }
}

@Composable
fun DiceFour() {
    Box(modifier = Modifier.fillMaxSize()) {
        DiceDot(Modifier.align(Alignment.TopStart))
        DiceDot(Modifier.align(Alignment.TopEnd))
        DiceDot(Modifier.align(Alignment.BottomStart))
        DiceDot(Modifier.align(Alignment.BottomEnd))
    }
}

@Composable
fun DiceFive() {
    Box(modifier = Modifier.fillMaxSize()) {
        DiceDot(Modifier.align(Alignment.TopStart))
        DiceDot(Modifier.align(Alignment.TopEnd))
        DiceDot(Modifier.align(Alignment.Center))
        DiceDot(Modifier.align(Alignment.BottomStart))
        DiceDot(Modifier.align(Alignment.BottomEnd))
    }
}

@Composable
fun DiceSix() {
    Box(modifier = Modifier.fillMaxSize()) {
        DiceDot(Modifier.align(Alignment.TopStart))
        DiceDot(Modifier.align(Alignment.TopEnd))
        Column(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .fillMaxHeight(),
            verticalArrangement = Arrangement.Center
        ) {
            DiceDot(Modifier)
        }
        Column(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .fillMaxHeight(),
            verticalArrangement = Arrangement.Center
        ) {
            DiceDot(Modifier)
        }
        DiceDot(Modifier.align(Alignment.BottomStart))
        DiceDot(Modifier.align(Alignment.BottomEnd))
    }
}

@Composable
fun DiceDot(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(12.dp)
            .background(Color.Black, androidx.compose.foundation.shape.CircleShape)
    )
}


