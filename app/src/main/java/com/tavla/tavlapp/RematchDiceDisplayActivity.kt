package com.tavla.tavlapp

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.app.Activity
import android.content.Intent
import android.view.WindowManager
import androidx.compose.foundation.Canvas
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat

/**
 * Rovansli Karsilasma zar gosterim ekrani
 * Zarlar sırayla gösterilir: Başlangıç zarları → Kazanan başlar → Sırayla oyuncular
 */
class RematchDiceDisplayActivity : ComponentActivity() {
    private lateinit var dbHelper: DatabaseHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        WindowCompat.setDecorFitsSystemWindows(window, false)
        val controller = WindowInsetsControllerCompat(window, window.decorView)
        controller.hide(WindowInsetsCompat.Type.systemBars())
        controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        dbHelper = DatabaseHelper(this)
        val encounterId = intent.getLongExtra("encounter_id", -1L)

        val doublingCubeValue = intent.getIntExtra("doubling_cube_value", 1)
        val player1CanDouble = intent.getBooleanExtra("player1_can_double", true)
        val player2CanDouble = intent.getBooleanExtra("player2_can_double", true)
        val isCrawfordGame = intent.getBooleanExtra("is_crawford_game", false)
        val player1Name = intent.getStringExtra("player1_name") ?: ""
        val player2Name = intent.getStringExtra("player2_name") ?: ""
        val player1Id = intent.getLongExtra("player1_id", -1L)
        val player2Id = intent.getLongExtra("player2_id", -1L)

