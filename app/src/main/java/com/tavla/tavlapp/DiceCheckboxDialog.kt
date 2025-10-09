package com.tavla.tavlapp

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog

/**
 * Zar atma sonrası checkbox işaretleme dialog'u
 *
 * @param dice1 İlk zar değeri (1-6)
 * @param dice2 İkinci zar değeri (1-6)
 * @param onDismiss Dialog kapatıldığında
 * @param onSave Checkbox'lar işaretlenip kaydedildiğinde
 */
@Composable
fun DiceCheckboxDialog(
    dice1: Int,
    dice2: Int,
    onDismiss: () -> Unit,
    onSave: (DiceRollResult) -> Unit
) {
    val isDouble = DiceHelper.isDouble(dice1, dice2)
    val diceCount = if (isDouble) 4 else 2

    // Checkbox durumları
    var dice1State by remember { mutableStateOf(DiceState.PLAYED) }
    var dice2State by remember { mutableStateOf(DiceState.PLAYED) }
    var dice3State by remember { mutableStateOf(if (isDouble) DiceState.PLAYED else DiceState.NONE) }
    var dice4State by remember { mutableStateOf(if (isDouble) DiceState.PLAYED else DiceState.NONE) }

    // Kısmi oynama değerleri (döndürülmüş zar için)
    var dice1PartialValue by remember { mutableIntStateOf(dice1) }
    var dice2PartialValue by remember { mutableIntStateOf(dice2) }
    var dice3PartialValue by remember { mutableIntStateOf(dice1) }
    var dice4PartialValue by remember { mutableIntStateOf(dice2) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Başlık
                Text(
                    text = "Atılan Zar: ${DiceHelper.normalizeDice(dice1, dice2)}",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )

                Divider()

                // Zar gösterimi + Checkbox'lar
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    // Zar 1
                    DiceCheckboxItem(
                        diceValue = dice1,
                        state = dice1State,
                        partialValue = dice1PartialValue,
                        onStateChange = { dice1State = it },
                        onPartialValueChange = { dice1PartialValue = it }
                    )

                    // Zar 2
                    DiceCheckboxItem(
                        diceValue = dice2,
                        state = dice2State,
                        partialValue = dice2PartialValue,
                        onStateChange = { dice2State = it },
                        onPartialValueChange = { dice2PartialValue = it }
                    )

                    // Çift zar ise 3. ve 4. zarlar
                    if (isDouble) {
                        DiceCheckboxItem(
                            diceValue = dice1,
                            state = dice3State,
                            partialValue = dice3PartialValue,
                            onStateChange = { dice3State = it },
                            onPartialValueChange = { dice3PartialValue = it }
                        )

                        DiceCheckboxItem(
                            diceValue = dice2,
                            state = dice4State,
                            partialValue = dice4PartialValue,
                            onStateChange = { dice4State = it },
                            onPartialValueChange = { dice4PartialValue = it }
                        )
                    }
                }

                Divider()

                // Hızlı seçim butonları
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            dice1State = DiceState.PLAYED
                            dice2State = DiceState.PLAYED
                            if (isDouble) {
                                dice3State = DiceState.PLAYED
                                dice4State = DiceState.PLAYED
                            }
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))
                    ) {
                        Text("Hepsi Tam", fontSize = 12.sp)
                    }

                    Button(
                        onClick = {
                            dice1State = DiceState.WASTED
                            dice2State = DiceState.WASTED
                            if (isDouble) {
                                dice3State = DiceState.WASTED
                                dice4State = DiceState.WASTED
                            }
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF9800))
                    ) {
                        Text("Hepsi Gele", fontSize = 12.sp)
                    }
                }

                // Kaydet / İptal butonları
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("İptal")
                    }

                    Button(
                        onClick = {
                            val result = DiceRollResult(
                                dice1 = dice1,
                                dice2 = dice2,
                                dice1State = dice1State,
                                dice2State = dice2State,
                                dice3State = dice3State,
                                dice4State = dice4State
                            )
                            onSave(result)
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2196F3))
                    ) {
                        Text("Kaydet")
                    }
                }
            }
        }
    }
}

/**
 * Tek bir zar + checkbox item
 */
@Composable
fun DiceCheckboxItem(
    diceValue: Int,
    state: DiceState,
    partialValue: Int,
    onStateChange: (DiceState) -> Unit,
    onPartialValueChange: (Int) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFFF5F5F5), RoundedCornerShape(8.dp))
            .padding(12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Zar gösterimi
        Box(
            modifier = Modifier
                .size(60.dp)
                .background(Color.White, RoundedCornerShape(8.dp))
                .border(2.dp, Color.Black, RoundedCornerShape(8.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = if (state == DiceState.PARTIAL_WASTED) {
                    partialValue.toString()
                } else {
                    diceValue.toString()
                },
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold
            )
        }

        // Checkbox durumu
        Box(
            modifier = Modifier
                .size(60.dp)
                .background(
                    when (state) {
                        DiceState.PLAYED -> Color(0xFF4CAF50)
                        DiceState.WASTED -> Color(0xFFFF9800)
                        DiceState.PARTIAL_WASTED -> Color(0xFF2196F3)
                        DiceState.END_WASTE -> Color(0xFFF44336)
                        DiceState.NONE -> Color.Gray
                    },
                    RoundedCornerShape(8.dp)
                )
                .border(2.dp, Color.Black, RoundedCornerShape(8.dp))
                .pointerInput(Unit) {
                    detectTapGestures(
                        onTap = {
                            // Normal tıklama: durumları döngü halinde değiştir
                            onStateChange(
                                when (state) {
                                    DiceState.PLAYED -> DiceState.WASTED
                                    DiceState.WASTED -> DiceState.END_WASTE
                                    DiceState.END_WASTE -> DiceState.PLAYED
                                    DiceState.PARTIAL_WASTED -> DiceState.PLAYED
                                    DiceState.NONE -> DiceState.NONE
                                }
                            )
                        },
                        onLongPress = {
                            // Uzun basma: Kısmi boşa giden moda geç (sadece 2+ zarlar için)
                            if (diceValue >= 2) {
                                if (state != DiceState.PARTIAL_WASTED) {
                                    onStateChange(DiceState.PARTIAL_WASTED)
                                    onPartialValueChange(1) // Varsayılan 1 evinden toplar
                                } else {
                                    // Kısmi değeri artır (1 → 2 → 3 → ... → diceValue-1 → 1)
                                    val nextValue = if (partialValue >= diceValue - 1) 1 else partialValue + 1
                                    onPartialValueChange(nextValue)
                                }
                            }
                        }
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = when (state) {
                    DiceState.PLAYED -> "✓"
                    DiceState.WASTED -> "✗"
                    DiceState.PARTIAL_WASTED -> "↻"
                    DiceState.END_WASTE -> "■"
                    DiceState.NONE -> ""
                },
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }

        // Durum açıklaması
        Text(
            text = when (state) {
                DiceState.PLAYED -> "Tam\nOynandı"
                DiceState.WASTED -> "Gele"
                DiceState.PARTIAL_WASTED -> "Kısmi\n(${diceValue - partialValue} boşa)"
                DiceState.END_WASTE -> "Bitiş\nArtığı"
                DiceState.NONE -> ""
            },
            fontSize = 12.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.width(70.dp)
        )
    }
}
