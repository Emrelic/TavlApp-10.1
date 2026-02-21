package com.tavla.tavlapp

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.content.Intent
import android.view.WindowManager
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat

/**
 * Rovansli Karsilasma karsilastirma analiz ekrani
 * Tam ekran tablo: scroll yok, weight-based kolon genislikleri
 */
class RematchComparisonActivity : ComponentActivity() {
    private lateinit var dbHelper: DatabaseHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Tam ekran modu
        WindowCompat.setDecorFitsSystemWindows(window, false)
        val controller = WindowInsetsControllerCompat(window, window.decorView)
        controller.hide(WindowInsetsCompat.Type.systemBars())
        controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

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
                    color = Color(0xFF121212)
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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(8.dp)
    ) {
        // ===== UST PANEL: Ozet + Buton (kompakt tek satir) =====
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF1E1E1E), RoundedCornerShape(6.dp))
                .padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Parti baslik
            Text(
                text = "Parti ${partyIndex + 1}",
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = Color(0xFFCE93D8)
            )

            // Tur 1 ozet
            Box(
                modifier = Modifier
                    .background(Color(0xFF1565C0).copy(alpha = 0.2f), RoundedCornerShape(4.dp))
                    .padding(horizontal = 8.dp, vertical = 3.dp)
            ) {
                if (data.round1PartyResult != null) {
                    val r1 = data.round1PartyResult!!
                    val w1 = if (r1.winnerId == enc.player1Id) enc.player1Name else enc.player2Name
                    Text(
                        text = "T1: $w1 ${r1.player1Score}-${r1.player2Score} (${r1.totalGamesPlayed}el)",
                        fontSize = 12.sp,
                        color = Color(0xFF90CAF9),
                        fontWeight = FontWeight.Bold
                    )
                } else {
                    Text("T1: --", fontSize = 12.sp, color = Color.Gray)
                }
            }

            // Tur 2 ozet
            Box(
                modifier = Modifier
                    .background(Color(0xFFC62828).copy(alpha = 0.2f), RoundedCornerShape(4.dp))
                    .padding(horizontal = 8.dp, vertical = 3.dp)
            ) {
                if (data.round2PartyResult != null) {
                    val r2 = data.round2PartyResult!!
                    val w2 = if (r2.winnerId == enc.player1Id) enc.player1Name else enc.player2Name
                    Text(
                        text = "T2: $w2 ${r2.player1Score}-${r2.player2Score} (${r2.totalGamesPlayed}el)",
                        fontSize = 12.sp,
                        color = Color(0xFFEF9A9A),
                        fontWeight = FontWeight.Bold
                    )
                } else {
                    Text("T2: --", fontSize = 12.sp, color = Color.Gray)
                }
            }

            // Istatistik kisaltmalari
            val bothPlayed = data.sameWinnerCount + data.differentWinnerCount
            if (bothPlayed > 0) {
                Text(
                    text = "Ayni:${data.sameWinnerCount} Fark:${data.differentWinnerCount}",
                    fontSize = 10.sp,
                    color = Color(0xFFB0BEC5)
                )
            }

            // Zar istatistikleri ozet
            Text(
                text = "Zar T1:${data.round1TotalDicePairs} T2:${data.round2TotalDicePairs}",
                fontSize = 10.sp,
                color = Color(0xFFB0BEC5)
            )
            if (data.round1TotalDoubles > 0 || data.round2TotalDoubles > 0) {
                Text(
                    text = "Care T1:${data.round1TotalDoubles} T2:${data.round2TotalDoubles}",
                    fontSize = 10.sp,
                    color = Color(0xFFFFAB40)
                )
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
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50)),
                shape = RoundedCornerShape(6.dp),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                modifier = Modifier.height(32.dp)
            ) {
                Text(
                    text = if (enc.status == RematchStatus.COMPLETED) "OZET" else "DEVAM",
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // ===== TABLO: Tam genislik, weight-based kolonlar =====

        // Tablo baslik - Tur etiketleri
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF311B92))
                .padding(vertical = 2.dp)
        ) {
            Box(modifier = Modifier.weight(W_NUM)) // # bosluk
            Box(
                modifier = Modifier.weight(W_ROUND_TOTAL),
                contentAlignment = Alignment.Center
            ) {
                Text("TUR 1 - ${enc.player1Name} (sol) / ${enc.player2Name} (sag)", color = Color(0xFF90CAF9), fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }
            Box(modifier = Modifier.width(2.dp))
            Box(
                modifier = Modifier.weight(W_ROUND_TOTAL),
                contentAlignment = Alignment.Center
            ) {
                Text("TUR 2 - ${enc.player2Name} (sol) / ${enc.player1Name} (sag)", color = Color(0xFFEF9A9A), fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }
        }

        // Tablo baslik - Kolon isimleri
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF4A148C))
                .padding(vertical = 4.dp)
        ) {
            HeaderCell("#", W_NUM)
            // Tur 1
            HeaderCell("Kazanan", W_WINNER)
            HeaderCell("Puan", W_SCORE)
            HeaderCell("Kup", W_CUBE)
            HeaderCell("El", W_DICE_CNT)
            HeaderCell("Care", W_DOUBLES)
            HeaderCell("Toplam", W_TOTAL)
            HeaderCell("Kvt", W_POWER)
            HeaderCell("Pip", W_PIP)
            // Ayirici
            Box(modifier = Modifier
                .width(2.dp)
                .height(16.dp)
                .background(Color.White.copy(alpha = 0.4f)))
            // Tur 2
            HeaderCell("Kazanan", W_WINNER)
            HeaderCell("Puan", W_SCORE)
            HeaderCell("Kup", W_CUBE)
            HeaderCell("El", W_DICE_CNT)
            HeaderCell("Care", W_DOUBLES)
            HeaderCell("Toplam", W_TOTAL)
            HeaderCell("Kvt", W_POWER)
            HeaderCell("Pip", W_PIP)
        }

        // Tablo icerik
        LazyColumn(
            modifier = Modifier.fillMaxSize()
        ) {
            items(data.gameComparisons) { row ->
                ComparisonRow(
                    row = row,
                    encounter = enc,
                    gameNumber = row.setIndex + 1
                )
            }
        }
    }
}

