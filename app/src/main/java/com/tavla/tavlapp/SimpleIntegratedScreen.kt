package com.tavla.tavlapp

import android.media.AudioManager
import android.media.ToneGenerator
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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Composable
fun SimpleIntegratedScreen(
    gameType: String,
    player1Name: String,
    player2Name: String,
    matchLength: Int,
    onBack: () -> Unit
) {
    // === OYUN DURUMU ===
    var currentPlayer by remember { mutableIntStateOf(1) } // 1 = Player1, 2 = Player2
    var gamePhase by remember { mutableStateOf("playing") } // "playing", "paused"
    
    // === ZAR DURUMU ===
    var dice1 by remember { mutableIntStateOf(1) }
    var dice2 by remember { mutableIntStateOf(2) }
    var isRolling by remember { mutableStateOf(false) }
    var diceMarkedAsPlayed by remember { mutableStateOf(false) }
    
    // === SÜRE DURUMU ===
    val reserveTimePerPlayer = matchLength * 2 * 60 // 2dk × maç uzunluğu  
    val moveTimeDelay = 12 // 12 saniye
    
    var player1ReserveTime by remember { mutableIntStateOf(reserveTimePerPlayer) }
    var player2ReserveTime by remember { mutableIntStateOf(reserveTimePerPlayer) }
    var player1MoveTime by remember { mutableIntStateOf(moveTimeDelay) }
    var player2MoveTime by remember { mutableIntStateOf(moveTimeDelay) }
    
    var timerRunning by remember { mutableStateOf(false) }
    
    // === İSTATİSTİK DURUMU ===
    var totalDiceRolls by remember { mutableIntStateOf(0) }
    var playedDiceRolls by remember { mutableIntStateOf(0) }
    var player1TotalMoves by remember { mutableIntStateOf(0) }
    var player2TotalMoves by remember { mutableIntStateOf(0) }
    
    // === TIMER SİSTEMİ ===
    LaunchedEffect(timerRunning, currentPlayer) {
        if (timerRunning) {
            while (timerRunning) {
                delay(1000)
                
                if (currentPlayer == 1) {
                    if (player1MoveTime > 0) {
                        player1MoveTime--
                    } else if (player1ReserveTime > 0) {
                        player1ReserveTime--
                    } else {
                        timerRunning = false // Zaman doldu
                    }
                } else {
                    if (player2MoveTime > 0) {
                        player2MoveTime--
                    } else if (player2ReserveTime > 0) {
                        player2ReserveTime--
                    } else {
                        timerRunning = false // Zaman doldu
                    }
                }
            }
        }
    }
    
    // === ZAR ATMA FONKSİYONU ===
    suspend fun rollDice() {
        if (!isRolling) {
            isRolling = true
            diceMarkedAsPlayed = false
            
            // Ses efekti
            try {
                val toneGenerator = ToneGenerator(AudioManager.STREAM_MUSIC, 70)
                toneGenerator.startTone(ToneGenerator.TONE_PROP_BEEP, 150)
                toneGenerator.release()
            } catch (e: Exception) { }
            
            // Animasyonlu zar atma
            repeat(8) {
                dice1 = (1..6).random()
                dice2 = (1..6).random()
                delay(80)
            }
            
            // Final zarlar
            dice1 = (1..6).random()
            dice2 = (1..6).random()
            
            isRolling = false
            totalDiceRolls++
            
            // Timer'ı başlat
            if (!timerRunning) {
                timerRunning = true
                if (currentPlayer == 1) {
                    player1MoveTime = moveTimeDelay
                } else {
                    player2MoveTime = moveTimeDelay
                }
            }
        }
    }
    
    // === ANA EKRAN: YARI YARIYA BÖLÜNMÜŞ ===
    Row(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF1A1A1A))
    ) {
        // === SOL DİKEY BUTON ===
        Column(
            modifier = Modifier
                .width(80.dp)
                .fillMaxHeight()
                .background(if (currentPlayer == 1) Color(0xFF000000) else Color(0xFF4CAF50)) // Aktif=Siyah, Pasif=Yeşil
                .clickable {
                    if (!isRolling) {
                        CoroutineScope(Dispatchers.Main).launch {
                            rollDice()
                            // Zar atıldıktan sonra timer'ı başlat
                            timerRunning = true
                        }
                    }
                }
                .padding(8.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Sol Oyuncu Bilgileri - Döndürülmüş (Simetrik düzen)
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .rotate(90f),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Hamle süresi - Sol tarafta
                Text(
                    text = "${player1MoveTime}s",
                    color = Color.White,
                    fontSize = 36.sp, // 3 katı (12sp × 3)
                    fontWeight = FontWeight.Bold,
                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                )
                
                Spacer(modifier = Modifier.width(16.dp))
                
                // Rezerv süre - Sağ tarafta  
                Text(
                    text = formatTimeSimple(player1ReserveTime),
                    color = Color.White,
                    fontSize = 54.sp, // 3 katı (18sp × 3)
                    fontWeight = FontWeight.Bold,
                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                )
            }
        }
        
        // === SOL YARIM: SOL ZAR ===
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .background(if (currentPlayer == 1) Color(0xFF000000) else Color(0xFF4CAF50)) // Aktif=Siyah, Pasif=Yeşil
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Sol Zar
            Enhanced3DDice(value = dice1, isRolling = isRolling, size = 150.dp)
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Sol Zar Kontrolleri
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "SOL ZAR",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Değer: $dice1",
                    color = Color(0xFFBBBBBB),
                    fontSize = 14.sp
                )
            }
        }
        
        // === ORTA ÇİZGİ ===
        Box(
            modifier = Modifier
                .width(4.dp)
                .fillMaxHeight()
                .background(Color.White) // Beyaz dikey çizgi
        )
        
        // === SAĞ YARIM: SAĞ ZAR ===
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .background(if (currentPlayer == 2) Color(0xFF000000) else Color(0xFF4CAF50)) // Aktif=Siyah, Pasif=Yeşil
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Sağ Zar
            Enhanced3DDice(value = dice2, isRolling = isRolling, size = 150.dp)
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Sağ Zar Kontrolleri
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "SAĞ ZAR",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Değer: $dice2",
                    color = Color(0xFFBBBBBB),
                    fontSize = 14.sp
                )
            }
        }
        
        // === SAĞ DİKEY BUTON ===
        Column(
            modifier = Modifier
                .width(80.dp)
                .fillMaxHeight()
                .background(if (currentPlayer == 2) Color(0xFF000000) else Color(0xFF4CAF50)) // Aktif=Siyah, Pasif=Yeşil
                .clickable {
                    // SAĞ BUTON: Her zaman çalışabilir - sıra değiştirme
                    currentPlayer = if (currentPlayer == 1) 2 else 1
                    
                    // Timer'ı durdur ve yeniden başlat
                    timerRunning = false
                    
                    // Aktif oyuncunun süresini sıfırla
                    if (currentPlayer == 1) {
                        player1TotalMoves++
                        player1MoveTime = moveTimeDelay
                    } else {
                        player2TotalMoves++
                        player2MoveTime = moveTimeDelay
                    }
                    
                    // Oynandı işaretleme
                    if (totalDiceRolls > 0) {
                        diceMarkedAsPlayed = true
                        playedDiceRolls++
                    }
                    
                    // Timer'ı tekrar başlat
                    timerRunning = true
                }
                .padding(8.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Sağ Oyuncu Bilgileri - Döndürülmüş (Simetrik düzen)
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .rotate(-90f),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Hamle süresi - Sol tarafta
                Text(
                    text = "${player2MoveTime}s",
                    color = Color.White,
                    fontSize = 36.sp, // 3 katı (12sp × 3)
                    fontWeight = FontWeight.Bold,
                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                )
                
                Spacer(modifier = Modifier.width(16.dp))
                
                // Rezerv süre - Sağ tarafta
                Text(
                    text = formatTimeSimple(player2ReserveTime),
                    color = Color.White,
                    fontSize = 54.sp, // 3 katı (18sp × 3)
                    fontWeight = FontWeight.Bold,
                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                )
            }
        }
    }
}

