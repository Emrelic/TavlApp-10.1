package com.tavla.tavlapp

import android.os.Bundle
import android.view.View
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

class DiceProcessingActivity : ComponentActivity() {
    private lateinit var dbHelper: DatabaseHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Full screen
        window.decorView.systemUiVisibility = (
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
                        View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or
                        View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
                        View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
                        View.SYSTEM_UI_FLAG_FULLSCREEN or
                        View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                )

        dbHelper = DatabaseHelper(this)

        val matchId = intent.getLongExtra("match_id", -1)
        val player1Id = intent.getLongExtra("player1_id", -1)
        val player2Id = intent.getLongExtra("player2_id", -1)
        val player1Name = intent.getStringExtra("player1_name") ?: "Oyuncu 1"
        val player2Name = intent.getStringExtra("player2_name") ?: "Oyuncu 2"
        val processPartialDice = intent.getBooleanExtra("process_partial_dice", false)

        setContent {
            MaterialTheme {
                DiceProcessingScreen(
                    dbHelper = dbHelper,
                    matchId = matchId,
                    player1Id = player1Id,
                    player2Id = player2Id,
                    player1Name = player1Name,
                    player2Name = player2Name,
                    processPartialDice = processPartialDice,
                    onClose = { finish() }
                )
            }
        }
    }
}

