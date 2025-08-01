package com.tavla.tavlapp

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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

class GameScoreActivity : ComponentActivity() {
    private lateinit var dbHelper: DatabaseHelper
    private var matchId: Long = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        dbHelper = DatabaseHelper(this)

        // Intent'ten oyuncu bilgilerini ve oyun bilgilerini alıyoruz
        val player1Name = intent.getStringExtra("player1_name") ?: "Oyuncu 1"
        val player2Name = intent.getStringExtra("player2_name") ?: "Oyuncu 2"
        val player1Id = intent.getLongExtra("player1_id", -1)
        val player2Id = intent.getLongExtra("player2_id", -1)
        val gameType = intent.getStringExtra("game_type") ?: "Modern"
        val targetRounds = intent.getIntExtra("rounds", 11)
        val isScoreAutomatic = intent.getBooleanExtra("is_score_automatic", true)

        // Yeni maç başlat ve ID'sini al
        matchId = dbHelper.startNewMatch(player1Id, player2Id, gameType, targetRounds)

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
                        matchId = matchId,
                        dbHelper = dbHelper,
                        onFinish = { this.finish() }
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
    matchId: Long,
    dbHelper: DatabaseHelper,
    onFinish: () -> Unit
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

    var showEndMatchConfirmation by remember { mutableStateOf(false) }

    // ✅ RECOMPOSE ETKİSİ - LaunchedEffect ekle
    LaunchedEffect(recomposeKey) {
        // Bu blok recomposeKey değiştiğinde çalışır ve UI'ı günceller
        // İçeriği boş bırakabilirsin, sadece recompose tetiklemek için
    }


    // Maç sona erdiğinde yapılacak işlemler
    fun endMatch() {
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

        showMatchEndDialog = true
    }

    // Ekran genişliğini dinamik olarak hesapla
    val screenWidthDp = configuration.screenWidthDp.dp
    val edgeOffset = (screenWidthDp / 2) - 50.dp // Ekran kenarına yakın, biraz iç tarafta

