package com.tavla.tavlapp

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.BorderStroke
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.content.Intent
import java.text.SimpleDateFormat
import java.util.Locale

// Tema renkleri
private val BgDark = Color(0xFF16213E)
private val BgCard = Color(0xFF1A2744)
private val BgCardSelected = Color(0xFF6A1B9A)
private val GoldLight = Color(0xFFE8D5B7)
private val GoldDark = Color(0xFFBFA47A)
private val BorderBrown = Color(0xFF5D4037)
private val StatusGreen = Color(0xFF4CAF50)
private val StatusBlue = Color(0xFF2196F3)
private val StatusOrange = Color(0xFFFF9800)
private val StatusRed = Color(0xFFF44336)

class RematchProgressActivity : ComponentActivity() {
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
                    RematchProgressScreen(dbHelper)
                }
            }
        }
    }
}

@Composable
fun RematchProgressScreen(dbHelper: DatabaseHelper) {
    val context = LocalContext.current

    var encounters by remember { mutableStateOf(listOf<RematchEncounter>()) }
    var selectedEncounter by remember { mutableStateOf<RematchEncounter?>(null) }
    var encounterStats by remember { mutableStateOf(listOf<RematchEncounterStats>()) }

    var showCancelDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var encounterToDelete by remember { mutableStateOf<RematchEncounter?>(null) }
    var showDeleteAllDialog by remember { mutableStateOf(false) }

    var allEncounterStats by remember { mutableStateOf(mapOf<Long, List<RematchEncounterStats>>()) }

    // Verileri yeniden yukleme fonksiyonu
    fun reloadData() {
        val loaded = dbHelper.getAllRematchEncounters()
        encounters = loaded
        val statsMap = mutableMapOf<Long, List<RematchEncounterStats>>()
        loaded.forEach { enc ->
            statsMap[enc.id] = dbHelper.getRematchEncounterStats(enc.id)
        }
        allEncounterStats = statsMap
    }

    LaunchedEffect(Unit) { reloadData() }

    LaunchedEffect(selectedEncounter) {
        selectedEncounter?.let { enc ->
            encounterStats = dbHelper.getRematchEncounterStats(enc.id)
        }
    }

    // --- Dialoglar ---

    // Iptal dialog
    if (showCancelDialog && selectedEncounter != null) {
        ThemedAlertDialog(
            title = "Karsilasmyi Iptal Et",
            text = "Bu karsilasmyi iptal etmek istediginizden emin misiniz?",
            confirmText = "Iptal Et",
            confirmColor = StatusRed,
            onConfirm = {
                dbHelper.cancelEncounter(selectedEncounter!!.id)
                reloadData()
                selectedEncounter = null
                showCancelDialog = false
                Toast.makeText(context, "Karsilasma iptal edildi", Toast.LENGTH_SHORT).show()
            },
            onDismiss = { showCancelDialog = false }
        )
    }

    // Silme dialog
    if (showDeleteDialog && selectedEncounter != null) {
        ThemedAlertDialog(
            title = "Karsilasmyi Sil",
            text = "Bu karsilasma ve tum verileri kalici olarak silinecek. Devam etmek istiyor musunuz?",
            confirmText = "Sil",
            confirmColor = StatusRed,
            onConfirm = {
                dbHelper.deleteRematchEncounter(selectedEncounter!!.id)
                reloadData()
                selectedEncounter = null
                showDeleteDialog = false
                Toast.makeText(context, "Karsilasma silindi", Toast.LENGTH_SHORT).show()
            },
            onDismiss = { showDeleteDialog = false }
        )
    }

    // Kart uzerinden silme dialog
    if (encounterToDelete != null) {
        ThemedAlertDialog(
            title = "Karsilasmyi Sil",
            text = "${encounterToDelete!!.player1Name} vs ${encounterToDelete!!.player2Name} karsilasmasi ve tum verileri kalici olarak silinecek.",
            confirmText = "Sil",
            confirmColor = StatusRed,
            onConfirm = {
                val encToDelete = encounterToDelete!!
                dbHelper.deleteRematchEncounter(encToDelete.id)
                reloadData()
                if (selectedEncounter?.id == encToDelete.id) {
                    selectedEncounter = null
                }
                encounterToDelete = null
                Toast.makeText(context, "Karsilasma silindi", Toast.LENGTH_SHORT).show()
            },
            onDismiss = { encounterToDelete = null }
        )
    }

    // Tumunu sil dialog
    if (showDeleteAllDialog) {
        ThemedAlertDialog(
            title = "Tum Karsilasmalari Sil",
            text = "${encounters.size} karsilasma ve tum verileri kalici olarak silinecek.",
            confirmText = "Tumunu Sil",
            confirmColor = StatusRed,
            onConfirm = {
                encounters.forEach { enc ->
                    dbHelper.deleteRematchEncounter(enc.id)
                }
                encounters = emptyList()
                allEncounterStats = emptyMap()
                selectedEncounter = null
                showDeleteAllDialog = false
                Toast.makeText(context, "Tum karsilasmalar silindi", Toast.LENGTH_SHORT).show()
            },
            onDismiss = { showDeleteAllDialog = false }
        )
    }

    // --- Ana Layout ---
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
                onClick = { (context as ComponentActivity).finish() },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF37474F)),
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
            ) {
                Text("Geri", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }

            Text(
                text = "Karsilasmalar",
                fontSize = 20.sp,
                fontWeight = FontWeight.ExtraBold,
                color = GoldLight,
                letterSpacing = 1.sp
            )

            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                if (encounters.isNotEmpty()) {
                    Button(
                        onClick = { showDeleteAllDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFB71C1C)),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text("Tumunu Sil", fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
                Button(
                    onClick = {
                        context.startActivity(Intent(context, RematchSetupActivity::class.java))
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = StatusGreen),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text("+ Yeni", fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }

        HorizontalDivider(thickness = 1.dp, color = BorderBrown)

        // Icerik
        Row(modifier = Modifier.fillMaxSize()) {
            // Sol panel - Karsilasma listesi
            Column(
                modifier = Modifier
                    .weight(0.4f)
                    .fillMaxHeight()
                    .background(BgDark.copy(alpha = 0.95f))
                    .padding(12.dp)
            ) {
                if (encounters.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "Henuz karsilasma yok",
                                color = GoldDark.copy(alpha = 0.6f),
                                fontSize = 14.sp
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(
                                onClick = {
                                    context.startActivity(Intent(context, RematchSetupActivity::class.java))
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = BgCardSelected),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("Yeni Karsilasma Baslat", color = Color.White, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(encounters) { encounter ->
                            EncounterListItem(
                                encounter = encounter,
                                isSelected = selectedEncounter?.id == encounter.id,
                                stats = allEncounterStats[encounter.id] ?: emptyList(),
                                onClick = { selectedEncounter = encounter },
                                onDelete = { encounterToDelete = encounter }
                            )
                        }
                    }
                }
            }

            // Dikey ayirici
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(1.dp)
                    .background(BorderBrown)
            )

            // Sag panel - Detaylar
            Column(
                modifier = Modifier
                    .weight(0.6f)
                    .fillMaxHeight()
                    .padding(16.dp)
            ) {
                if (selectedEncounter == null) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "Detaylari gormek icin",
                                color = GoldDark.copy(alpha = 0.5f),
                                fontSize = 14.sp
                            )
                            Text(
                                text = "bir karsilasma secin",
                                color = GoldDark.copy(alpha = 0.5f),
                                fontSize = 14.sp
                            )
                        }
                    }
                } else {
                    EncounterDetailPanel(
                        encounter = selectedEncounter!!,
                        stats = encounterStats,
                        onContinue = {
                            val enc = selectedEncounter!!
                            val intent = Intent(context, GameScoreActivity::class.java)
                            intent.putExtra("is_rematch_mode", true)
                            intent.putExtra("encounter_id", enc.id)
                            intent.putExtra("player1_id", enc.player1Id)
                            intent.putExtra("player2_id", enc.player2Id)
                            intent.putExtra("player1_name", enc.player1Name)
                            intent.putExtra("player2_name", enc.player2Name)
                            intent.putExtra("total_parties", enc.totalParties)
                            intent.putExtra("rounds", enc.targetScore)
                            context.startActivity(intent)
                        },
                        onCancel = { showCancelDialog = true },
                        onDelete = { showDeleteDialog = true },
                        onCompareParty = { partyIdx ->
                            val intent = Intent(context, RematchComparisonActivity::class.java)
                            intent.putExtra("encounter_id", selectedEncounter!!.id)
                            intent.putExtra("party_index", partyIdx)
                            context.startActivity(intent)
                        },
                        dbHelper = dbHelper
                    )
                }
            }
        }
    }
}

