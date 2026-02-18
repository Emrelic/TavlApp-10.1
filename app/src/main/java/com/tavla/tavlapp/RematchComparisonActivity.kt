package com.tavla.tavlapp

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
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

/**
 * Rovansli Karsilasma karsilastirma analiz ekrani
 * Ayni zar seti ile oynanan Tur 1 ve Tur 2 sonuclarini yan yana gosterir
 */
class RematchComparisonActivity : ComponentActivity() {
    private lateinit var dbHelper: DatabaseHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        dbHelper = DatabaseHelper(this)

        val encounterId = intent.getLongExtra("encounter_id", -1L)
        val partyIndex = intent.getIntExtra("party_index", 0)

        if (encounterId == -1L) {
            Toast.makeText(this, "Karsilasma bulunamadi", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        setContent {
            TavlaAppTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    RematchComparisonScreen(dbHelper, encounterId, partyIndex)
                }
            }
        }
    }
}

@Composable
fun RematchComparisonScreen(
    dbHelper: DatabaseHelper,
    encounterId: Long,
    partyIndex: Int
) {
    val context = LocalContext.current

    var encounter by remember { mutableStateOf<RematchEncounter?>(null) }
    var comparisonData by remember { mutableStateOf<PartyComparisonData?>(null) }

    LaunchedEffect(encounterId, partyIndex) {
        encounter = dbHelper.getRematchEncounter(encounterId)
        comparisonData = dbHelper.getPartyComparisonData(encounterId, partyIndex)
    }

    val enc = encounter ?: return
    val data = comparisonData ?: return

    Row(
        modifier = Modifier
            .fillMaxSize()
            .padding(12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // SOL PANEL - Ozet
        Column(
            modifier = Modifier
                .weight(0.38f)
                .fillMaxHeight(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Baslik
            Text(
                text = "Parti ${partyIndex + 1} - Karsilastirma",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF6A1B9A)
            )

            // Tur 1 ozet karti
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFE3F2FD))
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text("Tur 1", fontWeight = FontWeight.Bold, color = Color(0xFF1565C0), fontSize = 14.sp)
                    if (data.round1PartyResult != null) {
                        val r1 = data.round1PartyResult!!
                        val winnerName = if (r1.winnerId == enc.player1Id) enc.player1Name else enc.player2Name
                        Text(
                            text = "$winnerName kazandi",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                        Text(
                            text = "${r1.player1Score} - ${r1.player2Score} (${r1.totalGamesPlayed} oyun)",
                            fontSize = 14.sp,
                            color = Color.DarkGray
                        )
                    } else {
                        Text("Henuz oynanmadi", color = Color.Gray, fontSize = 13.sp)
                    }
                }
            }

            // Tur 2 ozet karti
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFFEBEE))
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text("Tur 2 (Rovans)", fontWeight = FontWeight.Bold, color = Color(0xFFC62828), fontSize = 14.sp)
                    if (data.round2PartyResult != null) {
                        val r2 = data.round2PartyResult!!
                        val winnerName = if (r2.winnerId == enc.player1Id) enc.player1Name else enc.player2Name
                        Text(
                            text = "$winnerName kazandi",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                        Text(
                            text = "${r2.player1Score} - ${r2.player2Score} (${r2.totalGamesPlayed} oyun)",
                            fontSize = 14.sp,
                            color = Color.DarkGray
                        )
                    } else {
                        Text("Henuz oynanmadi", color = Color.Gray, fontSize = 13.sp)
                    }
                }
            }

            // Istatistik karti
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFF3E5F5))
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text("Istatistikler", fontWeight = FontWeight.Bold, color = Color(0xFF6A1B9A), fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(6.dp))

                    // Ayni/farkli kazanan
                    val bothPlayedCount = data.sameWinnerCount + data.differentWinnerCount
                    if (bothPlayedCount > 0) {
                        StatRow("Ayni kazanan", "${data.sameWinnerCount}/$bothPlayedCount")
                        StatRow("Farkli kazanan", "${data.differentWinnerCount}/$bothPlayedCount")
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // Zar kullanimi
                    StatRow("Tur 1 toplam zar", "${data.round1TotalDicePairs}")
                    StatRow("Tur 2 toplam zar", "${data.round2TotalDicePairs}")

                    Spacer(modifier = Modifier.height(4.dp))

                    // Mars/Backgammon
                    if (data.round1MarsCount > 0 || data.round2MarsCount > 0) {
                        StatRow("Mars", "T1: ${data.round1MarsCount} / T2: ${data.round2MarsCount}")
                    }
                    if (data.round1BackgammonCount > 0 || data.round2BackgammonCount > 0) {
                        StatRow("Backgammon", "T1: ${data.round1BackgammonCount} / T2: ${data.round2BackgammonCount}")
                    }

                    // Kup
                    if (data.round1MaxCube > 1 || data.round2MaxCube > 1) {
                        StatRow("Max kup", "T1: ${data.round1MaxCube} / T2: ${data.round2MaxCube}")
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // Devam et butonu
            Button(
                onClick = {
                    if (enc.status == RematchStatus.COMPLETED) {
                        val intent = Intent(context, RematchProgressActivity::class.java)
                        context.startActivity(intent)
                    } else {
                        val intent = Intent(context, RematchDiceDisplayActivity::class.java)
                        intent.putExtra("encounter_id", encounterId)
                        context.startActivity(intent)
                    }
                    (context as? ComponentActivity)?.finish()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50)),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = if (enc.status == RematchStatus.COMPLETED) "KARSILASMA OZETI" else "DEVAM ET",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }
        }

        // SAG PANEL - Oyun bazinda karsilastirma tablosu
        Column(
            modifier = Modifier
                .weight(0.62f)
                .fillMaxHeight()
        ) {
            Text(
                text = "Oyun Bazinda Karsilastirma",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            // Tablo baslik satiri
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF6A1B9A))
                    .padding(vertical = 6.dp, horizontal = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                TableHeaderCell("#", 30.dp)
                // Tur 1
                TableHeaderCell("Kazanan", 80.dp)
                TableHeaderCell("Zar", 40.dp)
                TableHeaderCell("Pip", 40.dp)
                TableHeaderCell("Kup", 35.dp)
                // Ayirici
                Box(
                    modifier = Modifier
                        .width(2.dp)
                        .height(20.dp)
                        .background(Color.White.copy(alpha = 0.5f))
                )
                // Tur 2
                TableHeaderCell("Kazanan", 80.dp)
                TableHeaderCell("Zar", 40.dp)
                TableHeaderCell("Pip", 40.dp)
                TableHeaderCell("Kup", 35.dp)
            }

            // Tablo icerik
            LazyColumn(
                modifier = Modifier.fillMaxSize()
            ) {
                items(data.gameComparisons) { row ->
                    ComparisonTableRow(
                        row = row,
                        encounter = enc,
                        gameNumber = row.setIndex + 1
                    )
                }
            }
        }
    }
}

