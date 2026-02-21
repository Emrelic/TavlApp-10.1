package com.tavla.tavlapp.ui.board

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// Unicode zar karakterleri
object DiceUnicode {
    fun get(value: Int): String = when (value) {
        1 -> "\u2680"
        2 -> "\u2681"
        3 -> "\u2682"
        4 -> "\u2683"
        5 -> "\u2684"
        6 -> "\u2685"
        else -> "?"
    }
}

/**
 * Zar gosterim Composable.
 * Iki zar ve kullanilma durumlarini gosterir.
 */
@Composable
fun DiceDisplay(
    dice: List<Int>,           // Zar degerleri [die1, die2] veya [die, die, die, die]
    usedDice: List<Boolean>,   // Kullanildi mi
    isMyTurn: Boolean,         // Sira bende mi
    canRoll: Boolean,          // Zar atabilir miyim
    isRolling: Boolean,        // Zar atiliyor mu (animasyon)
    onRollDice: () -> Unit,    // Zar at callback
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (dice.isEmpty() && canRoll && isMyTurn) {
            // Zar at butonu
            RollDiceButton(
                isRolling = isRolling,
                onClick = onRollDice
            )
        } else if (dice.isNotEmpty()) {
            // Zar gosterimi
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Normal zarlar: 2 tane, Cift zarlar: 4 tane
                val displayDice = if (dice.size == 4) {
                    // Cift zar - 4 tane goster
                    dice.mapIndexed { index, value ->
                        Triple(value, usedDice.getOrElse(index) { false }, index)
                    }
                } else {
                    // Normal zar - 2 tane goster
                    dice.mapIndexed { index, value ->
                        Triple(value, usedDice.getOrElse(index) { false }, index)
                    }
                }

                for ((value, used, _) in displayDice) {
                    SingleDie(
                        value = value,
                        isUsed = used,
                        isRolling = isRolling
                    )
                }
            }
        }
    }
}

/**
 * Tek bir zar gosterimi.
 */
@Composable
fun SingleDie(
    value: Int,
    isUsed: Boolean,
    isRolling: Boolean,
    modifier: Modifier = Modifier
) {
    val rotation by if (isRolling) {
        val infiniteTransition = rememberInfiniteTransition(label = "dice_roll")
        infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 360f,
            animationSpec = infiniteRepeatable(
                animation = tween(300, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "dice_rotation"
        )
    } else {
        remember { mutableFloatStateOf(0f) }
    }

    val alpha = if (isUsed) 0.3f else 1f

    Box(
        modifier = modifier
            .size(50.dp)
            .rotate(if (isRolling) rotation else 0f)
            .background(
                color = Color.White.copy(alpha = alpha),
                shape = RoundedCornerShape(8.dp)
            )
            .border(
                width = 2.dp,
                color = Color.DarkGray.copy(alpha = alpha),
                shape = RoundedCornerShape(8.dp)
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = DiceUnicode.get(value),
            fontSize = 32.sp,
            color = Color.Black.copy(alpha = alpha)
        )
    }
}

/**
 * Zar at butonu.
 */
@Composable
fun RollDiceButton(
    isRolling: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        enabled = !isRolling,
        modifier = modifier
            .width(140.dp)
            .height(45.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = Color(0xFF9C27B0),  // Mor - mevcut uygulamadaki zar butonu rengi
            disabledContainerColor = Color.Gray
        ),
        shape = RoundedCornerShape(8.dp)
    ) {
        Text(
            text = if (isRolling) "..." else "ZAR AT",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
    }
}

/**
 * Baslangic zari gosterimi.
 * Her iki oyuncu birer zar atar, buyuk atan baslar.
 */
@Composable
fun StartingDiceDisplay(
    whiteDie: Int?,
    blackDie: Int?,
    isWaitingForRoll: Boolean,
    hasRolled: Boolean,
    onRollStartingDice: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "Baslangic Zari",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )

        Row(
            horizontalArrangement = Arrangement.spacedBy(24.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Beyaz oyuncunun zari
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Beyaz", fontSize = 12.sp, color = Color.White)
                if (whiteDie != null) {
                    SingleDie(value = whiteDie, isUsed = false, isRolling = false)
                } else {
                    Box(
                        modifier = Modifier
                            .size(50.dp)
                            .background(Color.Gray.copy(alpha = 0.5f), RoundedCornerShape(8.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("?", fontSize = 24.sp, color = Color.White)
                    }
                }
            }

            Text("vs", fontSize = 16.sp, color = Color.White)

            // Siyah oyuncunun zari
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Siyah", fontSize = 12.sp, color = Color.White)
                if (blackDie != null) {
                    SingleDie(value = blackDie, isUsed = false, isRolling = false)
                } else {
                    Box(
                        modifier = Modifier
                            .size(50.dp)
                            .background(Color.Gray.copy(alpha = 0.5f), RoundedCornerShape(8.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("?", fontSize = 24.sp, color = Color.White)
                    }
                }
            }
        }

        if (!hasRolled) {
            // Henuz zarini atmamis - buton goster
            RollDiceButton(isRolling = false, onClick = onRollStartingDice)
        } else if (isWaitingForRoll) {
            // Zarini atti ama rakip henuz atmadi - bekleme mesaji
            Text(
                text = "Rakibin zarini atmasi bekleniyor...",
                fontSize = 14.sp,
                color = Color(0xFFFF9800)
            )
        }

        if (whiteDie != null && blackDie != null) {
            val resultText = when {
                whiteDie > blackDie -> "Beyaz baslar!"
                blackDie > whiteDie -> "Siyah baslar!"
                else -> "Esit - tekrar atilacak"
            }
            Text(
                text = resultText,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF4CAF50)
            )
        }
    }
}