@Composable
fun ThemedAlertDialog(
    title: String,
    text: String,
    confirmText: String,
    confirmColor: Color,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = BgCard,
        shape = RoundedCornerShape(16.dp),
        title = { Text(title, color = GoldLight, fontWeight = FontWeight.Bold) },
        text = { Text(text, color = Color.White.copy(alpha = 0.8f)) },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = confirmColor),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(confirmText, color = Color.White, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF37474F)),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("Vazgec", color = Color.White)
            }
        }
    )
}

@Composable
fun EncounterListItem(
    encounter: RematchEncounter,
    isSelected: Boolean,
    stats: List<RematchEncounterStats>,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    val textColor = if (isSelected) Color.White else GoldLight
    val subtextColor = if (isSelected) Color.White.copy(alpha = 0.7f) else GoldDark.copy(alpha = 0.7f)

    val formattedDate = try {
        val inputFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        val outputFormat = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
        val date = inputFormat.parse(encounter.createdDate)
        if (date != null) outputFormat.format(date) else encounter.createdDate
    } catch (e: Exception) {
        encounter.createdDate
    }

    val p1Stats = stats.find { it.playerId == encounter.player1Id }
    val p2Stats = stats.find { it.playerId == encounter.player2Id }

    val statusColor = when (encounter.status) {
        RematchStatus.ACTIVE -> StatusGreen
        RematchStatus.COMPLETED -> StatusBlue
        RematchStatus.CANCELLED -> StatusRed
        else -> StatusOrange
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) BgCardSelected else BgCard
        ),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, if (isSelected) BgCardSelected else BorderBrown)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // Baslik + sil butonu
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${encounter.player1Name} vs ${encounter.player2Name}",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = textColor,
                    modifier = Modifier.weight(1f)
                )
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(22.dp)
                ) {
                    Text(
                        text = "\u2715",
                        fontSize = 12.sp,
                        color = if (isSelected) Color.White.copy(alpha = 0.5f) else GoldDark.copy(alpha = 0.4f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Tarih + durum
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(text = formattedDate, fontSize = 10.sp, color = subtextColor)
                Text(
                    text = when (encounter.status) {
                        RematchStatus.ACTIVE -> "Aktif"
                        RematchStatus.ROUND1_COMPLETE -> "Rovans Bekliyor"
                        RematchStatus.ROUND2_ACTIVE -> "Rovans"
                        RematchStatus.COMPLETED -> "Tamamlandi"
                        RematchStatus.CANCELLED -> "Iptal"
                    },
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = statusColor
                )
            }

            // Tur ve parti
            Text(
                text = "Tur ${encounter.currentRound}/2 | Parti ${encounter.currentPartyIndex + 1}/${encounter.totalParties}",
                fontSize = 10.sp,
                color = subtextColor
            )

            // Istatistikler
            if (p1Stats != null && p2Stats != null) {
                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 6.dp),
                    thickness = 1.dp,
                    color = if (isSelected) Color.White.copy(alpha = 0.2f) else BorderBrown.copy(alpha = 0.5f)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Parti: ${p1Stats.totalPartiesWon} - ${p2Stats.totalPartiesWon}",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = textColor
                    )
                    Text(
                        text = "Oyun: ${p1Stats.totalGamesWon} - ${p2Stats.totalGamesWon}",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = textColor
                    )
                }
                Text(
                    text = "Puan: ${p1Stats.totalPoints} - ${p2Stats.totalPoints}",
                    fontSize = 10.sp,
                    color = subtextColor
                )
            }
        }
    }
}

