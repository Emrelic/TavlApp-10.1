package com.tavla.tavlapp

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class RematchSetupActivity : ComponentActivity() {
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

    var selectedMatchCount by remember { mutableStateOf("100") }
    var manualMatchCount by remember { mutableStateOf("") }
    val matchCountOptions = listOf("10", "25", "50", "100", "200")

    var selectedTargetScore by remember { mutableStateOf("11") }
    val targetScoreOptions = listOf("3", "5", "7", "9", "11", "13", "15")

    var trackPipCount by remember { mutableStateOf(true) }
    var isGenerating by remember { mutableStateOf(false) }

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
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Baslik
        Text(
            text = "Rovansli Karsilasma",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF6A1B9A)
        )

        // Oyuncu secimi
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Oyuncu 1
            Column(modifier = Modifier.weight(1f)) {
                Text("Oyuncu 1", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Box {
                    OutlinedTextField(
                        value = selectedPlayer1?.name ?: "",
                        onValueChange = { },
                        modifier = Modifier.fillMaxWidth(),
                        readOnly = true,
                        placeholder = { Text("Sec", fontSize = 12.sp) },
                        trailingIcon = {
                            IconButton(onClick = { showPlayer1Menu = true }) {
                                Icon(Icons.Default.ArrowDropDown, "Dropdown")
                            }
                        },
                        singleLine = true
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
                        modifier = Modifier.fillMaxWidth(),
                        readOnly = true,
                        placeholder = { Text("Sec", fontSize = 12.sp) },
                        trailingIcon = {
                            IconButton(onClick = { showPlayer2Menu = true }) {
                                Icon(Icons.Default.ArrowDropDown, "Dropdown")
                            }
                        },
                        singleLine = true
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
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Sol: Parti Sayisi
            Column(modifier = Modifier.weight(1f)) {
                Text("Parti Sayisi", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Spacer(modifier = Modifier.height(4.dp))
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
                Text("Hedef Puan", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Spacer(modifier = Modifier.height(4.dp))
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

        // Pip sayisi toggle
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Pip Sayisi Islensin", fontWeight = FontWeight.Bold, fontSize = 13.sp)
            Switch(
                checked = trackPipCount,
                onCheckedChange = { trackPipCount = it },
                colors = SwitchDefaults.colors(checkedTrackColor = Color(0xFF6A1B9A))
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
                .height(56.dp),
            enabled = !isGenerating && selectedPlayer1 != null && selectedPlayer2 != null,
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50)),
            shape = RoundedCornerShape(8.dp)
        ) {
            Text(
                text = if (isGenerating) "URETILIYOR..." else "KARSILASMAYA BASLA",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
        }

        OutlinedButton(
            onClick = { (context as? ComponentActivity)?.finish() },
            modifier = Modifier.fillMaxWidth(),
            enabled = !isGenerating
        ) {
            Text("Iptal")
        }
    }
}
