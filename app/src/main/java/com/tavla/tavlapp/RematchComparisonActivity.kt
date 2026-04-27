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
    var allPartiesData by remember { mutableStateOf<List<PartyComparisonData>>(emptyList()) }

    LaunchedEffect(encounterId) {
        encounter = dbHelper.getRematchEncounter(encounterId)
        allPartiesData = dbHelper.getAllPartiesComparisonData(encounterId)
    }

    val enc = encounter ?: return
    if (allPartiesData.isEmpty()) return

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(8.dp)
    ) {
        // ===== UST PANEL: Karşılaşma Özeti + Buton =====
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF1E1E1E), RoundedCornerShape(6.dp))
                .padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Karşılaşma başlık
            Text(
                text = "${enc.player1Name} vs ${enc.player2Name}",
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = Color(0xFFCE93D8)
            )

            // Tüm partiler özeti
            val totalR1Won = allPartiesData.count { it.round1PartyResult?.winnerId != null }
            val totalR2Won = allPartiesData.count { it.round2PartyResult?.winnerId != null }
            
            Box(
                modifier = Modifier
                    .background(Color(0xFF1565C0).copy(alpha = 0.2f), RoundedCornerShape(4.dp))
                    .padding(horizontal = 8.dp, vertical = 3.dp)
            ) {
                Text(
                    text = "Tur 1: ${totalR1Won}/${enc.totalParties} parti",
                    fontSize = 12.sp,
                    color = Color(0xFF90CAF9),
                    fontWeight = FontWeight.Bold
                )
            }

            Box(
                modifier = Modifier
                    .background(Color(0xFFC62828).copy(alpha = 0.2f), RoundedCornerShape(4.dp))
                    .padding(horizontal = 8.dp, vertical = 3.dp)
            ) {
                Text(
                    text = "Tur 2: ${totalR2Won}/${enc.totalParties} parti",
                    fontSize = 12.sp,
                    color = Color(0xFFEF9A9A),
                    fontWeight = FontWeight.Bold
                )
            }

            // Durum
            Text(
                text = when (enc.status) {
                    RematchStatus.ACTIVE -> "DEVAM EDİYOR"
                    RematchStatus.COMPLETED -> "TAMAMLANDI"
                    else -> "BİLİNMEYEN"
                },
                fontSize = 10.sp,
                color = when (enc.status) {
                    RematchStatus.ACTIVE -> Color(0xFFFFAB40)
                    RematchStatus.COMPLETED -> Color(0xFF4CAF50)
                    else -> Color.Gray
                },
                fontWeight = FontWeight.Bold
            )

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

        // Tablo baslik - Kolon isimleri (parti, oyun, tur1, tur2)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF4A148C))
                .padding(vertical = 4.dp)
        ) {
            HeaderCell("Parti", 0.08f)
            HeaderCell("Oyun", 0.06f)
            
            // Tur 1 başlık
            Box(
                modifier = Modifier.weight(0.43f),
                contentAlignment = Alignment.Center
            ) {
                Text("TUR 1 - ${enc.player1Name}", color = Color(0xFF90CAF9), fontSize = 9.sp, fontWeight = FontWeight.Bold)
            }
            
            // Ayırıcı
            Box(modifier = Modifier.width(1.dp).background(Color.White.copy(alpha = 0.4f)))
            
            // Tur 2 başlık  
            Box(
                modifier = Modifier.weight(0.43f),
                contentAlignment = Alignment.Center
            ) {
                Text("TUR 2 - ${enc.player2Name}", color = Color(0xFFEF9A9A), fontSize = 9.sp, fontWeight = FontWeight.Bold)
            }
        }

        // Alt başlık satırı
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF311B92))
                .padding(vertical = 2.dp)
        ) {
            Box(modifier = Modifier.weight(0.08f)) // Parti boşluk
            Box(modifier = Modifier.weight(0.06f)) // Oyun boşluk
            
            // Tur 1 alt başlıklar
            HeaderCell("Kazanan", 0.12f)
            HeaderCell("Puan", 0.06f)
            HeaderCell("Küp", 0.08f)
            HeaderCell("Zar", 0.06f)
            HeaderCell("Çare", 0.06f)
            HeaderCell("Pip", 0.05f)
            
            Box(modifier = Modifier.width(1.dp)) // Ayırıcı
            
            // Tur 2 alt başlıklar  
            HeaderCell("Kazanan", 0.12f)
            HeaderCell("Puan", 0.06f)
            HeaderCell("Küp", 0.08f)
            HeaderCell("Zar", 0.06f)
            HeaderCell("Çare", 0.06f)
            HeaderCell("Pip", 0.05f)
        }

        // Tablo icerik - Tüm partilerin tüm oyunları
        LazyColumn(
            modifier = Modifier.fillMaxSize()
        ) {
            allPartiesData.forEachIndexed { partyIndex, partyData ->
                val allGameRows = mutableListOf<GameComparisonRow>()
                allGameRows.addAll(partyData.gameComparisons)
                
                items(allGameRows) { gameRow ->
                    AllPartiesComparisonRow(
                        partyIndex = partyIndex + 1,
                        gameRow = gameRow,
                        encounter = enc,
                        gameNumber = gameRow.setIndex + 1
                    )
                }
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
fun AllPartiesComparisonRow(
    partyIndex: Int,
    gameRow: GameComparisonRow,
    encounter: RematchEncounter,
    gameNumber: Int
) {
    val r1 = gameRow.round1Result
    val r2 = gameRow.round2Result

    val bgColor = when {
        r1 == null || r2 == null -> Color(0xFF212121)
        r1.winnerId == r2.winnerId -> Color(0xFF1B3D1B) // Koyu yeşil - aynı kazanan
        else -> Color(0xFF3D2E1B) // Koyu turuncu - farklı kazanan
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(bgColor)
            .border(0.5.dp, Color(0xFF424242))
            .padding(vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Parti numarası
        Text(
            text = "$partyIndex",
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            color = Color(0xFFCE93D8),
            modifier = Modifier.weight(0.08f),
            maxLines = 1
        )

        // Oyun numarası
        Text(
            text = "$gameNumber",
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            color = Color(0xFFB0BEC5),
            modifier = Modifier.weight(0.06f),
            maxLines = 1
        )

        // Tur 1 verileri
        CompactRoundCells(r1, encounter)

        // Ayırıcı
        Box(
            modifier = Modifier
                .width(1.dp)
                .height(16.dp)
                .background(Color(0xFF6A1B9A).copy(alpha = 0.5f))
        )

        // Tur 2 verileri
        CompactRoundCells(r2, encounter)
    }
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

@Composable
fun RowScope.CompactRoundCells(
    result: RematchGameResult?,
    encounter: RematchEncounter
) {
    if (result != null) {
        // Kazanan (kompakt)
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

        Text(
            text = if (winIcon.isNotEmpty()) "$shortName $winIcon" else shortName,
            fontSize = 9.sp,
            textAlign = TextAlign.Center,
            color = winColor,
            modifier = Modifier.weight(0.12f),
            maxLines = 1
        )

        // Puan
        Text(
            text = "${result.finalScore ?: "-"}",
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            color = Color(0xFF81C784),
            modifier = Modifier.weight(0.06f),
            maxLines = 1
        )

        // Küp bilgisi (kompakt)
        val cubeStr = if (result.cubeValue > 1) "x${result.cubeValue}" else "-"
        Text(
            text = cubeStr,
            fontSize = 8.sp,
            textAlign = TextAlign.Center,
            color = if (result.cubeValue > 1) Color(0xFFCE93D8) else Color(0xFF616161),
            modifier = Modifier.weight(0.08f),
            maxLines = 1
        )

        // Zar çiftleri
        Text(
            text = "${result.dicePairsUsed ?: "-"}",
            fontSize = 9.sp,
            textAlign = TextAlign.Center,
            color = Color(0xFFE0E0E0),
            modifier = Modifier.weight(0.06f),
            maxLines = 1
        )

        // Çare
        val totalDoubles = result.leftDoublesCount + result.rightDoublesCount
        Text(
            text = if (totalDoubles > 0) "$totalDoubles" else "-",
            fontSize = 9.sp,
            textAlign = TextAlign.Center,
            color = if (totalDoubles > 0) Color(0xFFFFAB40) else Color(0xFF616161),
            modifier = Modifier.weight(0.06f),
            maxLines = 1
        )

        // Pip
        Text(
            text = if ((result.loserPipCount ?: 0) > 0) "${result.loserPipCount}" else "-",
            fontSize = 9.sp,
            textAlign = TextAlign.Center,
            color = Color(0xFFE0E0E0),
            modifier = Modifier.weight(0.05f),
            maxLines = 1
        )
    } else {
        // Oynanmadı
        val emptyColor = Color(0xFF424242)
        Text("--", fontSize = 9.sp, textAlign = TextAlign.Center, color = emptyColor, modifier = Modifier.weight(0.12f), maxLines = 1)
        Text("--", fontSize = 9.sp, textAlign = TextAlign.Center, color = emptyColor, modifier = Modifier.weight(0.06f), maxLines = 1)
        Text("--", fontSize = 8.sp, textAlign = TextAlign.Center, color = emptyColor, modifier = Modifier.weight(0.08f), maxLines = 1)
        Text("--", fontSize = 9.sp, textAlign = TextAlign.Center, color = emptyColor, modifier = Modifier.weight(0.06f), maxLines = 1)
        Text("--", fontSize = 9.sp, textAlign = TextAlign.Center, color = emptyColor, modifier = Modifier.weight(0.06f), maxLines = 1)
        Text("--", fontSize = 9.sp, textAlign = TextAlign.Center, color = emptyColor, modifier = Modifier.weight(0.05f), maxLines = 1)
    }
}