@Composable
fun PlayerInfoCard(
    playerName: String,
    reserveTime: Int,
    moveTime: Int,
    isActive: Boolean,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .border(
                width = if (isActive) 3.dp else 1.dp,
                color = if (isActive) Color(0xFF4CAF50) else Color.Gray,
                shape = RoundedCornerShape(12.dp)
            ),
        colors = CardDefaults.cardColors(
            containerColor = if (isActive) Color(0xFF1B5E20) else Color(0xFF2C2C2C)
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = playerName,
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
            
            Text(
                text = formatTimeSimple(reserveTime),
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
            )
            
            Text(
                text = "Hamle: ${moveTime}s",
                color = Color(0xFFBBBBBB),
                fontSize = 12.sp
            )
        }
    }
}

@Composable
fun Enhanced3DDice(
    value: Int,
    isRolling: Boolean,
    size: androidx.compose.ui.unit.Dp
) {
    val animatedRotation by animateFloatAsState(
        targetValue = if (isRolling) 720f else 0f,
        animationSpec = if (isRolling) {
            infiniteRepeatable(
                animation = tween(600, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Restart
            )
        } else {
            tween(300, easing = FastOutSlowInEasing)
        }, label = ""
    )
    
    Box(
        modifier = Modifier
            .size(size)
            .rotate(animatedRotation)
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .shadow(8.dp, RoundedCornerShape(16.dp))
        ) {
            drawIntoCanvas { canvas ->
                // 3D Gölge efekti
                val shadowPath = Path().apply {
                    addRoundRect(
                        androidx.compose.ui.geometry.RoundRect(
                            left = 4f,
                            top = 4f,
                            right = this@Canvas.size.width + 4f,
                            bottom = this@Canvas.size.height + 4f,
                            radiusX = 32f,
                            radiusY = 32f
                        )
                    )
                }
                
                canvas.drawPath(
                    shadowPath,
                    Paint().apply {
                        color = Color.Black.copy(alpha = 0.3f)
                        isAntiAlias = true
                    }
                )
                
                // Ana zar gövdesi - Gradient
                val gradient = Brush.radialGradient(
                    colors = listOf(
                        Color(0xFFFFFFFF),
                        Color(0xFFF5F5F5),
                        Color(0xFFE0E0E0),
                        Color(0xFFD0D0D0)
                    ),
                    radius = this@Canvas.size.width * 0.8f
                )
                
                drawRoundRect(
                    brush = gradient,
                    size = this@Canvas.size,
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(32f, 32f)
                )
                
                // Border
                drawRoundRect(
                    color = Color(0xFF666666),
                    size = this@Canvas.size,
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(32f, 32f),
                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = 6f)
                )
                
                // Zar noktaları
                drawDiceDots(value, this@Canvas.size, Color(0xFF333333))
            }
        }
    }
}

