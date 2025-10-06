package com.tavla.tavlapp

import android.content.Context
import android.media.AudioManager
import android.media.ToneGenerator
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import kotlinx.coroutines.delay
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

// Gelişmiş İstatistik veri sınıfları
enum class CheckboxState { CHECKED, UNCHECKED, SQUARE }

data class DiceRoll(
    val originalValue: Int,
    val playedValue: Int,
    val checkboxState: CheckboxState
)

data class AdvancedDiceStats(
    // Atılan zar istatistikleri
    var thrownDice: MutableList<String> = mutableListOf(), // "6-5", "4-4" vb.
    var thrownPower: Int = 0,
    var thrownParts: Int = 0,
    var thrownDoubles: Int = 0,
    
    // Oynanan zar istatistikleri  
    var playedDice: MutableList<String> = mutableListOf(), // "6-1-1-X", "0-5" vb.
    var playedPower: Int = 0,
    var playedParts: Int = 0,
    
    // Kısmi oynanan zar istatistikleri
    var partialPower: Int = 0, // Boşa giden kuvvet
    var partialParts: Int = 0, // Boşa giden pare sayısı
    
    // Gele istatistikleri
    var gelePower: Int = 0,
    var geleParts: Int = 0,
    
    // Bitiş artığı istatistikleri
    var endRemainPower: Int = 0,
    var endRemainParts: Int = 0,
    
    // Çift zar istatistikleri
    var doublesFullPlayed: Int = 0, // Tam oynanan çiftler
    var doublesPartialPlayed: Int = 0 // Kısmi oynanan çiftler
)

