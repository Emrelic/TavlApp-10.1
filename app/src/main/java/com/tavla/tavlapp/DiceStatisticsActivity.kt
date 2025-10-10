package com.tavla.tavlapp

import android.os.Bundle
import android.view.View
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp


class DiceStatisticsActivity : ComponentActivity() {
    private lateinit var dbHelper: DatabaseHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Full screen - Landscape modda
        window.decorView.systemUiVisibility = (
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
                        View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or
                        View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
                        View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
                        View.SYSTEM_UI_FLAG_FULLSCREEN or
                        View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                )

        dbHelper = DatabaseHelper(this)

        val matchId = intent.getLongExtra("match_id", -1)
        val player1Id = intent.getLongExtra("player1_id", -1)
        val player2Id = intent.getLongExtra("player2_id", -1)
        val player1Name = intent.getStringExtra("player1_name") ?: "Oyuncu 1"
        val player2Name = intent.getStringExtra("player2_name") ?: "Oyuncu 2"

        setContent {
            MaterialTheme {
                DiceStatisticsScreen(
                    dbHelper = dbHelper,
                    matchId = matchId,
                    player1Id = player1Id,
                    player2Id = player2Id,
                    player1Name = player1Name,
                    player2Name = player2Name,
                    onClose = { finish() }
                )
            }
        }
    }
}

@Composable
fun DiceStatisticsScreen(
    dbHelper: DatabaseHelper,
    matchId: Long,
    player1Id: Long,
    player2Id: Long,
    player1Name: String,
    player2Name: String,
    onClose: () -> Unit
) {
    // İstatistikleri yükle
    val player1Stats = remember { dbHelper.getDiceStats(matchId, player1Id) }
    val player2Stats = remember { dbHelper.getDiceStats(matchId, player2Id) }

    val scrollState = rememberScrollState()

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color(0xFF1E1E1E)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // Başlık + Kapat butonu
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "ZAR İSTATİSTİKLERİ",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Button(
                    onClick = onClose,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFB71C1C))
                ) {
                    Text("KAPAT", fontSize = 14.sp)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Tablo
            Row(
                modifier = Modifier.fillMaxSize()
            ) {
                // Sol sütun: İstatistik başlıkları
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(scrollState)
                ) {
                    StatHeaderCell("İSTATİSTİK")
                    HorizontalDivider(color = Color.Gray, thickness = 2.dp)

                    // Atılan zar çeşitleri
                    StatSubHeaderCell("Atılan Zar Çeşidi")
                    DiceHelper.getAllDiceCombinations().forEach { combo ->
                        StatLabelCell(combo)
                    }

                    StatHeaderCell("Atılan Zar Kuvveti")
                    StatHeaderCell("Atılan Zar Paresi")
                    StatHeaderCell("Çift Atış Sayısı")
                    StatHeaderCell("Çift Atış Kuvveti")
                    StatHeaderCell("Oynanan Zar Kuvveti")
                    StatHeaderCell("Oynanan Zar Paresi")
                    StatHeaderCell("Kısmen Boşa Giden Zar Kuvveti")
                    StatHeaderCell("Kısmen Boşa Giden Zar Paresi")
                    StatHeaderCell("Bitiş Artığı Kuvveti")
                    StatHeaderCell("Bitiş Artığı Paresi")
                    StatHeaderCell("VERİMLİLİK %")
                }

                Spacer(modifier = Modifier.width(2.dp).fillMaxHeight().background(Color.Gray))

                // Orta sütun: Oyuncu 1
                Column(
                    modifier = Modifier
                        .weight(0.7f)
                        .verticalScroll(scrollState)
                ) {
                    StatPlayerHeaderCell(player1Name)
                    HorizontalDivider(color = Color.Gray, thickness = 2.dp)

                    // Atılan zar çeşitleri - Alt başlık cell'i ekle (A sütunuyla aynı yükseklikte)
                    StatSubHeaderCellValue("")
                    DiceHelper.getAllDiceCombinations().forEach { combo ->
                        StatValueCell(getDiceComboCount(player1Stats, combo))
                    }

                    StatValueCellHeader(player1Stats?.totalDicePower ?: 0)
                    StatValueCellHeader(player1Stats?.totalDicePieces ?: 0)
                    StatValueCellHeader(player1Stats?.doubleCount ?: 0)
                    StatValueCellHeader(player1Stats?.doublePower ?: 0)
                    StatValueCellHeader(player1Stats?.playedPower ?: 0)
                    StatValueCellHeader(player1Stats?.playedPieces ?: 0)
                    StatValueCellHeader(player1Stats?.partialWastedPower ?: 0)
                    StatValueCellHeader(player1Stats?.partialWastedPieces ?: 0)
                    StatValueCellHeader(player1Stats?.endWastePower ?: 0)
                    StatValueCellHeader(player1Stats?.endWastePieces ?: 0)
                    StatValueCell(calculateEfficiency(player1Stats), isPercentage = true)
                }

                Spacer(modifier = Modifier.width(2.dp).fillMaxHeight().background(Color.Gray))

                // Sağ sütun: Oyuncu 2
                Column(
                    modifier = Modifier
                        .weight(0.7f)
                        .verticalScroll(scrollState)
                ) {
                    StatPlayerHeaderCell(player2Name)
                    HorizontalDivider(color = Color.Gray, thickness = 2.dp)

                    // Atılan zar çeşitleri - Alt başlık cell'i ekle (A sütunuyla aynı yükseklikte)
                    StatSubHeaderCellValue("")
                    DiceHelper.getAllDiceCombinations().forEach { combo ->
                        StatValueCell(getDiceComboCount(player2Stats, combo))
                    }

                    StatValueCellHeader(player2Stats?.totalDicePower ?: 0)
                    StatValueCellHeader(player2Stats?.totalDicePieces ?: 0)
                    StatValueCellHeader(player2Stats?.doubleCount ?: 0)
                    StatValueCellHeader(player2Stats?.doublePower ?: 0)
                    StatValueCellHeader(player2Stats?.playedPower ?: 0)
                    StatValueCellHeader(player2Stats?.playedPieces ?: 0)
                    StatValueCellHeader(player2Stats?.partialWastedPower ?: 0)
                    StatValueCellHeader(player2Stats?.partialWastedPieces ?: 0)
                    StatValueCellHeader(player2Stats?.endWastePower ?: 0)
                    StatValueCellHeader(player2Stats?.endWastePieces ?: 0)
                    StatValueCell(calculateEfficiency(player2Stats), isPercentage = true)
                }
            }
        }
    }
}



