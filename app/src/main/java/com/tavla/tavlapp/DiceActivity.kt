package com.tavla.tavlapp

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

class DiceActivity : ComponentActivity() {
    private lateinit var dbHelper: DatabaseHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.decorView.systemUiVisibility = (
            View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
            View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or
            View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
            View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
            View.SYSTEM_UI_FLAG_FULLSCREEN or
            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
        )

        dbHelper = DatabaseHelper(this)

        val gameType = intent.getStringExtra("game_type") ?: "Modern"
        val player1Name = intent.getStringExtra("player1_name") ?: "Oyuncu 1"
        val player2Name = intent.getStringExtra("player2_name") ?: "Oyuncu 2"
        val matchLength = intent.getIntExtra("match_length", 11)
        val matchId = intent.getLongExtra("match_id", -1)
        val player1Id = intent.getLongExtra("player1_id", -1)
        val player2Id = intent.getLongExtra("player2_id", -1)
        val keepStatistics = intent.getBooleanExtra("keep_statistics", false)
        val useTimer = intent.getBooleanExtra("use_timer", false)
        val useSingleButtonForTimerAndDice = intent.getBooleanExtra("use_single_button_for_timer_and_dice", false)
        val useDiceRoller = intent.getBooleanExtra("use_dice_roller", false)
        val markDiceEvaluation = intent.getBooleanExtra("mark_dice_evaluation", false)
        
        // ✅ KATLAMA SİSTEMİ PARAMETRELERİ
        val doublingCubeValue = intent.getIntExtra("doubling_cube_value", 1)
        val player1CanDouble = intent.getBooleanExtra("player1_can_double", true)
        val player2CanDouble = intent.getBooleanExtra("player2_can_double", true)
        val isCrawfordGame = intent.getBooleanExtra("is_crawford_game", false)
        val showPlayer1DoublingMenu = intent.getBooleanExtra("show_player1_doubling_menu", false)
        val showPlayer2DoublingMenu = intent.getBooleanExtra("show_player2_doubling_menu", false)
        val doublingCubePosition = intent.getStringExtra("doubling_cube_position") ?: "CENTER"
        
        // ✅ REPLAY MOD PARAMETRESİ
        val replayDiceSetId = intent.getStringExtra("replay_dice_set_id")
        
        // ✅ HAMLE POZİSYONU PARAMETRELERİ
        val resumeTotalMoveCount = intent.getIntExtra("resume_total_move_count", 0)
        val resumeLeftMoveIndex = intent.getIntExtra("resume_left_move_index", 0)
        val resumeRightMoveIndex = intent.getIntExtra("resume_right_move_index", 0)
        val resumeCurrentPlayerTurn = intent.getIntExtra("resume_current_player_turn", 1)
        val resumeGamePhase = intent.getStringExtra("resume_game_phase") ?: "STARTING_DICE"
        