@Composable
fun StatRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, fontSize = 12.sp, color = Color.DarkGray)
        Text(text = value, fontSize = 12.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun TableHeaderCell(text: String, width: androidx.compose.ui.unit.Dp) {
    Text(
        text = text,
        color = Color.White,
        fontWeight = FontWeight.Bold,
        fontSize = 11.sp,
        textAlign = TextAlign.Center,
        modifier = Modifier.width(width)
    )
}

@Composable
fun ComparisonTableRow(
    row: GameComparisonRow,
    encounter: RematchEncounter,
    gameNumber: Int
) {
    val r1 = row.round1Result
    val r2 = row.round2Result

    // Renk kodlamasi
    val bgColor = when {
        r1 == null || r2 == null -> Color(0xFFF5F5F5) // Gri - tek turda oynandi
        r1.winnerId == r2.winnerId -> Color(0xFFE8F5E9) // Yesil - ayni kazanan
        else -> Color(0xFFFFF3E0) // Turuncu - farkli kazanan
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(bgColor)
            .border(0.5.dp, Color.LightGray)
            .padding(vertical = 4.dp, horizontal = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Oyun numarasi
        Text(
            text = "$gameNumber",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            modifier = Modifier.width(30.dp)
        )

        // Tur 1 verileri
        GameResultCells(r1, encounter)

        // Ayirici
        Box(
            modifier = Modifier
                .width(2.dp)
                .height(20.dp)
                .background(Color(0xFF6A1B9A).copy(alpha = 0.3f))
        )

        // Tur 2 verileri
        GameResultCells(r2, encounter)
    }
}

@Composable
fun RowScope.GameResultCells(
    result: RematchGameResult?,
    encounter: RematchEncounter
) {
    if (result != null) {
        val winnerName = when (result.winnerId) {
            encounter.player1Id -> encounter.player1Name
            encounter.player2Id -> encounter.player2Name
            else -> "?"
        }
        val shortName = if (winnerName.length > 8) winnerName.take(8) + ".." else winnerName
        val winTypeStr = when (result.winType) {
            WinTypes.MARS -> " (M)"
            WinTypes.BACKGAMMON -> " (B)"
            WinTypes.RESIGN -> " (P)"
            else -> ""
        }

        Text(
            text = "$shortName$winTypeStr",
            fontSize = 10.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.width(80.dp)
        )
        Text(
            text = "${result.dicePairsUsed ?: "-"}",
            fontSize = 11.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.width(40.dp)
        )
        Text(
            text = "${result.loserPipCount ?: "-"}",
            fontSize = 11.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.width(40.dp)
        )
        Text(
            text = if (result.cubeValue > 1) "${result.cubeValue}" else "1",
            fontSize = 11.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.width(35.dp)
        )
    } else {
        // Bu turda oynanmadi
        Text("--", fontSize = 11.sp, textAlign = TextAlign.Center, color = Color.Gray, modifier = Modifier.width(80.dp))
        Text("--", fontSize = 11.sp, textAlign = TextAlign.Center, color = Color.Gray, modifier = Modifier.width(40.dp))
        Text("--", fontSize = 11.sp, textAlign = TextAlign.Center, color = Color.Gray, modifier = Modifier.width(40.dp))
        Text("--", fontSize = 11.sp, textAlign = TextAlign.Center, color = Color.Gray, modifier = Modifier.width(35.dp))
    }
}
