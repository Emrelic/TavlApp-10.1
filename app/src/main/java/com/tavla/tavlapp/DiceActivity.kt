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
                    color = Color(0xFF1E1E1E) // Koyu gri, siyah değil
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
    // YENİ: SimpleIntegratedScreen kullan - tek tıklama zar + süre sistemi
    SimpleIntegratedScreen(
        gameType = gameType,
        player1Name = player1Name,
        player2Name = player2Name,
        matchLength = matchLength,
        onBack = onBack
    )
    return
    
    // Modern tavla süre tutucu sistemi (useTimer = true)
    // useDiceRoller durumuna bakılmaksızın ana DiceScreen çalışacak
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
    var gamePhase by remember { mutableStateOf(if (gameType == "Geleneksel") "opening_traditional" else "initial_question") }
    var currentPlayer by remember { mutableIntStateOf(0) } // 0 = soru işareti aşaması, 1 = sol (mavi), 2 = sağ (kırmızı)
    var openingDice1 by remember { mutableIntStateOf(0) }
    var openingDice2 by remember { mutableIntStateOf(0) }
    var turnCompleted by remember { mutableStateOf(false) } // Tur tamamlanma kontrolü
    
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
    var animatingDicePosition by remember { mutableStateOf(0f) } // Sürükleme pozisyonu
    
    // Sürükleme animasyonu ve oyun başlangıcı
    LaunchedEffect(showDragAnimation) {
        if (showDragAnimation) {
            delay(1000) // 1 saniye küçük zarın sürüklenme animasyonu
            showDragAnimation = false
        }
    }
    
    // Oyun başlangıcında karşılaştırma
    LaunchedEffect(openingDice1, openingDice2) {
        if (openingDice1 > 0 && openingDice2 > 0 && gamePhase == "opening_modern") {
            delay(500) // Kısa bir gecikme
            
            if (openingDice1 > openingDice2) {
                // Sol büyük attı, küçük zar sol tarafa sürüklenecek
                showDragAnimation = true
                currentPlayer = 1
                leftDice1 = openingDice1
                leftDice2 = openingDice2
                gamePhase = "playing"
                rightSideActive = false
                leftSideActive = true
                timerRunning = useTimer
                player1MoveTime = delayTime
            } else if (openingDice2 > openingDice1) {
                // Sağ büyük attı, küçük zar sağ tarafa sürüklenecek
                showDragAnimation = true
                currentPlayer = 2
                rightDice1 = openingDice1
                rightDice2 = openingDice2
                gamePhase = "playing"
                leftSideActive = false
                rightSideActive = true
                timerRunning = useTimer
                player2MoveTime = delayTime
            } else {
                // Eşit geldi, zarları sıfırla tekrar atılsın
                openingDice1 = 0
                openingDice2 = 0
                currentPlayer = 0 // Tekrar baştan başla
                gamePhase = "initial_question"
            }
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
    
    // DGT3000 Tarzı Ana Layout - Profesyonel Satranç Saati Görünümü
    Row(
        modifier = Modifier.fillMaxSize()
    ) {
        // SOL KENAR - Süre Göstergeli Dikey Buton
        Column(
            modifier = Modifier
                .width(100.dp)
                .fillMaxHeight()
                .background(if (currentPlayer == 1) Color(0xFF4CAF50) else Color(0xFF616161)) // Yeşil/Gri
                .clickable {
                    when (gamePhase) {
                        "initial_question" -> {
                            // İlk aşama: Sol oyuncu zar atıyor
                            if (!isRolling) {
                                isRolling = true
                                // Zar sesi efekti
                                try {
                                    val toneGenerator = ToneGenerator(AudioManager.STREAM_MUSIC, 50)
                                    toneGenerator.startTone(ToneGenerator.TONE_PROP_BEEP, 100)
                                    toneGenerator.release()
                                } catch (e: Exception) { }
                                openingDice1 = (1..6).random()
                                isRolling = false
                                currentPlayer = 1 // Sol oyuncu zar attı
                                
                                if (openingDice1 > 0 && openingDice2 > 0) {
                                    gamePhase = "opening_modern"
                                }
                            }
                        }
                        "opening_modern" -> {
                            if (!isRolling && currentPlayer == 1) {
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
                                
                                // Karşılaştırma LaunchedEffect'te yapılacak
                            }
                        }
                        "playing" -> {
                            if (!isRolling && currentPlayer == 1 && !turnCompleted) {
                                // Sol oyuncu zarı atıyor
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
                                turnCompleted = true // Sol oyuncu zarını attı, tur tamamlandı
                            } else if (turnCompleted && currentPlayer == 1) {
                                // Sol oyuncu tur tamamlama dokunuşu yapıyor (hamleyi bitiriyor)
                                turnCompleted = false
                                currentPlayer = 2  // SAĞ OYUNCUNUN SIRASI
                                player2MoveTime = delayTime
                                timerRunning = useTimer
                                
                                // Pasif/aktif durumları değiştir - SAĞ TARAF YEŞİL OLMALI
                                leftSideActive = false   // Sol pasif (gri)
                                rightSideActive = true   // Sağ aktif (yeşil)
                            }
                        }
                    }
                }
                .padding(8.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // SOL OYUNCU SÜRELERİ - 90° Döndürülmüş (Yukarıdan aşağıya okunur)
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .rotate(90f), // Sol oyuncu için 90° döndür
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Oyuncu adı - küçük
                Text(
                    text = player1Name,
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                if (useTimer) {
                    // Hamle süresi - küçük puntolar (SOL OYUNCU YUKARI)
                    Text(
                        text = "Hamle: ${player1MoveTime}s",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Normal
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // Rezerv süre - büyük puntolar (SOL OYUNCU AŞAĞI)
                    Text(
                        text = formatTime(player1ReserveTime),
                        color = Color.White,
                        fontSize = 36.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                    )
                }
            }
        }
        
        // ORTA ALAN - Zarlar
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .background(Color(0xFF2C2C2C)), // Koyu gri orta alan
            contentAlignment = Alignment.Center
        ) {
            // Sol oyuncu zarları
            when (gamePhase) {
                "initial_question" -> {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(24.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        ProfessionalDiceComponent(0, 100.dp, false, false) // Sol soru işareti
                        ProfessionalDiceComponent(0, 100.dp, false, false) // Sağ soru işareti
                    }
                }
                "opening_modern" -> {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(24.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (openingDice1 > 0) {
                            ProfessionalDiceComponent(openingDice1, 100.dp, isRolling, currentPlayer == 1)
                        } else {
                            ProfessionalDiceComponent(0, 100.dp, false, false)
                        }
                        if (openingDice2 > 0) {
                            ProfessionalDiceComponent(openingDice2, 100.dp, isRolling, currentPlayer == 2)
                        } else {
                            ProfessionalDiceComponent(0, 100.dp, false, false)
                        }
                    }
                }
                "playing" -> {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(24.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Sol oyuncu zarları
                        if (leftDice1 > 0 && leftDice2 > 0) {
                            ProfessionalDiceComponent(leftDice1, 100.dp, isRolling && currentPlayer == 1, currentPlayer == 1)
                            ProfessionalDiceComponent(leftDice2, 100.dp, isRolling && currentPlayer == 1, currentPlayer == 1)
                        }
                        // Sağ oyuncu zarları
                        if (rightDice1 > 0 && rightDice2 > 0) {
                            ProfessionalDiceComponent(rightDice1, 100.dp, isRolling && currentPlayer == 2, currentPlayer == 2)
                            ProfessionalDiceComponent(rightDice2, 100.dp, isRolling && currentPlayer == 2, currentPlayer == 2)
                        }
                    }
                }
            }
        }
        
        // SAĞ KENAR - Süre Göstergeli Dikey Buton
        Column(
            modifier = Modifier
                .width(100.dp)
                .fillMaxHeight()
                .background(if (currentPlayer == 2) Color(0xFF4CAF50) else Color(0xFF616161)) // Yeşil/Gri
                .clickable {
                    when (gamePhase) {
                        "initial_question" -> {
                            if (!isRolling && currentPlayer == 1) {
                                isRolling = true
                                try {
                                    val toneGenerator = ToneGenerator(AudioManager.STREAM_MUSIC, 50)
                                    toneGenerator.startTone(ToneGenerator.TONE_PROP_BEEP, 100)
                                    toneGenerator.release()
                                } catch (e: Exception) { }
                                openingDice2 = (1..6).random()
                                isRolling = false
                                currentPlayer = 2
                                
                                if (openingDice1 > 0 && openingDice2 > 0) {
                                    gamePhase = "opening_modern"
                                }
                            }
                        }
                        "playing" -> {
                            if (!isRolling && currentPlayer == 2 && !turnCompleted) {
                                isRolling = true
                                try {
                                    val toneGenerator = ToneGenerator(AudioManager.STREAM_MUSIC, 50)
                                    toneGenerator.startTone(ToneGenerator.TONE_PROP_BEEP, 100)
                                    toneGenerator.release()
                                } catch (e: Exception) { }
                                rightDice1 = (1..6).random()
                                rightDice2 = (1..6).random()
                                isRolling = false
                                turnCompleted = true
                            } else if (turnCompleted && currentPlayer == 2) {
                                turnCompleted = false
                                currentPlayer = 1
                                player1MoveTime = delayTime
                                timerRunning = useTimer
                                rightSideActive = false
                                leftSideActive = true
                            }
                        }
                    }
                }
                .padding(8.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // SAĞ OYUNCU SÜRELERİ - (-90°) Döndürülmüş (Aşağıdan yukarıya okunur)
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .rotate(-90f), // Sağ oyuncu için -90° döndür
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Oyuncu adı - küçük
                Text(
                    text = player2Name,
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                if (useTimer) {
                    // Rezerv süre - büyük puntolar (SAĞ OYUNCU YUKARI)
                    Text(
                        text = formatTime(player2ReserveTime),
                        color = Color.White,
                        fontSize = 36.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // Hamle süresi - küçük puntolar (SAĞ OYUNCU AŞAĞI)
                    Text(
                        text = "Hamle: ${player2MoveTime}s",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Normal
                    )
                }
            }
        }
    }
}

@Composable
fun ProfessionalDiceComponent(value: Int, size: androidx.compose.ui.unit.Dp, isAnimating: Boolean, isActive: Boolean) {
    val animatedRotation by animateFloatAsState(
        targetValue = if (isAnimating) 720f else 0f,
        animationSpec = if (isAnimating) {
            infiniteRepeatable(
                animation = tween(800, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Restart
            )
        } else {
            tween(0)
        }, label = ""
    )
    
    val diceColor = if (isActive) Color.White else Color(0xFFE0E0E0)
    val borderColor = if (isActive) Color(0xFF333333) else Color.DarkGray
    
    val gradientBrush = if (isActive) {
        Brush.radialGradient(
            colors = listOf(
                Color(0xFFFFFFFF),
                Color(0xFFF8F8F8),
                Color(0xFFE8E8E8),
                Color(0xFFD8D8D8)
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
                when (value) {
                    0 -> {
                        // Soru işareti göster
                        Text(
                            text = "?",
                            color = Color.Black,
                            fontSize = (size.value * 0.5f).sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
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

fun formatTime(seconds: Int): String {
    val minutes = seconds / 60
    val secs = seconds % 60
    return "${minutes}:${String.format("%02d", secs)}"
}

@Composable
fun ProfessionalDicePattern1(isActive: Boolean) {
    val pipColor = if (isActive) Color.Black else Color.DarkGray
    
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        PipComponent(pipColor, 18.dp)
    }
}

@Composable
fun ProfessionalDicePattern2(isActive: Boolean) {
    val pipColor = if (isActive) Color.Black else Color.DarkGray
    
    Box(modifier = Modifier.fillMaxSize()) {
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
    // Basit satranç saati implementasyonu
    Text("Chess Clock - $player1Name vs $player2Name")
}

