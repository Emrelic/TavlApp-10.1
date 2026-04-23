package com.tavla.tavlapp

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import java.text.SimpleDateFormat
import java.util.Locale
import android.view.View
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// Katlama zarı konumlarını temsil eden enum
enum class DoublingCubePosition {
    CENTER,          // Merkez (başlangıç pozisyonu)
    PLAYER1_OFFER,   // Oyuncu 1'in teklif bölgesi
    PLAYER1_CONTROL, // Oyuncu 1'in kontrol bölgesi
    PLAYER2_OFFER,   // Oyuncu 2'nin teklif bölgesi
    PLAYER2_CONTROL  // Oyuncu 2'nin kontrol bölgesi
}

private fun formatDisplayDate(dbDate: String): String {
    return try {
        val inputFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        val outputFormat = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())
        val date = inputFormat.parse(dbDate)
        if (date != null) outputFormat.format(date) else dbDate
    } catch (e: Exception) {
        dbDate
    }
}

class GameScoreActivity : ComponentActivity() {
    private lateinit var dbHelper: DatabaseHelper
    private var matchId: Long = -1

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

        dbHelper = DatabaseHelper(this)

        // Intent'ten oyuncu bilgilerini ve oyun bilgilerini alıyoruz
        val player1Name = intent.getStringExtra("player1_name") ?: "Oyuncu 1"
        val player2Name = intent.getStringExtra("player2_name") ?: "Oyuncu 2"
        val player1Id = intent.getLongExtra("player1_id", -1)
        val player2Id = intent.getLongExtra("player2_id", -1)
        val gameType = intent.getStringExtra("game_type") ?: "Modern"
        val targetRounds = intent.getIntExtra("rounds", 11)
        val isScoreAutomatic = intent.getBooleanExtra("is_score_automatic", true)
        val useDiceRoller = intent.getBooleanExtra("use_dice_roller", false)
        val useTimer = intent.getBooleanExtra("use_timer", false)
        val keepStatistics = intent.getBooleanExtra("keep_statistics", false)
        val markDiceEvaluation = intent.getBooleanExtra("mark_dice_evaluation", false)
        val processPartialDice = intent.getBooleanExtra("process_partial_dice", false)

        // Rövanşlı karşılaşma modu
        val isRematchMode = intent.getBooleanExtra("is_rematch_mode", false)
        val encounterId = intent.getLongExtra("encounter_id", -1L)
        val totalParties = intent.getIntExtra("total_parties", 100)

