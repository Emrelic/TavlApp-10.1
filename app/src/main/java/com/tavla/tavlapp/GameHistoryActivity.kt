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

// Tema renkleri
private val BgDark = Color(0xFF16213E)
private val BgCard = Color(0xFF1A2744)
private val GoldLight = Color(0xFFE8D5B7)
private val GoldDark = Color(0xFFBFA47A)
private val BorderBrown = Color(0xFF5D4037)

class GameHistoryActivity : ComponentActivity() {
    private lateinit var dbHelper: DatabaseHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        dbHelper = DatabaseHelper(this)

        setContent {
            TavlaAppTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = BgDark
                ) {
                    GameHistoryScreen(dbHelper) {
                        finish()
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

    var viewMode by remember { mutableStateOf("Tum Maclar") }
    val viewModeOptions = listOf("Tum Maclar", "Oyuncu Istatistikleri", "Ikili Karsilasmalar", "Rovansli Karsilasmalar")

    var isDeleteMode by remember { mutableStateOf(false) }
    val selectedMatches = remember { mutableStateListOf<Long>() }

    var refreshTrigger by remember { mutableStateOf(0) }
    val matches by remember(refreshTrigger) {
        mutableStateOf(dbHelper.getAllMatches())
    }
    val players = remember(refreshTrigger) {
        dbHelper.getAllPlayers().associateBy { it.id }
    }

    var showDeleteAllDialog by remember { mutableStateOf(false) }
    var showDeleteSelectedDialog by remember { mutableStateOf(false) }
    var matchToDelete by remember { mutableStateOf<Long?>(null) }
    var showPasswordDialog by remember { mutableStateOf(false) }
    var password by remember { mutableStateOf("") }

    Column(modifier = Modifier.fillMaxSize()) {
        // Ust bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(BgCard)
                .padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(
                onClick = onBack,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF37474F)),
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
            ) {
                Text("Geri", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }

            Text(
                text = "Oyun Gecmisi",
                fontSize = 20.sp,
                fontWeight = FontWeight.ExtraBold,
                color = GoldLight,
                letterSpacing = 1.sp
            )

            // Silme modu butonlari
            if (viewMode == "Tum Maclar" && matches.isNotEmpty()) {
                if (isDeleteMode) {
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        IconButton(
                            onClick = {
                                if (selectedMatches.isEmpty()) {
                                    isDeleteMode = false
                                } else {
                                    showDeleteSelectedDialog = true
                                }
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Secilenleri Sil",
                                tint = if (selectedMatches.isNotEmpty()) Color(0xFFF44336) else GoldDark.copy(alpha = 0.4f)
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
                                contentDescription = "Iptal",
                                tint = GoldDark
                            )
                        }
                    }
                } else {
                    IconButton(onClick = { isDeleteMode = true }) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "Diger Islemler",
                            tint = GoldDark
                        )
                    }
                }
            } else {
                Spacer(modifier = Modifier.width(48.dp))
            }
        }

        HorizontalDivider(thickness = 1.dp, color = BorderBrown)

        // Gorunum modu secimi
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(BgDark)
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
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
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            textAlign = TextAlign.Center,
                            fontSize = 10.sp,
                            lineHeight = 12.sp,
                            color = if (viewMode == option) Color.White else GoldDark
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        containerColor = BgCard,
                        selectedContainerColor = Color(0xFF6A1B9A)
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        borderColor = BorderBrown,
                        selectedBorderColor = Color(0xFF6A1B9A),
                        enabled = true,
                        selected = viewMode == option
                    ),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 3.dp)
                        .height(44.dp)
                )
            }
        }

        // Silme modu bilgi satiri
        if (isDeleteMode && viewMode == "Tum Maclar" && matches.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(BgCard)
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("${selectedMatches.size} mac secildi", color = GoldLight, fontSize = 13.sp)

                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    TextButton(
                        onClick = {
                            if (selectedMatches.size == matches.size) {
                                selectedMatches.clear()
                            } else {
                                selectedMatches.clear()
                                selectedMatches.addAll(matches.map { it.id })
                            }
                        }
                    ) {
                        Text(
                            if (selectedMatches.size == matches.size) "Secimi Temizle" else "Tumunu Sec",
                            color = GoldDark,
                            fontSize = 12.sp
                        )
                    }

                    TextButton(onClick = { showDeleteAllDialog = true }) {
                        Text("Tumunu Sil", color = Color(0xFFF44336), fontSize = 12.sp)
                    }
                }
            }
            HorizontalDivider(thickness = 1.dp, color = BorderBrown.copy(alpha = 0.5f))
        }

        // Icerik
        when (viewMode) {
            "Tum Maclar" -> {
                if (matches.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize().padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Henuz kaydedilmis oyun bulunmamaktadir",
                            color = GoldDark.copy(alpha = 0.6f),
                            fontSize = 14.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 12.dp, vertical = 4.dp)
                    ) {
                        items(matches) { match ->
                            MatchListItemWithDelete(
                                match = match,
                                players = players,
                                isDeleteMode = isDeleteMode,
                                isSelected = selectedMatches.contains(match.id),
                                onItemClick = { matchId ->
                                    if (isDeleteMode) {
                                        if (selectedMatches.contains(matchId)) {
                                            selectedMatches.remove(matchId)
                                        } else {
                                            selectedMatches.add(matchId)
                                        }
                                    } else {
                                        val intent = Intent(context, MatchDetailActivity::class.java)
                                        intent.putExtra("match_id", matchId)
                                        context.startActivity(intent)
                                    }
                                },
                                onDeleteClick = { matchToDelete = it }
                            )
                        }
                    }
                }
            }

            "Oyuncu Istatistikleri" -> {
                if (players.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize().padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Henuz kaydedilmis oyuncu bulunmamaktadir",
                            color = GoldDark.copy(alpha = 0.6f),
                            fontSize = 14.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 12.dp, vertical = 4.dp)
                    ) {
                        items(players.values.toList()) { player ->
                            PlayerListItem(player) { playerId ->
                                val intent = Intent(context, PlayerStatsActivity::class.java)
                                intent.putExtra("player_id", playerId)
                                context.startActivity(intent)
                            }
                        }
                    }
                }
            }

            "Ikili Karsilasmalar" -> {
                if (players.size < 2) {
                    Box(
                        modifier = Modifier.fillMaxSize().padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Ikili karsilasma icin en az 2 oyuncu gereklidir",
                            color = GoldDark.copy(alpha = 0.6f),
                            fontSize = 14.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                } else {
                    Box(
                        modifier = Modifier.fillMaxSize().padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Button(
                            onClick = {
                                val intent = Intent(context, PlayerVsPlayerActivity::class.java)
                                context.startActivity(intent)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1565C0)),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Ikili Karsilasma Istatistiklerini Goruntule", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            "Rovansli Karsilasmalar" -> {
                var showRematchDeleteDialog by remember { mutableStateOf(false) }
                var pendingDeleteEncounterId by remember { mutableStateOf(-1L) }
                var pendingDeleteEncounterName by remember { mutableStateOf("") }

                var rematchEncounters by remember(refreshTrigger) {
                    mutableStateOf(dbHelper.getAllRematchEncounters())
                }
                var allEncounterStats by remember(refreshTrigger) {
                    mutableStateOf(mapOf<Long, List<RematchEncounterStats>>())
                }

                LaunchedEffect(rematchEncounters) {
                    val statsMap = mutableMapOf<Long, List<RematchEncounterStats>>()
                    rematchEncounters.forEach { enc ->
                        statsMap[enc.id] = dbHelper.getRematchEncounterStats(enc.id)
                    }
                    allEncounterStats = statsMap
                }

                if (rematchEncounters.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize().padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "Henuz rovansli karsilasma bulunmamaktadir",
                                color = GoldDark.copy(alpha = 0.6f),
                                fontSize = 14.sp,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(
                                onClick = {
                                    context.startActivity(Intent(context, RematchProgressActivity::class.java))
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6A1B9A)),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("Yeni Karsilasma Baslat", color = Color.White, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                } else {
                    Column(modifier = Modifier.fillMaxSize()) {
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
                                text = "${rematchEncounters.size} karsilasma (${activeCount} aktif, ${completedCount} tamamlanmis)",
                                fontSize = 11.sp,
                                color = GoldDark
                            )
                            Button(
                                onClick = {
                                    context.startActivity(Intent(context, RematchProgressActivity::class.java))
                                },
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                modifier = Modifier.height(32.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50)),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("Yeni", fontSize = 12.sp, color = Color.White, fontWeight = FontWeight.Bold)
                            }
                        }

                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 12.dp)
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
                                        context.startActivity(Intent(context, RematchProgressActivity::class.java))
                                    },
                                    onDelete = {
                                        pendingDeleteEncounterId = encounter.id
                                        pendingDeleteEncounterName = "${encounter.player1Name} vs ${encounter.player2Name}"
                                        showRematchDeleteDialog = true
                                    }
                                )
                            }
                        }

                        // Rovans silme dialog
                        if (showRematchDeleteDialog) {
                            AlertDialog(
                                onDismissRequest = { showRematchDeleteDialog = false },
                                containerColor = BgCard,
                                shape = RoundedCornerShape(16.dp),
                                title = { Text("Karsilasmyi Sil", color = GoldLight, fontWeight = FontWeight.Bold) },
                                text = {
                                    Column {
                                        Text("Bu karsilasmyi arsivden cikarmak istediginizden emin misiniz?", color = Color.White.copy(alpha = 0.8f))
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(pendingDeleteEncounterName, fontWeight = FontWeight.Bold, color = GoldLight)
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text("Tum parti sonuclari, zar setleri ve istatistikler silinecektir.", fontSize = 12.sp, color = GoldDark)
                                    }
                                },
                                confirmButton = {
                                    Button(
                                        onClick = {
                                            dbHelper.deleteRematchEncounter(pendingDeleteEncounterId)
                                            showRematchDeleteDialog = false
                                            refreshTrigger++
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFB71C1C)),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text("Evet, Sil", color = Color.White, fontWeight = FontWeight.Bold)
                                    }
                                },
                                dismissButton = {
                                    Button(
                                        onClick = { showRematchDeleteDialog = false },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF37474F)),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text("Vazgec", color = Color.White)
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    // Tek mac silme dialog
    if (matchToDelete != null) {
        AlertDialog(
            onDismissRequest = { matchToDelete = null },
            containerColor = BgCard,
            shape = RoundedCornerShape(16.dp),
            title = { Text("Maci Sil", color = GoldLight, fontWeight = FontWeight.Bold) },
            text = { Text("Bu maci silmek istediginize emin misiniz? Bu islem geri alinamaz.", color = Color.White.copy(alpha = 0.8f)) },
            confirmButton = {
                Button(
                    onClick = {
                        val matchId = matchToDelete!!
                        scope.launch {
                            val result = dbHelper.deleteMatch(matchId)
                            if (result > 0) {
                                refreshTrigger++
                                Toast.makeText(context, "Mac silindi", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(context, "Mac silinemedi", Toast.LENGTH_SHORT).show()
                            }
                            matchToDelete = null
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFB71C1C)),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Evet, Sil", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                Button(
                    onClick = { matchToDelete = null },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF37474F)),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Iptal", color = Color.White)
                }
            }
        )
    }

    // Secili maclari silme dialog
    if (showDeleteSelectedDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteSelectedDialog = false },
            containerColor = BgCard,
            shape = RoundedCornerShape(16.dp),
            title = { Text("Secili Maclari Sil", color = GoldLight, fontWeight = FontWeight.Bold) },
            text = { Text("${selectedMatches.size} maci silmek istediginize emin misiniz? Bu islem geri alinamaz.", color = Color.White.copy(alpha = 0.8f)) },
            confirmButton = {
                Button(
                    onClick = {
                        scope.launch {
                            val result = dbHelper.deleteMatches(selectedMatches.toList())
                            if (result > 0) {
                                refreshTrigger++
                                Toast.makeText(context, "$result mac silindi", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(context, "Maclar silinemedi", Toast.LENGTH_SHORT).show()
                            }
                            showDeleteSelectedDialog = false
                            isDeleteMode = false
                            selectedMatches.clear()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFB71C1C)),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Evet, Sil", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                Button(
                    onClick = { showDeleteSelectedDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF37474F)),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Iptal", color = Color.White)
                }
            }
        )
    }

    // Tum maclari silme dialog
    if (showDeleteAllDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteAllDialog = false },
            containerColor = BgCard,
            shape = RoundedCornerShape(16.dp),
            title = { Text("Tum Verileri Sifirla", color = GoldLight, fontWeight = FontWeight.Bold) },
            text = {
                Text("Tum mac gecmisini ve oyuncu istatistiklerini sifirlamak istediginize emin misiniz? Bu islem geri alinamaz!", color = Color.White.copy(alpha = 0.8f))
            },
            confirmButton = {
                Button(
                    onClick = {
                        scope.launch {
                            val result = dbHelper.resetAllData()
                            if (result > 0) {
                                refreshTrigger++
                                Toast.makeText(context, "Tum veriler sifirlandi ($result mac silindi)", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(context, "Silinecek veri bulunamadi", Toast.LENGTH_SHORT).show()
                            }
                            showDeleteAllDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFB71C1C)),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Evet, Tumunu Sifirla", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                Button(
                    onClick = { showDeleteAllDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF37474F)),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Iptal", color = Color.White)
                }
            }
        )
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
    val (statusColor, statusText) = when (encounter.status) {
        RematchStatus.ACTIVE -> Color(0xFF4CAF50) to "Aktif"
        RematchStatus.ROUND1_COMPLETE -> Color(0xFFFF9800) to "Rovans Bekliyor"
        RematchStatus.ROUND2_ACTIVE -> Color(0xFFFF9800) to "Rovans"
        RematchStatus.COMPLETED -> Color(0xFF2196F3) to "Tamamlandi"
        RematchStatus.CANCELLED -> Color(0xFFF44336) to "Iptal"
    }

    val player1Stats = stats.find { it.playerId == encounter.player1Id }
    val player2Stats = stats.find { it.playerId == encounter.player2Id }

    val p1Parties = player1Stats?.totalPartiesWon ?: 0
    val p2Parties = player2Stats?.totalPartiesWon ?: 0
    val p1Games = player1Stats?.totalGamesWon ?: 0
    val p2Games = player2Stats?.totalGamesWon ?: 0
    val p1Points = player1Stats?.totalPoints ?: 0
    val p2Points = player2Stats?.totalPoints ?: 0

    val totalPartiesPlayed = p1Parties + p2Parties
    val totalPartiesTarget = encounter.totalParties * 2
    val progress = if (totalPartiesTarget > 0) {
        totalPartiesPlayed.toFloat() / totalPartiesTarget.toFloat()
    } else 0f
    val progressPercent = (progress * 100).toInt()

    val formattedDate = try {
        val inputFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        val outputFormat = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
        val parsedDate = inputFormat.parse(encounter.createdDate)
        if (parsedDate != null) outputFormat.format(parsedDate) else encounter.createdDate
    } catch (e: Exception) {
        encounter.createdDate
    }

    val isActive = encounter.status == RematchStatus.ACTIVE ||
            encounter.status == RematchStatus.ROUND2_ACTIVE ||
            encounter.status == RematchStatus.ROUND1_COMPLETE
    val canCompare = encounter.currentRound >= 2 || encounter.status == RematchStatus.COMPLETED

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable { onCardClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = BgCard),
        border = BorderStroke(1.dp, BorderBrown)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            // Ust satir: Oyuncu isimleri + Durum
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${encounter.player1Name} vs ${encounter.player2Name}",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = GoldLight,
                    modifier = Modifier.weight(1f)
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
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

            // Tarih + Tur/Parti
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(text = formattedDate, fontSize = 11.sp, color = GoldDark)
                Text(
                    text = "Tur ${encounter.currentRound} | Parti ${encounter.currentPartyIndex + 1}/${encounter.totalParties}",
                    fontSize = 11.sp,
                    color = GoldDark
                )
            }

            // Ilerleme cubugu
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
                    trackColor = BorderBrown.copy(alpha = 0.3f),
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = "%$progressPercent", fontSize = 11.sp, color = GoldDark)
            }

            // Skor satiri
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Parti", fontSize = 10.sp, color = GoldDark)
                    Text("$p1Parties-$p2Parties", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Oyun", fontSize = 10.sp, color = GoldDark)
                    Text("$p1Games-$p2Games", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Puan", fontSize = 10.sp, color = GoldDark)
                    Text("$p1Points-$p2Points", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
            }

            // Butonlar
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Sil butonu
                Button(
                    onClick = onDelete,
                    modifier = Modifier.height(32.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFB71C1C)),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Sil", fontSize = 12.sp, color = Color.White)
                }

                // Sag taraf butonlari
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (isActive) {
                        Button(
                            onClick = onContinue,
                            modifier = Modifier.height(32.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50)),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Devam Et", fontSize = 12.sp, color = Color.White)
                        }
                    }
                    if (canCompare) {
                        Button(
                            onClick = onCompare,
                            modifier = Modifier.height(32.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1565C0)),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Karsilastir", fontSize = 12.sp, color = Color.White)
                        }
                    }
                }
            }
        }
    }
}
