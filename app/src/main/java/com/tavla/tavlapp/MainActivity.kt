package com.tavla.tavlapp

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tavla.tavlapp.ui.online.OnlineLobbyActivity

class MainActivity : ComponentActivity() {
    private lateinit var dbHelper: DatabaseHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        dbHelper = DatabaseHelper(this)
        val players = dbHelper.getAllPlayers()

        if (players.isEmpty()) {
            dbHelper.addPlayer("Oyuncu 1")
            dbHelper.addPlayer("Oyuncu 2")
        }

        dbHelper.addActivityLog(
            actionType = ActionTypes.APP_OPEN,
            description = "Uygulama acildi"
        )

        setContent {
            TavlaAppTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = AppColors.BgDark
                ) {
                    MainScreen()
                }
            }
        }
    }
}

// Zar noktalarini cizen yardimci fonksiyon
@Composable
fun DiceView(value: Int, diceSize: Int = 56, dotColor: Color = Color.White, bgColor: Color = Color(0xFFC62828)) {
    val sizeDp = diceSize.dp
    Canvas(modifier = Modifier.size(sizeDp)) {
        val s = size.width
        val r = s * 0.15f
        val dotR = s * 0.08f

        // Golge
        drawRoundRect(
            color = Color.Black.copy(alpha = 0.3f),
            cornerRadius = CornerRadius(r, r),
            topLeft = Offset(2f, 3f),
            size = Size(s, s)
        )
        // Zar arka plani
        drawRoundRect(
            brush = Brush.verticalGradient(
                colors = listOf(bgColor, bgColor.copy(alpha = 0.7f))
            ),
            cornerRadius = CornerRadius(r, r),
            size = Size(s, s)
        )
        // Parlaklik
        drawRoundRect(
            brush = Brush.verticalGradient(
                colors = listOf(Color.White.copy(alpha = 0.2f), Color.Transparent),
                startY = 0f,
                endY = s * 0.45f
            ),
            cornerRadius = CornerRadius(r, r),
            size = Size(s, s)
        )

        val p1 = s * 0.26f
        val p2 = s * 0.50f
        val p3 = s * 0.74f

        fun dot(x: Float, y: Float) {
            drawCircle(color = Color.Black.copy(alpha = 0.15f), radius = dotR, center = Offset(x + 1f, y + 1f))
            drawCircle(color = dotColor, radius = dotR, center = Offset(x, y))
        }

        when (value) {
            1 -> { dot(p2, p2) }
            2 -> { dot(p1, p3); dot(p3, p1) }
            3 -> { dot(p1, p3); dot(p2, p2); dot(p3, p1) }
            4 -> { dot(p1, p1); dot(p3, p1); dot(p1, p3); dot(p3, p3) }
            5 -> { dot(p1, p1); dot(p3, p1); dot(p2, p2); dot(p1, p3); dot(p3, p3) }
            6 -> { dot(p1, p1); dot(p1, p2); dot(p1, p3); dot(p3, p1); dot(p3, p2); dot(p3, p3) }
        }
    }
}