@Composable
fun SimpleIntegratedScreen(
    gameType: String,
    player1Name: String,
    player2Name: String,
    matchLength: Int,
    keepStatistics: Boolean = false,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    
    var gamePhase by remember { mutableStateOf("opening_single") }
    var currentPlayer by remember { mutableIntStateOf(0) }
    var winner by remember { mutableIntStateOf(0) }
    
    var player1OpeningDice by remember { mutableIntStateOf(0) }
    var player2OpeningDice by remember { mutableIntStateOf(0) }
    var isRollingOpening1 by remember { mutableStateOf(false) }
    var isRollingOpening2 by remember { mutableStateOf(false) }
    
    var dice1 by remember { mutableIntStateOf(0) }
    var dice2 by remember { mutableIntStateOf(0) }
    var isRollingGame by remember { mutableStateOf(false) }
    var isDouble by remember { mutableStateOf(false) }
    
    var dice1State by remember { mutableStateOf(CheckboxState.CHECKED) }
    var dice2State by remember { mutableStateOf(CheckboxState.CHECKED) }
    var dice3State by remember { mutableStateOf(CheckboxState.CHECKED) }
    var dice4State by remember { mutableStateOf(CheckboxState.CHECKED) }
    
    val reserveTimePerPlayer = matchLength * 2 * 60
    val moveTimeDelay = 12
    
    var player1ReserveTime by remember { mutableIntStateOf(reserveTimePerPlayer) }
    var player2ReserveTime by remember { mutableIntStateOf(reserveTimePerPlayer) }
    var player1MoveTime by remember { mutableIntStateOf(moveTimeDelay) }
    var player2MoveTime by remember { mutableIntStateOf(moveTimeDelay) }
    var timerRunning by remember { mutableStateOf(false) }
    
    var player1Stats by remember { mutableStateOf(AdvancedDiceStats()) }
    var player2Stats by remember { mutableStateOf(AdvancedDiceStats()) }
    var showStatsDialog by remember { mutableStateOf(false) }
    
    var undoStack by remember { mutableStateOf(listOf<String>()) }
    var showUndoNotification by remember { mutableStateOf(false) }
    var undoMessage by remember { mutableStateOf("") }
    
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
    
    // === ELEME SİSTEMİ İLE ZAR ATMA ===
    suspend fun rollDiceWithElimination(updateDiceValue: (Int) -> Unit): Int {
        val numbers = mutableListOf(1, 2, 3, 4, 5, 6)

        // İlk random sayıyı belirle ve göster
        val firstRandom = (1..6).random()
        updateDiceValue(firstRandom)
        delay(80) // İlk değeri göster
        
        // İlk seçilen sayıyı listeden çıkar
        numbers.remove(firstRandom)

        // Kalan 5 sayıdan 4'ünü teker teker eleme
        repeat(4) {
            val randomIndex = numbers.indices.random()
            val eliminatedValue = numbers[randomIndex]
            numbers.removeAt(randomIndex)

            // Elenen sayıyı zarda göster
            updateDiceValue(eliminatedValue)
            delay(80) // Her eleme 80ms göster
        }

        // Son kalan sayıyı göster ve döndür (final result)
        val finalValue = numbers.first()
        updateDiceValue(finalValue)
        delay(120) // Final değeri biraz uzun göster

        return finalValue
    }

    // === TEK ZAR ATMA FONKSİYONU (Açılış) - ELEME SİSTEMİ ===
    fun rollOpeningDice(player: Int) {
        val isCurrentlyRolling = if (player == 1) isRollingOpening1 else isRollingOpening2
        
        if (!isCurrentlyRolling) {
            CoroutineScope(Dispatchers.Main).launch {
                // Sadece atılan oyuncunun zarını rolling yap
                if (player == 1) {
                    isRollingOpening1 = true
                } else {
                    isRollingOpening2 = true
                }

                // Ses efekti
                try {
                    val toneGenerator = ToneGenerator(AudioManager.STREAM_MUSIC, 70)
                    toneGenerator.startTone(ToneGenerator.TONE_PROP_BEEP, 150)
                    toneGenerator.release()
                } catch (e: Exception) { }

                // Eleme sistemi ile zar atma (her elenen sayı görünecek)
                val diceValue = rollDiceWithElimination { value ->
                    if (player == 1) {
                        player1OpeningDice = value
                    } else {
                        player2OpeningDice = value
                    }
                }

                delay(300)
                
                // Sadece atılan oyuncunun zarının rolling'ini bitir
                if (player == 1) {
                    isRollingOpening1 = false
                } else {
                    isRollingOpening2 = false
                }

                // Açılış kontrolü
                checkOpeningResult()
            }
        }
    }
    
    // === İKİ ZAR ATMA FONKSİYONU - ELEME SİSTEMİ ===
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

                // İki zar paralel olarak eleme sistemi ile at
                val job1 = launch {
                    dice1 = rollDiceWithElimination { value -> dice1 = value }
                }
                val job2 = launch {
                    dice2 = rollDiceWithElimination { value -> dice2 = value }
                }

                // Her iki zarın da tamamlanmasını bekle
                job1.join()
                job2.join()

                // Çift kontrolü
                isDouble = (dice1 == dice2)
                dice1State = CheckboxState.CHECKED
                dice2State = CheckboxState.CHECKED
                dice3State = CheckboxState.CHECKED
                dice4State = CheckboxState.CHECKED

                delay(300)
                isRollingGame = false
            }
        }
    }
    
    // === SIRAYI DEĞİŞTİR ===
    fun switchTurn() {
        CoroutineScope(Dispatchers.Main).launch {
            // Timer durdur
            timerRunning = false
            
            // Zar durumlarını sıfırla
            isDouble = false
            dice1 = 0
            dice2 = 0
            dice1State = CheckboxState.CHECKED
            dice2State = CheckboxState.CHECKED
            dice3State = CheckboxState.CHECKED
            dice4State = CheckboxState.CHECKED
            
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
    
    // === İSTATİSTİK KALICI SAKLAMA ===
    fun saveStatsToStorage() {
        try {
            val sharedPrefs = context.getSharedPreferences("tavla_stats", Context.MODE_PRIVATE)
            val editor = sharedPrefs.edit()
            
            // Player1 istatistiklerini kaydet
            editor.putInt("${player1Name}_power", player1Stats.playedPower)
            editor.putInt("${player1Name}_gele", player1Stats.gelePower)
            editor.putInt("${player1Name}_doubles", player1Stats.thrownDoubles)

            // Player2 istatistiklerini kaydet
            editor.putInt("${player2Name}_power", player2Stats.playedPower)
            editor.putInt("${player2Name}_gele", player2Stats.gelePower)
            editor.putInt("${player2Name}_doubles", player2Stats.thrownDoubles)
            
            editor.apply()
            
        } catch (e: Exception) {
            // Hata durumunda sessizce devam et
        }
    }
    
    // === MAÇI BİTİR VE İSTATİSTİKLERİ GÖSTER ===
    fun finishGameWithStats() {
        saveStatsToStorage()
        showStatsDialog = true
    }
    
    // === GERİ AL FONKSİYONU ===
    fun performUndo() {
        if (undoStack.isNotEmpty()) {
            val lastAction = undoStack.last()
            undoStack = undoStack.dropLast(1)
            
            undoMessage = "Geri alındı: $lastAction"
            showUndoNotification = true
            
            // 2 saniye sonra bildirimi gizle
            CoroutineScope(Dispatchers.Main).launch {
                delay(2000)
                showUndoNotification = false
            }
        }
    }
    
    // === GERİ AL AKSİYONU KAYDET ===
    fun addUndoAction(action: String) {
        undoStack = undoStack + action
        // Maksimum 5 aksiyon tut
        if (undoStack.size > 5) {
            undoStack = undoStack.drop(1)
        }
    }
    
    // === İSTATİSTİK KAYDET ===
    fun saveStats() {
        val stats = if (currentPlayer == 1) player1Stats else player2Stats

        if (isDouble) {
            // Çift zar istatistiği
            val combination = "${dice1}-${dice1}"
            stats.thrownDice.add(combination)
            stats.thrownDoubles++

            // Güç hesabı ve gele hesabı
            var playedDiceCount = 0
            var totalPower = 0
            if (dice1State == CheckboxState.CHECKED) { playedDiceCount++; totalPower += dice1 }
            if (dice2State == CheckboxState.CHECKED) { playedDiceCount++; totalPower += dice1 }
            if (dice3State == CheckboxState.CHECKED) { playedDiceCount++; totalPower += dice1 }
            if (dice4State == CheckboxState.CHECKED) { playedDiceCount++; totalPower += dice1 }

            stats.playedPower += totalPower
            stats.gelePower += (dice1 * 4) - totalPower

        } else {
            // Normal zar istatistiği
            val combination = if (dice1 <= dice2) "${dice1}-${dice2}" else "${dice2}-${dice1}"
            stats.thrownDice.add(combination)

            // Güç hesabı ve gele hesabı
            var totalPower = 0
            if (dice1State == CheckboxState.CHECKED) totalPower += dice1
            if (dice2State == CheckboxState.CHECKED) totalPower += dice2

            stats.playedPower += totalPower
            stats.gelePower += (dice1 + dice2) - totalPower
        }

        // Geri al aksiyonunu kaydet
        val playerName = if (currentPlayer == 1) player1Name else player2Name
        val diceInfo = if (isDouble) "${dice1}-${dice1} (çift)" else "${dice1}-${dice2}"
        addUndoAction("$playerName: $diceInfo zar kaydı")

        // Sırayı değiştir
        switchTurn()
    }
    
    // === ANA EKRAN ===
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF1A1A1A))
    ) {
        // === ZAR VE SÜRE ALANI ===
        Row(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
        // === SOL İSTATİSTİK BUTONU ===
        Box(
            modifier = Modifier
                .width(50.dp)
                .fillMaxHeight()
                .background(Color(0xFF2E7D32))
                .clickable {
                    when (gamePhase) {
                        "opening_single" -> rollOpeningDice(1)
                        "playing" -> {
                            if (keepStatistics) {
                                // İstatistik açıksa sadece currentPlayer istatistik kaydedebilir
                                if (currentPlayer == 1) saveStats()
                            } else {
                                // İstatistik kapalıysa kendi yarısına tıklayarak zar atabilir
                                if (currentPlayer == 1) rollGameDice()
                            }
                        }
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "ZAR AT/SAAT ÇALIŞTIR/İSTATİSTİK KAYDET",
                color = Color.White,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                maxLines = 1,
                modifier = Modifier.rotate(90f)
            )
        }
        
        
        // === SOL ZAR ALANI ===
        Row(
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
                .padding(16.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Sol oyuncu - süre göstergeleri zarların solunda
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.padding(end = 16.dp)
            ) {
                // Hamle süresi
                Text(
                    text = "${player1MoveTime}s",
                    color = Color(0xFFFFEB3B),
                    fontSize = 48.sp, // 2 katı
                    fontWeight = FontWeight.Bold,
                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Rezerv süre
                Text(
                    text = formatTimeSimple(player1ReserveTime),
                    color = Color.White,
                    fontSize = 32.sp, // Küçültüldü (50sp → 32sp)
                    fontWeight = FontWeight.Bold,
                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                )
            }
            
            // Zar alanı
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                when (gamePhase) {
                    "opening_single" -> {
                        // Tek zar
                        Enhanced3DDice(value = player1OpeningDice, isRolling = isRollingOpening1, size = 120.dp)
                    }
                    "playing" -> {
                        if (currentPlayer == 1) {
                            if (isDouble) {
                                // 4 zar (çift) - dikey alt alta
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    DiceWithCheckboxNoRoll(dice1, dice1, dice1State, { dice1State = it }, {}, "left", 60.dp)
                                    DiceWithCheckboxNoRoll(dice1, dice1, dice2State, { dice2State = it }, {}, "left", 60.dp)
                                    DiceWithCheckboxNoRoll(dice1, dice1, dice3State, { dice3State = it }, {}, "left", 60.dp)
                                    DiceWithCheckboxNoRoll(dice1, dice1, dice4State, { dice4State = it }, {}, "left", 60.dp)
                                }
                            } else {
                                // 2 zar (normal) - dikey alt alta, 2 katı büyük
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(16.dp)
                                ) {
                                    DiceWithCheckbox(dice1, dice1, dice1State, { dice1State = it }, {}, "left", 120.dp)
                                    DiceWithCheckbox(dice2, dice2, dice2State, { dice2State = it }, {}, "left", 120.dp)
                                }
                            }
                        } else {
                            // Soru işareti
                            Enhanced3DDice(value = 0, isRolling = false, size = 120.dp)
                        }
                    }
                }
            }
        }
        
        // === ORTA ÇİZGİ ===
        Box(
            modifier = Modifier
                .width(4.dp)
                .fillMaxHeight()
                .background(Color.White)
        )
        
        // === SAĞ ZAR ALANI ===
        Row(
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
                .padding(16.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Zar alanı
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                when (gamePhase) {
                    "opening_single" -> {
                        // Tek zar
                        Enhanced3DDice(value = player2OpeningDice, isRolling = isRollingOpening2, size = 120.dp)
                    }
                    "playing" -> {
                        if (currentPlayer == 2) {
                            if (isDouble) {
                                // 4 zar (çift) - dikey alt alta
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    DiceWithCheckboxNoRoll(dice1, dice1, dice1State, { dice1State = it }, {}, "right", 60.dp)
                                    DiceWithCheckboxNoRoll(dice1, dice1, dice2State, { dice2State = it }, {}, "right", 60.dp)
                                    DiceWithCheckboxNoRoll(dice1, dice1, dice3State, { dice3State = it }, {}, "right", 60.dp)
                                    DiceWithCheckboxNoRoll(dice1, dice1, dice4State, { dice4State = it }, {}, "right", 60.dp)
                                }
                            } else {
                                // 2 zar (normal) - dikey alt alta, 2 katı büyük
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(16.dp)
                                ) {
                                    DiceWithCheckbox(dice1, dice1, dice1State, { dice1State = it }, {}, "right", 120.dp)
                                    DiceWithCheckbox(dice2, dice2, dice2State, { dice2State = it }, {}, "right", 120.dp)
                                }
                            }
                        } else {
                            // Soru işareti
                            Enhanced3DDice(value = 0, isRolling = false, size = 120.dp)
                        }
                    }
                }
            }
            
            // Sağ oyuncu - süre göstergeleri zarların sağında
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.padding(start = 16.dp)
            ) {
                // Hamle süresi
                Text(
                    text = "${player2MoveTime}s",
                    color = Color(0xFFFFEB3B),
                    fontSize = 48.sp, // 2 katı
                    fontWeight = FontWeight.Bold,
                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                )
                
                Spacer(modifier = Modifier.height(8.dp))

                Spacer(modifier = Modifier.height(20.dp))

                // Rezerv süre
                Text(
                    text = formatTimeSimple(player2ReserveTime),
                    color = Color.White,
                    fontSize = 32.sp, // Küçültüldü (50sp → 32sp)
                    fontWeight = FontWeight.Bold,
                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                )
            }
        }
        
        
        // === SAĞ İSTATİSTİK BUTONU ===
        Box(
            modifier = Modifier
                .width(50.dp)
                .fillMaxHeight()
                .background(Color(0xFF2E7D32))
                .clickable {
                    when (gamePhase) {
                        "opening_single" -> rollOpeningDice(2)
                        "playing" -> {
                            if (keepStatistics) {
                                // İstatistik açıksa sadece currentPlayer istatistik kaydedebilir
                                if (currentPlayer == 2) saveStats()
                            } else {
                                // İstatistik kapalıysa kendi yarısına tıklayarak zar atabilir
                                if (currentPlayer == 2) rollGameDice()
                            }
                        }
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "ZAR AT/SAAT ÇALIŞTIR/İSTATİSTİK KAYDET",
                color = Color.White,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                maxLines = 1,
                modifier = Modifier.rotate(-90f)
            )
        }
        }
        
        // === ALT BUTON ALANI ===
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF1A1A1A))
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Geri Al Butonu (yarı genişlik)
            Button(
                onClick = { performUndo() },
                modifier = Modifier.weight(0.5f),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2196F3)),
                enabled = undoStack.isNotEmpty()
            ) {
                Text(
                    text = "↶ GERİ AL",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            // Maçı Bitir Butonu (2 katı genişlik)
            Button(
                onClick = { finishGameWithStats() },
                modifier = Modifier.weight(4f),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF5722))
            ) {
                Text(
                    text = "📊 MAÇI BİTİR VE İSTATİSTİKLERİ GÖSTER",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
    
    // === İSTATİSTİK DİALOGU ===
    if (showStatsDialog) {
        Dialog(onDismissRequest = { showStatsDialog = false }) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp)
                        .verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "🎲 ZAR İSTATİSTİKLERİ",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(bottom = 20.dp)
                    )
                    
                    // Player1 İstatistikleri
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFE3F2FD))
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "👤 $player1Name",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1976D2)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            Text("📊 Toplam Güç: ${player1Stats.playedPower}")
                            Text("❌ Gele Sayısı: ${player1Stats.gelePower}")
                            Text("🎯 Çift Sayısı: ${player1Stats.thrownDoubles}")

                            if (player1Stats.thrownDice.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text("🎲 Atılan Zarlar:", fontWeight = FontWeight.Bold)
                                player1Stats.thrownDice.forEach { dice ->
                                    Text("  $dice", fontSize = 14.sp)
                                }
                            }
                        }
                    }
                    
                    // Player2 İstatistikleri
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFEBEE))
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "👤 $player2Name",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFD32F2F)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            Text("📊 Toplam Güç: ${player2Stats.playedPower}")
                            Text("❌ Gele Sayısı: ${player2Stats.gelePower}")
                            Text("🎯 Çift Sayısı: ${player2Stats.thrownDoubles}")

                            if (player2Stats.thrownDice.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text("🎲 Atılan Zarlar:", fontWeight = FontWeight.Bold)
                                player2Stats.thrownDice.forEach { dice ->
                                    Text("  $dice", fontSize = 14.sp)
                                }
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(20.dp))
                    
                    // Butonlar
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Button(
                            onClick = { showStatsDialog = false },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = Color.Gray)
                        ) {
                            Text("KAPAT")
                        }
                        
                        Button(
                            onClick = { 
                                showStatsDialog = false
                                onBack()
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))
                        ) {
                            Text("ANA MENÜ")
                        }
                    }
                }
            }
        }
    }
    
    // === GERİ AL BİLDİRİM PENCERESİ ===
    if (showUndoNotification) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.TopCenter
        ) {
            Card(
                modifier = Modifier
                    .padding(top = 50.dp)
                    .wrapContentSize(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF4CAF50)),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = undoMessage,
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(16.dp)
                )
            }
        }
    }
}

