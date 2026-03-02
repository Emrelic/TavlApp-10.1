package com.tavla.tavlapp.online

import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tavla.tavlapp.engine.*
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
    val isWhitePlayer: Boolean = isWhite

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
    private var turnStartBoard = BoardState.initial()  // Tur basindaki orijinal tahta

    // Undo stack - her hamle oncesi snapshot kaydeder
    private data class MoveSnapshot(
        val board: BoardState,
        val remainingDice: List<Int>
    )
    private val moveUndoStack = mutableListOf<MoveSnapshot>()

    // Baglanti kopma job
    private var disconnectJob: Job? = null
    private var autoPassJob: Job? = null

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

        // Sira degisti mi?
        val myColorStr = if (isWhite) "white" else "black"
        val newIsMyTurn = state.turn == myColorStr
        val turnChanged = currentTurn != state.turn
        currentTurn = state.turn
        isMyTurn = newIsMyTurn

        // Tahta - aktif hamle veya undo islemi varken Firebase'den ezme
        if (pendingMoves.isEmpty() && moveUndoStack.isEmpty()) {
            boardState = state.board
            turnStartBoard = state.board.deepCopy()
            currentBoardForMoves = state.board.deepCopy()
        }

        // Zar
        val newDice = state.diceValues
        if (newDice != diceValues || turnChanged) {
            diceValues = newDice
            if (newDice.isNotEmpty() && pendingMoves.isEmpty()) {
                // Yeni zarlar geldi - kalan zarlari hesapla
                remainingDice = if (newDice.size == 2 && newDice[0] == newDice[1]) {
                    listOf(newDice[0], newDice[0], newDice[0], newDice[0])
                } else {
                    newDice.toList()
                }
                usedDice = List(remainingDice.size) { false }
                currentBoardForMoves = state.board.deepCopy()
                turnStartBoard = state.board.deepCopy()
                pendingMoves = emptyList()
                moveUndoStack.clear()
            }
        }

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
                // Hamle yapilamiyor - otomatik pas (onceki job'u iptal et)
                autoPassJob?.cancel()
                autoPassJob = viewModelScope.launch {
                    delay(1000)
                    gameManager.passTurn()
                }
            }
        } else {
            autoPassJob?.cancel()
        }
    }

    // ========== Kullanici Aksiyonlari ==========

    /**
     * Nokta tiklandiginda - 1 tikla otomatik hamle sistemi.
     * Pula tikla → ilk sIradaki zarla otomatik hareket eder.
     */
    fun onPointTapped(point: Int) {
        if (!isMyTurn || gamePhase != GamePhase.PLAYING || remainingDice.isEmpty()) return

        val player = gameManager.myColor
        val activeDie = remainingDice.first()

        // Barda tas varsa sadece bar tiklama
        if (currentBoardForMoves.hasOnBar(player)) {
            if (point != -1) return
            // Bardan giris: aktif zarla giris noktasini hesapla
            val entry = BackgammonEngine.barEntryPoint(player, activeDie)
            if (entry in 0..23 && !BackgammonEngine.isPointBlocked(currentBoardForMoves, entry, player)) {
                val isHit = BackgammonEngine.isHitMove(currentBoardForMoves, entry, player)
                val move = Move(from = -1, to = entry, dieUsed = activeDie, isHit = isHit)
                executeAutoMove(move)
            }
            return
        }

        if (point == -1) return  // Bar tiklanmis ama barda tas yok

        // Bu noktada benim tasim var mi?
        if (currentBoardForMoves.checkerCount(point, player) == 0) return

        // Aktif zarla hedefi hesapla
        val dest = BackgammonEngine.destinationPoint(point, activeDie, player)

        if (dest == -1) {
            // Tam bear off
            if (BackgammonEngine.canBearOff(currentBoardForMoves, player)) {
                val move = Move(from = point, to = -1, dieUsed = activeDie, isHit = false)
                executeAutoMove(move)
            }
        } else if (dest in 0..23) {
            // Normal hamle
            if (!BackgammonEngine.isPointBlocked(currentBoardForMoves, dest, player)) {
                val isHit = BackgammonEngine.isHitMove(currentBoardForMoves, dest, player)
                val move = Move(from = point, to = dest, dieUsed = activeDie, isHit = isHit)
                executeAutoMove(move)
            }
        } else {
            // Overshoot - en yuksek tastan cikarma
            if (BackgammonEngine.canBearOffWithExactOrHigher(currentBoardForMoves, player, point, activeDie)) {
                val move = Move(from = point, to = -1, dieUsed = activeDie, isHit = false)
                executeAutoMove(move)
            }
        }
    }

    /** Bear off bolgesi tiklandiginda */
    fun onBearOffTapped() {
        if (!isMyTurn || gamePhase != GamePhase.PLAYING || remainingDice.isEmpty()) return
        // Bear off bolgesi tiklaninca: secili nokta yoksa islem yok
        // Kullanici pula tiklamali, otomatik cikarma hesaplanir
    }

    /** Zar sirasini degistir (ornek: 4-2 → 2-4) */
    fun onSwapDice() {
        if (remainingDice.size < 2) return
        if (pendingMoves.isNotEmpty()) return  // Hamle yapildiysa swap yapilamaz
        // Ilk iki zari yer degistir
        val swapped = remainingDice.toMutableList()
        val temp = swapped[0]
        swapped[0] = swapped[1]
        swapped[1] = temp
        remainingDice = swapped
        updateUsedDice()
    }

    /** Otomatik hamle uygular (1-tikla sistemi) */
    private fun executeAutoMove(move: Move) {
        // Hamle oncesi durumu kaydet (undo icin)
        moveUndoStack.add(MoveSnapshot(
            board = currentBoardForMoves.deepCopy(),
            remainingDice = remainingDice.toList()
        ))

        // Hamleyi uygula
        currentBoardForMoves = BackgammonEngine.applyMove(currentBoardForMoves, move, gameManager.myColor)
        pendingMoves = pendingMoves + move

        // Kullanilan zari kaldir (ilk eslesen)
        val newRemaining = remainingDice.toMutableList()
        newRemaining.remove(move.dieUsed)
        remainingDice = newRemaining

        updateUsedDice()

        // UI state'i guncelle
        boardState = currentBoardForMoves.deepCopy()
    }

    /** Used dice durumunu gunceller */
    private fun updateUsedDice() {
        val allDice = if (diceValues.size == 2 && diceValues[0] == diceValues[1]) {
            listOf(diceValues[0], diceValues[0], diceValues[0], diceValues[0])
        } else {
            diceValues.toList()
        }
        val usedCount = allDice.size - remainingDice.size
        usedDice = List(allDice.size) { index -> index < usedCount }
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
        moveUndoStack.clear()
    }

    /** Son hamleyi geri al */
    fun onUndoLastMove() {
        if (moveUndoStack.isEmpty() || pendingMoves.isEmpty()) return

        val snapshot = moveUndoStack.removeAt(moveUndoStack.size - 1)
        pendingMoves = pendingMoves.dropLast(1)

        // Snapshot'tan durumu geri yukle
        currentBoardForMoves = snapshot.board
        remainingDice = snapshot.remainingDice
        boardState = currentBoardForMoves.deepCopy()

        // Secim durumunu temizle
        selectedPoint = null
        legalDestinations = emptyList()

        updateUsedDice()
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
