package com.tavla.tavlapp

import android.content.Intent
import android.os.Bundle
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
                        useDiceRoller = useDiceRoller,
                        useTimer = useTimer,
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
    useDiceRoller: Boolean,
    useTimer: Boolean,
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

    // Crawford kuralı için yeni değişkenler
    var matchTargetScore by remember { mutableIntStateOf(11) } // Parti hedef puanı
    var isCrawfordGame by remember { mutableStateOf(false) } // Şu an Crawford eli mi?
    var crawfordGamePlayed by remember { mutableStateOf(false) } // Crawford eli daha önce oynanmış mı?
    var isPostCrawford by remember { mutableStateOf(false) } // Post-Crawford durumu mu?

    var showEndMatchConfirmation by remember { mutableStateOf(false) }

    // Zar atma ekranı state'i
    var showDiceScreen by remember { mutableStateOf(false) }

    // Otomatik zar ekranı açma
    LaunchedEffect(useDiceRoller, useTimer) {
        if (useDiceRoller || useTimer) {
            showDiceScreen = true
        }
    }

    // ✅ RECOMPOSE ETKİSİ - LaunchedEffect ekle
    LaunchedEffect(recomposeKey) {
        // Bu blok recomposeKey değiştiğinde çalışır ve UI'ı günceller
        // İçeriği boş bırakabilirsin, sadece recompose tetiklemek için
    }

    // Crawford durumunu kontrol eden fonksiyon
    fun checkCrawfordStatus() {
        if (!crawfordGamePlayed && !isCrawfordGame) {
            // Crawford eli henüz oynanmamışsa kontrol et
            if (player1Score == matchTargetScore - 1 || player2Score == matchTargetScore - 1) {
                // Birisi hedef puanın 1 eksiğine ulaştı, bir sonraki el Crawford eli
                isCrawfordGame = true
            }
        }
    }

    // Crawford elinin bitişini kontrol eden fonksiyon
    fun handleCrawfordGameEnd() {
        if (isCrawfordGame) {
            crawfordGamePlayed = true
            isCrawfordGame = false

            // Crawford elinden sonra eğer parti devam ediyorsa Post-Crawford moduna geç
            if (player1Score < matchTargetScore && player2Score < matchTargetScore) {
                isPostCrawford = true
            }
        }
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

        // Crawford kontrolü yap
        handleCrawfordGameEnd()
        checkCrawfordStatus()

        // Hedef puana ulaşıldıysa maçı bitir
        if (player1Score >= matchTargetScore || player2Score >= matchTargetScore) {
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

                    // Butonlar için yeterli alan - Üst kısımda konumlandır
                    Spacer(modifier = Modifier.height(12.dp))

                    // Düğmeler alanı - Compact düzen
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        // Katla butonu - Oyuncu 1 için
                        if (!showPlayer1DoublingMenu && !showPlayer2DoublingMenu &&
                            (doublingCubePosition == DoublingCubePosition.CENTER ||
                                    doublingCubePosition == DoublingCubePosition.PLAYER1_CONTROL) &&
                            player1CanDouble &&
                            !isCrawfordGame // Crawford elinde küp kullanımı devre dışı
                        ) {
                            Button(
                                onClick = { player1OfferDouble() },
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF4CAF50).copy(alpha = 0.9f)
                                ),
                                modifier = Modifier
                                    .fillMaxWidth(0.9f)
                                    .height(35.dp)
                            ) {
                                Text(
                                    text = "⚡ KATLA",
                                    color = Color.White,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                        
                        // Eski geri alma butonu kaldırıldı - artık altta tek buton var
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
                                        .height(45.dp) // Daha ince cevap butonları
                                ) {
                                    Text(
                                        text = "✓ Kabul Et",
                                        fontSize = 20.sp, // Büyük font
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
                                        .height(45.dp) // Daha ince cevap butonları
                                ) {
                                    Text(
                                        text = "✗ Pes Et",
                                        fontSize = 20.sp, // Büyük font
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
                                        .height(45.dp) // Daha ince cevap butonları
                                ) {
                                    Text(
                                        text = "↩ İptal",
                                        fontSize = 20.sp, // Büyük font
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
                                        .height(40.dp) // Daha ince dikey mod cevap butonları
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
                                        .height(40.dp) // Daha ince dikey mod cevap butonları
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
                                        .height(40.dp) // Daha ince dikey mod cevap butonları
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

                // ORTA KISIM - Zar atma butonu ve hedef puan kutusu
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.offset(y = (-50).dp)
                ) {
                    // ZAR AT butonu
                    Button(
                        onClick = { showDiceScreen = true },
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF9C27B0).copy(alpha = 0.9f)
                        ),
                        modifier = Modifier
                            .width(100.dp)
                            .height(30.dp)
                    ) {
                        Text(
                            text = "🎲 ZAR AT",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

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

                    // Butonlar için yeterli alan - Üst kısımda konumlandır
                    Spacer(modifier = Modifier.height(12.dp))

                    // Düğmeler alanı - Compact düzen
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        // Katla butonu - Oyuncu 2 için
                        if (!showPlayer1DoublingMenu && !showPlayer2DoublingMenu &&
                            (doublingCubePosition == DoublingCubePosition.CENTER ||
                                    doublingCubePosition == DoublingCubePosition.PLAYER2_CONTROL) &&
                            player2CanDouble &&
                            !isCrawfordGame // Crawford elinde küp kullanımı devre dışı
                        ) {
                            Button(
                                onClick = { player2OfferDouble() },
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF4CAF50).copy(alpha = 0.9f)
                                ),
                                modifier = Modifier
                                    .fillMaxWidth(0.9f)
                                    .height(35.dp)
                            ) {
                                Text(
                                    text = "⚡ KATLA",
                                    color = Color.White,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                        
                        // Eski geri alma butonu kaldırıldı - artık altta tek buton var
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
                                        .height(45.dp) // Daha ince cevap butonları
                                ) {
                                    Text(
                                        text = "✓ Kabul Et",
                                        fontSize = 20.sp, // Büyük font
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
                                        .height(45.dp) // Daha ince cevap butonları
                                ) {
                                    Text(
                                        text = "✗ Pes Et",
                                        fontSize = 20.sp, // Büyük font
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
                                        .height(45.dp) // Daha ince cevap butonları
                                ) {
                                    Text(
                                        text = "↩ İptal",
                                        fontSize = 20.sp, // Büyük font
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
                                        .height(40.dp) // Daha ince dikey mod cevap butonları
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
                                        .height(40.dp) // Daha ince dikey mod cevap butonları
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
                                        .height(40.dp) // Daha ince dikey mod cevap butonları
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
                        // Yatay mod için özel büyük buton tasarımı - Geleneksel Tavla için
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(4.dp)
                                .height(70.dp), // Yükseklik daha basık
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // SOL TARAF (MAVİ BÖLGE) BUTONLARI - Tek bir Row içinde
                            Row(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .padding(end = 2.dp),
                                horizontalArrangement = Arrangement.SpaceEvenly
                            ) {
                                // T butonu - ağırlık 1 ile tam alanı kaplasın
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
                                            .weight(1f)
                                            .fillMaxHeight()
                                            .padding(horizontal = 4.dp)
                                    ) {
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                            Text(
                                                text = "T",
                                                fontSize = 28.sp, // Çok büyük font
                                                fontWeight = FontWeight.Bold,
                                                textAlign = TextAlign.Center,
                                                color = Color.White
                                            )
                                            Text(
                                                text = "1P",
                                                fontSize = 24.sp, // Çok büyük font
                                                fontWeight = FontWeight.Bold,
                                                textAlign = TextAlign.Center,
                                                color = Color.White
                                            )
                                        }
                                    }
                                }

                                // M butonu
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
                                            .weight(1f)
                                            .fillMaxHeight()
                                            .padding(horizontal = 4.dp)
                                    ) {
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                            Text(
                                                text = "M",
                                                fontSize = 24.sp,
                                                fontWeight = FontWeight.Bold,
                                                textAlign = TextAlign.Center,
                                                color = Color.White
                                            )
                                            Text(
                                                text = "2P",
                                                fontSize = 14.sp,
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
                                    .fillMaxHeight()
                                    .padding(start = 2.dp),
                                horizontalArrangement = Arrangement.SpaceEvenly
                            ) {
                                // Oyuncu 2 için T butonu
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
                                            .weight(1f)
                                            .fillMaxHeight()
                                            .padding(horizontal = 4.dp)
                                    ) {
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                            Text(
                                                text = "T",
                                                fontSize = 24.sp,
                                                fontWeight = FontWeight.Bold,
                                                textAlign = TextAlign.Center,
                                                color = Color.White
                                            )
                                            Text(
                                                text = "1P",
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.Bold,
                                                textAlign = TextAlign.Center,
                                                color = Color.White
                                            )
                                        }
                                    }
                                }

                                // Oyuncu 2 için M butonu
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
                                            .weight(1f)
                                            .fillMaxHeight()
                                            .padding(horizontal = 4.dp)
                                    ) {
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                            Text(
                                                text = "M",
                                                fontSize = 24.sp,
                                                fontWeight = FontWeight.Bold,
                                                textAlign = TextAlign.Center,
                                                color = Color.White
                                            )
                                            Text(
                                                text = "2P",
                                                fontSize = 14.sp,
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
                    }
                } else {
                    // Modern Tavla için T/M/B Butonları
                    if (isLandscape) {
                        // Yatay mod için özel büyük buton tasarımı - Modern Tavla
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(4.dp)
                                .height(70.dp), // Yükseklik daha basık
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // SOL TARAF (MAVİ BÖLGE) BUTONLARI - Tek bir Row içinde
                            Row(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .padding(end = 2.dp),
                                horizontalArrangement = Arrangement.SpaceEvenly
                            ) {
                                // T butonu - ağırlık 1 ile tam alanı kaplasın
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
                                            .weight(1f)
                                            .fillMaxHeight()
                                            .padding(horizontal = 4.dp)
                                    ) {
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                            Text(
                                                text = "T",
                                                fontSize = 28.sp, // Çok büyük font
                                                fontWeight = FontWeight.Bold,
                                                textAlign = TextAlign.Center,
                                                color = Color.White
                                            )
                                            Text(
                                                text = "1P",
                                                fontSize = 24.sp, // Çok büyük font
                                                fontWeight = FontWeight.Bold,
                                                textAlign = TextAlign.Center,
                                                color = Color.White
                                            )
                                        }
                                    }
                                }

                                // M butonu
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
                                            .weight(1f)
                                            .fillMaxHeight()
                                            .padding(horizontal = 4.dp)
                                    ) {
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                            Text(
                                                text = "M",
                                                fontSize = 24.sp,
                                                fontWeight = FontWeight.Bold,
                                                textAlign = TextAlign.Center,
                                                color = Color.White
                                            )
                                            Text(
                                                text = "2P",
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.Bold,
                                                textAlign = TextAlign.Center,
                                                color = Color.White
                                            )
                                        }
                                    }
                                }

                                // B butonu
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
                                            .weight(1f)
                                            .fillMaxHeight()
                                            .padding(horizontal = 4.dp)
                                    ) {
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                            Text(
                                                text = "B",
                                                fontSize = 24.sp,
                                                fontWeight = FontWeight.Bold,
                                                textAlign = TextAlign.Center,
                                                color = Color.White
                                            )
                                            Text(
                                                text = "3P",
                                                fontSize = 14.sp,
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
                                    .fillMaxHeight()
                                    .padding(start = 2.dp),
                                horizontalArrangement = Arrangement.SpaceEvenly
                            ) {
                                // Oyuncu 2 için T butonu
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
                                            .weight(1f)
                                            .fillMaxHeight()
                                            .padding(horizontal = 4.dp)
                                    ) {
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                            Text(
                                                text = "T",
                                                fontSize = 24.sp,
                                                fontWeight = FontWeight.Bold,
                                                textAlign = TextAlign.Center,
                                                color = Color.White
                                            )
                                            Text(
                                                text = "1P",
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.Bold,
                                                textAlign = TextAlign.Center,
                                                color = Color.White
                                            )
                                        }
                                    }
                                }

                                // Oyuncu 2 için M butonu
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
                                            .weight(1f)
                                            .fillMaxHeight()
                                            .padding(horizontal = 4.dp)
                                    ) {
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                            Text(
                                                text = "M",
                                                fontSize = 24.sp,
                                                fontWeight = FontWeight.Bold,
                                                textAlign = TextAlign.Center,
                                                color = Color.White
                                            )
                                            Text(
                                                text = "2P",
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.Bold,
                                                textAlign = TextAlign.Center,
                                                color = Color.White
                                            )
                                        }
                                    }
                                }

                                // Oyuncu 2 için B butonu
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
                                            .weight(1f)
                                            .fillMaxHeight()
                                            .padding(horizontal = 4.dp)
                                    ) {
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                            Text(
                                                text = "B",
                                                fontSize = 24.sp,
                                                fontWeight = FontWeight.Bold,
                                                textAlign = TextAlign.Center,
                                                color = Color.White
                                            )
                                            Text(
                                                text = "3P",
                                                fontSize = 14.sp,
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
                            // Crawford değişkenlerini sıfırla
                            isCrawfordGame = false
                            crawfordGamePlayed = false
                            isPostCrawford = false
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
                        // Crawford değişkenlerini sıfırla (el kaydedildikten sonra)
                        // Not: Bu durumda zaten addRound fonksiyonu çağrılacak
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Text("Bu Eli Kaydet")
                }
            }

            // ✅ Geri alma, zar atma ve maçı sonlandırma butonları - aynı satırda
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Son hamleyi geri al butonu - Koyu mavi
                Button(
                    onClick = { undoLastRound() },
                    enabled = undoStack.isNotEmpty(), // Pasif durumda göster
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF0D47A1), // Koyu mavi (iki ton koyu)
                        disabledContainerColor = Color(0xFFBDBDBD) // Gri (pasif)
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

                // Zar at butonu - Ortada mor renk
                Button(
                    onClick = { showDiceScreen = true },
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
                        Text("🎲", fontSize = 16.sp)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Zar At", color = Color.White, fontSize = 12.sp)
                    }
                }

                // Maçı sonlandırma butonu - Koyu kırmızı
                Button(
                    onClick = { showEndMatchConfirmation = true },
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
        LaunchedEffect(showDiceScreen) {
            val intent = Intent(context, DiceActivity::class.java).apply {
                putExtra("game_type", gameType)
                putExtra("use_dice_roller", useDiceRoller)
                putExtra("use_timer", useTimer)
                putExtra("player1_name", player1Name)
                putExtra("player2_name", player2Name)
            }
            context.startActivity(intent)
            showDiceScreen = false
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
