package com.tavla.tavlapp

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import kotlin.random.Random

class DiceActivity : AppCompatActivity() {
    
    // UI Elements
    private lateinit var player1Timer: TextView
    private lateinit var player2Timer: TextView
    private lateinit var player1Name: TextView
    private lateinit var player2Name: TextView
    private lateinit var dice1Left: ImageView
    private lateinit var dice2Left: ImageView
    private lateinit var dice1Right: ImageView
    private lateinit var dice2Right: ImageView
    private lateinit var statsText: TextView
    private lateinit var checkAllPlayed: CheckBox
    private lateinit var checkAllMissed: CheckBox
    private lateinit var diceCountNumber: TextView
    
    // Game State
    private var player1Time = 300 // 5 dakika
    private var player2Time = 300
    private var currentPlayer = 1
    private var timerRunning = false
    private val diceStats = mutableMapOf<String, Int>()
    private var diceCount = 0
    
    // Timer
    private val handler = Handler(Looper.getMainLooper())
    private lateinit var timerRunnable: Runnable
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dice)
        
        // Full screen
        window.decorView.systemUiVisibility = (
            View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
            View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or
            View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
            View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
            View.SYSTEM_UI_FLAG_FULLSCREEN or
            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
        )
        
        // Intent parametreleri
        val gameType = intent.getStringExtra("game_type") ?: "Modern"
        val useDiceRoller = intent.getBooleanExtra("use_dice_roller", false)
        val useTimer = intent.getBooleanExtra("use_timer", false)
        val player1NameText = intent.getStringExtra("player1_name") ?: "Oyuncu 1"
        val player2NameText = intent.getStringExtra("player2_name") ?: "Oyuncu 2"
        
        initViews()
        setupUI(player1NameText, player2NameText, useTimer)
        setupClickListeners()
        
        if (useTimer) {
            startTimer()
        }
    }
    
    private fun initViews() {
        player1Timer = findViewById(R.id.player1Timer)
        player2Timer = findViewById(R.id.player2Timer)
        player1Name = findViewById(R.id.player1Name)
        player2Name = findViewById(R.id.player2Name)
        dice1Left = findViewById(R.id.dice1Left)
        dice2Left = findViewById(R.id.dice2Left)
        dice1Right = findViewById(R.id.dice1Right)
        dice2Right = findViewById(R.id.dice2Right)
        statsText = findViewById(R.id.statsText)
        checkAllPlayed = findViewById(R.id.checkAllPlayed)
        checkAllMissed = findViewById(R.id.checkAllMissed)
        diceCountNumber = findViewById(R.id.diceCountNumber)
    }
    
    private fun setupUI(player1NameText: String, player2NameText: String, useTimer: Boolean) {
        player1Name.text = player1NameText
        player2Name.text = player2NameText
        
        if (!useTimer) {
            player1Timer.visibility = View.GONE
            player2Timer.visibility = View.GONE
        }
        
        updateTimerDisplay()
        updateStatsDisplay()
        diceCountNumber.text = diceCount.toString()
    }
    
    private fun setupClickListeners() {
        // Sol zarlar (Player 1)
        dice1Left.setOnClickListener { rollDice() }
        dice2Left.setOnClickListener { rollDice() }
        
        // Sağ zarlar (Player 2)
        dice1Right.setOnClickListener { rollDice() }
        dice2Right.setOnClickListener { rollDice() }
        
        // Checkboxlar
        checkAllPlayed.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) checkAllMissed.isChecked = false
        }
        
        checkAllMissed.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) checkAllPlayed.isChecked = false
        }
    }
    
    private fun rollDice() {
        val dice1Value = Random.nextInt(1, 7)
        val dice2Value = Random.nextInt(1, 7)
        
        // Zar görsellerini güncelle
        updateDiceImages(dice1Value, dice2Value)
        
        // İstatistik güncelle
        val diceKey = if (dice1Value <= dice2Value) "${dice1Value}-${dice2Value}" else "${dice2Value}-${dice1Value}"
        diceStats[diceKey] = (diceStats[diceKey] ?: 0) + 1
        diceCount++
        
        updateStatsDisplay()
        diceCountNumber.text = diceCount.toString()
        
        // Sırayı değiştir
        currentPlayer = if (currentPlayer == 1) 2 else 1
        updateTimerDisplay()
    }
    
    private fun updateDiceImages(dice1: Int, dice2: Int) {
        val dice1Resource = getDiceResource(dice1)
        val dice2Resource = getDiceResource(dice2)
        
        // Tüm zarlara aynı değerleri ata (senin tasarımında tüm zarlar aynı)
        dice1Left.setImageResource(dice1Resource)
        dice2Left.setImageResource(dice2Resource)
        dice1Right.setImageResource(dice1Resource)
        dice2Right.setImageResource(dice2Resource)
    }
    
    private fun getDiceResource(value: Int): Int {
        return when (value) {
            1 -> R.drawable.dice_1
            2 -> R.drawable.dice_2
            3 -> R.drawable.dice_3
            4 -> R.drawable.dice_4
            5 -> R.drawable.dice_5
            6 -> R.drawable.dice_6
            else -> R.drawable.dice_1
        }
    }
    
    private fun updateStatsDisplay() {
        val statsBuilder = StringBuilder()
        diceStats.forEach { (dice, count) ->
            statsBuilder.append("$dice: $count\n")
        }
        statsText.text = if (statsBuilder.isNotEmpty()) statsBuilder.toString().trim() else "Henüz zar atılmadı"
    }
    
    private fun startTimer() {
        timerRunning = true
        timerRunnable = object : Runnable {
            override fun run() {
                if (timerRunning) {
                    if (currentPlayer == 1 && player1Time > 0) {
                        player1Time--
                    } else if (currentPlayer == 2 && player2Time > 0) {
                        player2Time--
                    }
                    
                    updateTimerDisplay()
                    
                    if (player1Time > 0 && player2Time > 0) {
                        handler.postDelayed(this, 1000)
                    } else {
                        timerRunning = false
                    }
                }
            }
        }
        handler.post(timerRunnable)
    }
    
    private fun updateTimerDisplay() {
        val player1TimeText = "${player1Time / 60}:${String.format("%02d", player1Time % 60)}"
        val player2TimeText = "${player2Time / 60}:${String.format("%02d", player2Time % 60)}"
        
        player1Timer.text = player1TimeText
        player2Timer.text = player2TimeText
        
        // Aktif oyuncuyu sarı renkle göster
        player1Timer.setTextColor(if (currentPlayer == 1) 0xFFFFFF00.toInt() else 0xFFFFFFFF.toInt())
        player2Timer.setTextColor(if (currentPlayer == 2) 0xFFFFFF00.toInt() else 0xFFFFFFFF.toInt())
    }
    
    override fun onDestroy() {
        super.onDestroy()
        timerRunning = false
        handler.removeCallbacks(timerRunnable)
    }
}