@Composable
fun EncounterDetailPanel(
    encounter: RematchEncounter,
    stats: List<RematchEncounterStats>,
    onContinue: () -> Unit,
    onCancel: () -> Unit,
    onDelete: () -> Unit,
    onCompareParty: (Int) -> Unit = {},
    dbHelper: DatabaseHelper? = null
) {
    var completedRound2Parties by remember { mutableStateOf(listOf<Int>()) }
    LaunchedEffect(encounter.id) {
        if (dbHelper != null) {
            val r2Results = dbHelper.getRematchPartyResults(encounter.id, roundNumber = 2)
            completedRound2Parties = r2Results.map { it.partyIndex }
        }
    }

    val statusColor = when (encounter.status) {
        RematchStatus.ACTIVE -> StatusGreen
        RematchStatus.COMPLETED -> StatusBlue
        RematchStatus.CANCELLED -> StatusRed
        else -> StatusOrange
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Baslik
        Text(
            text = "${encounter.player1Name} vs ${encounter.player2Name}",
            fontWeight = FontWeight.ExtraBold,
            fontSize = 22.sp,
            color = GoldLight,
            letterSpacing = 1.sp
        )

        // Durum karti
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = BgCard),
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, BorderBrown)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Tur", color = GoldDark, fontSize = 11.sp)
                    Text(
                        "${encounter.currentRound}/2",
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        color = Color.White
                    )
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Parti", color = GoldDark, fontSize = 11.sp)
                    Text(
                        "${encounter.currentPartyIndex + 1}/${encounter.totalParties}",
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        color = Color.White
                    )
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Durum", color = GoldDark, fontSize = 11.sp)
                    Text(
                        when (encounter.status) {
                            RematchStatus.ACTIVE -> "Aktif"
                            RematchStatus.ROUND1_COMPLETE -> "Rovans Bekliyor"
                            RematchStatus.ROUND2_ACTIVE -> "Rovans Aktif"
                            RematchStatus.COMPLETED -> "Tamamlandi"
                            RematchStatus.CANCELLED -> "Iptal"
                        },
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = statusColor
                    )
                }
            }
        }

        // Istatistikler
        if (stats.isNotEmpty()) {
            Text(
                text = "Istatistikler",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = GoldLight
            )

            stats.forEach { playerStat ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = BgCard),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, BorderBrown)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = playerStat.playerName,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = GoldLight
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text("Tur 1", color = GoldDark, fontSize = 11.sp)
                                Text("${playerStat.round1PartiesWon} parti", color = Color.White, fontSize = 12.sp)
                                Text("${playerStat.round1GamesWon} oyun", color = Color.White, fontSize = 12.sp)
                                Text("${playerStat.round1Points} puan", color = Color.White, fontSize = 12.sp)
                            }
                            Column {
                                Text("Tur 2", color = GoldDark, fontSize = 11.sp)
                                Text("${playerStat.round2PartiesWon} parti", color = Color.White, fontSize = 12.sp)
                                Text("${playerStat.round2GamesWon} oyun", color = Color.White, fontSize = 12.sp)
                                Text("${playerStat.round2Points} puan", color = Color.White, fontSize = 12.sp)
                            }
                            Column {
                                Text("Toplam", color = GoldDark, fontSize = 11.sp)
                                Text(
                                    "${playerStat.totalPartiesWon} parti",
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    fontSize = 12.sp
                                )
                                Text(
                                    "${playerStat.totalGamesWon} oyun",
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    fontSize = 12.sp
                                )
                                Text(
                                    "${playerStat.totalPoints} puan",
                                    fontWeight = FontWeight.Bold,
                                    color = StatusOrange,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                }
            }
        }

        // Parti Karsilastirmalari
        if (completedRound2Parties.isNotEmpty()) {
            Text(
                text = "Parti Karsilastirmalari",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = GoldLight
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                completedRound2Parties.take(10).forEach { partyIdx ->
                    Button(
                        onClick = { onCompareParty(partyIdx) },
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = BgCard),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, BorderBrown)
                    ) {
                        Text("P${partyIdx + 1}", fontSize = 11.sp, color = GoldLight)
                    }
                }
                if (completedRound2Parties.size > 10) {
                    Text("...", color = GoldDark, modifier = Modifier.align(Alignment.CenterVertically))
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Butonlar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (encounter.status == RematchStatus.ACTIVE || encounter.status == RematchStatus.ROUND1_COMPLETE || encounter.status == RematchStatus.ROUND2_ACTIVE) {
                Button(
                    onClick = onContinue,
                    modifier = Modifier.weight(1f).height(46.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = StatusGreen),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Devam Et", color = Color.White, fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = onCancel,
                    modifier = Modifier.weight(1f).height(46.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF37474F)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Iptal Et", color = StatusRed, fontWeight = FontWeight.Bold)
                }
            }

            Button(
                onClick = onDelete,
                modifier = Modifier.weight(1f).height(46.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFB71C1C)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Sil", color = Color.White, fontWeight = FontWeight.Bold)
            }
        }
    }
}