    // Zarın pozisyonuna göre x ve y offset'lerini hesaplama - daha aşağıya ayarlandı
    val (xOffset, yOffset) = when (doublingCubePosition) {
        DoublingCubePosition.CENTER -> Pair(0.dp, -120.dp) // Küp daha aşağıya taşındı
        DoublingCubePosition.PLAYER1_OFFER -> Pair(-120.dp, -120.dp)
        DoublingCubePosition.PLAYER1_CONTROL -> if (isLandscape) Pair(-edgeOffset, -100.dp) else Pair(-edgeOffset, -100.dp)
        DoublingCubePosition.PLAYER2_OFFER -> Pair(120.dp, -120.dp)
        DoublingCubePosition.PLAYER2_CONTROL -> if (isLandscape) Pair(edgeOffset, -100.dp) else Pair(edgeOffset, -100.dp)
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

    // ✅ GÜNCELLENMİŞ: El ekle ve skoru güncelle
    fun addRound(playerId: Long, playerName: String, winType: String, score: Int) {
        currentRound++

        // Küp değeri ile çarparak gerçek skoru hesapla
        val finalScore = score * doublingCubeValue

        // ✅ El bilgisini veritabanına ekle ve ID'sini al
        val roundId = dbHelper.addRound(
            matchId = matchId,
            roundNumber = currentRound,
            winnerId = playerId,
            winType = winType,
            isDouble = doublingCubeValue > 1, // Küp kullanıldıysa true
            score = finalScore
        )

        // ✅ UNDO STACK'e ekle
        if (roundId != -1L) {
            undoStack = undoStack + roundId
        }

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

        // Hedef puana ulaşıldıysa maçı bitir
        if (player1Score >= targetRounds || player2Score >= targetRounds) {
            endMatch()
        }
    }

    /// ✅ DEBUG VERSİYONU: Son hamleyi geri al
    fun undoLastRound() {
        if (undoStack.isNotEmpty()) {
            try {
                val lastRoundId = undoStack.last()

                // ESKI DEĞERLER
                val oldPlayer1Score = player1Score
                val oldPlayer2Score = player2Score
                val oldRecomposeKey = recomposeKey

                Toast.makeText(context, "BAŞLA: P1=$oldPlayer1Score, P2=$oldPlayer2Score, Key=$oldRecomposeKey", Toast.LENGTH_LONG).show()

                // 1. Round'u veritabanından sil
                val deleteResult = dbHelper.deleteRound(lastRoundId)

                if (deleteResult > 0) {
                    // 2. Stack'ten çıkar
                    undoStack = undoStack.dropLast(1)

                    // 3. Maç durumunu veritabanından yeniden yükle
                    val updatedMatch = dbHelper.getMatchDetails(matchId)
                    if (updatedMatch != null) {

                        Toast.makeText(context, "VERİTABANI: P1=${updatedMatch.player1Score}, P2=${updatedMatch.player2Score}", Toast.LENGTH_LONG).show()

                        // State'leri güncelle
                        player1Score = updatedMatch.player1Score
                        player2Score = updatedMatch.player2Score
                        player1RoundsWon = updatedMatch.player1RoundsWon
                        player2RoundsWon = updatedMatch.player2RoundsWon
                        currentRound = updatedMatch.totalRounds

                        // Force recompose
                        recomposeKey++

                        Toast.makeText(context, "SON: P1=$player1Score, P2=$player2Score, Key=$recomposeKey", Toast.LENGTH_LONG).show()

                    }

                    // Katlama zarını sıfırla
                    doublingCubeValue = 1
                    doublingCubePosition = DoublingCubePosition.CENTER
                    player1CanDouble = true
                    player2CanDouble = true
                    showPlayer1DoublingMenu = false
                    showPlayer2DoublingMenu = false

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
            // Tekliften önceki değerleri kaydet
            previousDoublingCubeValue = doublingCubeValue
            previousDoublingCubePosition = doublingCubePosition

            // Küpü ikiye katla
            doublingCubeValue *= 2
            doublingCubePosition = DoublingCubePosition.PLAYER1_OFFER
            player1CanDouble = false
            showPlayer2DoublingMenu = true
        }
    }

    fun player2OfferDouble() {
        if (player2CanDouble) {
            // Tekliften önceki değerleri kaydet
            previousDoublingCubeValue = doublingCubeValue
            previousDoublingCubePosition = doublingCubePosition

            // Küpü ikiye katla
            doublingCubeValue *= 2
            doublingCubePosition = DoublingCubePosition.PLAYER2_OFFER
            player2CanDouble = false
            showPlayer1DoublingMenu = true
        }
    }

    fun player1AcceptDouble() {
        doublingCubePosition = DoublingCubePosition.PLAYER1_CONTROL
        showPlayer1DoublingMenu = false
        player1CanDouble = true
        player2CanDouble = false
    }

    fun player2AcceptDouble() {
        doublingCubePosition = DoublingCubePosition.PLAYER2_CONTROL
        showPlayer2DoublingMenu = false
        player2CanDouble = true
        player1CanDouble = false
    }

    fun player1Resign() {
        // Zarın değerinin yarısı kadar puan oyuncu 2'ye verilir
        val score = doublingCubeValue / 2
        player2Score += score
        Toast.makeText(context, "$player1Name pes etti. $player2Name'e $score puan eklendi.", Toast.LENGTH_SHORT).show()

        // Zarı sıfırla
        previousDoublingCubeValue = 1
        previousDoublingCubePosition = DoublingCubePosition.CENTER
        doublingCubeValue = 1
        doublingCubePosition = DoublingCubePosition.CENTER
        player1CanDouble = true
        player2CanDouble = true
        showPlayer1DoublingMenu = false
    }

    fun player2Resign() {
        // Zarın değerinin yarısı kadar puan oyuncu 1'e verilir
        val score = doublingCubeValue / 2
        player1Score += score
        Toast.makeText(context, "$player2Name pes etti. $player1Name'e $score puan eklendi.", Toast.LENGTH_SHORT).show()

        // Zarı sıfırla
        previousDoublingCubeValue = 1
        previousDoublingCubePosition = DoublingCubePosition.CENTER
        doublingCubeValue = 1
        doublingCubePosition = DoublingCubePosition.CENTER
        player1CanDouble = true
        player2CanDouble = true
        showPlayer2DoublingMenu = false
    }

    fun resetDoublingCube() {
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
            title = { Text("Maç Sonucu") },
            text = {
                Column {
                    Text("Kazanan: $winnerName ($winnerScore puan)")
                    Text("Kaybeden: $loserName ($loserScore puan)")
                    Text("Toplam El: $currentRound")
                    Text("$player1Name: $player1RoundsWon el kazandı")
                    Text("$player2Name: $player2RoundsWon el kazandı")
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
                    Text("Maçı sonlandırmak istediğinizden emin misiniz?")
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("$player1Name: $player1Score puan ($player1RoundsWon el)")
                    Text("$player2Name: $player2Score puan ($player2RoundsWon el)")
                    Text("Bu sonuçlar istatistiklere kaydedilecektir.")
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
            // Oyuncu bilgileri
            Row(modifier = Modifier.weight(1f)) {
                // Oyuncu 1 bilgileri
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .weight(1f)
                        .padding(16.dp)
                ) {
                    // Oyuncu adı
                    Text(
                        text = "$player1Name ($player1RoundsWon)",
                        color = Color.White,
                        style = MaterialTheme.typography.titleLarge
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Skor
                    Text(
                        text = player1Score.toString(),
                        color = Color.White,
                        style = MaterialTheme.typography.displayLarge,
                        fontSize = 60.sp
                    )

                    // Skor ile butonlar arasında ek boşluk
                    Spacer(modifier = Modifier.height(24.dp))

                    // Katla butonu - Oyuncu 1 için
                    if (!isTraditionalGame && !showPlayer1DoublingMenu && !showPlayer2DoublingMenu &&
                        (doublingCubePosition == DoublingCubePosition.CENTER ||
                                doublingCubePosition == DoublingCubePosition.PLAYER1_CONTROL) &&
                        player1CanDouble
                    ) {
                        Spacer(modifier = Modifier.height(20.dp))
                        Button(
                            onClick = { player1OfferDouble() },
                            shape = RoundedCornerShape(4.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.White.copy(alpha = 0.3f)
                            ),
                            modifier = Modifier
                                .padding(8.dp)
                                .width(120.dp) // Sabit genişlik - yatay/dikey aynı
                                .height(48.dp) // Sabit yükseklik - yatay/dikey aynı
                        ) {
                            Text(
                                text = "Katla",
                                color = Color.White,
                                fontSize = 16.sp, // Sabit font boyutu
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // ✅ YENİ EKLENEN: GERI ALMA BUTONU - Katlama butonlarının hemen altında
                    if (!showPlayer1DoublingMenu && !showPlayer2DoublingMenu && undoStack.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = { undoLastRound() },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF9800)), // Turuncu
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(45.dp)
                                .padding(horizontal = 8.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Text("↶", fontSize = 18.sp, color = Color.White)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Son Hamleyi Geri Al (${undoStack.size})", fontSize = 14.sp, color = Color.White)
                            }
                        }
                    }

                    // Katlama menüsü - Oyuncu 1 için
                    if (showPlayer1DoublingMenu) {
                        if (isLandscape) {
                            // Yatay mod için butonları yan yana düzenleme - İyileştirilmiş boyutlar
                            Row(
                                modifier = Modifier
                                    .padding(8.dp)
                                    .fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp) // Butonlar arası boşluk
                            ) {
                                // Kabul Et butonu - Daha büyük boyutlar
                                Button(
                                    onClick = { player1AcceptDouble() },
                                    shape = RoundedCornerShape(4.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color.Green.copy(alpha = 0.9f)
                                    ),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(56.dp) // Daha yüksek buton
                                ) {
                                    Text(
                                        text = "✓ Kabul Et",
                                        fontSize = 14.sp, // Daha büyük font
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }

                                // Pes Et butonu - Daha büyük boyutlar
                                Button(
                                    onClick = { player1Resign() },
                                    shape = RoundedCornerShape(4.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color.Red.copy(alpha = 0.9f)
                                    ),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(56.dp) // Daha yüksek buton
                                ) {
                                    Text(
                                        text = "✗ Pes Et",
                                        fontSize = 14.sp, // Daha büyük font
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }

                                // İptal butonu - Daha büyük boyutlar
                                Button(
                                    onClick = { resetDoublingCube() },
                                    shape = RoundedCornerShape(4.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color.Gray.copy(alpha = 0.9f)
                                    ),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(56.dp) // Daha yüksek buton
                                ) {
                                    Text(
                                        text = "↩ İptal",
                                        fontSize = 14.sp, // Daha büyük font
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            }
                        } else {
                            // Dikey mod için orijinal dikey düzenleme - İyileştirilmiş boyutlar
                            Column(
                                modifier = Modifier.padding(8.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                // Kabul Et butonu
                                Button(
                                    onClick = { player1AcceptDouble() },
                                    shape = RoundedCornerShape(4.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color.Green.copy(alpha = 0.9f)
                                    ),
                                    modifier = Modifier
                                        .padding(vertical = 4.dp)
                                        .fillMaxWidth()
                                        .height(48.dp) // Sabit yükseklik
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.Center
                                    ) {
                                        Text("✓", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Kabul Et", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                    }
                                }

                                // Pes Et butonu
                                Button(
                                    onClick = { player1Resign() },
                                    shape = RoundedCornerShape(4.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color.Red.copy(alpha = 0.9f)
                                    ),
                                    modifier = Modifier
                                        .padding(vertical = 4.dp)
                                        .fillMaxWidth()
                                        .height(48.dp) // Sabit yükseklik
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.Center
                                    ) {
                                        Text("✗", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Pes Et", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                    }
                                }

                                // İptal butonu
                                Button(
                                    onClick = { resetDoublingCube() },
                                    shape = RoundedCornerShape(4.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color.Gray.copy(alpha = 0.9f)
                                    ),
                                    modifier = Modifier
                                        .padding(vertical = 4.dp)
                                        .fillMaxWidth()
                                        .height(48.dp) // Sabit yükseklik
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.Center
                                    ) {
                                        Text("↩", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("İptal", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }

                // ORTA KISIM - Hedef el sayısı (iki oyuncu arasında)
                Box(
                    modifier = Modifier
                        .width(100.dp)
                        .height(100.dp)
                        .offset(y = (-90).dp), // Y offset'i koruyoruz
                    contentAlignment = Alignment.Center // Box scope'unda doğru kullanım
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

                // Oyuncu 2 bilgileri
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .weight(1f)
                        .padding(16.dp)
                ) {
                    // Oyuncu adı
                    Text(
                        text = "$player2Name ($player2RoundsWon)",
                        color = Color.White,
                        style = MaterialTheme.typography.titleLarge
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Skor
                    Text(
                        text = player2Score.toString(),
                        color = Color.White,
                        style = MaterialTheme.typography.displayLarge,
                        fontSize = 60.sp
                    )

                    // Skor ile butonlar arasında ek boşluk
                    Spacer(modifier = Modifier.height(24.dp))

                    // Katla butonu - Oyuncu 2 için
                    if (!isTraditionalGame && !showPlayer1DoublingMenu && !showPlayer2DoublingMenu &&
                        (doublingCubePosition == DoublingCubePosition.CENTER ||
                                doublingCubePosition == DoublingCubePosition.PLAYER2_CONTROL) &&
                        player2CanDouble
                    ) {
                        Spacer(modifier = Modifier.height(20.dp))
                        Button(
                            onClick = { player2OfferDouble() },
                            shape = RoundedCornerShape(4.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.White.copy(alpha = 0.3f)
                            ),
                            modifier = Modifier
                                .padding(8.dp)
                                .width(120.dp) // Sabit genişlik - yatay/dikey aynı
                                .height(48.dp) // Sabit yükseklik - yatay/dikey aynı
                        ) {
                            Text(
                                text = "Katla",
                                color = Color.White,
                                fontSize = 16.sp, // Sabit font boyutu
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // ✅ YENİ EKLENEN: GERI ALMA BUTONU - Oyuncu 2 için katlama butonlarının hemen altında
                    if (!showPlayer1DoublingMenu && !showPlayer2DoublingMenu && undoStack.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = { undoLastRound() },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF9800)), // Turuncu
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(45.dp)
                                .padding(horizontal = 8.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Text("↶", fontSize = 18.sp, color = Color.White)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Son Hamleyi Geri Al (${undoStack.size})", fontSize = 14.sp, color = Color.White)
                            }
                        }
                    }

                    // Katlama menüsü - Oyuncu 2 için
                    if (showPlayer2DoublingMenu) {
                        if (isLandscape) {
                            // Yatay mod için butonları yan yana düzenleme - İyileştirilmiş boyutlar
                            Row(
                                modifier = Modifier
                                    .padding(8.dp)
                                    .fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp) // Butonlar arası boşluk
                            ) {
                                // Kabul Et butonu - Daha büyük boyutlar
                                Button(
                                    onClick = { player2AcceptDouble() },
                                    shape = RoundedCornerShape(4.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color.Green.copy(alpha = 0.9f)
                                    ),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(56.dp) // Daha yüksek buton
                                ) {
                                    Text(
                                        text = "✓ Kabul Et",
                                        fontSize = 14.sp, // Daha büyük font
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }

                                // Pes Et butonu - Daha büyük boyutlar
                                Button(
                                    onClick = { player2Resign() },
                                    shape = RoundedCornerShape(4.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color.Red.copy(alpha = 0.9f)
                                    ),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(56.dp) // Daha yüksek buton
                                ) {
                                    Text(
                                        text = "✗ Pes Et",
                                        fontSize = 14.sp, // Daha büyük font
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }

                                // İptal butonu - Daha büyük boyutlar
                                Button(
                                    onClick = { resetDoublingCube() },
                                    shape = RoundedCornerShape(4.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color.Gray.copy(alpha = 0.9f)
                                    ),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(56.dp) // Daha yüksek buton
                                ) {
                                    Text(
                                        text = "↩ İptal",
                                        fontSize = 14.sp, // Daha büyük font
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            }
                        } else {
                            // Dikey mod için orijinal dikey düzenleme - İyileştirilmiş boyutlar
                            Column(
                                modifier = Modifier.padding(8.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                // Kabul Et butonu
                                Button(
                                    onClick = { player2AcceptDouble() },
                                    shape = RoundedCornerShape(4.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color.Green.copy(alpha = 0.9f)
                                    ),
                                    modifier = Modifier
                                        .padding(vertical = 4.dp)
                                        .fillMaxWidth()
                                        .height(48.dp) // Sabit yükseklik
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.Center
                                    ) {
                                        Text("✓", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Kabul Et", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                    }
                                }

                                // Pes Et butonu
                                Button(
                                    onClick = { player2Resign() },
                                    shape = RoundedCornerShape(4.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color.Red.copy(alpha = 0.9f)
                                    ),
                                    modifier = Modifier
                                        .padding(vertical = 4.dp)
                                        .fillMaxWidth()
                                        .height(48.dp) // Sabit yükseklik
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.Center
                                    ) {
                                        Text("✗", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Pes Et", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                    }
                                }

                                // İptal butonu
                                Button(
                                    onClick = { resetDoublingCube() },
                                    shape = RoundedCornerShape(4.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color.Gray.copy(alpha = 0.9f)
                                    ),
                                    modifier = Modifier
                                        .padding(vertical = 4.dp)
                                        .fillMaxWidth()
                                        .height(48.dp) // Sabit yükseklik
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.Center
                                    ) {
                                        Text("↩", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("İptal", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Mevcut el bilgisi
            Text(
                text = "El: $currentRound",
                color = Color.White,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                textAlign = TextAlign.Center
            )

            // Skor Artırma Butonları - isScoreAutomatic değerine göre farklı butonlar gösteriyoruz
            if (isScoreAutomatic) {
                if (isTraditionalGame) {
                    // Geleneksel Tavla için T/M Butonları (Backgammon ve Küp olmadan)
                    if (isLandscape) {
                        // Yatay mod için 3'er grup halinde tasarım - Geleneksel Tavla için
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(2.dp)
                                .height(80.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // SOL TARAF (MAVİ BÖLGE) - Oyuncu 1'in 2 butonu tam genişlik
                            Row(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight(),
                                horizontalArrangement = Arrangement.spacedBy(1.dp) // Minimal boşluk
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
                                        shape = RoundedCornerShape(2.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = Color.White.copy(alpha = 0.3f)
                                        ),
                                        modifier = Modifier
                                            .weight(1f) // Eşit genişlik - bölgeyi 3'e böl
                                            .fillMaxHeight()
                                    ) {
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                            Text(
                                                text = "T",
                                                fontSize = 20.sp,
                                                fontWeight = FontWeight.Bold,
                                                textAlign = TextAlign.Center,
                                                color = Color.White
                                            )
                                            Text(
                                                text = "1P",
                                                fontSize = 12.sp,
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
                                        shape = RoundedCornerShape(2.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = Color.White.copy(alpha = 0.3f)
                                        ),
                                        modifier = Modifier
                                            .weight(1f) // Eşit genişlik - bölgeyi 3'e böl
                                            .fillMaxHeight()
                                    ) {
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                            Text(
                                                text = "M",
                                                fontSize = 20.sp,
                                                fontWeight = FontWeight.Bold,
                                                textAlign = TextAlign.Center,
                                                color = Color.White
                                            )
                                            Text(
                                                text = "2P",
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                textAlign = TextAlign.Center,
                                                color = Color.White
                                            )
                                        }
                                    }
                                }

                                // Boş alan (üçüncü buton yok)
                                Spacer(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxHeight()
                                )
                            }

                            // SAĞ TARAF (KIRMIZI BÖLGE) - Oyuncu 2'nin 2 butonu tam genişlik
                            Row(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight(),
                                horizontalArrangement = Arrangement.spacedBy(1.dp) // Minimal boşluk
                            ) {
                                // Boş alan (üçüncü buton yok)
                                Spacer(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxHeight()
                                )

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
                                        shape = RoundedCornerShape(2.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = Color.White.copy(alpha = 0.3f)
                                        ),
                                        modifier = Modifier
                                            .weight(1f) // Eşit genişlik - bölgeyi 3'e böl
                                            .fillMaxHeight()
                                    ) {
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                            Text(
                                                text = "T",
                                                fontSize = 20.sp,
                                                fontWeight = FontWeight.Bold,
                                                textAlign = TextAlign.Center,
                                                color = Color.White
                                            )
                                            Text(
                                                text = "1P",
                                                fontSize = 12.sp,
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
                                        shape = RoundedCornerShape(2.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = Color.White.copy(alpha = 0.3f)
                                        ),
                                        modifier = Modifier
                                            .weight(1f) // Eşit genişlik - bölgeyi 3'e böl
                                            .fillMaxHeight()
                                    ) {
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                            Text(
                                                text = "M",
                                                fontSize = 20.sp,
                                                fontWeight = FontWeight.Bold,
                                                textAlign = TextAlign.Center,
                                                color = Color.White
                                            )
                                            Text(
                                                text = "2P",
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                textAlign = TextAlign.Center,
                                                color = Color.White
                                            )
                                        }
                                    }
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

                        // ✅ SON HAMLEYİ GERİ AL BUTONU - Geleneksel Tavla için (her zaman görünür)
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = { undoLastRound() },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (undoStack.isNotEmpty()) Color(0xFFFF9800) else Color.Gray
                            ),
                            enabled = undoStack.isNotEmpty(),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(45.dp)
                                .padding(horizontal = 8.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Text("↶", fontSize = 18.sp, color = Color.White)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (undoStack.isNotEmpty()) "Son Hamleyi Geri Al (${undoStack.size})" else "Son Hamleyi Geri Al (0)",
                                    fontSize = 14.sp,
                                    color = Color.White
                                )
                            }
                        }
                    }
                } else {
                    // Modern Tavla için T/M/B Butonları
                    if (isLandscape) {
                        // Yatay mod için bölge bazlı tasarım - Modern Tavla
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(80.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // MAVİ BÖLGE - Oyuncu 1'in T,M,B butonları
                            Row(
                                modifier = Modifier
                                    .weight(1f) // Ekranın yarısı
                                    .fillMaxHeight(),
                                horizontalArrangement = Arrangement.spacedBy(2.dp) // 2dp boşluk
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
                                            .weight(1.8f) // 1.8 kat geniş
                                            .fillMaxHeight()
                                    ) {
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                            Text(
                                                text = "T",
                                                fontSize = 20.sp,
                                                fontWeight = FontWeight.Bold,
                                                textAlign = TextAlign.Center,
                                                color = Color.White
                                            )
                                            Text(
                                                text = "1P",
                                                fontSize = 12.sp,
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
                                            .weight(1.8f) // 1.8 kat geniş
                                            .fillMaxHeight()
                                    ) {
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                            Text(
                                                text = "M",
                                                fontSize = 20.sp,
                                                fontWeight = FontWeight.Bold,
                                                textAlign = TextAlign.Center,
                                                color = Color.White
                                            )
                                            Text(
                                                text = "2P",
                                                fontSize = 12.sp,
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
                                            .weight(1.8f) // 1.8 kat geniş
                                            .fillMaxHeight()
                                    ) {
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                            Text(
                                                text = "B",
                                                fontSize = 20.sp,
                                                fontWeight = FontWeight.Bold,
                                                textAlign = TextAlign.Center,
                                                color = Color.White
                                            )
                                            Text(
                                                text = "3P",
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                textAlign = TextAlign.Center,
                                                color = Color.White
                                            )
                                        }
                                    }
                                }
                            }

                            // KIRMIZI BÖLGE - Oyuncu 2'nin T,M,B butonları
                            Row(
                                modifier = Modifier
                                    .weight(1f) // Ekranın yarısı
                                    .fillMaxHeight(),
                                horizontalArrangement = Arrangement.spacedBy(2.dp) // 2dp boşluk
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
                                            .weight(1.8f) // 1.8 kat geniş
                                            .fillMaxHeight()
                                    ) {
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                            Text(
                                                text = "T",
                                                fontSize = 20.sp,
                                                fontWeight = FontWeight.Bold,
                                                textAlign = TextAlign.Center,
                                                color = Color.White
                                            )
                                            Text(
                                                text = "1P",
                                                fontSize = 12.sp,
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
                                            .weight(1.8f) // 1.8 kat geniş
                                            .fillMaxHeight()
                                    ) {
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                            Text(
                                                text = "M",
                                                fontSize = 20.sp,
                                                fontWeight = FontWeight.Bold,
                                                textAlign = TextAlign.Center,
                                                color = Color.White
                                            )
                                            Text(
                                                text = "2P",
                                                fontSize = 12.sp,
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
                                            .weight(1.8f) // 1.8 kat geniş
                                            .fillMaxHeight()
                                    ) {
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                            Text(
                                                text = "B",
                                                fontSize = 20.sp,
                                                fontWeight = FontWeight.Bold,
                                                textAlign = TextAlign.Center,
                                                color = Color.White
                                            )
                                            Text(
                                                text = "3P",
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                textAlign = TextAlign.Center,
                                                color = Color.White
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // ✅ SON HAMLEYİ GERİ AL BUTONU - Modern Tavla için (her zaman görünür)
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = { undoLastRound() },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (undoStack.isNotEmpty()) Color(0xFFFF9800) else Color.Gray
                            ),
                            enabled = undoStack.isNotEmpty(),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(45.dp)
                                .padding(horizontal = 8.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Text("↶", fontSize = 18.sp, color = Color.White)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (undoStack.isNotEmpty()) "Son Hamleyi Geri Al (${undoStack.size})" else "Son Hamleyi Geri Al (0)",
                                    fontSize = 14.sp,
                                    color = Color.White
                                )
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
            } else {
                // Manuel skor artırma butonları - Manuel modda gösteriliyor
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp, vertical = 4.dp)
                ) {
                    // Sol taraf (Mavi) - Artı buton
                    Button(
                        onClick = {
                            // Skoru manuel olarak artır
                            player1Score++
                        },
                        shape = RoundedCornerShape(4.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.3f)),
                        modifier = Modifier
                            .weight(1f)
                            .height(60.dp)
                            .padding(horizontal = 2.dp)
                    ) {
                        Text(
                            text = "+",
                            fontSize = 20.sp,
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
                            .height(60.dp)
                            .padding(horizontal = 2.dp)
                    ) {
                        Text(
                            text = "-",
                            fontSize = 20.sp,
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
                        },
                        shape = RoundedCornerShape(4.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = purpleColor),
                        modifier = Modifier
                            .weight(1f)
                            .height(60.dp)
                            .padding(horizontal = 2.dp)
                    ) {
                        Text(
                            text = "C",
                            fontSize = 20.sp,
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
                            .height(60.dp)
                            .padding(horizontal = 2.dp)
                    ) {
                        Text(
                            text = "-",
                            fontSize = 20.sp,
                            textAlign = TextAlign.Center,
                            color = Color.White
                        )
                    }

                    // Sağ taraf (Kırmızı) - Artı buton
                    Button(
                        onClick = {
                            // Skoru manuel olarak artır
                            player2Score++
                        },
                        shape = RoundedCornerShape(4.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.3f)),
                        modifier = Modifier
                            .weight(1f)
                            .height(60.dp)
                            .padding(horizontal = 2.dp)
                    ) {
                        Text(
                            text = "+",
                            fontSize = 20.sp,
                            textAlign = TextAlign.Center,
                            color = Color.White
                        )
                    }
                }

                // Manuel modda kaydetme butonu ekliyoruz
                Button(
                    onClick = {
                        // Mevcut puanları kaydet
                        if (player1Score > player2Score) {
                            // Oyuncu 1 kazandı
                            val difference = player1Score - player2Score
                            val winType = when {
                                difference >= 3 && !isTraditionalGame -> "BACKGAMMON"
                                difference >= 2 -> "MARS"
                                else -> "SINGLE"
                            }
                            addRound(player1Id, player1Name, winType, difference)
                        } else if (player2Score > player1Score) {
                            // Oyuncu 2 kazandı
                            val difference = player2Score - player1Score
                            val winType = when {
                                difference >= 3 && !isTraditionalGame -> "BACKGAMMON"
                                difference >= 2 -> "MARS"
                                else -> "SINGLE"
                            }
                            addRound(player2Id, player2Name, winType, difference)
                        } else {
                            // Beraberlik - geçersiz durum
                            Toast.makeText(
                                context,
                                "Beraberlik olamaz! Lütfen geçerli bir skor girin.",
                                Toast.LENGTH_SHORT
                            ).show()
                        }

                        // Skorları sıfırla
                        player1Score = 0
                        player2Score = 0
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Text("Bu Eli Kaydet")
                }
            }


            // Maçı sonlandırma butonu
            Button(
                onClick = { showEndMatchConfirmation = true },
                colors = ButtonDefaults.buttonColors(containerColor = Color.Red),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(45.dp)
                    .padding(horizontal = 16.dp, vertical = 4.dp)
            ) {
                Text("Maçı Sonlandır", color = Color.White)
            }
        }

        // Katlama Zarı - Sadece Modern tavla için görünür
        if (!isTraditionalGame) {
            Box(
                modifier = Modifier
                    .offset(x = animatedXOffset, y = animatedYOffset)
                    .size(60.dp)
                    .align(Alignment.Center)
                    .background(Color.White, RoundedCornerShape(8.dp))
                    .border(2.dp, Color.Black, RoundedCornerShape(8.dp))
                    .clickable {
                        // Zarın pozisyonuna göre tıklama işlevi
                        when (doublingCubePosition) {
                            DoublingCubePosition.PLAYER1_CONTROL -> if (player1CanDouble) player1OfferDouble()
                            DoublingCubePosition.PLAYER2_CONTROL -> if (player2CanDouble) player2OfferDouble()
                            else -> {} // Diğer pozisyonlarda tıklama işlevi yok
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = doublingCubeValue.toString(),
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )
            }
        }
    }

    // Aktivite sonlandığında yapılacak işlemler
    DisposableEffect(Unit) {
        onDispose {
            // Eğer maç bitmeden aktivite kapatılırsa, maçı sonlandır
            if (!showMatchEndDialog && matchId != -1L) {
                dbHelper.finishMatch(matchId)
            }
        }
    }
}