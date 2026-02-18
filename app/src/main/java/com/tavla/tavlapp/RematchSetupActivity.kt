package com.tavla.tavlapp

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.content.Intent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Rovansli Karsilasma kurulum ekrani
 * Oyuncu secimi, mac sayisi ve zar seti uretimi
 */
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

    // Oyuncu listesi
    val playersList = remember { mutableStateOf(dbHelper.getAllPlayers()) }

    // Secilen oyuncular
    var selectedPlayer1 by remember { mutableStateOf<Player?>(null) }
    var selectedPlayer2 by remember { mutableStateOf<Player?>(null) }

    // Dropdown durumlari
    var showPlayer1Menu by remember { mutableStateOf(false) }
    var showPlayer2Menu by remember { mutableStateOf(false) }

    // Mac sayisi
    var selectedMatchCount by remember { mutableStateOf("100") }
    var manualMatchCount by remember { mutableStateOf("") }
    val matchCountOptions = listOf("10", "25", "50", "100", "200")

    // Uretim durumu
    var isGenerating by remember { mutableStateOf(false) }

    // Son mactan oyunculari varsayilan sec
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

    // Gercek mac sayisi (manuel veya secili)
    val actualMatchCount = manualMatchCount.toIntOrNull() ?: selectedMatchCount.toIntOrNull() ?: 100

    Row(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Sol panel - Ayarlar
        Column(
            modifier = Modifier
                .weight(0.6f)
                .fillMaxHeight(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Baslik
            Text(
                text = "Rovansli Karsilasma",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF6A1B9A)
            )

            // Oyuncu Secimi
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

            // Parti Sayisi
            Text("Parti Sayisi", fontWeight = FontWeight.Bold, fontSize = 13.sp)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Hazir butonlar
                matchCountOptions.forEach { count ->
                    Button(
                        onClick = {
                            selectedMatchCount = count
                            manualMatchCount = "" // Manuel girisi temizle
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (selectedMatchCount == count && manualMatchCount.isEmpty())
                                Color(0xFF6A1B9A) else Color.LightGray
                        ),
                        shape = RoundedCornerShape(6.dp),
                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = count,
                            color = if (selectedMatchCount == count && manualMatchCount.isEmpty())
                                Color.White else Color.Black,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                }

                // Manuel giris
                OutlinedTextField(
                    value = manualMatchCount,
                    onValueChange = { newValue ->
                        if (newValue.isEmpty() || (newValue.all { it.isDigit() } && newValue.length <= 4)) {
                            manualMatchCount = newValue
                        }
                    },
                    modifier = Modifier.width(80.dp),
                    placeholder = { Text("Diger", fontSize = 11.sp) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF6A1B9A),
                        unfocusedBorderColor = if (manualMatchCount.isNotEmpty()) Color(0xFF6A1B9A) else Color.Gray
                    )
                )
            }

            // Secilen parti sayisi gosterimi
            if (manualMatchCount.isNotEmpty()) {
                Text(
                    text = "Secilen: $actualMatchCount parti",
                    color = Color(0xFF6A1B9A),
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }
        }

        // Sag panel - Bilgi ve Butonlar
        Column(
            modifier = Modifier
                .weight(0.4f)
                .fillMaxHeight(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Bilgi kutusu
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFF3E5F5))
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = "Nasil Calisir?",
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF6A1B9A),
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "1. Her parti 11 puanda biter\n" +
                               "2. Bir partide max 21 el oynanir\n" +
                               "3. $actualMatchCount parti uretilir\n" +
                               "4. Rovans: Zarlar yer degistirir\n" +
                               "5. Toplam ${actualMatchCount * 2} parti",
                        fontSize = 12.sp,
                        lineHeight = 16.sp
                    )
                }
            }

            // Uretim ilerleme gostergesi
            if (isGenerating) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9))
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator(
                            color = Color(0xFF4CAF50),
                            modifier = Modifier.size(40.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Zar setleri uretiliyor...",
                            color = Color(0xFF4CAF50),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Butonlar
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // BASLAT butonu - buyuk ve belirgin
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
                                        actualMatchCount
                                    )
                                }

                                if (newEncounterId != -1L) {
                                    val success = withContext(Dispatchers.IO) {
                                        dbHelper.generateAndSaveDiceSets(newEncounterId, actualMatchCount)
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

                                        // Skorboard ekranına git (rövanşlı mod)
                                        val encounter = dbHelper.getRematchEncounter(newEncounterId)
                                        val intent = Intent(context, GameScoreActivity::class.java)
                                        intent.putExtra("is_rematch_mode", true)
                                        intent.putExtra("encounter_id", newEncounterId)
                                        intent.putExtra("player1_id", selectedPlayer1!!.id)
                                        intent.putExtra("player2_id", selectedPlayer2!!.id)
                                        intent.putExtra("player1_name", selectedPlayer1?.name ?: "")
                                        intent.putExtra("player2_name", selectedPlayer2?.name ?: "")
                                        intent.putExtra("total_parties", actualMatchCount)
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
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF4CAF50)
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = if (isGenerating) "URETILIYOR..." else "KARSILASMAYA BASLA",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }

                // Iptal butonu
                OutlinedButton(
                    onClick = { (context as? ComponentActivity)?.finish() },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isGenerating
                ) {
                    Text("Iptal")
                }
            }
        }
    }
}