@Composable
fun DiceWithCheckboxNoRoll(
    originalValue: Int,
    playedValue: Int,
    checkboxState: CheckboxState,
    onCheckboxStateChange: (CheckboxState) -> Unit,
    onValueChange: (Int) -> Unit,
    checkboxPosition: String = "bottom", // "bottom", "left", "right"
    diceSize: androidx.compose.ui.unit.Dp = 60.dp
) {
    when (checkboxPosition) {
        "left" -> {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AdvancedCheckbox(
                    state = checkboxState,
                    onStateChange = onCheckboxStateChange,
                    modifier = Modifier.size(32.dp)
                )
                Enhanced3DDice(
                    value = playedValue,
                    isRolling = false, // Animasyon kapalı
                    size = diceSize
                )
            }
        }
        "right" -> {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Enhanced3DDice(
                    value = playedValue,
                    isRolling = false, // Animasyon kapalı
                    size = diceSize
                )
                AdvancedCheckbox(
                    state = checkboxState,
                    onStateChange = onCheckboxStateChange,
                    modifier = Modifier.size(32.dp)
                )
            }
        }
        else -> {
            // bottom (default)
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Enhanced3DDice(
                    value = playedValue,
                    isRolling = false, // Animasyon kapalı
                    size = diceSize
                )
                Spacer(modifier = Modifier.height(4.dp))
                AdvancedCheckbox(
                    state = checkboxState,
                    onStateChange = onCheckboxStateChange,
                    modifier = Modifier.size(28.dp)
                )
            }
        }
    }
}

