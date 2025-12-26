package com.tavla.tavlapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Hareketler Dokumu Ekrani
 * Programda yapilan tum islemleri saat ve dakika olarak gosterir
 */
class ActivityLogActivity : ComponentActivity() {
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
                    ActivityLogScreen(
                        dbHelper = dbHelper,
                        onBack = { finish() }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActivityLogScreen(
    dbHelper: DatabaseHelper,
    onBack: () -> Unit
) {
    var logs by remember { mutableStateOf(listOf<ActivityLog>()) }
    var showClearDialog by remember { mutableStateOf(false) }
    var filterMode by remember { mutableStateOf("all") } // "all", "today", "game"

    // Loglari yukle
    LaunchedEffect(filterMode) {
        logs = when (filterMode) {
            "today" -> dbHelper.getTodayActivityLogs()
            else -> dbHelper.getAllActivityLogs()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Hareketler Dokumu") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Geri")
                    }
                },
                actions = {
                    IconButton(onClick = { showClearDialog = true }) {
                        Icon(Icons.Default.Delete, contentDescription = "Temizle")
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
            // Filtre butonlari
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    onClick = { filterMode = "all" },
                    label = { Text("Tum Kayitlar") },
                    selected = filterMode == "all"
                )
                FilterChip(
                    onClick = { filterMode = "today" },
                    label = { Text("Bugun") },
                    selected = filterMode == "today"
                )
            }

            // Log sayisi
            Text(
                text = "${logs.size} kayit",
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray
            )

            // Log listesi
            if (logs.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Henuz kayit yok",
                        color = Color.Gray
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(logs) { log ->
                        ActivityLogItem(log)
                    }
                }
            }
        }
    }

    // Temizleme onay dialog
    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = { Text("Kayitlari Temizle") },
            text = { Text("Tum hareketler dokumu silinecek. Bu islem geri alinamaz!") },
            confirmButton = {
                TextButton(
                    onClick = {
                        dbHelper.clearActivityLogs()
                        logs = emptyList()
                        showClearDialog = false
                    }
                ) {
                    Text("Temizle", color = Color.Red)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) {
                    Text("Iptal")
                }
            }
        )
    }
}

@Composable
fun ActivityLogItem(log: ActivityLog) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = getActionTypeColor(log.actionType).copy(alpha = 0.1f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Saat
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.width(60.dp)
            ) {
                Text(
                    text = log.timestamp,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = getActionTypeColor(log.actionType)
                )
                // Tarih (sadece gun-ay)
                val dateOnly = log.dateTime.substring(5, 10) // MM-dd
                Text(
                    text = dateOnly,
                    fontSize = 10.sp,
                    color = Color.Gray
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Ikon ve aciklama
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Aksiyon tipi ikonu
                    Text(
                        text = getActionTypeIcon(log.actionType),
                        fontSize = 16.sp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = getActionTypeLabel(log.actionType),
                        fontWeight = FontWeight.Medium,
                        fontSize = 12.sp,
                        color = getActionTypeColor(log.actionType)
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = log.description,
                    fontSize = 13.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                // Oyuncu isimleri (varsa)
                if (log.player1Name != null || log.player2Name != null) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = buildString {
                            if (log.player1Name != null && log.player2Name != null) {
                                append("${log.player1Name} vs ${log.player2Name}")
                            } else if (log.player1Name != null) {
                                append(log.player1Name)
                            } else if (log.player2Name != null) {
                                append(log.player2Name)
                            }
                        },
                        fontSize = 11.sp,
                        color = Color.Gray
                    )
                }
            }
        }
    }
}

// Aksiyon tipine gore renk
fun getActionTypeColor(actionType: String): Color {
    return when (actionType) {
        ActionTypes.APP_OPEN, ActionTypes.APP_CLOSE -> Color(0xFF2196F3) // Mavi
        ActionTypes.GAME_START -> Color(0xFF4CAF50) // Yesil
        ActionTypes.GAME_END -> Color(0xFF9C27B0) // Mor
        ActionTypes.SCORE_SINGLE, ActionTypes.SCORE_MARS, ActionTypes.SCORE_BACKGAMMON -> Color(0xFF4CAF50) // Yesil
        ActionTypes.SCORE_UNDO -> Color(0xFFFF9800) // Turuncu
        ActionTypes.DOUBLE_OFFER -> Color(0xFFFF5722) // Koyu turuncu
        ActionTypes.DOUBLE_ACCEPT -> Color(0xFF4CAF50) // Yesil
        ActionTypes.DOUBLE_REJECT -> Color(0xFFF44336) // Kirmizi
        ActionTypes.DOUBLE_CANCEL -> Color(0xFF9E9E9E) // Gri
        ActionTypes.DICE_ROLL -> Color(0xFF00BCD4) // Cyan
        ActionTypes.DATA_RESET -> Color(0xFFF44336) // Kirmizi
        else -> Color(0xFF757575) // Varsayilan gri
    }
}

