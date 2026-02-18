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
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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

        // Tam ekran modu
        WindowCompat.setDecorFitsSystemWindows(window, false)
        val controller = WindowInsetsControllerCompat(window, window.decorView)
        controller.hide(WindowInsetsCompat.Type.systemBars())
        controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        dbHelper = DatabaseHelper(this)
        val encounterId = intent.getLongExtra("encounter_id", -1L)

        if (encounterId == -1L) {
            Toast.makeText(this, "Karsilasma bulunamadi", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        setContent {
            TavlaAppTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color.Black
                ) {
                    RematchDiceDisplayScreen(dbHelper, encounterId) {
                        finish() // Geri dön
                    }
                }
            }
        }
    }
}

@Composable
fun RematchDiceDisplayScreen(
    dbHelper: DatabaseHelper,
    encounterId: Long,
    onBack: () -> Unit
) {
    val context = LocalContext.current

    // Karsilasma bilgisi
    val encounter = remember { mutableStateOf(dbHelper.getRematchEncounter(encounterId)) }

    // Mevcut zar seti
    var currentDiceSet by remember { mutableStateOf<RematchDiceSet?>(null) }

    // Oyun durumu
    var gamePhase by remember { mutableStateOf(GamePhase.STARTING_DICE) }
    var leftMoveIndex by remember { mutableStateOf(0) }   // Sol oyuncunun zar dizisi indeksi
    var rightMoveIndex by remember { mutableStateOf(0) }   // Sağ oyuncunun zar dizisi indeksi
    var totalMoveCount by remember { mutableStateOf(0) }   // Toplam hamle sayısı
    var currentPlayerTurn by remember { mutableStateOf(1) } // 1 veya 2 - kimin sırası

    // Tur bilgisi (1 veya 2)
    val currentRound = encounter.value?.currentRound ?: 1
    val currentPartyIndex = encounter.value?.currentPartyIndex ?: 0
    val currentGameIndex = encounter.value?.currentGameIndex ?: 0
    val totalParties = encounter.value?.totalParties ?: 100

    // Oyuncu isimleri (rovansta yer degisir)
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
        // Rovansta yer degistir
        leftPlayerName = encounter.value?.player2Name ?: "Oyuncu 2"
        rightPlayerName = encounter.value?.player1Name ?: "Oyuncu 1"
        leftPlayerId = encounter.value?.player2Id ?: 0L
        rightPlayerId = encounter.value?.player1Id ?: 0L
    }

    // Zar setini yukle
    LaunchedEffect(encounterId, currentPartyIndex, currentGameIndex) {
        currentDiceSet = dbHelper.getDiceSetForGame(encounterId, currentPartyIndex, currentGameIndex)
        gamePhase = GamePhase.STARTING_DICE
        leftMoveIndex = 0
        rightMoveIndex = 0
        totalMoveCount = 0
        // İlk oyuncuyu belirle (başlangıç zarlarına göre)
        currentDiceSet?.let { diceSet ->
            currentPlayerTurn = if (currentRound == 1) {
                diceSet.getFirstPlayer()
            } else {
                // Rovansta zarlar yer değiştirdiği için ters
                if (diceSet.getFirstPlayer() == 1) 2 else 1
            }
        }
    }

    // Mevcut zarlar (tura göre ayarla)
    val leftStartingDice: Int?
    val rightStartingDice: Int?
    val leftDice: List<Pair<Int, Int>>?
    val rightDice: List<Pair<Int, Int>>?

    if (currentRound == 1) {
        leftStartingDice = currentDiceSet?.startingDicePlayer1
        rightStartingDice = currentDiceSet?.startingDicePlayer2
        leftDice = currentDiceSet?.player1Dice
        rightDice = currentDiceSet?.player2Dice
    } else {
        // Rovansta zarlar yer değiştirir
        leftStartingDice = currentDiceSet?.startingDicePlayer2
        rightStartingDice = currentDiceSet?.startingDicePlayer1
        leftDice = currentDiceSet?.player2Dice
        rightDice = currentDiceSet?.player1Dice
    }

    // Başlayan oyuncu (1=sol, 2=sağ)
    val firstPlayer = currentDiceSet?.let {
        if (currentRound == 1) it.getFirstPlayer()
        else if (it.getFirstPlayer() == 1) 2 else 1
    } ?: 1

    // Şu anki oyuncunun zarı
    val currentDicePair = when (gamePhase) {
        GamePhase.FIRST_MOVE -> {
            // Başlangıç zarlarının kombinasyonu - büyük atanın zarı first, küçük atanın zarı second
            val d1 = leftStartingDice ?: 0
            val d2 = rightStartingDice ?: 0
            if (firstPlayer == 1) Pair(d1, d2) else Pair(d2, d1)
        }
        GamePhase.PLAYING -> {
            if (currentPlayerTurn == 1) {
                leftDice?.getOrNull(leftMoveIndex)
            } else {
                rightDice?.getOrNull(rightMoveIndex)
            }
        }
        else -> null
    }

    // Şu anki oyuncunun bilgileri
    val currentPlayerName = if (currentPlayerTurn == 1) leftPlayerName else rightPlayerName
    val currentPlayerColor = if (currentPlayerTurn == 1) Color(0xFF1565C0) else Color(0xFFC62828)
    val currentPlayerBgColor = if (currentPlayerTurn == 1) Color(0xFF1565C0).copy(alpha = 0.2f) else Color(0xFFC62828).copy(alpha = 0.2f)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF1E1E1E))
    ) {
        // Üst bilgi satırı
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF2D2D2D))
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Tur ${currentRound}/2",
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Parti ${currentPartyIndex + 1}/$totalParties | Oyun ${currentGameIndex + 1}",
                color = Color(0xFF6A1B9A),
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
            Text(
                text = when (gamePhase) {
                    GamePhase.STARTING_DICE -> "Başlangıç Zarı"
                    GamePhase.FIRST_MOVE -> "İlk Hamle (Başlangıç Zarı)"
                    GamePhase.PLAYING -> "Hamle $totalMoveCount"
                },
                color = Color.Gray
            )
        }

        // Ana zar gösterim alanı
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            when (gamePhase) {
                GamePhase.STARTING_DICE -> {
                    // Başlangıç zarları - iki oyuncu da görünür
                    StartingDiceDisplay(
                        leftPlayerName = leftPlayerName,
                        rightPlayerName = rightPlayerName,
                        leftDice = leftStartingDice,
                        rightDice = rightStartingDice,
                        firstPlayer = currentPlayerTurn
                    )
                }
                GamePhase.FIRST_MOVE -> {
                    // İlk hamle - başlangıç zarlarının kombinasyonu
                    SinglePlayerDiceDisplay(
                        playerName = currentPlayerName,
                        playerColor = currentPlayerColor,
                        bgColor = currentPlayerBgColor,
                        dicePair = currentDicePair,
                        moveIndex = 0,
                        label = "İlk Hamle (Başlangıç Zarı)"
                    )
                }
                GamePhase.PLAYING -> {
                    // Oyun zarları - sadece sıradaki oyuncunun zarı
                    val moveIdx = if (currentPlayerTurn == 1) leftMoveIndex else rightMoveIndex
                    SinglePlayerDiceDisplay(
                        playerName = currentPlayerName,
                        playerColor = currentPlayerColor,
                        bgColor = currentPlayerBgColor,
                        dicePair = currentDicePair,
                        moveIndex = moveIdx
                    )
                }
            }
        }

        // Alt kontrol paneli
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF2D2D2D))
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Önceki buton
            Button(
                onClick = {
                    when (gamePhase) {
                        GamePhase.STARTING_DICE -> {
                            // Başlangıçtan öncesine gidemez
                        }
                        GamePhase.FIRST_MOVE -> {
                            // İlk hamleden başlangıç zarlarına dön
                            gamePhase = GamePhase.STARTING_DICE
                            totalMoveCount = 0
                        }
                        GamePhase.PLAYING -> {
                            // İkinci oyuncu ilk zarında (indeks 0) ve başlayan da 0 ise → FIRST_MOVE'a dön
                            val secondPlayer = if (firstPlayer == 1) 2 else 1
                            val currentIdx = if (currentPlayerTurn == 1) leftMoveIndex else rightMoveIndex
                            val firstPlayerIdx = if (firstPlayer == 1) leftMoveIndex else rightMoveIndex
                            if (currentPlayerTurn == secondPlayer && currentIdx == 0 && firstPlayerIdx == 0) {
                                gamePhase = GamePhase.FIRST_MOVE
                                currentPlayerTurn = firstPlayer
                                totalMoveCount = 1
                            } else {
                                // Normal geri: diğer oyuncuya dön, onun indeksini azalt
                                val otherPlayer = if (currentPlayerTurn == 1) 2 else 1
                                if (otherPlayer == 1) leftMoveIndex-- else rightMoveIndex--
                                currentPlayerTurn = otherPlayer
                                totalMoveCount--
                            }
                        }
                    }
                },
                enabled = !(gamePhase == GamePhase.STARTING_DICE),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF757575)),
                modifier = Modifier.width(100.dp)
            ) {
                Text("◀ ÖNCEKİ", fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }

            // Sonraki zar butonu
            Button(
                onClick = {
                    when (gamePhase) {
                        GamePhase.STARTING_DICE -> {
                            // İlk hamleye geç (başlangıç zarı kombinasyonu)
                            gamePhase = GamePhase.FIRST_MOVE
                            // currentPlayerTurn zaten firstPlayer
                            totalMoveCount = 1
                        }
                        GamePhase.FIRST_MOVE -> {
                            // İlk hamle bitti, karşı taraf çift zar atar
                            gamePhase = GamePhase.PLAYING
                            val secondPlayer = if (firstPlayer == 1) 2 else 1
                            currentPlayerTurn = secondPlayer
                            // İkinci oyuncunun ilk zarı (indeks 0)
                            totalMoveCount = 2
                        }
                        GamePhase.PLAYING -> {
                            // Oyuncu değiştir
                            val prevPlayer = currentPlayerTurn
                            currentPlayerTurn = if (currentPlayerTurn == 1) 2 else 1
                            // Önceki oyuncunun indeksini artır (gösterilen zarı tüket)
                            // Yeni oyuncunun zarı gösterilecek
                            if (prevPlayer == 1) {
                                leftMoveIndex++
                            } else {
                                rightMoveIndex++
                            }
                            totalMoveCount++
                        }
                    }
                },
                enabled = leftMoveIndex < 199 && rightMoveIndex < 199,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50)),
                modifier = Modifier
                    .width(180.dp)
                    .height(56.dp)
            ) {
                Text(
                    text = when (gamePhase) {
                        GamePhase.STARTING_DICE -> "▶ OYUNA BAŞLA"
                        GamePhase.FIRST_MOVE -> "▶ SONRAKİ ZAR"
                        GamePhase.PLAYING -> "▶ SONRAKİ ZAR"
                    },
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }

            // El bitti butonu - PipEntry'ye git
            Button(
                onClick = {
                    // dice_pairs_used kaydet (SharedPreferences ile skorboard'a aktar)
                    val prefs = context.getSharedPreferences("rematch_prefs", android.content.Context.MODE_PRIVATE)
                    prefs.edit().putInt("dice_pairs_used_${encounterId}", totalMoveCount).apply()
                    // Skorboard'a geri dön
                    (context as? ComponentActivity)?.finish()
                },
                enabled = gamePhase == GamePhase.PLAYING || gamePhase == GamePhase.FIRST_MOVE,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFB71C1C)),
                modifier = Modifier
                    .width(120.dp)
                    .height(56.dp)
            ) {
                Text("EL BİTTİ", fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }

            // Geri dön butonu
            Button(
                onClick = onBack,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF757575)),
                modifier = Modifier.width(100.dp)
            ) {
                Text("SKORBOARD", fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }
        }
    }
}

