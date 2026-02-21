package com.tavla.tavlapp.online

import com.google.firebase.database.*
import com.tavla.tavlapp.engine.BoardState
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import kotlin.random.Random

/**
 * Oda (Room) yonetimi.
 * Oda olusturma, katilma, izleme ve ayrilma islemleri.
 */
class RoomManager {

    companion object {
        private const val ROOM_CODE_LENGTH = 6
        private const val DISCONNECT_TIMEOUT_MS = 60_000L  // 60 saniye
    }

    /**
     * Yeni oda olusturur.
     * @param config Mac ayarlari
     * @param displayName Oyuncu ismi
     * @return 6 haneli oda kodu
     */
    suspend fun createRoom(config: MatchConfig, displayName: String): String {
        val user = FirebaseManager.signInAnonymously()
        val roomCode = generateRoomCode()
        val roomRef = FirebaseManager.roomRef(roomCode)

        val roomData = mapOf(
            "createdAt" to ServerValue.TIMESTAMP,
            "status" to "waiting",
            "matchConfig" to config.toMap(),
            "players" to mapOf(
                "white" to mapOf(
                    "uid" to user.uid,
                    "displayName" to displayName,
                    "ready" to false,
                    "connected" to true
                )
            ),
            "match" to mapOf(
                "whiteScore" to 0,
                "blackScore" to 0,
                "currentGameIndex" to 0,
                "isCrawfordGame" to false,
                "crawfordGamePlayed" to false
            ),
            "currentGame" to mapOf(
                "status" to "waiting",
                "turn" to "white",
                "board" to BoardState.initial().toMap(),
                "dice" to mapOf("values" to emptyList<Int>()),
                "startingDice" to mapOf<String, Any?>(),
                "doublingCube" to mapOf(
                    "value" to 1,
                    "owner" to "center",
                    "pendingOffer" to null
                ),
                "turnMoves" to emptyList<Any>(),
                "winner" to null,
                "winType" to null
            )
        )

        roomRef.setValue(roomData).await()

        // Baglanti kopma handler'i
        setupDisconnectHandler(roomCode, "white")

        return roomCode
    }

    /**
     * Mevcut odaya katilir.
     * @param roomCode 6 haneli oda kodu
     * @param displayName Oyuncu ismi
     */
    suspend fun joinRoom(roomCode: String, displayName: String): JoinResult {
        val user = FirebaseManager.signInAnonymously()
        val roomRef = FirebaseManager.roomRef(roomCode)

        val snapshot = roomRef.get().await()
        if (!snapshot.exists()) {
            return JoinResult.Error("Oda bulunamadi")
        }

        val status = snapshot.child("status").getValue(String::class.java)
        if (status != "waiting") {
            return JoinResult.Error("Oda dolu veya oyun basladi")
        }

        // Siyah oyuncu olarak katil
        val blackPlayer = mapOf(
            "uid" to user.uid,
            "displayName" to displayName,
            "ready" to false,
            "connected" to true
        )

        val updates = mapOf<String, Any?>(
            "players/black" to blackPlayer,
            "status" to "playing",
            "currentGame/status" to "rolling_start"
        )
        roomRef.updateChildren(updates).await()

        // Baglanti kopma handler'i
        setupDisconnectHandler(roomCode, "black")

        return JoinResult.Success(roomCode)
    }

    /**
     * Oda durumunu izler.
     */
    fun observeRoom(roomCode: String): Flow<RoomState> = callbackFlow {
        val roomRef = FirebaseManager.roomRef(roomCode)
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val roomState = parseRoomState(snapshot)
                trySend(roomState)
            }

