package com.tavla.tavlapp.ui.online

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tavla.tavlapp.online.ChatMessage
import java.text.SimpleDateFormat
import java.util.*

/**
 * Oyun ici chat paneli.
 * Sag tarafta acilir/kapanir panel olarak gosterilir.
 */
@Composable
fun ChatPanel(
    messages: List<ChatMessage>,
    onSendMessage: (String) -> Unit,
    onClose: () -> Unit
) {
    var messageText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    // Yeni mesaj gelince en alta scroll
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.lastIndex)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0x88000000))
            .clickable { onClose() }
    ) {
        Column(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .width(320.dp)
                .fillMaxHeight()
                .background(Color(0xFF1E1E1E))
                .clickable(enabled = false) {} // Panele tiklaninca kapanmasin
                .padding(8.dp)
        ) {
            // Baslik
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Chat",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                TextButton(onClick = onClose) {
                    Text("Kapat", color = Color(0xFF2196F3))
                }
            }

            HorizontalDivider(color = Color(0xFF444444))

            // Hazir mesajlar
            QuickReplies(onSendMessage)

            Spacer(Modifier.height(4.dp))
            HorizontalDivider(color = Color(0xFF444444))
            Spacer(Modifier.height(4.dp))

            // Mesaj listesi
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                state = listState,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(messages) { message ->
                    ChatBubble(message)
                }
            }

            Spacer(Modifier.height(4.dp))

            // Mesaj girisi
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                BasicTextField(
                    value = messageText,
                    onValueChange = { if (it.length <= 200) messageText = it },
                    modifier = Modifier
                        .weight(1f)
                        .height(40.dp)
                        .background(Color(0xFF333333), RoundedCornerShape(20.dp))
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    textStyle = TextStyle(color = Color.White, fontSize = 14.sp),
                    singleLine = true,
                    decorationBox = { innerTextField ->
                        if (messageText.isEmpty()) {
                            Text("Mesaj yaz...", color = Color.Gray, fontSize = 14.sp)
                        }
                        innerTextField()
                    }
                )
                Spacer(Modifier.width(4.dp))
                Button(
                    onClick = {
                        if (messageText.isNotBlank()) {
                            onSendMessage(messageText)
                            messageText = ""
                        }
                    },
                    modifier = Modifier.height(40.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2196F3)),
                    shape = RoundedCornerShape(20.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp)
                ) {
                    Text("Gonder", fontSize = 13.sp)
                }
            }
        }
    }
}

/**
 * Hazir mesajlar (quick replies).
 */
@Composable
private fun QuickReplies(onSend: (String) -> Unit) {
    val quickMessages = listOf(
        "iyi oyun!",
        "tebrikler",
        "iyi sans",
        "guzel hamle"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        for (msg in quickMessages) {
            Box(
                modifier = Modifier
                    .background(Color(0xFF2E7D32), RoundedCornerShape(16.dp))
                    .clickable { onSend(msg) }
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(msg, color = Color.White, fontSize = 11.sp)
            }
        }
    }
}

/**
 * Chat baloncugu.
 */
@Composable
private fun ChatBubble(message: ChatMessage) {
    val alignment = if (message.isMine) Alignment.CenterEnd else Alignment.CenterStart
    val bgColor = if (message.isMine) Color(0xFF1565C0) else Color(0xFF333333)
    val timeFormat = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }

    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = alignment
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 240.dp)
                .background(bgColor, RoundedCornerShape(12.dp))
                .padding(horizontal = 10.dp, vertical = 6.dp)
        ) {
            if (!message.isMine) {
                Text(
                    message.name,
                    fontSize = 11.sp,
                    color = Color(0xFF90CAF9),
                    fontWeight = FontWeight.Bold
                )
            }
            Text(
                message.message,
                fontSize = 14.sp,
                color = Color.White
            )
            Text(
                if (message.timestamp > 0) timeFormat.format(Date(message.timestamp)) else "",
                fontSize = 10.sp,
                color = Color(0xFF999999)
            )
        }
    }
}
