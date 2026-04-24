package com.tavla.tavlapp

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.tavla.tavlapp.UIComponents.MatchListItemWithDelete
import com.tavla.tavlapp.UIComponents.PlayerListItem
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.shape.RoundedCornerShape

// Oyun geçmişi ekranı aktivitesi
class GameHistoryActivity : ComponentActivity() {
    private lateinit var dbHelper: DatabaseHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        dbHelper = DatabaseHelper(this)

        setContent {
            TavlaAppTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    GameHistoryScreen(dbHelper) {
                        finish() // Aktiviteyi sonlandır
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GameHistoryScreen(dbHelper: DatabaseHelper, onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // Görünüm modu için durum değişkeni
    var viewMode by remember { mutableStateOf("Tüm Maçlar") }
    val viewModeOptions = listOf("Tüm Maçlar", "Oyuncu İstatistikleri", "İkili Karşılaşmalar", "Rövanşlı Karşılaşmalar")

    // Silme modu için durum değişkeni
    var isDeleteMode by remember { mutableStateOf(false) }

    // Seçilen maçlar için durum değişkeni
    val selectedMatches = remember { mutableStateListOf<Long>() }

    // Tüm maçları getir - değişiklikler olduğunda otomatik yenile
    var refreshTrigger by remember { mutableStateOf(0) }
    val matches by remember(refreshTrigger) {
        mutableStateOf(dbHelper.getAllMatches())
    }

    // Tüm oyuncuları getir (oyuncu isimlerini bulmak için)
    val players = remember(refreshTrigger) {
        dbHelper.getAllPlayers().associateBy { it.id }
    }

    // Dialog gösterimi için durum değişkenleri
    var showDeleteAllDialog by remember { mutableStateOf(false) }
    var showDeleteSelectedDialog by remember { mutableStateOf(false) }
    var matchToDelete by remember { mutableStateOf<Long?>(null) }


// var showDeleteAllDialog by remember { mutableStateOf(false) } // Bu var olan değişken
    var showPasswordDialog by remember { mutableStateOf(false) } // Yeni değişken
    var password by remember { mutableStateOf("") } // Yeni değişken

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Oyun Geçmişi") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Geri"
                        )
                    }
                },
                actions = {
                    if (viewMode == "Tüm Maçlar" && matches.isNotEmpty()) {
                        if (isDeleteMode) {
                            // Silme modunda iken gösterilen butonlar
                            IconButton(
                                onClick = {
                                    // Hiçbir maç seçilmediyse iptal et
                                    if (selectedMatches.isEmpty()) {
                                        isDeleteMode = false
                                    } else {
                                        showDeleteSelectedDialog = true
                                    }
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Seçilenleri Sil",
                                    tint = if (selectedMatches.isNotEmpty()) Color.Red else Color.Gray
                                )
                            }

                            IconButton(
                                onClick = {
                                    isDeleteMode = false
                                    selectedMatches.clear()
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "İptal"
                                )
                            }
                        } else {
                            // Normal moddaki butonlar
                            IconButton(
                                onClick = { isDeleteMode = true }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.MoreVert,
                                    contentDescription = "Diğer İşlemler"
                                )
                            }
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Görünüm modu seçimi
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly // SpaceBetween yerine SpaceEvenly
            ) {
                viewModeOptions.forEach { option ->
                    FilterChip(
                        selected = viewMode == option,
                        onClick = {
                            viewMode = option
                            isDeleteMode = false
                            selectedMatches.clear()
                        },
                        label = {
                            Text(
                                text = option,
                                maxLines = 2, // Maksimum 2 satır
                                overflow = TextOverflow.Ellipsis, // Metin taşarsa ...
                                textAlign = TextAlign.Center, // Ortalanmış metin
                                fontSize = 11.sp, // 4 sekme sığması için küçük font
                                lineHeight = 13.sp // Satır yüksekliği
                            )
                        },
                        modifier = Modifier
                            .weight(1f) // Eşit genişlik
                            .padding(horizontal = 4.dp) // Butonlar arası boşluk
                            .height(48.dp) // Sabit yükseklik
                    )
                }
            }

            // Silme modu aktifse göster
            if (isDeleteMode && viewMode == "Tüm Maçlar" && matches.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("${selectedMatches.size} maç seçildi")

                    Row {
                        // Tümünü Seç/Temizle
                        TextButton(
                            onClick = {
                                if (selectedMatches.size == matches.size) {
                                    // Tümü seçiliyse, seçimi temizle
                                    selectedMatches.clear()
                                } else {
                                    // Değilse, tümünü seç
                                    selectedMatches.clear()
                                    selectedMatches.addAll(matches.map { it.id })
                                }
                            }
                        ) {
                            Text(
                                if (selectedMatches.size == matches.size) "Seçimi Temizle" else "Tümünü Seç"
                            )
                        }

                        // Tümünü Sil
                        TextButton(
                            onClick = { showDeleteAllDialog = true }
                        ) {
                            Text(
                                "Tümünü Sil",
                                color = Color.Red
                            )
                        }
                    }
                }
            }