/**
 * Oyun fazı
 */
enum class GamePhase {
    STARTING_DICE,  // Başlangıç zarları gösteriliyor
    FIRST_MOVE,     // İlk hamle - başlangıç zarı kombinasyonu
    PLAYING         // Oyun zarları gösteriliyor
}

/**
 * Başlangıç zarları gösterimi - iki oyuncu yan yana
 */
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
        // Sol oyuncu
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .weight(1f)
                .padding(16.dp)
        ) {
            Text(
                text = leftPlayerName,
                color = Color(0xFF64B5F6),
                fontWeight = FontWeight.Bold,
                fontSize = 24.sp
            )
            if (firstPlayer == 1) {
                Text(
                    text = "BAŞLIYOR",
                    color = Color(0xFF4CAF50),
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            DiceBox(
                value = leftDice ?: 0,
                size = 150.dp,
                backgroundColor = Color.White
            )
        }

        // Orta çizgi
        Box(
            modifier = Modifier
                .width(4.dp)
                .height(200.dp)
                .background(Color.DarkGray)
        )

        // Sağ oyuncu
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .weight(1f)
                .padding(16.dp)
        ) {
            Text(
                text = rightPlayerName,
                color = Color(0xFFEF9A9A),
                fontWeight = FontWeight.Bold,
                fontSize = 24.sp
            )
            if (firstPlayer == 2) {
                Text(
                    text = "BAŞLIYOR",
                    color = Color(0xFF4CAF50),
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            DiceBox(
                value = rightDice ?: 0,
                size = 150.dp,
                backgroundColor = Color.White
            )
        }
    }
}

/**
 * Tek oyuncu zar gösterimi - tam ekran
 */
@Composable
fun SinglePlayerDiceDisplay(
    playerName: String,
    playerColor: Color,
    bgColor: Color,
    dicePair: Pair<Int, Int>?,
    moveIndex: Int,
    label: String? = null
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(bgColor)
            .border(4.dp, playerColor),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Oyuncu adı
            Text(
                text = playerName,
                color = playerColor,
                fontWeight = FontWeight.Bold,
                fontSize = 36.sp
            )

            Text(
                text = label ?: "Hamle ${moveIndex + 1}",
                color = if (label != null) Color(0xFF4CAF50) else Color.Gray,
                fontSize = 18.sp
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Zar çifti
            Row(
                horizontalArrangement = Arrangement.spacedBy(32.dp)
            ) {
                DiceBox(
                    value = dicePair?.first ?: 0,
                    size = 160.dp,
                    backgroundColor = Color.White
                )
                DiceBox(
                    value = dicePair?.second ?: 0,
                    size = 160.dp,
                    backgroundColor = Color.White
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
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

                // Nokta pozisyonlari (oransal)
                val left = w * 0.2f
                val centerX = w * 0.5f
                val right = w * 0.8f
                val top = h * 0.2f
                val centerY = h * 0.5f
                val bottom = h * 0.8f

                // Her zar degeri icin noktalar
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