        // Rövanşlı modda normal maç kaydı oluşturma
        if (!isRematchMode) {
            matchId = dbHelper.startNewMatch(player1Id, player2Id, gameType, targetRounds)

            // Zar istatistikleri tutuluyorsa, initialize et
            if (keepStatistics) {
                dbHelper.initializeDiceStats(matchId, player1Id)
                dbHelper.initializeDiceStats(matchId, player2Id)
            }
        }

        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    GameScreen(
                        player1Name = player1Name,
                        player2Name = player2Name,
                        player1Id = player1Id,
                        player2Id = player2Id,
                        gameType = gameType,
                        targetRounds = targetRounds,
                        isScoreAutomatic = isScoreAutomatic,
                        useDiceRoller = useDiceRoller,
                        useTimer = useTimer,
                        keepStatistics = keepStatistics,
                        markDiceEvaluation = markDiceEvaluation,
                        processPartialDice = processPartialDice,
                        matchId = matchId,
                        dbHelper = dbHelper,
                        onFinish = { this.finish() },
                        isRematchMode = isRematchMode,
                        encounterId = encounterId,
                        totalParties = totalParties
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GameScreen(
    player1Name: String,
    player2Name: String,
    player1Id: Long,
    player2Id: Long,
    gameType: String,
    targetRounds: Int,
    isScoreAutomatic: Boolean,
    useDiceRoller: Boolean,
    useTimer: Boolean,
    keepStatistics: Boolean,
    markDiceEvaluation: Boolean,
    processPartialDice: Boolean,
    matchId: Long,
    dbHelper: DatabaseHelper,
    onFinish: () -> Unit,
    isRematchMode: Boolean = false,
    encounterId: Long = -1L,
    totalParties: Int = 100
) {
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.screenWidthDp > configuration.screenHeightDp

    // Geleneksel tavla kontrolü
    val isTraditionalGame = gameType == "Geleneksel"
    // ✅ RECOMPOSE TRİGGER EKLE - Bu satırı ekle
    var recomposeKey by remember { mutableIntStateOf(0) }

    // Oyun durumu
    var player1Score by remember { mutableStateOf(0) }
    var player2Score by remember { mutableStateOf(0) }
    var currentRound by remember { mutableStateOf(0) }
    var player1RoundsWon by remember { mutableStateOf(0) }
    var player2RoundsWon by remember { mutableStateOf(0) }
    var showMatchEndDialog by remember { mutableStateOf(false) }
    var winnerName by remember { mutableStateOf("") }
    var winnerScore by remember { mutableStateOf(0) }
    var loserName by remember { mutableStateOf("") }
    var loserScore by remember { mutableStateOf(0) }

    // ✅ YENİ EKLENEN: UNDO STACK
    var undoStack by remember { mutableStateOf(listOf<Long>()) }

    // Katlama zarı durumu
    var doublingCubeValue by remember { mutableIntStateOf(1) }
    var doublingCubePosition by remember { mutableStateOf(DoublingCubePosition.CENTER) }

    // Tekliften önceki küp değeri (iptal durumunda geri dönmek için)
    var previousDoublingCubeValue by remember { mutableIntStateOf(1) }
    var previousDoublingCubePosition by remember { mutableStateOf(DoublingCubePosition.CENTER) }

    // Katlama menüsü durumu
    var showPlayer1DoublingMenu by remember { mutableStateOf(false) }
    var showPlayer2DoublingMenu by remember { mutableStateOf(false) }

    // Hangi oyuncuların katlamaya izin verildiği
    var player1CanDouble by remember { mutableStateOf(true) }
    var player2CanDouble by remember { mutableStateOf(true) }

    // Crawford kuralı için yeni değişkenler
    var matchTargetScore by remember { mutableIntStateOf(targetRounds) } // Parti hedef puanı (ayarlardan)
    var isCrawfordGame by remember { mutableStateOf(false) } // Şu an Crawford eli mi?
    var crawfordGamePlayed by remember { mutableStateOf(false) } // Crawford eli daha önce oynanmış mı?
    var isPostCrawford by remember { mutableStateOf(false) } // Post-Crawford durumu mu?

    var showEndMatchConfirmation by remember { mutableStateOf(false) }
    var showActivityLogDialog by remember { mutableStateOf(false) }
    
    // ✅ ZAR SETİ TAKİP SİSTEMİ
    var currentDiceSetNumber by remember { mutableIntStateOf(1) }
    var currentPartNumber by remember { mutableIntStateOf(1) }
    var currentGameNumber by remember { mutableIntStateOf(1) }
    var diceSetHistory by remember { mutableStateOf(mutableListOf<String>()) } // P1-O1-S1 formatında
    var diceSetGameStates by remember { mutableStateOf(mutableMapOf<String, DiceSetGameState>()) } // Hamle pozisyonları
    
    // ✅ ZAR SETİ REPLAY SİSTEMİ
    var showDiceSetHistoryDialog by remember { mutableStateOf(false) }
    
    // ✅ TAM GERİ ALMA SİSTEMİ - GameState Snapshot
    data class GameStateSnapshot(
        val player1Score: Int,
        val player2Score: Int,
        val currentRound: Int,
        val player1RoundsWon: Int,
        val player2RoundsWon: Int,
        val doublingCubeValue: Int,
        val doublingCubePosition: DoublingCubePosition,
        val player1CanDouble: Boolean,
        val player2CanDouble: Boolean,
        val isCrawfordGame: Boolean,
        val crawfordGamePlayed: Boolean,
        val isPostCrawford: Boolean,
        val currentDiceSetNumber: Int,
        val diceSetHistory: List<String>,
        val timestamp: Long = System.currentTimeMillis()
    )
    
    var gameStateHistory by remember { mutableStateOf(mutableListOf<GameStateSnapshot>()) }
    
    // ✅ ZAR EKRANI RESULT LAUNCHER - Katlama sonuçlarını al
    val diceActivityLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val data = result.data
            val doublingResult = data?.getIntExtra("doubling_cube_value", doublingCubeValue) ?: doublingCubeValue
            val player1CanDoubleResult = data?.getBooleanExtra("player1_can_double", player1CanDouble) ?: player1CanDouble
            val player2CanDoubleResult = data?.getBooleanExtra("player2_can_double", player2CanDouble) ?: player2CanDouble
            val doublingPositionResult = data?.getStringExtra("doubling_cube_position") ?: doublingCubePosition.name
            val acceptedPlayerId = data?.getLongExtra("accepted_player_id", -1L) ?: -1L
            val resignedPlayerId = data?.getLongExtra("resigned_player_id", -1L) ?: -1L
            
            // ✅ El bitimi popup verileri
            val gameEnded = data?.getBooleanExtra("game_ended", false) ?: false
            val winnerIsLeft = data?.getBooleanExtra("winner_is_left", false) ?: false
            val scorePoints = data?.getIntExtra("score_points", 0) ?: 0
            val scoreType = data?.getStringExtra("score_type") ?: ""
            
            // ✅ Hamle pozisyonu verileri
            val totalMoveCount = data?.getIntExtra("total_move_count", 0) ?: 0
            val leftMoveIndex = data?.getIntExtra("left_move_index", 0) ?: 0
            val rightMoveIndex = data?.getIntExtra("right_move_index", 0) ?: 0
            val currentPlayerTurn = data?.getIntExtra("current_player_turn", 1) ?: 1
            val gamePhase = data?.getStringExtra("game_phase") ?: "STARTING_DICE"
            val playedSetId = data?.getStringExtra("played_set_id") ?: ""
            
            // ✅ Skorboard'u güncelle
            doublingCubeValue = doublingResult
            player1CanDouble = player1CanDoubleResult
            player2CanDouble = player2CanDoubleResult
            
            // Pozisyon güncellemesi
            doublingCubePosition = when (doublingPositionResult) {
                "PLAYER1_CONTROL" -> DoublingCubePosition.PLAYER1_CONTROL
                "PLAYER2_CONTROL" -> DoublingCubePosition.PLAYER2_CONTROL
                "PLAYER1_OFFER" -> DoublingCubePosition.PLAYER1_OFFER
                "PLAYER2_OFFER" -> DoublingCubePosition.PLAYER2_OFFER
                else -> DoublingCubePosition.CENTER
            }
            
            // ✅ El bitimi popup'tan tam kapsamlı skor işleme
            if (gameEnded && scorePoints > 0) {
                val winnerName: String
                val winnerId: Long
                val winnerScore: Int
                val loserScore: Int
                
                if (winnerIsLeft) {
                    // Sol oyuncu kazandı
                    winnerName = player1Name
                    winnerId = player1Id
                    player1Score += scorePoints
                    winnerScore = player1Score
                    loserScore = player2Score
                } else {
                    // Sağ oyuncu kazandı
                    winnerName = player2Name
                    winnerId = player2Id
                    player2Score += scorePoints
                    winnerScore = player2Score
                    loserScore = if (winnerIsLeft) player2Score else player1Score
                }
                
                // ✅ 1. Skorboard güncelleme simülasyonu (popup'tan gelen veri işleme)
                currentDiceSetNumber++ // Yeni el için zar seti arttır
                val setId = "P1-O${currentRound + 1}-S$currentDiceSetNumber"
                if (!diceSetHistory.contains(setId)) {
                    diceSetHistory.add(setId)
                }
                
                // ✅ 2. Activity Log - popup işlemi kaydı
                val actionType = when (scoreType) {
                    "SINGLE" -> ActionTypes.SCORE_SINGLE
                    "MARS" -> ActionTypes.SCORE_MARS
                    "BACKGAMMON" -> ActionTypes.SCORE_BACKGAMMON
                    else -> ActionTypes.SCORE_SINGLE
                }
                
                dbHelper.addActivityLog(
                    actionType = actionType,
                    description = "El bitimi popup: $winnerName kazandı ($scoreType, $setId, +$scorePoints puan)",
                    player1Name = player1Name,
                    player2Name = player2Name,
                    matchId = if (isRematchMode) -1 else matchId,
                    extraData = "score_source=popup,dice_set=$setId,doubling_cube=$doublingCubeValue,win_type=$scoreType"
                )
                
                // ✅ 3. Kullanıcı bildirimi
                Toast.makeText(context, "🎲 $winnerName kazandı: $scoreType (+$scorePoints puan) • Set: $setId", Toast.LENGTH_LONG).show()
                
                // ✅ 4. Parti bitişi kontrol et
                if (player1Score >= targetRounds || player2Score >= targetRounds) {
                    val winner = if (player1Score >= targetRounds) player1Name else player2Name
                    Toast.makeText(context, "🏆 Parti bitti! $winner $targetRounds puana ulaştı!", Toast.LENGTH_LONG).show()
                }
                
                // ✅ 8. Katlama zarını sıfırla (yeni el için)
                doublingCubeValue = 1
                doublingCubePosition = DoublingCubePosition.CENTER
                player1CanDouble = true
                player2CanDouble = true
            }
            
            // ✅ Hamle pozisyonu kaydetme (el tamamlanmadıysa)
            if (!gameEnded && playedSetId.isNotEmpty() && totalMoveCount > 0) {
                val gameState = DiceSetGameState(
                    setId = playedSetId,
                    totalMoveCount = totalMoveCount,
                    leftMoveIndex = leftMoveIndex,
                    rightMoveIndex = rightMoveIndex,
                    currentPlayerTurn = currentPlayerTurn,
                    gamePhase = gamePhase,
                    lastPlayedDate = System.currentTimeMillis(),
                    isCompleted = false
                )
                diceSetGameStates[playedSetId] = gameState
                
                // Activity log kaydet
                dbHelper.addActivityLog(
                    actionType = ActionTypes.DICE_SCREEN_CLOSE,
                    description = "Zar ekranından çıkıldı: $playedSetId hamle $totalMoveCount'de kaldı",
                    player1Name = player1Name,
                    player2Name = player2Name,
                    matchId = if (isRematchMode) -1 else matchId,
                    extraData = "set_id=$playedSetId,total_moves=$totalMoveCount,left_moves=$leftMoveIndex,right_moves=$rightMoveIndex,current_player=$currentPlayerTurn,phase=$gamePhase"
                )
            }
            
            // PES durumunda otomatik skor ekleme
            if (resignedPlayerId != -1L) {
                val winnerName = if (resignedPlayerId == player1Id) player2Name else player1Name
                val winnerId = if (resignedPlayerId == player1Id) player2Id else player1Id
                
                // Pes eden oyuncu kaybeder, karşı oyuncu katlama zarı değeri kadar puan alır
                val scorePoints = doublingCubeValue
                
                if (resignedPlayerId == player1Id) {
                    // Player1 pes etti, Player2 kazandı
                    player2Score += scorePoints
                } else {
                    // Player2 pes etti, Player1 kazandı
                    player1Score += scorePoints
                }
                
                // Activity log ekle
                dbHelper.addActivityLog(
                    actionType = ActionTypes.DOUBLE_REJECT,
                    description = "Zar ekranında pes: $winnerName kazandı (+$scorePoints puan)",
                    player1Name = player1Name,
                    player2Name = player2Name,
                    matchId = if (isRematchMode) -1 else matchId
                )
                
                Toast.makeText(context, "Zar ekranında pes edildi: $winnerName kazandı (+$scorePoints puan)", Toast.LENGTH_SHORT).show()
                
                // Parti bitişi kontrolü
                if (player1Score >= targetRounds || player2Score >= targetRounds) {
                    val winner = if (player1Score >= targetRounds) player1Name else player2Name
                    Toast.makeText(context, "🏆 Parti bitti! $winner $targetRounds puana ulaştı!", Toast.LENGTH_LONG).show()
                }
                
                // Katlama zarını sıfırla
                doublingCubeValue = 1
                doublingCubePosition = DoublingCubePosition.CENTER
                player1CanDouble = true
                player2CanDouble = true
            }
            
            if (acceptedPlayerId != -1L) {
                val accepterName = if (acceptedPlayerId == player1Id) player1Name else player2Name
                Toast.makeText(context, "Zar ekranında katlama kabul edildi: $accepterName (x$doublingCubeValue)", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // ✅ RÖVANŞ ZAR EKRANI RESULT LAUNCHER - Katlama sonuçlarını al
    val rematchDiceLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val data = result.data
            val doublingResult = data?.getIntExtra("doubling_cube_value", doublingCubeValue) ?: doublingCubeValue
            val player1CanDoubleResult = data?.getBooleanExtra("player1_can_double", player1CanDouble) ?: player1CanDouble
            val player2CanDoubleResult = data?.getBooleanExtra("player2_can_double", player2CanDouble) ?: player2CanDouble
            val doublingPositionResult = data?.getStringExtra("doubling_cube_position") ?: doublingCubePosition.name
            val acceptedPlayerId = data?.getLongExtra("accepted_player_id", -1L) ?: -1L
            val resignedPlayerId = data?.getLongExtra("resigned_player_id", -1L) ?: -1L
            
            // ✅ Rövanş el bitimi popup verileri
            val gameEnded = data?.getBooleanExtra("game_ended", false) ?: false
            val winnerIsLeft = data?.getBooleanExtra("winner_is_left", false) ?: false
            val scorePoints = data?.getIntExtra("score_points", 0) ?: 0
            val scoreType = data?.getStringExtra("score_type") ?: ""
            
            // ✅ Rövanş hamle pozisyonu verileri
            val totalMoveCount = data?.getIntExtra("total_move_count", 0) ?: 0
            val leftMoveIndex = data?.getIntExtra("left_move_index", 0) ?: 0
            val rightMoveIndex = data?.getIntExtra("right_move_index", 0) ?: 0
            val currentPlayerTurn = data?.getIntExtra("current_player_turn", 1) ?: 1
            val gamePhase = data?.getStringExtra("game_phase") ?: "STARTING_DICE"
            val playedSetId = data?.getStringExtra("played_set_id") ?: ""

            // Skorboard'u güncelle
            doublingCubeValue = doublingResult
            player1CanDouble = player1CanDoubleResult
            player2CanDouble = player2CanDoubleResult

            doublingCubePosition = when (doublingPositionResult) {
                "PLAYER1_CONTROL" -> DoublingCubePosition.PLAYER1_CONTROL
                "PLAYER2_CONTROL" -> DoublingCubePosition.PLAYER2_CONTROL
                "PLAYER1_OFFER" -> DoublingCubePosition.PLAYER1_OFFER
                "PLAYER2_OFFER" -> DoublingCubePosition.PLAYER2_OFFER
                else -> DoublingCubePosition.CENTER
            }
            
            // ✅ Rövanş el bitimi popup'tan tam kapsamlı skor işleme  
            if (gameEnded && scorePoints > 0) {
                val winnerName: String
                val winnerId: Long
                
                if (winnerIsLeft) {
                    // Sol oyuncu kazandı
                    winnerName = player1Name
                    winnerId = player1Id
                    player1Score += scorePoints
                } else {
                    // Sağ oyuncu kazandı
                    winnerName = player2Name
                    winnerId = player2Id
                    player2Score += scorePoints
                }
                
                // ✅ 1. Rövanş skorboard güncelleme simülasyonu (popup'tan gelen veri işleme)
                currentDiceSetNumber++ // Yeni el için zar seti arttır
                val setId = "P1-O${currentRound + 1}-S$currentDiceSetNumber"
                if (!diceSetHistory.contains(setId)) {
                    diceSetHistory.add(setId)
                }
                
                // ✅ 2. Rövanş Activity Log
                val actionType = when (scoreType) {
                    "SINGLE" -> ActionTypes.SCORE_SINGLE
                    "MARS" -> ActionTypes.SCORE_MARS
                    "BACKGAMMON" -> ActionTypes.SCORE_BACKGAMMON
                    else -> ActionTypes.SCORE_SINGLE
                }
                
                dbHelper.addActivityLog(
                    actionType = actionType,
                    description = "Rövanş el bitimi popup: $winnerName kazandı ($scoreType, $setId, +$scorePoints puan)",
                    player1Name = player1Name,
                    player2Name = player2Name,
                    matchId = -1L, // Rövanş modu için -1
                    extraData = "score_source=rematch_popup,dice_set=$setId,doubling_cube=$doublingCubeValue,win_type=$scoreType"
                )
                
                // ✅ 3. Kullanıcı bildirimi
                Toast.makeText(context, "🎯 Rövanş: $winnerName kazandı ($scoreType, +$scorePoints) • Set: $setId", Toast.LENGTH_LONG).show()
                
                // ✅ 4. Rövanş parti bitişi kontrol
                if (player1Score >= targetRounds || player2Score >= targetRounds) {
                    val winner = if (player1Score >= targetRounds) player1Name else player2Name
                    Toast.makeText(context, "🏆 Rövanş Parti bitti! $winner $targetRounds puana ulaştı!", Toast.LENGTH_LONG).show()
                }
                
                // ✅ 8. Katlama zarını sıfırla
                doublingCubeValue = 1
                doublingCubePosition = DoublingCubePosition.CENTER
                player1CanDouble = true
                player2CanDouble = true
            }
            
            // ✅ Rövanş hamle pozisyonu kaydetme (el tamamlanmadıysa)
            if (!gameEnded && playedSetId.isNotEmpty() && totalMoveCount > 0) {
                val gameState = DiceSetGameState(
                    setId = playedSetId,
                    totalMoveCount = totalMoveCount,
                    leftMoveIndex = leftMoveIndex,
                    rightMoveIndex = rightMoveIndex,
                    currentPlayerTurn = currentPlayerTurn,
                    gamePhase = gamePhase,
                    lastPlayedDate = System.currentTimeMillis(),
                    isCompleted = false
                )
                diceSetGameStates[playedSetId] = gameState
                
                // Activity log kaydet
                dbHelper.addActivityLog(
                    actionType = ActionTypes.DICE_SCREEN_CLOSE,
                    description = "Rövanş zar ekranından çıkıldı: $playedSetId hamle $totalMoveCount'de kaldı",
                    player1Name = player1Name,
                    player2Name = player2Name,
                    matchId = -1L,
                    extraData = "set_id=$playedSetId,total_moves=$totalMoveCount,left_moves=$leftMoveIndex,right_moves=$rightMoveIndex,current_player=$currentPlayerTurn,phase=$gamePhase"
                )
            }

            // Rövanş modunda PES durumunda otomatik skor ekleme
            if (resignedPlayerId != -1L) {
                val winnerName = if (resignedPlayerId == player1Id) player2Name else player1Name
                val winnerId = if (resignedPlayerId == player1Id) player2Id else player1Id
                
                // Pes eden oyuncu kaybeder, karşı oyuncu katlama zarı değeri kadar puan alır
                val scorePoints = doublingCubeValue
                
                if (resignedPlayerId == player1Id) {
                    // Player1 pes etti, Player2 kazandı
                    player2Score += scorePoints
                } else {
                    // Player2 pes etti, Player1 kazandı
                    player1Score += scorePoints
                }
                
                // Activity log ekle
                dbHelper.addActivityLog(
                    actionType = ActionTypes.DOUBLE_REJECT,
                    description = "Rövanş zar ekranında pes: $winnerName kazandı (+$scorePoints puan)",
                    player1Name = player1Name,
                    player2Name = player2Name,
                    matchId = if (isRematchMode) -1 else matchId
                )
                
                Toast.makeText(context, "Pes edildi: $winnerName kazandı (+$scorePoints puan)", Toast.LENGTH_SHORT).show()
                
                // Parti bitişi kontrolü (rövanş modunda)
                if (player1Score >= targetRounds || player2Score >= targetRounds) {
                    val winner = if (player1Score >= targetRounds) player1Name else player2Name
                    Toast.makeText(context, "🏆 Parti bitti! $winner $targetRounds puana ulaştı!", Toast.LENGTH_LONG).show()
                }
                
                // Katlama zarını sıfırla
                doublingCubeValue = 1
                doublingCubePosition = DoublingCubePosition.CENTER
                player1CanDouble = true
                player2CanDouble = true
            }

            if (acceptedPlayerId != -1L) {
                val accepterName = if (acceptedPlayerId == player1Id) player1Name else player2Name
                Toast.makeText(context, "Katlama kabul: $accepterName (x$doublingCubeValue)", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // ✅ Maç bitirici skor onay sistemi
    var showMatchWinConfirmation by remember { mutableStateOf(false) }
    var pendingWinnerId by remember { mutableStateOf(-1L) }
    var pendingWinnerName by remember { mutableStateOf("") }
    var pendingWinType by remember { mutableStateOf("") }
    var pendingScore by remember { mutableStateOf(0) }
    var pendingFinalPlayer1Score by remember { mutableStateOf(0) }
    var pendingFinalPlayer2Score by remember { mutableStateOf(0) }

    // Zar atma ekranı state'i
    var showDiceScreen by remember { mutableStateOf(false) }

    // === RÖVANŞLI KARŞILAŞMA STATE ===
    var rematchEncounter by remember { mutableStateOf<RematchEncounter?>(null) }
    var rematchCurrentRound by remember { mutableIntStateOf(1) }
    var rematchPartyIndex by remember { mutableIntStateOf(0) }
    var rematchGameIndex by remember { mutableIntStateOf(0) }
    var rematchDicePairsUsed by remember { mutableIntStateOf(0) }
    var rematchLastDoublerPlayerId by remember { mutableStateOf<Long?>(null) }
    var rematchLeftDiceTotal by remember { mutableIntStateOf(0) }
    var rematchRightDiceTotal by remember { mutableIntStateOf(0) }
    var rematchLeftDoublesCount by remember { mutableIntStateOf(0) }
    var rematchRightDoublesCount by remember { mutableIntStateOf(0) }
    var rematchUndoStack by remember { mutableStateOf(listOf<Long>()) }

    // Pip input dialog
    var showPipInputDialog by remember { mutableStateOf(false) }
    var pendingRematchWinnerId by remember { mutableStateOf(-1L) }
    var pendingRematchWinnerName by remember { mutableStateOf("") }
    var pendingRematchWinType by remember { mutableStateOf("") }
    var pendingRematchBaseScore by remember { mutableIntStateOf(0) }
    var pipCountInput by remember { mutableStateOf("") }

    // Oyun kimlik bilgileri
    var gameDisplayId by remember { mutableStateOf("") }
    var gameStartDate by remember { mutableStateOf("") }

    // Parti/Tur/Karşılaşma geçiş dialogları
    var showPartyEndDialog by remember { mutableStateOf(false) }
    var showRoundEndDialog by remember { mutableStateOf(false) }
    var showEncounterEndDialog by remember { mutableStateOf(false) }
    var partyEndInfo by remember { mutableStateOf("") }

    // Otomatik zar ekranı açma ve mevcut zar setini initialize et
    LaunchedEffect(useDiceRoller, useTimer) {
        if (useDiceRoller || useTimer) {
            showDiceScreen = true
        }
        // ✅ Oyun başladığında mevcut seti ekle
        val currentSetId = if (isRematchMode) {
            "P${rematchPartyIndex + 1}-O${currentRound + 1}-S$currentDiceSetNumber"
        } else {
            "P$currentPartNumber-O${currentRound + 1}-S$currentDiceSetNumber"
        }
        if (!diceSetHistory.contains(currentSetId)) {
            diceSetHistory.add(currentSetId)
        }
    }

    // ✅ RECOMPOSE ETKİSİ - LaunchedEffect ekle
    LaunchedEffect(recomposeKey) {
        // Bu blok recomposeKey değiştiğinde çalışır ve UI'ı günceller
    }

    // === RÖVANŞLI KARŞILAŞMA: Encounter state yükleme ===
    LaunchedEffect(isRematchMode, encounterId, recomposeKey) {
        if (isRematchMode && encounterId != -1L) {
            val encounter = dbHelper.getRematchEncounter(encounterId)
            if (encounter != null) {
                rematchEncounter = encounter
                rematchCurrentRound = encounter.currentRound
                rematchPartyIndex = encounter.currentPartyIndex
                rematchGameIndex = encounter.currentGameIndex
                matchTargetScore = targetRounds
                gameDisplayId = "R-${encounter.id}"
                gameStartDate = formatDisplayDate(encounter.createdDate)

                // Mevcut parti skorlarını yükle
                val (p1Score, p2Score) = dbHelper.getPartyScore(
                    encounterId, encounter.currentPartyIndex, encounter.currentRound
                )
                player1Score = p1Score
                player2Score = p2Score

                // Mevcut partideki oyun sayısını bul
                val gamesInParty = dbHelper.getRematchGameResults(
                    encounterId, roundNumber = encounter.currentRound, partyIndex = encounter.currentPartyIndex
                )
                currentRound = gamesInParty.size

                // SharedPreferences'dan zar verilerini oku
                val prefs = context.getSharedPreferences("rematch_prefs", android.content.Context.MODE_PRIVATE)
                rematchDicePairsUsed = prefs.getInt("dice_pairs_used_${encounterId}", 0)
                rematchLeftDiceTotal = prefs.getInt("left_dice_total_${encounterId}", 0)
                rematchRightDiceTotal = prefs.getInt("right_dice_total_${encounterId}", 0)
                rematchLeftDoublesCount = prefs.getInt("left_doubles_count_${encounterId}", 0)
                rematchRightDoublesCount = prefs.getInt("right_doubles_count_${encounterId}", 0)
            }
        }
    }

    // === NORMAL MOD: Oyun kimlik bilgileri yükleme ===
    LaunchedEffect(isRematchMode, matchId) {
        if (!isRematchMode && matchId != -1L) {
            val match = dbHelper.getMatchDetails(matchId)
            if (match != null) {
                gameDisplayId = "M-$matchId"
                gameStartDate = formatDisplayDate(match.date)
            }
        }
    }

    // Crawford durumunu kontrol eden fonksiyon - El bitiş sayısından bir önceki sayıya gelince
    fun checkCrawfordStatus() {
        if (!crawfordGamePlayed && !isCrawfordGame) {
            // Crawford eli henüz oynanmamışsa kontrol et
            if (player1Score == targetRounds - 1 || player2Score == targetRounds - 1) {
                // Birisi hedef puanın 1 eksiğine ulaştı, Crawford eli başlar
                isCrawfordGame = true
                
                // Crawford elinde katlama butonları devre dışı
                player1CanDouble = false
                player2CanDouble = false
                
                // Activity log kaydet
                dbHelper.addActivityLog(
                    actionType = ActionTypes.GAME_START,
                    description = "Crawford eli başladı - Katlama zarı devre dışı (${targetRounds-1} puana ulaşıldı)",
                    player1Name = player1Name,
                    player2Name = player2Name,
                    matchId = if (isRematchMode) -1 else matchId,
                    extraData = "crawford_start=true,target_score=$targetRounds,p1_score=$player1Score,p2_score=$player2Score"
                )
                
                Toast.makeText(context, "⚠️ CRAWFORD ELİ - Katlama zarı bu el için devre dışı!", Toast.LENGTH_LONG).show()
            }
        }
    }

    // Crawford elinin bitişini kontrol eden fonksiyon
    fun handleCrawfordGameEnd() {
        if (isCrawfordGame) {
            crawfordGamePlayed = true
            isCrawfordGame = false

            // Crawford elinden sonra katlama butonları tekrar aktif
            player1CanDouble = true
            player2CanDouble = true
            
            // Crawford elinden sonra eğer parti devam ediyorsa Post-Crawford moduna geç
            if (player1Score < targetRounds && player2Score < targetRounds) {
                isPostCrawford = true
                
                // Activity log kaydet
                dbHelper.addActivityLog(
                    actionType = ActionTypes.GAME_END,
                    description = "Crawford eli bitti - Post-Crawford modu başladı (Katlama zarı tekrar aktif)",
                    player1Name = player1Name,
                    player2Name = player2Name,
                    matchId = if (isRematchMode) -1 else matchId,
                    extraData = "crawford_end=true,post_crawford=true,p1_score=$player1Score,p2_score=$player2Score"
                )
                
                Toast.makeText(context, "✅ Crawford eli bitti - Katlama zarı tekrar aktif!", Toast.LENGTH_LONG).show()
            } else {
                // Activity log kaydet  
                dbHelper.addActivityLog(
                    actionType = ActionTypes.GAME_END,
                    description = "Crawford eli bitti - Parti sona erdi",
                    player1Name = player1Name,
                    player2Name = player2Name,
                    matchId = if (isRematchMode) -1 else matchId,
                    extraData = "crawford_end=true,match_finished=true,p1_score=$player1Score,p2_score=$player2Score"
                )
            }
        }
    }
    
    // ✅ ZAR SETİ NUMARALANDIRMA FONKSİYONLARI
    fun getCurrentDiceSetId(): String {
        return if (isRematchMode) {
            "P${rematchPartyIndex + 1}-O${currentRound + 1}-S$currentDiceSetNumber"
        } else {
            "P$currentPartNumber-O${currentRound + 1}-S$currentDiceSetNumber"
        }
    }
    
    fun incrementDiceSet() {
        currentDiceSetNumber++
        val setId = getCurrentDiceSetId()
        diceSetHistory.add(setId)
        
        // Activity log'a kaydet
        dbHelper.addActivityLog(
            actionType = ActionTypes.DICE_ROLL,
            description = "Yeni zar seti: $setId",
            player1Name = player1Name,
            player2Name = player2Name,
            matchId = if (isRematchMode) -1 else matchId
        )
    }
    
    // ✅ OYUN BAŞLADIĞINDA MEVCUT SETİ EKLE
    fun initializeCurrentDiceSet() {
        val currentSetId = getCurrentDiceSetId()
        if (!diceSetHistory.contains(currentSetId)) {
            diceSetHistory.add(currentSetId)
        }
    }

    
    fun resetDiceSetForNewGame() {
        currentDiceSetNumber = 1
        currentGameNumber++
    }
    
    fun resetDiceSetForNewParty() {
        currentDiceSetNumber = 1
        currentGameNumber = 1
        currentPartNumber++
    }
    
    // ✅ TAM GERİ ALMA FONKSİYONLARI
    fun captureGameState(): GameStateSnapshot {
        return GameStateSnapshot(
            player1Score = player1Score,
            player2Score = player2Score,
            currentRound = currentRound,
            player1RoundsWon = player1RoundsWon,
            player2RoundsWon = player2RoundsWon,
            doublingCubeValue = doublingCubeValue,
            doublingCubePosition = doublingCubePosition,
            player1CanDouble = player1CanDouble,
            player2CanDouble = player2CanDouble,
            isCrawfordGame = isCrawfordGame,
            crawfordGamePlayed = crawfordGamePlayed,
            isPostCrawford = isPostCrawford,
            currentDiceSetNumber = currentDiceSetNumber,
            diceSetHistory = diceSetHistory.toList()
        )
    }
    
    fun saveGameStateSnapshot() {
        val snapshot = captureGameState()
        gameStateHistory.add(snapshot)
        
        // En fazla 50 snapshot tut (hafıza optimizasyonu)
        if (gameStateHistory.size > 50) {
            gameStateHistory.removeAt(0)
        }
    }
    
    fun restoreGameState(snapshot: GameStateSnapshot) {
        player1Score = snapshot.player1Score
        player2Score = snapshot.player2Score
        currentRound = snapshot.currentRound
        player1RoundsWon = snapshot.player1RoundsWon
        player2RoundsWon = snapshot.player2RoundsWon
        doublingCubeValue = snapshot.doublingCubeValue
        doublingCubePosition = snapshot.doublingCubePosition
        player1CanDouble = snapshot.player1CanDouble
        player2CanDouble = snapshot.player2CanDouble
        isCrawfordGame = snapshot.isCrawfordGame
        crawfordGamePlayed = snapshot.crawfordGamePlayed
        isPostCrawford = snapshot.isPostCrawford
        currentDiceSetNumber = snapshot.currentDiceSetNumber
        diceSetHistory = snapshot.diceSetHistory.toMutableList()
        
        // UI'yi yeniden çiz
        recomposeKey++
    }

    // Maç sona erdiğinde yapılacak işlemler
    fun endMatch() {
        // Rövanşlı modda endMatch çağrılmaz (handleRematchPartyEnd kullanılır)
        if (isRematchMode) {
            showEndMatchConfirmation = true
            return
        }
        val winnerId = dbHelper.finishMatch(matchId)

        // Kazananı belirle
        if (player1Score > player2Score) {
            winnerName = player1Name
            winnerScore = player1Score
            loserName = player2Name
            loserScore = player2Score
        } else {
            winnerName = player2Name
            winnerScore = player2Score
            loserName = player1Name
            loserScore = player1Score
        }

        // Mac bitis islemini logla
        dbHelper.addActivityLog(
            actionType = ActionTypes.GAME_END,
            description = "Mac bitti: $winnerName kazandi ($winnerScore - $loserScore)",
            player1Name = player1Name,
            player2Name = player2Name,
            matchId = matchId,
            extraData = """{"winnerId":$winnerId,"winnerScore":$winnerScore,"loserScore":$loserScore,"totalRounds":$currentRound}"""
        )

        showMatchEndDialog = true
    }

    // Ekran genişliğini dinamik olarak hesapla
    val screenWidthDp = configuration.screenWidthDp.dp
    val edgeOffset = (screenWidthDp / 2) - 50.dp // Ekran kenarına yakın, biraz iç tarafta

    // Zarın pozisyonuna göre x ve y offset'lerini hesaplama - ekran yönüne göre ayarlandı ve
    // kontrol pozisyonları için ekran kenarlarına daha yakın yerleşim
    val (xOffset, yOffset) = when (doublingCubePosition) {
        DoublingCubePosition.CENTER -> Pair(0.dp, -80.dp) // Küp başlangıç pozisyonu biraz aşağı çekildi
        DoublingCubePosition.PLAYER1_OFFER -> Pair(-120.dp, -40.dp)
        DoublingCubePosition.PLAYER1_CONTROL -> if (isLandscape) Pair(-edgeOffset, 30.dp) else Pair(-edgeOffset, 60.dp)
        DoublingCubePosition.PLAYER2_OFFER -> Pair(120.dp, -40.dp)
        DoublingCubePosition.PLAYER2_CONTROL -> if (isLandscape) Pair(edgeOffset, 30.dp) else Pair(edgeOffset, 60.dp)
    }

    // Animasyonlu offset değerleri
    val animatedXOffset by animateDpAsState(
        targetValue = xOffset,
        animationSpec = tween(durationMillis = 500),
        label = "xOffset"
    )

    val animatedYOffset by animateDpAsState(
        targetValue = yOffset,
        animationSpec = tween(durationMillis = 500),
        label = "yOffset"
    )

    // === RÖVANŞLI MOD: Parti bitişi yönetimi ===
    fun handleRematchPartyEnd() {
        val totalGamesPlayed = rematchGameIndex + 1
        val partyWinnerId = if (player1Score >= player2Score) player1Id else player2Id
        val partyWinnerName = if (player1Score >= player2Score) player1Name else player2Name

        // Parti sonucunu kaydet
        dbHelper.saveRematchPartyResult(
            encounterId = encounterId,
            partyIndex = rematchPartyIndex,
            roundNumber = rematchCurrentRound,
            player1Score = player1Score,
            player2Score = player2Score,
            winnerId = partyWinnerId,
            totalGamesPlayed = totalGamesPlayed
        )

        dbHelper.addActivityLog(
            actionType = ActionTypes.REMATCH_MATCH_END,
            description = "Parti ${rematchPartyIndex+1} bitti: $partyWinnerName kazandi ($player1Score-$player2Score)",
            player1Name = player1Name,
            player2Name = player2Name
        )

        val nextPartyIndex = rematchPartyIndex + 1

        if (nextPartyIndex >= totalParties) {
            if (rematchCurrentRound == 1) {
                dbHelper.completeFirstRound(encounterId)
                dbHelper.advanceToRematchRound(encounterId)
                showRoundEndDialog = true
            } else {
                dbHelper.completeEncounter(encounterId)
                showEncounterEndDialog = true
            }
        } else {
            dbHelper.updateEncounterProgress(encounterId, nextPartyIndex, 0)

            if (rematchCurrentRound == 2) {
                val intent = Intent(context, RematchComparisonActivity::class.java)
                intent.putExtra("encounter_id", encounterId)
                intent.putExtra("party_index", rematchPartyIndex)
                context.startActivity(intent)
            }

            partyEndInfo = "Parti ${rematchPartyIndex+1}: $partyWinnerName kazandi ($player1Score-$player2Score)\nParti ${nextPartyIndex+1} basliyor."
            player1Score = 0
            player2Score = 0
            currentRound = 0
            rematchPartyIndex = nextPartyIndex
            rematchGameIndex = 0
            rematchUndoStack = emptyList()

            isCrawfordGame = false
            crawfordGamePlayed = false
            isPostCrawford = false

            showPartyEndDialog = true
            recomposeKey++
        }
    }

    // === RÖVANŞLI MOD: Oyun sonucu kaydet ===
    fun executeRematchAddRound(playerId: Long, playerName: String, winType: String, score: Int) {
        val finalScore = score * doublingCubeValue

        // SharedPreferences'dan zar verilerini KAYDETME ANINDA oku
        // (Zar ekranından döndükten sonra güncel değerleri almak için)
        val prefs = context.getSharedPreferences("rematch_prefs", android.content.Context.MODE_PRIVATE)
        rematchDicePairsUsed = prefs.getInt("dice_pairs_used_${encounterId}", 0)
        rematchLeftDiceTotal = prefs.getInt("left_dice_total_${encounterId}", 0)
        rematchRightDiceTotal = prefs.getInt("right_dice_total_${encounterId}", 0)
        rematchLeftDoublesCount = prefs.getInt("left_doubles_count_${encounterId}", 0)
        rematchRightDoublesCount = prefs.getInt("right_doubles_count_${encounterId}", 0)

        val leftPlayerId: Long
        val rightPlayerId: Long
        if (rematchCurrentRound == 1) {
            leftPlayerId = player1Id
            rightPlayerId = player2Id
        } else {
            leftPlayerId = player2Id
            rightPlayerId = player1Id
        }

        val resultId = dbHelper.saveRematchGameResult(
            encounterId = encounterId,
            partyIndex = rematchPartyIndex,
            setIndex = rematchGameIndex,
            roundNumber = rematchCurrentRound,
            leftPlayerId = leftPlayerId,
            rightPlayerId = rightPlayerId,
            winnerId = playerId,
            winType = winType,
            cubeValue = doublingCubeValue,
            finalScore = finalScore,
            loserPipCount = pipCountInput.toIntOrNull() ?: 0,
            dicePairsUsed = rematchDicePairsUsed,
            doublerPlayerId = rematchLastDoublerPlayerId,
            leftDiceTotal = rematchLeftDiceTotal,
            rightDiceTotal = rematchRightDiceTotal,
            leftDoublesCount = rematchLeftDoublesCount,
            rightDoublesCount = rematchRightDoublesCount
        )

        if (resultId != -1L) {
            rematchUndoStack = rematchUndoStack + resultId
        }

        currentRound++
        if (playerId == player1Id) {
            player1Score += finalScore
            player1RoundsWon++
        } else {
            player2Score += finalScore
            player2RoundsWon++
        }

        previousDoublingCubeValue = 1
        previousDoublingCubePosition = DoublingCubePosition.CENTER
        doublingCubeValue = 1
        doublingCubePosition = DoublingCubePosition.CENTER
        player1CanDouble = true
        player2CanDouble = true
        showPlayer1DoublingMenu = false
        showPlayer2DoublingMenu = false

        pipCountInput = ""
        rematchDicePairsUsed = 0
        rematchLastDoublerPlayerId = null
        rematchLeftDiceTotal = 0
        rematchRightDiceTotal = 0
        rematchLeftDoublesCount = 0
        rematchRightDoublesCount = 0

        handleCrawfordGameEnd()
        checkCrawfordStatus()

        val winTypeText = when (winType) {
            "SINGLE" -> "Tek"
            "MARS" -> "Mars"
            "BACKGAMMON" -> "Backgammon"
            else -> winType
        }
        dbHelper.addActivityLog(
            actionType = ActionTypes.SCORE_SINGLE,
            description = "Rovansli: $playerName $winTypeText (+$finalScore) - Parti ${rematchPartyIndex+1} El ${rematchGameIndex+1}",
            player1Name = player1Name,
            player2Name = player2Name
        )

        Toast.makeText(context, "$playerName: $winTypeText (+$finalScore puan)", Toast.LENGTH_SHORT).show()

        if (player1Score >= matchTargetScore || player2Score >= matchTargetScore) {
            handleRematchPartyEnd()
        } else if (rematchGameIndex + 1 >= DiceGenerator.maxSetsForTargetScore(matchTargetScore)) {
            handleRematchPartyEnd()
        } else {
            rematchGameIndex++
            dbHelper.updateEncounterProgress(encounterId, rematchPartyIndex, rematchGameIndex)
        }
    }

    // ✅ Gerçek round ekleme işlemi (onay sonrası veya maç bitmeyecekse direkt çağrılır)
    fun executeAddRound(playerId: Long, playerName: String, winType: String, score: Int) {
        // ✅ İşlem öncesi oyun durumunu kaydet
        val gameSnapshot = captureGameState()
        gameStateHistory.add(gameSnapshot)
        
        // === RÖVANŞLI MOD ===
        if (isRematchMode) {
            executeRematchAddRound(playerId, playerName, winType, score)
            return
        }

        // === NORMAL MOD ===
        currentRound++

        // Küp değeri ile çarparak gerçek skoru hesapla
        val finalScore = score * doublingCubeValue

        // El bilgisini veritabanına ekle ve ID'sini al
        val roundId = dbHelper.addRound(
            matchId = matchId,
            roundNumber = currentRound,
            winnerId = playerId,
            winType = winType,
            isDouble = doublingCubeValue > 1,
            score = finalScore
        )

        // UNDO STACK'e ekle
        if (roundId != -1L) {
            undoStack = undoStack + roundId
        }

        // Skor ekleme islemini logla
        val actionType = when (winType) {
            "SINGLE" -> ActionTypes.SCORE_SINGLE
            "MARS" -> ActionTypes.SCORE_MARS
            "BACKGAMMON" -> ActionTypes.SCORE_BACKGAMMON
            else -> ActionTypes.SCORE_SINGLE
        }
        val winTypeText = when (winType) {
            "SINGLE" -> "Tek"
            "MARS" -> "Mars"
            "BACKGAMMON" -> "Backgammon"
            else -> winType
        }
        dbHelper.addActivityLog(
            actionType = actionType,
            description = "$playerName: $winTypeText (+$finalScore puan) - El $currentRound",
            player1Name = player1Name,
            player2Name = player2Name,
            matchId = matchId,
            extraData = """{"winnerId":$playerId,"winType":"$winType","score":$finalScore,"cubeValue":$doublingCubeValue,"round":$currentRound}"""
        )

        // Kazanan oyuncuya puanı ekle ve kazandığı el sayısını güncelle
        if (playerId == player1Id) {
            player1Score += finalScore
            player1RoundsWon++
            Toast.makeText(context, "$player1Name: $winType (+$finalScore puan)", Toast.LENGTH_SHORT).show()
        } else {
            player2Score += finalScore
            player2RoundsWon++
            Toast.makeText(context, "$player2Name: $winType (+$finalScore puan)", Toast.LENGTH_SHORT).show()
        }

        // Katlama zarını sıfırla
        previousDoublingCubeValue = 1
        previousDoublingCubePosition = DoublingCubePosition.CENTER
        doublingCubeValue = 1
        doublingCubePosition = DoublingCubePosition.CENTER
        player1CanDouble = true
        player2CanDouble = true
        showPlayer1DoublingMenu = false
        showPlayer2DoublingMenu = false

        // Crawford kontrolü yap
        handleCrawfordGameEnd()
        checkCrawfordStatus()
        
        // ✅ Yeni oyun için zar setini sıfırla
        resetDiceSetForNewGame()

        // Hedef puana ulaşıldıysa maçı bitir
        if (player1Score >= matchTargetScore || player2Score >= matchTargetScore) {
            endMatch()
        }
    }

    // ✅ GÜNCELLENMİŞ: El ekle ve skoru güncelle (onay sistemi ile)
    fun addRound(playerId: Long, playerName: String, winType: String, score: Int) {
        // Küp değeri ile çarparak gerçek skoru hesapla
        val finalScore = score * doublingCubeValue

        if (isRematchMode) {
            // Rövanşlı modda: pending bilgileri kaydet, pip input göster
            pendingRematchWinnerId = playerId
            pendingRematchWinnerName = playerName
            pendingRematchWinType = winType
            pendingRematchBaseScore = score

            // Bu parti bitecek mi kontrol et
            val newP1 = if (playerId == player1Id) player1Score + finalScore else player1Score
            val newP2 = if (playerId == player2Id) player2Score + finalScore else player2Score

            if (newP1 >= matchTargetScore || newP2 >= matchTargetScore) {
                // Parti bitecek - onay al
                pendingWinnerId = playerId
                pendingWinnerName = playerName
                pendingWinType = winType
                pendingScore = score
                pendingFinalPlayer1Score = newP1
                pendingFinalPlayer2Score = newP2
                showMatchWinConfirmation = true
            } else {
                // Parti bitmeyecek - pip input göster
                showPipInputDialog = true
            }
            return
        }

        // === NORMAL MOD ===
        // Bu işlem sonrası skorları hesapla
        val newPlayer1Score = if (playerId == player1Id) player1Score + finalScore else player1Score
        val newPlayer2Score = if (playerId == player2Id) player2Score + finalScore else player2Score

        // Bu işlem maçı bitirecek mi? Öyleyse önce onay al
        if (newPlayer1Score >= matchTargetScore || newPlayer2Score >= matchTargetScore) {
            // Bekleyen işlem bilgilerini kaydet
            pendingWinnerId = playerId
            pendingWinnerName = playerName
            pendingWinType = winType
            pendingScore = score
            pendingFinalPlayer1Score = newPlayer1Score
            pendingFinalPlayer2Score = newPlayer2Score
            // Onay dialog'unu göster
            showMatchWinConfirmation = true
        } else {
            // Maçı bitirmeyecek, direkt işlemi yap
            executeAddRound(playerId, playerName, winType, score)
        }
    }

    // ✅ Son hamleyi TAM geri al - Snapshot sistemi ile
    fun undoLastRound() {
        // ✅ Önce snapshot gerisini dene
        if (gameStateHistory.isNotEmpty()) {
            val lastSnapshot = gameStateHistory.last()
            gameStateHistory = gameStateHistory.dropLast(1).toMutableList()
            restoreGameState(lastSnapshot)
            
            // Veritabanından da geri al (eğer varsa)
            if (isRematchMode && rematchUndoStack.isNotEmpty()) {
                val lastResultId = rematchUndoStack.last()
                dbHelper.deleteRematchGameResult(lastResultId)
                rematchUndoStack = rematchUndoStack.dropLast(1)
            } else if (!isRematchMode && undoStack.isNotEmpty()) {
                val lastRoundId = undoStack.last()
                dbHelper.deleteRound(lastRoundId)
                undoStack = undoStack.dropLast(1)
            }
            
            // Activity log'a kaydet
            dbHelper.addActivityLog(
                actionType = ActionTypes.SCORE_UNDO,
                description = "Son işlem tamamen geri alındı (Snapshot #${gameStateHistory.size + 1})",
                player1Name = player1Name,
                player2Name = player2Name,
                matchId = if (isRematchMode) -1 else matchId
            )
            
            Toast.makeText(context, "Son işlem tamamen geri alındı", Toast.LENGTH_SHORT).show()
            return
        }
        
        // === ESKI SİSTEM - Snapshot yoksa kullan ===
        // === RÖVANŞLI MOD UNDO ===
        if (isRematchMode) {
            if (rematchUndoStack.isNotEmpty()) {
                try {
                    val lastResultId = rematchUndoStack.last()
                    val deleted = dbHelper.deleteRematchGameResult(lastResultId)
                    if (deleted > 0) {
                        rematchUndoStack = rematchUndoStack.dropLast(1)

                        // Parti skorlarını DB'den yeniden yükle
                        val (p1Score, p2Score) = dbHelper.getPartyScore(
                            encounterId, rematchPartyIndex, rematchCurrentRound
                        )
                        player1Score = p1Score
                        player2Score = p2Score
                        currentRound--
                        if (rematchGameIndex > 0) {
                            rematchGameIndex--
                            dbHelper.updateEncounterProgress(encounterId, rematchPartyIndex, rematchGameIndex)
                        }

                        // Küpü sıfırla
                        doublingCubeValue = 1
                        doublingCubePosition = DoublingCubePosition.CENTER
                        player1CanDouble = true
                        player2CanDouble = true
                        showPlayer1DoublingMenu = false
                        showPlayer2DoublingMenu = false

                        checkCrawfordStatus()
                        recomposeKey++
                        Toast.makeText(context, "Son hamle geri alindi", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    Toast.makeText(context, "Hata: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
            return
        }

        // === NORMAL MOD UNDO ===
        if (undoStack.isNotEmpty()) {
            try {
                val lastRoundId = undoStack.last()

                // 1. Round'u veritabanından sil
                val deleteResult = dbHelper.deleteRound(lastRoundId)

                if (deleteResult > 0) {
                    // 2. Stack'ten çıkar
                    undoStack = undoStack.dropLast(1)

                    // 3. Maç durumunu veritabanından yeniden yükle
                    val updatedMatch = dbHelper.getMatchDetails(matchId)
                    if (updatedMatch != null) {
                        // State'leri güncelle
                        player1Score = updatedMatch.player1Score
                        player2Score = updatedMatch.player2Score
                        player1RoundsWon = updatedMatch.player1RoundsWon
                        player2RoundsWon = updatedMatch.player2RoundsWon
                        currentRound = updatedMatch.totalRounds

                        // Force recompose
                        recomposeKey++
                    }

                    // Katlama zarını sıfırla
                    doublingCubeValue = 1
                    doublingCubePosition = DoublingCubePosition.CENTER
                    player1CanDouble = true
                    player2CanDouble = true
                    showPlayer1DoublingMenu = false
                    showPlayer2DoublingMenu = false

                    // Crawford durumunu kontrol et
                    checkCrawfordStatus()

                    // Geri alma islemini logla
                    dbHelper.addActivityLog(
                        actionType = ActionTypes.SCORE_UNDO,
                        description = "Son hamle geri alindi (El ${currentRound + 1})",
                        player1Name = player1Name,
                        player2Name = player2Name,
                        matchId = matchId
                    )

                    Toast.makeText(context, "Son hamle geri alındı", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "Geri alma başarısız", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Hata: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }


    // Renkler
    val blueColor = Color(0xFF2196F3)
    val redColor = Color(0xFFE91E63)
    val purpleColor = Color(0xFF9C27B0) // Mavi ve kırmızının karışımı

    // Katlama zarı işlemleri
    fun player1OfferDouble() {
        if (player1CanDouble) {
            // ✅ Katlama teklifi öncesi oyun durumunu kaydet
            val gameSnapshot = captureGameState()
            gameStateHistory.add(gameSnapshot)
            
            // Tekliften önceki değerleri kaydet
            previousDoublingCubeValue = doublingCubeValue
            previousDoublingCubePosition = doublingCubePosition

            // Küpü ikiye katla
            doublingCubeValue *= 2
            doublingCubePosition = DoublingCubePosition.PLAYER1_OFFER
            player1CanDouble = false
            showPlayer2DoublingMenu = true
            if (isRematchMode) rematchLastDoublerPlayerId = player1Id

            // Katlama teklifi logla
            dbHelper.addActivityLog(
                actionType = ActionTypes.DOUBLE_OFFER,
                description = "$player1Name katlama teklifi yapti (x$doublingCubeValue)",
                player1Name = player1Name,
                player2Name = player2Name,
                matchId = matchId
            )
        }
    }

    fun player2OfferDouble() {
        if (player2CanDouble) {
            // ✅ Katlama teklifi öncesi oyun durumunu kaydet
            val gameSnapshot = captureGameState()
            gameStateHistory.add(gameSnapshot)
            
            // Tekliften önceki değerleri kaydet
            previousDoublingCubeValue = doublingCubeValue
            previousDoublingCubePosition = doublingCubePosition

            // Küpü ikiye katla
            doublingCubeValue *= 2
            doublingCubePosition = DoublingCubePosition.PLAYER2_OFFER
            player2CanDouble = false
            showPlayer1DoublingMenu = true
            if (isRematchMode) rematchLastDoublerPlayerId = player2Id

            // Katlama teklifi logla
            dbHelper.addActivityLog(
                actionType = ActionTypes.DOUBLE_OFFER,
                description = "$player2Name katlama teklifi yapti (x$doublingCubeValue)",
                player1Name = player1Name,
                player2Name = player2Name,
                matchId = matchId
            )
        }
    }

    fun player1AcceptDouble() {
        // ✅ Katlama kabul öncesi oyun durumunu kaydet
        val gameSnapshot = captureGameState()
        gameStateHistory.add(gameSnapshot)
        
        doublingCubePosition = DoublingCubePosition.PLAYER1_CONTROL
        showPlayer1DoublingMenu = false
        player1CanDouble = true
        player2CanDouble = false

        // Katlama kabul logla
        dbHelper.addActivityLog(
            actionType = ActionTypes.DOUBLE_ACCEPT,
            description = "$player1Name katlamayi kabul etti (x$doublingCubeValue)",
            player1Name = player1Name,
            player2Name = player2Name,
            matchId = matchId
        )
    }

    fun player2AcceptDouble() {
        // ✅ Katlama kabul öncesi oyun durumunu kaydet
        val gameSnapshot = captureGameState()
        gameStateHistory.add(gameSnapshot)
        
        doublingCubePosition = DoublingCubePosition.PLAYER2_CONTROL
        showPlayer2DoublingMenu = false
        player2CanDouble = true
        player1CanDouble = false

        // Katlama kabul logla
        dbHelper.addActivityLog(
            actionType = ActionTypes.DOUBLE_ACCEPT,
            description = "$player2Name katlamayi kabul etti (x$doublingCubeValue)",
            player1Name = player1Name,
            player2Name = player2Name,
            matchId = matchId
        )
    }

    fun player1Resign() {
        // ✅ Oyuncu 1 pes etti, Oyuncu 2 bu oyunu kazandı
        // Pes etme islemini logla
        dbHelper.addActivityLog(
            actionType = ActionTypes.DOUBLE_REJECT,
            description = "$player1Name pes etti - $player2Name eli kazandi",
            player1Name = player1Name,
            player2Name = player2Name,
            matchId = matchId
        )

        // Teklif öncesi küp değeri kullanılır (mevcut değerin yarısı)
        val previousCubeValue = doublingCubeValue / 2
        // Küp değerini teklif öncesine çevir (addRound bu değerle çarpacak)
        doublingCubeValue = if (previousCubeValue > 0) previousCubeValue else 1

        // Menüyü kapat (addRound'dan önce, çünkü addRound da kapatıyor)
        showPlayer1DoublingMenu = false

        // Oyuncu 2 tek oyun (RESIGN) kazandı - addRound ile kaydet
        // Bu sayede: roundsWon artacak, veritabanına kaydedilecek, geri alınabilecek
        addRound(player2Id, player2Name, "RESIGN", 1)

        Toast.makeText(context, "$player1Name pes etti. $player2Name oyunu kazandı.", Toast.LENGTH_SHORT).show()
    }

    fun player2Resign() {
        // ✅ Oyuncu 2 pes etti, Oyuncu 1 bu oyunu kazandı
        // Pes etme islemini logla
        dbHelper.addActivityLog(
            actionType = ActionTypes.DOUBLE_REJECT,
            description = "$player2Name pes etti - $player1Name eli kazandi",
            player1Name = player1Name,
            player2Name = player2Name,
            matchId = matchId
        )

        // Teklif öncesi küp değeri kullanılır (mevcut değerin yarısı)
        val previousCubeValue = doublingCubeValue / 2
        // Küp değerini teklif öncesine çevir (addRound bu değerle çarpacak)
        doublingCubeValue = if (previousCubeValue > 0) previousCubeValue else 1

        // Menüyü kapat (addRound'dan önce, çünkü addRound da kapatıyor)
        showPlayer2DoublingMenu = false

        // Oyuncu 1 tek oyun (RESIGN) kazandı - addRound ile kaydet
        // Bu sayede: roundsWon artacak, veritabanına kaydedilecek, geri alınabilecek
        addRound(player1Id, player1Name, "RESIGN", 1)

        Toast.makeText(context, "$player2Name pes etti. $player1Name oyunu kazandı.", Toast.LENGTH_SHORT).show()
    }

    fun resetDoublingCube() {
        // Katlama iptal islemini logla
        dbHelper.addActivityLog(
            actionType = ActionTypes.DOUBLE_CANCEL,
            description = "Katlama teklifi iptal edildi",
            player1Name = player1Name,
            player2Name = player2Name,
            matchId = matchId
        )

        // Önceki pozisyon ve değere dön
        doublingCubeValue = previousDoublingCubeValue
        doublingCubePosition = previousDoublingCubePosition

        // Menüleri kapat
        showPlayer1DoublingMenu = false
        showPlayer2DoublingMenu = false

        // Oyuncuların katlama haklarını doğru şekilde güncelle
        when (previousDoublingCubePosition) {
            DoublingCubePosition.CENTER -> {
                player1CanDouble = true
                player2CanDouble = true
            }
            DoublingCubePosition.PLAYER1_CONTROL -> {
                player1CanDouble = true
                player2CanDouble = false
            }
            DoublingCubePosition.PLAYER2_CONTROL -> {
                player1CanDouble = false
                player2CanDouble = true
            }
            else -> {
                // Diğer durumlar için varsayılan ayarlar
                player1CanDouble = true
                player2CanDouble = true
            }
        }
    }

    // Maç sonu diyaloğu
    if (showMatchEndDialog) {
        AlertDialog(
            onDismissRequest = {
                showMatchEndDialog = false
                onFinish()
            },
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Maç Sonucu")
                    if (isCrawfordGame) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = Color.Red.copy(alpha = 0.8f)
                            ),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = "CRAWFORD ELİ",
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                color = Color.White,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    if (isPostCrawford) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = Color(0xFF4CAF50).copy(alpha = 0.8f)
                            ),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = "POST-CRAWFORD",
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                color = Color.White,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            },
            text = {
                Column {
                    Text("Kazanan: $winnerName ($winnerScore puan)")
                    Text("Kaybeden: $loserName ($loserScore puan)")
                    Text("Toplam El: $currentRound")
                    Text("$player1Name: $player1RoundsWon el kazandı")
                    Text("$player2Name: $player2RoundsWon el kazandı")
                    
                    // Crawford durumu bilgileri
                    if (crawfordGamePlayed) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = Color(0xFF2196F3).copy(alpha = 0.2f)
                            ),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(8.dp)
                            ) {
                                Text(
                                    text = "📊 Crawford Kuralı Bilgileri:",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = Color(0xFF1976D2)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                
                                if (crawfordGamePlayed) {
                                    Text("✅ Crawford eli oynanmış", fontSize = 11.sp)
                                }
                                if (isPostCrawford) {
                                    Text("🔄 Post-Crawford modunda", fontSize = 11.sp)
                                } else if (isCrawfordGame) {
                                    Text("⚠️ Crawford eli devam ediyor", fontSize = 11.sp)
                                }
                                
                                Text(
                                    text = "Hedef puan: $targetRounds",
                                    fontSize = 11.sp,
                                    color = Color.Gray
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    showMatchEndDialog = false
                    onFinish()
                }) {
                    Text("Ana Menüye Dön")
                }
            }
        )
    }

    // Maçı sonlandırma onay diyaloğu
    if (showEndMatchConfirmation) {
        AlertDialog(
            onDismissRequest = { showEndMatchConfirmation = false },
            title = { Text("Maçı Sonlandır") },
            text = {
                Column {
                    if (isRematchMode) {
                        Text(
                            "RÖVANŞLI KARŞILAŞMA!",
                            color = Color(0xFFCE93D8),
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Bu bir rövanşlı karşılaşma. Maçı şimdi sonlandırırsanız zar setini kaybedebilirsiniz.")
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Sonlandırmak istediğinizden EMİN mİsınız?", fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("$player1Name: $player1Score puan")
                        Text("$player2Name: $player2Score puan")
                    } else {
                        Text("Maçı sonlandırmak istediğinizden emin misiniz?")
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("$player1Name: $player1Score puan ($player1RoundsWon el)")
                        Text("$player2Name: $player2Score puan ($player2RoundsWon el)")
                        Text("Bu sonuçlar istatistiklere kaydedilecektir.")
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showEndMatchConfirmation = false
                        endMatch() // Onaylandığında maçı sonlandır
                    }
                ) {
                    Text("Evet, Sonlandır")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEndMatchConfirmation = false }) {
                    Text("Hayır, Devam Et")
                }
            }
        )
    }

    // Hareketler Dökümü Dialog
    if (showActivityLogDialog) {
        val activityLogs = remember { dbHelper.getActivityLogsByMatch(matchId) }

        AlertDialog(
            onDismissRequest = { showActivityLogDialog = false },
            title = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Hareketler Dökümü", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    Text("${activityLogs.size} kayıt", fontSize = 12.sp, color = Color.Gray)
                }
            },
            text = {
                if (activityLogs.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Henüz kayıt yok", color = Color.Gray)
                    }
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(300.dp)
                            .padding(4.dp)
                    ) {
                        // Scrollable log listesi
                        androidx.compose.foundation.lazy.LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            items(activityLogs.size) { index ->
                                val log = activityLogs[index]
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(
                                            Color(0xFFF5F5F5),
                                            RoundedCornerShape(4.dp)
                                        )
                                        .padding(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Saat
                                    Text(
                                        text = log.timestamp,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF1976D2),
                                        modifier = Modifier.width(55.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    // Açıklama
                                    Text(
                                        text = log.description,
                                        fontSize = 11.sp,
                                        maxLines = 2,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showActivityLogDialog = false }) {
                    Text("Kapat")
                }
            }
        )
    }

    // ✅ Maç kazanma onay diyaloğu (skor ekleme maçı bitiriyorsa)
    if (showMatchWinConfirmation) {
        val winnerDisplayName = if (pendingFinalPlayer1Score > pendingFinalPlayer2Score) player1Name else player2Name
        val winnerDisplayScore = maxOf(pendingFinalPlayer1Score, pendingFinalPlayer2Score)
        val loserDisplayScore = minOf(pendingFinalPlayer1Score, pendingFinalPlayer2Score)

        AlertDialog(
            onDismissRequest = {
                // Dialog dışına tıklayınca iptal et
                showMatchWinConfirmation = false
            },
            title = { Text("Maç Sonu Onayı") },
            text = {
                Column {
                    Text(
                        text = "$winnerDisplayName maçı kazandı!",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Skor: $winnerDisplayScore - $loserDisplayScore")
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("$player1Name: $pendingFinalPlayer1Score puan")
                    Text("$player2Name: $pendingFinalPlayer2Score puan")
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Bu sonucu onaylıyor musunuz?")
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showMatchWinConfirmation = false
                        if (isRematchMode) {
                            // Rövanşlı modda pip input göster
                            showPipInputDialog = true
                        } else {
                            // Normal modda direkt onayla
                            executeAddRound(pendingWinnerId, pendingWinnerName, pendingWinType, pendingScore)
                        }
                    }
                ) {
                    Text("Evet, Onayla", color = Color(0xFF4CAF50))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showMatchWinConfirmation = false
                        Toast.makeText(context, "İşlem iptal edildi", Toast.LENGTH_SHORT).show()
                    }
                ) {
                    Text("Hayır, İptal", color = Color.Red)
                }
            }
        )
    }

    // === RÖVANŞLI MOD: Pip Input Dialog ===
    if (showPipInputDialog) {
        AlertDialog(
            onDismissRequest = { showPipInputDialog = false },
            title = { Text("Pip Sayisi") },
            text = {
                Column {
                    Text("Kaybeden pip sayisi (opsiyonel):")
                    Spacer(modifier = Modifier.height(8.dp))
                    androidx.compose.material3.OutlinedTextField(
                        value = pipCountInput,
                        onValueChange = { newVal ->
                            if (newVal.isEmpty() || (newVal.all { it.isDigit() } && (newVal.toIntOrNull() ?: 0) <= 167)) {
                                pipCountInput = newVal
                            }
                        },
                        placeholder = { Text("0-167") },
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    showPipInputDialog = false
                    executeAddRound(pendingRematchWinnerId, pendingRematchWinnerName, pendingRematchWinType, pendingRematchBaseScore)
                }) {
                    Text("Kaydet", color = Color(0xFF4CAF50))
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showPipInputDialog = false
                    pipCountInput = ""
                    executeAddRound(pendingRematchWinnerId, pendingRematchWinnerName, pendingRematchWinType, pendingRematchBaseScore)
                }) {
                    Text("Atla (pip=0)")
                }
            }
        )
    }

    // === RÖVANŞLI MOD: Parti Bitiş Dialog ===
    if (showPartyEndDialog) {
        AlertDialog(
            onDismissRequest = { showPartyEndDialog = false },
            title = { Text("Parti Tamamlandi") },
            text = { Text(partyEndInfo) },
            confirmButton = {
                TextButton(onClick = { showPartyEndDialog = false }) {
                    Text("Sonraki Partiye Devam", color = Color(0xFF4CAF50))
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showPartyEndDialog = false
                    onFinish()
                }) {
                    Text("Daha Sonra Devam Et", color = Color.Gray)
                }
            }
        )
    }

    // === RÖVANŞLI MOD: Tur Bitiş Dialog ===
    if (showRoundEndDialog) {
        AlertDialog(
            onDismissRequest = {},
            title = { Text("Tur 1 Tamamlandi!") },
            text = {
                Column {
                    Text("Tum $totalParties parti oynandi.")
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Rovans turu basliyor. Zarlar yer degistirecek.",
                        fontWeight = FontWeight.Bold)
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    showRoundEndDialog = false
                    // Tur 2 için sıfırla
                    player1Score = 0
                    player2Score = 0
                    currentRound = 0
                    rematchCurrentRound = 2
                    rematchPartyIndex = 0
                    rematchGameIndex = 0
                    rematchUndoStack = emptyList()
                    isCrawfordGame = false
                    crawfordGamePlayed = false
                    isPostCrawford = false
                    recomposeKey++
                }) {
                    Text("Rovans Turuna Basla", color = Color(0xFF4CAF50))
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showRoundEndDialog = false
                    onFinish()
                }) {
                    Text("Daha Sonra Devam Et", color = Color.Gray)
                }
            }
        )
    }

    // === RÖVANŞLI MOD: Karşılaşma Bitiş Dialog ===
    if (showEncounterEndDialog) {
        AlertDialog(
            onDismissRequest = {},
            title = { Text("Karsilasma Tamamlandi!") },
            text = { Text("Tum turlar ve partiler oynandi.") },
            confirmButton = {
                TextButton(onClick = {
                    showEncounterEndDialog = false
                    val intent = Intent(context, RematchComparisonActivity::class.java)
                    intent.putExtra("encounter_id", encounterId)
                    intent.putExtra("party_index", rematchPartyIndex)
                    intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
                    context.startActivity(intent)
                    onFinish()
                }) {
                    Text("Sonuclari Gor", color = Color(0xFF4CAF50))
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showEncounterEndDialog = false
                    onFinish()
                }) {
                    Text("Ana Menuye Don")
                }
            }
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // Arka plan renk bölümleri (en altta)
        Row(modifier = Modifier.fillMaxSize()) {
            // Sol mavi bölge
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxSize()
                    .background(blueColor)
            )

            // Sağ kırmızı bölge
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxSize()
                    .background(redColor)
            )
        }

        // İçerik (arka planın üzerinde)
        Column(modifier = Modifier.fillMaxSize()) {
            // Oyuncu bilgileri (bilgi barı dahil)
            Row(modifier = Modifier.weight(1f)) {
                // Oyuncu 1 bilgileri
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .padding(8.dp)
                ) {
                    // Üst kısım - Bilgi barı + İsim ve Skor
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Bilgi satırı (sol taraf)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = gameDisplayId,
                                color = if (isRematchMode) Color(0xFFFFD54F) else Color(0xFF81D4FA),
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp
                            )
                            if (isRematchMode) {
                                Text(
                                    text = "Tur ${rematchCurrentRound}/2",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp
                                )
                            } else {
                                Text(
                                    text = gameStartDate,
                                    color = Color.LightGray,
                                    fontSize = 10.sp
                                )
                            }
                        }
                        // Oyuncu adı
                        Text(
                            text = "$player1Name ($player1RoundsWon)",
                            color = Color.White,
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Bold
                        )

                        // Skor
                        Text(
                            text = player1Score.toString(),
                            color = Color.White,
                            fontSize = if (isTraditionalGame) 100.sp else 72.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // Alt kısım - Butonlar
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        // Katla butonu - Oyuncu 1 için (sadece Modern tavla, menü kapalıyken)
                        if (!isTraditionalGame &&
                            !showPlayer1DoublingMenu && !showPlayer2DoublingMenu &&
                            (doublingCubePosition == DoublingCubePosition.CENTER ||
                                    doublingCubePosition == DoublingCubePosition.PLAYER1_CONTROL) &&
                            player1CanDouble &&
                            !isCrawfordGame
                        ) {
                            Button(
                                onClick = { player1OfferDouble() },
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFFFFB300)
                                ),
                                contentPadding = PaddingValues(0.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(45.dp)
                            ) {
                                Text(
                                    text = "KATLA",
                                    color = Color.White,
                                    fontSize = 22.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        // Katlama menüsü - Oyuncu 1 için (Katla yerine gösterilir)
                        if (showPlayer1DoublingMenu && isLandscape) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 4.dp),
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Button(
                                    onClick = { player1AcceptDouble() },
                                    shape = RoundedCornerShape(8.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color(0xFF4CAF50)
                                    ),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(60.dp)
                                ) {
                                    Text("KABUL", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                }

                                Button(
                                    onClick = { player1Resign() },
                                    shape = RoundedCornerShape(8.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color(0xFFF44336)
                                    ),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(60.dp)
                                ) {
                                    Text("PES", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                }

                                Button(
                                    onClick = { resetDoublingCube() },
                                    shape = RoundedCornerShape(8.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color(0xFF757575)
                                    ),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(60.dp)
                                ) {
                                    Text("İPTAL", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                }
                            }
                        }
                    }

                }

                // ORTA KISIM - Hedef puan kutusu
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .width(160.dp)
                        .offset(y = (-42).dp)
                ) {
                    // Tarih ve parti bilgisi
                    if (isRematchMode) {
                        Text(
                            text = "$gameStartDate  P${rematchPartyIndex + 1}/$totalParties",
                            color = Color(0xFFCE93D8),
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp
                        )
                    }
                    
                    // Crawford durumu göstergesi (skorboard üstünde)
                    if (isCrawfordGame || isPostCrawford || crawfordGamePlayed) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = when {
                                    isCrawfordGame -> Color.Red.copy(alpha = 0.9f)
                                    isPostCrawford -> Color(0xFF4CAF50).copy(alpha = 0.9f)
                                    else -> Color(0xFF2196F3).copy(alpha = 0.7f)
                                }
                            ),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = when {
                                    isCrawfordGame -> "⚠️ CRAWFORD ELİ"
                                    isPostCrawford -> "🔄 POST-CRAWFORD"
                                    crawfordGamePlayed -> "✅ CRAWFORD OYNANDI"
                                    else -> "📊 CRAWFORD"
                                },
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                color = Color.White,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(2.dp))
                    // Hedef puan kutusu
                    Box(
                        modifier = Modifier
                            .width(100.dp)
                            .height(100.dp),
                        contentAlignment = Alignment.Center
                    ) {
                    // Arka plan bölgeleri
                    Row(modifier = Modifier.fillMaxSize()) {
                        // Sol mavi yarım
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxSize()
                                .background(blueColor)
                        )

                        // Sağ kırmızı yarım
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxSize()
                                .background(redColor)
                        )
                    }

                    // Metin (arka planın üzerinde)
                    Text(
                        text = "$targetRounds",
                        color = Color.White,
                        style = MaterialTheme.typography.displayMedium,
                        fontWeight = FontWeight.Bold
                    )
                    }
                }

                // Oyuncu 2 bilgileri
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .padding(8.dp)
                ) {
                    // Üst kısım - Bilgi barı + İsim ve Skor
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Bilgi satırı (sag taraf)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            if (isRematchMode) {
                                Text(
                                    text = "RÖVANŞLI",
                                    color = Color(0xFFCE93D8),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 10.sp
                                )
                                Text(
                                    text = "El ${rematchGameIndex + 1}/${DiceGenerator.maxSetsForTargetScore(matchTargetScore)}",
                                    color = Color.White,
                                    fontSize = 11.sp
                                )
                            } else {
                                Text(
                                    text = gameType,
                                    color = Color(0xFF81C784),
                                    fontSize = 10.sp
                                )
                                Text(
                                    text = gameStartDate,
                                    color = Color.LightGray,
                                    fontSize = 10.sp
                                )
                            }
                        }
                        // Oyuncu adı
                        Text(
                            text = "$player2Name ($player2RoundsWon)",
                            color = Color.White,
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Bold
                        )