            // Seçilen moda göre farklı içerik göster
            when (viewMode) {
                "Tüm Maçlar" -> {
                    if (matches.isEmpty()) {
                        // Hiç maç yoksa boş ekran göster
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Henüz kaydedilmiş oyun bulunmamaktadır",
                                style = MaterialTheme.typography.bodyLarge,
                                textAlign = TextAlign.Center
                            )
                        }
                    } else {
                        // Maç listesini göster
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 16.dp)
                        ) {
                            items(matches) { match ->
                                MatchListItemWithDelete(
                                    match = match,
                                    players = players,
                                    isDeleteMode = isDeleteMode,
                                    isSelected = selectedMatches.contains(match.id),
                                    onItemClick = { matchId ->
                                        if (isDeleteMode) {
                                            // Silme modunda ise, seçim listesine ekle/çıkar
                                            if (selectedMatches.contains(matchId)) {
                                                selectedMatches.remove(matchId)
                                            } else {
                                                selectedMatches.add(matchId)
                                            }
                                        } else {
                                            // Normal modda maç detayına git
                                            val intent =
                                                Intent(context, MatchDetailActivity::class.java)
                                            intent.putExtra("match_id", matchId)
                                            context.startActivity(intent)
                                        }
                                    },
                                    onDeleteClick = {
                                        matchToDelete = it
                                    }
                                )
                            }
                        }
                    }
                }

                "Oyuncu İstatistikleri" -> {
                    if (players.isEmpty()) {
                        // Hiç oyuncu yoksa boş ekran göster
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Henüz kaydedilmiş oyuncu bulunmamaktadır",
                                style = MaterialTheme.typography.bodyLarge,
                                textAlign = TextAlign.Center
                            )
                        }
                    } else {
                        // Oyuncu listesini göster
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 16.dp)
                        ) {
                            items(players.values.toList()) { player ->
                                PlayerListItem(player) { playerId ->
                                    // Oyuncu istatistik ekranına git
                                    val intent = Intent(context, PlayerStatsActivity::class.java)
                                    intent.putExtra("player_id", playerId)
                                    context.startActivity(intent)
                                }
                            }
                        }
                    }
                }

                "İkili Karşılaşmalar" -> {
                    if (players.size < 2) {
                        // Yeterli oyuncu yoksa boş ekran göster
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "İkili karşılaşma için en az 2 oyuncu gereklidir",
                                style = MaterialTheme.typography.bodyLarge,
                                textAlign = TextAlign.Center
                            )
                        }
                    } else {
                        // İkili karşılaşma ekranına git butonu
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Button(
                                onClick = {
                                    val intent = Intent(context, PlayerVsPlayerActivity::class.java)
                                    context.startActivity(intent)
                                }
                            ) {
                                Text("İkili Karşılaşma İstatistiklerini Görüntüle")
                            }
                        }
                    }
                }

                "Rövanşlı Karşılaşmalar" -> {
                    // Karşılaşma verileri
                    // Rövanş silme onay dialog state'leri
                    var showRematchDeleteDialog by remember { mutableStateOf(false) }
                    var pendingDeleteEncounterId by remember { mutableStateOf(-1L) }
                    var pendingDeleteEncounterName by remember { mutableStateOf("") }

                    var rematchEncounters by remember(refreshTrigger) {
                        mutableStateOf(dbHelper.getAllRematchEncounters())
                    }
                    var allEncounterStats by remember(refreshTrigger) {
                        mutableStateOf(mapOf<Long, List<RematchEncounterStats>>())
                    }

                    // İstatistikleri yükle
                    LaunchedEffect(rematchEncounters) {
                        val statsMap = mutableMapOf<Long, List<RematchEncounterStats>>()
                        rematchEncounters.forEach { enc ->
                            statsMap[enc.id] = dbHelper.getRematchEncounterStats(enc.id)
                        }
                        allEncounterStats = statsMap
                    }

                    if (rematchEncounters.isEmpty()) {
                        // Boş durum
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = "Henüz rövanşlı karşılaşma bulunmamaktadır",
                                    style = MaterialTheme.typography.bodyLarge,
                                    textAlign = TextAlign.Center,
                                    color = Color.Gray
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Button(
                                    onClick = {
                                        val intent = Intent(context, RematchProgressActivity::class.java)
                                        context.startActivity(intent)
                                    }
                                ) {
                                    Text("Yeni Karşılaşma Başlat")
                                }
                            }
                        }
                    } else {
                        Column(modifier = Modifier.fillMaxSize()) {
                            // Üst bar: Özet bilgi + Yeni butonu
                            val activeCount = rematchEncounters.count {
                                it.status == RematchStatus.ACTIVE || it.status == RematchStatus.ROUND2_ACTIVE
                            }
                            val completedCount = rematchEncounters.count {
                                it.status == RematchStatus.COMPLETED
                            }

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "${rematchEncounters.size} karşılaşma (${activeCount} aktif, ${completedCount} tamamlanmış)",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.Gray
                                )
                                Button(
                                    onClick = {
                                        val intent = Intent(context, RematchProgressActivity::class.java)
                                        context.startActivity(intent)
                                    },
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                    modifier = Modifier.height(32.dp)
                                ) {
                                    Text("Yeni", fontSize = 12.sp)
                                }
                            }

                            // Karşılaşma listesi
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 16.dp)
                            ) {
                                items(rematchEncounters) { encounter ->
                                    val stats = allEncounterStats[encounter.id] ?: emptyList()
                                    RematchEncounterCard(
                                        encounter = encounter,
                                        stats = stats,
                                        onContinue = {
                                            val intent = Intent(context, GameScoreActivity::class.java)
                                            intent.putExtra("is_rematch_mode", true)
                                            intent.putExtra("encounter_id", encounter.id)
                                            intent.putExtra("player1_id", encounter.player1Id)
                                            intent.putExtra("player2_id", encounter.player2Id)
                                            intent.putExtra("player1_name", encounter.player1Name)
                                            intent.putExtra("player2_name", encounter.player2Name)
                                            intent.putExtra("total_parties", encounter.totalParties)
                                            intent.putExtra("rounds", encounter.targetScore)
                                            context.startActivity(intent)
                                        },
                                        onCompare = {
                                            val intent = Intent(context, RematchComparisonActivity::class.java)
                                            intent.putExtra("encounter_id", encounter.id)
                                            context.startActivity(intent)
                                        },
                                        onCardClick = {
                                            val intent = Intent(context, RematchProgressActivity::class.java)
                                            context.startActivity(intent)
                                        },
                                        onDelete = {
                                            pendingDeleteEncounterId = encounter.id
                                            pendingDeleteEncounterName = "${encounter.player1Name} vs ${encounter.player2Name}"
                                            showRematchDeleteDialog = true
                                        }
                                    )
                                }
                            }

                            // Rövanş karşılaşma silme onay dialog'u
                            if (showRematchDeleteDialog) {
                                AlertDialog(
                                    onDismissRequest = { showRematchDeleteDialog = false },
                                    title = { Text("Karşılaşmayı Sil") },
                                    text = {
                                        Column {
                                            Text("Bu karşılaşmayı arşivden çıkarmak istediğinizden emin misiniz?")
                                            Spacer(modifier = Modifier.height(8.dp))
                                            Text(
                                                pendingDeleteEncounterName,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                "Tüm parti sonuçları, zar setleri ve istatistikler silinecektir.",
                                                fontSize = 12.sp,
                                                color = Color.Gray
                                            )
                                        }
                                    },
                                    confirmButton = {
                                        TextButton(
                                            onClick = {
                                                dbHelper.deleteRematchEncounter(pendingDeleteEncounterId)
                                                showRematchDeleteDialog = false
                                                refreshTrigger++
                                            }
                                        ) {
                                            Text("Evet, Sil", color = Color(0xFFF44336))
                                        }
                                    },
                                    dismissButton = {
                                        TextButton(onClick = { showRematchDeleteDialog = false }) {
                                            Text("Vazgeç")
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }

        // Tek maç silme onay dialogu
        if (matchToDelete != null) {
            AlertDialog(
                onDismissRequest = { matchToDelete = null },
                title = { Text("Maçı Sil") },
                text = { Text("Bu maçı silmek istediğinize emin misiniz? Bu işlem geri alınamaz.") },
                confirmButton = {
                    TextButton(
                        onClick = {
                            val matchId = matchToDelete!!
                            scope.launch {
                                val result = dbHelper.deleteMatch(matchId)
                                if (result > 0) {
                                    // Silme başarılı, listeyi güncelle
                                    refreshTrigger++
                                    Toast.makeText(context, "Maç silindi", Toast.LENGTH_SHORT)
                                        .show()
                                } else {
                                    Toast.makeText(context, "Maç silinemedi", Toast.LENGTH_SHORT)
                                        .show()
                                }
                                matchToDelete = null
                            }
                        }
                    ) {
                        Text("Evet, Sil", color = Color.Red)
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = { matchToDelete = null }
                    ) {
                        Text("İptal")
                    }
                }
            )
        }

        // Seçili maçları silme onay dialogu
        if (showDeleteSelectedDialog) {
            AlertDialog(
                onDismissRequest = { showDeleteSelectedDialog = false },
                title = { Text("Seçili Maçları Sil") },
                text = { Text("${selectedMatches.size} maçı silmek istediğinize emin misiniz? Bu işlem geri alınamaz.") },
                confirmButton = {
                    TextButton(
                        onClick = {
                            scope.launch {
                                val result = dbHelper.deleteMatches(selectedMatches.toList())
                                if (result > 0) {
                                    // Silme başarılı, listeyi güncelle
                                    refreshTrigger++
                                    Toast.makeText(
                                        context,
                                        "$result maç silindi",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                } else {
                                    Toast.makeText(context, "Maçlar silinemedi", Toast.LENGTH_SHORT)
                                        .show()
                                }
                                showDeleteSelectedDialog = false
                                isDeleteMode = false
                                selectedMatches.clear()
                            }
                        }
                    ) {
                        Text("Evet, Sil", color = Color.Red)
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = { showDeleteSelectedDialog = false }
                    ) {
                        Text("İptal")
                    }
                }
            )
        }

        // Tüm maçları silme onay dialogu
        if (showDeleteAllDialog) {
            AlertDialog(
                onDismissRequest = { showDeleteAllDialog = false },
                title = { Text("Tüm Verileri Sıfırla") },
                text = {
                    Text("Tüm maç geçmişini ve oyuncu istatistiklerini sıfırlamak istediğinize emin misiniz? Bu işlem geri alınamaz!")
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            scope.launch {
                                val result = dbHelper.resetAllData()  // Yeni fonksiyonumuzu çağırın
                                if (result > 0) {
                                    // Silme başarılı, listeyi güncelle
                                    refreshTrigger++
                                    Toast.makeText(
                                        context,
                                        "Tüm veriler sıfırlandı ($result maç silindi)",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                } else {
                                    Toast.makeText(
                                        context,
                                        "Silinecek veri bulunamadı",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                                showDeleteAllDialog = false
                            }
                        }
                    ) {
                        Text("Evet, Tümünü Sıfırla", color = Color.Red)
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = { showDeleteAllDialog = false }
                    ) {
                        Text("İptal")
                    }
                }
            )
        }
    }
}

@Composable
fun RematchEncounterCard(
    encounter: RematchEncounter,
    stats: List<RematchEncounterStats>,
    onContinue: () -> Unit,
    onCompare: () -> Unit,
    onCardClick: () -> Unit,
    onDelete: () -> Unit = {}
) {
    // Durum rengi ve metni
    val (statusColor, statusText) = when (encounter.status) {
        RematchStatus.ACTIVE -> Color(0xFF4CAF50) to "Aktif"
        RematchStatus.ROUND1_COMPLETE -> Color(0xFFFF9800) to "Rövanş Bekliyor"
        RematchStatus.ROUND2_ACTIVE -> Color(0xFFFF9800) to "Rövanş"
        RematchStatus.COMPLETED -> Color(0xFF2196F3) to "Tamamlandı"
        RematchStatus.CANCELLED -> Color(0xFFF44336) to "İptal"
    }

    // İstatistik verileri
    val player1Stats = stats.find { it.playerId == encounter.player1Id }
    val player2Stats = stats.find { it.playerId == encounter.player2Id }

    val p1Parties = player1Stats?.totalPartiesWon ?: 0
    val p2Parties = player2Stats?.totalPartiesWon ?: 0
    val p1Games = player1Stats?.totalGamesWon ?: 0
    val p2Games = player2Stats?.totalGamesWon ?: 0
    val p1Points = player1Stats?.totalPoints ?: 0
    val p2Points = player2Stats?.totalPoints ?: 0

    // İlerleme hesaplama
    val totalPartiesPlayed = p1Parties + p2Parties
    val totalPartiesTarget = encounter.totalParties * 2 // İki tur
    val progress = if (totalPartiesTarget > 0) {
        totalPartiesPlayed.toFloat() / totalPartiesTarget.toFloat()
    } else 0f
    val progressPercent = (progress * 100).toInt()

    // Tarih formatı
    val formattedDate = try {
        val inputFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        val outputFormat = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
        val parsedDate = inputFormat.parse(encounter.createdDate)
        if (parsedDate != null) outputFormat.format(parsedDate) else encounter.createdDate
    } catch (e: Exception) {
        encounter.createdDate
    }

    // Aktif mi?
    val isActive = encounter.status == RematchStatus.ACTIVE ||
            encounter.status == RematchStatus.ROUND2_ACTIVE ||
            encounter.status == RematchStatus.ROUND1_COMPLETE

    // Karşılaştırma mümkün mü? (en az 1 parti tamamlanmış)
    val canCompare = encounter.currentRound >= 2 || encounter.status == RematchStatus.COMPLETED

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable { onCardClick() },
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            // Üst satır: Oyuncu isimleri + Durum
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${encounter.player1Name} vs ${encounter.player2Name}",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(statusColor, RoundedCornerShape(4.dp))
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = statusText,
                        fontSize = 11.sp,
                        color = statusColor,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            // İkinci satır: Tarih + Tur/Parti bilgisi
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = formattedDate,
                    fontSize = 11.sp,
                    color = Color.Gray
                )
                Text(
                    text = "Tur ${encounter.currentRound} | Parti ${encounter.currentPartyIndex + 1}/${encounter.totalParties}",
                    fontSize = 11.sp,
                    color = Color.Gray
                )
            }

            // İlerleme çubuğu
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                LinearProgressIndicator(
                    progress = { progress.coerceIn(0f, 1f) },
                    modifier = Modifier
                        .weight(1f)
                        .height(6.dp),
                    color = statusColor,
                    trackColor = Color.LightGray.copy(alpha = 0.3f),
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "%$progressPercent",
                    fontSize = 11.sp,
                    color = Color.Gray
                )
            }

            // Skor satırı
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Parti", fontSize = 10.sp, color = Color.Gray)
                    Text(
                        "$p1Parties-$p2Parties",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Oyun", fontSize = 10.sp, color = Color.Gray)
                    Text(
                        "$p1Games-$p2Games",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Puan", fontSize = 10.sp, color = Color.Gray)
                    Text(
                        "$p1Points-$p2Points",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Butonlar
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Sil butonu (sol taraf)
                OutlinedButton(
                    onClick = onDelete,
                    modifier = Modifier.height(32.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFF44336)),
                    border = BorderStroke(1.dp, Color(0xFFF44336).copy(alpha = 0.5f))
                ) {
                    Text("Sil", fontSize = 12.sp)
                }

                // Sağ taraf butonları
                Row {
                    if (isActive) {
                        OutlinedButton(
                            onClick = onContinue,
                            modifier = Modifier.height(32.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                        ) {
                            Text("Devam Et", fontSize = 12.sp)
                        }
                    }
                    if (canCompare) {
                        if (isActive) Spacer(modifier = Modifier.width(8.dp))
                        OutlinedButton(
                            onClick = onCompare,
                            modifier = Modifier.height(32.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                        ) {
                            Text("Karşılaştır", fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }
}

        /*
@Composable
fun MatchListItemWithDelete(
    match: Match,
    players: Map<Long, Player>,
    isDeleteMode: Boolean,
    isSelected: Boolean,
    onItemClick: (Long) -> Unit,
    onDeleteClick: (Long) -> Unit
) {
    val player1Name = players[match.player1Id]?.name ?: "Bilinmeyen"
    val player2Name = players[match.player2Id]?.name ?: "Bilinmeyen"
    val winnerName = players[match.winnerId]?.name ?: "Bilinmeyen"

    // Tarih formatını düzenle
    val date = try {
        val inputFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        val outputFormat = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())
        val parsedDate = inputFormat.parse(match.date)
        outputFormat.format(parsedDate)
    } catch (e: Exception) {
        match.date
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable { onItemClick(match.id) }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
                .background(if (isSelected && isDeleteMode) Color.LightGray.copy(alpha = 0.3f) else Color.Transparent),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Silme modu aktifse checkbox göster
            if (isDeleteMode) {
                Checkbox(
                    checked = isSelected,
                    onCheckedChange = { onItemClick(match.id) },
                    modifier = Modifier.padding(end = 8.dp)
                )
            }

            // Maç bilgileri
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(8.dp)
            ) {
                // Maç başlığı ve tarih
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Maç #${match.id}",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = date,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Oyuncu isimleri ve skorları
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(
                        horizontalAlignment = Alignment.Start
                    ) {
                        Text(
                            text = player1Name,
                            fontWeight = if (match.winnerId == match.player1Id) FontWeight.Bold else FontWeight.Normal
                        )
                        Text(text = "${match.player1Score} puan")
                        Text(text = "${match.player1RoundsWon} el")
                    }

                    Text(
                        text = "vs",
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )

                    Column(
                        horizontalAlignment = Alignment.End
                    ) {
                        Text(
                            text = player2Name,
                            fontWeight = if (match.winnerId == match.player2Id) FontWeight.Bold else FontWeight.Normal
                        )
                        Text(text = "${match.player2Score} puan")
                        Text(text = "${match.player2RoundsWon} el")
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Oyun tipi ve kazanan bilgisi
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = "${match.gameType} Tavla")
                    Text(
                        text = "Kazanan: $winnerName",
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Normal modda ise silme butonu göster
            if (!isDeleteMode) {
                IconButton(
                    onClick = { onDeleteClick(match.id) }
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Sil",
                        tint = Color.Red
                    )
                }
            }
        }
    }
}

 */
        /*
@Composable
fun PlayerListItem(player: Player, onItemClick: (Long) -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable { onItemClick(player.id) }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = player.name,
                style = MaterialTheme.typography.titleMedium
            )

            Icon(
                imageVector = Icons.Default.KeyboardArrowRight,
                contentDescription = "Görüntüle",
                modifier = Modifier.padding(start = 8.dp)
            )
        }
    }
}

 */