        // ✅ Result geri göndermek için callback fonksiyonu
        val sendResult = { resultData: Intent ->
            setResult(Activity.RESULT_OK, resultData)
            finish()
        }

        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color(0xFF1E1E1E)
                ) {
                    DiceScreen(
                        gameType = gameType,
                        player1Name = player1Name,
                        player2Name = player2Name,
                        matchLength = matchLength,
                        keepStatistics = keepStatistics,
                        useTimer = useTimer,
                        useSingleButtonForTimerAndDice = useSingleButtonForTimerAndDice,
                        useDiceRoller = useDiceRoller,
                        markDiceEvaluation = markDiceEvaluation,
                        dbHelper = dbHelper,
                        matchId = matchId,
                        player1Id = player1Id,
                        player2Id = player2Id,
                        doublingCubeValue = doublingCubeValue,
                        player1CanDouble = player1CanDouble,
                        player2CanDouble = player2CanDouble,
                        isCrawfordGame = isCrawfordGame,
                        showPlayer1DoublingMenu = showPlayer1DoublingMenu,
                        showPlayer2DoublingMenu = showPlayer2DoublingMenu,
                        doublingCubePosition = doublingCubePosition,
                        replayDiceSetId = replayDiceSetId,
                        resumeTotalMoveCount = resumeTotalMoveCount,
                        resumeLeftMoveIndex = resumeLeftMoveIndex,
                        resumeRightMoveIndex = resumeRightMoveIndex,
                        resumeCurrentPlayerTurn = resumeCurrentPlayerTurn,
                        resumeGamePhase = resumeGamePhase,
                        onBack = {
                            // ✅ Geri tuşuyla çıkışta da katlama verisini skorboard'a gönder
                            val resultIntent = Intent().apply {
                                putExtra("doubling_cube_value", doublingCubeValue)
                                putExtra("player1_can_double", player1CanDouble)
                                putExtra("player2_can_double", player2CanDouble)
                                putExtra("doubling_cube_position", doublingCubePosition)
                            }
                            setResult(Activity.RESULT_OK, resultIntent)
                            finish()
                        },
                        onDoublingResult = sendResult
                    )
                }
            }
        }
    }
}

@Composable
fun DiceScreen(
    gameType: String,
    player1Name: String,
    player2Name: String,
    matchLength: Int,
    keepStatistics: Boolean,
    useTimer: Boolean,
    useSingleButtonForTimerAndDice: Boolean,
    useDiceRoller: Boolean,
    markDiceEvaluation: Boolean,
    dbHelper: DatabaseHelper,
    matchId: Long,
    player1Id: Long,
    player2Id: Long,
    doublingCubeValue: Int = 1,
    player1CanDouble: Boolean = true,
    player2CanDouble: Boolean = true,
    isCrawfordGame: Boolean = false,
    showPlayer1DoublingMenu: Boolean = false,
    showPlayer2DoublingMenu: Boolean = false,
    doublingCubePosition: String = "CENTER",
    replayDiceSetId: String? = null,
    resumeTotalMoveCount: Int = 0,
    resumeLeftMoveIndex: Int = 0,
    resumeRightMoveIndex: Int = 0,
    resumeCurrentPlayerTurn: Int = 1,
    resumeGamePhase: String = "STARTING_DICE",
    onBack: () -> Unit,
    onDoublingResult: (Intent) -> Unit = {}
) {
    // ✅ ZAR EKRANI KATLAMA SİSTEMİ
    DiceScreenWithDoubling(
        gameType = gameType,
        player1Name = player1Name,
        player2Name = player2Name,
        matchLength = matchLength,
        keepStatistics = keepStatistics,
        useTimer = useTimer,
        useSingleButtonForTimerAndDice = useSingleButtonForTimerAndDice,
        useDiceRoller = useDiceRoller,
        markDiceEvaluation = markDiceEvaluation,
        dbHelper = dbHelper,
        matchId = matchId,
        player1Id = player1Id,
        player2Id = player2Id,
        doublingCubeValue = doublingCubeValue,
        player1CanDouble = player1CanDouble,
        player2CanDouble = player2CanDouble,
        isCrawfordGame = isCrawfordGame,
        showPlayer1DoublingMenu = showPlayer1DoublingMenu,
        showPlayer2DoublingMenu = showPlayer2DoublingMenu,
        doublingCubePosition = doublingCubePosition,
        replayDiceSetId = replayDiceSetId,
        resumeTotalMoveCount = resumeTotalMoveCount,
        resumeLeftMoveIndex = resumeLeftMoveIndex,
        resumeRightMoveIndex = resumeRightMoveIndex,
        resumeCurrentPlayerTurn = resumeCurrentPlayerTurn,
        resumeGamePhase = resumeGamePhase,
        onBack = onBack,
        onDoublingResult = onDoublingResult
    )
}