                        // Skor
                        Text(
                            text = player2Score.toString(),
                            color = Color.White,
                            fontSize = if (isTraditionalGame) 100.sp else 72.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // Alt kısım - Butonlar
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        // Katla butonu - Oyuncu 2 için (sadece Modern tavla, menü kapalıyken)
                        if (!isTraditionalGame &&
                            !showPlayer1DoublingMenu && !showPlayer2DoublingMenu &&
                            (doublingCubePosition == DoublingCubePosition.CENTER ||
                                    doublingCubePosition == DoublingCubePosition.PLAYER2_CONTROL) &&
                            player2CanDouble &&
                            !isCrawfordGame
                        ) {
                            Button(
                                onClick = { player2OfferDouble() },
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFFFFB300)
                                ),
                                contentPadding = PaddingValues(0.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(45.dp)
                            ) {
                                Text(
                                    text = "KATLA",
                                    color = Color.White,
                                    fontSize = 22.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        // Katlama menüsü - Oyuncu 2 için (Katla yerine gösterilir)
                        if (showPlayer2DoublingMenu && isLandscape) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 4.dp),
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Button(
                                    onClick = { player2AcceptDouble() },
                                    shape = RoundedCornerShape(8.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color(0xFF4CAF50)
                                    ),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(60.dp)
                                ) {
                                    Text("KABUL", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                }

                                Button(
                                    onClick = { player2Resign() },
                                    shape = RoundedCornerShape(8.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color(0xFFF44336)
                                    ),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(60.dp)
                                ) {
                                    Text("PES", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                }

                                Button(
                                    onClick = { resetDoublingCube() },
                                    shape = RoundedCornerShape(8.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color(0xFF757575)
                                    ),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(60.dp)
                                ) {
                                    Text("İPTAL", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                }
                            }
                        }
                    }
                }
            }

