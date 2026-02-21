package com.tavla.tavlapp.ui.online

import android.content.Intent
import android.content.pm.ActivityInfo
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tavla.tavlapp.online.*
import kotlinx.coroutines.launch

class OnlineLobbyActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        setContent {
            MaterialTheme {
                OnlineLobbyScreen()
            }
        }
    }
}

@Composable
fun OnlineLobbyScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val roomManager = remember { RoomManager() }

    var screen by remember { mutableStateOf<LobbyScreen>(LobbyScreen.Main) }
    var displayName by remember { mutableStateOf("") }
    var roomCode by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var targetScore by remember { mutableIntStateOf(11) }
    var gameType by remember { mutableStateOf("Modern") }
    var isRematchMode by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF1E1E1E))
            .padding(16.dp)
    ) {
        when (screen) {
            LobbyScreen.Main -> {
                MainLobby(
                    displayName = displayName,
                    onDisplayNameChange = { displayName = it },
                    errorMessage = errorMessage,
                    onCreateRoom = {
                        if (displayName.isBlank()) {
                            errorMessage = "Isim giriniz"
                            return@MainLobby
                        }
                        screen = LobbyScreen.Settings
                    },
                    onJoinRoom = {
                        if (displayName.isBlank()) {
                            errorMessage = "Isim giriniz"
                            return@MainLobby
                        }
                        screen = LobbyScreen.JoinRoom
                    },
                    onBack = { (context as? ComponentActivity)?.finish() }
                )
            }

            LobbyScreen.Settings -> {
                RoomSettingsScreen(
                    targetScore = targetScore,
                    gameType = gameType,
                    isRematchMode = isRematchMode,
                    onTargetScoreChange = { targetScore = it },
                    onGameTypeChange = { gameType = it },
                    onRematchModeChange = { isRematchMode = it },
                    onConfirm = {
                        screen = LobbyScreen.CreatingRoom
                        scope.launch {
                            try {
                                val config = MatchConfig(
                                    targetScore = targetScore,
                                    gameType = gameType,
                                    isRematchMode = isRematchMode
                                )
                                val code = roomManager.createRoom(config, displayName)
                                roomCode = code
                                screen = LobbyScreen.WaitingForOpponent
                            } catch (e: Exception) {
                                errorMessage = "Oda olusturulamadi: ${e.message}"
                                screen = LobbyScreen.Main
                            }
                        }
                    },
                    onBack = { screen = LobbyScreen.Main }
                )
            }

            LobbyScreen.CreatingRoom -> {
                LoadingScreen("Oda olusturuluyor...")
            }

            LobbyScreen.WaitingForOpponent -> {
                WaitingScreen(
                    roomCode = roomCode,
                    roomManager = roomManager,
                    onOpponentJoined = {
                        // Oyun ekranina gecis
                        val intent = Intent(context, OnlineGameActivity::class.java).apply {
                            putExtra("roomCode", roomCode)
                            putExtra("isWhite", true)
                            putExtra("displayName", displayName)
                        }
                        context.startActivity(intent)
                        (context as? ComponentActivity)?.finish()
                    },
                    onCancel = {
                        scope.launch {
                            roomManager.leaveRoom(roomCode)
                            screen = LobbyScreen.Main
                        }
                    }
                )
            }

            LobbyScreen.JoinRoom -> {
                JoinRoomScreen(
                    roomCode = roomCode,
                    onRoomCodeChange = { roomCode = it },
                    errorMessage = errorMessage,
                    onJoin = {
                        if (roomCode.length != 6) {
                            errorMessage = "6 haneli oda kodu giriniz"
                            return@JoinRoomScreen
                        }
                        screen = LobbyScreen.JoiningRoom
                        scope.launch {
                            when (val result = roomManager.joinRoom(roomCode, displayName)) {
                                is JoinResult.Success -> {
                                    val intent = Intent(context, OnlineGameActivity::class.java).apply {
                                        putExtra("roomCode", roomCode)
                                        putExtra("isWhite", false)
                                        putExtra("displayName", displayName)
                                    }
                                    context.startActivity(intent)
                                    (context as? ComponentActivity)?.finish()
                                }
                                is JoinResult.Error -> {
                                    errorMessage = result.message
                                    screen = LobbyScreen.JoinRoom
                                }
                            }
                        }
                    },
                    onBack = {
                        errorMessage = null
                        screen = LobbyScreen.Main
                    }
                )
            }

            LobbyScreen.JoiningRoom -> {
                LoadingScreen("Odaya katiliniyor...")
            }
        }
    }
}

@Composable
private fun MainLobby(
    displayName: String,
    onDisplayNameChange: (String) -> Unit,
    errorMessage: String?,
    onCreateRoom: () -> Unit,
    onJoinRoom: () -> Unit,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            "Online Tavla",
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
        Spacer(Modifier.height(32.dp))

        // Isim girisi
        Text("Oyuncu Ismi:", fontSize = 16.sp, color = Color.White)
        Spacer(Modifier.height(8.dp))
        BasicTextField(
            value = displayName,
            onValueChange = onDisplayNameChange,
            modifier = Modifier
                .width(250.dp)
                .height(45.dp)
                .background(Color.White, RoundedCornerShape(8.dp))
                .padding(horizontal = 12.dp, vertical = 10.dp),
            textStyle = TextStyle(fontSize = 18.sp, color = Color.Black),
            singleLine = true
        )

        if (errorMessage != null) {
            Spacer(Modifier.height(8.dp))
            Text(errorMessage, color = Color.Red, fontSize = 14.sp)
        }

        Spacer(Modifier.height(24.dp))

        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Button(
                onClick = onCreateRoom,
                modifier = Modifier.width(180.dp).height(50.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50)),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("ODA OLUSTUR", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }

            Button(
                onClick = onJoinRoom,
                modifier = Modifier.width(180.dp).height(50.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2196F3)),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("ODAYA KATIL", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(Modifier.height(16.dp))

        TextButton(onClick = onBack) {
            Text("Geri", color = Color.Gray)
        }
    }
}

