package com.tavla.tavlapp.ui.online

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tavla.tavlapp.engine.*
import com.tavla.tavlapp.online.*
import com.tavla.tavlapp.ui.board.*

/**
 * Ana online oyun ekrani.
 * Landscape layout: Sol tarafta tahta, sag tarafta kontrol paneli.
 */
@Composable
fun OnlineGameScreen(
    viewModel: OnlineGameViewModel,
    onExit: () -> Unit
) {
    val myColor = if (viewModel.isMyTurn) "senin siran" else "rakibin sirasi"

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF1A1A1A))
    ) {
        Row(modifier = Modifier.fillMaxSize()) {
            // Sol: Tahta (agirlikli)
            Box(
                modifier = Modifier
                    .weight(0.65f)
                    .fillMaxHeight()
                    .padding(4.dp)
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    // Ust bilgi satiri - Rakip
                    PlayerInfoBar(
                        name = viewModel.opponentName,
                        score = viewModel.opponentScore,
                        isActive = !viewModel.isMyTurn,
                        isConnected = viewModel.isOpponentConnected,
                        isTop = true
                    )

                    // Tahta
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                    ) {
                        when (viewModel.gamePhase) {
                            GamePhase.WAITING -> {
                                WaitingOverlay()
                            }
                            GamePhase.ROLLING_START -> {
                                StartingDiceOverlay(viewModel)
                            }
                            else -> {
                                BackgammonBoard(
                                    boardState = viewModel.boardState,
                                    perspective = if (viewModel.isWhitePlayer) PlayerColor.WHITE else PlayerColor.BLACK,
                                    selectedPoint = viewModel.selectedPoint,
                                    legalDestinations = viewModel.legalDestinations,
                                    onPointTapped = { viewModel.onPointTapped(it) },
                                    onBearOffTapped = { viewModel.onBearOffTapped() },
                                    doublingCube = DoublingCubeState(
                                        value = viewModel.doublingCubeValue,
                                        owner = viewModel.cubeOwner,
                                        pendingOffer = viewModel.pendingDoubleOffer
                                    )
                                )

                                // Katlama teklifi overlay
                                if (viewModel.gamePhase == GamePhase.DOUBLING) {
                                    DoublingOverlay(viewModel)
                                }

                                // Oyun sonu overlay
                                if (viewModel.gamePhase == GamePhase.GAME_OVER) {
                                    GameOverOverlay(viewModel, onExit)
                                }
                            }
                        }
                    }

                    // Alt bilgi satiri - Ben
                    PlayerInfoBar(
                        name = viewModel.myName,
                        score = viewModel.myScore,
                        isActive = viewModel.isMyTurn,
                        isConnected = viewModel.isConnected,
                        isTop = false
                    )
                }
            }

            // Sag: Kontrol paneli
            Column(
                modifier = Modifier
                    .weight(0.35f)
                    .fillMaxHeight()
                    .background(Color(0xFF252525))
                    .padding(8.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Ust kisim: Mac bilgisi + chat butonu
                Column {
                    // Mac bilgisi
                    MatchInfoPanel(viewModel)
                    Spacer(Modifier.height(8.dp))

                    // Baglanti kopma uyarisi
                    if (!viewModel.isOpponentConnected && viewModel.disconnectCountdown != null) {
                        DisconnectWarning(viewModel.disconnectCountdown!!)
                        Spacer(Modifier.height(8.dp))
                    }

                    // Crawford bilgisi
                    if (viewModel.isCrawfordGame) {
                        CrawfordBadge()
                        Spacer(Modifier.height(8.dp))
                    }
                }

                // Orta kisim: Zar + aksiyonlar
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Zar gosterimi (tiklayinca sira degisir)
                    if (viewModel.remainingDice.isNotEmpty() && viewModel.isMyTurn) {
                        // Aktif zar (ilk siradaki) vurgulu gosterilir
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { viewModel.onSwapDice() }
                                .background(Color(0xFF333333), RoundedCornerShape(8.dp))
                                .padding(8.dp)
                        ) {
                            viewModel.remainingDice.forEachIndexed { index, die ->
                                Box(
                                    modifier = Modifier
                                        .size(45.dp)
                                        .background(
                                            if (index == 0) Color.White else Color.White.copy(alpha = 0.4f),
                                            RoundedCornerShape(6.dp)
                                        )
                                        .border(
                                            width = if (index == 0) 3.dp else 1.dp,
                                            color = if (index == 0) Color(0xFF4CAF50) else Color.Gray,
                                            shape = RoundedCornerShape(6.dp)
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = DiceUnicode.get(die),
                                        fontSize = 28.sp,
                                        color = if (index == 0) Color.Black else Color.Black.copy(alpha = 0.4f)
                                    )
                                }
                            }
                            if (viewModel.remainingDice.size >= 2 && viewModel.pendingMoves.isEmpty()) {
                                Text("  ↔", fontSize = 18.sp, color = Color(0xFF4CAF50))
                            }
                        }
                        if (viewModel.remainingDice.size >= 2 && viewModel.pendingMoves.isEmpty()) {
                            Text(
                                "Zarlara tikla: sira degistir",
                                fontSize = 10.sp,
                                color = Color.Gray
                            )
                        }
                        Spacer(Modifier.height(8.dp))
                    } else if (viewModel.diceValues.isNotEmpty() && !viewModel.isMyTurn) {
                        // Rakibin zarlari (sadece gosterim)
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.padding(4.dp)
                        ) {
                            viewModel.diceValues.forEach { die ->
                                SingleDie(value = die, isUsed = false, isRolling = false)
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                    }

                    // Zar at butonu
                    if (viewModel.isMyTurn && viewModel.diceValues.isEmpty() && viewModel.gamePhase == GamePhase.PLAYING) {
                        RollDiceButton(
                            isRolling = viewModel.isRolling,
                            onClick = { viewModel.onRollDice() }
                        )
                        Spacer(Modifier.height(8.dp))
                    }

                    // Hamle kontrol butonlari
                    if (viewModel.isMyTurn && viewModel.pendingMoves.isNotEmpty()) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            // Geri al
                            Button(
                                onClick = { viewModel.onUndoLastMove() },
                                modifier = Modifier.weight(1f).height(40.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0D47A1)),
                                shape = RoundedCornerShape(6.dp),
                                contentPadding = PaddingValues(4.dp)
                            ) {
                                Text("GERI AL", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }

                            // Hamleyi onayla
                            Button(
                                onClick = { viewModel.onConfirmTurn() },
                                modifier = Modifier.weight(1f).height(40.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50)),
                                shape = RoundedCornerShape(6.dp),
                                contentPadding = PaddingValues(4.dp)
                            ) {
                                Text("ONAYLA", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                    }

                    // Katlama butonu
                    if (viewModel.isMyTurn &&
                        viewModel.gamePhase == GamePhase.PLAYING &&
                        viewModel.diceValues.isEmpty() &&
                        !viewModel.isCrawfordGame &&
                        viewModel.gameType == "Modern"
                    ) {
                        val myColorStr = if (viewModel.isMyTurn) (if (viewModel.currentTurn == "white") "white" else "black") else ""
                        val canDouble = viewModel.cubeOwner == "center" || viewModel.cubeOwner == myColorStr

                        if (canDouble) {
                            Button(
                                onClick = { viewModel.onOfferDouble() },
                                modifier = Modifier.fillMaxWidth().height(40.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF9800)),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    "KATLA (${viewModel.doublingCubeValue * 2})",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                // Alt kisim: Chat butonu
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Sira bilgisi
                    Text(
                        text = if (viewModel.isMyTurn) "Senin siran" else "Rakibin sirasi",
                        fontSize = 14.sp,
                        color = if (viewModel.isMyTurn) Color(0xFF4CAF50) else Color.Gray,
                        fontWeight = FontWeight.Bold
                    )

                    // Chat butonu
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(Color(0xFF2196F3))
                            .clickable { viewModel.onToggleChat() }
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Chat", color = Color.White, fontSize = 14.sp)
                            if (viewModel.chatUnreadCount > 0) {
                                Spacer(Modifier.width(4.dp))
                                Box(
                                    modifier = Modifier
                                        .size(20.dp)
                                        .background(Color.Red, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        viewModel.chatUnreadCount.toString(),
                                        color = Color.White,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Chat paneli overlay
        if (viewModel.isChatOpen) {
            ChatPanel(
                messages = viewModel.chatMessages,
                onSendMessage = { viewModel.onSendChat(it) },
                onClose = { viewModel.onToggleChat() }
            )
        }
    }
}

@Composable
private fun PlayerInfoBar(
    name: String,
    score: Int,
    isActive: Boolean,
    isConnected: Boolean,
    isTop: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                if (isActive) Color(0xFF1B5E20).copy(alpha = 0.5f)
                else Color(0xFF333333)
            )
            .padding(horizontal = 8.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            // Baglanti gostergesi
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .background(
                        if (isConnected) Color(0xFF4CAF50) else Color.Red,
                        CircleShape
                    )
            )
            Spacer(Modifier.width(6.dp))
            Text(
                name.ifEmpty { "..." },
                fontSize = 14.sp,
                color = Color.White,
                fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal
            )
        }
        Text(
            "Puan: $score",
            fontSize = 14.sp,
            color = Color.White,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun MatchInfoPanel(viewModel: OnlineGameViewModel) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF333333), RoundedCornerShape(6.dp))
            .padding(8.dp)
    ) {
        Text(
            "Mac: ${viewModel.myScore} - ${viewModel.opponentScore} / ${viewModel.targetScore}",
            fontSize = 14.sp,
            color = Color.White,
            fontWeight = FontWeight.Bold
        )
        Text(
            "Kup: ${viewModel.doublingCubeValue}x",
            fontSize = 12.sp,
            color = Color(0xFFFF9800)
        )
        Text(
            viewModel.gameType,
            fontSize = 12.sp,
            color = Color.Gray
        )
    }
}

@Composable
private fun WaitingOverlay() {
    Box(
        modifier = Modifier.fillMaxSize().background(Color(0xCC000000)),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(color = Color(0xFF4CAF50))
            Spacer(Modifier.height(16.dp))
            Text("Rakip bekleniyor...", color = Color.White, fontSize = 20.sp)
        }
    }
}

