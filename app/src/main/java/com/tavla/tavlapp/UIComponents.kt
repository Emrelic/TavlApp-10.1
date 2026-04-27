package com.tavla.tavlapp

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.BorderStroke
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.SimpleDateFormat
import java.util.*

// Tema renkleri (paylasilir)
object AppColors {
    val BgDark = Color(0xFF16213E)
    val BgCard = Color(0xFF1A2744)
    val GoldLight = Color(0xFFE8D5B7)
    val GoldDark = Color(0xFFBFA47A)
    val BorderBrown = Color(0xFF5D4037)
    val TextWhite = Color.White
    val TextMuted = Color.White.copy(alpha = 0.6f)
}

object UIComponents {

    @Composable
    fun MatchListItem(match: Match, players: Map<Long, Player>, onItemClick: (Long) -> Unit) {
        val player1Name = players[match.player1Id]?.name ?: "Bilinmeyen"
        val player2Name = players[match.player2Id]?.name ?: "Bilinmeyen"
        val winnerName = players[match.winnerId]?.name ?: "Bilinmeyen"

        val date = try {
            val inputFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
            val outputFormat = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())
            val parsedDate = inputFormat.parse(match.date)
            outputFormat.format(parsedDate)
        } catch (e: Exception) {
            match.date
        }

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp)
                .clickable { onItemClick(match.id) },
            colors = CardDefaults.cardColors(containerColor = AppColors.BgCard),
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, AppColors.BorderBrown)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Mac #${match.id}",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = AppColors.GoldLight
                    )
                    Text(
                        text = date,
                        fontSize = 11.sp,
                        color = AppColors.GoldDark
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(horizontalAlignment = Alignment.Start) {
                        Text(
                            text = player1Name,
                            fontWeight = if (match.winnerId == match.player1Id) FontWeight.Bold else FontWeight.Normal,
                            color = AppColors.TextWhite,
                            fontSize = 13.sp
                        )
                        Text(text = "${match.player1Score} puan", color = AppColors.TextMuted, fontSize = 11.sp)
                        Text(text = "${match.player1RoundsWon} el", color = AppColors.TextMuted, fontSize = 11.sp)
                    }

                    Text(
                        text = "vs",
                        modifier = Modifier.padding(horizontal = 8.dp),
                        color = AppColors.GoldDark,
                        fontSize = 12.sp
                    )

                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = player2Name,
                            fontWeight = if (match.winnerId == match.player2Id) FontWeight.Bold else FontWeight.Normal,
                            color = AppColors.TextWhite,
                            fontSize = 13.sp
                        )
                        Text(text = "${match.player2Score} puan", color = AppColors.TextMuted, fontSize = 11.sp)
                        Text(text = "${match.player2RoundsWon} el", color = AppColors.TextMuted, fontSize = 11.sp)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = "${match.gameType} Tavla", color = AppColors.GoldDark, fontSize = 11.sp)
                    Text(
                        text = "Kazanan: $winnerName",
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF4CAF50),
                        fontSize = 12.sp
                    )
                }
            }
        }
    }

    @Composable
    fun MatchListItemWithDelete(
        match: Match,
        players: Map<Long, Player>,
        isDeleteMode: Boolean,
        isSelected: Boolean,
        onItemClick: (Long) -> Unit,
        onDeleteClick: (Long) -> Unit
    ) {
        val player1Name = players[match.player1Id]?.name ?: "Bilinmeyen"
        val player2Name = players[match.player2Id]?.name ?: "Bilinmeyen"
        val winnerName = players[match.winnerId]?.name ?: "Bilinmeyen"

        val date = try {
            val inputFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
            val outputFormat = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())
            val parsedDate = inputFormat.parse(match.date)
            outputFormat.format(parsedDate)
        } catch (e: Exception) {
            match.date
        }

        val cardBg = when {
            isSelected && isDeleteMode -> Color(0xFF6A1B9A).copy(alpha = 0.4f)
            else -> AppColors.BgCard
        }

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp)
                .clickable { onItemClick(match.id) },
            colors = CardDefaults.cardColors(containerColor = cardBg),
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, if (isSelected && isDeleteMode) Color(0xFF6A1B9A) else AppColors.BorderBrown)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (isDeleteMode) {
                    Checkbox(
                        checked = isSelected,
                        onCheckedChange = { onItemClick(match.id) },
                        modifier = Modifier.padding(end = 8.dp),
                        colors = CheckboxDefaults.colors(
                            checkedColor = Color(0xFF6A1B9A),
                            uncheckedColor = AppColors.GoldDark,
                            checkmarkColor = Color.White
                        )
                    )
                }

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 4.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Mac #${match.id}",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = AppColors.GoldLight
                        )
                        Text(
                            text = date,
                            fontSize = 11.sp,
                            color = AppColors.GoldDark
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(horizontalAlignment = Alignment.Start) {
                            Text(
                                text = player1Name,
                                fontWeight = if (match.winnerId == match.player1Id) FontWeight.Bold else FontWeight.Normal,
                                color = AppColors.TextWhite,
                                fontSize = 13.sp
                            )
                            Text(text = "${match.player1Score} puan", color = AppColors.TextMuted, fontSize = 11.sp)
                            Text(text = "${match.player1RoundsWon} el", color = AppColors.TextMuted, fontSize = 11.sp)
                        }
                        Text(
                            text = "vs",
                            modifier = Modifier.padding(horizontal = 8.dp),
                            color = AppColors.GoldDark,
                            fontSize = 12.sp
                        )
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = player2Name,
                                fontWeight = if (match.winnerId == match.player2Id) FontWeight.Bold else FontWeight.Normal,
                                color = AppColors.TextWhite,
                                fontSize = 13.sp
                            )
                            Text(text = "${match.player2Score} puan", color = AppColors.TextMuted, fontSize = 11.sp)
                            Text(text = "${match.player2RoundsWon} el", color = AppColors.TextMuted, fontSize = 11.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = "${match.gameType} Tavla", color = AppColors.GoldDark, fontSize = 11.sp)
                        Text(
                            text = "Kazanan: $winnerName",
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF4CAF50),
                            fontSize = 12.sp
                        )
                    }
                }

                if (!isDeleteMode) {
                    IconButton(
                        onClick = { onDeleteClick(match.id) },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Sil",
                            tint = Color(0xFFF44336).copy(alpha = 0.7f),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }

    @Composable
    fun PlayerListItem(player: Player, onItemClick: (Long) -> Unit) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp)
                .clickable { onItemClick(player.id) },
            colors = CardDefaults.cardColors(containerColor = AppColors.BgCard),
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, AppColors.BorderBrown)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = player.name,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = AppColors.GoldLight
                )
                Icon(
                    imageVector = Icons.Default.KeyboardArrowRight,
                    contentDescription = "Goruntule",
                    modifier = Modifier.padding(start = 8.dp),
                    tint = AppColors.GoldDark
                )
            }
        }
    }
}
