package com.tavla.tavlapp.data

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

/**
 * FIBO Tavla/Backgammon Süre Kontrol Sistemi
 * Uluslararası turnuva kuralları: 90 saniye rezerv + 12 saniye hamle süresi
 */
class BackgammonTimeControl(
    private val reserveTimeSeconds: Int = 90, // FIBO standardı: 90 saniye
    private val moveDelaySeconds: Int = 12,   // FIBO standardı: 12 saniye hamle süresi
    private val onTimeUpdate: (player: Player, reserveTime: Duration, currentMoveTime: Duration, isActive: Boolean) -> Unit = { _, _, _, _ -> },
    private val onTimeExpired: (player: Player) -> Unit = { _ -> }
) {
    
    enum class Player {
        PLAYER1, PLAYER2
    }
    
    data class PlayerTimeState(
        var reserveTime: Duration,
        var currentMoveTime: Duration,
        var isActive: Boolean = false,
        var timerJob: Job? = null
    )
    
    private val player1State = PlayerTimeState(
        reserveTime = reserveTimeSeconds.seconds,
        currentMoveTime = moveDelaySeconds.seconds
    )
    private val player2State = PlayerTimeState(
        reserveTime = reserveTimeSeconds.seconds,
        currentMoveTime = moveDelaySeconds.seconds
    )
    private val coroutineScope = CoroutineScope(Dispatchers.Main)
    
    private var gameStarted = false
    private var gamePaused = false
    
    /**
     * Oyunu başlatır ve ilk oyuncuyu aktif hale getirir
     */
    fun startGame(firstPlayer: Player) {
        if (gameStarted) return
        
        gameStarted = true
        gamePaused = false
        
        // Süreleri sıfırla
        resetTimes()
        
        // İlk oyuncuyu başlat
        switchToPlayer(firstPlayer)
    }
    
    /**
     * Aktif oyuncuyu değiştirir
     */
    fun switchToPlayer(player: Player) {
        if (!gameStarted || gamePaused) return
        
        // Mevcut aktif zamanlayıcıyı durdur
        stopAllTimers()
        
        when (player) {
            Player.PLAYER1 -> {
                player1State.isActive = true
                player2State.isActive = false
                startPlayerTimer(player1State, Player.PLAYER1)
            }
            Player.PLAYER2 -> {
                player2State.isActive = true
                player1State.isActive = false
                startPlayerTimer(player2State, Player.PLAYER2)
            }
        }
    }
    
    /**
     * Hamle tamamlandığında çağrılır - süreyi sıfırlar ve diğer oyuncuya geçer
     */
    fun completeMoveAndSwitchPlayer() {
        val currentPlayer = if (player1State.isActive) Player.PLAYER1 else Player.PLAYER2
        val nextPlayer = if (currentPlayer == Player.PLAYER1) Player.PLAYER2 else Player.PLAYER1
        
        // Aktif oyuncunun hamle süresini sıfırla
        if (currentPlayer == Player.PLAYER1) {
            player1State.currentMoveTime = moveDelaySeconds.seconds
        } else {
            player2State.currentMoveTime = moveDelaySeconds.seconds
        }
        
        // Diğer oyuncuya geç
        switchToPlayer(nextPlayer)
    }
    
    /**
     * Oyunu duraklatır
     */
    fun pauseGame() {
        gamePaused = true
        stopAllTimers()
    }
    
    /**
     * Oyunu devam ettirir
     */
    fun resumeGame() {
        if (!gameStarted) return
        
        gamePaused = false
        val activePlayer = if (player1State.isActive) Player.PLAYER1 else Player.PLAYER2
        switchToPlayer(activePlayer)
    }
    
    /**
     * Oyunu durdurur ve süreleri sıfırlar
     */
    fun stopGame() {
        gameStarted = false
        gamePaused = false
        stopAllTimers()
        resetTimes()
    }
    
    /**
     * Oyuncunun mevcut durumunu döndürür
     */
    fun getPlayerState(player: Player): PlayerTimeState {
        return when (player) {
            Player.PLAYER1 -> player1State.copy()
            Player.PLAYER2 -> player2State.copy()
        }
    }
    
    /**
     * FIBO kurallarına göre süre bilgisi - formatlanmış string
     */
    fun getFormattedTime(player: Player): String {
        val state = getPlayerState(player)
        val reserveMinutes = state.reserveTime.inWholeMinutes
        val reserveSeconds = state.reserveTime.inWholeSeconds % 60
        val moveSeconds = state.currentMoveTime.inWholeSeconds
        
        return "Rezerv: ${reserveMinutes}:${reserveSeconds.toString().padStart(2, '0')}\nHamle: ${moveSeconds}s"
    }
    
    private fun startPlayerTimer(playerState: PlayerTimeState, player: Player) {
        playerState.timerJob = coroutineScope.launch {
            while (playerState.isActive && !gamePaused) {
                delay(100.milliseconds) // Her 100ms'de güncelle
                
                // Önce hamle süresini azalt
                if (playerState.currentMoveTime > Duration.ZERO) {
                    playerState.currentMoveTime -= 100.milliseconds
                } else {
                    // Hamle süresi bittiyse rezerv süreyi azalt
                    if (playerState.reserveTime > Duration.ZERO) {
                        playerState.reserveTime -= 100.milliseconds
                    } else {
                        // Süre bitti
                        playerState.isActive = false
                        onTimeExpired(player)
                        return@launch
                    }
                }
                
                // UI'yi güncelle
                onTimeUpdate(
                    player, 
                    playerState.reserveTime, 
                    playerState.currentMoveTime, 
                    playerState.isActive
                )
            }
        }
    }
    
    private fun stopAllTimers() {
        player1State.timerJob?.cancel()
        player2State.timerJob?.cancel()
        player1State.timerJob = null
        player2State.timerJob = null
    }
    
    private fun resetTimes() {
        player1State.reserveTime = reserveTimeSeconds.seconds
        player1State.currentMoveTime = moveDelaySeconds.seconds
        player1State.isActive = false
        
        player2State.reserveTime = reserveTimeSeconds.seconds
        player2State.currentMoveTime = moveDelaySeconds.seconds
        player2State.isActive = false
        
        // UI'yi güncelle
        onTimeUpdate(Player.PLAYER1, player1State.reserveTime, player1State.currentMoveTime, false)
        onTimeUpdate(Player.PLAYER2, player2State.reserveTime, player2State.currentMoveTime, false)
    }
    
    /**
     * FIBO turnuva formatı bilgileri
     */
    companion object {
        const val FIBO_RESERVE_TIME_SECONDS = 90
        const val FIBO_MOVE_DELAY_SECONDS = 12
        
        /**
         * FIBO kuralları açıklaması
         */
        fun getFIBORulesDescription(): String {
            return """
                FIBO Tavla/Backgammon Süre Kuralları:
                
                • Rezerv Süre: $FIBO_RESERVE_TIME_SECONDS saniye (1:30)
                • Hamle Süresi: $FIBO_MOVE_DELAY_SECONDS saniye
                • Sistem: Bronstein benzeri gecikme sistemi
                
                Nasıl Çalışır:
                1. Her hamle için $FIBO_MOVE_DELAY_SECONDS saniye verilir
                2. Bu süre biterse rezerv süreden düşer
                3. Rezerv süre biterse oyuncu kaybeder
                4. Hamle tamamlanınca süre sıfırlanır
                
                Profesyonel turnuvalarda kullanılan standarttır.
            """.trimIndent()
        }
    }
}