package com.tavla.tavlapp.online

import com.google.firebase.database.*
import com.tavla.tavlapp.engine.*
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

/**
 * Online oyun durumu yoneticisi.
 * Firebase Realtime Database ile oyun durumunu senkronize eder.
 */
class OnlineGameManager(
    private val roomCode: String,
    private val isWhite: Boolean
) {
    private val roomRef = FirebaseManager.roomRef(roomCode)
    private val gameRef = roomRef.child("currentGame")
    private val matchRef = roomRef.child("match")
    private val chatRef = roomRef.child("chat")

    val myColor: PlayerColor = if (isWhite) PlayerColor.WHITE else PlayerColor.BLACK

    // ========== Oyun Durumu Izleme ==========

    /** Oyun durumunu izler */
    fun observeGameState(): Flow<RoomState> = callbackFlow {
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val state = parseGameState(snapshot)
                trySend(state)
            }
            override fun onCancelled(error: DatabaseError) {
                close(error.toException())
            }
        }
        roomRef.addValueEventListener(listener)
        awaitClose { roomRef.removeEventListener(listener) }
    }

    /** Tahta durumunu izler */
    fun observeBoard(): Flow<BoardState> = callbackFlow {
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                @Suppress("UNCHECKED_CAST")
                val map = snapshot.value as? Map<String, Any?> ?: return
                trySend(BoardState.fromMap(map))
            }
            override fun onCancelled(error: DatabaseError) {
                close(error.toException())
            }
        }
        gameRef.child("board").addValueEventListener(listener)
        awaitClose { gameRef.child("board").removeEventListener(listener) }
    }

    /** Baglanti durumunu izler */
    fun observeConnection(): Flow<Boolean> = callbackFlow {
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                trySend(snapshot.getValue(Boolean::class.java) ?: false)
            }
            override fun onCancelled(error: DatabaseError) {
                close(error.toException())
            }
        }
        FirebaseManager.connectedRef().addValueEventListener(listener)
        awaitClose { FirebaseManager.connectedRef().removeEventListener(listener) }
    }

    /** Rakibin baglanti durumunu izler */
    fun observeOpponentConnection(): Flow<Boolean> = callbackFlow {
        val opponentPath = if (isWhite) "players/black/connected" else "players/white/connected"
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                trySend(snapshot.getValue(Boolean::class.java) ?: false)
            }
            override fun onCancelled(error: DatabaseError) {
                close(error.toException())
            }
        }
        roomRef.child(opponentPath).addValueEventListener(listener)
        awaitClose { roomRef.child(opponentPath).removeEventListener(listener) }
    }

    // ========== Zar (Istemci Tarafi) ==========

    /** Baslangic zari atar - istemci tarafinda uretilir, Firebase'e yazilir */
    suspend fun rollStartingDice() {
        val die = (1..6).random()
        val playerKey = if (isWhite) "white" else "black"

        // Kendi zarimi yaz
        gameRef.child("startingDice/$playerKey").setValue(die).await()

        // Her iki oyuncu da atti mi kontrol et
        val startingDiceSnap = gameRef.child("startingDice").get().await()
        val whiteDie = startingDiceSnap.child("white").getValue(Int::class.java)
        val blackDie = startingDiceSnap.child("black").getValue(Int::class.java)

        if (whiteDie != null && blackDie != null) {
            if (whiteDie != blackDie) {
                // Buyuk atan baslar
                val firstPlayer = if (whiteDie > blackDie) "white" else "black"
                val updates = mapOf<String, Any?>(
                    "currentGame/startingDice/firstPlayer" to firstPlayer,
                    "currentGame/turn" to firstPlayer,
                    "currentGame/dice/values" to listOf(whiteDie, blackDie),
                    "currentGame/status" to "playing"
                )
                roomRef.updateChildren(updates).await()
            } else {
                // Esit - sifirla, tekrar atilacak
                val updates = mapOf<String, Any?>(
                    "currentGame/startingDice/white" to null,
                    "currentGame/startingDice/black" to null,
                    "currentGame/startingDice/firstPlayer" to null
                )
                roomRef.updateChildren(updates).await()
            }
        }
    }

    /** Normal zar atar - istemci tarafinda uretilir, Firebase'e yazilir */
    suspend fun rollDice() {
        val die1 = (1..6).random()
        val die2 = (1..6).random()
        gameRef.child("dice/values").setValue(listOf(die1, die2)).await()
    }

    // ========== Hamle ==========

    /** Turu gonder (hamleleri uygula ve sirayi devret) */
    suspend fun submitTurn(
        moves: List<Move>,
        newBoard: BoardState,
        usedDice: List<Int>
    ) {
        val updates = mutableMapOf<String, Any?>()

        // Tahta durumunu guncelle
        updates["currentGame/board"] = newBoard.toMap()

        // Hamleleri kaydet
        updates["currentGame/turnMoves"] = moves.map { it.toMap() }

        // Zarlari temizle
        updates["currentGame/dice/values"] = emptyList<Int>()

        // Oyun bitti mi kontrol et
        if (BackgammonEngine.isGameOver(newBoard)) {
            val winner = BackgammonEngine.getWinner(newBoard)!!
            val winType = BackgammonEngine.getWinType(newBoard, winner)
            val winnerStr = if (winner == PlayerColor.WHITE) "white" else "black"

            updates["currentGame/winner"] = winnerStr
            updates["currentGame/winType"] = winType.code
            updates["currentGame/status"] = "finished"
        } else {
            // Sirayi devret
            val nextTurn = if (isWhite) "black" else "white"
            updates["currentGame/turn"] = nextTurn
        }

        roomRef.updateChildren(updates).await()
    }

    /** Bos tur gecer (hamle yapamiyorsa) */
    suspend fun passTurn() {
        val updates = mapOf<String, Any?>(
            "currentGame/dice/values" to emptyList<Int>(),
            "currentGame/turnMoves" to emptyList<Any>(),
            "currentGame/turn" to if (isWhite) "black" else "white"
        )
        roomRef.updateChildren(updates).await()
    }

    // ========== Katlama (Doubling) ==========

    /** Katlama teklif eder */
    suspend fun offerDouble() {
        val offerFrom = if (isWhite) "white" else "black"
        val updates = mapOf<String, Any?>(
            "currentGame/doublingCube/pendingOffer" to offerFrom,
            "currentGame/status" to "doubling"
        )
        roomRef.updateChildren(updates).await()
    }

    /** Katlama teklifine cevap verir */
    suspend fun respondToDouble(accept: Boolean) {
        if (accept) {
            // Kabul - kup degerini ikiye katla, kontrolu kabul eden alir
            val currentCubeSnap = gameRef.child("doublingCube/value").get().await()
            val currentValue = currentCubeSnap.getValue(Int::class.java) ?: 1
            val newValue = currentValue * 2
            val newOwner = if (isWhite) "white" else "black"

            val updates = mapOf<String, Any?>(
                "currentGame/doublingCube/value" to newValue,
                "currentGame/doublingCube/owner" to newOwner,
                "currentGame/doublingCube/pendingOffer" to null,
                "currentGame/status" to "playing"
            )
            roomRef.updateChildren(updates).await()
        } else {
            // Pes - teklif eden kazanir
            val winner = if (isWhite) "black" else "white"  // Pes eden kaybeder
            val cubeSnap = gameRef.child("doublingCube/value").get().await()
            val cubeValue = cubeSnap.getValue(Int::class.java) ?: 1

            val updates = mapOf<String, Any?>(
                "currentGame/winner" to winner,
                "currentGame/winType" to "T",  // Pes = Single
                "currentGame/status" to "finished",
                "currentGame/doublingCube/pendingOffer" to null
            )
            roomRef.updateChildren(updates).await()
        }
    }

    // ========== Mac Yonetimi ==========

    /** Oyun sonucunu isle ve yeni oyun baslat */
    suspend fun processGameEnd(
        winner: String,
        winType: String,
        cubeValue: Int
    ) {
        val matchSnap = matchRef.get().await()
        val whiteScore = matchSnap.child("whiteScore").getValue(Int::class.java) ?: 0
        val blackScore = matchSnap.child("blackScore").getValue(Int::class.java) ?: 0
        val currentGameIndex = matchSnap.child("currentGameIndex").getValue(Int::class.java) ?: 0
        val crawfordPlayed = matchSnap.child("crawfordGamePlayed").getValue(Boolean::class.java) ?: false

        // Puan hesapla
        val winTypeEnum = when (winType) {
            "M" -> WinType.MARS
            "B" -> WinType.BACKGAMMON
            else -> WinType.SINGLE
        }
        val score = BackgammonEngine.calculateScore(winTypeEnum, cubeValue)

        val newWhiteScore = if (winner == "white") whiteScore + score else whiteScore
        val newBlackScore = if (winner == "black") blackScore + score else blackScore

        // Oyun gecmisine ekle
        val gameHistoryRef = roomRef.child("gameHistory").push()
        gameHistoryRef.setValue(mapOf(
            "winner" to winner,
            "winType" to winType,
            "cubeValue" to cubeValue,
            "score" to score
        )).await()

        // Mac ayarlarini al
        val configSnap = roomRef.child("matchConfig/targetScore").get().await()
        val targetScore = configSnap.getValue(Int::class.java) ?: 11

        // Mac bitti mi?
        if (newWhiteScore >= targetScore || newBlackScore >= targetScore) {
            // Mac bitti
            val updates = mapOf<String, Any?>(
                "match/whiteScore" to newWhiteScore,
                "match/blackScore" to newBlackScore,
                "status" to "finished"
            )
            roomRef.updateChildren(updates).await()
            return
        }

        // Crawford kontrolu
        val isCrawford = matchSnap.child("isCrawfordGame").getValue(Boolean::class.java) ?: false
        var newCrawfordGame = false
        var newCrawfordPlayed = crawfordPlayed

        if (isCrawford) {
            newCrawfordPlayed = true
        }

        // Bir sonraki oyun Crawford mi?
        if (!newCrawfordPlayed) {
            if (newWhiteScore == targetScore - 1 || newBlackScore == targetScore - 1) {
                newCrawfordGame = true
            }
        }

        // Yeni oyun baslat
        val newBoard = BoardState.initial()
        val updates = mapOf<String, Any?>(
            "match/whiteScore" to newWhiteScore,
            "match/blackScore" to newBlackScore,
            "match/currentGameIndex" to currentGameIndex + 1,
            "match/isCrawfordGame" to newCrawfordGame,
            "match/crawfordGamePlayed" to newCrawfordPlayed,
            "currentGame/status" to "rolling_start",
            "currentGame/turn" to "white",
            "currentGame/board" to newBoard.toMap(),
            "currentGame/dice/values" to emptyList<Int>(),
            "currentGame/startingDice" to mapOf<String, Any?>(),
            "currentGame/doublingCube/value" to 1,
            "currentGame/doublingCube/owner" to "center",
            "currentGame/doublingCube/pendingOffer" to null,
            "currentGame/turnMoves" to emptyList<Any>(),
            "currentGame/winner" to null,
            "currentGame/winType" to null
        )
        roomRef.updateChildren(updates).await()
    }

    // ========== Chat ==========

    /** Chat mesaji gonderir */
    suspend fun sendChatMessage(message: String, senderName: String) {
        val uid = FirebaseManager.uid ?: return
        val messageData = mapOf(
            "uid" to uid,
            "name" to senderName,
            "message" to message.take(200),  // Max 200 karakter
            "timestamp" to ServerValue.TIMESTAMP
        )
        chatRef.push().setValue(messageData).await()
    }

    /** Chat mesajlarini izler */
    fun observeChat(): Flow<List<ChatMessage>> = callbackFlow {
        val listener = object : ChildEventListener {
            val messages = mutableListOf<ChatMessage>()

            override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                val msg = parseChatMessage(snapshot) ?: return
                messages.add(msg)
                trySend(messages.toList())
            }
            override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {}
            override fun onChildRemoved(snapshot: DataSnapshot) {}
            override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {}
            override fun onCancelled(error: DatabaseError) {
                close(error.toException())
            }
        }
        val query = chatRef.orderByChild("timestamp")
        query.addChildEventListener(listener)
        awaitClose { query.removeEventListener(listener) }
    }

    private fun parseChatMessage(snapshot: DataSnapshot): ChatMessage? {
        val uid = snapshot.child("uid").getValue(String::class.java) ?: return null
        val name = snapshot.child("name").getValue(String::class.java) ?: return null
        val message = snapshot.child("message").getValue(String::class.java) ?: return null
        val timestamp = snapshot.child("timestamp").getValue(Long::class.java) ?: 0L
        val isMine = uid == FirebaseManager.uid
        return ChatMessage(uid, name, message, timestamp, isMine)
    }

    // ========== Baglanti Kopma ==========

    /** Baglanti durumunu gunceller */
    suspend fun setConnected(connected: Boolean) {
        val playerPath = if (isWhite) "players/white/connected" else "players/black/connected"
        roomRef.child(playerPath).setValue(connected).await()
    }

    /** Rakip kopunca galip ilan eder */
    suspend fun claimWinByDisconnect() {
        val winner = if (isWhite) "white" else "black"
        val updates = mapOf<String, Any?>(
            "currentGame/winner" to winner,
            "currentGame/winType" to "T",
            "currentGame/status" to "finished",
            "status" to "finished"
        )
        roomRef.updateChildren(updates).await()
    }

    // ========== Yardimci ==========

    private fun parseGameState(snapshot: DataSnapshot): RoomState {
        // RoomManager'daki ayni parse mantigi
        if (!snapshot.exists()) return RoomState(exists = false)

        val status = snapshot.child("status").getValue(String::class.java) ?: "unknown"

        val configSnap = snapshot.child("matchConfig")
        val matchConfig = MatchConfig(
            targetScore = configSnap.child("targetScore").getValue(Int::class.java) ?: 11,
            gameType = configSnap.child("gameType").getValue(String::class.java) ?: "Modern",
            isRematchMode = configSnap.child("isRematchMode").getValue(Boolean::class.java) ?: false
        )

        val whiteSnap = snapshot.child("players/white")
        val blackSnap = snapshot.child("players/black")

        val whitePlayer = if (whiteSnap.exists()) OnlinePlayer(
            uid = whiteSnap.child("uid").getValue(String::class.java) ?: "",
            displayName = whiteSnap.child("displayName").getValue(String::class.java) ?: "",
            ready = whiteSnap.child("ready").getValue(Boolean::class.java) ?: false,
            connected = whiteSnap.child("connected").getValue(Boolean::class.java) ?: false
        ) else null

        val blackPlayer = if (blackSnap.exists()) OnlinePlayer(
            uid = blackSnap.child("uid").getValue(String::class.java) ?: "",
            displayName = blackSnap.child("displayName").getValue(String::class.java) ?: "",
            ready = blackSnap.child("ready").getValue(Boolean::class.java) ?: false,
            connected = blackSnap.child("connected").getValue(Boolean::class.java) ?: false
        ) else null

        val matchSnap = snapshot.child("match")
        val gameSnap = snapshot.child("currentGame")

        @Suppress("UNCHECKED_CAST")
        val boardMap = gameSnap.child("board").value as? Map<String, Any?> ?: emptyMap()

        @Suppress("UNCHECKED_CAST")
        val diceValues = (gameSnap.child("dice/values").value as? List<Long>)?.map { it.toInt() } ?: emptyList()

        val cubeMap = gameSnap.child("doublingCube")

        return RoomState(
            exists = true,
            roomCode = snapshot.key ?: "",
            status = status,
            matchConfig = matchConfig,
            whitePlayer = whitePlayer,
            blackPlayer = blackPlayer,
            whiteScore = matchSnap.child("whiteScore").getValue(Int::class.java) ?: 0,
            blackScore = matchSnap.child("blackScore").getValue(Int::class.java) ?: 0,
            isCrawfordGame = matchSnap.child("isCrawfordGame").getValue(Boolean::class.java) ?: false,
            crawfordGamePlayed = matchSnap.child("crawfordGamePlayed").getValue(Boolean::class.java) ?: false,
            currentGameIndex = matchSnap.child("currentGameIndex").getValue(Int::class.java) ?: 0,
            gameStatus = gameSnap.child("status").getValue(String::class.java) ?: "waiting",
            turn = gameSnap.child("turn").getValue(String::class.java) ?: "white",
            board = BoardState.fromMap(boardMap),
            diceValues = diceValues,
            startingDiceWhite = gameSnap.child("startingDice/white").getValue(Int::class.java),
            startingDiceBlack = gameSnap.child("startingDice/black").getValue(Int::class.java),
            firstPlayer = gameSnap.child("startingDice/firstPlayer").getValue(String::class.java),
            cubeValue = cubeMap.child("value").getValue(Int::class.java) ?: 1,
            cubeOwner = cubeMap.child("owner").getValue(String::class.java) ?: "center",
            pendingDoubleOffer = cubeMap.child("pendingOffer").getValue(String::class.java),
            winner = gameSnap.child("winner").getValue(String::class.java),
            winType = gameSnap.child("winType").getValue(String::class.java)
        )
    }
}

/** Chat mesaji */
data class ChatMessage(
    val uid: String,
    val name: String,
    val message: String,
    val timestamp: Long,
    val isMine: Boolean
)