@Composable
fun DiceWithCheckbox(
    originalValue: Int,
    playedValue: Int,
    checkboxState: CheckboxState,
    onCheckboxStateChange: (CheckboxState) -> Unit,
    onValueChange: (Int) -> Unit,
    checkboxPosition: String = "bottom", // "bottom", "left", "right"
    diceSize: androidx.compose.ui.unit.Dp = 60.dp
) {
    when (checkboxPosition) {
        "left" -> {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AdvancedCheckbox(
                    state = checkboxState,
                    onStateChange = onCheckboxStateChange,
                    modifier = Modifier.size(32.dp)
                )
                DraggableDice(
                    value = playedValue,
                    onValueChange = onValueChange,
                    size = diceSize
                )
            }
        }
        "right" -> {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                DraggableDice(
                    value = playedValue,
                    onValueChange = onValueChange,
                    size = diceSize
                )
                AdvancedCheckbox(
                    state = checkboxState,
                    onStateChange = onCheckboxStateChange,
                    modifier = Modifier.size(32.dp)
                )
            }
        }
        else -> {
            // bottom (default)
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                DraggableDice(
                    value = playedValue,
                    onValueChange = onValueChange,
                    size = diceSize
                )
                Spacer(modifier = Modifier.height(4.dp))
                AdvancedCheckbox(
                    state = checkboxState,
                    onStateChange = onCheckboxStateChange,
                    modifier = Modifier.size(28.dp)
                )
            }
        }
    }
}