@Composable
private fun StartingDiceOverlay(viewModel: OnlineGameViewModel) {
    val myDieRolled = if (viewModel.isWhitePlayer) viewModel.startingDiceWhite != null
                      else viewModel.startingDiceBlack != null

    Box(
        modifier = Modifier.fillMaxSize().background(Color(0xCC000000)),
        contentAlignment = Alignment.Center
    ) {
        StartingDiceDisplay(
            whiteDie = viewModel.startingDiceWhite,
            blackDie = viewModel.startingDiceBlack,
            isWaitingForRoll = viewModel.startingDiceWhite == null || viewModel.startingDiceBlack == null,
            hasRolled = myDieRolled,
            onRollStartingDice = { viewModel.onRollDice() }
        )
    }
}

@Composable
private fun DoublingOverlay(viewModel: OnlineGameViewModel) {
    val myColor = if (viewModel.isWhitePlayer) "white" else "black"
    val isOfferForMe = viewModel.pendingDoubleOffer != null &&
            viewModel.pendingDoubleOffer != myColor

    Box(
        modifier = Modifier.fillMaxSize().background(Color(0xCC000000)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .background(Color(0xFF333333), RoundedCornerShape(12.dp))
                .padding(24.dp)
        ) {
            Text(
                "KATLAMA TEKLIFI",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFFF9800)
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "${viewModel.doublingCubeValue} -> ${viewModel.doublingCubeValue * 2}",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Spacer(Modifier.height(16.dp))

            if (isOfferForMe) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Button(
                        onClick = { viewModel.onAcceptDouble() },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50)),
                        modifier = Modifier.height(45.dp)
                    ) {
                        Text("KABUL ET", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                    Button(
                        onClick = { viewModel.onDeclineDouble() },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFB71C1C)),
                        modifier = Modifier.height(45.dp)
                    ) {
                        Text("PES ET", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                }
            } else {
                Text(
                    "Rakip dusunuyor...",
                    color = Color.Gray,
                    fontSize = 16.sp
                )
            }
        }
    }
}