// Aksiyon tipine gore ikon
fun getActionTypeIcon(actionType: String): String {
    return when (actionType) {
        ActionTypes.APP_OPEN -> "📱"
        ActionTypes.APP_CLOSE -> "👋"
        ActionTypes.GAME_START -> "🎮"
        ActionTypes.GAME_END -> "🏆"
        ActionTypes.SCORE_SINGLE -> "1️⃣"
        ActionTypes.SCORE_MARS -> "2️⃣"
        ActionTypes.SCORE_BACKGAMMON -> "3️⃣"
        ActionTypes.SCORE_UNDO -> "↩️"
        ActionTypes.DOUBLE_OFFER -> "🎲"
        ActionTypes.DOUBLE_ACCEPT -> "✓"
        ActionTypes.DOUBLE_REJECT -> "✗"
        ActionTypes.DOUBLE_CANCEL -> "↩️"
        ActionTypes.DICE_ROLL -> "🎲"
        ActionTypes.DICE_SCREEN_OPEN -> "🎲"
        ActionTypes.DICE_SCREEN_CLOSE -> "🎲"
        ActionTypes.TIMER_START -> "⏱️"
        ActionTypes.TIMER_PAUSE -> "⏸️"
        ActionTypes.TIMER_RESET -> "🔄"
        ActionTypes.DATA_RESET -> "🗑️"
        ActionTypes.SETTINGS_PLAYER1_SELECT, ActionTypes.SETTINGS_PLAYER2_SELECT -> "👤"
        ActionTypes.SETTINGS_GAME_TYPE -> "🎯"
        ActionTypes.SETTINGS_ROUNDS -> "🔢"
        ActionTypes.NEW_PLAYER_ADDED -> "➕"
        else -> "•"
    }
}

// Aksiyon tipine gore etiket
fun getActionTypeLabel(actionType: String): String {
    return when (actionType) {
        ActionTypes.APP_OPEN -> "Uygulama Acildi"
        ActionTypes.APP_CLOSE -> "Uygulama Kapandi"
        ActionTypes.GAME_START -> "Oyun Basladi"
        ActionTypes.GAME_END -> "Mac Bitti"
        ActionTypes.SCORE_SINGLE -> "Tek Sayi"
        ActionTypes.SCORE_MARS -> "Mars"
        ActionTypes.SCORE_BACKGAMMON -> "Backgammon"
        ActionTypes.SCORE_UNDO -> "Geri Alma"
        ActionTypes.DOUBLE_OFFER -> "Katlama Teklifi"
        ActionTypes.DOUBLE_ACCEPT -> "Katlama Kabul"
        ActionTypes.DOUBLE_REJECT -> "Pes Etme"
        ActionTypes.DOUBLE_CANCEL -> "Katlama Iptal"
        ActionTypes.DICE_ROLL -> "Zar Atma"
        ActionTypes.DICE_SCREEN_OPEN -> "Zar Ekrani"
        ActionTypes.DICE_SCREEN_CLOSE -> "Zar Ekrani Kapandi"
        ActionTypes.TIMER_START -> "Zamanlayici"
        ActionTypes.TIMER_PAUSE -> "Duraklama"
        ActionTypes.TIMER_RESET -> "Sifirlama"
        ActionTypes.DATA_RESET -> "Veri Silme"
        ActionTypes.SETTINGS_PLAYER1_SELECT -> "Oyuncu 1"
        ActionTypes.SETTINGS_PLAYER2_SELECT -> "Oyuncu 2"
        ActionTypes.SETTINGS_GAME_TYPE -> "Oyun Tipi"
        ActionTypes.SETTINGS_ROUNDS -> "El Sayisi"
        ActionTypes.SETTINGS_SCORE_MODE -> "Skor Modu"
        ActionTypes.SETTINGS_DICE_ROLLER -> "Zar Atici"
        ActionTypes.SETTINGS_TIMER -> "Zamanlayici"
        ActionTypes.SETTINGS_STATISTICS -> "Istatistik"
        ActionTypes.SETTINGS_DICE_EVAL -> "Zar Degerlendirme"
        ActionTypes.NEW_PLAYER_ADDED -> "Yeni Oyuncu"
        else -> actionType
    }
}
