package com.tavla.tavlapp.online

import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tavla.tavlapp.engine.*
import com.tavla.tavlapp.ui.board.BoardInteraction
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * Online oyun ViewModel'i.
 * MVVM state yonetimi - UI state, oyuncu aksiyonlari ve oyun mantigi.
 */
class OnlineGameViewModel(
    private val roomCode: String,
    private val isWhite: Boolean,
    private val displayName: String
) : ViewModel() {

    private val gameManager = OnlineGameManager(roomCode, isWhite)

    // ========== UI State ==========
    var boardState by mutableStateOf(BoardState.initial())
        private set
    var diceValues by mutableStateOf<List<Int>>(emptyList())
        private set
    var remainingDice by mutableStateOf<List<Int>>(emptyList())
        private set
    var usedDice by mutableStateOf<List<Boolean>>(emptyList())
        private set
    var currentTurn by mutableStateOf("white")
        private set
    var isMyTurn by mutableStateOf(false)
        private set
    var gamePhase by mutableStateOf(GamePhase.WAITING)
        private set

    // Secim durumu
    var selectedPoint by mutableStateOf<Int?>(null)
        private set
    var legalDestinations by mutableStateOf<List<Int>>(emptyList())
        private set
    var pendingMoves by mutableStateOf<List<Move>>(emptyList())
        private set

    // Skor durumu
    var myScore by mutableStateOf(0)
        private set
    var opponentScore by mutableStateOf(0)
        private set
    var doublingCubeValue by mutableStateOf(1)
        private set
    var cubeOwner by mutableStateOf("center")
        private set
    var pendingDoubleOffer by mutableStateOf<String?>(null)
        private set
    var isCrawfordGame by mutableStateOf(false)
        private set

    // Mac bilgisi
    var myName by mutableStateOf(displayName)
        private set
    var opponentName by mutableStateOf("")
        private set
    var targetScore by mutableStateOf(11)
        private set
    var gameType by mutableStateOf("Modern")
        private set

    // Baglanti
    var isConnected by mutableStateOf(true)
        private set
    var isOpponentConnected by mutableStateOf(true)
        private set
    var disconnectCountdown by mutableStateOf<Int?>(null)
        private set

    // Chat
    var chatMessages by mutableStateOf<List<ChatMessage>>(emptyList())
        private set
    var chatUnreadCount by mutableStateOf(0)
        private set
    var isChatOpen by mutableStateOf(false)
        private set

    // Baslangic zari
    var startingDiceWhite by mutableStateOf<Int?>(null)
        private set
    var startingDiceBlack by mutableStateOf<Int?>(null)
        private set

    // Oyun sonu
    var winner by mutableStateOf<String?>(null)
        private set
    var winType by mutableStateOf<String?>(null)
        private set
    var matchFinished by mutableStateOf(false)
        private set

    // Zar atma durumu
    var isRolling by mutableStateOf(false)
        private set

    // Oyun motoru tahta durumu (hamle takibi icin)
    private var currentBoardForMoves = BoardState.initial()

    // Baglanti kopma job
    private var disconnectJob: Job? = null

    // Board interaction
    val boardInteraction = BoardInteraction(
        onMoveExecuted = { move -> onMoveExecuted(move) },
        onSelectionChanged = { point, destinations ->
            selectedPoint = point
            legalDestinations = destinations
        }
    )

    init {
        observeGameState()
        observeChat()
        observeConnection()
    }

    // ========== Oyun Durumu Izleme ==========

    private fun observeGameState() {
        viewModelScope.launch {
            gameManager.observeGameState().collectLatest { state ->
                updateFromRoomState(state)
            }
        }
    }

    private fun observeChat() {
        viewModelScope.launch {
            gameManager.observeChat().collectLatest { messages ->
                val hadMessages = chatMessages.size
                chatMessages = messages
                if (!isChatOpen && messages.size > hadMessages) {
                    chatUnreadCount += messages.size - hadMessages
                }
            }
        }
    }

    private fun observeConnection() {
        viewModelScope.launch {
            gameManager.observeConnection().collectLatest { connected ->
                isConnected = connected
                if (connected) {
                    gameManager.setConnected(true)
                }
            }
        }
        viewModelScope.launch {
            gameManager.observeOpponentConnection().collectLatest { connected ->
                isOpponentConnected = connected
                if (!connected && gamePhase == GamePhase.PLAYING) {
                    startDisconnectCountdown()
                } else {
                    cancelDisconnectCountdown()
                }
            }
        }
    }

    private fun updateFromRoomState(state: RoomState) {
        if (!state.exists) return

        // Mac bilgileri
        targetScore = state.matchConfig.targetScore
        gameType = state.matchConfig.gameType

        // Oyuncu isimleri
        if (isWhite) {
            myName = state.whitePlayer?.displayName ?: displayName
            opponentName = state.blackPlayer?.displayName ?: ""
        } else {
            myName = state.blackPlayer?.displayName ?: displayName
            opponentName = state.whitePlayer?.displayName ?: ""
        }

        // Skor
        myScore = if (isWhite) state.whiteScore else state.blackScore
        opponentScore = if (isWhite) state.blackScore else state.whiteScore

        // Crawford
        isCrawfordGame = state.isCrawfordGame

        // Katlama zari
        doublingCubeValue = state.cubeValue
        cubeOwner = state.cubeOwner
        pendingDoubleOffer = state.pendingDoubleOffer

        // Tahta
        boardState = state.board

        // Zar
        val newDice = state.diceValues
        if (newDice != diceValues) {
            diceValues = newDice
            if (newDice.isNotEmpty() && pendingMoves.isEmpty()) {
                // Yeni zarlar geldi - kalan zarlari hesapla
                remainingDice = if (newDice.size == 2 && newDice[0] == newDice[1]) {
                    listOf(newDice[0], newDice[0], newDice[0], newDice[0])
                } else {
                    newDice.toList()
                }
                usedDice = List(remainingDice.size) { false }
                currentBoardForMoves = state.board
                pendingMoves = emptyList()
            }
        }

        // Sira
        currentTurn = state.turn
        val myColorStr = if (isWhite) "white" else "black"
        isMyTurn = state.turn == myColorStr

        // Baslangic zarlari
        startingDiceWhite = state.startingDiceWhite
        startingDiceBlack = state.startingDiceBlack

        // Oyun fazi
        gamePhase = when (state.gameStatus) {
            "waiting" -> GamePhase.WAITING
            "rolling_start" -> GamePhase.ROLLING_START
            "playing" -> GamePhase.PLAYING
            "doubling" -> GamePhase.DOUBLING
            "finished" -> {
                winner = state.winner
                winType = state.winType
                GamePhase.GAME_OVER
            }
            else -> GamePhase.PLAYING
        }

        // Mac bitti mi?
        if (state.status == "finished") {
            matchFinished = true
        }

        // Baglanti durumu
        isOpponentConnected = if (isWhite) {
            state.blackPlayer?.connected ?: false
        } else {
            state.whitePlayer?.connected ?: false
        }

        // Hamle yapilabilirlik kontrolu
        if (gamePhase == GamePhase.PLAYING && isMyTurn && remainingDice.isNotEmpty()) {
            if (!MoveGenerator.hasAnyLegalMove(currentBoardForMoves, gameManager.myColor, remainingDice)) {
                // Hamle yapilamiyor - otomatik pas
                viewModelScope.launch {
                    delay(1000)
                    gameManager.passTurn()
                }
            }
        }
    }

    // ========== Kullanici Aksiyonlari ==========

    /** Nokta tiklandiginda */
    fun onPointTapped(point: Int) {
        if (!isMyTurn || gamePhase != GamePhase.PLAYING || remainingDice.isEmpty()) return
        boardInteraction.onPointTapped(point, currentBoardForMoves, gameManager.myColor, remainingDice)
    }

    /** Bear off tiklandiginda */
    fun onBearOffTapped() {
        if (!isMyTurn || gamePhase != GamePhase.PLAYING || remainingDice.isEmpty()) return
        boardInteraction.onBearOffTapped(currentBoardForMoves, gameManager.myColor, remainingDice)
    }

    /** Hamle yapildiginda (BoardInteraction'dan callback) */
    private fun onMoveExecuted(move: Move) {
        // Hamleyi uygula
        currentBoardForMoves = BackgammonEngine.applyMove(currentBoardForMoves, move, gameManager.myColor)
        pendingMoves = pendingMoves + move

        // Kullanilan zari kaldir
        val newRemaining = remainingDice.toMutableList()
        newRemaining.remove(move.dieUsed)
        remainingDice = newRemaining

        // Used dice durumunu guncelle
        val allDice = if (diceValues.size == 2 && diceValues[0] == diceValues[1]) {
            listOf(diceValues[0], diceValues[0], diceValues[0], diceValues[0])
        } else {
            diceValues.toList()
        }
        usedDice = allDice.mapIndexed { index, _ ->
            index >= remainingDice.size || !remainingDice.contains(allDice[index])
        }

        // Daha fazla hamle var mi?
        if (remainingDice.isEmpty() ||
            !MoveGenerator.hasAnyLegalMove(currentBoardForMoves, gameManager.myColor, remainingDice)
        ) {
            // Otomatik tur gonder
            viewModelScope.launch {
                delay(300)
                submitTurn()
            }
        }

        // UI state'i guncelle
        boardState = currentBoardForMoves
    }

    /** Turu gonder */
    fun onConfirmTurn() {
        viewModelScope.launch { submitTurn() }
    }

    private suspend fun submitTurn() {
        if (pendingMoves.isEmpty()) {
            gameManager.passTurn()
        } else {
            gameManager.submitTurn(pendingMoves, currentBoardForMoves, diceValues)
        }
        pendingMoves = emptyList()
        remainingDice = emptyList()
        usedDice = emptyList()
        selectedPoint = null
        legalDestinations = emptyList()
        boardInteraction.clearSelection()
    }

    /** Son hamleyi geri al */
    fun onUndoLastMove() {
        if (pendingMoves.isEmpty()) return

        val lastMove = pendingMoves.last()
        pendingMoves = pendingMoves.dropLast(1)

        // Kalan zarlari guncelle
        remainingDice = remainingDice + lastMove.dieUsed

        // Tahtayi yeniden hesapla
        currentBoardForMoves = boardState // Orijinal board state'e don
        for (move in pendingMoves) {
            currentBoardForMoves = BackgammonEngine.applyMove(currentBoardForMoves, move, gameManager.myColor)
        }

        // Used dice guncelle
        val allDice = if (diceValues.size == 2 && diceValues[0] == diceValues[1]) {
            listOf(diceValues[0], diceValues[0], diceValues[0], diceValues[0])
        } else {
            diceValues.toList()
        }
        usedDice = allDice.mapIndexed { index, _ ->
            index >= remainingDice.size || !remainingDice.contains(allDice[index])
        }

        boardInteraction.clearSelection()
    }

    /** Zar at */
    fun onRollDice() {
        if (isRolling) return
        isRolling = true

        viewModelScope.launch {
            try {
                if (gamePhase == GamePhase.ROLLING_START) {
                    gameManager.rollStartingDice()
                } else {
                    gameManager.rollDice()
                }
            } catch (e: Exception) {
                // Hata durumunda
            } finally {
                isRolling = false
            }
        }
    }

    /** Katlama teklif et */
    fun onOfferDouble() {
        if (!isMyTurn || isCrawfordGame || gamePhase != GamePhase.PLAYING) return

        // Kup kontrolu - sadece kup ortadaysa veya bende ise teklif edebilirim
        val myColorStr = if (isWhite) "white" else "black"
        if (cubeOwner != "center" && cubeOwner != myColorStr) return

        viewModelScope.launch {
            gameManager.offerDouble()
        }
    }

    /** Katlama teklifini kabul et */
    fun onAcceptDouble() {
        viewModelScope.launch {
            gameManager.respondToDouble(true)
        }
    }

    /** Katlama teklifinde pes et */
    fun onDeclineDouble() {
        viewModelScope.launch {
            gameManager.respondToDouble(false)
        }
    }

    /** Oyun sonunu isle */
    fun onProcessGameEnd() {
        val w = winner ?: return
        val wt = winType ?: return

        viewModelScope.launch {
            gameManager.processGameEnd(w, wt, doublingCubeValue)
        }
    }

    // ========== Chat ==========

    fun onSendChat(message: String) {
        if (message.isBlank()) return
        viewModelScope.launch {
            gameManager.sendChatMessage(message.trim(), myName)
        }
    }

    fun onToggleChat() {
        isChatOpen = !isChatOpen
        if (isChatOpen) {
            chatUnreadCount = 0
        }
    }

    // ========== Baglanti Kopma ==========

    private fun startDisconnectCountdown() {
        disconnectJob?.cancel()
        disconnectJob = viewModelScope.launch {
            for (i in 60 downTo 0) {
                disconnectCountdown = i
                delay(1000)
            }
            // 60 saniye doldu - galip ilan et
            gameManager.claimWinByDisconnect()
            disconnectCountdown = null
        }
    }

    private fun cancelDisconnectCountdown() {
        disconnectJob?.cancel()
        disconnectJob = null
        disconnectCountdown = null
    }

    override fun onCleared() {
        super.onCleared()
        disconnectJob?.cancel()
    }
}

enum class GamePhase {
    WAITING,        // Rakip bekleniyor
    ROLLING_START,  // Baslangic zari
    PLAYING,        // Oyun devam ediyor
    DOUBLING,       // Katlama teklifi
    GAME_OVER       // Oyun bitti
}