@Composable
fun DiceProcessingScreen(
    dbHelper: DatabaseHelper,
    matchId: Long,
    player1Id: Long,
    player2Id: Long,
    player1Name: String,
    player2Name: String,
    processPartialDice: Boolean,
    onClose: () -> Unit
) {
    // Sıra durumu: true = oyuncu 1, false = oyuncu 2
    var currentPlayerTurn by remember { mutableStateOf(true) }
    var selectedDiceCombo by remember { mutableStateOf("") }
    var selectedDiceEvaluation by remember { mutableIntStateOf(0) }
    
    // Zar durumu checkboxları
    var dice1State by remember { mutableStateOf(DiceState.NONE) }
    var dice2State by remember { mutableStateOf(DiceState.NONE) }
    var dice3State by remember { mutableStateOf(DiceState.NONE) }
    var dice4State by remember { mutableStateOf(DiceState.NONE) }
    
    val scrollState = rememberScrollState()
    
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = if (currentPlayerTurn) Color(0xFFE3F2FD) else Color(0xFFFFEBEE) // Açık mavi/kırmızı
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // Başlık ve Kapat butonu
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "ZAR İSTATİSTİKLERİ İŞLEME",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1976D2)
                )
                Button(
                    onClick = onClose,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFB71C1C))
                ) {
                    Text("KAPAT", fontSize = 12.sp)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Sıra göstergesi
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Sol oyuncu
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(60.dp)
                        .background(
                            color = if (currentPlayerTurn) Color(0xFF1976D2) else Color(0xFFBBBBBB),
                            shape = RoundedCornerShape(topStart = 12.dp, bottomStart = 12.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = player1Name.uppercase(),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
                
                // Ok
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(Color(0xFF424242), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (currentPlayerTurn) "◀" else "▶",
                        fontSize = 20.sp,
                        color = Color.White
                    )
                }
                
                // Sağ oyuncu
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(60.dp)
                        .background(
                            color = if (!currentPlayerTurn) Color(0xFFD32F2F) else Color(0xFFBBBBBB),
                            shape = RoundedCornerShape(topEnd = 12.dp, bottomEnd = 12.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = player2Name.uppercase(),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(modifier = Modifier.fillMaxSize()) {
                // Sol sütun: Zar kombinasyonları tablosu
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(scrollState)
                        .padding(end = 8.dp)
                ) {
                    Text(
                        text = "ZAR KOMBİNASYONLARI",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    
                    DiceHelper.getAllDiceCombinations().forEach { combo ->
                        val isSelected = selectedDiceCombo == combo
                        val isDouble = combo.split("-").let { it[0] == it[1] }
                        
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 2.dp)
                                .clickable { 
                                    selectedDiceCombo = combo
                                    // Çift zar seçilirse 4 checkbox, normal zar seçilirse 2 checkbox
                                    if (!isDouble) {
                                        dice3State = DiceState.NONE
                                        dice4State = DiceState.NONE
                                    }
                                },
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSelected) Color(0xFF4CAF50) else Color(0xFFF5F5F5)
                            ),
                            border = BorderStroke(
                                width = if (isSelected) 2.dp else 1.dp,
                                color = if (isSelected) Color(0xFF2E7D32) else Color.Gray
                            )
                        ) {
                            Text(
                                text = combo + if (isDouble) " (4 zar)" else "",
                                fontSize = 14.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) Color.White else Color.Black,
                                modifier = Modifier.padding(12.dp),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }

                // Sağ sütun: İşleme kontrolları
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(scrollState)
                        .padding(start = 8.dp)
                ) {
                    if (selectedDiceCombo.isNotEmpty()) {
                        Text(
                            text = "Seçilen Zar: $selectedDiceCombo",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )

                        // Zar durumu checkboxları
                        val isDouble = selectedDiceCombo.split("-").let { it[0] == it[1] }
                        val numberOfDice = if (isDouble) 4 else 2

                        Text(
                            text = "Zar Durumları:",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )

                        // Zar 1
                        DiceStateRow(
                            label = "Zar 1:",
                            state = dice1State,
                            onStateChange = { dice1State = it },
                            processPartialDice = processPartialDice
                        )

                        // Zar 2
                        DiceStateRow(
                            label = "Zar 2:",
                            state = dice2State,
                            onStateChange = { dice2State = it },
                            processPartialDice = processPartialDice
                        )

                        // Çift zar ise ek zarlar
                        if (isDouble) {
                            DiceStateRow(
                                label = "Zar 3:",
                                state = dice3State,
                                onStateChange = { dice3State = it },
                                processPartialDice = processPartialDice
                            )

                            DiceStateRow(
                                label = "Zar 4:",
                                state = dice4State,
                                onStateChange = { dice4State = it },
                                processPartialDice = processPartialDice
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Zar değerlendirmesi (1-6 skala)
                        Text(
                            text = "Zar Değerlendirmesi:",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            (1..6).forEach { rating ->
                                Button(
                                    onClick = { selectedDiceEvaluation = rating },
                                    modifier = Modifier
                                        .size(40.dp)
                                        .padding(2.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = when {
                                            selectedDiceEvaluation == rating -> Color(0xFF4CAF50)
                                            rating <= 2 -> Color(0xFFE53935) // Kırmızı (kötü)
                                            rating == 3 -> Color(0xFFFF9800) // Turuncu (vasat)
                                            rating == 4 -> Color(0xFFFFEB3B) // Sarı (faydalı)
                                            rating >= 5 -> Color(0xFF4CAF50) // Yeşil (iyi)
                                            else -> Color.Gray
                                        }
                                    ),
                                    contentPadding = PaddingValues(0.dp)
                                ) {
                                    Text(
                                        text = rating.toString(),
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }
                            }
                        }

                        // Değerlendirme açıklamaları
                        Spacer(modifier = Modifier.height(8.dp))
                        Column {
                            Text("6: Maç puan kazandıran şanslı zar", fontSize = 10.sp, color = Color(0xFF4CAF50))
                            Text("5: Oyunun ibresini çeviren faydalı zar", fontSize = 10.sp, color = Color(0xFF4CAF50))
                            Text("4: Yapısal olarak faydalı zar", fontSize = 10.sp, color = Color(0xFFFFEB3B))
                            Text("3: Vasat zar (niteliksiz)", fontSize = 10.sp, color = Color(0xFFFF9800))
                            Text("2: Zora sokan zar", fontSize = 10.sp, color = Color(0xFFE53935))
                            Text("1: Oyun/puan kaybettiren şansız zar", fontSize = 10.sp, color = Color(0xFFE53935))
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // İşle butonu
                        Button(
                            onClick = {
                                if (selectedDiceCombo.isNotEmpty() && selectedDiceEvaluation > 0) {
                                    // Zar istatistiklerini işle
                                    processDiceRoll(
                                        dbHelper = dbHelper,
                                        matchId = matchId,
                                        playerId = if (currentPlayerTurn) player1Id else player2Id,
                                        diceCombo = selectedDiceCombo,
                                        evaluation = selectedDiceEvaluation,
                                        dice1State = dice1State,
                                        dice2State = dice2State,
                                        dice3State = dice3State,
                                        dice4State = dice4State
                                    )
                                    
                                    // Sırayı değiştir ve formu sıfırla
                                    currentPlayerTurn = !currentPlayerTurn
                                    selectedDiceCombo = ""
                                    selectedDiceEvaluation = 0
                                    dice1State = DiceState.NONE
                                    dice2State = DiceState.NONE
                                    dice3State = DiceState.NONE
                                    dice4State = DiceState.NONE
                                }
                            },
                            enabled = selectedDiceCombo.isNotEmpty() && selectedDiceEvaluation > 0,
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF4CAF50)
                            )
                        ) {
                            Text("İŞLE", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        }
                    } else {
                        Text(
                            text = "Lütfen sol taraftan bir zar kombinasyonu seçin",
                            fontSize = 14.sp,
                            textAlign = TextAlign.Center,
                            color = Color.Gray
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun DiceStateRow(
    label: String,
    state: DiceState,
    onStateChange: (DiceState) -> Unit,
    processPartialDice: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            modifier = Modifier.width(60.dp)
        )
        
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Tam Oynandı
            DiceStateButton(
                text = "☑",
                isSelected = state == DiceState.PLAYED,
                color = Color(0xFF4CAF50),
                onClick = { onStateChange(DiceState.PLAYED) }
            )
            
            // Gele
            DiceStateButton(
                text = "☐",
                isSelected = state == DiceState.WASTED,
                color = Color(0xFFFF9800),
                onClick = { onStateChange(DiceState.WASTED) }
            )
            
            // Kısmi boşa (sadece processPartialDice true ise)
            if (processPartialDice) {
                DiceStateButton(
                    text = "↻",
                    isSelected = state == DiceState.PARTIAL_WASTED,
                    color = Color(0xFF9C27B0),
                    onClick = { onStateChange(DiceState.PARTIAL_WASTED) }
                )
                
                // Bitiş artığı
                DiceStateButton(
                    text = "■",
                    isSelected = state == DiceState.END_WASTE,
                    color = Color(0xFFE53935),
                    onClick = { onStateChange(DiceState.END_WASTE) }
                )
            }
        }
    }
}

@Composable
fun DiceStateButton(
    text: String,
    isSelected: Boolean,
    color: Color,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = Modifier.size(32.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isSelected) color else Color.LightGray
        ),
        contentPadding = PaddingValues(0.dp)
    ) {
        Text(
            text = text,
            fontSize = 14.sp,
            color = Color.White
        )
    }
}

fun processDiceRoll(
    dbHelper: DatabaseHelper,
    matchId: Long,
    playerId: Long,
    diceCombo: String,
    evaluation: Int,
    dice1State: DiceState,
    dice2State: DiceState,
    dice3State: DiceState,
    dice4State: DiceState
) {
    // Zar kombinasyonunu parse et
    val diceParts = diceCombo.split("-")
    val dice1 = diceParts[0].toInt()
    val dice2 = diceParts[1].toInt()
    
    // DiceRollResult oluştur
    val rollResult = DiceRollResult(
        dice1 = dice1,
        dice2 = dice2,
        dice1State = dice1State,
        dice2State = dice2State,
        dice3State = dice3State,
        dice4State = dice4State
    )
    
    // Veritabanına kaydet
    dbHelper.saveDiceRoll(matchId, playerId, rollResult)
    
    // Zar değerlendirmesini de kaydet
    dbHelper.saveDiceEvaluation(matchId, playerId, diceCombo, evaluation)
}