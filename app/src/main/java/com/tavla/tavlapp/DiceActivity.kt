package com.tavla.tavlapp

import android.os.Bundle
import android.view.View
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.random.Random
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
                    SimpleDiceScreen(useTimer = useTimer) { finish() }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            finish()
        }
    }
}

@Composable
fun SimpleDiceScreen(useTimer: Boolean = false, onExit: () -> Unit) {
    var leftDice1 by remember { mutableIntStateOf(1) }
    var leftDice2 by remember { mutableIntStateOf(2) }
    var rightDice1 by remember { mutableIntStateOf(3) }
    var rightDice2 by remember { mutableIntStateOf(4) }
    
    var isAnimating by remember { mutableStateOf(false) }
    
    // Süre durumları
    var leftPlayerReserveMinutes by remember { mutableIntStateOf(1) }
    var leftPlayerReserveSeconds by remember { mutableIntStateOf(30) }
    var rightPlayerReserveMinutes by remember { mutableIntStateOf(1) }
    var rightPlayerReserveSeconds by remember { mutableIntStateOf(30) }
    
    var leftPlayerMoveSeconds by remember { mutableIntStateOf(10) }
    var rightPlayerMoveSeconds by remember { mutableIntStateOf(10) }
    
    // Oyun durumu
    var currentPlayer by remember { mutableIntStateOf(0) } // 0: Başlangıç, 1: Sol, 2: Sağ
    var isTimerRunning by remember { mutableStateOf(false) }
    var gamePhase by remember { mutableIntStateOf(0) } // 0: Başlangıç zarı, 1: Normal oyun
    
    // Başlangıç zar değerleri
    var leftStartDice by remember { mutableIntStateOf(1) }
    var rightStartDice by remember { mutableIntStateOf(1) }
    var showStartDiceResult by remember { mutableStateOf(false) }
    
    // Timer effect
    LaunchedEffect(isTimerRunning, currentPlayer) {
        if (useTimer && isTimerRunning && currentPlayer > 0) {
            while (isTimerRunning) {
                delay(1000)
                
                if (currentPlayer == 1) {
                    // Sol oyuncu süresi
                    if (leftPlayerMoveSeconds > 0) {
                        leftPlayerMoveSeconds--
                    } else if (leftPlayerReserveSeconds > 0) {
                        leftPlayerReserveSeconds--
                        leftPlayerMoveSeconds = 10
                    } else if (leftPlayerReserveMinutes > 0) {
                        leftPlayerReserveMinutes--
                        leftPlayerReserveSeconds = 59
                        leftPlayerMoveSeconds = 10
                    } else {
                        // Süre doldu
                        isTimerRunning = false
                    }
                } else if (currentPlayer == 2) {
                    // Sağ oyuncu süresi
                    if (rightPlayerMoveSeconds > 0) {
                        rightPlayerMoveSeconds--
                    } else if (rightPlayerReserveSeconds > 0) {
                        rightPlayerReserveSeconds--
                        rightPlayerMoveSeconds = 10
                    } else if (rightPlayerReserveMinutes > 0) {
                        rightPlayerReserveMinutes--
                        rightPlayerReserveSeconds = 59
                        rightPlayerMoveSeconds = 10
                    } else {
                        // Süre doldu
                        isTimerRunning = false
                    }
                }
            }
        }
    }

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        Row(modifier = Modifier.fillMaxSize()) {
            // Sol taraf - uçuk mavi
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .background(Color(0xFFE3F2FD))
            )
            
            // Orta çizgi - siyah
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .fillMaxHeight()
                    .background(Color.Black)
            )
            
            // Sağ taraf - uçuk kırmızı
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .background(Color(0xFFFFEBEE))
            )
        }

        // Sol zar at butonu
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .width(80.dp)
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
                                
                                // Süre yönetimi
                                if (useTimer) {
                                    isTimerRunning = false
                                    // Sağ oyuncunun süresini başlat
                                    currentPlayer = 2
                                    rightPlayerMoveSeconds = 10
                                    isTimerRunning = true
                                }
                            }
                            isAnimating = false
                        }
                    }
                }
                .background(Color.Blue.copy(alpha = 0.3f)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = if (gamePhase == 0) "BAŞLANGIÇ\nZARI" else "ZAR\nAT",
                fontSize = if (gamePhase == 0) 12.sp else 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                textAlign = TextAlign.Center
            )
        }

        // Sağ zar at butonu
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .width(80.dp)
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
                                
                                // Süre yönetimi
                                if (useTimer) {
                                    isTimerRunning = false
                                    // Sol oyuncunun süresini başlat
                                    currentPlayer = 1
                                    leftPlayerMoveSeconds = 10
                                    isTimerRunning = true
                                }
                            }
                            isAnimating = false
                        }
                    }
                }
                .background(Color.Red.copy(alpha = 0.3f)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = if (gamePhase == 0) "BAŞLANGIÇ\nZARI" else "ZAR\nAT",
                fontSize = if (gamePhase == 0) 12.sp else 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                textAlign = TextAlign.Center
            )
        }

        // Ana içerik
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = 80.dp, end = 80.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceEvenly
        ) {
            Text(
                text = if (gamePhase == 0) "BAŞLANGIÇ ZAR ATMA" else "ZAR ATMA EKRANI",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black
            )

            // Süre sayaçları (sadece süre kullanımı açıkken ve normal oyunda)
            if (useTimer && gamePhase == 1) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    // Sol oyuncu süreleri
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "REZERV SÜRE",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (currentPlayer == 1) Color.Blue else Color.Gray
                        )
                        Text(
                            text = "${leftPlayerReserveMinutes.toString().padStart(2, '0')}:${leftPlayerReserveSeconds.toString().padStart(2, '0')}",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (currentPlayer == 1) Color.Blue else Color.Gray
                        )
                        Text(
                            text = "HAMLE SÜRESİ",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (currentPlayer == 1) Color.Blue else Color.Gray
                        )
                        Text(
                            text = leftPlayerMoveSeconds.toString().padStart(2, '0'),
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (currentPlayer == 1) Color.Red else Color.Gray
                        )
                    }

                    // Sağ oyuncu süreleri
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "REZERV SÜRE",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (currentPlayer == 2) Color.Red else Color.Gray
                        )
                        Text(
                            text = "${rightPlayerReserveMinutes.toString().padStart(2, '0')}:${rightPlayerReserveSeconds.toString().padStart(2, '0')}",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (currentPlayer == 2) Color.Red else Color.Gray
                        )
                        Text(
                            text = "HAMLE SÜRESİ",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (currentPlayer == 2) Color.Red else Color.Gray
                        )
                        Text(
                            text = rightPlayerMoveSeconds.toString().padStart(2, '0'),
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (currentPlayer == 2) Color.Red else Color.Gray
                        )
                    }
                }
            }

            // Zar görünümü
            if (gamePhase == 0) {
                // Başlangıç zarları
                if (showStartDiceResult) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text("Sol Oyuncu", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            DiceView(value = leftStartDice)
                        }

                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text("Sağ Oyuncu", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            DiceView(value = rightStartDice)
                        }
                    }
                    
                    // Sonuç ve devam et butonu
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
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
                            Button(
                                onClick = {
                                    gamePhase = 1
                                    currentPlayer = if (leftStartDice > rightStartDice) 1 else 2
                                    if (useTimer) {
                                        isTimerRunning = true
                                        if (currentPlayer == 1) {
                                            leftPlayerMoveSeconds = 10
                                        } else {
                                            rightPlayerMoveSeconds = 10
                                        }
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color.Green)
                            ) {
                                Text("Oyuna Başla", fontSize = 16.sp)
                            }
                        }
                    }
                } else {
                    Text(
                        text = "Her iki oyuncu da başlangıç zarını atsın",
                        fontSize = 16.sp,
                        color = Color.Gray
                    )
                }
            } else {
                // Normal oyun zarları
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    // Sol taraf zarları
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        DiceView(value = leftDice1)
                        DiceView(value = leftDice2)
                    }

                    // Sağ taraf zarları
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        DiceView(value = rightDice1)
                        DiceView(value = rightDice2)
                    }
                }
            }

            Button(
                onClick = onExit,
                modifier = Modifier.size(width = 200.dp, height = 60.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color.Gray)
            ) {
                Text(text = "KAPAT", fontSize = 20.sp)
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


