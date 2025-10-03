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

// İstatistik veri sınıfı
data class DiceStats(
    var combinationCount: MutableMap<String, Int> = mutableMapOf(),
    var powerSum: Int = 0,
    var doublesCount: Int = 0,
    var geleCount: Int = 0
)

@Composable
fun SimpleIntegratedScreen(
    gameType: String,
    player1Name: String,
    player2Name: String,
    matchLength: Int,
    onBack: () -> Unit
) {
    // === OYUN DURUMU ===
    var gamePhase by remember { mutableStateOf("opening_single") } // opening_single, playing, finished
    var currentPlayer by remember { mutableIntStateOf(0) } // 0 = hiçbiri, 1 = Player1, 2 = Player2
    var winner by remember { mutableIntStateOf(0) } // Açılış turunu kazanan
    
    // === TEK ZAR DURUMU (Açılış) ===
    var player1OpeningDice by remember { mutableIntStateOf(0) }
    var player2OpeningDice by remember { mutableIntStateOf(0) }
    var isRollingOpening by remember { mutableStateOf(false) }
    
    // === İKİ ZAR DURUMU (Oyun) ===
    var dice1 by remember { mutableIntStateOf(0) }
    var dice2 by remember { mutableIntStateOf(0) }
    var isRollingGame by remember { mutableStateOf(false) }
    
    // === ÇİFT ZAR DURUMU ===
    var isDouble by remember { mutableStateOf(false) }
    var dice1Checked by remember { mutableStateOf(true) }
    var dice2Checked by remember { mutableStateOf(true) }
    var dice3Checked by remember { mutableStateOf(true) }
    var dice4Checked by remember { mutableStateOf(true) }
    
    // === SÜRE DURUMU ===
    val reserveTimePerPlayer = matchLength * 2 * 60 // 2dk × maç uzunluğu
    val moveTimeDelay = 12 // 12 saniye
    
    var player1ReserveTime by remember { mutableIntStateOf(reserveTimePerPlayer) }
    var player2ReserveTime by remember { mutableIntStateOf(reserveTimePerPlayer) }
    var player1MoveTime by remember { mutableIntStateOf(moveTimeDelay) }
    var player2MoveTime by remember { mutableIntStateOf(moveTimeDelay) }
    var timerRunning by remember { mutableStateOf(false) }
    
    // === İSTATİSTİK DURUMU ===
    var player1Stats by remember { mutableStateOf(DiceStats()) }
    var player2Stats by remember { mutableStateOf(DiceStats()) }
    
    // === ANİMASYON DURUMU ===
    var showDragAnimation by remember { mutableStateOf(false) }
    val dragOffset by animateFloatAsState(
        targetValue = if (showDragAnimation) 1f else 0f,
        animationSpec = tween(1000, easing = FastOutSlowInEasing),
        label = "drag"
    )
    
    // === TIMER SİSTEMİ ===
    LaunchedEffect(timerRunning, currentPlayer) {
        if (timerRunning && gamePhase == "playing") {
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
    
    // === AÇILIŞI SIFIRLA ===
    fun resetOpening() {
        player1OpeningDice = 0
        player2OpeningDice = 0
    }
    
    // === OYUNA BAŞLA ===
    fun startPlaying() {
        CoroutineScope(Dispatchers.Main).launch {
            gamePhase = "playing"
            showDragAnimation = true
            delay(1000) // Sürükleme animasyonu bekle
            showDragAnimation = false
            
            // Timer başlat
            if (currentPlayer == 1) {
                player1MoveTime = moveTimeDelay
            } else {
                player2MoveTime = moveTimeDelay
            }
            timerRunning = true
        }
    }
    
    // === AÇILIŞ SONUCU KONTROLÜ ===
    fun checkOpeningResult() {
        CoroutineScope(Dispatchers.Main).launch {
            if (player1OpeningDice > 0 && player2OpeningDice > 0) {
                delay(500)
                
                when (gameType) {
                    "Geleneksel" -> {
                        // Geleneksel tavla kuralları
                        when {
                            player1OpeningDice == 1 -> {
                                // Player1 1 attı, Player2 başlar çift zarla
                                winner = 2
                                currentPlayer = 2
                                startPlaying()
                            }
                            player2OpeningDice == 1 -> {
                                // Player2 1 attı, Player1 başlar çift zarla
                                winner = 1
                                currentPlayer = 1
                                startPlaying()
                            }
                            player1OpeningDice == 6 -> {
                                // Player1 6 attı, Player1 başlar çift zarla
                                winner = 1
                                currentPlayer = 1
                                startPlaying()
                            }
                            player2OpeningDice == 6 -> {
                                // Player2 6 attı, Player2 başlar çift zarla
                                winner = 2
                                currentPlayer = 2
                                startPlaying()
                            }
                            player1OpeningDice == player2OpeningDice -> {
                                // Eşit, yeniden at
                                resetOpening()
                            }
                            player1OpeningDice > player2OpeningDice -> {
                                // Player1 büyük attı
                                winner = 1
                                currentPlayer = 1
                                startPlaying()
                            }
                            else -> {
                                // Player2 büyük attı
                                winner = 2
                                currentPlayer = 2
                                startPlaying()
                            }
                        }
                    }
                    else -> {
                        // Modern tavla kuralları
                        when {
                            player1OpeningDice == player2OpeningDice -> {
                                // Eşit, yeniden at
                                resetOpening()
                            }
                            player1OpeningDice > player2OpeningDice -> {
                                // Player1 büyük attı, açılış zarlarıyla başlar
                                winner = 1
                                currentPlayer = 1
                                dice1 = player1OpeningDice
                                dice2 = player2OpeningDice
                                startPlaying()
                            }
                            else -> {
                                // Player2 büyük attı, açılış zarlarıyla başlar
                                winner = 2
                                currentPlayer = 2
                                dice1 = player1OpeningDice
                                dice2 = player2OpeningDice
                                startPlaying()
                            }
                        }
                    }
                }
            }
        }
    }
    
    // === TEK ZAR ATMA FONKSİYONU (Açılış) ===
    fun rollOpeningDice(player: Int) {
        if (!isRollingOpening) {
            CoroutineScope(Dispatchers.Main).launch {
                isRollingOpening = true
                
                // Ses efekti
                try {
                    val toneGenerator = ToneGenerator(AudioManager.STREAM_MUSIC, 70)
                    toneGenerator.startTone(ToneGenerator.TONE_PROP_BEEP, 150)
                    toneGenerator.release()
                } catch (e: Exception) { }
                
                // Random zar atma
                val diceValue = (1..6).random()
                
                if (player == 1) {
                    player1OpeningDice = diceValue
                } else {
                    player2OpeningDice = diceValue
                }
                
                delay(500)
                isRollingOpening = false
                
                // Açılış kontrolü
                checkOpeningResult()
            }
        }
    }
    
    // === İKİ ZAR ATMA FONKSİYONU ===
    fun rollGameDice() {
        if (!isRollingGame) {
            CoroutineScope(Dispatchers.Main).launch {
                isRollingGame = true
                
                // Ses efekti
                try {
                    val toneGenerator = ToneGenerator(AudioManager.STREAM_MUSIC, 70)
                    toneGenerator.startTone(ToneGenerator.TONE_PROP_BEEP, 150)
                    toneGenerator.release()
                } catch (e: Exception) { }
                
                // İki zar at
                dice1 = (1..6).random()
                dice2 = (1..6).random()
                
                // Çift kontrolü
                isDouble = (dice1 == dice2)
                dice1Checked = true
                dice2Checked = true
                dice3Checked = true
                dice4Checked = true
                
                delay(500)
                isRollingGame = false
            }
        }
    }
    
    // === SIRAYI DEĞİŞTİR ===
    fun switchTurn() {
        CoroutineScope(Dispatchers.Main).launch {
            // Timer durdur
            timerRunning = false
            
            // Sırayı değiştir
            currentPlayer = if (currentPlayer == 1) 2 else 1
            
            // Yeni hamle süresi ayarla
            if (currentPlayer == 1) {
                player1MoveTime = moveTimeDelay
            } else {
                player2MoveTime = moveTimeDelay
            }
            
            // Zar at ve timer başlat
            rollGameDice()
            delay(600)
            timerRunning = true
        }
    }
    
    // === İSTATİSTİK KAYDET ===
    fun saveStats() {
        val stats = if (currentPlayer == 1) player1Stats else player2Stats
        
        if (isDouble) {
            // Çift zar istatistiği
            val combination = "${dice1}-${dice1}"
            stats.combinationCount[combination] = stats.combinationCount.getOrDefault(combination, 0) + 1
            stats.doublesCount++
            
            // Güç hesabı ve gele hesabı
            var playedDice = 0
            var totalPower = 0
            if (dice1Checked) { playedDice++; totalPower += dice1 }
            if (dice2Checked) { playedDice++; totalPower += dice1 }
            if (dice3Checked) { playedDice++; totalPower += dice1 }
            if (dice4Checked) { playedDice++; totalPower += dice1 }
            
            stats.powerSum += totalPower
            stats.geleCount += (dice1 * 4) - totalPower
            
        } else {
            // Normal zar istatistiği
            val combination = if (dice1 <= dice2) "${dice1}-${dice2}" else "${dice2}-${dice1}"
            stats.combinationCount[combination] = stats.combinationCount.getOrDefault(combination, 0) + 1
            
            // Güç hesabı ve gele hesabı
            var totalPower = 0
            if (dice1Checked) totalPower += dice1
            if (dice2Checked) totalPower += dice2
            
            stats.powerSum += totalPower
            stats.geleCount += (dice1 + dice2) - totalPower
        }
        
        // Sırayı değiştir
        switchTurn()
    }
    
    // === ANA EKRAN ===
    Row(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF1A1A1A))
    ) {
        // === SOL İSTATİSTİK BUTONU ===
        Box(
            modifier = Modifier
                .width(80.dp)
                .fillMaxHeight()
                .background(Color(0xFF2E7D32))
                .clickable { if (gamePhase == "playing" && currentPlayer == 1) saveStats() },
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "İSTATİSTİK\nKAYDET",
                color = Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.rotate(90f)
            )
        }
        
        // === SOL YARIM EKRAN ===
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .background(
                    when (gamePhase) {
                        "opening_single" -> Color(0xFFB3E5FC) // Açık mavi
                        "playing" -> if (currentPlayer == 1) Color(0xFF000000) else Color(0xFF808080)
                        else -> Color(0xFF808080)
                    }
                )
                .clickable {
                    when (gamePhase) {
                        "opening_single" -> rollOpeningDice(1)
                        "playing" -> if (currentPlayer == 1) switchTurn()
                    }
                }
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Üst: Hamle süresi
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(top = 16.dp)
            ) {
                Text(
                    text = "HAMLE",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${player1MoveTime}s",
                    color = Color(0xFFFFEB3B),
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                )
            }
            
            // Orta: Zarlar
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.weight(1f)
            ) {
                when (gamePhase) {
                    "opening_single" -> {
                        // Tek zar
                        Enhanced3DDice(value = player1OpeningDice, isRolling = isRollingOpening, size = 120.dp)
                    }
                    "playing" -> {
                        if (currentPlayer == 1) {
                            if (isDouble) {
                                // 4 zar (çift)
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Column {
                                        DiceWithCheckbox(dice1, dice1Checked) { dice1Checked = !dice1Checked }
                                        Spacer(modifier = Modifier.height(8.dp))
                                        DiceWithCheckbox(dice1, dice3Checked) { dice3Checked = !dice3Checked }
                                    }
                                    Column {
                                        DiceWithCheckbox(dice1, dice2Checked) { dice2Checked = !dice2Checked }
                                        Spacer(modifier = Modifier.height(8.dp))
                                        DiceWithCheckbox(dice1, dice4Checked) { dice4Checked = !dice4Checked }
                                    }
                                }
                            } else {
                                // 2 zar (normal)
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                                ) {
                                    DiceWithCheckbox(dice1, dice1Checked) { dice1Checked = !dice1Checked }
                                    DiceWithCheckbox(dice2, dice2Checked) { dice2Checked = !dice2Checked }
                                }
                            }
                        } else {
                            // Soru işareti
                            Enhanced3DDice(value = 0, isRolling = false, size = 120.dp)
                        }
                    }
                }
            }
            
            // Alt: Rezerv süre
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(bottom = 16.dp)
            ) {
                Text(
                    text = "REZERV",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = formatTimeSimple(player1ReserveTime),
                    color = Color.White,
                    fontSize = 40.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                )
            }
        }
        
        // === ORTA ÇİZGİ ===
        Box(
            modifier = Modifier
                .width(4.dp)
                .fillMaxHeight()
                .background(Color.White)
        )
        
        // === SAĞ YARIM EKRAN ===
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .background(
                    when (gamePhase) {
                        "opening_single" -> Color(0xFFFFCDD2) // Açık kırmızı
                        "playing" -> if (currentPlayer == 2) Color(0xFF000000) else Color(0xFF808080)
                        else -> Color(0xFF808080)
                    }
                )
                .clickable {
                    when (gamePhase) {
                        "opening_single" -> rollOpeningDice(2)
                        "playing" -> if (currentPlayer == 2) switchTurn()
                    }
                }
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Üst: Hamle süresi
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(top = 16.dp)
            ) {
                Text(
                    text = "HAMLE",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${player2MoveTime}s",
                    color = Color(0xFFFFEB3B),
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                )
            }
            
            // Orta: Zarlar
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.weight(1f)
            ) {
                when (gamePhase) {
                    "opening_single" -> {
                        // Tek zar
                        Enhanced3DDice(value = player2OpeningDice, isRolling = isRollingOpening, size = 120.dp)
                    }
                    "playing" -> {
                        if (currentPlayer == 2) {
                            if (isDouble) {
                                // 4 zar (çift)
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Column {
                                        DiceWithCheckbox(dice1, dice1Checked) { dice1Checked = !dice1Checked }
                                        Spacer(modifier = Modifier.height(8.dp))
                                        DiceWithCheckbox(dice1, dice3Checked) { dice3Checked = !dice3Checked }
                                    }
                                    Column {
                                        DiceWithCheckbox(dice1, dice2Checked) { dice2Checked = !dice2Checked }
                                        Spacer(modifier = Modifier.height(8.dp))
                                        DiceWithCheckbox(dice1, dice4Checked) { dice4Checked = !dice4Checked }
                                    }
                                }
                            } else {
                                // 2 zar (normal)
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                                ) {
                                    DiceWithCheckbox(dice1, dice1Checked) { dice1Checked = !dice1Checked }
                                    DiceWithCheckbox(dice2, dice2Checked) { dice2Checked = !dice2Checked }
                                }
                            }
                        } else {
                            // Soru işareti
                            Enhanced3DDice(value = 0, isRolling = false, size = 120.dp)
                        }
                    }
                }
            }
            
            // Alt: Rezerv süre
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(bottom = 16.dp)
            ) {
                Text(
                    text = "REZERV",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = formatTimeSimple(player2ReserveTime),
                    color = Color.White,
                    fontSize = 40.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                )
            }
        }
        
        // === SAĞ İSTATİSTİK BUTONU ===
        Box(
            modifier = Modifier
                .width(80.dp)
                .fillMaxHeight()
                .background(Color(0xFF2E7D32))
                .clickable { if (gamePhase == "playing" && currentPlayer == 2) saveStats() },
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "İSTATİSTİK\nKAYDET",
                color = Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.rotate(-90f)
            )
        }
    }
}

@Composable
fun DiceWithCheckbox(
    value: Int,
    checked: Boolean,
    onCheckedChange: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Enhanced3DDice(value = value, isRolling = false, size = 60.dp)
        Spacer(modifier = Modifier.height(4.dp))
        Checkbox(
            checked = checked,
            onCheckedChange = { onCheckedChange() },
            modifier = Modifier.size(20.dp)
        )
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

                // Zar noktaları veya soru işareti
                if (value > 0) {
                    drawDiceDots(value, this@Canvas.size, Color(0xFF333333))
                } else {
                    // Soru işareti çiz
                    drawContext.canvas.nativeCanvas.apply {
                        val paint = android.graphics.Paint().apply {
                            color = android.graphics.Color.BLACK
                            textSize = this@Canvas.size.width * 0.5f
                            isAntiAlias = true
                            textAlign = android.graphics.Paint.Align.CENTER
                            typeface = android.graphics.Typeface.DEFAULT_BOLD
                        }
                        drawText(
                            "?",
                            this@Canvas.size.width / 2,
                            this@Canvas.size.height / 2 + paint.textSize / 3,
                            paint
                        )
                    }
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