@Composable
fun StatHeaderCell(text: String) {
    val context = androidx.compose.ui.platform.LocalContext.current
    
    val explanation = when (text) {
        "Atılan Zar Kuvveti" -> "Oyuncu tarafından atılan tüm zar değerlerinin toplamı (pip sayısı)"
        "Atılan Zar Paresi" -> "Oyuncu tarafından atılan toplam zar parça sayısı"
        "Çift Atış Sayısı" -> "Oyuncunun attığı çift zarların sayısı (1-1, 2-2, 3-3, vs.)"
        "Çift Atış Kuvveti" -> "Çift zarlarda elde edilen toplam kuvvet (pip sayısı)"
        "Oynanan Zar Kuvveti" -> "Gerçekten oyunda kullanılan zar değerlerinin toplamı"
        "Oynanan Zar Paresi" -> "Gerçekten oyunda kullanılan zar parçalarının sayısı"
        "Kısmen Boşa Giden Zar Kuvveti" -> "Kısmen oynanan zarlardan boşa giden kısım (kısmi kayp)"
        "Kısmen Boşa Giden Zar Paresi" -> "Kısmen oynanan zar parçalarının sayısı"
        "Bitiş Artığı Kuvveti" -> "Oyun sonunda kalan ve kullanılamayan zar değerleri"
        "Bitiş Artığı Paresi" -> "Oyun sonunda kalan ve kullanılamayan zar parçaları"
        "VERİMLİLİK %" -> "Oynanan zar kuvvetinin toplam zar kuvvetine oranı (yüzde olarak)"
        "Atılan Zar Çeşidi" -> "Oyuncunun attığı farklı zar kombinasyonlarının dağılımı"
        else -> ""
    }
    
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .background(Color(0xFF424242))
            .border(1.dp, Color.Gray)
            .clickable {
                if (explanation.isNotEmpty()) {
                    android.widget.Toast.makeText(context, explanation, android.widget.Toast.LENGTH_LONG).show()
                }
            },
        contentAlignment = Alignment.CenterStart
    ) {
        Text(
            text = text,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            modifier = Modifier.padding(horizontal = 8.dp)
        )
    }
}