// Tavla tahtasi gorseli
@Composable
fun BackgammonBoardVisual() {
    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(170.dp)
            .padding(horizontal = 12.dp)
    ) {
        val w = size.width
        val h = size.height
        val boardPadding = 4f
        val boardW = w - boardPadding * 2
        val boardH = h - boardPadding * 2
        val barWidth = boardW * 0.045f
        val frameWidth = 10f
        val sideW = (boardW - barWidth) / 2f

        // Dis golge
        drawRoundRect(
            color = Color.Black.copy(alpha = 0.4f),
            cornerRadius = CornerRadius(14f, 14f),
            topLeft = Offset(boardPadding + 3f, boardPadding + 3f),
            size = Size(boardW, boardH)
        )

        // Dis cerceve - koyu ahsap tonu (tema uyumlu)
        drawRoundRect(
            brush = Brush.verticalGradient(
                colors = listOf(Color(0xFF2A1F3D), Color(0xFF1E1533))
            ),
            cornerRadius = CornerRadius(14f, 14f),
            topLeft = Offset(boardPadding, boardPadding),
            size = Size(boardW, boardH)
        )

        // Cerceve ic kenari - ince altin cizgi
        drawRoundRect(
            color = Color(0xFF8B7355).copy(alpha = 0.4f),
            cornerRadius = CornerRadius(12f, 12f),
            topLeft = Offset(boardPadding + 2f, boardPadding + 2f),
            size = Size(boardW - 4f, boardH - 4f),
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.5f)
        )

        // Oyun alani arka plani - koyu lacivert
        val playLeft = boardPadding + frameWidth
        val playTop = boardPadding + frameWidth
        val playW = boardW - frameWidth * 2
        val playH = boardH - frameWidth * 2

        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(Color(0xFF0F1B2E), Color(0xFF132240))
            ),
            topLeft = Offset(playLeft, playTop),
            size = Size(playW, playH)
        )

        // Orta bar - ahsap tonu
        val barX = boardPadding + sideW
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(Color(0xFF2A1F3D), Color(0xFF1E1533))
            ),
            topLeft = Offset(barX, boardPadding),
            size = Size(barWidth, boardH)
        )
        // Bar ic cizgi
        drawLine(
            color = Color(0xFF8B7355).copy(alpha = 0.3f),
            start = Offset(barX + barWidth / 2, boardPadding + frameWidth),
            end = Offset(barX + barWidth / 2, boardPadding + boardH - frameWidth),
            strokeWidth = 1f
        )

        // Ucgen boyutlari
        val triAreaW = sideW - frameWidth
        val triW = triAreaW / 6f
        val triH = (playH) * 0.42f

        // Ucgen renkleri - tema uyumlu soft tonlar
        val triColor1 = Color(0xFFBFA47A) // altin/bej
        val triColor2 = Color(0xFF2D4A6F) // koyu mavi

        // Ust ucgenler - sol taraf
        for (i in 0 until 6) {
            val startX = playLeft + i * triW
            val color = if (i % 2 == 0) triColor1 else triColor2
            val path = Path().apply {
                moveTo(startX, playTop)
                lineTo(startX + triW, playTop)
                lineTo(startX + triW / 2, playTop + triH)
                close()
            }
            drawPath(path, color)
            // Ucgen kenari
            drawPath(path, Color.Black.copy(alpha = 0.15f),
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 0.5f))
        }
        // Ust ucgenler - sag taraf
        for (i in 0 until 6) {
            val startX = barX + barWidth + i * triW
            val color = if (i % 2 == 0) triColor2 else triColor1
            val path = Path().apply {
                moveTo(startX, playTop)
                lineTo(startX + triW, playTop)
                lineTo(startX + triW / 2, playTop + triH)
                close()
            }
            drawPath(path, color)
            drawPath(path, Color.Black.copy(alpha = 0.15f),
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 0.5f))
        }

        // Alt ucgenler - sol taraf
        val bottomY = playTop + playH
        for (i in 0 until 6) {
            val startX = playLeft + i * triW
            val color = if (i % 2 == 0) triColor2 else triColor1
            val path = Path().apply {
                moveTo(startX, bottomY)
                lineTo(startX + triW, bottomY)
                lineTo(startX + triW / 2, bottomY - triH)
                close()
            }
            drawPath(path, color)
            drawPath(path, Color.Black.copy(alpha = 0.15f),
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 0.5f))
        }
        // Alt ucgenler - sag taraf
        for (i in 0 until 6) {
            val startX = barX + barWidth + i * triW
            val color = if (i % 2 == 0) triColor1 else triColor2
            val path = Path().apply {
                moveTo(startX, bottomY)
                lineTo(startX + triW, bottomY)
                lineTo(startX + triW / 2, bottomY - triH)
                close()
            }
            drawPath(path, color)
            drawPath(path, Color.Black.copy(alpha = 0.15f),
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 0.5f))
        }

        // Pullar
        val pulR = triW * 0.32f
        val pulLight = Color(0xFFE8D5B7) // GoldLight - acik pul
        val pulLightInner = Color(0xFFD4C0A0)
        val pulDark = Color(0xFF1A2744) // BgCard - koyu pul
        val pulDarkInner = Color(0xFF253555)
        val pulStroke = Color(0xFF8B7355).copy(alpha = 0.5f)

        fun drawPul(cx: Float, cy: Float, isLight: Boolean) {
            val outerColor = if (isLight) pulLight else pulDark
            val innerColor = if (isLight) pulLightInner else pulDarkInner
            // Golge
            drawCircle(color = Color.Black.copy(alpha = 0.2f), radius = pulR, center = Offset(cx + 1f, cy + 1.5f))
            // Dis halka
            drawCircle(color = outerColor, radius = pulR, center = Offset(cx, cy))
            // Ic daire (3d efekt)
            drawCircle(color = innerColor, radius = pulR * 0.78f, center = Offset(cx, cy))
            // Parlaklik
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color.White.copy(alpha = if (isLight) 0.25f else 0.1f), Color.Transparent),
                    center = Offset(cx - pulR * 0.2f, cy - pulR * 0.2f),
                    radius = pulR * 0.6f
                ),
                radius = pulR * 0.7f,
                center = Offset(cx, cy)
            )
            // Cerceve
            drawCircle(
                color = pulStroke,
                radius = pulR,
                center = Offset(cx, cy),
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1f)
            )
        }

        // Ust sol 6. ucgen - 5 acik pul
        for (j in 0 until 5) {
            drawPul(
                playLeft + 5 * triW + triW / 2,
                playTop + pulR + j * (pulR * 1.9f),
                isLight = true
            )
        }

        // Alt sag 6. ucgen - 5 koyu pul
        for (j in 0 until 5) {
            drawPul(
                barX + barWidth + 5 * triW + triW / 2,
                bottomY - pulR - j * (pulR * 1.9f),
                isLight = false
            )
        }

        // Ust sag 1. ucgen - 3 koyu pul
        for (j in 0 until 3) {
            drawPul(
                barX + barWidth + triW / 2,
                playTop + pulR + j * (pulR * 1.9f),
                isLight = false
            )
        }

        // Alt sol 1. ucgen - 3 acik pul
        for (j in 0 until 3) {
            drawPul(
                playLeft + triW / 2,
                bottomY - pulR - j * (pulR * 1.9f),
                isLight = true
            )
        }

        // Ust sol 4. ucgen - 2 koyu pul
        for (j in 0 until 2) {
            drawPul(
                playLeft + 3 * triW + triW / 2,
                playTop + pulR + j * (pulR * 1.9f),
                isLight = false
            )
        }

        // Alt sag 4. ucgen - 2 acik pul
        for (j in 0 until 2) {
            drawPul(
                barX + barWidth + 3 * triW + triW / 2,
                bottomY - pulR - j * (pulR * 1.9f),
                isLight = true
            )
        }

        // Orta bar uzerinde dekoratif altin nokta
        val barCenterX = barX + barWidth / 2
        drawCircle(
            color = Color(0xFFBFA47A).copy(alpha = 0.3f),
            radius = barWidth * 0.8f,
            center = Offset(barCenterX, boardPadding + boardH / 2)
        )
    }
}

