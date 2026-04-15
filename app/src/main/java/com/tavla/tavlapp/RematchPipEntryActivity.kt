package com.tavla.tavlapp

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.content.Intent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Oyun sonu pip girisi ve sonuc kaydetme ekrani
 */
class RematchPipEntryActivity : ComponentActivity() {
    private lateinit var dbHelper: DatabaseHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        dbHelper = DatabaseHelper(this)

        val encounterId = intent.getLongExtra("encounter_id", -1L)
        val partyIndex = intent.getIntExtra("party_index", 0)
        val setIndex = intent.getIntExtra("set_index", 0)
        val roundNumber = intent.getIntExtra("round_number", 1)
        val leftPlayerId = intent.getLongExtra("left_player_id", 0L)
        val rightPlayerId = intent.getLongExtra("right_player_id", 0L)
        val leftPlayerName = intent.getStringExtra("left_player_name") ?: "Oyuncu 1"
        val rightPlayerName = intent.getStringExtra("right_player_name") ?: "Oyuncu 2"
        val dicePairsUsed = intent.getIntExtra("dice_pairs_used", 0)

        setContent {
            TavlaAppTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    RematchPipEntryScreen(
                        dbHelper = dbHelper,
                        encounterId = encounterId,
                        partyIndex = partyIndex,
                        setIndex = setIndex,
                        roundNumber = roundNumber,
                        leftPlayerId = leftPlayerId,
                        rightPlayerId = rightPlayerId,
                        leftPlayerName = leftPlayerName,
                        rightPlayerName = rightPlayerName,
                        dicePairsUsed = dicePairsUsed
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RematchPipEntryScreen(
    dbHelper: DatabaseHelper,
    encounterId: Long,
    partyIndex: Int,
    setIndex: Int,
    roundNumber: Int,
    leftPlayerId: Long,
    rightPlayerId: Long,
    leftPlayerName: String,
    rightPlayerName: String,
    dicePairsUsed: Int
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // Mevcut parti skoru
    var currentLeftScore by remember { mutableStateOf(0) }
    var currentRightScore by remember { mutableStateOf(0) }

    // Parti skorunu yukle
    LaunchedEffect(encounterId, partyIndex, roundNumber) {
        val encounter = dbHelper.getRematchEncounter(encounterId)
        if (encounter != null) {
            val (p1Score, p2Score) = dbHelper.getPartyScore(encounterId, partyIndex, roundNumber)
            if (roundNumber == 1) {
                currentLeftScore = p1Score
                currentRightScore = p2Score
            } else {
                // Rovansta oyuncular yer degistiriyor
                currentLeftScore = p2Score
                currentRightScore = p1Score
            }
        }
    }

    // Kazanan secimi
    var selectedWinner by remember { mutableStateOf<String?>(null) } // "left" veya "right"

    // Pip sayisi
    var loserPipCount by remember { mutableStateOf("") }

    // Kazanma tipi
    var selectedWinType by remember { mutableStateOf("SINGLE") }

    // Kup degeri
    var cubeValue by remember { mutableStateOf(1) }

    // Kaydetme durumu
    var isSaving by remember { mutableStateOf(false) }

    // Kazanma tipi secenekleri
    val winTypeOptions = listOf(
        Triple("SINGLE", "Tek", 1),
        Triple("MARS", "Mars", 2),
        Triple("BACKGAMMON", "Backgammon", 3),
        Triple("RESIGN", "Pes", 1)
    )

    // Kup secenekleri
    val cubeOptions = listOf(1, 2, 4, 8, 16, 32, 64)

    // Final skoru hesapla
    val baseScore = winTypeOptions.find { it.first == selectedWinType }?.third ?: 1
    val finalScore = baseScore * cubeValue

    // Tahmini yeni skor
    // Encounter'dan hedef puani al
    var partyTargetScore by remember { mutableStateOf(11) }
    var trackPipCount by remember { mutableStateOf(true) }

    LaunchedEffect(encounterId) {
        val encounter = dbHelper.getRematchEncounter(encounterId)
        if (encounter != null) {
            partyTargetScore = encounter.targetScore
            trackPipCount = encounter.trackPipCount
        }
    }

    val projectedLeftScore = if (selectedWinner == "left") currentLeftScore + finalScore else currentLeftScore
    val projectedRightScore = if (selectedWinner == "right") currentRightScore + finalScore else currentRightScore
    val partyWillEnd = projectedLeftScore >= partyTargetScore || projectedRightScore >= partyTargetScore

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Parti skoru karti
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = if (partyWillEnd && selectedWinner != null) Color(0xFFE8F5E9) else Color(0xFFF3E5F5)
            )
        ) {
            Column(
                modifier = Modifier.padding(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Parti ${partyIndex + 1} - Tur $roundNumber | Oyun ${setIndex + 1}",
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF6A1B9A),
                    fontSize = 14.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "$leftPlayerName $currentLeftScore - $currentRightScore $rightPlayerName",
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp
                )
                if (selectedWinner != null) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (partyWillEnd)
                            "Sonrasi: $projectedLeftScore - $projectedRightScore  PARTi BiTER!"
                        else
                            "Sonrasi: $projectedLeftScore - $projectedRightScore",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (partyWillEnd) Color(0xFF4CAF50) else Color.Gray
                    )
                }
            }
        }

        // Baslik
        Text(
            text = "Oyun Sonucu",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )

        Text(
            text = "Tur $roundNumber - Parti ${partyIndex + 1} - Oyun ${setIndex + 1}",
            color = Color.Gray,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Kazanan secimi
        Text(
            text = "Kazanan",
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Sol oyuncu
            Card(
                modifier = Modifier
                    .weight(1f)
                    .clickable { selectedWinner = "left" },
                colors = CardDefaults.cardColors(
                    containerColor = if (selectedWinner == "left")
                        Color(0xFF1565C0) else Color(0xFFE3F2FD)
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = leftPlayerName,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = if (selectedWinner == "left") Color.White else Color.Black
                    )
                }
            }

            // Sag oyuncu
            Card(
                modifier = Modifier
                    .weight(1f)
                    .clickable { selectedWinner = "right" },
                colors = CardDefaults.cardColors(
                    containerColor = if (selectedWinner == "right")
                        Color(0xFFC62828) else Color(0xFFFFEBEE)
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = rightPlayerName,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = if (selectedWinner == "right") Color.White else Color.Black
                    )
                }
            }
        }

        // Kazanma tipi
        Text(
            text = "Kazanma Tipi",
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            winTypeOptions.forEach { (type, label, _) ->
                Button(
                    onClick = { selectedWinType = type },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (selectedWinType == type)
                            Color(0xFF6A1B9A) else Color.LightGray
                    ),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = label,
                        color = if (selectedWinType == type) Color.White else Color.Black,
                        fontSize = 12.sp
                    )
                }
            }
        }

        // Kup degeri
        Text(
            text = "Kup Degeri",
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            cubeOptions.forEach { value ->
                Button(
                    onClick = { cubeValue = value },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (cubeValue == value)
                            Color(0xFF6A1B9A) else Color.LightGray
                    ),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(4.dp)
                ) {
                    Text(
                        text = value.toString(),
                        color = if (cubeValue == value) Color.White else Color.Black,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // Pip sayisi (ayardan acik ise goster)
        if (trackPipCount) {
            Text(
                text = "Kaybeden Pip Sayisi",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )

            OutlinedTextField(
                value = loserPipCount,
                onValueChange = { newValue ->
                    if (newValue.isEmpty() || newValue.all { it.isDigit() }) {
                        loserPipCount = newValue
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Pip sayisi girin") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        // Sonuc ozeti
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFF3E5F5))
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Sonuc",
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF6A1B9A)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = if (selectedWinner != null) {
                        val winnerName = if (selectedWinner == "left") leftPlayerName else rightPlayerName
                        "$winnerName kazandi - $finalScore puan"
                    } else {
                        "Kazanan seciniz"
                    },
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // Kaydet butonu
        Button(
            onClick = {
                if (selectedWinner == null) {
                    Toast.makeText(context, "Lutfen kazanan seciniz", Toast.LENGTH_SHORT).show()
                    return@Button
                }

                isSaving = true
                val winnerId = if (selectedWinner == "left") leftPlayerId else rightPlayerId
                val pipCount = loserPipCount.toIntOrNull() ?: 0

                coroutineScope.launch {
                    try {
                        val navigateIntent = withContext(Dispatchers.IO) {
                            // SharedPreferences'dan zar istatistiklerini oku
                            val prefs = context.getSharedPreferences("rematch_prefs", android.content.Context.MODE_PRIVATE)
                            val savedLeftDiceTotal = prefs.getInt("left_dice_total_${encounterId}", 0)
                            val savedRightDiceTotal = prefs.getInt("right_dice_total_${encounterId}", 0)
                            val savedLeftDoublesCount = prefs.getInt("left_doubles_count_${encounterId}", 0)
                            val savedRightDoublesCount = prefs.getInt("right_doubles_count_${encounterId}", 0)

                            // 1. Oyun sonucunu kaydet
                            dbHelper.saveRematchGameResult(
                                encounterId = encounterId,
                                partyIndex = partyIndex,
                                setIndex = setIndex,
                                roundNumber = roundNumber,
                                leftPlayerId = leftPlayerId,
                                rightPlayerId = rightPlayerId,
                                winnerId = winnerId,
                                winType = selectedWinType,
                                cubeValue = cubeValue,
                                finalScore = finalScore,
                                loserPipCount = pipCount,
                                dicePairsUsed = dicePairsUsed,
                                leftDiceTotal = savedLeftDiceTotal,
                                rightDiceTotal = savedRightDiceTotal,
                                leftDoublesCount = savedLeftDoublesCount,
                                rightDoublesCount = savedRightDoublesCount
                            )

                            // 2. Guncel parti skorunu al
                            val encounter = dbHelper.getRematchEncounter(encounterId)!!
                            val totalParties = encounter.totalParties
                            val (p1Score, p2Score) = dbHelper.getPartyScore(encounterId, partyIndex, roundNumber)

                            val targetScore = encounter.targetScore
                            val partyIsOver = p1Score >= targetScore || p2Score >= targetScore
                            val totalGamesPlayed = setIndex + 1

                            // Activity log
                            val winnerName = if (selectedWinner == "left") leftPlayerName else rightPlayerName
                            dbHelper.addActivityLog(
                                actionType = ActionTypes.REMATCH_MATCH_END,
                                description = "Rovansli oyun: $winnerName kazandi, $finalScore puan (Parti skoru: $p1Score-$p2Score)",
                                player1Name = leftPlayerName,
                                player2Name = rightPlayerName
                            )

                            if (partyIsOver) {
                                // 3. Parti bitti - parti sonucunu kaydet
                                val partyWinnerId = if (p1Score >= targetScore) encounter.player1Id else encounter.player2Id
                                dbHelper.saveRematchPartyResult(
                                    encounterId = encounterId,
                                    partyIndex = partyIndex,
                                    roundNumber = roundNumber,
                                    player1Score = p1Score,
                                    player2Score = p2Score,
                                    winnerId = partyWinnerId,
                                    totalGamesPlayed = totalGamesPlayed
                                )

                                dbHelper.addActivityLog(
                                    actionType = ActionTypes.REMATCH_ROUND_COMPLETE,
                                    description = "Parti ${partyIndex + 1} bitti: $p1Score-$p2Score ($totalGamesPlayed oyun)",
                                    player1Name = leftPlayerName,
                                    player2Name = rightPlayerName
                                )

                                // 4. Sonraki adimi belirle
                                val nextPartyIndex = partyIndex + 1

                                if (nextPartyIndex >= totalParties) {
                                    // Tum partiler bitti
                                    if (roundNumber == 1) {
                                        // Tur 1 tamamlandi -> Tur 2'ye gec
                                        dbHelper.completeFirstRound(encounterId)
                                        dbHelper.advanceToRematchRound(encounterId)
                                        dbHelper.addActivityLog(
                                            actionType = ActionTypes.REMATCH_ROUND_COMPLETE,
                                            description = "Tur 1 tamamlandi, rovans turu basliyor"
                                        )
                                        // Zar gosterim ekranina git (Tur 2 baslar)
                                        Intent(context, RematchDiceDisplayActivity::class.java).apply {
                                            putExtra("encounter_id", encounterId)
                                            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
                                        }
                                    } else {
                                        // Tum karsilasma bitti
                                        dbHelper.completeEncounter(encounterId)
                                        dbHelper.addActivityLog(
                                            actionType = ActionTypes.REMATCH_ENCOUNTER_COMPLETE,
                                            description = "Rovansli karsilasma tamamlandi"
                                        )
                                        // Son parti karsilastirmasini goster
                                        Intent(context, RematchComparisonActivity::class.java).apply {
                                            putExtra("encounter_id", encounterId)
                                            putExtra("party_index", partyIndex)
                                            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
                                        }
                                    }
                                } else {
                                    // Sonraki partiye gec
                                    dbHelper.updateEncounterProgress(encounterId, nextPartyIndex, 0)

                                    if (roundNumber == 2) {
                                        // Tur 2'de parti bitti -> karsilastirma ekranini goster
                                        Intent(context, RematchComparisonActivity::class.java).apply {
                                            putExtra("encounter_id", encounterId)
                                            putExtra("party_index", partyIndex)
                                            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
                                        }
                                    } else {
                                        // Tur 1'de sonraki partiye devam
                                        Intent(context, RematchDiceDisplayActivity::class.java).apply {
                                            putExtra("encounter_id", encounterId)
                                            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
                                        }
                                    }
                                }
                            } else if (setIndex + 1 >= DiceGenerator.maxSetsForTargetScore(targetScore)) {
                                // Maksimum oyun sayisina ulasildi ama kimse hedefe ulasamadi
                                // Mevcut skorlarla partiyi bitir
                                val partyWinnerId = if (p1Score > p2Score) encounter.player1Id else encounter.player2Id
                                dbHelper.saveRematchPartyResult(
                                    encounterId = encounterId,
                                    partyIndex = partyIndex,
                                    roundNumber = roundNumber,
                                    player1Score = p1Score,
                                    player2Score = p2Score,
                                    winnerId = partyWinnerId,
                                    totalGamesPlayed = totalGamesPlayed
                                )

                                val nextPartyIndex = partyIndex + 1
                                if (nextPartyIndex >= totalParties) {
                                    if (roundNumber == 1) {
                                        dbHelper.completeFirstRound(encounterId)
                                        dbHelper.advanceToRematchRound(encounterId)
                                        Intent(context, RematchDiceDisplayActivity::class.java).apply {
                                            putExtra("encounter_id", encounterId)
                                            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
                                        }
                                    } else {
                                        dbHelper.completeEncounter(encounterId)
                                        Intent(context, RematchComparisonActivity::class.java).apply {
                                            putExtra("encounter_id", encounterId)
                                            putExtra("party_index", partyIndex)
                                            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
                                        }
                                    }
                                } else {
                                    dbHelper.updateEncounterProgress(encounterId, nextPartyIndex, 0)
                                    if (roundNumber == 2) {
                                        Intent(context, RematchComparisonActivity::class.java).apply {
                                            putExtra("encounter_id", encounterId)
                                            putExtra("party_index", partyIndex)
                                            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
                                        }
                                    } else {
                                        Intent(context, RematchDiceDisplayActivity::class.java).apply {
                                            putExtra("encounter_id", encounterId)
                                            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
                                        }
                                    }
                                }
                            } else {
                                // Parti devam ediyor - sonraki oyuna gec
                                dbHelper.updateEncounterProgress(encounterId, partyIndex, setIndex + 1)
                                Intent(context, RematchDiceDisplayActivity::class.java).apply {
                                    putExtra("encounter_id", encounterId)
                                    flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
                                }
                            }
                        }

                        // Navigasyon
                        context.startActivity(navigateIntent)
                        (context as? ComponentActivity)?.finish()

                    } catch (e: Exception) {
                        Toast.makeText(context, "Hata: ${e.message}", Toast.LENGTH_SHORT).show()
                    } finally {
                        isSaving = false
                    }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            enabled = !isSaving && selectedWinner != null,
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))
        ) {
            Text(
                text = if (isSaving) "Kaydediliyor..." else "KAYDET",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
        }
    }
}