        if (encounterId == -1L) {
            Toast.makeText(this, "Karsilasma bulunamadi", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        val sendResult = { resultData: Intent ->
            setResult(Activity.RESULT_OK, resultData)
            finish()
        }

        setContent {
            TavlaAppTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color.Black
                ) {
                    RematchDiceDisplayScreen(
                        dbHelper = dbHelper,
                        encounterId = encounterId,
                        doublingCubeValue = doublingCubeValue,
                        player1CanDouble = player1CanDouble,
                        player2CanDouble = player2CanDouble,
                        isCrawfordGame = isCrawfordGame,
                        player1Name = player1Name,
                        player2Name = player2Name,
                        player1Id = player1Id,
                        player2Id = player2Id,
                        onBack = {
                            val resultIntent = Intent().apply {
                                putExtra("doubling_cube_value", doublingCubeValue)
                                putExtra("player1_can_double", player1CanDouble)
                                putExtra("player2_can_double", player2CanDouble)
                                putExtra("doubling_cube_position", "CENTER")
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

// Katlama zarı pozisyonları
enum class CubePos {
    LEFT_CTRL,    // Sol oyuncunun uhdesinde (en sol)
    LEFT_OFFER,   // Sol oyuncuya teklif (2/5 noktası)
    CENTER,       // Ortada (başlangıç)
    RIGHT_OFFER,  // Sağ oyuncuya teklif (4/5 noktası)
    RIGHT_CTRL    // Sağ oyuncunun uhdesinde (en sağ)
}

@Composable
fun RematchDiceDisplayScreen(
    dbHelper: DatabaseHelper,
    encounterId: Long,
    doublingCubeValue: Int = 1,
    player1CanDouble: Boolean = true,
    player2CanDouble: Boolean = true,
    isCrawfordGame: Boolean = false,
    player1Name: String = "",
    player2Name: String = "",
    player1Id: Long = -1L,
    player2Id: Long = -1L,
    onBack: () -> Unit,
    onDoublingResult: (Intent) -> Unit = {}
) {
    val context = LocalContext.current
    val encounter = remember { mutableStateOf(dbHelper.getRematchEncounter(encounterId)) }

    var currentDiceSet by remember { mutableStateOf<RematchDiceSet?>(null) }
    var gamePhase by remember { mutableStateOf(GamePhase.STARTING_DICE) }
    var leftMoveIndex by remember { mutableStateOf(0) }
    var rightMoveIndex by remember { mutableStateOf(0) }
    var totalMoveCount by remember { mutableStateOf(0) }
    var currentPlayerTurn by remember { mutableStateOf(1) }
    
    // El bitimi puan hesaplama popup
    var showGameEndScoring by remember { mutableStateOf(false) }

    val currentRound = encounter.value?.currentRound ?: 1
    val currentPartyIndex = encounter.value?.currentPartyIndex ?: 0
    val currentGameIndex = encounter.value?.currentGameIndex ?: 0
    val totalParties = encounter.value?.totalParties ?: 100

    val leftPlayerName: String
    val rightPlayerName: String
    val leftPlayerId: Long
    val rightPlayerId: Long

    if (currentRound == 1) {
        leftPlayerName = encounter.value?.player1Name ?: "Oyuncu 1"
        rightPlayerName = encounter.value?.player2Name ?: "Oyuncu 2"
        leftPlayerId = encounter.value?.player1Id ?: 0L
        rightPlayerId = encounter.value?.player2Id ?: 0L
    } else {
        leftPlayerName = encounter.value?.player2Name ?: "Oyuncu 2"
        rightPlayerName = encounter.value?.player1Name ?: "Oyuncu 1"
        leftPlayerId = encounter.value?.player2Id ?: 0L
        rightPlayerId = encounter.value?.player1Id ?: 0L
    }

    // Zar seti yükle (currentRound ile reverse play)
    LaunchedEffect(encounterId, currentPartyIndex, currentGameIndex, currentRound) {
        currentDiceSet = dbHelper.getDiceSetForGame(encounterId, currentPartyIndex, currentGameIndex, currentRound)
        gamePhase = GamePhase.STARTING_DICE
        leftMoveIndex = 0
        rightMoveIndex = 0
        totalMoveCount = 0
        currentDiceSet?.let { diceSet ->
            currentPlayerTurn = diceSet.getFirstPlayer()
        }
    }

    // Parti skorlarını DB'den al
    val partyScores = remember(encounterId, currentPartyIndex, currentRound) {
        dbHelper.getPartyScore(encounterId, currentPartyIndex, currentRound)
    }
    val leftScore = if (currentRound == 1) partyScores.first else partyScores.second
    val rightScore = if (currentRound == 1) partyScores.second else partyScores.first

    val leftStartingDice = currentDiceSet?.startingDicePlayer1
    val rightStartingDice = currentDiceSet?.startingDicePlayer2
    val leftDice = currentDiceSet?.player1Dice
    val rightDice = currentDiceSet?.player2Dice
    val firstPlayer = currentDiceSet?.getFirstPlayer() ?: 1

    val currentDicePair = when (gamePhase) {
        GamePhase.FIRST_MOVE -> {
            val d1 = leftStartingDice ?: 0
            val d2 = rightStartingDice ?: 0
            if (firstPlayer == 1) Pair(d1, d2) else Pair(d2, d1)
        }
        GamePhase.PLAYING -> {
            if (currentPlayerTurn == 1) leftDice?.getOrNull(leftMoveIndex)
            else rightDice?.getOrNull(rightMoveIndex)
        }
        else -> null
    }

    // ✅ KATLAMA ZARI STATE (sol/sağ bazlı)
    val leftIsP1 = currentRound == 1
    var cubeValue by remember { mutableIntStateOf(doublingCubeValue) }
    var leftCanDouble by remember { mutableStateOf(
        if (leftIsP1) player1CanDouble else player2CanDouble
    )}
    var rightCanDouble by remember { mutableStateOf(
        if (leftIsP1) player2CanDouble else player1CanDouble
    )}
    // İptal için önceki durum
    var prevCubeValue by remember { mutableIntStateOf(doublingCubeValue) }
    var prevCubePos by remember { mutableStateOf(CubePos.CENTER) }
    var prevLeftCanDouble by remember { mutableStateOf(true) }
    var prevRightCanDouble by remember { mutableStateOf(true) }
    var cubePos by remember { mutableStateOf(
        when {
            leftCanDouble && rightCanDouble -> CubePos.CENTER
            leftCanDouble && !rightCanDouble -> CubePos.LEFT_CTRL
            !leftCanDouble && rightCanDouble -> CubePos.RIGHT_CTRL
            else -> CubePos.CENTER
        }
    )}

    // Intent oluşturma yardımcısı
    fun makeDoublingIntent(accepted: Long = -1L, resigned: Long = -1L): Intent {
        val p1Can = if (leftIsP1) leftCanDouble else rightCanDouble
        val p2Can = if (leftIsP1) rightCanDouble else leftCanDouble
        val pos = when (cubePos) {
            CubePos.LEFT_CTRL -> if (leftIsP1) "PLAYER1_CONTROL" else "PLAYER2_CONTROL"
            CubePos.RIGHT_CTRL -> if (leftIsP1) "PLAYER2_CONTROL" else "PLAYER1_CONTROL"
            else -> "CENTER"
        }
        return Intent().apply {
            putExtra("doubling_cube_value", cubeValue)
            putExtra("player1_can_double", p1Can)
            putExtra("player2_can_double", p2Can)
            putExtra("doubling_cube_position", pos)
            if (accepted != -1L) putExtra("accepted_player_id", accepted)
            if (resigned != -1L) putExtra("resigned_player_id", resigned)
        }
    }

    // Sonraki zar
    val advanceToNextDice: () -> Unit = {
        when (gamePhase) {
            GamePhase.STARTING_DICE -> {
                gamePhase = GamePhase.FIRST_MOVE
                totalMoveCount = 1
            }
            GamePhase.FIRST_MOVE -> {
                gamePhase = GamePhase.PLAYING
                val secondPlayer = if (firstPlayer == 1) 2 else 1
                currentPlayerTurn = secondPlayer
                totalMoveCount = 2
            }
            GamePhase.PLAYING -> {
                val prevPlayer = currentPlayerTurn
                currentPlayerTurn = if (currentPlayerTurn == 1) 2 else 1
                if (prevPlayer == 1) leftMoveIndex++ else rightMoveIndex++
                totalMoveCount++
            }
        }
    }

    val nextDiceEnabled = leftMoveIndex < DiceGenerator.DICE_PAIRS_PER_SET && rightMoveIndex < DiceGenerator.DICE_PAIRS_PER_SET

    // Sıra renkleri (barlar + arka plan için)
    val turnColorMain = if (currentPlayerTurn == 1) Color(0xFF1565C0) else Color(0xFFC62828)
    val turnColorDark = if (currentPlayerTurn == 1) Color(0xFF0D47A1) else Color(0xFF8E0000)
    val turnBarBrush = Brush.verticalGradient(listOf(turnColorDark.copy(alpha = 0.7f), turnColorDark.copy(alpha = 0.5f)))

    // ✅ ANA LAYOUT
    Row(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF1A1A1A))
    ) {
        // SOL ZAR AT BUTONU
        Button(
            onClick = advanceToNextDice,
            enabled = nextDiceEnabled,
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF1565C0),
                disabledContainerColor = Color(0xFF1565C0).copy(alpha = 0.4f)
            ),
            shape = RoundedCornerShape(0.dp),
            modifier = Modifier.fillMaxHeight().width(64.dp)
        ) {
            Text(
                text = when (gamePhase) {
                    GamePhase.STARTING_DICE -> "B\nA\nŞ\nL\nA"
                    else -> "Z\nA\nR\n\nA\nT"
                },
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                textAlign = TextAlign.Center,
                lineHeight = 22.sp
            )
        }

        // ORTA İÇERİK
        Column(
            modifier = Modifier.weight(1f).fillMaxHeight()
        ) {
            // ✅ ÜST BAR: KATLAMA ZARI ALANI (sıra rengine bürünür)
            run {
                // Küp kutusu composable
                @Composable
                fun CubeBox(value: Int, alpha: Float = 1f) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .shadow(4.dp, RoundedCornerShape(6.dp))
                            .background(
                                if (value == 1)
                                    Brush.verticalGradient(listOf(Color(0xFFE0E0E0).copy(alpha = 0.5f), Color(0xFFD0D0D0).copy(alpha = 0.4f)))
                                else
                                    Brush.verticalGradient(listOf(Color(0xFFFFFFF0), Color(0xFFE8E0D0))),
                                RoundedCornerShape(6.dp)
                            )
                            .border(1.5.dp, Color(0xFF8B7355).copy(alpha = alpha), RoundedCornerShape(6.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = value.toString(),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (value == 1) Color.Gray.copy(alpha = 0.5f) else Color(0xFF2D1B00)
                        )
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(58.dp)
                        .background(turnBarBrush)
                        .padding(horizontal = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // SOL TARAF
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(3.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (isCrawfordGame) {
                            Button(
                                onClick = {}, enabled = false,
                                colors = ButtonDefaults.buttonColors(
                                    disabledContainerColor = Color.Gray.copy(alpha = 0.3f),
                                    disabledContentColor = Color.White.copy(alpha = 0.5f)
                                ),
                                modifier = Modifier.height(42.dp).widthIn(min = 160.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp),
                                shape = RoundedCornerShape(8.dp)
                            ) { Text("CRAWFORD", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                        } else {
                            when (cubePos) {
                                CubePos.CENTER -> {
                                    if (leftCanDouble) {
                                        Button(
                                            onClick = {
                                                prevCubeValue = cubeValue; prevCubePos = cubePos; prevLeftCanDouble = leftCanDouble; prevRightCanDouble = rightCanDouble
                                                cubeValue *= 2; leftCanDouble = false
                                                cubePos = CubePos.LEFT_OFFER
                                                Toast.makeText(context, "$leftPlayerName katlama teklifi: x$cubeValue", Toast.LENGTH_SHORT).show()
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7B1FA2)),
                                            modifier = Modifier.height(42.dp).widthIn(min = 160.dp),
                                            contentPadding = PaddingValues(horizontal = 10.dp),
                                            shape = RoundedCornerShape(8.dp)
                                        ) { Text("KATLA", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                                    }
                                }
                                CubePos.LEFT_CTRL -> {
                                    // Sol kabul etmiş: KATLA butonu + küp hemen yanında
                                    if (leftCanDouble) {
                                        Button(
                                            onClick = {
                                                prevCubeValue = cubeValue; prevCubePos = cubePos; prevLeftCanDouble = leftCanDouble; prevRightCanDouble = rightCanDouble
                                                cubeValue *= 2; leftCanDouble = false
                                                cubePos = CubePos.LEFT_OFFER
                                                Toast.makeText(context, "$leftPlayerName katlama teklifi: x$cubeValue", Toast.LENGTH_SHORT).show()
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7B1FA2)),
                                            modifier = Modifier.height(42.dp).widthIn(min = 160.dp),
                                            contentPadding = PaddingValues(horizontal = 10.dp),
                                            shape = RoundedCornerShape(8.dp)
                                        ) { Text("KATLA", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                                    }
                                    Spacer(modifier = Modifier.width(4.dp))
                                    CubeBox(cubeValue)
                                }
                                CubePos.RIGHT_OFFER -> {
                                    // Sağ teklif etti, sol cevap veriyor
                                    Button(
                                        onClick = { leftCanDouble = true; rightCanDouble = false; cubePos = CubePos.LEFT_CTRL
                                            Toast.makeText(context, "$leftPlayerName kabul: x$cubeValue", Toast.LENGTH_SHORT).show() },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                                        modifier = Modifier.height(44.dp).widthIn(min = 104.dp), contentPadding = PaddingValues(horizontal = 14.dp),
                                        shape = RoundedCornerShape(8.dp)
                                    ) { Text("KABUL", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                                    Button(
                                        onClick = { cubeValue /= 2; cubePos = CubePos.CENTER
                                            onDoublingResult(makeDoublingIntent(resigned = leftPlayerId)) },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFC62828)),
                                        modifier = Modifier.height(44.dp).widthIn(min = 93.dp), contentPadding = PaddingValues(horizontal = 14.dp),
                                        shape = RoundedCornerShape(8.dp)
                                    ) { Text("PES", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                                    Button(
                                        onClick = { cubeValue = prevCubeValue; cubePos = prevCubePos; leftCanDouble = prevLeftCanDouble; rightCanDouble = prevRightCanDouble },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.2f)),
                                        modifier = Modifier.height(44.dp).widthIn(min = 93.dp), contentPadding = PaddingValues(horizontal = 10.dp),
                                        shape = RoundedCornerShape(8.dp)
                                    ) { Text("\u0130PTAL", fontSize = 10.sp) }
                                }
                                else -> {}
                            }
                        }
                    }

                    // ORTA ALAN
                    Spacer(modifier = Modifier.weight(1f))
                    // Küp ortada: başlangıçta (CENTER, value=1) veya teklif aşamasında (OFFER)
                    if (!isCrawfordGame) {
                        when (cubePos) {
                            CubePos.CENTER -> CubeBox(cubeValue)
                            CubePos.LEFT_OFFER, CubePos.RIGHT_OFFER -> CubeBox(cubeValue)
                            else -> {}
                        }
                    }
                    Spacer(modifier = Modifier.weight(1f))

                    // SAĞ TARAF
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(3.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (isCrawfordGame) {
                            Button(
                                onClick = {}, enabled = false,
                                colors = ButtonDefaults.buttonColors(
                                    disabledContainerColor = Color.Gray.copy(alpha = 0.3f),
                                    disabledContentColor = Color.White.copy(alpha = 0.5f)
                                ),
                                modifier = Modifier.height(42.dp).widthIn(min = 160.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp),
                                shape = RoundedCornerShape(8.dp)
                            ) { Text("CRAWFORD", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                        } else {
                            when (cubePos) {
                                CubePos.CENTER -> {
                                    if (rightCanDouble) {
                                        Button(
                                            onClick = {
                                                prevCubeValue = cubeValue; prevCubePos = cubePos; prevLeftCanDouble = leftCanDouble; prevRightCanDouble = rightCanDouble
                                                cubeValue *= 2; rightCanDouble = false
                                                cubePos = CubePos.RIGHT_OFFER
                                                Toast.makeText(context, "$rightPlayerName katlama teklifi: x$cubeValue", Toast.LENGTH_SHORT).show()
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7B1FA2)),
                                            modifier = Modifier.height(42.dp).widthIn(min = 160.dp),
                                            contentPadding = PaddingValues(horizontal = 10.dp),
                                            shape = RoundedCornerShape(8.dp)
                                        ) { Text("KATLA", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                                    }
                                }
                                CubePos.RIGHT_CTRL -> {
                                    // Sağ kabul etmiş: küp hemen yanında + KATLA butonu
                                    CubeBox(cubeValue)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    if (rightCanDouble) {
                                        Button(
                                            onClick = {
                                                prevCubeValue = cubeValue; prevCubePos = cubePos; prevLeftCanDouble = leftCanDouble; prevRightCanDouble = rightCanDouble
                                                cubeValue *= 2; rightCanDouble = false
                                                cubePos = CubePos.RIGHT_OFFER
                                                Toast.makeText(context, "$rightPlayerName katlama teklifi: x$cubeValue", Toast.LENGTH_SHORT).show()
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7B1FA2)),
                                            modifier = Modifier.height(42.dp).widthIn(min = 160.dp),
                                            contentPadding = PaddingValues(horizontal = 10.dp),
                                            shape = RoundedCornerShape(8.dp)
                                        ) { Text("KATLA", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                                    }
                                }
                                CubePos.LEFT_OFFER -> {
                                    // Sol teklif etti, sağ cevap veriyor
                                    Button(
                                        onClick = { cubeValue = prevCubeValue; cubePos = prevCubePos; leftCanDouble = prevLeftCanDouble; rightCanDouble = prevRightCanDouble },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.2f)),
                                        modifier = Modifier.height(44.dp).widthIn(min = 93.dp), contentPadding = PaddingValues(horizontal = 10.dp),
                                        shape = RoundedCornerShape(8.dp)
                                    ) { Text("\u0130PTAL", fontSize = 10.sp) }
                                    Button(
                                        onClick = { cubeValue /= 2; cubePos = CubePos.CENTER
                                            onDoublingResult(makeDoublingIntent(resigned = rightPlayerId)) },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFC62828)),
                                        modifier = Modifier.height(44.dp).widthIn(min = 93.dp), contentPadding = PaddingValues(horizontal = 14.dp),
                                        shape = RoundedCornerShape(8.dp)
                                    ) { Text("PES", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                                    Button(
                                        onClick = { rightCanDouble = true; leftCanDouble = false; cubePos = CubePos.RIGHT_CTRL
                                            Toast.makeText(context, "$rightPlayerName kabul: x$cubeValue", Toast.LENGTH_SHORT).show() },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                                        modifier = Modifier.height(44.dp).widthIn(min = 104.dp), contentPadding = PaddingValues(horizontal = 14.dp),
                                        shape = RoundedCornerShape(8.dp)
                                    ) { Text("KABUL", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                                }
                                else -> {}
                            }
                        }
                    }
                }
            }

            // ✅ ANA ZAR ALANI
            Box(
                modifier = Modifier.weight(1f).fillMaxWidth()
            ) {
                when (gamePhase) {
                    GamePhase.STARTING_DICE -> {
                        StartingDiceDisplay(
                            leftPlayerName = leftPlayerName,
                            rightPlayerName = rightPlayerName,
                            leftDice = leftStartingDice,
                            rightDice = rightStartingDice,
                            firstPlayer = currentPlayerTurn
                        )
                    }
                    GamePhase.FIRST_MOVE, GamePhase.PLAYING -> {
                        val turnBgBrush = if (currentPlayerTurn == 1)
                            Brush.radialGradient(
                                listOf(Color(0xFF1565C0).copy(alpha = 0.25f), Color(0xFF0D47A1).copy(alpha = 0.15f))
                            )
                        else
                            Brush.radialGradient(
                                listOf(Color(0xFFC62828).copy(alpha = 0.25f), Color(0xFF8E0000).copy(alpha = 0.15f))
                            )
                        val turnBorderColor = if (currentPlayerTurn == 1)
                            Color(0xFF1976D2).copy(alpha = 0.8f) else Color(0xFFD32F2F).copy(alpha = 0.8f)

                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(turnBgBrush)
                                .border(2.dp, turnBorderColor, RoundedCornerShape(2.dp))
                        ) {
                            // Sol oyuncu adı - SOL ÜST KÖŞE
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopStart)
                                    .padding(8.dp)
                                    .background(
                                        if (currentPlayerTurn == 1)
                                            Brush.horizontalGradient(listOf(Color(0xFF1565C0), Color(0xFF1976D2)))
                                        else
                                            Brush.horizontalGradient(listOf(Color(0xFF37474F).copy(alpha = 0.5f), Color(0xFF455A64).copy(alpha = 0.3f))),
                                        RoundedCornerShape(6.dp)
                                    )
                                    .padding(horizontal = 14.dp, vertical = 5.dp)
                            ) {
                                Text(
                                    text = leftPlayerName,
                                    color = if (currentPlayerTurn == 1) Color.White else Color.White.copy(alpha = 0.35f),
                                    fontSize = 30.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            // Sağ oyuncu adı - SAĞ ÜST KÖŞE
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(8.dp)
                                    .background(
                                        if (currentPlayerTurn == 2)
                                            Brush.horizontalGradient(listOf(Color(0xFFD32F2F), Color(0xFFC62828)))
                                        else
                                            Brush.horizontalGradient(listOf(Color(0xFF455A64).copy(alpha = 0.3f), Color(0xFF37474F).copy(alpha = 0.5f))),
                                        RoundedCornerShape(6.dp)
                                    )
                                    .padding(horizontal = 14.dp, vertical = 5.dp)
                            ) {
                                Text(
                                    text = rightPlayerName,
                                    color = if (currentPlayerTurn == 2) Color.White else Color.White.copy(alpha = 0.35f),
                                    fontSize = 30.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            // Parti/Oyun bilgisi - zarların hemen üstünde ortada
                            Text(
                                text = "Parti ${currentPartyIndex + 1}/$totalParties \u2022 Oyun ${currentGameIndex + 1}",
                                modifier = Modifier.align(Alignment.TopCenter).padding(top = 10.dp),
                                color = Color(0xFFCE93D8).copy(alpha = 0.6f),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )

                            // Zarlar - oyuncu isim çerçevesinin hemen altında
                            Row(
                                modifier = Modifier.align(Alignment.TopCenter).padding(top = 58.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(36.dp)
                            ) {
                                DiceBox(currentDicePair?.first ?: 0, 180.dp)
                                DiceBox(currentDicePair?.second ?: 0, 180.dp)
                            }

                            // Hamle ve Tur bilgisi - alt butonların hemen üstünde
                            Row(
                                modifier = Modifier
                                    .align(Alignment.BottomCenter)
                                    .padding(bottom = 8.dp),
                                horizontalArrangement = Arrangement.spacedBy(24.dp)
                            ) {
                                Text(
                                    text = when (gamePhase) {
                                        GamePhase.STARTING_DICE -> "Ba\u015Flang\u0131\u00E7"
                                        GamePhase.FIRST_MOVE -> "\u0130lk Hamle"
                                        GamePhase.PLAYING -> "Hamle: $totalMoveCount"
                                    },
                                    color = Color.White.copy(alpha = 0.8f),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                                Text(
                                    text = "Tur: ${currentRound}/2",
                                    color = Color.White.copy(alpha = 0.8f),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                            }

                            // Sol skor (oyuncu 1) - SOL ALT KÖŞE
                            // Bir rakam genişliği soldan, yarım rakam genişliği alttan uzak
                            Text(
                                text = "$leftScore",
                                color = Color(0xFF64B5F6),
                                fontSize = 80.sp,
                                fontWeight = FontWeight.ExtraBold,
                                modifier = Modifier
                                    .align(Alignment.BottomStart)
                                    .padding(start = 48.dp, bottom = 24.dp)
                            )

                            // Sağ skor (oyuncu 2) - SAĞ ALT KÖŞE
                            Text(
                                text = "$rightScore",
                                color = Color(0xFFEF9A9A),
                                fontSize = 80.sp,
                                fontWeight = FontWeight.ExtraBold,
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .padding(end = 48.dp, bottom = 24.dp)
                            )
                        }
                    }
                }
            }

            // ✅ ALT KONTROL PANELİ (sıra rengine bürünür, simetrik)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(turnBarBrush)
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // ÖNCEKİ butonu
                Button(
                    onClick = {
                        when (gamePhase) {
                            GamePhase.STARTING_DICE -> { }
                            GamePhase.FIRST_MOVE -> {
                                gamePhase = GamePhase.STARTING_DICE
                                totalMoveCount = 0
                            }
                            GamePhase.PLAYING -> {
                                val secondPlayer = if (firstPlayer == 1) 2 else 1
                                val currentIdx = if (currentPlayerTurn == 1) leftMoveIndex else rightMoveIndex
                                val firstPlayerIdx = if (firstPlayer == 1) leftMoveIndex else rightMoveIndex
                                if (currentPlayerTurn == secondPlayer && currentIdx == 0 && firstPlayerIdx == 0) {
                                    gamePhase = GamePhase.FIRST_MOVE
                                    currentPlayerTurn = firstPlayer
                                    totalMoveCount = 1
                                } else {
                                    val otherPlayer = if (currentPlayerTurn == 1) 2 else 1
                                    val otherIdx = if (otherPlayer == 1) leftMoveIndex else rightMoveIndex
                                    if (otherIdx > 0) {
                                        if (otherPlayer == 1) leftMoveIndex-- else rightMoveIndex--
                                        currentPlayerTurn = otherPlayer
                                        totalMoveCount--
                                    }
                                }
                            }
                        }
                    },
                    enabled = !(gamePhase == GamePhase.STARTING_DICE),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White.copy(alpha = 0.15f),
                        disabledContainerColor = Color.White.copy(alpha = 0.05f)
                    ),
                    modifier = Modifier.weight(1.5f).height(42.dp),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Text("\u25C0 \u00D6NCEK\u0130", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                }

                // EL BİTTİ butonu (ortada, vurgulu)
                Button(
                    onClick = {
                        showGameEndScoring = true
                    },
                    enabled = gamePhase == GamePhase.PLAYING || gamePhase == GamePhase.FIRST_MOVE,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF6A1B9A),
                        disabledContainerColor = Color(0xFF6A1B9A).copy(alpha = 0.3f)
                    ),
                    modifier = Modifier.weight(1.3f).height(42.dp),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Text("EL B\u0130TT\u0130", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }

                // SKORBOARD butonu
                Button(
                    onClick = { onDoublingResult(makeDoublingIntent()) },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White.copy(alpha = 0.15f)
                    ),
                    modifier = Modifier.weight(1.5f).height(42.dp),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Text("SKOR \u25B6", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                }
            }
        }

        // SAĞ ZAR AT BUTONU
        Button(
            onClick = advanceToNextDice,
            enabled = nextDiceEnabled,
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFFC62828),
                disabledContainerColor = Color(0xFFC62828).copy(alpha = 0.4f)
            ),
            shape = RoundedCornerShape(0.dp),
            modifier = Modifier.fillMaxHeight().width(64.dp)
        ) {
            Text(
                text = when (gamePhase) {
                    GamePhase.STARTING_DICE -> "B\nA\u015E\nL\nA"
                    else -> "Z\nA\nR\n\nA\nT"
                },
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                textAlign = TextAlign.Center,
                lineHeight = 22.sp
            )
        }
    }
    
    // El bitimi puan hesaplama popup
    if (showGameEndScoring) {
        GameEndScoringDialog(
            leftPlayerName = leftPlayerName,
            rightPlayerName = rightPlayerName,
            doublingCubeValue = cubeValue,
            onScoreSelected = { winnerIsLeft, scoreType ->
                // Puanı hesapla
                val baseScore = when (scoreType) {
                    "SINGLE" -> cubeValue
                    "MARS" -> cubeValue * 2
                    "BACKGAMMON" -> cubeValue * 3
                    else -> cubeValue
                }
                
                // Intent oluştur ve skorboard'a dön
                val resultIntent = makeDoublingIntent().apply {
                    putExtra("game_ended", true)
                    putExtra("winner_is_left", winnerIsLeft)
                    putExtra("score_points", baseScore)
                    putExtra("score_type", scoreType)
                }
                
                showGameEndScoring = false
                onDoublingResult(resultIntent)
            },
            onDismiss = { showGameEndScoring = false }
        )
    }
}

enum class GamePhase {
    STARTING_DICE,
    FIRST_MOVE,
    PLAYING
}

@Composable
fun StartingDiceDisplay(
    leftPlayerName: String,
    rightPlayerName: String,
    leftDice: Int?,
    rightDice: Int?,
    firstPlayer: Int
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.weight(1f).padding(16.dp)
        ) {
            Text(leftPlayerName, color = Color(0xFF64B5F6), fontWeight = FontWeight.Bold, fontSize = 24.sp)
            if (firstPlayer == 1) {
                Text("BA\u015ELIYOR", color = Color(0xFF4CAF50), fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }
            Spacer(modifier = Modifier.height(16.dp))
            DiceBox(value = leftDice ?: 0, size = 150.dp, backgroundColor = Color.White)
        }

        Box(modifier = Modifier.width(4.dp).height(200.dp).background(Color.DarkGray))

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.weight(1f).padding(16.dp)
        ) {
            Text(rightPlayerName, color = Color(0xFFEF9A9A), fontWeight = FontWeight.Bold, fontSize = 24.sp)
            if (firstPlayer == 2) {
                Text("BA\u015ELIYOR", color = Color(0xFF4CAF50), fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }
            Spacer(modifier = Modifier.height(16.dp))
            DiceBox(value = rightDice ?: 0, size = 150.dp, backgroundColor = Color.White)
        }
    }
}

@Composable
fun DiceBox(
    value: Int,
    size: androidx.compose.ui.unit.Dp,
    backgroundColor: Color = Color(0xFFFFFDF5)
) {
    val dotColor = Color(0xFF1A1A1A)
    val cornerRadius = size * 0.15f

    Box(
        modifier = Modifier
            .size(size)
            .shadow(
                elevation = 6.dp,
                shape = RoundedCornerShape(cornerRadius),
                ambientColor = Color.Black.copy(alpha = 0.4f),
                spotColor = Color.Black.copy(alpha = 0.3f)
            )
            .clip(RoundedCornerShape(cornerRadius))
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFFFFFEF8),
                        backgroundColor,
                        Color(0xFFF5F0E0)
                    )
                )
            )
            .border(2.dp, Color(0xFF8B8680), RoundedCornerShape(cornerRadius)),
        contentAlignment = Alignment.Center
    ) {
        if (value in 1..6) {
            Canvas(modifier = Modifier.size(size * 0.72f)) {
                val w = this.size.width
                val h = this.size.height
                val dotRadius = w * 0.12f

                val left = w * 0.2f
                val centerX = w * 0.5f
                val right = w * 0.8f
                val top = h * 0.2f
                val centerY = h * 0.5f
                val bottom = h * 0.8f

                when (value) {
                    1 -> {
                        drawCircle(dotColor, dotRadius * 1.15f, Offset(centerX, centerY))
                    }
                    2 -> {
                        drawCircle(dotColor, dotRadius, Offset(left, top))
                        drawCircle(dotColor, dotRadius, Offset(right, bottom))
                    }
                    3 -> {
                        drawCircle(dotColor, dotRadius, Offset(left, top))
                        drawCircle(dotColor, dotRadius, Offset(centerX, centerY))
                        drawCircle(dotColor, dotRadius, Offset(right, bottom))
                    }
                    4 -> {
                        drawCircle(dotColor, dotRadius, Offset(left, top))
                        drawCircle(dotColor, dotRadius, Offset(right, top))
                        drawCircle(dotColor, dotRadius, Offset(left, bottom))
                        drawCircle(dotColor, dotRadius, Offset(right, bottom))
                    }
                    5 -> {
                        drawCircle(dotColor, dotRadius, Offset(left, top))
                        drawCircle(dotColor, dotRadius, Offset(right, top))
                        drawCircle(dotColor, dotRadius, Offset(centerX, centerY))
                        drawCircle(dotColor, dotRadius, Offset(left, bottom))
                        drawCircle(dotColor, dotRadius, Offset(right, bottom))
                    }
                    6 -> {
                        drawCircle(dotColor, dotRadius, Offset(left, top))
                        drawCircle(dotColor, dotRadius, Offset(right, top))
                        drawCircle(dotColor, dotRadius, Offset(left, centerY))
                        drawCircle(dotColor, dotRadius, Offset(right, centerY))
                        drawCircle(dotColor, dotRadius, Offset(left, bottom))
                        drawCircle(dotColor, dotRadius, Offset(right, bottom))
                    }
                }
            }
        } else {
            Text(
                text = "?",
                fontSize = (size.value * 0.5f).sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black
            )
        }
    }
}

@Composable
fun GameEndScoringDialog(
    leftPlayerName: String,
    rightPlayerName: String,
    doublingCubeValue: Int,
    onScoreSelected: (winnerIsLeft: Boolean, scoreType: String) -> Unit,
    onDismiss: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.7f)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .width(518.dp)
                .shadow(12.dp, RoundedCornerShape(16.dp))
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color(0xFF263238), Color(0xFF1A1A1A))
                    ),
                    RoundedCornerShape(16.dp)
                )
                .border(2.dp, Color(0xFF37474F), RoundedCornerShape(16.dp))
                .padding(23.dp)
        ) {
            // Başlık
            Text(
                "EL BİTİMİ PUAN HESAPLAMA",
                color = Color.White,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 18.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
            )
            
            Text(
                "Hangi oyuncunun oyunu kazandığını işaretleyiniz",
                color = Color.White.copy(alpha = 0.7f),
                fontSize = 13.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(bottom = 20.dp)
            )
            
            // Ana kartlar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(230.dp),
                horizontalArrangement = Arrangement.spacedBy(23.dp)
            ) {
                // Sol oyuncu kartı
                Card(
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.Transparent)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(Color(0xFF1976D2), Color(0xFF0D47A1))
                                ),
                                RoundedCornerShape(16.dp)
                            )
                            .border(2.dp, Color(0xFF42A5F5), RoundedCornerShape(16.dp))
                            .padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            leftPlayerName,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            textAlign = TextAlign.Center,
                            maxLines = 2
                        )
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        // Sol taraf butonları (mavi tonları)
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            ScoringButton(
                                text = "TEK",
                                points = doublingCubeValue,
                                onClick = { onScoreSelected(true, "SINGLE") },
                                color = Color(0xFF42A5F5),
                                height = 46.dp
                            )
                            ScoringButton(
                                text = "MARS",
                                points = doublingCubeValue * 2,
                                onClick = { onScoreSelected(true, "MARS") },
                                color = Color(0xFF2196F3),
                                height = 46.dp
                            )
                            ScoringButton(
                                text = "BACKGAMMON",
                                points = doublingCubeValue * 3,
                                onClick = { onScoreSelected(true, "BACKGAMMON") },
                                color = Color(0xFF1976D2),
                                height = 46.dp
                            )
                        }
                    }
                }
                
                // Orta katlama küpü (gri tonları)
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.width(92.dp).fillMaxHeight()
                ) {
                    Text("KÜPE DEĞER", color = Color.White.copy(alpha = 0.6f), fontSize = 11.sp, textAlign = TextAlign.Center)

                    Box(
                        modifier = Modifier
                            .size(69.dp)
                            .shadow(8.dp, RoundedCornerShape(12.dp))
                            .background(
                                Brush.radialGradient(
                                    colors = listOf(Color(0xFFECEFF1), Color(0xFFB0BEC5))
                                ),
                                RoundedCornerShape(12.dp)
                            )
                            .border(2.dp, Color(0xFF90A4AE), RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            doublingCubeValue.toString(),
                            color = Color(0xFF263238),
                            fontSize = 22.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }
                
                // Sağ oyuncu kartı
                Card(
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.Transparent)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(Color(0xFFD32F2F), Color(0xFF8E0000))
                                ),
                                RoundedCornerShape(16.dp)
                            )
                            .border(2.dp, Color(0xFFEF5350), RoundedCornerShape(16.dp))
                            .padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            rightPlayerName,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            textAlign = TextAlign.Center,
                            maxLines = 2
                        )
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        // Sağ taraf butonları (kırmızı tonları)
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            ScoringButton(
                                text = "TEK",
                                points = doublingCubeValue,
                                onClick = { onScoreSelected(false, "SINGLE") },
                                color = Color(0xFFEF5350),
                                height = 46.dp
                            )
                            ScoringButton(
                                text = "MARS",
                                points = doublingCubeValue * 2,
                                onClick = { onScoreSelected(false, "MARS") },
                                color = Color(0xFFF44336),
                                height = 46.dp
                            )
                            ScoringButton(
                                text = "BACKGAMMON",
                                points = doublingCubeValue * 3,
                                onClick = { onScoreSelected(false, "BACKGAMMON") },
                                color = Color(0xFFD32F2F),
                                height = 46.dp
                            )
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(20.dp))
            
            // İptal butonu (gri tonları)
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF546E7A)
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth().height(52.dp)
            ) {
                Text("İPTAL", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
        }
    }
}

@Composable
fun ScoringButton(
    text: String,
    points: Int,
    onClick: () -> Unit,
    color: Color,
    height: androidx.compose.ui.unit.Dp = 37.dp
) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(
            containerColor = color.copy(alpha = 0.9f)
        ),
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier.fillMaxWidth().height(height),
        contentPadding = PaddingValues(horizontal = 9.dp)
    ) {
        Text(
            "$text ($points)",
            color = Color.White,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
    }
}