@Composable
fun StatSubHeaderCell(text: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(40.dp)
            .background(Color(0xFF616161))
            .border(1.dp, Color.Gray),
        contentAlignment = Alignment.CenterStart
    ) {
        Text(
            text = text,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color.White,
            modifier = Modifier.padding(horizontal = 8.dp)
        )
    }
}

@Composable
fun StatLabelCell(text: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(36.dp)
            .background(Color(0xFF303030))
            .border(0.5.dp, Color.Gray),
        contentAlignment = Alignment.CenterStart
    ) {
        Text(
            text = "  $text",
            fontSize = 12.sp,
            color = Color.White,
            modifier = Modifier.padding(horizontal = 8.dp)
        )
    }
}

@Composable
fun StatPlayerHeaderCell(name: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .background(Color(0xFF1976D2))
            .border(1.dp, Color.Gray),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = name.uppercase(),
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun StatValueCell(value: Int, isPercentage: Boolean = false) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(if (isPercentage) 48.dp else 36.dp)
            .background(Color(0xFF212121))
            .border(0.5.dp, Color.Gray),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = if (isPercentage && value > 0) {
                "%.1f%%".format(value / 10.0)
            } else {
                value.toString()
            },
            fontSize = if (isPercentage) 16.sp else 14.sp,
            fontWeight = if (isPercentage) FontWeight.Bold else FontWeight.Normal,
            color = if (isPercentage) Color(0xFF4CAF50) else Color.White,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun StatSubHeaderCellValue(text: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(40.dp)
            .background(Color(0xFF212121))
            .border(0.5.dp, Color.Gray),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color.White
        )
    }
}

@Composable
fun StatValueCellHeader(value: Int) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .background(Color(0xFF212121))
            .border(0.5.dp, Color.Gray),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = value.toString(),
            fontSize = 14.sp,
            fontWeight = FontWeight.Normal,
            color = Color.White,
            textAlign = TextAlign.Center
        )
    }
}

/**
 * DiceStatistics objesinden zar kombinasyonu sayısını al
 */
fun getDiceComboCount(stats: DiceStatistics?, combo: String): Int {
    if (stats == null) return 0

    return when (combo) {
        "1-1" -> stats.dice_1_1
        "1-2" -> stats.dice_1_2
        "1-3" -> stats.dice_1_3
        "1-4" -> stats.dice_1_4
        "1-5" -> stats.dice_1_5
        "1-6" -> stats.dice_1_6
        "2-2" -> stats.dice_2_2
        "2-3" -> stats.dice_2_3
        "2-4" -> stats.dice_2_4
        "2-5" -> stats.dice_2_5
        "2-6" -> stats.dice_2_6
        "3-3" -> stats.dice_3_3
        "3-4" -> stats.dice_3_4
        "3-5" -> stats.dice_3_5
        "3-6" -> stats.dice_3_6
        "4-4" -> stats.dice_4_4
        "4-5" -> stats.dice_4_5
        "4-6" -> stats.dice_4_6
        "5-5" -> stats.dice_5_5
        "5-6" -> stats.dice_5_6
        "6-6" -> stats.dice_6_6
        else -> 0
    }
}

/**
 * Verimlilik hesapla: (Oynanan / Toplam) * 1000 (yüzde olarak döner, 1 decimal için)
 */
fun calculateEfficiency(stats: DiceStatistics?): Int {
    if (stats == null || stats.totalDicePower == 0) return 0
    return (stats.playedPower.toDouble() / stats.totalDicePower.toDouble() * 1000).toInt()
}
