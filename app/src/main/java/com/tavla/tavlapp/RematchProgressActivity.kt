package com.tavla.tavlapp

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import android.content.Intent
import java.text.SimpleDateFormat
import java.util.Locale

/**
 * Rovansli karsilasma ilerleme ve istatistik ekrani
 */
class RematchProgressActivity : ComponentActivity() {
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
                    RematchProgressScreen(dbHelper)
                }
            }
        }
    }
}

@Composable
fun RematchProgressScreen(dbHelper: DatabaseHelper) {
    val context = LocalContext.current

    // Karsilasma listesi
    var encounters by remember { mutableStateOf(listOf<RematchEncounter>()) }
    var selectedEncounter by remember { mutableStateOf<RematchEncounter?>(null) }
    var encounterStats by remember { mutableStateOf(listOf<RematchEncounterStats>()) }

    // Dialog durumlari
    var showCancelDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var encounterToDelete by remember { mutableStateOf<RematchEncounter?>(null) }
    var showDeleteAllDialog by remember { mutableStateOf(false) }

    // Tum karsilasmalarin istatistikleri
    var allEncounterStats by remember { mutableStateOf(mapOf<Long, List<RematchEncounterStats>>()) }

    // Verileri yukle
    LaunchedEffect(Unit) {
        val loaded = dbHelper.getAllRematchEncounters()
        encounters = loaded
        val statsMap = mutableMapOf<Long, List<RematchEncounterStats>>()
        loaded.forEach { enc ->
            statsMap[enc.id] = dbHelper.getRematchEncounterStats(enc.id)
        }
        allEncounterStats = statsMap
    }

    // Secili karsilasma istatistiklerini yukle
    LaunchedEffect(selectedEncounter) {
        selectedEncounter?.let { enc ->
            encounterStats = dbHelper.getRematchEncounterStats(enc.id)
        }
    }

    // Iptal dialog
    if (showCancelDialog && selectedEncounter != null) {
        AlertDialog(
            onDismissRequest = { showCancelDialog = false },
            title = { Text("Karsilasmyi Iptal Et") },
            text = { Text("Bu karsilasmyi iptal etmek istediginizden emin misiniz?") },
            confirmButton = {
                Button(
                    onClick = {
                        dbHelper.cancelEncounter(selectedEncounter!!.id)
                        val reloaded = dbHelper.getAllRematchEncounters()
                        encounters = reloaded
                        val statsMap = mutableMapOf<Long, List<RematchEncounterStats>>()
                        reloaded.forEach { enc -> statsMap[enc.id] = dbHelper.getRematchEncounterStats(enc.id) }
                        allEncounterStats = statsMap
                        selectedEncounter = null
                        showCancelDialog = false
                        Toast.makeText(context, "Karsilasma iptal edildi", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                ) {
                    Text("Iptal Et")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showCancelDialog = false }) {
                    Text("Vazgec")
                }
            }
        )
    }

    // Silme dialog
    if (showDeleteDialog && selectedEncounter != null) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Karsilasmyi Sil") },
            text = { Text("Bu karsilasma ve tum verileri kalici olarak silinecek. Devam etmek istiyor musunuz?") },
            confirmButton = {
                Button(
                    onClick = {
                        dbHelper.deleteRematchEncounter(selectedEncounter!!.id)
                        val reloaded = dbHelper.getAllRematchEncounters()
                        encounters = reloaded
                        val statsMap = mutableMapOf<Long, List<RematchEncounterStats>>()
                        reloaded.forEach { enc -> statsMap[enc.id] = dbHelper.getRematchEncounterStats(enc.id) }
                        allEncounterStats = statsMap
                        selectedEncounter = null
                        showDeleteDialog = false
                        Toast.makeText(context, "Karsilasma silindi", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                ) {
                    Text("Sil")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showDeleteDialog = false }) {
                    Text("Vazgec")
                }
            }
        )
    }

    // Kart uzerinden silme dialog
    if (encounterToDelete != null) {
        AlertDialog(
            onDismissRequest = { encounterToDelete = null },
            title = { Text("Karsilasmyi Sil") },
            text = {
                Text("${encounterToDelete!!.player1Name} vs ${encounterToDelete!!.player2Name} karsilasmasi ve tum verileri kalici olarak silinecek. Devam etmek istiyor musunuz?")
            },
            confirmButton = {
                Button(
                    onClick = {
                        val encToDelete = encounterToDelete!!
                        dbHelper.deleteRematchEncounter(encToDelete.id)
                        val reloaded = dbHelper.getAllRematchEncounters()
                        encounters = reloaded
                        val statsMap = mutableMapOf<Long, List<RematchEncounterStats>>()
                        reloaded.forEach { enc -> statsMap[enc.id] = dbHelper.getRematchEncounterStats(enc.id) }
                        allEncounterStats = statsMap
                        if (selectedEncounter?.id == encToDelete.id) {
                            selectedEncounter = null
                        }
                        encounterToDelete = null
                        Toast.makeText(context, "Karsilasma silindi", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                ) {
                    Text("Sil")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { encounterToDelete = null }) {
                    Text("Vazgec")
                }
            }
        )
    }

    // Tumunu sil dialog
    if (showDeleteAllDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteAllDialog = false },
            title = { Text("Tum Karsilasmalari Sil") },
            text = { Text("${encounters.size} karsilasma ve tum verileri kalici olarak silinecek. Devam etmek istiyor musunuz?") },
            confirmButton = {
                Button(
                    onClick = {
                        encounters.forEach { enc ->
                            dbHelper.deleteRematchEncounter(enc.id)
                        }
                        encounters = emptyList()
                        allEncounterStats = emptyMap()
                        selectedEncounter = null
                        showDeleteAllDialog = false
                        Toast.makeText(context, "Tum karsilasmalar silindi", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                ) {
                    Text("Tumunu Sil")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showDeleteAllDialog = false }) {
                    Text("Vazgec")
                }
            }
        )
    }

    Row(
        modifier = Modifier.fillMaxSize()
    ) {
        // Sol panel - Karsilasma listesi
        Column(
            modifier = Modifier
                .weight(0.4f)
                .fillMaxHeight()
                .background(Color(0xFFF5F5F5))
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Karsilasmalar",
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp
                )
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (encounters.isNotEmpty()) {
                        Button(
                            onClick = { showDeleteAllDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFB71C1C)),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text("Tumunu Sil", fontSize = 13.sp)
                        }
                    }
                    Button(
                        onClick = {
                            context.startActivity(Intent(context, RematchSetupActivity::class.java))
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50)),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text("+ Yeni", fontSize = 13.sp)
                    }
                }
            }

            if (encounters.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "Henuz karsilasma yok",
                            color = Color.Gray
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = {
                                context.startActivity(Intent(context, RematchSetupActivity::class.java))
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6A1B9A))
                        ) {
                            Text("Yeni Karsilasma Baslat")
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
                    Text(
                        text = "Detaylari gormek icin bir karsilasma secin",
                        color = Color.Gray
                    )
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

@Composable
fun EncounterListItem(
    encounter: RematchEncounter,
    isSelected: Boolean,
    stats: List<RematchEncounterStats>,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    val textColor = if (isSelected) Color.White else Color.Black
    val subtextColor = if (isSelected) Color.White.copy(alpha = 0.8f) else Color.Gray

    // Tarih formatlama
    val formattedDate = try {
        val inputFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        val outputFormat = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
        val date = inputFormat.parse(encounter.createdDate)
        if (date != null) outputFormat.format(date) else encounter.createdDate
    } catch (e: Exception) {
        encounter.createdDate
    }

    // Istatistikler
    val p1Stats = stats.find { it.playerId == encounter.player1Id }
    val p2Stats = stats.find { it.playerId == encounter.player2Id }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) Color(0xFF6A1B9A) else Color.White
        ),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            // Baslik satiri + silme butonu
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${encounter.player1Name} vs ${encounter.player2Name}",
                    fontWeight = FontWeight.Bold,
                    color = textColor,
                    modifier = Modifier.weight(1f)
                )
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(24.dp)
                ) {
                    Text(
                        text = "\u2715",
                        fontSize = 14.sp,
                        color = if (isSelected) Color.White.copy(alpha = 0.7f) else Color.Gray
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Tarih ve durum satiri
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = formattedDate,
                    fontSize = 11.sp,
                    color = subtextColor
                )
                Text(
                    text = when (encounter.status) {
                        RematchStatus.ACTIVE -> "Aktif"
                        RematchStatus.ROUND1_COMPLETE -> "Rovans Bekliyor"
                        RematchStatus.ROUND2_ACTIVE -> "Rovans"
                        RematchStatus.COMPLETED -> "Tamamlandi"
                        RematchStatus.CANCELLED -> "Iptal"
                    },
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = when (encounter.status) {
                        RematchStatus.ACTIVE -> if (isSelected) Color.Green else Color(0xFF4CAF50)
                        RematchStatus.COMPLETED -> if (isSelected) Color.Cyan else Color(0xFF2196F3)
                        RematchStatus.CANCELLED -> if (isSelected) Color.Red else Color(0xFFF44336)
                        else -> if (isSelected) Color.Yellow else Color(0xFFFF9800)
                    }
                )
            }

            // Tur ve parti satiri
            Text(
                text = "Tur ${encounter.currentRound}/2 | Parti ${encounter.currentPartyIndex + 1}/${encounter.totalParties}",
                fontSize = 11.sp,
                color = subtextColor
            )

            // Istatistikler bolumu
            if (p1Stats != null && p2Stats != null) {
                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 6.dp),
                    thickness = 1.dp,
                    color = if (isSelected) Color.White.copy(alpha = 0.3f) else Color.LightGray
                )

                // Parti ve oyun skoru
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Parti: ${p1Stats.totalPartiesWon} - ${p2Stats.totalPartiesWon}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = textColor
                    )
                    Text(
                        text = "Oyun: ${p1Stats.totalGamesWon} - ${p2Stats.totalGamesWon}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = textColor
                    )
                }

                // Puan
                Text(
                    text = "Puan: ${p1Stats.totalPoints} - ${p2Stats.totalPoints}",
                    fontSize = 11.sp,
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
    // Karsilastirilabilecek partileri bul (Tur 2'de tamamlanan partiler)
    var completedRound2Parties by remember { mutableStateOf(listOf<Int>()) }
    LaunchedEffect(encounter.id) {
        if (dbHelper != null) {
            val r2Results = dbHelper.getRematchPartyResults(encounter.id, roundNumber = 2)
            completedRound2Parties = r2Results.map { it.partyIndex }
        }
    }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Baslik
        Text(
            text = "${encounter.player1Name} vs ${encounter.player2Name}",
            fontWeight = FontWeight.Bold,
            fontSize = 24.sp,
            color = Color(0xFF6A1B9A)
        )

        // Durum karti
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFF3E5F5))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Tur", color = Color.Gray, fontSize = 12.sp)
                    Text(
                        "${encounter.currentRound}/2",
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp
                    )
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Parti", color = Color.Gray, fontSize = 12.sp)
                    Text(
                        "${encounter.currentPartyIndex + 1}/${encounter.totalParties}",
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp
                    )
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Durum", color = Color.Gray, fontSize = 12.sp)
                    Text(
                        when (encounter.status) {
                            RematchStatus.ACTIVE -> "Aktif"
                            RematchStatus.ROUND1_COMPLETE -> "Rovans Bekliyor"
                            RematchStatus.ROUND2_ACTIVE -> "Rovans Aktif"
                            RematchStatus.COMPLETED -> "Tamamlandi"
                            RematchStatus.CANCELLED -> "Iptal"
                        },
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = when (encounter.status) {
                            RematchStatus.ACTIVE -> Color(0xFF4CAF50)
                            RematchStatus.COMPLETED -> Color(0xFF2196F3)
                            RematchStatus.CANCELLED -> Color(0xFFF44336)
                            else -> Color(0xFFFF9800)
                        }
                    )
                }
            }
        }

        // Istatistikler
        if (stats.isNotEmpty()) {
            Text(
                text = "Istatistikler",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )

            stats.forEach { playerStat ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = playerStat.playerName,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text("Tur 1", color = Color.Gray, fontSize = 12.sp)
                                Text("${playerStat.round1PartiesWon} parti")
                                Text("${playerStat.round1GamesWon} oyun")
                                Text("${playerStat.round1Points} puan")
                            }
                            Column {
                                Text("Tur 2", color = Color.Gray, fontSize = 12.sp)
                                Text("${playerStat.round2PartiesWon} parti")
                                Text("${playerStat.round2GamesWon} oyun")
                                Text("${playerStat.round2Points} puan")
                            }
                            Column {
                                Text("Toplam", color = Color.Gray, fontSize = 12.sp)
                                Text(
                                    "${playerStat.totalPartiesWon} parti",
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    "${playerStat.totalGamesWon} oyun",
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    "${playerStat.totalPoints} puan",
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF6A1B9A)
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
                fontSize = 16.sp
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                completedRound2Parties.take(10).forEach { partyIdx ->
                    OutlinedButton(
                        onClick = { onCompareParty(partyIdx) },
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text("P${partyIdx + 1}", fontSize = 12.sp)
                    }
                }
                if (completedRound2Parties.size > 10) {
                    Text("...", modifier = Modifier.align(Alignment.CenterVertically))
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Butonlar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (encounter.status == RematchStatus.ACTIVE || encounter.status == RematchStatus.ROUND1_COMPLETE || encounter.status == RematchStatus.ROUND2_ACTIVE) {
                Button(
                    onClick = onContinue,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))
                ) {
                    Text("Devam Et")
                }

                OutlinedButton(
                    onClick = onCancel,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Iptal Et", color = Color.Red)
                }
            }

            Button(
                onClick = onDelete,
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
            ) {
                Text("Sil")
            }
        }
    }
}