// ✅ KATLAMALI ZAR EKRANI
@Composable
fun DiceScreenWithDoubling(
    gameType: String,
    player1Name: String,
    player2Name: String,
    matchLength: Int,
    keepStatistics: Boolean,
    useTimer: Boolean,
    useSingleButtonForTimerAndDice: Boolean,
    useDiceRoller: Boolean,
    markDiceEvaluation: Boolean,
    dbHelper: DatabaseHelper,
    matchId: Long,
    player1Id: Long,
    player2Id: Long,
    doublingCubeValue: Int,
    player1CanDouble: Boolean,
    player2CanDouble: Boolean,
    isCrawfordGame: Boolean,
    showPlayer1DoublingMenu: Boolean,
    showPlayer2DoublingMenu: Boolean,
    doublingCubePosition: String,
    replayDiceSetId: String?,
    resumeTotalMoveCount: Int = 0,
    resumeLeftMoveIndex: Int = 0,
    resumeRightMoveIndex: Int = 0,
    resumeCurrentPlayerTurn: Int = 1,
    resumeGamePhase: String = "STARTING_DICE",
    onBack: () -> Unit,
    onDoublingResult: (Intent) -> Unit
) {
    val context = LocalContext.current
    var localDoublingCubeValue by remember { mutableIntStateOf(doublingCubeValue) }
    var localPlayer1CanDouble by remember { mutableStateOf(player1CanDouble) }
    var localPlayer2CanDouble by remember { mutableStateOf(player2CanDouble) }
    var localShowPlayer1Menu by remember { mutableStateOf(showPlayer1DoublingMenu) }
    var localShowPlayer2Menu by remember { mutableStateOf(showPlayer2DoublingMenu) }
    
    // ✅ REPLAY MOD - Eğer replay açıldıysa üst başlık göster
    val isReplayMode = replayDiceSetId != null
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF1E1E1E))
            .padding(8.dp)
    ) {
        // ✅ REPLAY MOD BAŞLIĞI
        if (isReplayMode) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFF4CAF50)
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "▶️ REPLAY MOD: $replayDiceSetId",
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
        
        // Üst kısım - Katlama kontrolleri (Modern tavla)
        if (gameType == "Modern") {
            // Crawford durumu göstergesi
            if (isCrawfordGame) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 4.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFF5722))
                ) {
                    Text(
                        text = "CRAWFORD ELİ - Katlama Devre Dışı",
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(8.dp).fillMaxWidth(),
                        textAlign = TextAlign.Center
                    )
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF2E2E2E), RoundedCornerShape(8.dp))
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Sol oyuncu katlama
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = player1Name,
                        color = if (localPlayer1CanDouble && !isCrawfordGame) Color(0xFF4CAF50) else Color.Gray,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    if (localShowPlayer1Menu && !isCrawfordGame) {
                        // Player2 katlama teklif etti, Player1 cevap verecek
                        Text(
                            text = "Katlama teklifi: x$localDoublingCubeValue",
                            color = Color(0xFFFFB300),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Button(
                                onClick = {
                                    val resultIntent = Intent().apply {
                                        putExtra("doubling_cube_value", localDoublingCubeValue)
                                        putExtra("player1_can_double", true)
                                        putExtra("player2_can_double", false)
                                        putExtra("doubling_cube_position", "PLAYER1_CONTROL")
                                        putExtra("accepted_player_id", player1Id)
                                    }
                                    onDoublingResult(resultIntent)
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50)),
                                modifier = Modifier.height(40.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp)
                            ) {
                                Text("KABUL", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                            Button(
                                onClick = {
                                    val resultIntent = Intent().apply {
                                        putExtra("doubling_cube_value", localDoublingCubeValue / 2)
                                        putExtra("player1_can_double", true)
                                        putExtra("player2_can_double", true)
                                        putExtra("doubling_cube_position", "CENTER")
                                        putExtra("resigned_player_id", player1Id)
                                    }
                                    onDoublingResult(resultIntent)
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F)),
                                modifier = Modifier.height(40.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp)
                            ) {
                                Text("PES", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                            Button(
                                onClick = {
                                    localShowPlayer1Menu = false
                                    localDoublingCubeValue /= 2
                                    localPlayer2CanDouble = true
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF757575)),
                                modifier = Modifier.height(40.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp)
                            ) {
                                Text("İPTAL", color = Color.White, fontSize = 11.sp)
                            }
                        }
                    } else if (localPlayer1CanDouble && !isCrawfordGame) {
                        Button(
                            onClick = {
                                if (!isReplayMode) {
                                    localDoublingCubeValue *= 2
                                    localPlayer1CanDouble = false
                                    localShowPlayer2Menu = true
                                    Toast.makeText(context, "$player1Name katlama teklifi: x$localDoublingCubeValue", Toast.LENGTH_SHORT).show()
                                } else {
                                    Toast.makeText(context, "Replay modunda katlama yapılamaz", Toast.LENGTH_SHORT).show()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF8F00)),
                            modifier = Modifier.height(42.dp).fillMaxWidth(0.9f),
                            contentPadding = PaddingValues(horizontal = 16.dp)
                        ) {
                            Text("KATLA", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        }
                    } else {
                        Text(
                            text = if (isCrawfordGame) "Pasif" else if (!localPlayer1CanDouble && !localShowPlayer2Menu) "Küp karşıda" else "",
                            color = Color.Gray,
                            fontSize = 11.sp
                        )
                    }
                }

                // Orta - Küp değeri ve sahibi
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .padding(horizontal = 8.dp)
                        .offset(y = (-15).dp)
                ) {
                    // Küp sahibi göstergesi
                    val cubeOwnerText = when {
                        localPlayer1CanDouble && localPlayer2CanDouble -> "Ortada"
                        localPlayer1CanDouble -> player1Name
                        else -> player2Name
                    }
                    Text(
                        text = cubeOwnerText,
                        color = Color(0xFFBBBBBB),
                        fontSize = 10.sp
                    )
                    Box(
                        modifier = Modifier
                            .size(55.dp)
                            .background(Color.White, RoundedCornerShape(8.dp))
                            .border(3.dp, if (isCrawfordGame) Color.Red else Color.Black, RoundedCornerShape(8.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = localDoublingCubeValue.toString(),
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isCrawfordGame) Color.Red else Color.Black
                        )
                    }
                }

                // Sağ oyuncu katlama
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = player2Name,
                        color = if (localPlayer2CanDouble && !isCrawfordGame) Color(0xFF4CAF50) else Color.Gray,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    if (localShowPlayer2Menu && !isCrawfordGame) {
                        // Player1 katlama teklif etti, Player2 cevap verecek
                        Text(
                            text = "Katlama teklifi: x$localDoublingCubeValue",
                            color = Color(0xFFFFB300),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Button(
                                onClick = {
                                    val resultIntent = Intent().apply {
                                        putExtra("doubling_cube_value", localDoublingCubeValue)
                                        putExtra("player1_can_double", false)
                                        putExtra("player2_can_double", true)
                                        putExtra("doubling_cube_position", "PLAYER2_CONTROL")
                                        putExtra("accepted_player_id", player2Id)
                                    }
                                    onDoublingResult(resultIntent)
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50)),
                                modifier = Modifier.height(40.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp)
                            ) {
                                Text("KABUL", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                            Button(
                                onClick = {
                                    val resultIntent = Intent().apply {
                                        putExtra("doubling_cube_value", localDoublingCubeValue / 2)
                                        putExtra("player1_can_double", true)
                                        putExtra("player2_can_double", true)
                                        putExtra("doubling_cube_position", "CENTER")
                                        putExtra("resigned_player_id", player2Id)
                                    }
                                    onDoublingResult(resultIntent)
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F)),
                                modifier = Modifier.height(40.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp)
                            ) {
                                Text("PES", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                            Button(
                                onClick = {
                                    localShowPlayer2Menu = false
                                    localDoublingCubeValue /= 2
                                    localPlayer1CanDouble = true
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF757575)),
                                modifier = Modifier.height(40.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp)
                            ) {
                                Text("İPTAL", color = Color.White, fontSize = 11.sp)
                            }
                        }
                    } else if (localPlayer2CanDouble && !isCrawfordGame) {
                        Button(
                            onClick = {
                                if (!isReplayMode) {
                                    localDoublingCubeValue *= 2
                                    localPlayer2CanDouble = false
                                    localShowPlayer1Menu = true
                                    Toast.makeText(context, "$player2Name katlama teklifi: x$localDoublingCubeValue", Toast.LENGTH_SHORT).show()
                                } else {
                                    Toast.makeText(context, "Replay modunda katlama yapılamaz", Toast.LENGTH_SHORT).show()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF8F00)),
                            modifier = Modifier.height(42.dp).fillMaxWidth(0.9f),
                            contentPadding = PaddingValues(horizontal = 16.dp)
                        ) {
                            Text("KATLA", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        }
                    } else {
                        Text(
                            text = if (isCrawfordGame) "Pasif" else if (!localPlayer2CanDouble && !localShowPlayer1Menu) "Küp karşıda" else "",
                            color = Color.Gray,
                            fontSize = 11.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))
        }
        
        // Alt kısım - Esas zar ekranı ve kapat butonu
        Box(modifier = Modifier.weight(1f)) {
            SimpleIntegratedScreen(
                gameType = gameType,
                player1Name = player1Name,
                player2Name = player2Name,
                matchLength = matchLength,
                keepStatistics = keepStatistics,
                useTimer = useTimer,
                useSingleButtonForTimerAndDice = useSingleButtonForTimerAndDice,
                useDiceRoller = useDiceRoller,
                markDiceEvaluation = markDiceEvaluation,
                dbHelper = dbHelper,
                matchId = matchId,
                player1Id = player1Id,
                player2Id = player2Id,
                onBack = {
                    // ✅ GERİ butonuyla çıkışta da katlama verisini skorboard'a gönder
                    val resultIntent = Intent().apply {
                        putExtra("doubling_cube_value", localDoublingCubeValue)
                        putExtra("player1_can_double", localPlayer1CanDouble)
                        putExtra("player2_can_double", localPlayer2CanDouble)
                        putExtra("doubling_cube_position", when {
                            localPlayer1CanDouble && localPlayer2CanDouble -> "CENTER"
                            localPlayer1CanDouble -> "PLAYER1_CONTROL"
                            else -> "PLAYER2_CONTROL"
                        })
                    }
                    onDoublingResult(resultIntent)
                }
            )
            
            // KAPAT butonu - sağ alt köşe
            Button(
                onClick = {
                    if (!isReplayMode) {
                        // ✅ Normal kapatış - değişiklik yoksa sadece kapat
                        val resultIntent = Intent().apply {
                            putExtra("doubling_cube_value", localDoublingCubeValue)
                            putExtra("player1_can_double", localPlayer1CanDouble)
                            putExtra("player2_can_double", localPlayer2CanDouble)
                            putExtra("doubling_cube_position", when {
                                localPlayer1CanDouble && localPlayer2CanDouble -> "CENTER"
                                localPlayer1CanDouble -> "PLAYER1_CONTROL"
                                else -> "PLAYER2_CONTROL"
                            })
                        }
                        onDoublingResult(resultIntent)
                    } else {
                        // ✅ Replay modunda sadece kapat (değişiklik yok)
                        onBack()
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF616161)),
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp)
                    .size(width = 80.dp, height = 40.dp)
            ) {
                Text("KAPAT", color = Color.White, fontSize = 12.sp)
            }
        }
    }
}