@Composable
private fun RoomSettingsScreen(
    targetScore: Int,
    gameType: String,
    isRematchMode: Boolean,
    onTargetScoreChange: (Int) -> Unit,
    onGameTypeChange: (String) -> Unit,
    onRematchModeChange: (Boolean) -> Unit,
    onConfirm: () -> Unit,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("Mac Ayarlari", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color.White)
        Spacer(Modifier.height(24.dp))

        // Hedef puan
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Hedef Puan: ", fontSize = 16.sp, color = Color.White)
            listOf(5, 7, 11, 13, 15).forEach { score ->
                Button(
                    onClick = { onTargetScoreChange(score) },
                    modifier = Modifier.padding(horizontal = 2.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (targetScore == score) Color(0xFF4CAF50) else Color.DarkGray
                    ),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                ) {
                    Text("$score", fontSize = 14.sp)
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        // Oyun tipi
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Oyun Tipi: ", fontSize = 16.sp, color = Color.White)
            listOf("Modern", "Geleneksel").forEach { type ->
                Button(
                    onClick = { onGameTypeChange(type) },
                    modifier = Modifier.padding(horizontal = 4.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (gameType == type) Color(0xFF4CAF50) else Color.DarkGray
                    )
                ) {
                    Text(type, fontSize = 14.sp)
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        // Rovansli mod
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Rovansli Mac: ", fontSize = 16.sp, color = Color.White)
            Switch(
                checked = isRematchMode,
                onCheckedChange = onRematchModeChange,
                colors = SwitchDefaults.colors(checkedTrackColor = Color(0xFF4CAF50))
            )
        }

        Spacer(Modifier.height(24.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Button(
                onClick = onBack,
                colors = ButtonDefaults.buttonColors(containerColor = Color.Gray)
            ) { Text("Geri") }
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))
            ) { Text("ODA OLUSTUR") }
        }
    }
}

@Composable
private fun WaitingScreen(
    roomCode: String,
    roomManager: RoomManager,
    onOpponentJoined: () -> Unit,
    onCancel: () -> Unit
) {
    // Oda durumunu izle
    val roomState by roomManager.observeRoom(roomCode).collectAsState(initial = null)

    LaunchedEffect(roomState) {
        val state = roomState ?: return@LaunchedEffect
        if (state.blackPlayer != null && state.status == "ready") {
            onOpponentJoined()
        }
    }

    // Bekleme animasyonu
    val infiniteTransition = rememberInfiniteTransition(label = "waiting")
    val dots by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 3f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "dots"
    )

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("Oda Kodu", fontSize = 18.sp, color = Color.Gray)
        Spacer(Modifier.height(8.dp))

        // Buyuk oda kodu gosterimi
        Text(
            text = roomCode,
            fontSize = 48.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF4CAF50),
            letterSpacing = 8.sp
        )

        Spacer(Modifier.height(4.dp))
        Text(
            "Bu kodu rakibinize gonderin",
            fontSize = 14.sp,
            color = Color.Gray
        )

        Spacer(Modifier.height(32.dp))

        val dotsText = ".".repeat(dots.toInt() + 1)
        Text(
            "Rakip bekleniyor$dotsText",
            fontSize = 20.sp,
            color = Color.White
        )

        Spacer(Modifier.height(24.dp))

        Button(
            onClick = onCancel,
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFB71C1C))
        ) {
            Text("IPTAL", fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun JoinRoomScreen(
    roomCode: String,
    onRoomCodeChange: (String) -> Unit,
    errorMessage: String?,
    onJoin: () -> Unit,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("Odaya Katil", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color.White)
        Spacer(Modifier.height(24.dp))

        Text("6 Haneli Oda Kodu:", fontSize = 16.sp, color = Color.White)
        Spacer(Modifier.height(8.dp))

        BasicTextField(
            value = roomCode,
            onValueChange = { if (it.length <= 6 && it.all { c -> c.isDigit() }) onRoomCodeChange(it) },
            modifier = Modifier
                .width(200.dp)
                .height(55.dp)
                .background(Color.White, RoundedCornerShape(8.dp))
                .padding(horizontal = 12.dp, vertical = 12.dp),
            textStyle = TextStyle(
                fontSize = 28.sp,
                color = Color.Black,
                textAlign = TextAlign.Center,
                letterSpacing = 6.sp,
                fontWeight = FontWeight.Bold
            ),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true
        )

        if (errorMessage != null) {
            Spacer(Modifier.height(8.dp))
            Text(errorMessage, color = Color.Red, fontSize = 14.sp)
        }

        Spacer(Modifier.height(24.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Button(
                onClick = onBack,
                colors = ButtonDefaults.buttonColors(containerColor = Color.Gray)
            ) { Text("Geri") }
            Button(
                onClick = onJoin,
                enabled = roomCode.length == 6,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2196F3))
            ) { Text("KATIL", fontWeight = FontWeight.Bold) }
        }
    }
}

@Composable
private fun LoadingScreen(message: String) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(color = Color(0xFF4CAF50))
            Spacer(Modifier.height(16.dp))
            Text(message, color = Color.White, fontSize = 18.sp)
        }
    }
}

private sealed class LobbyScreen {
    data object Main : LobbyScreen()
    data object Settings : LobbyScreen()
    data object CreatingRoom : LobbyScreen()
    data object WaitingForOpponent : LobbyScreen()
    data object JoinRoom : LobbyScreen()
    data object JoiningRoom : LobbyScreen()
}
