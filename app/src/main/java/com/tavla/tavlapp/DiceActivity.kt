package com.tavla.tavlapp

import android.os.Bundle
import android.view.View
import android.media.AudioManager
import android.media.ToneGenerator
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
    
    // Animasyon state'leri
    var isAnimatingDice by remember { mutableStateOf(false) }
    var showDragAnimation by remember { mutableStateOf(false) }
    
    // Sürükleme animasyonu
    LaunchedEffect(showDragAnimation) {
        if (showDragAnimation) {
            delay(1000) // 1 saniye bekle
            showDragAnimation = false
        }
    }
    
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
                    if (!isRolling && currentPlayer == 1) {
                        // SOL TARAFA BASILDI - Karşı tarafın (sağın) saati başlamalı
                        when (gamePhase) {
                            "opening_modern" -> {
                                // Sol tarafa basıldı, sol zarı at
                                isRolling = true
                                // Zar sesi efekti
                                try {
                                    val toneGenerator = ToneGenerator(AudioManager.STREAM_MUSIC, 50)
                                    toneGenerator.startTone(ToneGenerator.TONE_PROP_BEEP, 100)
                                    toneGenerator.release()
                                } catch (e: Exception) { }
                                openingDice1 = (1..6).random()
                                isRolling = false
                                
                                // Her iki zar da atıldıysa karşılaştır
                                if (openingDice1 > 0 && openingDice2 > 0) {
                                    if (openingDice1 > openingDice2) {
                                        // Sol büyük attı, sol başlar
                                        showDragAnimation = true
                                        currentPlayer = 1
                                        leftDice1 = openingDice1
                                        leftDice2 = openingDice2
                                        gamePhase = "playing"
                                        rightSideActive = false
                                        timerRunning = useTimer
                                    } else if (openingDice2 > openingDice1) {
                                        // Sağ büyük attı, sağ başlar
                                        showDragAnimation = true
                                        currentPlayer = 2
                                        rightDice1 = openingDice1
                                        rightDice2 = openingDice2
                                        gamePhase = "playing" 
                                        leftSideActive = false
                                        timerRunning = useTimer
                                    } else {
                                        // Eşit, tekrar at
                                        openingDice1 = 0
                                        openingDice2 = 0
                                    }
                                }
                            }
                            "playing" -> {
                                // SOL TARAFA BASILDI - Karşı tarafın (sağın) saati başlamalı
                                if (!isRolling) {
                                    isRolling = true
                                    // Zar sesi efekti
                                    try {
                                        val toneGenerator = ToneGenerator(AudioManager.STREAM_MUSIC, 50)
                                        toneGenerator.startTone(ToneGenerator.TONE_PROP_BEEP, 100)
                                        toneGenerator.release()
                                    } catch (e: Exception) { }
                                    leftDice1 = (1..6).random()
                                    leftDice2 = (1..6).random()
                                    isRolling = false
                                    
                                    // Sol tarafa basıldı = Sol oyuncu hamlesi bitti, SAĞ oyuncunun saati çalışmalı
                                    println("SOL TARAF BASILDI: Sol oyuncu bitti, SAĞ oyuncunun saati çalışacak")
                                    currentPlayer = 2  // SAĞ OYUNCUNUN SAATİ ÇALIŞACAK
                                    player2MoveTime = delayTime
                                    timerRunning = useTimer // TIMER'I BAŞLAT
                                    println("currentPlayer = $currentPlayer, timerRunning = $timerRunning, useTimer = $useTimer")
                                    
                                    // Pasif/aktif durumları değiştir - SAĞ TARAF YEŞİL OLMALI
                                    leftSideActive = false   // Sol pasif
                                    rightSideActive = true   // Sağ aktif (yeşil)
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
                        color = if (currentPlayer == 2) Color.Yellow else Color.White, // ÇAPRAZLAMA
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
                        color = if (currentPlayer == 2) Color.Red else Color.White, // ÇAPRAZLAMA
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
                .background(if (currentPlayer == 1) Color(0xFF4CAF50) else Color(0xFF90A4AE)), // SOL YEŞİL: Sağ tıklama→currentPlayer=1
            contentAlignment = Alignment.Center
        ) {
            // Sol taraf zarları
            when (gamePhase) {
                "opening_modern" -> {
                    if (openingDice1 > 0) {
                        ProfessionalDiceComponent(
                            value = openingDice1,
                            size = 120.dp, // 200dp → 120dp (ekrana sığacak boyut) // 2x büyük (100dp → 200dp)
                            isAnimating = isRolling,
                            isActive = currentPlayer == 1
                        )
                    }
                }
                "playing" -> {
                    if (leftDice1 > 0 && leftDice2 > 0) {
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            ProfessionalDiceComponent(leftDice1, 120.dp, isRolling && currentPlayer == 1, currentPlayer == 1)
                            ProfessionalDiceComponent(leftDice2, 120.dp, isRolling && currentPlayer == 1, currentPlayer == 1)
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
                .background(if (currentPlayer == 2) Color(0xFF4CAF50) else Color(0xFF90A4AE)), // SAĞ YEŞİL: Sol tıklama→currentPlayer=2
            contentAlignment = Alignment.Center
        ) {
            // Sağ taraf zarları
            when (gamePhase) {
                "opening_modern" -> {
                    if (openingDice2 > 0) {
                        ProfessionalDiceComponent(
                            value = openingDice2,
                            size = 120.dp, // 200dp → 120dp (ekrana sığacak boyut)
                            isAnimating = isRolling,
                            isActive = currentPlayer == 2
                        )
                    }
                }
                "playing" -> {
                    if (rightDice1 > 0 && rightDice2 > 0) {
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            ProfessionalDiceComponent(rightDice1, 120.dp, isRolling && currentPlayer == 2, currentPlayer == 2)
                            ProfessionalDiceComponent(rightDice2, 120.dp, isRolling && currentPlayer == 2, currentPlayer == 2)
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
                    if (!isRolling && currentPlayer == 2) {
                        when (gamePhase) {
                            "opening_modern" -> {
                                // Sağ tarafa basıldı, sağ zarı at
                                isRolling = true
                                // Zar sesi efekti
                                try {
                                    val toneGenerator = ToneGenerator(AudioManager.STREAM_MUSIC, 50)
                                    toneGenerator.startTone(ToneGenerator.TONE_PROP_BEEP, 100)
                                    toneGenerator.release()
                                } catch (e: Exception) { }
                                openingDice2 = (1..6).random()
                                isRolling = false
                                
                                // Her iki zar da atıldıysa karşılaştır
                                if (openingDice1 > 0 && openingDice2 > 0) {
                                    if (openingDice1 > openingDice2) {
                                        // Sol büyük attı, sol başlar
                                        showDragAnimation = true
                                        currentPlayer = 1
                                        leftDice1 = openingDice1
                                        leftDice2 = openingDice2
                                        gamePhase = "playing"
                                        rightSideActive = false
                                        timerRunning = useTimer
                                        player1MoveTime = delayTime
                                    } else if (openingDice2 > openingDice1) {
                                        // Sağ büyük attı, sağ başlar
                                        showDragAnimation = true
                                        currentPlayer = 2
                                        rightDice1 = openingDice1
                                        rightDice2 = openingDice2
                                        gamePhase = "playing" 
                                        leftSideActive = false
                                        timerRunning = useTimer
                                        player2MoveTime = delayTime
                                    } else {
                                        // Eşit, tekrar at
                                        openingDice1 = 0
                                        openingDice2 = 0
                                    }
                                }
                            }
                            "playing" -> {
                                // SAĞ TARAFA BASILDI - Karşı tarafın (solun) saati başlamalı
                                if (!isRolling) {
                                    isRolling = true
                                    // Zar sesi efekti
                                    try {
                                        val toneGenerator = ToneGenerator(AudioManager.STREAM_MUSIC, 50)
                                        toneGenerator.startTone(ToneGenerator.TONE_PROP_BEEP, 100)
                                        toneGenerator.release()
                                    } catch (e: Exception) { }
                                    rightDice1 = (1..6).random()
                                    rightDice2 = (1..6).random()
                                    isRolling = false
                                    
                                    // Sağ tarafa basıldı = Sağ oyuncu hamlesi bitti, SOL oyuncunun saati çalışmalı
                                    println("SAĞ TARAF BASILDI: Sağ oyuncu bitti, SOL oyuncunun saati çalışacak")
                                    currentPlayer = 1  // SOL OYUNCUNUN SAATİ ÇALIŞACAK
                                    player1MoveTime = delayTime
                                    timerRunning = useTimer // TIMER'I BAŞLAT
                                    println("currentPlayer = $currentPlayer, timerRunning = $timerRunning, useTimer = $useTimer")
                                    
                                    // Pasif/aktif durumları değiştir - SOL TARAF YEŞİL OLMALI
                                    rightSideActive = false  // Sağ pasif
                                    leftSideActive = true    // Sol aktif (yeşil)
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
                        color = if (currentPlayer == 1) Color.Yellow else Color.White, // ÇAPRAZLAMA
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
                        color = if (currentPlayer == 1) Color.Red else Color.White, // ÇAPRAZLAMA
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
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Maçı Bitir butonu
            Button(
                onClick = { onBack() },
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFD32F2F) // Kırmızı
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(40.dp)
                    .padding(horizontal = 8.dp)
            ) {
                Text(
                    text = "MAÇI BİTİR",
                    color = Color.White,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
fun ProfessionalDiceComponent(value: Int, size: androidx.compose.ui.unit.Dp, isAnimating: Boolean, isActive: Boolean) {
    val animatedRotation by animateFloatAsState(
        targetValue = if (isAnimating) 720f else 0f, // 2 tam dönüş
        animationSpec = if (isAnimating) {
            infiniteRepeatable(
                animation = tween(800, easing = FastOutSlowInEasing), // Daha uzun, gerçekçi easing
                repeatMode = RepeatMode.Restart
            )
        } else {
            tween(0)
        }, label = ""
    )
    
    // Y ekseni dönüşü için ayrı animasyon
    val animatedRotationY by animateFloatAsState(
        targetValue = if (isAnimating) 540f else 0f, // 1.5 tam dönüş
        animationSpec = if (isAnimating) {
            infiniteRepeatable(
                animation = tween(700, easing = FastOutSlowInEasing),
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
        modifier = Modifier
            .size(size)
            .rotate(animatedRotation)
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
    // Ayar popup state
    var showSettingsDialog by remember { mutableStateOf(false) }
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
                        // ÇAPRAZLAMA: Sol taraf currentPlayer==1 olduğunda yeşil olmalı (Sağ tıklama)
                        if (currentPlayer == 1) Color(0xFF2E7D32) // Aktif yeşil
                        else Color(0xFF424242) // Pasif gri
                    )
                    .clickable { 
                        // SOL TARAFA TIKLANINCA SAĞ OYUNCUNUN SAATİ ÇALIŞMALI (ÇAPRAZLAMA)
                        onPlayerSwitch(2)  // 1 değil 2 olmalı!
                    }
                    .border(
                        // ÇAPRAZLAMA: Sol taraf currentPlayer==1 olduğunda aktif border (Sağ tıklama)
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
                        color = if (currentPlayer == 2) Color(0xFFFFEB3B) else Color(0xFFBDBDBD), // ÇAPRAZLAMA
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
                
                // Aktif oyuncu göstergesi - ÇAPRAZLAMA
                if (currentPlayer == 2) {
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
                            .height(50.dp)
                            .width(90.dp)
                    ) {
                        Text(
                            text = "RESET",
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    
                    // AYARLAR Butonu
                    Button(
                        onClick = { showSettingsDialog = true },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF607D8B)
                        ),
                        modifier = Modifier
                            .height(50.dp)
                            .width(90.dp)
                    ) {
                        Text(
                            text = "AYAR",
                            color = Color.White,
                            fontSize = 11.sp,
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
                        // ÇAPRAZLAMA: Sağ taraf currentPlayer==2 olduğunda yeşil olmalı (Sol tıklama)
                        if (currentPlayer == 2) Color(0xFF2E7D32) // Aktif yeşil
                        else Color(0xFF424242) // Pasif gri
                    )
                    .clickable { 
                        // SAĞ TARAFA TIKLANINCA SOL OYUNCUNUN SAATİ ÇALIŞMALI (ÇAPRAZLAMA)
                        onPlayerSwitch(1)  // 2 değil 1 olmalı!
                    }
                    .border(
                        // ÇAPRAZLAMA: Sağ taraf currentPlayer==2 olduğunda aktif border (Sol tıklama)
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
                        color = if (currentPlayer == 1) Color(0xFFFFEB3B) else Color(0xFFBDBDBD), // ÇAPRAZLAMA
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
                
                // Aktif oyuncu göstergesi - ÇAPRAZLAMA
                if (currentPlayer == 1) {
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .background(Color(0xFF4CAF50), CircleShape)
                            .align(Alignment.TopEnd)
                    )
                }
            }
        }
        
        // AYARLAR POPUP DİALOG
        if (showSettingsDialog) {
            ChessClockSettingsDialog(
                onDismiss = { showSettingsDialog = false },
                onApplySettings = { /* Ayarları uygula */ }
            )
        }
    }
}

@Composable
fun ChessClockSettingsDialog(
    onDismiss: () -> Unit,
    onApplySettings: (ChessClockSettings) -> Unit
) {
    // Ayar state'leri
    var selectedTimeControl by remember { mutableStateOf("Fischer") }
    var initialTime by remember { mutableIntStateOf(15) } // dakika
    var incrementTime by remember { mutableIntStateOf(10) } // saniye
    var delayTime by remember { mutableIntStateOf(12) } // saniye
    var selectedGameMode by remember { mutableStateOf("Blitz") }
    
    // Backdrop overlay
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.7f))
            .clickable { onDismiss() },
        contentAlignment = Alignment.Center
    ) {
        // Ana ayar paneli
        Card(
            modifier = Modifier
                .fillMaxWidth(0.85f)
                .fillMaxHeight(0.8f)
                .clickable { /* Dialog içi tıklamasını engelle */ },
            colors = CardDefaults.cardColors(containerColor = Color(0xFF2E2E2E)),
            elevation = CardDefaults.cardElevation(defaultElevation = 16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Başlık
                Text(
                    text = "⚙️ Satranç Saati Ayarları",
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
                
                // Süre kontrolü seçimi
                Text(
                    text = "Süre Kontrolü:",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    listOf("Fischer", "Bronstein", "Simple").forEach { mode ->
                        FilterChip(
                            onClick = { selectedTimeControl = mode },
                            label = { Text(mode) },
                            selected = selectedTimeControl == mode,
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Color(0xFF4CAF50),
                                selectedLabelColor = Color.White
                            )
                        )
                    }
                }
                
                // Oyun modu seçimi
                Text(
                    text = "Oyun Modu:",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    listOf("Bullet", "Blitz", "Rapid", "Classical").forEach { mode ->
                        FilterChip(
                            onClick = { 
                                selectedGameMode = mode
                                // Preset değerleri ayarla
                                when (mode) {
                                    "Bullet" -> { initialTime = 1; incrementTime = 1 }
                                    "Blitz" -> { initialTime = 5; incrementTime = 3 }
                                    "Rapid" -> { initialTime = 15; incrementTime = 10 }
                                    "Classical" -> { initialTime = 90; incrementTime = 30 }
                                }
                            },
                            label = { Text(mode) },
                            selected = selectedGameMode == mode,
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Color(0xFF2196F3),
                                selectedLabelColor = Color.White
                            )
                        )
                    }
                }
                
                // Başlangıç süresi
                Text(
                    text = "Başlangıç Süresi: $initialTime dakika",
                    color = Color.White,
                    fontSize = 14.sp
                )
                Slider(
                    value = initialTime.toFloat(),
                    onValueChange = { initialTime = it.toInt() },
                    valueRange = 1f..180f,
                    steps = 178,
                    colors = SliderDefaults.colors(
                        thumbColor = Color(0xFF4CAF50),
                        activeTrackColor = Color(0xFF4CAF50)
                    )
                )
                
                // Increment/Delay süresi
                Text(
                    text = "${if (selectedTimeControl == "Fischer") "Increment" else "Delay"}: $incrementTime saniye",
                    color = Color.White,
                    fontSize = 14.sp
                )
                Slider(
                    value = incrementTime.toFloat(),
                    onValueChange = { incrementTime = it.toInt() },
                    valueRange = 0f..60f,
                    steps = 59,
                    colors = SliderDefaults.colors(
                        thumbColor = Color(0xFF4CAF50),
                        activeTrackColor = Color(0xFF4CAF50)
                    )
                )
                
                // Açıklama metni
                Text(
                    text = when (selectedTimeControl) {
                        "Fischer" -> "Fischer: Her hamleden sonra süre eklenir"
                        "Bronstein" -> "Bronstein: Harcanan süre kadar eklenir (max delay)"
                        "Simple" -> "Simple: Sabit delay süresi"
                        else -> ""
                    },
                    color = Color(0xFFBDBDBD),
                    fontSize = 12.sp,
                    modifier = Modifier.fillMaxWidth()
                )
                
                Spacer(modifier = Modifier.weight(1f))
                
                // Butonlar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Button(
                        onClick = onDismiss,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF757575)
                        ),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("İptal", color = Color.White)
                    }
                    
                    Spacer(modifier = Modifier.width(12.dp))
                    
                    Button(
                        onClick = {
                            val settings = ChessClockSettings(
                                timeControl = selectedTimeControl,
                                initialTimeMinutes = initialTime,
                                incrementSeconds = incrementTime,
                                gameMode = selectedGameMode
                            )
                            onApplySettings(settings)
                            onDismiss()
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF4CAF50)
                        ),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Uygula", color = Color.White)
                    }
                }
            }
        }
    }
}

// Ayar data class'ı
data class ChessClockSettings(
    val timeControl: String,
    val initialTimeMinutes: Int,
    val incrementSeconds: Int,
    val gameMode: String
)