@Composable
fun MenuButton(
    text: String,
    containerColor: Color,
    modifier: Modifier = Modifier,
    textColor: Color = Color.White,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = modifier
            .height(50.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = text,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = textColor,
                letterSpacing = 0.8.sp
            )
        }
    }
}

@Composable
fun MainScreen() {
    val context = LocalContext.current
    val dbHelper = remember { DatabaseHelper(context) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF0A1628),
                        Color(0xFF16213E),
                        Color(0xFF1A2744)
                    )
                )
            )
            .padding(horizontal = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // === UST YARI: Logo + Zarlar + Tahta ===
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Baslik: Zar + TavlApp + Zar
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth()
            ) {
                DiceView(value = 5, diceSize = 44, bgColor = Color(0xFF8B3A3A))

                Spacer(modifier = Modifier.width(14.dp))

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "TavlApp",
                        fontSize = 34.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = AppColors.GoldLight,
                        fontFamily = FontFamily.Serif,
                        letterSpacing = 2.sp
                    )
                    Text(
                        text = "Tavla Skor Takibi",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Normal,
                        color = AppColors.GoldDark,
                        letterSpacing = 3.sp
                    )
                }

                Spacer(modifier = Modifier.width(14.dp))

                DiceView(value = 6, diceSize = 44, bgColor = Color(0xFF2D4A6F))
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Tavla tahtasi gorseli
            BackgammonBoardVisual()
        }

        // === ALT YARI: Butonlar ===
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Yeni Oyun + Rovansli Oyun yan yana
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                MenuButton(
                    text = "YENI OYUN",
                    containerColor = Color(0xFF2E5D3A),
                    modifier = Modifier.weight(1f),
                    onClick = {
                        context.startActivity(Intent(context, NewGameActivity::class.java))
                    }
                )
                MenuButton(
                    text = "ROVANSLI OYUN",
                    containerColor = Color(0xFF2D4A6F),
                    modifier = Modifier.weight(1f),
                    onClick = {
                        context.startActivity(Intent(context, RematchSetupActivity::class.java))
                    }
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Online + Gecmis yan yana
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                MenuButton(
                    text = "ONLINE",
                    containerColor = Color(0xFF3D3260),
                    modifier = Modifier.weight(1f),
                    onClick = {
                        context.startActivity(Intent(context, OnlineLobbyActivity::class.java))
                    }
                )
                MenuButton(
                    text = "GECMIS",
                    containerColor = Color(0xFF2C3E50),
                    modifier = Modifier.weight(1f),
                    onClick = {
                        context.startActivity(Intent(context, GameHistoryActivity::class.java))
                    }
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Hareketler Dokumu tam genislik
            MenuButton(
                text = "HAREKETLER DOKUMU",
                containerColor = Color(0xFF1E2D3D),
                modifier = Modifier.fillMaxWidth(),
                onClick = {
                    context.startActivity(Intent(context, ActivityLogActivity::class.java))
                }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Sifirla + Cikis yan yana (kucuk)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Card(
                    onClick = {
                        val builder = AlertDialog.Builder(context)
                        builder.setTitle("Tum Verileri Sifirla")
                        builder.setMessage("Tum mac gecmisi ve oyuncu istatistikleri sifirlanacak. Bu islem geri alinamaz!")
                        builder.setPositiveButton("Evet, Sifirla") { _, _ ->
                            val db = DatabaseHelper(context)
                            val silinen = db.resetAllData()
                            db.addActivityLog(
                                actionType = ActionTypes.DATA_RESET,
                                description = "Tum veriler sifirlandi ($silinen mac silindi)"
                            )
                            Toast.makeText(context, "Tum veriler sifirlandi ($silinen mac silindi)", Toast.LENGTH_SHORT).show()
                        }
                        builder.setNegativeButton("Iptal", null)
                        builder.show()
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(42.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF3D2020)),
                    shape = RoundedCornerShape(10.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            text = "TUMUNU SIL",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFCF7070),
                            letterSpacing = 1.sp
                        )
                    }
                }

                Card(
                    onClick = {
                        (context as ComponentActivity).finish()
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(42.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF5A2020)),
                    shape = RoundedCornerShape(10.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            text = "CIKIS",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            letterSpacing = 1.sp
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))
    }
}

@Preview(showBackground = true)
@Composable
fun MainScreenPreview() {
    TavlaAppTheme {
        MainScreen()
    }
}