            // Mevcut el ve zar seti bilgisi
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "El: $currentRound",
                    color = Color.White,
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = "Zar Seti: ${getCurrentDiceSetId()}",
                    color = Color(0xFFFFE082),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            
            Spacer(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = if (isTraditionalGame && isScoreAutomatic) 39.dp else if (isTraditionalGame) 24.dp else 4.dp)
            )

            // Skor Artırma Butonları - isScoreAutomatic değerine göre farklı butonlar gösteriyoruz
            if (isScoreAutomatic) {
                if (isTraditionalGame) {
                    // Geleneksel Tavla için T/M Butonları (Backgammon ve Küp olmadan)
                    // Buton renkleri: Sol koyu mavi, sağ koyu kırmızı
                    val leftButtonColor = Color(0xFF1565C0) // Blue 800 - iki ton koyu mavi
                    val rightButtonColor = Color(0xFFAD1457) // Pink 800 - iki ton koyu kırmızı

                    if (isLandscape) {
                        // Yatay mod için - Geleneksel Tavla (4 buton: T M | T M)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(4.dp)
                                .height(85.dp),
                            horizontalArrangement = Arrangement.spacedBy(1.dp), // Kılcal boşluk
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // SOL TARAF - Oyuncu 1 T butonu
                            Button(
                                onClick = { addRound(player1Id, player1Name, "SINGLE", 1) },
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = leftButtonColor),
                                contentPadding = PaddingValues(0.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("T", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                    Text("1P", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                }
                            }

                            // SOL TARAF - Oyuncu 1 M butonu
                            Button(
                                onClick = { addRound(player1Id, player1Name, "MARS", 2) },
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = leftButtonColor),
                                contentPadding = PaddingValues(0.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("M", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                    Text("2P", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                }
                            }

                            // SAĞ TARAF - Oyuncu 2 T butonu
                            Button(
                                onClick = { addRound(player2Id, player2Name, "SINGLE", 1) },
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = rightButtonColor),
                                contentPadding = PaddingValues(0.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("T", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                    Text("1P", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                }
                            }

                            // SAĞ TARAF - Oyuncu 2 M butonu
                            Button(
                                onClick = { addRound(player2Id, player2Name, "MARS", 2) },
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = rightButtonColor),
                                contentPadding = PaddingValues(0.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("M", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                    Text("2P", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                }
                            }
                        }
                    } else {
                        // Dikey mod için orijinal tasarım - Geleneksel Tavla
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // SOL TARAF (MAVİ BÖLGE) BUTONLARI - 2 buton eşit aralıklı
                            Row(
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(horizontal = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceEvenly
                            ) {
                                // Oyuncu 1 - T butonu
                                TooltipBox(
                                    positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
                                    tooltip = {
                                        Surface(
                                            color = Color.Black.copy(alpha = 0.8f),
                                            shape = RoundedCornerShape(4.dp)
                                        ) {
                                            Text(
                                                text = "Tek Oyun",
                                                color = Color.White,
                                                modifier = Modifier.padding(8.dp)
                                            )
                                        }
                                    },
                                    state = rememberTooltipState(isPersistent = false)
                                ) {
                                    Button(
                                        onClick = { addRound(player1Id, player1Name, "SINGLE", 1) },
                                        shape = RoundedCornerShape(4.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = Color.White.copy(alpha = 0.3f)
                                        ),
                                        modifier = Modifier
                                            .weight(1f) // Eşit ağırlık
                                            .height(60.dp)
                                            .padding(horizontal = 4.dp)
                                    ) {
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                            Text(
                                                text = "T",
                                                fontSize = 18.sp,
                                                fontWeight = FontWeight.Bold,
                                                textAlign = TextAlign.Center,
                                                color = Color.White
                                            )
                                            Text(
                                                text = "1P",
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                textAlign = TextAlign.Center,
                                                color = Color.White
                                            )
                                        }
                                    }
                                }

                                // Oyuncu 1 - M butonu
                                TooltipBox(
                                    positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
                                    tooltip = {
                                        Surface(
                                            color = Color.Black.copy(alpha = 0.8f),
                                            shape = RoundedCornerShape(4.dp)
                                        ) {
                                            Text(
                                                text = "Mars",
                                                color = Color.White,
                                                modifier = Modifier.padding(8.dp)
                                            )
                                        }
                                    },
                                    state = rememberTooltipState(isPersistent = false)
                                ) {
                                    Button(
                                        onClick = { addRound(player1Id, player1Name, "MARS", 2) },
                                        shape = RoundedCornerShape(4.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = Color.White.copy(alpha = 0.3f)
                                        ),
                                        modifier = Modifier
                                            .weight(1f) // Eşit ağırlık
                                            .height(60.dp)
                                            .padding(horizontal = 4.dp)
                                    ) {
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                            Text(
                                                text = "M",
                                                fontSize = 18.sp,
                                                fontWeight = FontWeight.Bold,
                                                textAlign = TextAlign.Center,
                                                color = Color.White
                                            )
                                            Text(
                                                text = "2P",
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                textAlign = TextAlign.Center,
                                                color = Color.White
                                            )
                                        }
                                    }
                                }
                            }

                            // SAĞ TARAF (KIRMIZI BÖLGE) BUTONLARI - 2 buton eşit aralıklı
                            Row(
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(horizontal = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceEvenly
                            ) {
                                // Oyuncu 2 - T butonu
                                TooltipBox(
                                    positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
                                    tooltip = {
                                        Surface(
                                            color = Color.Black.copy(alpha = 0.8f),
                                            shape = RoundedCornerShape(4.dp)
                                        ) {
                                            Text(
                                                text = "Tek Oyun",
                                                color = Color.White,
                                                modifier = Modifier.padding(8.dp)
                                            )
                                        }
                                    },
                                    state = rememberTooltipState(isPersistent = false)
                                ) {
                                    Button(
                                        onClick = { addRound(player2Id, player2Name, "SINGLE", 1) },
                                        shape = RoundedCornerShape(4.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = Color.White.copy(alpha = 0.3f)
                                        ),
                                        modifier = Modifier
                                            .weight(1f) // Eşit ağırlık
                                            .height(60.dp)
                                            .padding(horizontal = 4.dp)
                                    ) {
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                            Text(
                                                text = "T",
                                                fontSize = 18.sp,
                                                fontWeight = FontWeight.Bold,
                                                textAlign = TextAlign.Center,
                                                color = Color.White
                                            )
                                            Text(
                                                text = "1P",
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                textAlign = TextAlign.Center,
                                                color = Color.White
                                            )
                                        }
                                    }
                                }

                                // Oyuncu 2 - M butonu
                                TooltipBox(
                                    positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
                                    tooltip = {
                                        Surface(
                                            color = Color.Black.copy(alpha = 0.8f),
                                            shape = RoundedCornerShape(4.dp)
                                        ) {
                                            Text(
                                                text = "Mars",
                                                color = Color.White,
                                                modifier = Modifier.padding(8.dp)
                                            )
                                        }
                                    },
                                    state = rememberTooltipState(isPersistent = false)
                                ) {
                                    Button(
                                        onClick = { addRound(player2Id, player2Name, "MARS", 2) },
                                        shape = RoundedCornerShape(4.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = Color.White.copy(alpha = 0.3f)
                                        ),
                                        modifier = Modifier
                                            .weight(1f) // Eşit ağırlık
                                            .height(60.dp)
                                            .padding(horizontal = 4.dp)
                                    ) {
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                            Text(
                                                text = "M",
                                                fontSize = 18.sp,
                                                fontWeight = FontWeight.Bold,
                                                textAlign = TextAlign.Center,
                                                color = Color.White
                                            )
                                            Text(
                                                text = "2P",
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                textAlign = TextAlign.Center,
                                                color = Color.White
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                } else {
                    // Modern Tavla için T/M/B Butonları
                    if (isLandscape) {
                        // Yatay mod için özel büyük buton tasarımı - Modern Tavla
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(4.dp)
                                .height(85.dp), // Yükseklik daha basık
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Buton renkleri: Sol koyu mavi, sağ koyu kırmızı
                            val leftButtonColor = Color(0xFF1565C0) // Blue 800 - iki ton koyu mavi
                            val rightButtonColor = Color(0xFFAD1457) // Pink 800 - iki ton koyu kırmızı

                            // SOL TARAF (MAVİ BÖLGE) - T butonu
                            Button(
                                onClick = { addRound(player1Id, player1Name, "SINGLE", 1) },
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = leftButtonColor),
                                contentPadding = PaddingValues(0.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .padding(end = 1.dp) // Kılcal boşluk
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("T", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                    Text("1P", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                }
                            }

                            // SOL TARAF - M butonu
                            Button(
                                onClick = { addRound(player1Id, player1Name, "MARS", 2) },
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = leftButtonColor),
                                contentPadding = PaddingValues(0.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .padding(horizontal = 1.dp) // Kılcal boşluk
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("M", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                    Text("2P", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                }
                            }

                            // SOL TARAF - B butonu
                            Button(
                                onClick = { addRound(player1Id, player1Name, "BACKGAMMON", 3) },
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = leftButtonColor),
                                contentPadding = PaddingValues(0.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .padding(horizontal = 1.dp) // Kılcal boşluk
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("B", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                    Text("3P", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                }
                            }

                            // SAĞ TARAF (KIRMIZI BÖLGE) - T butonu
                            Button(
                                onClick = { addRound(player2Id, player2Name, "SINGLE", 1) },
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = rightButtonColor),
                                contentPadding = PaddingValues(0.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .padding(horizontal = 1.dp) // Kılcal boşluk
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("T", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                    Text("1P", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                }
                            }

                            // SAĞ TARAF - M butonu
                            Button(
                                onClick = { addRound(player2Id, player2Name, "MARS", 2) },
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = rightButtonColor),
                                contentPadding = PaddingValues(0.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .padding(horizontal = 1.dp) // Kılcal boşluk
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("M", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                    Text("2P", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                }
                            }

                            // SAĞ TARAF - B butonu
                            Button(
                                onClick = { addRound(player2Id, player2Name, "BACKGAMMON", 3) },
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = rightButtonColor),
                                contentPadding = PaddingValues(0.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .padding(start = 1.dp) // Kılcal boşluk
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("B", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                    Text("3P", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                }
                            }
                        }
                    } else {
                        // Dikey mod için orijinal tasarım - Modern Tavla
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // SOL TARAF (MAVİ BÖLGE) BUTONLARI
                            Row(
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(horizontal = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Oyuncu 1 - T butonu
                                TooltipBox(
                                    positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
                                    tooltip = {
                                        Surface(
                                            color = Color.Black.copy(alpha = 0.8f),
                                            shape = RoundedCornerShape(4.dp)
                                        ) {
                                            Text(
                                                text = "Tek Oyun",
                                                color = Color.White,
                                                modifier = Modifier.padding(8.dp)
                                            )
                                        }
                                    },
                                    state = rememberTooltipState(isPersistent = false)
                                ) {
                                    Button(
                                        onClick = { addRound(player1Id, player1Name, "SINGLE", 1) },
                                        shape = RoundedCornerShape(4.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = Color.White.copy(alpha = 0.3f)
                                        ),
                                        modifier = Modifier
                                            .weight(1f) // Eşit ağırlık
                                            .height(60.dp)
                                            .padding(horizontal = 2.dp)
                                    ) {
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                            // Büyük harf
                                            Text(
                                                text = "T",
                                                fontSize = 18.sp,
                                                fontWeight = FontWeight.Bold,
                                                textAlign = TextAlign.Center,
                                                color = Color.White
                                            )

                                            // Puan
                                            Text(
                                                text = "1P",
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                textAlign = TextAlign.Center,
                                                color = Color.White
                                            )
                                        }
                                    }
                                }

                                // Oyuncu 1 - M butonu
                                TooltipBox(
                                    positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
                                    tooltip = {
                                        Surface(
                                            color = Color.Black.copy(alpha = 0.8f),
                                            shape = RoundedCornerShape(4.dp)
                                        ) {
                                            Text(
                                                text = "Mars",
                                                color = Color.White,
                                                modifier = Modifier.padding(8.dp)
                                            )
                                        }
                                    },
                                    state = rememberTooltipState(isPersistent = false)
                                ) {
                                    Button(
                                        onClick = { addRound(player1Id, player1Name, "MARS", 2) },
                                        shape = RoundedCornerShape(4.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = Color.White.copy(alpha = 0.3f)
                                        ),
                                        modifier = Modifier
                                            .weight(1f) // Eşit ağırlık
                                            .height(60.dp)
                                            .padding(horizontal = 2.dp)
                                    ) {
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                            // Büyük harf
                                            Text(
                                                text = "M",
                                                fontSize = 18.sp,
                                                fontWeight = FontWeight.Bold,
                                                textAlign = TextAlign.Center,
                                                color = Color.White
                                            )

                                            // Puan
                                            Text(
                                                text = "2P",
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                textAlign = TextAlign.Center,
                                                color = Color.White
                                            )
                                        }
                                    }
                                }

                                // Oyuncu 1 - B butonu
                                TooltipBox(
                                    positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
                                    tooltip = {
                                        Surface(
                                            color = Color.Black.copy(alpha = 0.8f),
                                            shape = RoundedCornerShape(4.dp)
                                        ) {
                                            Text(
                                                text = "Backgammon",
                                                color = Color.White,
                                                modifier = Modifier.padding(8.dp)
                                            )
                                        }
                                    },
                                    state = rememberTooltipState(isPersistent = false)
                                ) {
                                    Button(
                                        onClick = {
                                            addRound(
                                                player1Id,
                                                player1Name,
                                                "BACKGAMMON",
                                                3
                                            )
                                        },
                                        shape = RoundedCornerShape(4.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = Color.White.copy(alpha = 0.3f)
                                        ),
                                        modifier = Modifier
                                            .weight(1f) // Eşit ağırlık
                                            .height(60.dp)
                                            .padding(horizontal = 2.dp)
                                    ) {
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                            // Büyük harf
                                            Text(
                                                text = "B",
                                                fontSize = 18.sp,
                                                fontWeight = FontWeight.Bold,
                                                textAlign = TextAlign.Center,
                                                color = Color.White
                                            )

                                            // Puan
                                            Text(
                                                text = "3P",
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                textAlign = TextAlign.Center,
                                                color = Color.White
                                            )
                                        }
                                    }
                                }
                            }

                            // SAĞ TARAF (KIRMIZI BÖLGE) BUTONLARI
                            Row(
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(horizontal = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Oyuncu 2 - T butonu
                                TooltipBox(
                                    positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
                                    tooltip = {
                                        Surface(
                                            color = Color.Black.copy(alpha = 0.8f),
                                            shape = RoundedCornerShape(4.dp)
                                        ) {
                                            Text(
                                                text = "Tek Oyun",
                                                color = Color.White,
                                                modifier = Modifier.padding(8.dp)
                                            )
                                        }
                                    },
                                    state = rememberTooltipState(isPersistent = false)
                                ) {
                                    Button(
                                        onClick = { addRound(player2Id, player2Name, "SINGLE", 1) },
                                        shape = RoundedCornerShape(4.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = Color.White.copy(alpha = 0.3f)
                                        ),
                                        modifier = Modifier
                                            .weight(1f) // Eşit ağırlık
                                            .height(60.dp)
                                            .padding(horizontal = 2.dp)
                                    ) {
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                            // Büyük harf
                                            Text(
                                                text = "T",
                                                fontSize = 18.sp,
                                                fontWeight = FontWeight.Bold,
                                                textAlign = TextAlign.Center,
                                                color = Color.White
                                            )

                                            // Puan
                                            Text(
                                                text = "1P",
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                textAlign = TextAlign.Center,
                                                color = Color.White
                                            )
                                        }
                                    }
                                }

                                // Oyuncu 2 - M butonu
                                TooltipBox(
                                    positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
                                    tooltip = {
                                        Surface(
                                            color = Color.Black.copy(alpha = 0.8f),
                                            shape = RoundedCornerShape(4.dp)
                                        ) {
                                            Text(
                                                text = "Mars",
                                                color = Color.White,
                                                modifier = Modifier.padding(8.dp)
                                            )
                                        }
                                    },
                                    state = rememberTooltipState(isPersistent = false)
                                ) {
                                    Button(
                                        onClick = { addRound(player2Id, player2Name, "MARS", 2) },
                                        shape = RoundedCornerShape(4.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = Color.White.copy(alpha = 0.3f)
                                        ),
                                        modifier = Modifier
                                            .weight(1f) // Eşit ağırlık
                                            .height(60.dp)
                                            .padding(horizontal = 2.dp)
                                    ) {
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                            // Büyük harf
                                            Text(
                                                text = "M",
                                                fontSize = 18.sp,
                                                fontWeight = FontWeight.Bold,
                                                textAlign = TextAlign.Center,
                                                color = Color.White
                                            )

                                            // Puan
                                            Text(
                                                text = "2P",
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                textAlign = TextAlign.Center,
                                                color = Color.White
                                            )
                                        }
                                    }
                                }

                                // Oyuncu 2 - B butonu
                                TooltipBox(
                                    positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
                                    tooltip = {
                                        Surface(
                                            color = Color.Black.copy(alpha = 0.8f),
                                            shape = RoundedCornerShape(4.dp)
                                        ) {
                                            Text(
                                                text = "Backgammon",
                                                color = Color.White,
                                                modifier = Modifier.padding(8.dp)
                                            )
                                        }
                                    },
                                    state = rememberTooltipState(isPersistent = false)
                                ) {
                                    Button(
                                        onClick = {
                                            addRound(
                                                player2Id,
                                                player2Name,
                                                "BACKGAMMON",
                                                3
                                            )
                                        },
                                        shape = RoundedCornerShape(4.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = Color.White.copy(alpha = 0.3f)
                                        ),
                                        modifier = Modifier
                                            .weight(1f) // Eşit ağırlık
                                            .height(60.dp)
                                            .padding(horizontal = 2.dp)
                                    ) {
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                            // Büyük harf
                                            Text(
                                                text = "B",
                                                fontSize = 18.sp,
                                                fontWeight = FontWeight.Bold,
                                                textAlign = TextAlign.Center,
                                                color = Color.White
                                            )

                                            // Puan
                                            Text(
                                                text = "3P",
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                textAlign = TextAlign.Center,
                                                color = Color.White
                                            )
                                        }
                                    }
                                }

                            }
                        }
                    }
                }
            }

            // Manuel skor artırma butonları - Sadece manuel skor modunda göster
            if (!isScoreAutomatic) {
                Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 2.dp), // Daha az padding
                horizontalArrangement = Arrangement.spacedBy(4.dp) // Butonlar arası minimal boşluk
            ) {
                // Sol taraf (Mavi) - Artı buton
                Button(
                    onClick = {
                        // Skoru manuel olarak artır
                        player1Score++
                        
                        // Hedef puana ulaşıldığında maçı bitir
                        if (player1Score >= matchTargetScore) {
                            endMatch()
                        }
                    },
                    shape = RoundedCornerShape(4.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.3f)),
                    modifier = Modifier
                        .weight(1f)
                        .height(45.dp) // Daha küçük yükseklik
                        .padding(horizontal = 1.dp) // Minimal padding
                ) {
                    Text(
                        text = "+",
                        fontSize = 18.sp, // Daha küçük font
                        textAlign = TextAlign.Center,
                        color = Color.White
                    )
                }

                // Sol taraf (Mavi) - Eksi buton
                Button(
                    onClick = {
                        // Skoru manuel olarak azalt
                        if (player1Score > 0) player1Score--
                    },
                    shape = RoundedCornerShape(4.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.3f)),
                    modifier = Modifier
                        .weight(1f)
                        .height(45.dp)
                        .padding(horizontal = 1.dp)
                ) {
                    Text(
                        text = "-",
                        fontSize = 18.sp,
                        textAlign = TextAlign.Center,
                        color = Color.White
                    )
                }

                // Ortadaki temizle butonu (Mor)
                Button(
                    onClick = {
                        // Skorları sıfırla
                        player1Score = 0
                        player2Score = 0
                        // Crawford değişkenlerini sıfırla
                        isCrawfordGame = false
                        crawfordGamePlayed = false
                        isPostCrawford = false
                    },
                    shape = RoundedCornerShape(4.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = purpleColor),
                    modifier = Modifier
                        .weight(1f)
                        .height(45.dp)
                        .padding(horizontal = 1.dp)
                ) {
                    Text(
                        text = "C",
                        fontSize = 18.sp,
                        textAlign = TextAlign.Center,
                        color = Color.White
                    )
                }

                // Sağ taraf (Kırmızı) - Eksi buton
                Button(
                    onClick = {
                        // Skoru manuel olarak azalt
                        if (player2Score > 0) player2Score--
                    },
                    shape = RoundedCornerShape(4.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.3f)),
                    modifier = Modifier
                        .weight(1f)
                        .height(45.dp)
                        .padding(horizontal = 1.dp)
                ) {
                    Text(
                        text = "-",
                        fontSize = 18.sp,
                        textAlign = TextAlign.Center,
                        color = Color.White
                    )
                }

                // Sağ taraf (Kırmızı) - Artı buton
                Button(
                    onClick = {
                        // Skoru manuel olarak artır
                        player2Score++
                        
                        // Hedef puana ulaşıldığında maçı bitir
                        if (player2Score >= matchTargetScore) {
                            endMatch()
                        }
                    },
                    shape = RoundedCornerShape(4.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.3f)),
                    modifier = Modifier
                        .weight(1f)
                        .height(45.dp)
                        .padding(horizontal = 1.dp)
                ) {
                    Text(
                        text = "+",
                        fontSize = 18.sp,
                        textAlign = TextAlign.Center,
                        color = Color.White
                    )
                }
            }
            }

            // ✅ Geri alma, zar atma ve maçı sonlandırma butonları - aynı satırda
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Geri al butonu - Her modda göster
                Button(
                    onClick = { undoLastRound() },
                    enabled = if (isRematchMode) rematchUndoStack.isNotEmpty() else undoStack.size > 0,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF0D47A1), // Koyu mavi (Blue 900)
                        disabledContainerColor = Color(0xFF1565C0).copy(alpha = 0.5f) // Soluk koyu mavi (pasif)
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text("↶", fontSize = 20.sp, color = Color.White)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Geri Al", color = Color.White, fontSize = 12.sp)
                    }
                }

                // Geleneksel modda kaydet butonu, Modern modda zar/saat butonu
                if (isTraditionalGame) {
                    // Bu Eli Kaydet butonu - Yeşil renk
                    Button(
                        onClick = {
                            
                            // Mevcut puanları kaydet
                            if (player1Score > player2Score) {
                                // Oyuncu 1 kazandı
                                val difference = player1Score - player2Score
                                val winType = when {
                                    difference >= 2 -> "MARS"
                                    else -> "SINGLE"
                                }
                                addRound(player1Id, player1Name, winType, difference)
                            } else if (player2Score > player1Score) {
                                // Oyuncu 2 kazandı
                                val difference = player2Score - player1Score
                                val winType = when {
                                    difference >= 2 -> "MARS"
                                    else -> "SINGLE"
                                }
                                addRound(player2Id, player2Name, winType, difference)
                            } else {
                                Toast.makeText(
                                    context,
                                    "Skorlar eşit, kazanan yok!",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }

                            // Skorları sıfırla
                            player1Score = 0
                            player2Score = 0
                            
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF2E7D32) // Yeşil renk
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Text("💾 Bu Eli Kaydet", color = Color.White, fontSize = 12.sp, maxLines = 1)
                        }
                    }
                } else {
                    // Modern mod - Zar/Saat veya Rövanş butonu
                    if (isRematchMode) {
                        Button(
                            onClick = {
                                incrementDiceSet() // ✅ Zar setini artır
                                val intent = Intent(context, RematchDiceDisplayActivity::class.java).apply {
                                    putExtra("encounter_id", encounterId)
                                    // ✅ KATLAMA SİSTEMİ PARAMETRELERİ
                                    putExtra("doubling_cube_value", doublingCubeValue)
                                    putExtra("player1_can_double", player1CanDouble)
                                    putExtra("player2_can_double", player2CanDouble)
                                    putExtra("is_crawford_game", isCrawfordGame)
                                    putExtra("player1_name", player1Name)
                                    putExtra("player2_name", player2Name)
                                    putExtra("player1_id", player1Id)
                                    putExtra("player2_id", player2Id)
                                }
                                rematchDiceLauncher.launch(intent)
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF6A1B9A) // Mor
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .height(50.dp)
                        ) {
                            Text("🎲 Zarlar", color = Color.White, fontSize = 12.sp)
                        }
                    } else if (useDiceRoller || useTimer) {
                        val buttonIcon = when {
                            useDiceRoller && useTimer -> "🎲⏰"
                            useDiceRoller -> "🎲"
                            useTimer -> "⏰"
                            else -> "🎲"
                        }
                        val buttonText = when {
                            useDiceRoller && useTimer -> "Zar/Saat"
                            useDiceRoller -> "Zar At"
                            useTimer -> "Saat Kullan"
                            else -> "Zar At"
                        }

                        Button(
                            onClick = {
                                incrementDiceSet() // ✅ Zar setini artır
                                // ✅ Activity Launcher ile aç
                                val intent = Intent(context, DiceActivity::class.java).apply {
                                    putExtra("game_type", gameType)
                                    putExtra("use_dice_roller", useDiceRoller)
                                    putExtra("use_timer", useTimer)
                                    putExtra("player1_name", player1Name)
                                    putExtra("player2_name", player2Name)
                                    putExtra("match_length", targetRounds)
                                    putExtra("match_id", matchId)
                                    putExtra("player1_id", player1Id)
                                    putExtra("player2_id", player2Id)
                                    putExtra("keep_statistics", keepStatistics)
                                    putExtra("mark_dice_evaluation", markDiceEvaluation)
                                    // ✅ KATLAMA SİSTEMİ PARAMETRELERİ
                                    putExtra("doubling_cube_value", doublingCubeValue)
                                    putExtra("player1_can_double", player1CanDouble)
                                    putExtra("player2_can_double", player2CanDouble)
                                    putExtra("is_crawford_game", isCrawfordGame)
                                    putExtra("show_player1_doubling_menu", showPlayer1DoublingMenu)
                                    putExtra("show_player2_doubling_menu", showPlayer2DoublingMenu)
                                    putExtra("doubling_cube_position", doublingCubePosition.name)
                                }
                                diceActivityLauncher.launch(intent)
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF9C27B0) // Mor renk
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .height(50.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Text(buttonIcon, fontSize = 16.sp)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(buttonText, color = Color.White, fontSize = 12.sp)
                            }
                        }
                    }
                    
                    // ✅ ZAR SETLERİNİ GÖRÜNTÜLE BUTONU
                    if (diceSetHistory.isNotEmpty()) {
                        Button(
                            onClick = { 
                                showDiceSetHistoryDialog = true 
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF2196F3) // Açık mavi
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .height(50.dp)
                        ) {
                            Text("📋 Zar Setleri", color = Color.White, fontSize = 11.sp)
                        }
                    }
                }

                // Hareketler Dökümü butonu - orta hattın sağında
                Button(
                    onClick = { showActivityLogDialog = true },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF616161) // Gri
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp)
                ) {
                    Text("📋 Döküm", color = Color.White, fontSize = 12.sp)
                }

                // Zar İstatistikleri butonu - Sadece keepStatistics true ise göster
                if (keepStatistics) {
                    Button(
                        onClick = {
                            if (keepStatistics && markDiceEvaluation && !useDiceRoller) {
                                // Manuel zar değerlendirme ekranına git
                                val intent = Intent(context, DiceProcessingActivity::class.java).apply {
                                    putExtra("match_id", matchId)
                                    putExtra("player1_id", player1Id)
                                    putExtra("player2_id", player2Id)
                                    putExtra("player1_name", player1Name)
                                    putExtra("player2_name", player2Name)
                                    putExtra("current_player", player1Id)
                                    putExtra("current_player_name", player1Name)
                                }
                                context.startActivity(intent)
                            } else {
                                // Normal zar istatistikleri ekranına git
                                val intent = Intent(context, DiceStatisticsActivity::class.java).apply {
                                    putExtra("match_id", matchId)
                                    putExtra("player1_id", player1Id)
                                    putExtra("player2_id", player2Id)
                                    putExtra("player1_name", player1Name)
                                    putExtra("player2_name", player2Name)
                                }
                                context.startActivity(intent)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF1976D2) // Mavi renk
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp)
                    ) {
                        Text("📊 Zar İst.", color = Color.White, fontSize = 12.sp)
                    }
                }

                // Maçı sonlandırma butonu - Koyu kırmızı
                Button(
                    onClick = {
                        // Rövanşlı karşılaşmalarda ek güvenlik sorusu
                        if (isRematchMode) {
                            showEndMatchConfirmation = true
                        } else {
                            showEndMatchConfirmation = true
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFB71C1C) // Koyu kırmızı (iki ton koyu)
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp)
                ) {
                    Text("Maçı Sonlandır", color = Color.White, fontSize = 12.sp)
                }
            }
        }

        // Katlama Zarı - Sadece Modern tavla için görünür
        if (!isTraditionalGame) {
            Box(
                modifier = Modifier
                    .offset(x = animatedXOffset, y = animatedYOffset)
                    .size(60.dp) // Eski boyutuna döndürüldü
                    .align(Alignment.Center)
                    .background(Color.White, RoundedCornerShape(8.dp))
                    .border(2.dp, Color.Black, RoundedCornerShape(8.dp))
                    .clickable(enabled = !isCrawfordGame) {
                        // Crawford elinde küp tıklama devre dışı
                        if (!isCrawfordGame) {
                            // Zarın pozisyonuna göre tıklama işlevi
                            when (doublingCubePosition) {
                                DoublingCubePosition.PLAYER1_CONTROL -> if (player1CanDouble) player1OfferDouble()
                                DoublingCubePosition.PLAYER2_CONTROL -> if (player2CanDouble) player2OfferDouble()
                                else -> {} // Diğer pozisyonlarda tıklama işlevi yok
                            }
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                // Crawford göstergesi ekle
                if (isCrawfordGame) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "CRAWFORD",
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Red
                        )
                        Text(
                            text = doublingCubeValue.toString(),
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Gray // Crawford'da gri renk
                        )
                    }
                } else {
                    Text(
                        text = doublingCubeValue.toString(),
                        fontSize = 24.sp, // Eski yazı boyutuna döndürüldü
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )
                }
            }
        }
    }

    // Zar atma ekranı - Yeni tam sayfa Activity'ye yönlendirme
    if (showDiceScreen) {
        DisposableEffect(Unit) {
            try {
                val intent = Intent(context, DiceActivity::class.java).apply {
                    putExtra("game_type", gameType)
                    putExtra("use_dice_roller", useDiceRoller)
                    putExtra("use_timer", useTimer)
                    putExtra("player1_name", player1Name)
                    putExtra("player2_name", player2Name)
                    putExtra("match_length", targetRounds)
                    putExtra("match_id", matchId)
                    putExtra("player1_id", player1Id)
                    putExtra("player2_id", player2Id)
                    putExtra("keep_statistics", keepStatistics)
                    putExtra("mark_dice_evaluation", markDiceEvaluation)
                    // ✅ KATLAMA SİSTEMİ PARAMETRELERİ
                    putExtra("doubling_cube_value", doublingCubeValue)
                    putExtra("player1_can_double", player1CanDouble)
                    putExtra("player2_can_double", player2CanDouble)
                    putExtra("is_crawford_game", isCrawfordGame)
                    putExtra("show_player1_doubling_menu", showPlayer1DoublingMenu)
                    putExtra("show_player2_doubling_menu", showPlayer2DoublingMenu)
                }
                context.startActivity(intent)
                Toast.makeText(context, "Zar ekranı açılıyor...", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(context, "Zar ekranı açılamadı: ${e.message}", Toast.LENGTH_LONG).show()
            }

            onDispose {
                // Activity açıldığında state'i sıfırla
            }
        }

        // State'i hemen sıfırla
        showDiceScreen = false
    }

    // ✅ ZAR SETİ GEÇMİŞİ DİALOGU - Yeniden Tasarım
    if (showDiceSetHistoryDialog) {
        AlertDialog(
            onDismissRequest = { showDiceSetHistoryDialog = false },
            title = {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "🎲 Zar Setleri",
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White,
                            fontSize = 18.sp
                        )
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = Color(0xFF1976D2).copy(alpha = 0.8f)
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                text = "${diceSetHistory.size} adet",
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    if (isRematchMode) {
                        Card(
                            modifier = Modifier.padding(top = 8.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = Color(0xFF0D47A1).copy(alpha = 0.6f)
                            ),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = "📊 21 Zar Seti Ön-üretilmiş • P1-O1-S1 Format • Round 2 Reverse Play",
                                modifier = Modifier.padding(8.dp),
                                fontSize = 11.sp,
                                color = Color(0xFFBBDEFB),
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            },
            text = {
                LazyColumn {
                    items(diceSetHistory) { setId ->
                        val index = diceSetHistory.indexOf(setId)
                        val isCurrentSet = setId == getCurrentDiceSetId()
                        val parsedId = DiceGenerator.parseSetId(setId)
                        val displayText = if (parsedId != null) {
                            val (partyIdx, gameIdx, setIdx) = parsedId
                            "$setId (Parti ${partyIdx+1}, El ${gameIdx+1}, Set ${setIdx+1})"
                        } else {
                            setId
                        }
                        
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = if (isCurrentSet) 
                                    Color(0xFF1976D2).copy(alpha = 0.9f) else Color(0xFF424242).copy(alpha = 0.8f)
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 3.dp)
                                .clickable {
                                    // ✅ Zar setini rövanşlı ekranda oynat
                                    showDiceSetHistoryDialog = false
                                    if (isRematchMode) {
                                        // Rövanşlı mod - RematchDiceDisplayActivity kullan
                                        val intent = Intent(context, RematchDiceDisplayActivity::class.java).apply {
                                            putExtra("encounter_id", encounterId)
                                            putExtra("replay_dice_set_id", setId)
                                            putExtra("doubling_cube_value", doublingCubeValue)
                                            putExtra("player1_can_double", player1CanDouble)
                                            putExtra("player2_can_double", player2CanDouble)
                                            putExtra("is_crawford_game", isCrawfordGame)
                                            putExtra("player1_name", player1Name)
                                            putExtra("player2_name", player2Name)
                                            putExtra("player1_id", player1Id)
                                            putExtra("player2_id", player2Id)
                                            
                                            // Hamle pozisyonu bilgilerini ekle
                                            val gameState = diceSetGameStates[setId]
                                            if (gameState != null) {
                                                putExtra("resume_total_move_count", gameState.totalMoveCount)
                                                putExtra("resume_left_move_index", gameState.leftMoveIndex)
                                                putExtra("resume_right_move_index", gameState.rightMoveIndex)
                                                putExtra("resume_current_player_turn", gameState.currentPlayerTurn)
                                                putExtra("resume_game_phase", gameState.gamePhase)
                                            }
                                        }
                                        rematchDiceLauncher.launch(intent)
                                    } else {
                                        // Normal mod - DiceActivity kullan
                                        val intent = Intent(context, DiceActivity::class.java).apply {
                                            putExtra("game_type", gameType)
                                            putExtra("use_dice_roller", useDiceRoller)
                                            putExtra("use_timer", useTimer)
                                            putExtra("player1_name", player1Name)
                                            putExtra("player2_name", player2Name)
                                            putExtra("match_length", targetRounds)
                                            putExtra("match_id", matchId)
                                            putExtra("player1_id", player1Id)
                                            putExtra("player2_id", player2Id)
                                            putExtra("keep_statistics", keepStatistics)
                                            putExtra("mark_dice_evaluation", markDiceEvaluation)
                                            putExtra("doubling_cube_value", doublingCubeValue)
                                            putExtra("player1_can_double", player1CanDouble)
                                            putExtra("player2_can_double", player2CanDouble)
                                            putExtra("is_crawford_game", isCrawfordGame)
                                            putExtra("show_player1_doubling_menu", showPlayer1DoublingMenu)
                                            putExtra("show_player2_doubling_menu", showPlayer2DoublingMenu)
                                            putExtra("doubling_cube_position", doublingCubePosition.name)
                                            putExtra("replay_dice_set_id", setId)
                                            
                                            // Hamle pozisyonu bilgilerini ekle
                                            val gameState = diceSetGameStates[setId]
                                            if (gameState != null) {
                                                putExtra("resume_total_move_count", gameState.totalMoveCount)
                                                putExtra("resume_left_move_index", gameState.leftMoveIndex)
                                                putExtra("resume_right_move_index", gameState.rightMoveIndex)
                                                putExtra("resume_current_player_turn", gameState.currentPlayerTurn)
                                                putExtra("resume_game_phase", gameState.gamePhase)
                                            }
                                        }
                                        diceActivityLauncher.launch(intent)
                                    }
                                },
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    modifier = Modifier.weight(1f),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Set numarası badge
                                    Card(
                                        colors = CardDefaults.cardColors(
                                            containerColor = if (isCurrentSet) 
                                                Color(0xFFFFEB3B).copy(alpha = 0.9f) 
                                            else Color(0xFF9E9E9E).copy(alpha = 0.7f)
                                        ),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier.fillMaxSize(),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = "${index + 1}",
                                                color = if (isCurrentSet) Color.Black else Color.White,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.ExtraBold
                                            )
                                        }
                                    }
                                    
                                    Spacer(modifier = Modifier.width(12.dp))
                                    
                                    // Set bilgileri
                                    Column {
                                        Text(
                                            text = setId,
                                            color = if (isCurrentSet) Color.Yellow else Color.White,
                                            fontSize = 14.sp,
                                            fontWeight = if (isCurrentSet) FontWeight.Bold else FontWeight.Medium
                                        )
                                        if (parsedId != null) {
                                            val (partyIdx, gameIdx, setIdx) = parsedId
                                            Text(
                                                text = "P${partyIdx+1} • O${gameIdx+1} • S${setIdx+1}",
                                                color = Color(0xFF90CAF9),
                                                fontSize = 10.sp
                                            )
                                        }
                                        if (isCurrentSet) {
                                            Text(
                                                text = "⚡ Aktif Set",
                                                color = Color(0xFFFFEB3B),
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                        
                                        // Hamle pozisyonu bilgisi
                                        val gameState = diceSetGameStates[setId]
                                        if (gameState != null && gameState.totalMoveCount > 0) {
                                            Text(
                                                text = if (gameState.isCompleted) {
                                                    "✓ Tamamlandı (${gameState.totalMoveCount} hamle)"
                                                } else {
                                                    "📍 ${gameState.totalMoveCount}. hamlede kaldı"
                                                },
                                                color = if (gameState.isCompleted) Color(0xFF4CAF50) else Color(0xFFFF9800),
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Medium
                                            )
                                        }
                                    }
                                }
                                
                                // Oynat butonu
                                Card(
                                    colors = CardDefaults.cardColors(
                                        containerColor = Color(0xFF4CAF50).copy(alpha = 0.8f)
                                    ),
                                    shape = RoundedCornerShape(20.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "▶️",
                                            fontSize = 12.sp
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "Oynat",
                                            color = Color.White,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                    }
                }
            },
            confirmButton = {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFFF44336).copy(alpha = 0.8f)
                    ),
                    shape = RoundedCornerShape(25.dp),
                    modifier = Modifier.clickable { showDiceSetHistoryDialog = false }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "🚪",
                            fontSize = 14.sp
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Kapat", 
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                }
            },
            containerColor = Color(0xFF1E1E1E)
        )
    }

    // Aktivite sonlandığında yapılacak işlemler
    DisposableEffect(Unit) {
        onDispose {
            // Rövanşlı modda finishMatch çağırma (encounter DB'de kalmalı)
            if (!isRematchMode && !showMatchEndDialog && matchId != -1L) {
                dbHelper.finishMatch(matchId)
            }
        }
    }
}
