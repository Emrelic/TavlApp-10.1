package com.tavla.tavlapp

import android.os.Bundle
import android.view.View
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.random.Random

class DiceActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Full screen
        window.decorView.systemUiVisibility = (
            View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
            View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or
            View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
            View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
            View.SYSTEM_UI_FLAG_FULLSCREEN or
            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
        )

        // Intent parametreleri
        val gameType = intent.getStringExtra("game_type") ?: "Modern"
        val useDiceRoller = intent.getBooleanExtra("use_dice_roller", false)
        val useTimer = intent.getBooleanExtra("use_timer", false)
        val player1NameText = intent.getStringExtra("player1_name") ?: "Oyuncu 1"
        val player2NameText = intent.getStringExtra("player2_name") ?: "Oyuncu 2"

        setContent {
            TavlaAppTheme {
                DiceScreen(
                    gameType = gameType,
                    useDiceRoller = useDiceRoller,
                    useTimer = useTimer,
                    player1Name = player1NameText,
                    player2Name = player2NameText,
                    onExit = { finish() }
                )
            }
        }
    }
}

@Composable
fun DiceScreen(
    gameType: String,
    useDiceRoller: Boolean,
    useTimer: Boolean,
    player1Name: String,
    player2Name: String,
    onExit: () -> Unit
) {
    var dice1Value by remember { mutableIntStateOf(5) }
    var dice2Value by remember { mutableIntStateOf(3) }
    var isRolling by remember { mutableStateOf(false) }

    // Full screen background
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Başlık
            Text(
                text = "ZAR ATMA EKRANİ",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF0066CC)
            )

            // Oyuncu bilgileri
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Text(
                    text = player1Name,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "VS",
                    fontSize = 16.sp,
                    color = Color.Gray
                )
                Text(
                    text = player2Name,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            // Büyük zarlar
            Row(
                horizontalArrangement = Arrangement.spacedBy(32.dp)
            ) {
                // Sol zar
                Box(
                    modifier = Modifier
                        .size(120.dp)
                        .background(Color.White, RoundedCornerShape(8.dp))
                        .border(2.dp, Color.Black, RoundedCornerShape(8.dp))
                        .clickable { rollDice(dice1Value, dice2Value) { d1, d2 ->
                            dice1Value = d1
                            dice2Value = d2
                        }},
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = dice1Value.toString(),
                        fontSize = 48.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Red
                    )
                }

                // Sağ zar
                Box(
                    modifier = Modifier
                        .size(120.dp)
                        .background(Color.White, RoundedCornerShape(8.dp))
                        .border(2.dp, Color.Black, RoundedCornerShape(8.dp))
                        .clickable { rollDice(dice1Value, dice2Value) { d1, d2 ->
                            dice1Value = d1
                            dice2Value = d2
                        }},
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = dice2Value.toString(),
                        fontSize = 48.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Red
                    )
                }
            }

            // Zar sonucu
            Text(
                text = "Sonuç: $dice1Value - $dice2Value",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )

            // Roll butonu
            Button(
                onClick = {
                    rollDice(dice1Value, dice2Value) { d1, d2 ->
                        dice1Value = d1
                        dice2Value = d2
                    }
                },
                modifier = Modifier.size(width = 160.dp, height = 48.dp)
            ) {
                Text(
                    text = if (isRolling) "Atılıyor..." else "🎲 ZAR AT",
                    fontSize = 16.sp
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Çıkış butonu
            Button(
                onClick = onExit,
                colors = ButtonDefaults.buttonColors(containerColor = Color.Red),
                modifier = Modifier.size(width = 120.dp, height = 40.dp)
            ) {
                Text(
                    text = "ÇIKIŞ",
                    color = Color.White,
                    fontSize = 14.sp
                )
            }
        }
    }
}

private fun rollDice(currentDice1: Int, currentDice2: Int, onResult: (Int, Int) -> Unit) {
    val newDice1 = Random.nextInt(1, 7)
    val newDice2 = Random.nextInt(1, 7)
    onResult(newDice1, newDice2)
}