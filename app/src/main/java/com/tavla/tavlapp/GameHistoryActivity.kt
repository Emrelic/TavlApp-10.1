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
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Button(
                            onClick = {
                                val intent = Intent(context, RematchProgressActivity::class.java)
                                context.startActivity(intent)
                            }
                        ) {
                            Text("Rövanşlı Karşılaşmaları Görüntüle")
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