@Composable 
fun AdvancedCheckbox(
    state: CheckboxState,
    onStateChange: (CheckboxState) -> Unit,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    
    Box(
        modifier = modifier
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = {
                        // Normal tıklama: CHECKED <-> UNCHECKED
                        val newState = when (state) {
                            CheckboxState.CHECKED -> CheckboxState.UNCHECKED
                            CheckboxState.UNCHECKED -> CheckboxState.CHECKED
                            CheckboxState.SQUARE -> CheckboxState.CHECKED
                        }
                        onStateChange(newState)
                    },
                    onLongPress = {
                        // Uzun basma: SQUARE durumuna geç
                        onStateChange(CheckboxState.SQUARE)
                    }
                )
            }
            .background(
                color = when (state) {
                    CheckboxState.CHECKED -> Color(0xFF4CAF50)
                    CheckboxState.UNCHECKED -> Color.Transparent
                    CheckboxState.SQUARE -> Color(0xFF2196F3)
                },
                shape = RoundedCornerShape(4.dp)
            )
            .border(
                width = 2.dp,
                color = Color.Gray,
                shape = RoundedCornerShape(4.dp)
            ),
        contentAlignment = Alignment.Center
    ) {
        when (state) {
            CheckboxState.CHECKED -> Text("✓", color = Color.White, fontSize = 14.sp)
            CheckboxState.UNCHECKED -> {} // Boş
            CheckboxState.SQUARE -> Text("■", color = Color.White, fontSize = 12.sp)
        }
    }
}

@Composable
fun DraggableDice(
    value: Int,
    onValueChange: (Int) -> Unit,
    size: androidx.compose.ui.unit.Dp = 60.dp
) {
    var dragAmount by remember { mutableFloatStateOf(0f) }
    val sensitivity = 20f // Her 20px için 1 azalma
    
    Box(
        modifier = Modifier
            .size(size)
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragEnd = {
                        dragAmount = 0f
                    }
                ) { change, dragAmountChange ->
                    dragAmount += dragAmountChange.y
                    val steps = (dragAmount / sensitivity).toInt()
                    if (steps != 0) {
                        val newValue = (value - steps).coerceIn(1, 6)
                        if (newValue != value) {
                            onValueChange(newValue)
                        }
                        dragAmount = 0f
                    }
                }
            }
    ) {
        Enhanced3DDice(value = value, isRolling = false, size = size)
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