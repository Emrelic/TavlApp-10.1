package com.tavla.tavlapp

import android.os.Bundle
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.content.Intent
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class RematchSetupActivity : ComponentActivity() {
    private lateinit var dbHelper: DatabaseHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // ✅ Tam ekran - Android status bar gizle
        WindowCompat.setDecorFitsSystemWindows(window, false)
        val controller = WindowInsetsControllerCompat(window, window.decorView)
        controller.hide(WindowInsetsCompat.Type.systemBars())
        controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE

        dbHelper = DatabaseHelper(this)

        setContent {
            TavlaAppTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    RematchSetupScreen(dbHelper)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RematchSetupScreen(dbHelper: DatabaseHelper) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val playersList = remember { mutableStateOf(dbHelper.getAllPlayers()) }
    var selectedPlayer1 by remember { mutableStateOf<Player?>(null) }
    var selectedPlayer2 by remember { mutableStateOf<Player?>(null) }
    var showPlayer1Menu by remember { mutableStateOf(false) }
    var showPlayer2Menu by remember { mutableStateOf(false) }

    var selectedMatchCount by remember { mutableStateOf("3") }
    var manualMatchCount by remember { mutableStateOf("") }
    val matchCountOptions = listOf("1", "2", "3", "4", "5")

    var selectedTargetScore by remember { mutableStateOf("11") }
    val targetScoreOptions = listOf("3", "5", "7", "9", "11", "13", "15")

    var trackPipCount by remember { mutableStateOf(false) }
    var isGenerating by remember { mutableStateOf(false) }

    // ✅ SAAT AYARLARI
    var useTimer by remember { mutableStateOf(false) }
    var showTimerSettingsDialog by remember { mutableStateOf(false) }
    var timerMode by remember { mutableStateOf("DELAY") } // DELAY veya FISCHER
    var reserveTimeSeconds by remember { mutableIntStateOf(120) } // Varsayılan 2 dakika
    var delayTimeSeconds by remember { mutableIntStateOf(12) } // Varsayılan 12 saniye

    LaunchedEffect(playersList.value) {
        if (playersList.value.isNotEmpty() && selectedPlayer1 == null && selectedPlayer2 == null) {
            val lastMatches = dbHelper.getAllMatches()
            if (lastMatches.isNotEmpty()) {
                val lastMatch = lastMatches.first()
                val lastPlayer1 = playersList.value.find { it.id == lastMatch.player1Id }
                val lastPlayer2 = playersList.value.find { it.id == lastMatch.player2Id }
                if (lastPlayer1 != null && lastPlayer2 != null) {
                    selectedPlayer1 = lastPlayer1
                    selectedPlayer2 = lastPlayer2
                }
            } else if (playersList.value.size >= 2) {
                selectedPlayer1 = playersList.value[0]
                selectedPlayer2 = playersList.value[1]
            }
        }
    }

    val actualMatchCount = manualMatchCount.toIntOrNull() ?: selectedMatchCount.toIntOrNull() ?: 100
    val actualTargetScore = selectedTargetScore.toIntOrNull() ?: 11

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        // Baslik
        Text(
            text = "Rovansli Karsilasma",
            fontWeight = FontWeight.ExtraBold,
            fontSize = 18.sp,
            color = Color(0xFF6A1B9A)
        )

        // Oyuncu secimi
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Oyuncu 1
            Column(modifier = Modifier.weight(1f)) {
                Text("Oyuncu 1", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Box {
                    OutlinedTextField(
                        value = selectedPlayer1?.name ?: "",
                        onValueChange = { },
                        modifier = Modifier.fillMaxWidth().height(58.dp),
                        readOnly = true,
                        placeholder = { Text("Sec", fontSize = 14.sp) },
                        trailingIcon = {
                            IconButton(onClick = { showPlayer1Menu = true }) {
                                Icon(Icons.Default.ArrowDropDown, "Dropdown")
                            }
                        },
                        singleLine = true,
                        textStyle = LocalTextStyle.current.copy(fontSize = 16.sp, fontWeight = FontWeight.Medium)
                    )
                    DropdownMenu(
                        expanded = showPlayer1Menu,
                        onDismissRequest = { showPlayer1Menu = false }
                    ) {
                        playersList.value.forEach { player ->
                            DropdownMenuItem(
                                text = { Text(player.name) },
                                onClick = {
                                    selectedPlayer1 = player
                                    showPlayer1Menu = false
                                }
                            )
                        }
                    }
                }
            }

            // Oyuncu 2
            Column(modifier = Modifier.weight(1f)) {
                Text("Oyuncu 2", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Box {
                    OutlinedTextField(
                        value = selectedPlayer2?.name ?: "",
                        onValueChange = { },
                        modifier = Modifier.fillMaxWidth().height(58.dp),
                        readOnly = true,
                        placeholder = { Text("Sec", fontSize = 14.sp) },
                        trailingIcon = {
                            IconButton(onClick = { showPlayer2Menu = true }) {
                                Icon(Icons.Default.ArrowDropDown, "Dropdown")
                            }
                        },
                        singleLine = true,
                        textStyle = LocalTextStyle.current.copy(fontSize = 16.sp, fontWeight = FontWeight.Medium)
                    )
                    DropdownMenu(
                        expanded = showPlayer2Menu,
                        onDismissRequest = { showPlayer2Menu = false }
                    ) {
                        playersList.value.forEach { player ->
                            DropdownMenuItem(
                                text = { Text(player.name) },
                                onClick = {
                                    selectedPlayer2 = player
                                    showPlayer2Menu = false
                                }
                            )
                        }
                    }
                }
            }
        }

        // Ayarlar satirlari
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Sol: Parti Sayisi
            Column(modifier = Modifier.weight(1f)) {
                Text("Parti Sayisi", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    matchCountOptions.forEach { count ->
                        Button(
                            onClick = {
                                selectedMatchCount = count
                                manualMatchCount = ""
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (selectedMatchCount == count && manualMatchCount.isEmpty())
                                    Color(0xFF6A1B9A) else Color.LightGray
                            ),
                            shape = RoundedCornerShape(6.dp),
                            contentPadding = PaddingValues(horizontal = 2.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = count,
                                color = if (selectedMatchCount == count && manualMatchCount.isEmpty())
                                    Color.White else Color.Black,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }
                    }
                    OutlinedTextField(
                        value = manualMatchCount,
                        onValueChange = { newValue ->
                            if (newValue.isEmpty() || (newValue.all { it.isDigit() } && newValue.length <= 4)) {
                                manualMatchCount = newValue
                            }
                        },
                        modifier = Modifier.width(70.dp),
                        placeholder = { Text("Diger", fontSize = 10.sp) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF6A1B9A),
                            unfocusedBorderColor = if (manualMatchCount.isNotEmpty()) Color(0xFF6A1B9A) else Color.Gray
                        )
                    )
                }
            }

            // Sag: Hedef Puan
            Column(modifier = Modifier.weight(1f)) {
                Text("Hedef Puan", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    targetScoreOptions.forEach { score ->
                        Button(
                            onClick = { selectedTargetScore = score },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (selectedTargetScore == score)
                                    Color(0xFF6A1B9A) else Color.LightGray
                            ),
                            shape = RoundedCornerShape(6.dp),
                            contentPadding = PaddingValues(horizontal = 2.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = score,
                                color = if (selectedTargetScore == score)
                                    Color.White else Color.Black,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }
        }

        // Pip + Saat toggle'ları aynı satırda
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Pip sayisi
            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Pip Sayisi", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                Switch(
                    checked = trackPipCount,
                    onCheckedChange = { trackPipCount = it },
                    colors = SwitchDefaults.colors(checkedTrackColor = Color(0xFF6A1B9A)),
                    modifier = Modifier.height(28.dp)
                )
            }

            // Saat Kullan
            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Saat Kullan", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    if (useTimer) {
                        val modeLabel = if (timerMode == "DELAY") "Delay" else "Fischer"
                        Text(
                            text = "$modeLabel ${reserveTimeSeconds}s+${delayTimeSeconds}s",
                            fontSize = 9.sp,
                            color = Color(0xFF6A1B9A),
                            modifier = Modifier.clickable { showTimerSettingsDialog = true }
                        )
                    }
                }
                Switch(
                    checked = useTimer,
                    onCheckedChange = {
                        useTimer = it
                        if (it) showTimerSettingsDialog = true
                    },
                    colors = SwitchDefaults.colors(checkedTrackColor = Color(0xFF6A1B9A)),
                    modifier = Modifier.height(28.dp)
                )
            }
        }

        // ✅ Saat Ayarları Dialog
        if (showTimerSettingsDialog) {
            var tempMode by remember { mutableStateOf(timerMode) }
            var tempReserve by remember { mutableStateOf(reserveTimeSeconds.toString()) }
            var tempDelay by remember { mutableStateOf(delayTimeSeconds.toString()) }

            AlertDialog(
                onDismissRequest = { showTimerSettingsDialog = false },
                title = { Text("Saat Ayarlari", fontWeight = FontWeight.ExtraBold) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        // Şablon presetler
                        Text("Sablonlar:", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            listOf(
                                Triple("Hizli", 60, 8),
                                Triple("Normal", 120, 12),
                                Triple("Yavas", 180, 15)
                            ).forEach { (label, res, del) ->
                                OutlinedButton(
                                    onClick = {
                                        tempReserve = res.toString()
                                        tempDelay = del.toString()
                                    },
                                    modifier = Modifier.weight(1f),
                                    contentPadding = PaddingValues(4.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        containerColor = if (tempReserve == res.toString() && tempDelay == del.toString())
                                            Color(0xFF6A1B9A).copy(alpha = 0.15f) else Color.Transparent
                                    )
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(label, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        Text("${res}sn+${del}sn", fontSize = 9.sp)
                                    }
                                }
                            }
                        }

                        Divider()

                        // Saat modu
                        Text("Saat Modu:", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf("DELAY" to "Delay (FIBO)", "FISCHER" to "Fischer").forEach { (mode, label) ->
                                FilterChip(
                                    selected = tempMode == mode,
                                    onClick = { tempMode = mode },
                                    label = { Text(label, fontSize = 11.sp) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = Color(0xFF6A1B9A),
                                        selectedLabelColor = Color.White
                                    )
                                )
                            }
                        }
                        Divider()

                        // Elle ayar
                        Text("Elle Ayar:", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Rezerv (saniye)", fontSize = 14.sp, fontWeight = FontWeight.Medium)
                                OutlinedTextField(
                                    value = tempReserve,
                                    onValueChange = { tempReserve = it.filter { c -> c.isDigit() } },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true,
                                    textStyle = LocalTextStyle.current.copy(fontSize = 18.sp, fontWeight = FontWeight.Bold)
                                )
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Hamle suresi (saniye)", fontSize = 14.sp, fontWeight = FontWeight.Medium)
                                OutlinedTextField(
                                    value = tempDelay,
                                    onValueChange = { tempDelay = it.filter { c -> c.isDigit() } },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true,
                                    textStyle = LocalTextStyle.current.copy(fontSize = 18.sp, fontWeight = FontWeight.Bold)
                                )
                            }
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            timerMode = tempMode
                            reserveTimeSeconds = tempReserve.toIntOrNull() ?: 120
                            delayTimeSeconds = tempDelay.toIntOrNull() ?: 12
                            showTimerSettingsDialog = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))
                    ) { Text("Tamam") }
                },
                dismissButton = {
                    TextButton(onClick = { showTimerSettingsDialog = false }) {
                        Text("Iptal")
                    }
                }
            )
        }

        // Uretim ilerleme
        if (isGenerating) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9))
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator(
                        color = Color(0xFF4CAF50),
                        modifier = Modifier.size(32.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Zar setleri uretiliyor...",
                        color = Color(0xFF4CAF50),
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        // Butonlar
        Button(
            onClick = {
                if (selectedPlayer1 == null || selectedPlayer2 == null) {
                    Toast.makeText(context, "Lutfen iki oyuncu secin", Toast.LENGTH_SHORT).show()
                    return@Button
                }
                if (selectedPlayer1?.id == selectedPlayer2?.id) {
                    Toast.makeText(context, "Farkli oyuncular secmelisiniz", Toast.LENGTH_SHORT).show()
                    return@Button
                }
                if (actualMatchCount < 1 || actualMatchCount > 1000) {
                    Toast.makeText(context, "Parti sayisi 1-1000 arasi olmali", Toast.LENGTH_SHORT).show()
                    return@Button
                }

                isGenerating = true

                coroutineScope.launch {
                    try {
                        val newEncounterId = withContext(Dispatchers.IO) {
                            dbHelper.createRematchEncounter(
                                selectedPlayer1!!.id,
                                selectedPlayer2!!.id,
                                actualMatchCount,
                                actualTargetScore,
                                trackPipCount
                            )
                        }

                        if (newEncounterId != -1L) {
                            val success = withContext(Dispatchers.IO) {
                                dbHelper.generateAndSaveDiceSets(newEncounterId, actualMatchCount, actualTargetScore)
                            }

                            if (success) {
                                withContext(Dispatchers.IO) {
                                    dbHelper.addActivityLog(
                                        actionType = ActionTypes.REMATCH_ENCOUNTER_CREATE,
                                        description = "Rovansli karsilasma: ${selectedPlayer1?.name} vs ${selectedPlayer2?.name}, $actualMatchCount parti",
                                        player1Name = selectedPlayer1?.name,
                                        player2Name = selectedPlayer2?.name
                                    )
                                }

                                val intent = Intent(context, GameScoreActivity::class.java)
                                intent.putExtra("is_rematch_mode", true)
                                intent.putExtra("encounter_id", newEncounterId)
                                intent.putExtra("player1_id", selectedPlayer1!!.id)
                                intent.putExtra("player2_id", selectedPlayer2!!.id)
                                intent.putExtra("player1_name", selectedPlayer1?.name ?: "")
                                intent.putExtra("player2_name", selectedPlayer2?.name ?: "")
                                intent.putExtra("total_parties", actualMatchCount)
                                intent.putExtra("rounds", actualTargetScore)
                                // ✅ Saat parametreleri
                                intent.putExtra("use_timer", useTimer)
                                intent.putExtra("timer_mode", timerMode)
                                intent.putExtra("reserve_time", reserveTimeSeconds)
                                intent.putExtra("delay_time", delayTimeSeconds)
                                context.startActivity(intent)
                                (context as? ComponentActivity)?.finish()
                            } else {
                                Toast.makeText(context, "Zar setleri uretilemedi", Toast.LENGTH_SHORT).show()
                            }
                        } else {
                            Toast.makeText(context, "Karsilasma olusturulamadi", Toast.LENGTH_SHORT).show()
                        }
                    } catch (e: Exception) {
                        Toast.makeText(context, "Hata: ${e.message}", Toast.LENGTH_SHORT).show()
                    } finally {
                        isGenerating = false
                    }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(42.dp),
            enabled = !isGenerating && selectedPlayer1 != null && selectedPlayer2 != null,
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50)),
            shape = RoundedCornerShape(8.dp),
            contentPadding = PaddingValues(0.dp)
        ) {
            Text(
                text = if (isGenerating) "URETILIYOR..." else "KARSILASMAYA BASLA",
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )
        }

        OutlinedButton(
            onClick = { (context as? ComponentActivity)?.finish() },
            modifier = Modifier.fillMaxWidth().height(34.dp),
            enabled = !isGenerating,
            contentPadding = PaddingValues(0.dp)
        ) {
            Text("Iptal", fontSize = 12.sp)
        }
    }
}