fun DrawScope.drawDiceDots(value: Int, canvasSize: androidx.compose.ui.geometry.Size, color: Color) {
    val dotRadius = canvasSize.width * 0.08f
    val centerX = canvasSize.width / 2
    val centerY = canvasSize.height / 2
    val offset = canvasSize.width * 0.25f
    
    when (value) {
        1 -> {
            drawCircle(color, dotRadius, Offset(centerX, centerY))
        }
        2 -> {
            drawCircle(color, dotRadius, Offset(centerX - offset, centerY - offset))
            drawCircle(color, dotRadius, Offset(centerX + offset, centerY + offset))
        }
        3 -> {
            drawCircle(color, dotRadius, Offset(centerX - offset, centerY - offset))
            drawCircle(color, dotRadius, Offset(centerX, centerY))
            drawCircle(color, dotRadius, Offset(centerX + offset, centerY + offset))
        }
        4 -> {
            drawCircle(color, dotRadius, Offset(centerX - offset, centerY - offset))
            drawCircle(color, dotRadius, Offset(centerX + offset, centerY - offset))
            drawCircle(color, dotRadius, Offset(centerX - offset, centerY + offset))
            drawCircle(color, dotRadius, Offset(centerX + offset, centerY + offset))
        }
        5 -> {
            drawCircle(color, dotRadius, Offset(centerX - offset, centerY - offset))
            drawCircle(color, dotRadius, Offset(centerX + offset, centerY - offset))
            drawCircle(color, dotRadius, Offset(centerX, centerY))
            drawCircle(color, dotRadius, Offset(centerX - offset, centerY + offset))
            drawCircle(color, dotRadius, Offset(centerX + offset, centerY + offset))
        }
        6 -> {
            drawCircle(color, dotRadius, Offset(centerX - offset, centerY - offset))
            drawCircle(color, dotRadius, Offset(centerX + offset, centerY - offset))
            drawCircle(color, dotRadius, Offset(centerX - offset, centerY))
            drawCircle(color, dotRadius, Offset(centerX + offset, centerY))
            drawCircle(color, dotRadius, Offset(centerX - offset, centerY + offset))
            drawCircle(color, dotRadius, Offset(centerX + offset, centerY + offset))
        }
    }
}

@Composable
fun StatItem(label: String, value: String) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            color = Color.White,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = label,
            color = Color(0xFFBBBBBB),
            fontSize = 12.sp
        )
    }
}

private fun formatTimeSimple(seconds: Int): String {
    val minutes = seconds / 60
    val remainingSeconds = seconds % 60
    return String.format("%02d:%02d", minutes, remainingSeconds)
}