@Composable
private fun GameOverOverlay(viewModel: OnlineGameViewModel, onExit: () -> Unit) {
    val myColor = if (viewModel.isWhitePlayer) "white" else "black"
    val isWinner = viewModel.winner == myColor
    val winTypeText = when (viewModel.winType) {
        "M" -> "Mars"
        "B" -> "Backgammon"
        else -> "Normal"
    }

    Box(
        modifier = Modifier.fillMaxSize().background(Color(0xCC000000)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .background(Color(0xFF333333), RoundedCornerShape(12.dp))
                .padding(24.dp)
        ) {
            Text(
                if (viewModel.matchFinished) "MAC BITTI" else "OYUN BITTI",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFFF9800)
            )
            Spacer(Modifier.height(8.dp))

            Text(
                "${viewModel.winner ?: ""} kazandi!",
                fontSize = 20.sp,
                color = Color.White
            )
            Text(
                "Tur: $winTypeText | Kup: ${viewModel.doublingCubeValue}x",
                fontSize = 16.sp,
                color = Color.Gray
            )

            Spacer(Modifier.height(8.dp))
            Text(
                "Skor: ${viewModel.myScore} - ${viewModel.opponentScore}",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            Spacer(Modifier.height(16.dp))

            if (viewModel.matchFinished) {
                Button(
                    onClick = onExit,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))
                ) {
                    Text("ANA MENU", fontWeight = FontWeight.Bold)
                }
            } else {
                Button(
                    onClick = { viewModel.onProcessGameEnd() },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))
                ) {
                    Text("SONRAKI OYUN", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun DisconnectWarning(countdown: Int) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFFB71C1C), RoundedCornerShape(6.dp))
            .padding(8.dp)
    ) {
        Text(
            "Rakip baglantisi kesildi! ($countdown sn)",
            color = Color.White,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun CrawfordBadge() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFFE65100), RoundedCornerShape(6.dp))
            .padding(6.dp)
    ) {
        Text(
            "CRAWFORD ELI",
            color = Color.White,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
    }
}
