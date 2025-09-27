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

        // Intent'den oyun ayarlarını al
        val useTimer = intent.getBooleanExtra("use_timer", false)
        val gameType = intent.getStringExtra("game_type") ?: "Modern Tavla"
        val player1Name = intent.getStringExtra("player1_name") ?: "Oyuncu 1"
        val player2Name = intent.getStringExtra("player2_name") ?: "Oyuncu 2"

        try {
            setContent {
                MaterialTheme {
                    NewDiceScreen(
                        useTimer = useTimer, 
                        gameType = gameType,
                        player1Name = player1Name,
                        player2Name = player2Name
                    ) { finish() }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            finish()
        }
    }
}

@Composable
fun NewDiceScreen(
    useTimer: Boolean = false, 
    gameType: String = "Modern Tavla",
    player1Name: String = "Oyuncu 1",
    player2Name: String = "Oyuncu 2",
    onExit: () -> Unit
) {
    // Oyun modu kontrolü
    val isModernBackgammon = gameType == "Modern Tavla"
    
    // Zar durumları
    var leftDice1 by remember { mutableIntStateOf(1) }
    var leftDice2 by remember { mutableIntStateOf(2) }
    var rightDice1 by remember { mutableIntStateOf(3) }
    var rightDice2 by remember { mutableIntStateOf(4) }
    
    // Animasyon durumları
    var isAnimating by remember { mutableStateOf(false) }
    var leftDiceAnimating by remember { mutableStateOf(false) }
    var rightDiceAnimating by remember { mutableStateOf(false) }
    
    // Zar görünüm durumları
    var leftDiceActive by remember { mutableStateOf(false) }
    var rightDiceActive by remember { mutableStateOf(false) }
    
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
    
    // Oyun durumu ve fazları
    var gamePhase by remember { mutableIntStateOf(0) } 
    // 0: Başlangıç zarı, 1: Geleneksel ikinci zar (eğer gerekli), 2: Normal oyun
    
    // Başlangıç zar değerleri
    var leftStartDice by remember { mutableIntStateOf(1) }
    var rightStartDice by remember { mutableIntStateOf(1) }
    var showStartDiceResult by remember { mutableStateOf(false) }
    var leftHasRolled by remember { mutableStateOf(false) }
    var rightHasRolled by remember { mutableStateOf(false) }
    
    // Geleneksel tavla için ekstra durumlar
    var traditionalFirstPlayer by remember { mutableIntStateOf(0) } // Geleneksel tavlada ilk atan oyuncu
    var traditionalFirstDice by remember { mutableIntStateOf(0) } // İlk atılan zar değeri
    var needSecondRoll by remember { mutableStateOf(false) } // İkinci zar atış gereksinimi
    
    // Kazanan belirleme
    var winnerDetermined by remember { mutableStateOf(false) }
    var gameWinner by remember { mutableIntStateOf(0) } // 1: Sol, 2: Sağ

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
                        leftDiceAnimating = true
                        leftDiceActive = true
                        val scope = CoroutineScope(Dispatchers.Main)
                        scope.launch {
                            if (gamePhase == 0) {
                                // Başlangıç zarı atma - sol taraf
                                leftStartDice = Random.nextInt(1, 7)
                                leftHasRolled = true
                                showStartDiceResult = true
                                
                                if (isModernBackgammon) {
                                    // Modern tavla: İki oyuncu da attıysa sonucu değerlendir
                                    if (rightHasRolled) {
                                        // Her iki taraf da attı, karşılaştır
                                        if (leftStartDice > rightStartDice) {
                                            gameWinner = 1
                                            leftDice1 = leftStartDice
                                            leftDice2 = rightStartDice
                                            rightDice1 = 1
                                            rightDice2 = 1
                                        } else if (rightStartDice > leftStartDice) {
                                            gameWinner = 2
                                            rightDice1 = rightStartDice
                                            rightDice2 = leftStartDice
                                            leftDice1 = 1
                                            leftDice2 = 1
                                        } else {
                                            // Berabere - tekrar at
                                            gameWinner = 0
                                            leftDiceActive = false
                                            rightDiceActive = false
                                            leftHasRolled = false
                                            rightHasRolled = false
                                            showStartDiceResult = false
                                            leftDiceAnimating = false
                                            isAnimating = false
                                            return@launch
                                        }
                                        
                                        winnerDetermined = true
                                        gamePhase = 2
                                        
                                        if (useTimer) {
                                            timeControl?.startGame(
                                                if (gameWinner == 1) BackgammonTimeControl.Player.PLAYER1 
                                                else BackgammonTimeControl.Player.PLAYER2
                                            )
                                        }
                                    }
                                    // Eğer sadece sol taraf attıysa, bekle (sağ taraf atmadı)
                                } else {
                                    // Geleneksel tavla: İlk zar sonucu
                                    traditionalFirstPlayer = 1
                                    traditionalFirstDice = leftStartDice
                                    
                                    when (traditionalFirstDice) {
                                        1 -> {
                                            gameWinner = 2 // Karşı taraf başlar
                                            winnerDetermined = true
                                            gamePhase = 2
                                            // Karşı taraf zarları
                                            rightDice1 = Random.nextInt(1, 7)
                                            delay(200)
                                            rightDice2 = Random.nextInt(1, 7)
                                            leftDice1 = 1; leftDice2 = 1
                                            
                                            if (useTimer) {
                                                timeControl?.startGame(BackgammonTimeControl.Player.PLAYER2)
                                            }
                                        }
                                        6 -> {
                                            gameWinner = 1 // Kendi başlar
                                            winnerDetermined = true  
                                            gamePhase = 2
                                            // Kendi zarları
                                            leftDice1 = Random.nextInt(1, 7)
                                            delay(200)
                                            leftDice2 = Random.nextInt(1, 7)
                                            rightDice1 = 1; rightDice2 = 1
                                            
                                            if (useTimer) {
                                                timeControl?.startGame(BackgammonTimeControl.Player.PLAYER1)
                                            }
                                        }
                                        else -> {
                                            needSecondRoll = true
                                            gamePhase = 1
                                        }
                                    }
                                }
                            } else if (gamePhase == 1 && !isModernBackgammon) {
                                // Geleneksel tavla ikinci zar atışı
                                leftStartDice = Random.nextInt(1, 7)
                                
                                val secondDice = rightStartDice
                                if (traditionalFirstDice > secondDice) {
                                    gameWinner = 1
                                } else if (secondDice > traditionalFirstDice) {
                                    gameWinner = 2
                                } else {
                                    gameWinner = 0
                                    leftDiceActive = false
                                    rightDiceActive = false
                                    showStartDiceResult = false
                                    gamePhase = 0
                                    needSecondRoll = false
                                    leftDiceAnimating = false
                                    isAnimating = false
                                    return@launch
                                }
                                
                                winnerDetermined = true
                                gamePhase = 2
                                
                                if (gameWinner == 1) {
                                    leftDice1 = Random.nextInt(1, 7)
                                    delay(200)
                                    leftDice2 = Random.nextInt(1, 7)
                                    rightDice1 = 1; rightDice2 = 1
                                } else {
                                    rightDice1 = Random.nextInt(1, 7)
                                    delay(200)
                                    rightDice2 = Random.nextInt(1, 7)
                                    leftDice1 = 1; leftDice2 = 1
                                }
                                
                                if (useTimer) {
                                    timeControl?.startGame(
                                        if (gameWinner == 1) BackgammonTimeControl.Player.PLAYER1 
                                        else BackgammonTimeControl.Player.PLAYER2
                                    )
                                }
                            } else {
                                // Normal oyun - çift zar
                                leftDice1 = Random.nextInt(1, 7)
                                delay(200)
                                leftDice2 = Random.nextInt(1, 7)
                                
                                // Süre yönetimi - Player1'e geç
                                timeControl?.switchToPlayer(BackgammonTimeControl.Player.PLAYER1)
                            }
                            leftDiceAnimating = false
                            isAnimating = false
                            
                            // GamePhase 0'da zar attıktan sonra aktif kal (sadece animasyon bitsin)
                            if (gamePhase == 0 && !winnerDetermined) {
                                leftDiceActive = true
                            }
                        }
                    }
                }
                .background(
                    if (player1IsActive || gameWinner == 1 || (gameWinner == 0 && gamePhase <= 1)) 
                        Color(0xFF1976D2).copy(alpha = 1.0f) // Parlak mavi - aktif
                    else 
                        Color(0xFF1976D2).copy(alpha = 0.4f) // Mat mavi - pasif
                ),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                if (useTimer) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Rezerv süre - daha büyük
                        Text(
                            text = "${(player1ReserveTime.inWholeMinutes)}:${(player1ReserveTime.inWholeSeconds % 60).toString().padStart(2, '0')}",
                            fontSize = 36.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.graphicsLayer(rotationZ = 90f)
                        )
                        
                        // Hamle süresi
                        Text(
                            text = "${player1MoveTime.inWholeSeconds}s",
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (player1IsActive) Color.Yellow else Color.White,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.graphicsLayer(rotationZ = 90f)
                        )
                    }
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
                        rightDiceAnimating = true
                        rightDiceActive = true
                        val scope = CoroutineScope(Dispatchers.Main)
                        scope.launch {
                            if (gamePhase == 0) {
                                // Başlangıç zarı atma - sağ taraf
                                rightStartDice = Random.nextInt(1, 7)
                                rightHasRolled = true
                                showStartDiceResult = true
                                
                                if (isModernBackgammon) {
                                    // Modern tavla: İki oyuncu da attıysa sonucu değerlendir
                                    if (leftHasRolled) {
                                        // Her iki taraf da attı, karşılaştır
                                        if (leftStartDice > rightStartDice) {
                                            gameWinner = 1
                                            leftDice1 = leftStartDice
                                            leftDice2 = rightStartDice
                                            rightDice1 = 1
                                            rightDice2 = 1
                                        } else if (rightStartDice > leftStartDice) {
                                            gameWinner = 2
                                            rightDice1 = rightStartDice
                                            rightDice2 = leftStartDice
                                            leftDice1 = 1
                                            leftDice2 = 1
                                        } else {
                                            // Berabere - tekrar at
                                            gameWinner = 0
                                            leftDiceActive = false
                                            rightDiceActive = false
                                            leftHasRolled = false
                                            rightHasRolled = false
                                            showStartDiceResult = false
                                            rightDiceAnimating = false
                                            isAnimating = false
                                            return@launch
                                        }
                                        
                                        winnerDetermined = true
                                        gamePhase = 2
                                        
                                        if (useTimer) {
                                            timeControl?.startGame(
                                                if (gameWinner == 1) BackgammonTimeControl.Player.PLAYER1 
                                                else BackgammonTimeControl.Player.PLAYER2
                                            )
                                        }
                                    }
                                    // Eğer sadece sağ taraf attıysa, bekle (sol taraf atmadı)
                                } else {
                                    // Geleneksel tavla: İlk zar sonucu (sağ)
                                    traditionalFirstPlayer = 2
                                    traditionalFirstDice = rightStartDice
                                    
                                    when (traditionalFirstDice) {
                                        1 -> {
                                            gameWinner = 1 // Karşı taraf başlar
                                            winnerDetermined = true
                                            gamePhase = 2
                                            leftDice1 = Random.nextInt(1, 7)
                                            delay(200)
                                            leftDice2 = Random.nextInt(1, 7)
                                            rightDice1 = 1; rightDice2 = 1
                                            
                                            if (useTimer) {
                                                timeControl?.startGame(BackgammonTimeControl.Player.PLAYER1)
                                            }
                                        }
                                        6 -> {
                                            gameWinner = 2 // Kendi başlar
                                            winnerDetermined = true  
                                            gamePhase = 2
                                            rightDice1 = Random.nextInt(1, 7)
                                            delay(200)
                                            rightDice2 = Random.nextInt(1, 7)
                                            leftDice1 = 1; leftDice2 = 1
                                            
                                            if (useTimer) {
                                                timeControl?.startGame(BackgammonTimeControl.Player.PLAYER2)
                                            }
                                        }
                                        else -> {
                                            needSecondRoll = true
                                            gamePhase = 1
                                        }
                                    }
                                }
                            } else if (gamePhase == 1 && !isModernBackgammon) {
                                // Geleneksel tavla ikinci zar atışı (sağ)
                                rightStartDice = Random.nextInt(1, 7)
                                
                                val secondDice = leftStartDice
                                if (traditionalFirstDice > secondDice) {
                                    gameWinner = 2
                                } else if (secondDice > traditionalFirstDice) {
                                    gameWinner = 1
                                } else {
                                    gameWinner = 0
                                    leftDiceActive = false
                                    rightDiceActive = false
                                    showStartDiceResult = false
                                    gamePhase = 0
                                    needSecondRoll = false
                                    rightDiceAnimating = false
                                    isAnimating = false
                                    return@launch
                                }
                                
                                winnerDetermined = true
                                gamePhase = 2
                                
                                if (gameWinner == 1) {
                                    leftDice1 = Random.nextInt(1, 7)
                                    delay(200)
                                    leftDice2 = Random.nextInt(1, 7)
                                    rightDice1 = 1; rightDice2 = 1
                                } else {
                                    rightDice1 = Random.nextInt(1, 7)
                                    delay(200)
                                    rightDice2 = Random.nextInt(1, 7)
                                    leftDice1 = 1; leftDice2 = 1
                                }
                                
                                if (useTimer) {
                                    timeControl?.startGame(
                                        if (gameWinner == 1) BackgammonTimeControl.Player.PLAYER1 
                                        else BackgammonTimeControl.Player.PLAYER2
                                    )
                                }
                            } else {
                                // Normal oyun - çift zar
                                rightDice1 = Random.nextInt(1, 7)
                                delay(200)
                                rightDice2 = Random.nextInt(1, 7)
                                
                                // Süre yönetimi - Player2'ye geç
                                timeControl?.switchToPlayer(BackgammonTimeControl.Player.PLAYER2)
                            }
                            rightDiceAnimating = false
                            isAnimating = false
                            
                            // GamePhase 0'da zar attıktan sonra aktif kal (sadece animasyon bitsin)
                            if (gamePhase == 0 && !winnerDetermined) {
                                rightDiceActive = true
                            }
                        }
                    }
                }
                .background(
                    if (player2IsActive || gameWinner == 2 || (gameWinner == 0 && gamePhase <= 1)) 
                        Color(0xFFD32F2F).copy(alpha = 1.0f) // Parlak kırmızı - aktif
                    else 
                        Color(0xFFD32F2F).copy(alpha = 0.4f) // Mat kırmızı - pasif
                ),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                if (useTimer) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Rezerv süre - daha büyük
                        Text(
                            text = "${(player2ReserveTime.inWholeMinutes)}:${(player2ReserveTime.inWholeSeconds % 60).toString().padStart(2, '0')}",
                            fontSize = 36.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.graphicsLayer(rotationZ = 90f)
                        )
                        
                        // Hamle süresi
                        Text(
                            text = "${player2MoveTime.inWholeSeconds}s",
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (player2IsActive) Color.Yellow else Color.White,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.graphicsLayer(rotationZ = 90f)
                        )
                    }
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
                        if (leftHasRolled) {
                            Text(
                                player1Name,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.Blue
                            )
                            DiceView(
                                value = leftStartDice, 
                                isActive = leftDiceActive,
                                isAnimating = leftDiceAnimating
                            )
                        } else {
                            Text(
                                "Başlangıç\nZarı At",
                                fontSize = 14.sp,
                                textAlign = TextAlign.Center,
                                color = Color.Blue
                            )
                            DiceView(
                                value = 1, // Pasif zar
                                isActive = false,
                                isAnimating = leftDiceAnimating
                            )
                        }
                    } else if (gamePhase == 1 && !isModernBackgammon) {
                        // Geleneksel tavla ikinci zar fazı
                        Text(
                            if (needSecondRoll) "İkinci Zar" else player1Name,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Blue
                        )
                        DiceView(
                            value = leftStartDice,
                            isActive = leftDiceActive,
                            isAnimating = leftDiceAnimating
                        )
                    } else {
                        // Normal oyun - çift zar
                        DiceView(
                            value = if (gameWinner == 1 || gameWinner == 0) leftDice1 else 1,
                            isActive = gameWinner == 1 || gameWinner == 0,
                            isAnimating = leftDiceAnimating
                        )
                        DiceView(
                            value = if (gameWinner == 1 || gameWinner == 0) leftDice2 else 1,
                            isActive = gameWinner == 1 || gameWinner == 0,
                            isAnimating = leftDiceAnimating
                        )
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
                        if (rightHasRolled) {
                            Text(
                                player2Name,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.Red
                            )
                            DiceView(
                                value = rightStartDice,
                                isActive = rightDiceActive,
                                isAnimating = rightDiceAnimating
                            )
                        } else {
                            Text(
                                "Başlangıç\nZarı At",
                                fontSize = 14.sp,
                                textAlign = TextAlign.Center,
                                color = Color.Red
                            )
                            DiceView(
                                value = 1, // Pasif zar
                                isActive = false,
                                isAnimating = rightDiceAnimating
                            )
                        }
                    } else if (gamePhase == 1 && !isModernBackgammon) {
                        // Geleneksel tavla ikinci zar fazı
                        Text(
                            if (needSecondRoll) "İkinci Zar" else player2Name,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Red
                        )
                        DiceView(
                            value = rightStartDice,
                            isActive = rightDiceActive,
                            isAnimating = rightDiceAnimating
                        )
                    } else {
                        // Normal oyun - çift zar
                        DiceView(
                            value = if (gameWinner == 2 || gameWinner == 0) rightDice1 else 1,
                            isActive = gameWinner == 2 || gameWinner == 0,
                            isAnimating = rightDiceAnimating
                        )
                        DiceView(
                            value = if (gameWinner == 2 || gameWinner == 0) rightDice2 else 1,
                            isActive = gameWinner == 2 || gameWinner == 0,
                            isAnimating = rightDiceAnimating
                        )
                    }
                }
            }
            
            // Başlangıç sonuç ve kontroller
            if (showStartDiceResult && !winnerDetermined) {
                Spacer(modifier = Modifier.height(24.dp))
                
                if (isModernBackgammon && leftHasRolled && rightHasRolled) {
                    // Modern tavla sonuç gösterimi
                    Text(
                        text = when {
                            leftStartDice > rightStartDice -> "$player1Name Başlıyor!"
                            rightStartDice > leftStartDice -> "$player2Name Başlıyor!"
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
                } else if (!isModernBackgammon) {
                    // Geleneksel tavla sonuç gösterimi
                    when (gamePhase) {
                        0 -> {
                            Text(
                                text = when (traditionalFirstDice) {
                                    1 -> "${if (traditionalFirstPlayer == 1) player2Name else player1Name} Başlıyor! (1 atıldı)"
                                    6 -> "${if (traditionalFirstPlayer == 1) player1Name else player2Name} Başlıyor! (6 atıldı)"
                                    else -> "Karşı taraf da zar atsın (${traditionalFirstDice} atıldı)"
                                },
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.Black
                            )
                        }
                        1 -> {
                            if (leftDiceActive && rightDiceActive) {
                                Text(
                                    text = when {
                                        leftStartDice > rightStartDice -> "$player1Name Başlıyor!"
                                        rightStartDice > leftStartDice -> "$player2Name Başlıyor!"
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
                            }
                        }
                    }
                }
            }
            
            // Oyuna başla butonu (sadece kazanan belirlendiğinde)
            if (winnerDetermined && gamePhase == 2) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "${if (gameWinner == 1) player1Name else player2Name} oyuna başladı!",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (gameWinner == 1) Color.Blue else Color.Red
                )
            }

            Spacer(modifier = Modifier.height(32.dp))
            
            Spacer(modifier = Modifier.height(16.dp))
            
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
fun DiceView(value: Int, isActive: Boolean = true, isAnimating: Boolean = false) {
    val backgroundColor = when {
        isAnimating -> Color.White // Parlak beyaz (animasyon sırasında)
        isActive -> Color.White    // Parlak beyaz (aktif)
        else -> Color(0xFFE0E0E0)  // Açık gri (pasif)
    }
    
    val dotColor = when {
        isAnimating -> Color.Black // Siyah noktalar (animasyon)
        isActive -> Color.Black    // Siyah noktalar (aktif)
        else -> Color.Gray         // Gri noktalar (pasif)
    }
    
    Box(
        modifier = Modifier
            .size(80.dp)
            .background(
                backgroundColor,
                androidx.compose.foundation.shape.RoundedCornerShape(12.dp)
            )
            .padding(8.dp),
        contentAlignment = Alignment.Center
    ) {
        when (value) {
            1 -> DiceOne(dotColor)
            2 -> DiceTwo(dotColor)
            3 -> DiceThree(dotColor)
            4 -> DiceFour(dotColor)
            5 -> DiceFive(dotColor)
            6 -> DiceSix(dotColor)
            else -> DiceOne(dotColor) // Varsayılan
        }
    }
}

@Composable
fun DiceOne(dotColor: Color = Color.Black) {
    Box(modifier = Modifier.fillMaxSize()) {
        DiceDot(Modifier.align(Alignment.Center), dotColor)
    }
}

@Composable
fun DiceTwo(dotColor: Color = Color.Black) {
    Box(modifier = Modifier.fillMaxSize()) {
        DiceDot(Modifier.align(Alignment.TopStart), dotColor)
        DiceDot(Modifier.align(Alignment.BottomEnd), dotColor)
    }
}

@Composable
fun DiceThree(dotColor: Color = Color.Black) {
    Box(modifier = Modifier.fillMaxSize()) {
        DiceDot(Modifier.align(Alignment.TopStart), dotColor)
        DiceDot(Modifier.align(Alignment.Center), dotColor)
        DiceDot(Modifier.align(Alignment.BottomEnd), dotColor)
    }
}

@Composable
fun DiceFour(dotColor: Color = Color.Black) {
    Box(modifier = Modifier.fillMaxSize()) {
        DiceDot(Modifier.align(Alignment.TopStart), dotColor)
        DiceDot(Modifier.align(Alignment.TopEnd), dotColor)
        DiceDot(Modifier.align(Alignment.BottomStart), dotColor)
        DiceDot(Modifier.align(Alignment.BottomEnd), dotColor)
    }
}

@Composable
fun DiceFive(dotColor: Color = Color.Black) {
    Box(modifier = Modifier.fillMaxSize()) {
        DiceDot(Modifier.align(Alignment.TopStart), dotColor)
        DiceDot(Modifier.align(Alignment.TopEnd), dotColor)
        DiceDot(Modifier.align(Alignment.Center), dotColor)
        DiceDot(Modifier.align(Alignment.BottomStart), dotColor)
        DiceDot(Modifier.align(Alignment.BottomEnd), dotColor)
    }
}

@Composable
fun DiceSix(dotColor: Color = Color.Black) {
    Box(modifier = Modifier.fillMaxSize()) {
        DiceDot(Modifier.align(Alignment.TopStart), dotColor)
        DiceDot(Modifier.align(Alignment.TopEnd), dotColor)
        Column(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .fillMaxHeight(),
            verticalArrangement = Arrangement.Center
        ) {
            DiceDot(Modifier, dotColor)
        }
        Column(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .fillMaxHeight(),
            verticalArrangement = Arrangement.Center
        ) {
            DiceDot(Modifier, dotColor)
        }
        DiceDot(Modifier.align(Alignment.BottomStart), dotColor)
        DiceDot(Modifier.align(Alignment.BottomEnd), dotColor)
    }
}

@Composable
fun DiceDot(modifier: Modifier = Modifier, color: Color = Color.Black) {
    Box(
        modifier = modifier
            .size(12.dp)
            .background(color, androidx.compose.foundation.shape.CircleShape)
    )
}



