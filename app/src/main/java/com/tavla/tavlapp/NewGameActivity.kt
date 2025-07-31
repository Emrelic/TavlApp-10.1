package com.tavla.tavlapp

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.border
import androidx.compose.foundation.BorderStroke
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import android.content.Intent

// Yeni oyun ayarları ekranı aktivitesi
class NewGameActivity : ComponentActivity() {
    // Veritabanı yardımcısını tanımla
    private lateinit var dbHelper: DatabaseHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Veritabanı yardımcısını başlat
        dbHelper = DatabaseHelper(this)

        setContent {
            TavlaAppTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    NewGameScreen(dbHelper)
                }
            }
        }
    }
}

// Yeni Oyun ekranının UI tasarımı
@OptIn(ExperimentalMaterial3Api::class) // Henüz deneysel olan Material3 API'larını kullanıyoruz

@Composable
fun NewGameScreen(dbHelper: DatabaseHelper) {
    val context = LocalContext.current

    // Oyuncu listesini veritabanından yükle
    val playersList = remember {
        mutableStateOf(dbHelper.getAllPlayers())
    }

    // Dialog durumları
    var showNewPlayerDialog by remember { mutableStateOf(false) }
    var newPlayerName by remember { mutableStateOf("") }
    var dialogForPlayer by remember { mutableStateOf(1) } // Hangi oyuncu için dialog açıldığı

    // Seçilen oyuncular için durum - başlangıçta null
    var selectedPlayer1 by remember { mutableStateOf<Player?>(null) }
    var selectedPlayer2 by remember { mutableStateOf<Player?>(null) }

    // Son maçtaki oyuncuları varsayılan olarak seç
    LaunchedEffect(playersList.value) {
        if (playersList.value.isNotEmpty() && selectedPlayer1 == null && selectedPlayer2 == null) {
            // En son oynanan maçı al
            val lastMatches = dbHelper.getAllMatches()
            if (lastMatches.isNotEmpty()) {
                val lastMatch = lastMatches.first() // İlk eleman en son maç (DESC sıralı)

                // Son maçtaki oyuncuları bul
                val lastPlayer1 = playersList.value.find { it.id == lastMatch.player1Id }
                val lastPlayer2 = playersList.value.find { it.id == lastMatch.player2Id }

                // Oyuncular bulunduysa seç
                if (lastPlayer1 != null && lastPlayer2 != null) {
                    selectedPlayer1 = lastPlayer1
                    selectedPlayer2 = lastPlayer2
                }
            } else {
                // Eğer hiç maç yoksa, ilk iki oyuncuyu seç (mevcut davranış)
                if (playersList.value.isNotEmpty()) {
                    selectedPlayer1 = playersList.value[0]
                }
                if (playersList.value.size > 1) {
                    selectedPlayer2 = playersList.value[1]
                }
            }
        }
    }

    // Oyun türü ve el sayısı için durum
    var selectedGameType by remember { mutableStateOf("Modern") }

    var selectedRounds by remember { mutableStateOf("11") }
    var showRoundsMenu by remember { mutableStateOf(false) }

    // Skor giriş modu için durum (Otomatik=true, Manuel=false)
    var isScoreAutomatic by remember { mutableStateOf(true) }

    // El sayısı seçenekleri
    val roundsOptions = listOf("3", "5", "7", "9","11", "15", "17", "21")

    // Oyuncu 1 ve 2 dropdown için durum
    var showPlayer1Menu by remember { mutableStateOf(false) }
    var showPlayer2Menu by remember { mutableStateOf(false) }

    // Tavla türü değiştiğinde el sayısını güncelle
    LaunchedEffect(selectedGameType) {
        if (selectedGameType == "Geleneksel") {
            selectedRounds = "5"
        }
    }

    // Ana düzen - dikey bir sütun oluşturuyoruz
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp) // Öğeler arasında 16dp boşluk
    ) {
        // Başlık
        Text(
            text = "Yeni Oyun Ayarları",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )

        // Oyuncu Seçimi - Yan yana
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Oyuncu 1
            Box(modifier = Modifier.weight(1f)) {
                OutlinedTextField(
                    value = selectedPlayer1?.name ?: "",
                    onValueChange = { },
                    modifier = Modifier.fillMaxWidth(),
                    readOnly = true,
                    placeholder = { Text("Oyuncu 1 Seç") },
                    trailingIcon = {
                        IconButton(onClick = { showPlayer1Menu = true }) {
                            Icon(
                                imageVector = Icons.Default.ArrowDropDown,
                                contentDescription = "Dropdown"
                            )
                        }
                    }
                )

                DropdownMenu(
                    expanded = showPlayer1Menu,
                    onDismissRequest = { showPlayer1Menu = false },
                    modifier = Modifier.fillMaxWidth(0.9f)
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
                    DropdownMenuItem(
                        text = { Text("Yeni Oyuncu Adı Gir") },
                        onClick = {
                            dialogForPlayer = 1
                            showNewPlayerDialog = true
                            showPlayer1Menu = false
                        }
                    )
                }
            }

            // Oyuncu 2
            Box(modifier = Modifier.weight(1f)) {
                OutlinedTextField(
                    value = selectedPlayer2?.name ?: "",
                    onValueChange = { },
                    modifier = Modifier.fillMaxWidth(),
                    readOnly = true,
                    placeholder = { Text("Oyuncu 2 Seç") },
                    trailingIcon = {
                        IconButton(onClick = { showPlayer2Menu = true }) {
                            Icon(
                                imageVector = Icons.Default.ArrowDropDown,
                                contentDescription = "Dropdown"
                            )
                        }
                    }
                )

                DropdownMenu(
                    expanded = showPlayer2Menu,
                    onDismissRequest = { showPlayer2Menu = false },
                    modifier = Modifier.fillMaxWidth(0.9f)
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
                    DropdownMenuItem(
                        text = { Text("Yeni Oyuncu Adı Gir") },
                        onClick = {
                            dialogForPlayer = 2
                            showNewPlayerDialog = true
                            showPlayer2Menu = false
                        }
                    )
                }
            }
        }

        // Tüm Oyun Ayarları - Tek satırda 5 çerçeve
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.Top
        ) {
            // Tavla Türü Çerçevesi
            Card(
                modifier = Modifier
                    .weight(1f)
                    .height(120.dp),
                border = BorderStroke(1.dp, Color.Gray)
            ) {
                Column(
                    modifier = Modifier
                        .padding(8.dp)
                        .fillMaxHeight(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Başlık - Sabit pozisyon
                    Text(
                        text = "Tavla Türü",
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                    
                    // İçerik alanı - ortalanmış
                    Column(
                        modifier = Modifier.fillMaxHeight(),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.width(100.dp)
                        ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Start,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    selectedGameType = "Geleneksel"
                                    selectedRounds = "5"
                                }
                        ) {
                            RadioButton(
                                selected = selectedGameType == "Geleneksel",
                                onClick = {
                                    selectedGameType = "Geleneksel"
                                    selectedRounds = "5"
                                },
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = "Geleneksel",
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(start = 4.dp)
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(6.dp))
                        
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Start,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    selectedGameType = "Modern"
                                }
                        ) {
                            RadioButton(
                                selected = selectedGameType == "Modern",
                                onClick = { selectedGameType = "Modern" },
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = "Modern",
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(start = 4.dp)
                            )
                        }
                        }
                    }
                }
            }

            // El Sayısı Çerçevesi
            Card(
                modifier = Modifier
                    .weight(1f)
                    .height(120.dp),
                border = BorderStroke(1.dp, Color.Gray)
            ) {
                Column(
                    modifier = Modifier
                        .padding(8.dp)
                        .fillMaxHeight(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Başlık - Sabit pozisyon
                    Text(
                        text = "El Sayısı",
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                    
                    // İçerik alanı - ortalanmış
                    Column(
                        modifier = Modifier.fillMaxHeight(),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box {
                        OutlinedTextField(
                            value = selectedRounds,
                            onValueChange = { },
                            modifier = Modifier.width(90.dp),
                            readOnly = true,
                            textStyle = MaterialTheme.typography.bodyMedium.copy(
                                textAlign = TextAlign.Center
                            ),
                            trailingIcon = {
                                Icon(
                                    imageVector = Icons.Default.ArrowDropDown,
                                    contentDescription = "Dropdown",
                                    modifier = Modifier.clickable { showRoundsMenu = true }
                                )
                            }
                        )

                        DropdownMenu(
                            expanded = showRoundsMenu,
                            onDismissRequest = { showRoundsMenu = false }
                        ) {
                            roundsOptions.forEach { rounds ->
                                DropdownMenuItem(
                                    text = { Text(rounds) },
                                    onClick = {
                                        selectedRounds = rounds
                                        showRoundsMenu = false
                                    }
                                )
                            }
                        }
                    }
                    }
                }
            }

            // Skor Giriş Modu Çerçevesi
            Card(
                modifier = Modifier
                    .weight(1f)
                    .height(120.dp),
                border = BorderStroke(1.dp, Color.Gray)
            ) {
                Column(
                    modifier = Modifier
                        .padding(8.dp)
                        .fillMaxHeight(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Başlık - Sabit pozisyon
                    Text(
                        text = "Skor Modu",
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                    
                    // İçerik alanı - ortalanmış
                    Column(
                        modifier = Modifier.fillMaxHeight(),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                    
                    Text(
                        text = if (isScoreAutomatic) "Otomatik" else "Manuel",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Switch(
                        checked = isScoreAutomatic,
                        onCheckedChange = { isScoreAutomatic = it }
                    )
                    }
                }
            }

            // Zar Atıcı Çerçevesi
            Card(
                modifier = Modifier
                    .weight(1f)
                    .height(120.dp),
                border = BorderStroke(1.dp, Color.Gray)
            ) {
                Column(
                    modifier = Modifier
                        .padding(8.dp)
                        .fillMaxHeight(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Başlık - Sabit pozisyon
                    Text(
                        text = "Zar Atıcı Kullanımı",
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                    
                    // İçerik alanı - ortalanmış
                    Column(
                        modifier = Modifier.fillMaxHeight(),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                    
                    var useDiceRoller by remember { mutableStateOf(false) }
                    
                    Switch(
                        checked = useDiceRoller,
                        onCheckedChange = { useDiceRoller = it }
                    )
                    }
                }
            }
            
            // Süre Tutucu Çerçevesi
            Card(
                modifier = Modifier
                    .weight(1f)
                    .height(120.dp),
                border = BorderStroke(1.dp, Color.Gray)
            ) {
                Column(
                    modifier = Modifier
                        .padding(8.dp)
                        .fillMaxHeight(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Başlık - Sabit pozisyon
                    Text(
                        text = "Süre Tutucu Kullanımı",
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                    
                    // İçerik alanı - ortalanmış
                    Column(
                        modifier = Modifier.fillMaxHeight(),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                    
                    var useTimer by remember { mutableStateOf(false) }
                    
                    Switch(
                        checked = useTimer,
                        onCheckedChange = { useTimer = it }
                    )
                    }
                }
            }
        }

        // Boş alan ekleyerek butonları ekranın alt kısmına yakın konumlandırıyoruz
        Spacer(modifier = Modifier.weight(1f))

        // İşlem Butonları
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Button(
                onClick = {
                    // İptal et
                    (context as ComponentActivity).finish()
                },
                modifier = Modifier.weight(1f)
            ) {
                Text("İptal")
            }

            Button(
                onClick = {
                    // Gerekli kontroller
                    if (selectedPlayer1 == null || selectedPlayer2 == null) {
                        Toast.makeText(context, "Lütfen iki oyuncu seçin", Toast.LENGTH_SHORT).show()
                        return@Button
                    }

                    if (selectedPlayer1?.id == selectedPlayer2?.id) {
                        Toast.makeText(context, "Lütfen iki farklı oyuncu seçin", Toast.LENGTH_SHORT).show()
                        return@Button
                    }

                    // Oyun bilgilerini al
                    val player1Id = selectedPlayer1!!.id
                    val player2Id = selectedPlayer2!!.id
                    val player1Name = selectedPlayer1!!.name
                    val player2Name = selectedPlayer2!!.name
                    val rounds = selectedRounds.toInt()

                    // Skor ekranını başlat
                    val intent = Intent(context, GameScoreActivity::class.java).apply {
                        putExtra("player1_name", player1Name)
                        putExtra("player2_name", player2Name)
                        putExtra("game_type", selectedGameType)
                        putExtra("rounds", rounds)
                        putExtra("player1_id", player1Id)
                        putExtra("player2_id", player2Id)
                        putExtra("is_score_automatic", isScoreAutomatic)
                    }
                    context.startActivity(intent)
                },
                modifier = Modifier.weight(1f)
            ) {
                Text("Oyunu Başlat")
            }
        }

        // Yeni Oyuncu Dialog
        if (showNewPlayerDialog) {
            Dialog(onDismissRequest = { showNewPlayerDialog = false }) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            text = "Yeni Oyuncu Ekle",
                            style = MaterialTheme.typography.headlineSmall
                        )
                        
                        OutlinedTextField(
                            value = newPlayerName,
                            onValueChange = { newPlayerName = it },
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = { Text("Oyuncu adı girin") },
                            singleLine = true
                        )
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = { 
                                    showNewPlayerDialog = false
                                    newPlayerName = ""
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Vazgeç")
                            }
                            
                            Button(
                                onClick = {
                                    val name = newPlayerName.trim()
                                    if (name.isNotEmpty()) {
                                        val id = dbHelper.addPlayer(name)
                                        if (id != -1L) {
                                            val newPlayer = Player(id, name)
                                            val updatedList = playersList.value.toMutableList()
                                            updatedList.add(newPlayer)
                                            playersList.value = updatedList
                                            
                                            // Hangi oyuncu için dialog açıldıysa o oyuncuyu seç
                                            if (dialogForPlayer == 1) {
                                                selectedPlayer1 = newPlayer
                                            } else {
                                                selectedPlayer2 = newPlayer
                                            }
                                            
                                            showNewPlayerDialog = false
                                            newPlayerName = ""
                                            Toast.makeText(context, "Oyuncu eklendi", Toast.LENGTH_SHORT).show()
                                        } else {
                                            Toast.makeText(context, "Bu isimde bir oyuncu zaten var", Toast.LENGTH_SHORT).show()
                                        }
                                    } else {
                                        Toast.makeText(context, "Lütfen bir oyuncu adı girin", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Kaydet")
                            }
                        }
                    }
                }
            }
        }
    }
}