            override fun onCancelled(error: DatabaseError) {
                close(error.toException())
            }
        }

        roomRef.addValueEventListener(listener)
        awaitClose { roomRef.removeEventListener(listener) }
    }

    /**
     * Oyuncu hazir durumunu ayarlar.
     */
    suspend fun setPlayerReady(roomCode: String, isWhite: Boolean) {
        val playerPath = if (isWhite) "players/white/ready" else "players/black/ready"
        FirebaseManager.roomRef(roomCode).child(playerPath).setValue(true).await()
    }

    /**
     * Odadan ayrilir.
     */
    suspend fun leaveRoom(roomCode: String) {
        val uid = FirebaseManager.uid ?: return
        val roomRef = FirebaseManager.roomRef(roomCode)
        val snapshot = roomRef.get().await()

        if (!snapshot.exists()) return

        val whiteUid = snapshot.child("players/white/uid").getValue(String::class.java)
        val blackUid = snapshot.child("players/black/uid").getValue(String::class.java)

        when (uid) {
            whiteUid -> {
                if (blackUid == null) {
                    // Rakip yoksa odayi sil
                    roomRef.removeValue().await()
                } else {
                    roomRef.child("players/white/connected").setValue(false).await()
                    roomRef.child("status").setValue("abandoned").await()
                }
            }
            blackUid -> {
                roomRef.child("players/black/connected").setValue(false).await()
                roomRef.child("status").setValue("abandoned").await()
            }
        }
    }

    /**
     * Baglanti kopma handler'i ayarlar.
     */
    private fun setupDisconnectHandler(roomCode: String, playerColor: String) {
        val connectedRef = FirebaseManager.roomRef(roomCode)
            .child("players/$playerColor/connected")
        connectedRef.onDisconnect().setValue(false)
    }

    /**
     * 6 haneli rasgele oda kodu uretir.
     */
    private fun generateRoomCode(): String {
        return buildString {
            repeat(ROOM_CODE_LENGTH) {
                append(Random.nextInt(0, 10))
            }
        }
    }

    /**
     * Snapshot'tan RoomState parse eder.
     */
    private fun parseRoomState(snapshot: DataSnapshot): RoomState {
        if (!snapshot.exists()) {
            return RoomState(exists = false)
        }

        val status = snapshot.child("status").getValue(String::class.java) ?: "unknown"

        // Match config
        val configSnap = snapshot.child("matchConfig")
        val matchConfig = MatchConfig(
            targetScore = configSnap.child("targetScore").getValue(Int::class.java) ?: 11,
            gameType = configSnap.child("gameType").getValue(String::class.java) ?: "Modern",
            isRematchMode = configSnap.child("isRematchMode").getValue(Boolean::class.java) ?: false
        )

        // Players
        val whiteSnap = snapshot.child("players/white")
        val blackSnap = snapshot.child("players/black")
        val whitePlayer = if (whiteSnap.exists()) {
            OnlinePlayer(
                uid = whiteSnap.child("uid").getValue(String::class.java) ?: "",
                displayName = whiteSnap.child("displayName").getValue(String::class.java) ?: "",
                ready = whiteSnap.child("ready").getValue(Boolean::class.java) ?: false,
                connected = whiteSnap.child("connected").getValue(Boolean::class.java) ?: false
            )
        } else null

        val blackPlayer = if (blackSnap.exists()) {
            OnlinePlayer(
                uid = blackSnap.child("uid").getValue(String::class.java) ?: "",
                displayName = blackSnap.child("displayName").getValue(String::class.java) ?: "",
                ready = blackSnap.child("ready").getValue(Boolean::class.java) ?: false,
                connected = blackSnap.child("connected").getValue(Boolean::class.java) ?: false
            )
        } else null

        // Match scores
        val matchSnap = snapshot.child("match")
        val whiteScore = matchSnap.child("whiteScore").getValue(Int::class.java) ?: 0
        val blackScore = matchSnap.child("blackScore").getValue(Int::class.java) ?: 0
        val isCrawfordGame = matchSnap.child("isCrawfordGame").getValue(Boolean::class.java) ?: false
        val crawfordGamePlayed = matchSnap.child("crawfordGamePlayed").getValue(Boolean::class.java) ?: false
        val currentGameIndex = matchSnap.child("currentGameIndex").getValue(Int::class.java) ?: 0

        // Current game
        val gameSnap = snapshot.child("currentGame")
        val gameStatus = gameSnap.child("status").getValue(String::class.java) ?: "waiting"
        val turn = gameSnap.child("turn").getValue(String::class.java) ?: "white"

        // Board
        @Suppress("UNCHECKED_CAST")
        val boardMap = gameSnap.child("board").value as? Map<String, Any?> ?: emptyMap()
        val board = BoardState.fromMap(boardMap)

        // Dice
        @Suppress("UNCHECKED_CAST")
        val diceValues = (gameSnap.child("dice/values").value as? List<Long>)?.map { it.toInt() } ?: emptyList()

        // Starting dice
        val startWhite = gameSnap.child("startingDice/white").getValue(Int::class.java)
        val startBlack = gameSnap.child("startingDice/black").getValue(Int::class.java)
        val firstPlayer = gameSnap.child("startingDice/firstPlayer").getValue(String::class.java)

        // Doubling cube
        @Suppress("UNCHECKED_CAST")
        val cubeMap = gameSnap.child("doublingCube").value as? Map<String, Any?> ?: emptyMap()
        val cubeValue = (cubeMap["value"] as? Long)?.toInt() ?: 1
        val cubeOwner = cubeMap["owner"] as? String ?: "center"
        val pendingOffer = cubeMap["pendingOffer"] as? String

        // Turn moves
        @Suppress("UNCHECKED_CAST")
        val turnMovesList = gameSnap.child("turnMoves").value as? List<Map<String, Any?>> ?: emptyList()

        val winner = gameSnap.child("winner").getValue(String::class.java)
        val winType = gameSnap.child("winType").getValue(String::class.java)

        return RoomState(
            exists = true,
            roomCode = snapshot.key ?: "",
            status = status,
            matchConfig = matchConfig,
            whitePlayer = whitePlayer,
            blackPlayer = blackPlayer,
            whiteScore = whiteScore,
            blackScore = blackScore,
            isCrawfordGame = isCrawfordGame,
            crawfordGamePlayed = crawfordGamePlayed,
            currentGameIndex = currentGameIndex,
            gameStatus = gameStatus,
            turn = turn,
            board = board,
            diceValues = diceValues,
            startingDiceWhite = startWhite,
            startingDiceBlack = startBlack,
            firstPlayer = firstPlayer,
            cubeValue = cubeValue,
            cubeOwner = cubeOwner,
            pendingDoubleOffer = pendingOffer,
            turnMoves = turnMovesList,
            winner = winner,
            winType = winType
        )
    }
}

