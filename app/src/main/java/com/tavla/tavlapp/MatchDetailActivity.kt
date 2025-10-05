package com.tavla.tavlapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import java.text.SimpleDateFormat
import java.util.*
import com.tavla.tavlapp.Match

class MatchDetailActivity : ComponentActivity() {
    private lateinit var dbHelper: DatabaseHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val matchId = intent.getLongExtra("match_id", -1)
        dbHelper = DatabaseHelper(this)

        setContent {
            TavlaAppTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MatchDetailScreen(matchId, dbHelper) {
                        finish()
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MatchDetailScreen(matchId: Long, dbHelper: DatabaseHelper, onBack: () -> Unit) {
    // Maç detaylarını veritabanından çek
    val match = dbHelper.getMatchDetails(matchId)
    val rounds = dbHelper.getMatchRounds(matchId)

    // Oyuncu bilgilerini çek
    val players = dbHelper.getAllPlayers().associateBy { it.id }

    val player1 = players[match?.player1Id]
    val player2 = players[match?.player2Id]

    // Detaylı istatistikler
    val player1Stats = if (match != null) dbHelper.calculatePlayerMatchStats(rounds, match.player1Id) else PlayerRoundStats()
    val player2Stats = if (match != null) dbHelper.calculatePlayerMatchStats(rounds, match.player2Id) else PlayerRoundStats()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Maç Detayı") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, "Geri")
                    }
                }
            )
        }
    ) { paddingValues ->
        if (match == null) {
            // Maç bulunamadı durumu
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Text("Maç bulunamadı")
            }
        } else {
            // Maç detay görünümü
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp)
            ) {
                // Maç başlığı
                item {
                    MatchHeader(match, player1?.name ?: "Bilinmeyen", player2?.name ?: "Bilinmeyen")
                    Divider(modifier = Modifier.padding(vertical = 8.dp))
                }

                // Oyuncu 1 istatistikleri
                item {
                    PlayerMatchStats(player1?.name ?: "Bilinmeyen", player1Stats)
                    Spacer(modifier = Modifier.height(16.dp))
                }

                // Oyuncu 2 istatistikleri
                item {
                    PlayerMatchStats(player2?.name ?: "Bilinmeyen", player2Stats)
                    Spacer(modifier = Modifier.height(16.dp))
                }

                // El detayları başlığı
                item {
                    Text(
                        text = "Oyun Elleri",
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }

                // Her el için detay
                items(rounds) { round ->
                    RoundItem(round, players)
                    Divider(modifier = Modifier.padding(vertical = 4.dp))
                }
            }
        }
    }
}

@Composable
fun MatchHeader(match: Match, player1Name: String, player2Name: String) {
    Column(modifier = Modifier.fillMaxWidth()) {
        // Tarih formatını düzenle
        val date = try {
            val inputFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
            val outputFormat = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())
            val parsedDate = inputFormat.parse(match.date)
            outputFormat.format(parsedDate)
        } catch (e: Exception) {
            match.date
        }

        // Maç başlığı
        Text(
            text = "Maç #${match.id}",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 4.dp)
        )

        Text(
            text = date,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        Text(
            text = "${match.gameType} Tavla - ${match.totalRounds} El",
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        // Skor tablosu
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Oyuncu 1 skor
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = player1Name,
                    fontWeight = if (match.winnerId == match.player1Id) FontWeight.Bold else FontWeight.Normal
                )
                Text(
                    text = "${match.player1Score}",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = if (match.winnerId == match.player1Id) FontWeight.Bold else FontWeight.Normal
                )
                Text(text = "${match.player1RoundsWon} el")
            }

            // VS ayırıcı
            Text(
                text = "VS",
                modifier = Modifier
                    .padding(horizontal = 8.dp)
                    .align(Alignment.CenterVertically)
            )

            // Oyuncu 2 skor
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = player2Name,
                    fontWeight = if (match.winnerId == match.player2Id) FontWeight.Bold else FontWeight.Normal
                )
                Text(
                    text = "${match.player2Score}",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = if (match.winnerId == match.player2Id) FontWeight.Bold else FontWeight.Normal
                )
                Text(text = "${match.player2RoundsWon} el")
            }
        }

        // Kazanan bilgisi
        val winnerName = if (match.winnerId == match.player1Id) player1Name else player2Name
        Text(
            text = "Kazanan: $winnerName",
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
        )
    }
}

@Composable
fun PlayerMatchStats(playerName: String, stats: PlayerRoundStats) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "$playerName İstatistikleri",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(8.dp))

            // WinTypeUtils kullanarak - RoundStats için özel fonksiyon çağırın
            val winTypeMap = WinTypeUtils.roundStatsToWinTypeMap(stats)
            val sortedWinTypes = WinTypeUtils.sortWinTypes(winTypeMap.keys)

            Text("Toplam Kazanılan El: ${stats.totalWins}")

            if (sortedWinTypes.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))

                // 2 sütunlu grid düzeni oluştur - win type'ları iki kolonda göster
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Sol sütun - ilk yarı
                    Column(modifier = Modifier.weight(1f)) {
                        sortedWinTypes.take((sortedWinTypes.size + 1) / 2).forEach { type ->
                            val count = winTypeMap[type] ?: 0
                            Row(
                                modifier = Modifier
                                    .padding(vertical = 2.dp)
                                    .fillMaxWidth()
                            ) {
                                Text(
                                    text = type,
                                    modifier = Modifier.width(40.dp)
                                )
                                Text(
                                    text = count.toString(),
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    // Sağ sütun - ikinci yarı
                    if (sortedWinTypes.size > 1) {
                        Column(modifier = Modifier.weight(1f)) {
                            sortedWinTypes.drop((sortedWinTypes.size + 1) / 2).forEach { type ->
                                val count = winTypeMap[type] ?: 0
                                Row(
                                    modifier = Modifier
                                        .padding(vertical = 2.dp)
                                        .fillMaxWidth()
                                ) {
                                    Text(
                                        text = type,
                                        modifier = Modifier.width(40.dp)
                                    )
                                    Text(
                                        text = count.toString(),
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun RoundItem(round: Round, players: Map<Long, Player>) {
    val winnerName = players[round.winnerId]?.name ?: "Bilinmeyen"

    // WinTypeUtils kullanarak gösterim tipini al
    val displayType = WinTypeUtils.getDisplayType(round)
    val displayText = WinTypeUtils.typeToDisplayText(displayType)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // El numarası
        Text(
            text = "El ${round.roundNumber}",
            modifier = Modifier.width(80.dp)
        )

        // Kazanan oyuncu
        Text(
            text = winnerName,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.width(100.dp)
        )

        // Kazanma türü
        Text(
            text = displayType,
            modifier = Modifier.width(50.dp)
        )

        // Açıklama
        Text(
            text = displayText,
            modifier = Modifier.weight(1f)
        )

        // Puan
        Text(
            text = "+${round.score} puan",
            fontWeight = FontWeight.Bold
        )
    }
}
