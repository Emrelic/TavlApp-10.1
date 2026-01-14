package com.tavla.tavlapp

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.Dp

// Zar kombinasyonu veri sınıfı
data class DiceCombo(
    val dice1: Int,
    val dice2: Int,
    val isDouble: Boolean = dice1 == dice2
) {
    val displayText: String get() = "$dice1$dice2"
    val diceList: List<Int> get() = if (isDouble) listOf(dice1, dice1, dice1, dice1) else listOf(dice1, dice2)
}

// Zar durumu enum
enum class ProcessingDiceState {
    NORMAL,   // ✓ Normal onaylandı
    GELE,     // ☐ Gele (checkbox kaldırıldı)
    KISMI,    // ↻ Kısmen (yuvarlanarak azaltıldı)
    ARTIK     // ■ Artık zar (kare dolu)
}

// Tekil zar veri sınıfı
data class IndividualDice(
    val originalValue: Int,
    val currentValue: Int = originalValue,
    val state: ProcessingDiceState = ProcessingDiceState.NORMAL,
    val timesReduced: Int = 0
) {
    val isReduced: Boolean get() = currentValue < originalValue
}

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
        val currentPlayerId = player1Id // Varsayılan olarak player1
        val currentPlayerName = player1Name // Varsayılan olarak player1

        setContent {
            TavlaAppTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    DiceProcessingScreen(
                        matchId = matchId,
                        player1Id = player1Id,
                        player2Id = player2Id,
                        player1Name = player1Name,
                        player2Name = player2Name,
                        currentPlayerId = currentPlayerId,
                        currentPlayerName = currentPlayerName,
                        dbHelper = dbHelper,
                        onBack = { finish() }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiceProcessingScreen(
    matchId: Long,
    player1Id: Long,
    player2Id: Long,
    player1Name: String,
    player2Name: String,
    currentPlayerId: Long,
    currentPlayerName: String,
    dbHelper: DatabaseHelper,
    onBack: () -> Unit
) {
    // Piramit düzende zar kombinasyonları
    val diceCombos = remember {
        listOf(
            // 6'lı satır
            listOf(DiceCombo(6, 6), DiceCombo(6, 5), DiceCombo(6, 4), DiceCombo(6, 3), DiceCombo(6, 2), DiceCombo(6, 1)),
            // 5'li satır
            listOf(DiceCombo(5, 5), DiceCombo(5, 4), DiceCombo(5, 3), DiceCombo(5, 2), DiceCombo(5, 1)),
            // 4'lü satır
            listOf(DiceCombo(4, 4), DiceCombo(4, 3), DiceCombo(4, 2), DiceCombo(4, 1)),
            // 3'lü satır
            listOf(DiceCombo(3, 3), DiceCombo(3, 2), DiceCombo(3, 1)),
            // 2'li satır
            listOf(DiceCombo(2, 2), DiceCombo(2, 1)),
            // 1'li satır
            listOf(DiceCombo(1, 1))
        )
    }

    var selectedCombo by remember { mutableStateOf<DiceCombo?>(null) }
    var individualDices by remember { mutableStateOf<List<IndividualDice>>(emptyList()) }
    var selectedRating by remember { mutableStateOf<Int?>(null) }

    val playerColor = if (currentPlayerId == player1Id) Color(0xFF2196F3) else Color(0xFFF44336)

    // Seçilen kombo değiştiğinde zar listesini güncelle
    LaunchedEffect(selectedCombo) {
        selectedCombo?.let { combo ->
            individualDices = combo.diceList.map { IndividualDice(it) }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text("Zar İstatistik İşleme", fontSize = 18.sp)
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Geri")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = playerColor.copy(alpha = 0.1f)
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(8.dp)
        ) {
            // Oyuncu bilgisi
            Text(
                text = "Sıra: $currentPlayerName",
                fontSize = 14.sp,
                color = playerColor,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            // Piramit düzen zar seçimi (sola dayalı)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalAlignment = Alignment.Start
            ) {
                diceCombos.forEach { row ->
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(vertical = 4.dp)
                    ) {
                        row.forEach { combo ->
                            PyramidDiceCard(
                                combo = combo,
                                isSelected = selectedCombo == combo,
                                playerColor = playerColor,
                                onClick = { selectedCombo = combo }
                            )
                        }
                    }
                }
            }

            // Seçili zarların işleme paneli
            if (selectedCombo != null && individualDices.isNotEmpty()) {
                Spacer(modifier = Modifier.height(16.dp))
                
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF8F8F8))
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "Seçilen: ${selectedCombo!!.displayText}",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = playerColor
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Tekil zarlar işleme
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            individualDices.forEachIndexed { index, dice ->
                                IndividualDiceProcessor(
                                    dice = dice,
                                    playerColor = playerColor,
                                    onDiceChange = { newDice ->
                                        individualDices = individualDices.toMutableList().also { list ->
                                            list[index] = newDice
                                        }
                                    },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Değerlendirme puanları (1-6)
                        Text("Değerlendirme:", fontWeight = FontWeight.Medium)
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.padding(top = 8.dp)
                        ) {
                            (1..6).forEach { rating ->
                                Button(
                                    onClick = { selectedRating = rating },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (selectedRating == rating) playerColor else Color.Gray.copy(alpha = 0.3f)
                                    ),
                                    modifier = Modifier
                                        .size(40.dp)
                                        .weight(1f)
                                ) {
                                    Text(
                                        text = rating.toString(),
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Kaydet butonu
                        Button(
                            onClick = {
                                selectedRating?.let { rating ->
                                    val stateInfo = analyzeIndividualDiceStates(individualDices)
                                    
                                    dbHelper.saveDiceEvaluation(
                                        matchId = matchId,
                                        playerId = currentPlayerId,
                                        diceCombo = "${selectedCombo!!.displayText} ($stateInfo)",
                                        rating = rating,
                                        state = stateInfo
                                    )
                                    onBack()
                                }
                            },
                            enabled = selectedRating != null,
                            colors = ButtonDefaults.buttonColors(containerColor = playerColor),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Kaydet ve Bitir", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PyramidDiceCard(
    combo: DiceCombo,
    isSelected: Boolean,
    playerColor: Color,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .size(55.dp)
            .clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) playerColor.copy(alpha = 0.1f) else Color(0xFFFAFAFA)
        ),
        border = androidx.compose.foundation.BorderStroke(
            width = if (isSelected) 3.dp else 1.dp,
            color = if (isSelected) playerColor else Color.Gray.copy(alpha = 0.4f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Zar desenleri - HER ZAMAN YAN YANA 2 ZAR
            Row(
                horizontalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                MiniDiceVisual(combo.dice1, 12.dp)
                MiniDiceVisual(combo.dice2, 12.dp)
            }
            
            Spacer(modifier = Modifier.height(2.dp))
            
            // Sayısal gösterim
            Text(
                text = combo.displayText,
                fontSize = 8.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = if (isSelected) playerColor else Color.DarkGray
            )
        }
    }
}

@Composable
fun IndividualDiceProcessor(
    dice: IndividualDice,
    playerColor: Color,
    onDiceChange: (IndividualDice) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Zar görseli
        Box(
            modifier = Modifier
                .size(60.dp)
                .clickable { 
                    // Zarı yuvarla (değeri 1 azalt, minimum 1)
                    if (dice.currentValue > 1) {
                        onDiceChange(
                            dice.copy(
                                currentValue = dice.currentValue - 1,
                                state = ProcessingDiceState.KISMI,
                                timesReduced = dice.timesReduced + 1
                            )
                        )
                    }
                }
                .rotate(if (dice.isReduced) (dice.timesReduced * 30f) else 0f) // Yuvarlandıkça dönsün
        ) {
            DiceDotLarge(
                dice.currentValue, 
                if (dice.isReduced) 55.dp else 60.dp
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Üç durumlu checkbox
        DiceStateCheckbox(
            state = dice.state,
            playerColor = playerColor,
            onStateChange = { newState ->
                onDiceChange(dice.copy(state = newState))
            }
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = when (dice.state) {
                ProcessingDiceState.NORMAL -> "ONAY"
                ProcessingDiceState.GELE -> "GELE"
                ProcessingDiceState.KISMI -> "KISMİ"
                ProcessingDiceState.ARTIK -> "ARTIK"
            },
            fontSize = 10.sp,
            color = when (dice.state) {
                ProcessingDiceState.NORMAL -> Color(0xFF4CAF50)
                ProcessingDiceState.GELE -> Color(0xFF9E9E9E)
                ProcessingDiceState.KISMI -> Color(0xFFFF9800)
                ProcessingDiceState.ARTIK -> Color(0xFF9C27B0)
            },
            fontWeight = FontWeight.Bold
        )

        // Orijinal değerden farklıysa göster
        if (dice.isReduced) {
            Text(
                text = "(${dice.originalValue}→${dice.currentValue})",
                fontSize = 8.sp,
                color = Color.Gray
            )
        }
    }
}

@Composable
fun DiceStateCheckbox(
    state: ProcessingDiceState,
    playerColor: Color,
    onStateChange: (ProcessingDiceState) -> Unit
) {
    Box(
        modifier = Modifier
            .size(24.dp)
            .clickable {
                val newState = when (state) {
                    ProcessingDiceState.NORMAL -> ProcessingDiceState.GELE
                    ProcessingDiceState.GELE -> ProcessingDiceState.ARTIK
                    ProcessingDiceState.KISMI -> ProcessingDiceState.ARTIK
                    ProcessingDiceState.ARTIK -> ProcessingDiceState.NORMAL
                }
                onStateChange(newState)
            }
            .border(
                2.dp, 
                when (state) {
                    ProcessingDiceState.NORMAL -> Color(0xFF4CAF50)
                    ProcessingDiceState.GELE -> Color(0xFF9E9E9E)
                    ProcessingDiceState.KISMI -> Color(0xFFFF9800)
                    ProcessingDiceState.ARTIK -> Color(0xFF9C27B0)
                },
                RoundedCornerShape(4.dp)
            )
            .background(
                when (state) {
                    ProcessingDiceState.NORMAL -> Color(0xFF4CAF50).copy(alpha = 0.2f)
                    ProcessingDiceState.GELE -> Color.Transparent
                    ProcessingDiceState.KISMI -> Color(0xFFFF9800).copy(alpha = 0.2f)
                    ProcessingDiceState.ARTIK -> Color(0xFF9C27B0)
                },
                RoundedCornerShape(4.dp)
            ),
        contentAlignment = Alignment.Center
    ) {
        when (state) {
            ProcessingDiceState.NORMAL -> Text("✓", color = Color.White, fontWeight = FontWeight.Bold)
            ProcessingDiceState.GELE -> Text("☐", color = Color.Gray)
            ProcessingDiceState.KISMI -> Text("↻", color = Color.White, fontWeight = FontWeight.Bold)
            ProcessingDiceState.ARTIK -> Text("■", color = Color.White, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun MiniDiceVisual(
    number: Int,
    size: Dp
) {
    Box(
        modifier = Modifier
            .size(size)
            .background(Color.White, RoundedCornerShape(4.dp))
            .border(1.dp, Color.Black, RoundedCornerShape(4.dp)),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = number.toString(),
            color = Color.Black,
            fontSize = (size.value * 0.6f).sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun DiceDotLarge(
    number: Int,
    size: Dp
) {
    Box(
        modifier = Modifier
            .size(size)
            .background(Color.White, RoundedCornerShape(8.dp))
            .border(2.dp, Color.Black, RoundedCornerShape(8.dp)),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = number.toString(),
            color = Color.Black,
            fontSize = (size.value * 0.5f).sp,
            fontWeight = FontWeight.Bold
        )
    }
}

// Zar durumlarını analiz et
fun analyzeIndividualDiceStates(dices: List<IndividualDice>): String {
    val normalCount = dices.count { it.state == ProcessingDiceState.NORMAL }
    val geleCount = dices.count { it.state == ProcessingDiceState.GELE }
    val kismiCount = dices.count { it.state == ProcessingDiceState.KISMI }
    val artikCount = dices.count { it.state == ProcessingDiceState.ARTIK }
    
    return buildString {
        if (normalCount > 0) append("ONAY:$normalCount ")
        if (geleCount > 0) append("GELE:$geleCount ")
        if (kismiCount > 0) append("KISMİ:$kismiCount ")
        if (artikCount > 0) append("ARTIK:$artikCount")
    }.trim()
}