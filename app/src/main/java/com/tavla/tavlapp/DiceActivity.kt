package com.tavla.tavlapp

import android.os.Bundle
import android.view.View
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawStyle
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.draw.rotate
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
        val matchLength = intent.getIntExtra("match_length", 11)

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
    useDiceRoller: Boolean,
    useTimer: Boolean,
    player1Name: String,
    player2Name: String,
    matchLength: Int,
    onBack: () -> Unit
) {
    // Ekran tipini belirle: Sadece süre tutucu ise satranç saati ekranı
    if (!useDiceRoller && useTimer) {
        ChessClockScreen(
            player1Name = player1Name,
            player2Name = player2Name,
            matchLength = matchLength,
            onBack = onBack
        )
        return
    }
    // Profesyonel tavla saat sistemi - USBGF/WBF kuralları
    val initialReserveTime = matchLength * 2 * 60 // 2 dakika × maç uzunluğu
    val delayTime = 12 // 12 saniye delay per hamle
    
    // Zar state'leri
    var leftDice1 by remember { mutableIntStateOf(1) }
    var leftDice2 by remember { mutableIntStateOf(1) }
    var rightDice1 by remember { mutableIntStateOf(1) }
    var rightDice2 by remember { mutableIntStateOf(1) }
    var isRolling by remember { mutableStateOf(false) }
    
    // Oyun durumu
    var gamePhase by remember { mutableStateOf(if (gameType == "Geleneksel") "opening_traditional" else "opening_modern") }
    var currentPlayer by remember { mutableIntStateOf(1) } // 1 = sol (mavi), 2 = sağ (kırmızı)
    var openingDice1 by remember { mutableIntStateOf(0) }
    var openingDice2 by remember { mutableIntStateOf(0) }
    
    // Süre state'leri - Profesyonel sistem
    var player1ReserveTime by remember { mutableIntStateOf(initialReserveTime) }
    var player2ReserveTime by remember { mutableIntStateOf(initialReserveTime) }
    var player1MoveTime by remember { mutableIntStateOf(delayTime) }
    var player2MoveTime by remember { mutableIntStateOf(delayTime) }
    var timerRunning by remember { mutableStateOf(false) }
    
    // Pasif/aktif durum
    var leftSideActive by remember { mutableStateOf(true) }
    var rightSideActive by remember { mutableStateOf(true) }
    
    // Profesyonel timer sistemi
    LaunchedEffect(timerRunning, currentPlayer) {
        if (timerRunning && useTimer && gamePhase == "playing") {
            while (timerRunning) {
                delay(1000)
                
                if (currentPlayer == 1) {
                    if (player1MoveTime > 0) {
                        player1MoveTime--
                    } else if (player1ReserveTime > 0) {
                        player1ReserveTime--
                    } else {
                        // Zaman doldu
                        timerRunning = false
                    }
                } else {
                    if (player2MoveTime > 0) {
                        player2MoveTime--
                    } else if (player2ReserveTime > 0) {
                        player2ReserveTime--
                    } else {
                        // Zaman doldu
                        timerRunning = false
                    }
                }
            }
        }
    }
    
    // Ana layout - Yatay düzen (YARISI AÇIK MAVİ, YARISI AÇIK KIRMIZI)
    Row(
        modifier = Modifier.fillMaxSize()
    ) {
        // EN SOL KENAR - Player 1 Buton Kolonu
        Column(
            modifier = Modifier
                .width(120.dp) // 60dp → 120dp (2x büyük)
                .fillMaxHeight()
                .background(Color(0xFF1976D2)) // Koyu mavi buton alanı
                .clickable {
                    if (!isRolling && leftSideActive) {
                        when (gamePhase) {
                            "opening_modern" -> {
                                if (currentPlayer == 1) {
                                    isRolling = true
                                    openingDice1 = (1..6).random()
                                    isRolling = false
                                    currentPlayer = 2
                                }
                            }
                            "playing" -> {
                                if (currentPlayer == 1) {
                                    isRolling = true
                                    leftDice1 = (1..6).random()
                                    leftDice2 = (1..6).random()
                                    isRolling = false
                                    
                                    // Sırayı karşı tarafa geçir
                                    currentPlayer = 2
                                    player2MoveTime = delayTime
                                    
                                    // Pasif/aktif durumları değiştir
                                    leftSideActive = false
                                    rightSideActive = true
                                }
                            }
                        }
                    }
                }
                .padding(4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Üst - Zaman göstergeleri
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = player1Name,
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
                
                if (useTimer) {
                    Text(
                        text = "Rezerv",
                        color = Color.White,
                        fontSize = 12.sp
                    )
                    Text(
                        text = formatTime(player1ReserveTime),
                        color = if (currentPlayer == 1) Color.Yellow else Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    
                    Text(
                        text = "Hamle",
                        color = Color.White,
                        fontSize = 12.sp
                    )
                    Text(
                        text = "${player1MoveTime}s",
                        color = if (currentPlayer == 1) Color.Red else Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            
            // Orta - ZAR AT yazısı (Buton yüksekliği 60dp = ~60sp eşdeğeri)
            Text(
                text = "ZAR\nAT",
                color = Color.White,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(8.dp))
        }
        
        // SOL YARI - Player 1 Bölgesi (AÇIK MAVİ)
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .background(if (leftSideActive) Color(0xFF64B5F6) else Color(0xFF90A4AE)), // Açık mavi / Mat gri
            contentAlignment = Alignment.Center
        ) {
            // Sol taraf zarları
            when (gamePhase) {
                "opening_modern" -> {
                    if (openingDice1 > 0 || currentPlayer == 1) {
                        ProfessionalDiceComponent(
                            value = openingDice1,
                            size = 120.dp, // 200dp → 120dp (ekrana sığacak boyut) // 2x büyük (100dp → 200dp)
                            isAnimating = isRolling && currentPlayer == 1,
                            isActive = leftSideActive
                        )
                    }
                }
                "playing" -> {
                    if (currentPlayer == 1 || leftSideActive) {
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            ProfessionalDiceComponent(leftDice1, 120.dp, isRolling && currentPlayer == 1, leftSideActive)
                            ProfessionalDiceComponent(leftDice2, 120.dp, isRolling && currentPlayer == 1, leftSideActive)
                        }
                    }
                }
            }
        }
        
        // ORTA ÇİZGİ - Ayırıcı
        Box(
            modifier = Modifier
                .width(4.dp)
                .fillMaxHeight()
                .background(Color.White)
        )
        
        // SAĞ YARI - Player 2 Bölgesi (AÇIK KIRMIZI)
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .background(if (rightSideActive) Color(0xFFEF5350) else Color(0xFF90A4AE)), // Açık kırmızı / Mat gri
            contentAlignment = Alignment.Center
        ) {
            // Sağ taraf zarları
            when (gamePhase) {
                "opening_modern" -> {
                    if (openingDice2 > 0 || currentPlayer == 2) {
                        ProfessionalDiceComponent(
                            value = openingDice2,
                            size = 120.dp, // 200dp → 120dp (ekrana sığacak boyut)
                            isAnimating = isRolling && currentPlayer == 2,
                            isActive = rightSideActive
                        )
                    }
                }
                "playing" -> {
                    if (currentPlayer == 2 || rightSideActive) {
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            ProfessionalDiceComponent(rightDice1, 120.dp, isRolling && currentPlayer == 2, rightSideActive)
                            ProfessionalDiceComponent(rightDice2, 120.dp, isRolling && currentPlayer == 2, rightSideActive)
                        }
                    }
                }
            }
        }
        
        // EN SAĞ KENAR - Player 2 Buton Kolonu
        Column(
            modifier = Modifier
                .width(120.dp) // 60dp → 120dp (2x büyük)
                .fillMaxHeight()
                .background(Color(0xFFD32F2F)) // Koyu kırmızı buton alanı
                .clickable {
                    if (!isRolling && rightSideActive) {
                        when (gamePhase) {
                            "opening_modern" -> {
                                if (currentPlayer == 2) {
                                    isRolling = true
                                    openingDice2 = (1..6).random()
                                    isRolling = false
                                    
                                    // Modern tavla: büyük atan başlar
                                    if (openingDice2 > openingDice1) {
                                        currentPlayer = 2
                                        rightDice1 = openingDice1
                                        rightDice2 = openingDice2
                                        rightSideActive = true
                                        leftSideActive = false
                                    } else if (openingDice2 < openingDice1) {
                                        currentPlayer = 1
                                        leftDice1 = openingDice1
                                        leftDice2 = openingDice2
                                        leftSideActive = true
                                        rightSideActive = false
                                    } else {
                                        // Eşit, tekrar at
                                        currentPlayer = 1
                                        openingDice1 = 0
                                        openingDice2 = 0
                                        return@clickable
                                    }
                                    
                                    gamePhase = "playing"
                                    timerRunning = true
                                    if (currentPlayer == 1) player1MoveTime = delayTime
                                    else player2MoveTime = delayTime
                                }
                            }
                            "playing" -> {
                                if (currentPlayer == 2) {
                                    isRolling = true
                                    rightDice1 = (1..6).random()
                                    rightDice2 = (1..6).random()
                                    isRolling = false
                                    
                                    // Sırayı karşı tarafa geçir
                                    currentPlayer = 1
                                    player1MoveTime = delayTime
                                    
                                    // Pasif/aktif durumları değiştir
                                    rightSideActive = false
                                    leftSideActive = true
                                }
                            }
                        }
                    }
                }
                .padding(4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Üst - Zaman göstergeleri
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = player2Name,
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
                
                if (useTimer) {
                    Text(
                        text = "Rezerv",
                        color = Color.White,
                        fontSize = 12.sp
                    )
                    Text(
                        text = formatTime(player2ReserveTime),
                        color = if (currentPlayer == 2) Color.Yellow else Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    
                    Text(
                        text = "Hamle",
                        color = Color.White,
                        fontSize = 12.sp
                    )
                    Text(
                        text = "${player2MoveTime}s",
                        color = if (currentPlayer == 2) Color.Red else Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            
            // Orta - ZAR AT yazısı (Buton yüksekliği 60dp = ~60sp eşdeğeri)
            Text(
                text = "ZAR\nAT",
                color = Color.White,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
fun ProfessionalDiceComponent(value: Int, size: androidx.compose.ui.unit.Dp, isAnimating: Boolean, isActive: Boolean) {
    val animatedRotation by animateFloatAsState(
        targetValue = if (isAnimating) 360f else 0f,
        animationSpec = if (isAnimating) {
            infiniteRepeatable(
                animation = tween(300, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            )
        } else {
            tween(0)
        }, label = ""
    )

    // Gerçekçi zar renkleri ve efektleri
    val diceColor = if (isActive) Color.White else Color(0xFFE0E0E0)
    val borderColor = if (isActive) Color(0xFF333333) else Color.DarkGray
    val shadowColor = if (isActive) Color.Black.copy(alpha = 0.6f) else Color.Gray.copy(alpha = 0.4f)
    
    // Gerçekçi 3D Gradient efekti - köşe aydınlatma
    val gradientBrush = if (isActive) {
        Brush.radialGradient(
            colors = listOf(
                Color(0xFFFFFFFF), // Parlak beyaz merkez
                Color(0xFFF8F8F8), // Hafif gri
                Color(0xFFE8E8E8), // Kenar gölgesi
                Color(0xFFD8D8D8)  // Dış kenar
            ),
            radius = size.value * 0.8f
        )
    } else {
        Brush.radialGradient(
            colors = listOf(
                Color(0xFFE0E0E0),
                Color(0xFFD0D0D0),
                Color(0xFFC0C0C0),
                Color(0xFFB0B0B0)
            ),
            radius = size.value * 0.8f
        )
    }

    Box(
        modifier = Modifier.size(size)
    ) {
        // Gerçekçi gölge efekti - offset ve blur
        repeat(4) { index ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .offset(x = (4 + index * 2).dp, y = (4 + index * 2).dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(shadowColor.copy(alpha = shadowColor.alpha / (index + 1)))
            )
        }
        
        // Ana zar kutusu
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(16.dp))
                .background(gradientBrush)
                .border(
                    width = 3.dp,
                    color = borderColor,
                    shape = RoundedCornerShape(18.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            // İç gölgelendirme efekti
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(4.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                diceColor.copy(alpha = 0.9f),
                                diceColor.copy(alpha = 0.7f)
                            ),
                            radius = size.value * 0.8f
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (value > 0) {
                    when (value) {
                        1 -> ProfessionalDicePattern1(isActive)
                        2 -> ProfessionalDicePattern2(isActive)
                        3 -> ProfessionalDicePattern3(isActive)
                        4 -> ProfessionalDicePattern4(isActive)
                        5 -> ProfessionalDicePattern5(isActive)
                        6 -> ProfessionalDicePattern6(isActive)
                    }
                }
            }
        }
    }
}

// Zaman formatı
fun formatTime(seconds: Int): String {
    val minutes = seconds / 60
    val secs = seconds % 60
    return "${minutes}:${String.format("%02d", secs)}"
}

// PROFESYONEL ZAR PIP DESENLERİ - Gerçek zar kurallarına göre
@Composable
fun ProfessionalDicePattern1(isActive: Boolean) {
    val pipColor = if (isActive) Color.Black else Color.DarkGray
    
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        // Tek nokta - merkez
        PipComponent(pipColor, 18.dp)
    }
}

@Composable
fun ProfessionalDicePattern2(isActive: Boolean) {
    val pipColor = if (isActive) Color.Black else Color.DarkGray
    
    Box(modifier = Modifier.fillMaxSize()) {
        // Çapraz düzen
        PipComponent(
            pipColor, 16.dp,
            modifier = Modifier.align(Alignment.TopStart).offset(x = 15.dp, y = 15.dp)
        )
        PipComponent(
            pipColor, 16.dp,
            modifier = Modifier.align(Alignment.BottomEnd).offset(x = (-15).dp, y = (-15).dp)
        )
    }
}

@Composable
fun ProfessionalDicePattern3(isActive: Boolean) {
    val pipColor = if (isActive) Color.Black else Color.DarkGray
    
    Box(modifier = Modifier.fillMaxSize()) {
        // Çapraz + merkez
        PipComponent(
            pipColor, 14.dp,
            modifier = Modifier.align(Alignment.TopStart).offset(x = 12.dp, y = 12.dp)
        )
        PipComponent(
            pipColor, 14.dp,
            modifier = Modifier.align(Alignment.Center)
        )
        PipComponent(
            pipColor, 14.dp,
            modifier = Modifier.align(Alignment.BottomEnd).offset(x = (-12).dp, y = (-12).dp)
        )
    }
}

@Composable
fun ProfessionalDicePattern4(isActive: Boolean) {
    val pipColor = if (isActive) Color.Black else Color.DarkGray
    
    Box(modifier = Modifier.fillMaxSize()) {
        // Dörtgen köşeler
        PipComponent(
            pipColor, 14.dp,
            modifier = Modifier.align(Alignment.TopStart).offset(x = 15.dp, y = 15.dp)
        )
        PipComponent(
            pipColor, 14.dp,
            modifier = Modifier.align(Alignment.TopEnd).offset(x = (-15).dp, y = 15.dp)
        )
        PipComponent(
            pipColor, 14.dp,
            modifier = Modifier.align(Alignment.BottomStart).offset(x = 15.dp, y = (-15).dp)
        )
        PipComponent(
            pipColor, 14.dp,
            modifier = Modifier.align(Alignment.BottomEnd).offset(x = (-15).dp, y = (-15).dp)
        )
    }
}

@Composable
fun ProfessionalDicePattern5(isActive: Boolean) {
    val pipColor = if (isActive) Color.Black else Color.DarkGray
    
    Box(modifier = Modifier.fillMaxSize()) {
        // Dörtgen köşeler + merkez (Quincunx pattern)
        PipComponent(
            pipColor, 12.dp,
            modifier = Modifier.align(Alignment.TopStart).offset(x = 12.dp, y = 12.dp)
        )
        PipComponent(
            pipColor, 12.dp,
            modifier = Modifier.align(Alignment.TopEnd).offset(x = (-12).dp, y = 12.dp)
        )
        PipComponent(
            pipColor, 12.dp,
            modifier = Modifier.align(Alignment.Center)
        )
        PipComponent(
            pipColor, 12.dp,
            modifier = Modifier.align(Alignment.BottomStart).offset(x = 12.dp, y = (-12).dp)
        )
        PipComponent(
            pipColor, 12.dp,
            modifier = Modifier.align(Alignment.BottomEnd).offset(x = (-12).dp, y = (-12).dp)
        )
    }
}

@Composable
fun ProfessionalDicePattern6(isActive: Boolean) {
    val pipColor = if (isActive) Color.Black else Color.DarkGray
    
    Box(modifier = Modifier.fillMaxSize()) {
        // İki sütun - 3+3 düzeni
        // Sol sütun
        PipComponent(
            pipColor, 12.dp,
            modifier = Modifier.align(Alignment.TopStart).offset(x = 15.dp, y = 12.dp)
        )
        PipComponent(
            pipColor, 12.dp,
            modifier = Modifier.align(Alignment.CenterStart).offset(x = 15.dp, y = 0.dp)
        )
        PipComponent(
            pipColor, 12.dp,
            modifier = Modifier.align(Alignment.BottomStart).offset(x = 15.dp, y = (-12).dp)
        )
        
        // Sağ sütun
        PipComponent(
            pipColor, 12.dp,
            modifier = Modifier.align(Alignment.TopEnd).offset(x = (-15).dp, y = 12.dp)
        )
        PipComponent(
            pipColor, 12.dp,
            modifier = Modifier.align(Alignment.CenterEnd).offset(x = (-15).dp, y = 0.dp)
        )
        PipComponent(
            pipColor, 12.dp,
            modifier = Modifier.align(Alignment.BottomEnd).offset(x = (-15).dp, y = (-12).dp)
        )
    }
}

@Composable
fun PipComponent(
    color: Color,
    size: androidx.compose.ui.unit.Dp,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(size)
            .background(
                brush = Brush.radialGradient(
                    colors = listOf(
                        color,
                        color.copy(alpha = 0.8f),
                        color.copy(alpha = 0.9f)
                    ),
                    radius = size.value * 0.8f
                ),
                shape = CircleShape
            )
            .border(
                width = 0.5.dp,
                color = color.copy(alpha = 0.3f),
                shape = CircleShape
            )
    )
}

@Composable
fun ChessClockScreen(
    player1Name: String,
    player2Name: String,
    matchLength: Int,
    onBack: () -> Unit
) {
    // Süre state'leri
    val initialReserveTime = matchLength * 2 * 60 // 2 dakika × maç uzunluğu
    val delayTime = 12 // 12 saniye delay per hamle
    
    var player1ReserveTime by remember { mutableIntStateOf(initialReserveTime) }
    var player2ReserveTime by remember { mutableIntStateOf(initialReserveTime) }
    var player1MoveTime by remember { mutableIntStateOf(delayTime) }
    var player2MoveTime by remember { mutableIntStateOf(delayTime) }
    var currentPlayer by remember { mutableIntStateOf(0) } // 0=hiçbiri, 1=player1, 2=player2
    var timerRunning by remember { mutableStateOf(false) }
    
    // Tahterevalli animasyon state'i
    var seesaw1Pressed by remember { mutableStateOf(false) }
    var seesaw2Pressed by remember { mutableStateOf(false) }
    
    // Timer LaunchedEffect
    LaunchedEffect(timerRunning, currentPlayer) {
        if (timerRunning && currentPlayer != 0) {
            while (timerRunning) {
                delay(1000)
                
                if (currentPlayer == 1) {
                    if (player1MoveTime > 0) {
                        player1MoveTime--
                    } else if (player1ReserveTime > 0) {
                        player1ReserveTime--
                    } else {
                        timerRunning = false
                    }
                } else if (currentPlayer == 2) {
                    if (player2MoveTime > 0) {
                        player2MoveTime--
                    } else if (player2ReserveTime > 0) {
                        player2ReserveTime--
                    } else {
                        timerRunning = false
                    }
                }
            }
        }
    }
    
    // Saat değiştirme fonksiyonu
    fun switchPlayer(newPlayer: Int) {
        if (newPlayer == currentPlayer) return
        
        // Önceki oyuncunun hamle süresini sıfırla
        if (currentPlayer == 1) player1MoveTime = delayTime
        else if (currentPlayer == 2) player2MoveTime = delayTime
        
        currentPlayer = newPlayer
        timerRunning = true
        
        // Tahterevalli animasyonu
        seesaw1Pressed = (newPlayer == 1)
        seesaw2Pressed = (newPlayer == 2)
    }
    
    // PROFESYONEL DGT-TARZI ELEKTRONİK SATRANÇ SAATİ TASARIMI
    ProfessionalChessClockScreen(
        player1Name = player1Name,
        player2Name = player2Name,
        player1MoveTime = player1MoveTime,
        player2MoveTime = player2MoveTime,
        player1ReserveTime = player1ReserveTime,
        player2ReserveTime = player2ReserveTime,
        currentPlayer = currentPlayer,
        isTimerRunning = timerRunning,
        onPlayerSwitch = { newPlayer -> switchPlayer(newPlayer) },
        onPauseTimer = { timerRunning = !timerRunning },
        onResetTimer = { 
            currentPlayer = 0
            timerRunning = false
            player1MoveTime = delayTime
            player2MoveTime = delayTime
            // Rezerv süreleri sıfırlanmayacak - sadece hamle süreleri
        }
    )
}

@Composable
fun ProfessionalChessClockScreen(
    player1Name: String,
    player2Name: String,
    player1MoveTime: Int,
    player2MoveTime: Int,
    player1ReserveTime: Int,
    player2ReserveTime: Int,
    currentPlayer: Int,
    isTimerRunning: Boolean,
    onPlayerSwitch: (Int) -> Unit,
    onPauseTimer: () -> Unit,
    onResetTimer: () -> Unit
) {
    // Profesyonel DGT3000 tarzı tasarım - YATAY (LANDSCAPE) DÜZEN
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF1E1E1E)) // Koyu gri profesyonel arka plan
    ) {
        Row(
            modifier = Modifier.fillMaxSize()
        ) {
            // SOL OYUNCU BÖLÜMÜ (90 derece döndürülmüş)
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .weight(1f)
                    .background(
                        if (currentPlayer == 1) Color(0xFF2E7D32) // Aktif yeşil
                        else Color(0xFF424242) // Pasif gri
                    )
                    .clickable { onPlayerSwitch(1) }
                    .border(
                        width = if (currentPlayer == 1) 4.dp else 1.dp,
                        color = if (currentPlayer == 1) Color(0xFF4CAF50) else Color(0xFF757575)
                    )
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                // 90 derece döndürme - sol oyuncu için
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.rotate(90f)
                ) {
                    // Oyuncu adı
                    Text(
                        text = player1Name,
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // Ana süre - büyük ekran
                    Text(
                        text = formatTime(player1ReserveTime),
                        color = Color.White,
                        fontSize = 48.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                    )
                    
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    // Hamle süresi
                    Text(
                        text = "Hamle: ${player1MoveTime}s",
                        color = if (currentPlayer == 1) Color(0xFFFFEB3B) else Color(0xFFBDBDBD),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
                
                // Aktif oyuncu göstergesi
                if (currentPlayer == 1) {
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .background(Color(0xFF4CAF50), CircleShape)
                            .align(Alignment.TopEnd)
                    )
                }
            }
            
            // ORTA KONTROL PANELİ - DİKEY
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(120.dp)
                    .background(Color(0xFF37474F))
                    .padding(vertical = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    verticalArrangement = Arrangement.SpaceEvenly,
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxHeight()
                ) {
                    // PAUSE/PLAY Butonu
                    Button(
                        onClick = onPauseTimer,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isTimerRunning) Color(0xFFFF5722) else Color(0xFF4CAF50)
                        ),
                        modifier = Modifier
                            .height(60.dp)
                            .width(90.dp)
                    ) {
                        Text(
                            text = if (isTimerRunning) "PAUSE" else "PLAY",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    
                    // Timer durumu göstergesi
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = if (isTimerRunning) "RUNNING" else "PAUSED",
                            color = if (isTimerRunning) Color(0xFF4CAF50) else Color(0xFFFF9800),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "DGT",
                            color = Color(0xFFBDBDBD),
                            fontSize = 9.sp
                        )
                        Text(
                            text = "Timer",
                            color = Color(0xFFBDBDBD),
                            fontSize = 9.sp
                        )
                    }
                    
                    // RESET Butonu
                    Button(
                        onClick = onResetTimer,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF757575)
                        ),
                        modifier = Modifier
                            .height(60.dp)
                            .width(90.dp)
                    ) {
                        Text(
                            text = "RESET",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
            
            // SAĞ OYUNCU BÖLÜMÜ (-90 derece döndürülmüş)
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .weight(1f)
                    .background(
                        if (currentPlayer == 2) Color(0xFF2E7D32) // Aktif yeşil
                        else Color(0xFF424242) // Pasif gri
                    )
                    .clickable { onPlayerSwitch(2) }
                    .border(
                        width = if (currentPlayer == 2) 4.dp else 1.dp,
                        color = if (currentPlayer == 2) Color(0xFF4CAF50) else Color(0xFF757575)
                    )
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                // -90 derece döndürme - sağ oyuncu için
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.rotate(-90f)
                ) {
                    // Oyuncu adı
                    Text(
                        text = player2Name,
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // Ana süre - büyük ekran
                    Text(
                        text = formatTime(player2ReserveTime),
                        color = Color.White,
                        fontSize = 48.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                    )
                    
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    // Hamle süresi
                    Text(
                        text = "Hamle: ${player2MoveTime}s",
                        color = if (currentPlayer == 2) Color(0xFFFFEB3B) else Color(0xFFBDBDBD),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
                
                // Aktif oyuncu göstergesi
                if (currentPlayer == 2) {
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .background(Color(0xFF4CAF50), CircleShape)
                            .align(Alignment.TopEnd)
                    )
                }
            }
        }
    }
}