/** Katilma sonucu */
sealed class JoinResult {
    data class Success(val roomCode: String) : JoinResult()
    data class Error(val message: String) : JoinResult()
}

/** Mac ayarlari */
data class MatchConfig(
    val targetScore: Int = 11,
    val gameType: String = "Modern",
    val isRematchMode: Boolean = false
) {
    fun toMap(): Map<String, Any> = mapOf(
        "targetScore" to targetScore,
        "gameType" to gameType,
        "isRematchMode" to isRematchMode
    )
}

/** Online oyuncu */
data class OnlinePlayer(
    val uid: String,
    val displayName: String,
    val ready: Boolean,
    val connected: Boolean
)

/** Oda durumu */
data class RoomState(
    val exists: Boolean = true,
    val roomCode: String = "",
    val status: String = "unknown",
    val matchConfig: MatchConfig = MatchConfig(),
    val whitePlayer: OnlinePlayer? = null,
    val blackPlayer: OnlinePlayer? = null,
    val whiteScore: Int = 0,
    val blackScore: Int = 0,
    val isCrawfordGame: Boolean = false,
    val crawfordGamePlayed: Boolean = false,
    val currentGameIndex: Int = 0,
    val gameStatus: String = "waiting",
    val turn: String = "white",
    val board: BoardState = BoardState.initial(),
    val diceValues: List<Int> = emptyList(),
    val startingDiceWhite: Int? = null,
    val startingDiceBlack: Int? = null,
    val firstPlayer: String? = null,
    val cubeValue: Int = 1,
    val cubeOwner: String = "center",
    val pendingDoubleOffer: String? = null,
    val turnMoves: List<Map<String, Any?>> = emptyList(),
    val winner: String? = null,
    val winType: String? = null
)
