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
    var gamePhase by remember { mutableStateOf("initial") } // "initial", "playing"

    // === ZAR DURUMU ===
    var dice1 by remember { mutableIntStateOf(1) }
    var dice2 by remember { mutableIntStateOf(2) }
    var leftDice by remember { mutableIntStateOf(0) } // Sol taraf zarı
    var rightDice by remember { mutableIntStateOf(0) } // Sağ taraf zarı
    var isRolling by remember { mutableStateOf(false) }
    var isRollingLeft by remember { mutableStateOf(false) }
    var isRollingRight by remember { mutableStateOf(false) }

    // === SÜRE DURUMU ===
    val reserveTimePerPlayer = matchLength * 2 * 60 // 2dk × maç uzunluğu
    val moveTimeDelay = 12 // 12 saniye

    var player1ReserveTime by remember { mutableIntStateOf(reserveTimePerPlayer) }
    var player2ReserveTime by remember { mutableIntStateOf(reserveTimePerPlayer) }
    var player1MoveTime by remember { mutableIntStateOf(moveTimeDelay) }
    var player2MoveTime by remember { mutableIntStateOf(moveTimeDelay) }

    var timerRunning by remember { mutableStateOf(false) }

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

    // === ZAR RANDOM ATMA ALGORİTMASI (1-6 diziden elenme) ===
    suspend fun rollDiceWithElimination(): Int {
        val numbers = mutableListOf(1, 2, 3, 4, 5, 6)
        var result = 1

        // 5 sayı elenecek
        repeat(5) {
            val randomIndex = numbers.indices.random()
            result = numbers[randomIndex]
            numbers.removeAt(randomIndex)
            delay(80) // Her eleme görsel olarak gösterilecek
        }

        // Son kalan sayı
        return numbers[0]
    }

    // === SOL TARAF ZAR ATMA (Oyun başlangıcı) ===
    fun rollLeftDice() {
        if (!isRollingLeft) {
            CoroutineScope(Dispatchers.Main).launch {
                isRollingLeft = true

                // Ses efekti
                try {
                    val toneGenerator = ToneGenerator(AudioManager.STREAM_MUSIC, 70)
                    toneGenerator.startTone(ToneGenerator.TONE_PROP_BEEP, 150)
                    toneGenerator.release()
                } catch (e: Exception) { }

                // Random eliminasyon algoritması
                val numbers = mutableListOf(1, 2, 3, 4, 5, 6)
                repeat(5) {
                    val randomIndex = numbers.indices.random()
                    leftDice = numbers[randomIndex]
                    numbers.removeAt(randomIndex)
                    delay(80)
                }
                leftDice = numbers[0] // Son kalan sayı

                isRollingLeft = false
            }
        }
    }

    // === SAĞ TARAF ZAR ATMA (Oyun başlangıcı) ===
    fun rollRightDice() {
        if (!isRollingRight) {
            CoroutineScope(Dispatchers.Main).launch {
                isRollingRight = true

                // Ses efekti
                try {
                    val toneGenerator = ToneGenerator(AudioManager.STREAM_MUSIC, 70)
                    toneGenerator.startTone(ToneGenerator.TONE_PROP_BEEP, 150)
                    toneGenerator.release()
                } catch (e: Exception) { }

                // Random eliminasyon algoritması
                val numbers = mutableListOf(1, 2, 3, 4, 5, 6)
                repeat(5) {
                    val randomIndex = numbers.indices.random()
                    rightDice = numbers[randomIndex]
                    numbers.removeAt(randomIndex)
                    delay(80)
                }
                rightDice = numbers[0] // Son kalan sayı

                isRollingRight = false

                // Karşılaştır ve oyunu başlat
                if (leftDice > 0 && rightDice > 0) {
                    delay(500)
                    if (leftDice > rightDice) {
                        // Sol kazandı
                        currentPlayer = 1
                        dice1 = leftDice
                        dice2 = rightDice
                        gamePhase = "playing"
                        timerRunning = true
                    } else if (rightDice > leftDice) {
                        // Sağ kazandı
                        currentPlayer = 2
                        dice1 = leftDice
                        dice2 = rightDice
                        gamePhase = "playing"
                        timerRunning = true
                    } else {
                        // Berabere - yeniden at
                        leftDice = 0
                        rightDice = 0
                        delay(1000)
                        rollLeftDice()
                    }
                }
            }
        }
    }

    // === NORMAL ZAR ATMA (Oyun sırasında) ===
    suspend fun rollDice() {
        if (!isRolling) {
            isRolling = true

            // Ses efekti
            try {
                val toneGenerator = ToneGenerator(AudioManager.STREAM_MUSIC, 70)
                toneGenerator.startTone(ToneGenerator.TONE_PROP_BEEP, 150)
                toneGenerator.release()
            } catch (e: Exception) { }

            // İki zar için random eliminasyon
            val numbers1 = mutableListOf(1, 2, 3, 4, 5, 6)
            val numbers2 = mutableListOf(1, 2, 3, 4, 5, 6)

            repeat(5) {
                val idx1 = numbers1.indices.random()
                val idx2 = numbers2.indices.random()
                dice1 = numbers1[idx1]
                dice2 = numbers2[idx2]
                numbers1.removeAt(idx1)
                numbers2.removeAt(idx2)
                delay(80)
            }

            dice1 = numbers1[0]
            dice2 = numbers2[0]

            isRolling = false

            // Timer'ı başlat
            if (!timerRunning) {
                timerRunning = true
            }
        }
    }

    // === ANA EKRAN ===
    Row(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF1A1A1A))
    ) {
        // === SOL DİKEY BUTON ===
        Box(
            modifier = Modifier
                .width(120.dp)
                .fillMaxHeight()
                .background(
                    if (gamePhase == "initial") Color(0xFFB3E5FC) // Açık mavi başlangıç
                    else if (currentPlayer == 1) Color(0xFF000000) else Color(0xFF808080) // Aktif=Siyah, Pasif=Gri
                )
                .clickable {
                    if (gamePhase == "initial") {
                        // Başlangıç zar atma
                        rollLeftDice()
                    } else {
                        // Normal zar atma
                        if (!isRolling && currentPlayer == 1) {
                            CoroutineScope(Dispatchers.Main).launch {
                                rollDice()
                                timerRunning = true
                            }
                        }
                    }
                }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp),
                verticalArrangement = Arrangement.SpaceBetween,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
            // Üst: Hamle süresi
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .rotate(90f)
                    .padding(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "HAMLE",
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${player1MoveTime}s",
                    color = Color(0xFFFFEB3B),
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Alt: Rezerv süre
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .rotate(90f)
                    .padding(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "REZERV",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = formatTimeSimple(player1ReserveTime),
                    color = Color.White,
                    fontSize = 36.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                )
            }
            }
        }

        // === SOL YARIM: SOL ZAR ===
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .background(
                    if (gamePhase == "initial") Color(0xFFB3E5FC) // Açık mavi başlangıç
                    else if (currentPlayer == 1) Color(0xFF000000) else Color(0xFF808080)
                )
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            if (gamePhase == "initial") {
                // Başlangıç zarı
                Enhanced3DDice(value = leftDice, isRolling = isRollingLeft, size = 120.dp)
            } else {
                // Normal zar
                Enhanced3DDice(value = dice1, isRolling = isRolling, size = 120.dp)
            }
        }

        // === ORTA ÇİZGİ ===
        Box(
            modifier = Modifier
                .width(4.dp)
                .fillMaxHeight()
                .background(Color.White)
        )

        // === SAĞ YARIM: SAĞ ZAR ===
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .background(
                    if (gamePhase == "initial") Color(0xFFFFCDD2) // Açık kırmızı başlangıç
                    else if (currentPlayer == 2) Color(0xFF000000) else Color(0xFF808080)
                )
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            if (gamePhase == "initial") {
                // Başlangıç zarı
                Enhanced3DDice(value = rightDice, isRolling = isRollingRight, size = 120.dp)
            } else {
                // Normal zar
                Enhanced3DDice(value = dice2, isRolling = isRolling, size = 120.dp)
            }
        }

        // === SAĞ DİKEY BUTON ===
        Box(
            modifier = Modifier
                .width(120.dp)
                .fillMaxHeight()
                .background(
                    if (gamePhase == "initial") Color(0xFFFFCDD2) // Açık kırmızı başlangıç
                    else if (currentPlayer == 2) Color(0xFF000000) else Color(0xFF808080)
                )
                .clickable {
                    if (gamePhase == "initial") {
                        // Başlangıç zar atma
                        rollRightDice()
                    } else {
                        // Sıra değiştirme
                        currentPlayer = if (currentPlayer == 1) 2 else 1
                        timerRunning = false

                        if (currentPlayer == 1) {
                            player1MoveTime = moveTimeDelay
                        } else {
                            player2MoveTime = moveTimeDelay
                        }

                        timerRunning = true
                    }
                }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp),
                verticalArrangement = Arrangement.SpaceBetween,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
            // Üst: Hamle süresi
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .rotate(-90f)
                    .padding(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "HAMLE",
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${player2MoveTime}s",
                    color = Color(0xFFFFEB3B),
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Alt: Rezerv süre
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .rotate(-90f)
                    .padding(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "REZERV",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = formatTimeSimple(player2ReserveTime),
                    color = Color.White,
                    fontSize = 36.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                )
            }
            }
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
                if (value > 0) {
                    drawDiceDots(value, this@Canvas.size, Color(0xFF333333))
                }
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

private fun formatTimeSimple(seconds: Int): String {
    val minutes = seconds / 60
    val remainingSeconds = seconds % 60
    return String.format("%02d:%02d", minutes, remainingSeconds)
}