// ===== Kolon agirlik sabitleri =====
private const val W_NUM = 0.028f
private const val W_WINNER = 0.09f
private const val W_SCORE = 0.04f
private const val W_CUBE = 0.075f
private const val W_DICE_CNT = 0.035f
private const val W_DOUBLES = 0.04f
private const val W_TOTAL = 0.05f
private const val W_POWER = 0.045f
private const val W_PIP = 0.04f
private const val W_ROUND_TOTAL = W_WINNER + W_SCORE + W_CUBE + W_DICE_CNT + W_DOUBLES + W_TOTAL + W_POWER + W_PIP

@Composable
fun RowScope.HeaderCell(text: String, weight: Float) {
    Text(
        text = text,
        color = Color.White,
        fontWeight = FontWeight.Bold,
        fontSize = 10.sp,
        textAlign = TextAlign.Center,
        modifier = Modifier.weight(weight),
        maxLines = 1
    )
}

@Composable
fun ComparisonRow(
    row: GameComparisonRow,
    encounter: RematchEncounter,
    gameNumber: Int
) {
    val r1 = row.round1Result
    val r2 = row.round2Result

    val bgColor = when {
        r1 == null || r2 == null -> Color(0xFF212121)
        r1.winnerId == r2.winnerId -> Color(0xFF1B3D1B) // Koyu yesil - ayni kazanan
        else -> Color(0xFF3D2E1B) // Koyu turuncu - farkli kazanan
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(bgColor)
            .border(0.5.dp, Color(0xFF424242))
            .padding(vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Oyun numarasi
        Text(
            text = "$gameNumber",
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            color = Color(0xFFB0BEC5),
            modifier = Modifier.weight(W_NUM),
            maxLines = 1
        )

        // Tur 1 verileri
        RoundCells(r1, encounter)

        // Ayirici
        Box(
            modifier = Modifier
                .width(2.dp)
                .height(16.dp)
                .background(Color(0xFF6A1B9A).copy(alpha = 0.5f))
        )

        // Tur 2 verileri
        RoundCells(r2, encounter)
    }
}

@Composable
fun RowScope.RoundCells(
    result: RematchGameResult?,
    encounter: RematchEncounter
) {
    if (result != null) {
        // Kazanan
        val winnerName = when (result.winnerId) {
            encounter.player1Id -> encounter.player1Name
            encounter.player2Id -> encounter.player2Name
            else -> "?"
        }
        val shortName = if (winnerName.length > 6) winnerName.take(6) + "." else winnerName
        val winIcon = when (result.winType) {
            WinTypes.MARS -> "M"
            WinTypes.BACKGAMMON -> "B"
            WinTypes.RESIGN -> "P"
            else -> ""
        }
        val winColor = when (result.winType) {
            WinTypes.MARS -> Color(0xFFFF8A65)
            WinTypes.BACKGAMMON -> Color(0xFFFF5252)
            WinTypes.RESIGN -> Color(0xFFFFD54F)
            else -> Color(0xFFE0E0E0)
        }

        // Kazanan + tip
        Text(
            text = if (winIcon.isNotEmpty()) "$shortName $winIcon" else shortName,
            fontSize = 10.sp,
            textAlign = TextAlign.Center,
            color = winColor,
            modifier = Modifier.weight(W_WINNER),
            maxLines = 1
        )

        // Puan
        Text(
            text = "${result.finalScore ?: "-"}",
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            color = Color(0xFF81C784),
            modifier = Modifier.weight(W_SCORE),
            maxLines = 1
        )

        // Küp bilgisi
        val cubeStr = if (result.cubeValue > 1) {
            val doublerShort = when (result.doublerPlayerId) {
                encounter.player1Id -> encounter.player1Name.take(3)
                encounter.player2Id -> encounter.player2Name.take(3)
                else -> ""
            }
            if (result.winType == WinTypes.RESIGN) {
                "x${result.cubeValue} ${doublerShort}\u2192P"
            } else {
                "x${result.cubeValue} ${doublerShort}\u2192K"
            }
        } else {
            "-"
        }
        Text(
            text = cubeStr,
            fontSize = 9.sp,
            textAlign = TextAlign.Center,
            color = if (result.cubeValue > 1) Color(0xFFCE93D8) else Color(0xFF616161),
            modifier = Modifier.weight(W_CUBE),
            maxLines = 1
        )

        // El (zar çifti sayısı)
        Text(
            text = "${result.dicePairsUsed ?: "-"}",
            fontSize = 10.sp,
            textAlign = TextAlign.Center,
            color = Color(0xFFE0E0E0),
            modifier = Modifier.weight(W_DICE_CNT),
            maxLines = 1
        )

        // Çare (çift/double sayısı)
        val totalDoubles = result.leftDoublesCount + result.rightDoublesCount
        Text(
            text = if (totalDoubles > 0) "$totalDoubles" else "-",
            fontSize = 10.sp,
            textAlign = TextAlign.Center,
            color = if (totalDoubles > 0) Color(0xFFFFAB40) else Color(0xFF616161),
            fontWeight = if (totalDoubles > 0) FontWeight.Bold else FontWeight.Normal,
            modifier = Modifier.weight(W_DOUBLES),
            maxLines = 1
        )

        // Toplam zar birimi
        val totalDice = result.leftDiceTotal + result.rightDiceTotal
        Text(
            text = if (totalDice > 0) "$totalDice" else "-",
            fontSize = 10.sp,
            textAlign = TextAlign.Center,
            color = Color(0xFFE0E0E0),
            modifier = Modifier.weight(W_TOTAL),
            maxLines = 1
        )

        // Zar kuvveti (toplam / kullanılan çift sayısı)
        val dicePower = if ((result.dicePairsUsed ?: 0) > 0 && totalDice > 0) {
            String.format("%.1f", totalDice.toFloat() / (result.dicePairsUsed ?: 1))
        } else "-"
        Text(
            text = dicePower,
            fontSize = 10.sp,
            textAlign = TextAlign.Center,
            color = Color(0xFF80CBC4),
            modifier = Modifier.weight(W_POWER),
            maxLines = 1
        )

        // Pip
        Text(
            text = if ((result.loserPipCount ?: 0) > 0) "${result.loserPipCount}" else "-",
            fontSize = 10.sp,
            textAlign = TextAlign.Center,
            color = Color(0xFFE0E0E0),
            modifier = Modifier.weight(W_PIP),
            maxLines = 1
        )
    } else {
        // Oynanmadi
        val emptyColor = Color(0xFF424242)
        Text("--", fontSize = 10.sp, textAlign = TextAlign.Center, color = emptyColor, modifier = Modifier.weight(W_WINNER), maxLines = 1)
        Text("--", fontSize = 10.sp, textAlign = TextAlign.Center, color = emptyColor, modifier = Modifier.weight(W_SCORE), maxLines = 1)
        Text("--", fontSize = 9.sp, textAlign = TextAlign.Center, color = emptyColor, modifier = Modifier.weight(W_CUBE), maxLines = 1)
        Text("--", fontSize = 10.sp, textAlign = TextAlign.Center, color = emptyColor, modifier = Modifier.weight(W_DICE_CNT), maxLines = 1)
        Text("--", fontSize = 10.sp, textAlign = TextAlign.Center, color = emptyColor, modifier = Modifier.weight(W_DOUBLES), maxLines = 1)
        Text("--", fontSize = 10.sp, textAlign = TextAlign.Center, color = emptyColor, modifier = Modifier.weight(W_TOTAL), maxLines = 1)
        Text("--", fontSize = 10.sp, textAlign = TextAlign.Center, color = emptyColor, modifier = Modifier.weight(W_POWER), maxLines = 1)
        Text("--", fontSize = 10.sp, textAlign = TextAlign.Center, color = emptyColor, modifier = Modifier.weight(W_PIP), maxLines = 1)
    }
}
