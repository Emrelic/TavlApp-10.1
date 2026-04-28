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
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
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
    var useSingleButtonForTimerAndDice by remember { mutableStateOf(false) }
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

    val accentColor = Color(0xFF6A1B9A)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF16213E))
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Baslik
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Rovansli Karsilasma",
                fontWeight = FontWeight.ExtraBold,
                fontSize = 20.sp,
                color = Color(0xFFCE93D8)
            )
            // Ozet bilgi
            if (selectedPlayer1 != null && selectedPlayer2 != null) {
                Text(
                    text = "$actualMatchCount parti | ${actualTargetScore}P",
                    fontSize = 11.sp,
                    color = Color(0xFFBFA47A),
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // Oyuncu secimi - mavi ve kirmizi kartlarla
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Oyuncu 1 - Mavi kart
            Card(
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1565C0).copy(alpha = 0.3f)),
                border = BorderStroke(1.dp, Color(0xFF1565C0).copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(8.dp)) {
                    Text("Oyuncu 1", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Color(0xFF90CAF9))
                    Box {
                        OutlinedTextField(
                            value = selectedPlayer1?.name ?: "",
                            onValueChange = { },
                            modifier = Modifier.fillMaxWidth().height(50.dp),
                            readOnly = true,
                            placeholder = { Text("Sec", fontSize = 13.sp, color = Color.Gray) },
                            trailingIcon = {
                                IconButton(onClick = { showPlayer1Menu = true }) {
                                    Icon(Icons.Default.ArrowDropDown, "Dropdown", tint = Color(0xFF90CAF9))
                                }
                            },
                            singleLine = true,
                            textStyle = LocalTextStyle.current.copy(fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.White),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFF1565C0),
                                unfocusedBorderColor = Color(0xFF1565C0).copy(alpha = 0.4f)
                            )
                        )
                        DropdownMenu(expanded = showPlayer1Menu, onDismissRequest = { showPlayer1Menu = false }) {
                            playersList.value.forEach { player ->
                                DropdownMenuItem(text = { Text(player.name) }, onClick = { selectedPlayer1 = player; showPlayer1Menu = false })
                            }
                        }
                    }
                }
            }

            // VS
            Text("VS", fontWeight = FontWeight.ExtraBold, fontSize = 16.sp, color = Color(0xFF8B7355), modifier = Modifier.align(Alignment.CenterVertically))

            // Oyuncu 2 - Kirmizi kart
            Card(
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFC62828).copy(alpha = 0.3f)),
                border = BorderStroke(1.dp, Color(0xFFC62828).copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(8.dp)) {
                    Text("Oyuncu 2", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Color(0xFFEF9A9A))
                    Box {
                        OutlinedTextField(
                            value = selectedPlayer2?.name ?: "",
                            onValueChange = { },
                            modifier = Modifier.fillMaxWidth().height(50.dp),
                            readOnly = true,
                            placeholder = { Text("Sec", fontSize = 13.sp, color = Color.Gray) },
                            trailingIcon = {
                                IconButton(onClick = { showPlayer2Menu = true }) {
                                    Icon(Icons.Default.ArrowDropDown, "Dropdown", tint = Color(0xFFEF9A9A))
                                }
                            },
                            singleLine = true,
                            textStyle = LocalTextStyle.current.copy(fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.White),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFFC62828),
                                unfocusedBorderColor = Color(0xFFC62828).copy(alpha = 0.4f)
                            )
                        )
                        DropdownMenu(expanded = showPlayer2Menu, onDismissRequest = { showPlayer2Menu = false }) {
                            playersList.value.forEach { player ->
                                DropdownMenuItem(text = { Text(player.name) }, onClick = { selectedPlayer2 = player; showPlayer2Menu = false })
                            }
                        }
                    }
                }
            }
        }

        // Parti Sayisi + Hedef Puan
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Parti Sayisi
            Card(
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(10.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A2E)),
                border = BorderStroke(1.dp, accentColor.copy(alpha = 0.3f))
            ) {
                Column(modifier = Modifier.padding(8.dp)) {
                    Text("Parti Sayisi", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Color(0xFFCE93D8))
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(3.dp), verticalAlignment = Alignment.CenterVertically) {
                        matchCountOptions.forEach { count ->
                            val isSelected = selectedMatchCount == count && manualMatchCount.isEmpty()
                            Button(
                                onClick = { selectedMatchCount = count; manualMatchCount = "" },
                                modifier = Modifier.weight(1f).height(34.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isSelected) accentColor else Color(0xFF2A2A4A)
                                ),
                                shape = RoundedCornerShape(6.dp),
                                contentPadding = PaddingValues(0.dp)
                            ) {
                                Text(count, color = if (isSelected) Color.White else Color(0xFF9E9E9E), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                        }
                        OutlinedTextField(
                            value = manualMatchCount,
                            onValueChange = { newVal: String ->
                                if (newVal.isEmpty() || (newVal.length <= 4 && newVal.all(Char::isDigit))) {
                                    manualMatchCount = newVal
                                }
                            },
                            modifier = Modifier.width(55.dp).height(34.dp),
                            placeholder = { Text("N", fontSize = 10.sp, color = Color.Gray) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            textStyle = LocalTextStyle.current.copy(fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White, textAlign = TextAlign.Center),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = accentColor,
                                unfocusedBorderColor = if (manualMatchCount.isNotEmpty()) accentColor else Color.Gray.copy(alpha = 0.3f)
                            )
                        )
                    }
                }
            }

            // Hedef Puan
            Card(
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(10.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A2E)),
                border = BorderStroke(1.dp, accentColor.copy(alpha = 0.3f))
            ) {
                Column(modifier = Modifier.padding(8.dp)) {
                    Text("Hedef Puan", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Color(0xFFCE93D8))
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                        targetScoreOptions.forEach { score ->
                            val isSelected = selectedTargetScore == score
                            Button(
                                onClick = { selectedTargetScore = score },
                                modifier = Modifier.weight(1f).height(34.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isSelected) accentColor else Color(0xFF2A2A4A)
                                ),
                                shape = RoundedCornerShape(6.dp),
                                contentPadding = PaddingValues(0.dp)
                            ) {
                                Text(score, color = if (isSelected) Color.White else Color(0xFF9E9E9E), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        }

        // Pip + Saat + Tek Buton
        var showButtonModeInfo by remember { mutableStateOf(false) }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // Pip
            Card(
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(10.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A2E)),
                border = BorderStroke(1.dp, if (trackPipCount) accentColor else Color.Gray.copy(alpha = 0.2f))
            ) {
                Column(modifier = Modifier.padding(6.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Pip", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Color(0xFFCE93D8))
                    Switch(checked = trackPipCount, onCheckedChange = { trackPipCount = it }, colors = SwitchDefaults.colors(checkedTrackColor = accentColor), modifier = Modifier.height(28.dp))
                }
            }
            // Saat
            Card(
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(10.dp),
                colors = CardDefaults.cardColors(containerColor = if (useTimer) accentColor.copy(alpha = 0.15f) else Color(0xFF1A1A2E)),
                border = BorderStroke(1.dp, if (useTimer) accentColor else Color.Gray.copy(alpha = 0.2f))
            ) {
                Column(modifier = Modifier.padding(6.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Saat", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Color(0xFFCE93D8))
                    if (useTimer) {
                        val ml = if (timerMode == "DELAY") "D" else "F"
                        Text("$ml ${reserveTimeSeconds}+${delayTimeSeconds}", fontSize = 8.sp, color = accentColor, modifier = Modifier.clickable { showTimerSettingsDialog = true })
                    }
                    Switch(checked = useTimer, onCheckedChange = { useTimer = it; if (it) showTimerSettingsDialog = true }, colors = SwitchDefaults.colors(checkedTrackColor = accentColor), modifier = Modifier.height(28.dp))
                }
            }
            // Tek Buton
            Card(
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(10.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A2E)),
                border = BorderStroke(1.dp, if (useTimer && useSingleButtonForTimerAndDice) accentColor else Color.Gray.copy(alpha = 0.2f))
            ) {
                Column(modifier = Modifier.padding(6.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Tek\nButon", fontWeight = FontWeight.Bold, fontSize = 10.sp, lineHeight = 12.sp, color = if (useTimer) Color(0xFFCE93D8) else Color.Gray, textAlign = TextAlign.Center)
                    Switch(checked = useSingleButtonForTimerAndDice, onCheckedChange = { useSingleButtonForTimerAndDice = it; showButtonModeInfo = true }, enabled = useTimer, colors = SwitchDefaults.colors(checkedTrackColor = accentColor), modifier = Modifier.height(28.dp))
                }
            }
        }

        if (showButtonModeInfo) {
            AlertDialog(
                onDismissRequest = { showButtonModeInfo = false },
                title = {
                    Text(
                        if (useSingleButtonForTimerAndDice) "Tek Buton Modu" else "Çift Buton Modu",
                        fontWeight = FontWeight.ExtraBold
                    )
                },
                text = {
                    Text(
                        if (useSingleButtonForTimerAndDice)
                            "Tek buton modunda:\n\n" +
                            "• Butonuna basan oyuncu KARŞI TARAFIN zarını atar\n" +
                            "• Kendi süresini durdurur\n" +
                            "• Karşı tarafın süresini başlatır\n" +
                            "• Sırayı karşı tarafa geçirir\n\n" +
                            "Oyuncu hamlesini oynadıktan sonra butonuna basar."
                        else
                            "Çift buton modunda:\n\n" +
                            "• Her oyuncu KENDİ zarını atar (ZAR AT)\n" +
                            "• Hamlesini oynar\n" +
                            "• OYNADIM butonuna basarak süresini durdurur\n" +
                            "• Karşı tarafın süresi başlar\n\n" +
                            "Döngü: SIRA KARŞIDA → ZAR AT → OYNADIM → SIRA KARŞIDA",
                        fontSize = 13.sp,
                        lineHeight = 18.sp
                    )
                },
                confirmButton = {
                    TextButton(onClick = { showButtonModeInfo = false }) {
                        Text("TAMAM", fontWeight = FontWeight.Bold)
                    }
                }
            )
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
                shape = RoundedCornerShape(10.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1B5E20).copy(alpha = 0.3f))
            ) {
                Row(
                    modifier = Modifier.padding(12.dp).fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator(color = Color(0xFF4CAF50), modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("Zar setleri uretiliyor...", color = Color(0xFF4CAF50), fontWeight = FontWeight.Bold, fontSize = 13.sp)
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
                                intent.putExtra("use_single_button_for_timer_and_dice", useSingleButtonForTimerAndDice)
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
                .height(48.dp),
            enabled = !isGenerating && selectedPlayer1 != null && selectedPlayer2 != null,
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF4CAF50),
                disabledContainerColor = Color(0xFF4CAF50).copy(alpha = 0.3f)
            ),
            shape = RoundedCornerShape(12.dp),
            contentPadding = PaddingValues(0.dp)
        ) {
            Text(
                text = if (isGenerating) "URETILIYOR..." else "KARSILASMAYA BASLA",
                fontWeight = FontWeight.ExtraBold,
                fontSize = 16.sp,
                color = Color.White
            )
        }

        OutlinedButton(
            onClick = { (context as? ComponentActivity)?.finish() },
            modifier = Modifier.fillMaxWidth().height(34.dp),
            enabled = !isGenerating,
            shape = RoundedCornerShape(8.dp),
            border = BorderStroke(1.dp, Color.Gray.copy(alpha = 0.3f)),
            contentPadding = PaddingValues(0.dp)
        ) {
            Text("Iptal", fontSize = 12.sp, color = Color.Gray)
        }
    }
}
