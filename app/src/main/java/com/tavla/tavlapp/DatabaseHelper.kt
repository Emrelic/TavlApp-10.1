package com.tavla.tavlapp

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class DatabaseHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_VERSION = 2
        private const val DATABASE_NAME = "TavlaScoreboard.db"

        // Tablo adları
        private const val TABLE_PLAYERS = "players"
        private const val TABLE_MATCHES = "matches"
        private const val TABLE_ROUNDS = "rounds"
        private const val TABLE_PLAYER_STATS = "player_stats"

        // Players Tablo Sütunları
        private const val COLUMN_PLAYER_ID = "id"
        private const val COLUMN_PLAYER_NAME = "name"

        // Matches Tablo Sütunları
        private const val COLUMN_MATCH_ID = "id"
        private const val COLUMN_PLAYER1_ID = "player1_id"
        private const val COLUMN_PLAYER2_ID = "player2_id"
        private const val COLUMN_PLAYER1_SCORE = "player1_score"
        private const val COLUMN_PLAYER2_SCORE = "player2_score"
        private const val COLUMN_GAME_TYPE = "game_type"
        private const val COLUMN_TOTAL_ROUNDS = "total_rounds"
        private const val COLUMN_PLAYER1_ROUNDS_WON = "player1_rounds_won"
        private const val COLUMN_PLAYER2_ROUNDS_WON = "player2_rounds_won"
        private const val COLUMN_WINNER_ID = "winner_id"
        private const val COLUMN_MATCH_DATE = "match_date"

        // Rounds Tablo Sütunları
        private const val COLUMN_ROUND_ID = "id"
        private const val COLUMN_ROUND_NUMBER = "round_number"
        private const val COLUMN_ROUND_MATCH_ID = "match_id"
        private const val COLUMN_ROUND_WINNER_ID = "winner_id"
        private const val COLUMN_WIN_TYPE = "win_type"
        private const val COLUMN_IS_DOUBLE = "is_double"
        private const val COLUMN_ROUND_SCORE = "score"
        private const val COLUMN_ROUND_DATE = "round_date"

        // Player Stats Tablo Sütunları
        private const val COLUMN_STATS_PLAYER_ID = "player_id"
        private const val COLUMN_TOTAL_MATCHES = "total_matches"
        private const val COLUMN_MATCHES_WON = "matches_won"
        private const val COLUMN_STATS_TOTAL_ROUNDS = "total_rounds"
        private const val COLUMN_ROUNDS_WON = "rounds_won"
        private const val COLUMN_SINGLE_WINS = "single_wins"
        private const val COLUMN_MARS_WINS = "mars_wins"
        private const val COLUMN_BACKGAMMON_WINS = "backgammon_wins"
        private const val COLUMN_DOUBLE_SINGLE_WINS = "double_single_wins"
        private const val COLUMN_DOUBLE_MARS_WINS = "double_mars_wins"
        private const val COLUMN_DOUBLE_BACKGAMMON_WINS = "double_backgammon_wins"
    }

    override fun onCreate(db: SQLiteDatabase) {
        val createPlayersTable = """
            CREATE TABLE $TABLE_PLAYERS (
                $COLUMN_PLAYER_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COLUMN_PLAYER_NAME TEXT UNIQUE
            )
        """.trimIndent()
        db.execSQL(createPlayersTable)

        val createMatchesTable = """
            CREATE TABLE $TABLE_MATCHES (
                $COLUMN_MATCH_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COLUMN_PLAYER1_ID INTEGER,
                $COLUMN_PLAYER2_ID INTEGER,
                $COLUMN_PLAYER1_SCORE INTEGER,
                $COLUMN_PLAYER2_SCORE INTEGER,
                $COLUMN_GAME_TYPE TEXT,
                $COLUMN_TOTAL_ROUNDS INTEGER,
                $COLUMN_PLAYER1_ROUNDS_WON INTEGER,
                $COLUMN_PLAYER2_ROUNDS_WON INTEGER,
                $COLUMN_WINNER_ID INTEGER,
                $COLUMN_MATCH_DATE TEXT,
                FOREIGN KEY($COLUMN_PLAYER1_ID) REFERENCES $TABLE_PLAYERS($COLUMN_PLAYER_ID),
                FOREIGN KEY($COLUMN_PLAYER2_ID) REFERENCES $TABLE_PLAYERS($COLUMN_PLAYER_ID),
                FOREIGN KEY($COLUMN_WINNER_ID) REFERENCES $TABLE_PLAYERS($COLUMN_PLAYER_ID)
            )
        """.trimIndent()
        db.execSQL(createMatchesTable)

        val createRoundsTable = """
            CREATE TABLE $TABLE_ROUNDS (
                $COLUMN_ROUND_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COLUMN_ROUND_MATCH_ID INTEGER,
                $COLUMN_ROUND_NUMBER INTEGER,
                $COLUMN_ROUND_WINNER_ID INTEGER,
                $COLUMN_WIN_TYPE TEXT,
                $COLUMN_IS_DOUBLE INTEGER,
                $COLUMN_ROUND_SCORE INTEGER,
                $COLUMN_ROUND_DATE TEXT,
                FOREIGN KEY($COLUMN_ROUND_MATCH_ID) REFERENCES $TABLE_MATCHES($COLUMN_MATCH_ID),
                FOREIGN KEY($COLUMN_ROUND_WINNER_ID) REFERENCES $TABLE_PLAYERS($COLUMN_PLAYER_ID)
            )
        """.trimIndent()
        db.execSQL(createRoundsTable)

        val createPlayerStatsTable = """
            CREATE TABLE $TABLE_PLAYER_STATS (
                $COLUMN_STATS_PLAYER_ID INTEGER PRIMARY KEY,
                $COLUMN_TOTAL_MATCHES INTEGER DEFAULT 0,
                $COLUMN_MATCHES_WON INTEGER DEFAULT 0,
                $COLUMN_STATS_TOTAL_ROUNDS INTEGER DEFAULT 0,
                $COLUMN_ROUNDS_WON INTEGER DEFAULT 0,
                $COLUMN_SINGLE_WINS INTEGER DEFAULT 0,
                $COLUMN_MARS_WINS INTEGER DEFAULT 0,
                $COLUMN_BACKGAMMON_WINS INTEGER DEFAULT 0,
                $COLUMN_DOUBLE_SINGLE_WINS INTEGER DEFAULT 0,
                $COLUMN_DOUBLE_MARS_WINS INTEGER DEFAULT 0,
                $COLUMN_DOUBLE_BACKGAMMON_WINS INTEGER DEFAULT 0,
                FOREIGN KEY($COLUMN_STATS_PLAYER_ID) REFERENCES $TABLE_PLAYERS($COLUMN_PLAYER_ID)
            )
        """.trimIndent()
        db.execSQL(createPlayerStatsTable)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_ROUNDS")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_PLAYER_STATS")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_MATCHES")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_PLAYERS")
        onCreate(db)
    }

    fun addPlayer(playerName: String): Long {
        val db = this.writableDatabase
        val values = ContentValues()
        values.put(COLUMN_PLAYER_NAME, playerName)
        val id = db.insertWithOnConflict(TABLE_PLAYERS, null, values, SQLiteDatabase.CONFLICT_IGNORE)
        if (id != -1L) {
            initializePlayerStats(id)
        }
        db.close()
        return id
    }

    private fun initializePlayerStats(playerId: Long) {
        val db = this.writableDatabase
        val values = ContentValues()
        values.put(COLUMN_STATS_PLAYER_ID, playerId)
        db.insertWithOnConflict(TABLE_PLAYER_STATS, null, values, SQLiteDatabase.CONFLICT_REPLACE)
    }

    fun getAllPlayers(): List<Player> {
        val playersList = mutableListOf<Player>()
        val selectQuery = "SELECT * FROM $TABLE_PLAYERS ORDER BY $COLUMN_PLAYER_NAME ASC"
        val db = this.readableDatabase
        val cursor = db.rawQuery(selectQuery, null)
        if (cursor.moveToFirst()) {
            do {
                val id = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_PLAYER_ID))
                val name = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_PLAYER_NAME))
                playersList.add(Player(id, name))
            } while (cursor.moveToNext())
        }
        cursor.close()
        db.close()
        return playersList
    }

    fun startNewMatch(player1Id: Long, player2Id: Long, gameType: String, targetRounds: Int): Long {
        val db = this.writableDatabase
        val values = ContentValues()
        values.put(COLUMN_PLAYER1_ID, player1Id)
        values.put(COLUMN_PLAYER2_ID, player2Id)
        values.put(COLUMN_PLAYER1_SCORE, 0)
        values.put(COLUMN_PLAYER2_SCORE, 0)
        values.put(COLUMN_GAME_TYPE, gameType)
        values.put(COLUMN_TOTAL_ROUNDS, 0)
        values.put(COLUMN_PLAYER1_ROUNDS_WON, 0)
        values.put(COLUMN_PLAYER2_ROUNDS_WON, 0)
        values.put(COLUMN_MATCH_DATE, getCurrentDateTime())
        val id = db.insert(TABLE_MATCHES, null, values)
        db.close()
        updatePlayerStatForNewMatch(player1Id)
        updatePlayerStatForNewMatch(player2Id)
        return id
    }

    private fun updatePlayerStatForNewMatch(playerId: Long) {
        val db = this.writableDatabase
        db.execSQL("""
            UPDATE $TABLE_PLAYER_STATS 
            SET $COLUMN_TOTAL_MATCHES = $COLUMN_TOTAL_MATCHES + 1 
            WHERE $COLUMN_STATS_PLAYER_ID = $playerId
        """)
        db.close()
    }

    fun addRound(
        matchId: Long,
        roundNumber: Int,
        winnerId: Long,
        winType: String,
        isDouble: Boolean,
        score: Int
    ): Long {
        val db = this.writableDatabase
        val values = ContentValues()
        values.put(COLUMN_ROUND_MATCH_ID, matchId)
        values.put(COLUMN_ROUND_NUMBER, roundNumber)
        values.put(COLUMN_ROUND_WINNER_ID, winnerId)
        values.put(COLUMN_WIN_TYPE, winType)
        values.put(COLUMN_IS_DOUBLE, if (isDouble) 1 else 0)
        values.put(COLUMN_ROUND_SCORE, score)
        values.put(COLUMN_ROUND_DATE, getCurrentDateTime())

        val id = db.insert(TABLE_ROUNDS, null, values)

        val matchInfo = updateMatchForRound(matchId, winnerId, score)
        if (matchInfo != null) {
            val player1Id = matchInfo.first
            val player2Id = matchInfo.second
            updatePlayerTotalRounds(player1Id)
            updatePlayerTotalRounds(player2Id)
            updatePlayerWinStats(winnerId, winType, isDouble)
        }

        db.close()
        return id
    }

    private fun updatePlayerTotalRounds(playerId: Long) {
        val db = this.writableDatabase
        db.execSQL("""
        UPDATE $TABLE_PLAYER_STATS 
        SET $COLUMN_STATS_TOTAL_ROUNDS = $COLUMN_STATS_TOTAL_ROUNDS + 1
        WHERE $COLUMN_STATS_PLAYER_ID = $playerId
    """)
        db.close()
    }

    private fun updateMatchForRound(matchId: Long, winnerId: Long, score: Int): Pair<Long, Long>? {
        val db = this.writableDatabase
        val cursor = db.rawQuery("""
            SELECT * FROM $TABLE_MATCHES 
            WHERE $COLUMN_MATCH_ID = $matchId
        """, null)

        if (cursor.moveToFirst()) {
            val player1Id = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_PLAYER1_ID))
            val player2Id = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_PLAYER2_ID))
            val player1Score = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_PLAYER1_SCORE))
            val player2Score = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_PLAYER2_SCORE))
            val player1RoundsWon = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_PLAYER1_ROUNDS_WON))
            val player2RoundsWon = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_PLAYER2_ROUNDS_WON))
            val totalRounds = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_TOTAL_ROUNDS))

            val newTotalRounds = totalRounds + 1
            var newPlayer1Score = player1Score
            var newPlayer2Score = player2Score
            var newPlayer1RoundsWon = player1RoundsWon
            var newPlayer2RoundsWon = player2RoundsWon

            if (winnerId == player1Id) {
                newPlayer1Score += score
                newPlayer1RoundsWon++
            } else if (winnerId == player2Id) {
                newPlayer2Score += score
                newPlayer2RoundsWon++
            }

            val values = ContentValues()
            values.put(COLUMN_PLAYER1_SCORE, newPlayer1Score)
            values.put(COLUMN_PLAYER2_SCORE, newPlayer2Score)
            values.put(COLUMN_TOTAL_ROUNDS, newTotalRounds)
            values.put(COLUMN_PLAYER1_ROUNDS_WON, newPlayer1RoundsWon)
            values.put(COLUMN_PLAYER2_ROUNDS_WON, newPlayer2RoundsWon)

            db.update(TABLE_MATCHES, values, "$COLUMN_MATCH_ID = ?", arrayOf(matchId.toString()))

            cursor.close()
            return Pair(player1Id, player2Id)
        }

        cursor.close()
        return null
    }

    private fun updatePlayerWinStats(winnerId: Long, winType: String, isDouble: Boolean) {
        val db = this.writableDatabase

        db.execSQL("""
        UPDATE $TABLE_PLAYER_STATS 
        SET $COLUMN_ROUNDS_WON = $COLUMN_ROUNDS_WON + 1
        WHERE $COLUMN_STATS_PLAYER_ID = $winnerId
    """)

        val column = when {
            isDouble && winType == "SINGLE" -> COLUMN_DOUBLE_SINGLE_WINS
            isDouble && winType == "MARS" -> COLUMN_DOUBLE_MARS_WINS
            isDouble && winType == "BACKGAMMON" -> COLUMN_DOUBLE_BACKGAMMON_WINS
            !isDouble && winType == "SINGLE" -> COLUMN_SINGLE_WINS
            !isDouble && winType == "MARS" -> COLUMN_MARS_WINS
            !isDouble && winType == "BACKGAMMON" -> COLUMN_BACKGAMMON_WINS
            else -> null
        }

        if (column != null) {
            db.execSQL("""
            UPDATE $TABLE_PLAYER_STATS 
            SET $column = $column + 1
            WHERE $COLUMN_STATS_PLAYER_ID = $winnerId
        """)
        }

        db.close()
    }

    // ============== YENİ UNDO FONKSİYONLARI ==============

    // BASIT VERSİYON: Belirli bir round'u ID ile getir
    fun getRoundById(roundId: Long): Round? {
        val db = this.readableDatabase
        val cursor = db.rawQuery("""
        SELECT * FROM $TABLE_ROUNDS 
        WHERE $COLUMN_ROUND_ID = $roundId
    """, null)

        if (cursor.moveToFirst()) {
            try {
                val round = Round(
                    id = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_ROUND_ID)),
                    matchId = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_ROUND_MATCH_ID)),
                    roundNumber = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_ROUND_NUMBER)),
                    winnerId = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_ROUND_WINNER_ID)),
                    winType = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_WIN_TYPE)),
                    isDouble = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_IS_DOUBLE)) == 1,
                    doubleValue = 2, // Basit: hep 2 olarak kabul et
                    score = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_ROUND_SCORE)),
                    date = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_ROUND_DATE))
                )
                cursor.close()
                db.close()
                return round
            } catch (e: Exception) {
                cursor.close()
                db.close()
                return null
            }
        }

        cursor.close()
        db.close()
        return null
    }
    /// DatabaseHelper.kt - deleteRound fonksiyonunu bu DEBUG versiyonu ile değiştir:

    // DatabaseHelper.kt - deleteRound fonksiyonunu bu şekilde düzelt:

    fun deleteRound(roundId: Long): Int {
        val db = this.writableDatabase

        try {
            

            // 1. Round bilgilerini al (silmeden önce)
            val roundCursor = db.rawQuery("""
            SELECT $COLUMN_ROUND_MATCH_ID, $COLUMN_ROUND_WINNER_ID, $COLUMN_ROUND_SCORE 
            FROM $TABLE_ROUNDS 
            WHERE $COLUMN_ROUND_ID = ?
        """, arrayOf(roundId.toString()))

            if (!roundCursor.moveToFirst()) {
                roundCursor.close()
                
                return 0 // Round bulunamadı
            }

            val matchId = roundCursor.getLong(0)
            val winnerId = roundCursor.getLong(1)
            val score = roundCursor.getInt(2)
            roundCursor.close()

            

            // 2. Round'u sil
            val deleteResult = db.delete(TABLE_ROUNDS, "$COLUMN_ROUND_ID = ?", arrayOf(roundId.toString()))

            

            if (deleteResult > 0) {
                

                // 3. ✅ AAYNI DB ÖRNEĞİNİ KULLANARAK Maçı yeniden hesapla
                recalculateMatchFromRoundsWithDb(matchId, db)

                
            }

            return deleteResult

        } catch (e: Exception) {
            
            return 0
        } finally {
            db.close() // Artık güvenle kapatabiliriz
        }
    }
    // DatabaseHelper.kt - recalculateMatchFromRoundsWithDb fonksiyonunu düzelt:

    private fun recalculateMatchFromRoundsWithDb(matchId: Long, db: SQLiteDatabase) {
        try {
            

            // ✅ AYNI DB CONNECTION İLE round'ları al (getMatchRounds kullanma!)
            val rounds = getMatchRoundsWithDb(matchId, db)

            

            // Maç bilgilerini al
            val matchCursor = db.rawQuery("""
            SELECT $COLUMN_PLAYER1_ID, $COLUMN_PLAYER2_ID 
            FROM $TABLE_MATCHES 
            WHERE $COLUMN_MATCH_ID = ?
        """, arrayOf(matchId.toString()))

            if (matchCursor.moveToFirst()) {
                val player1Id = matchCursor.getLong(0)
                val player2Id = matchCursor.getLong(1)

                

                // Skorları sıfırdan hesapla
                var player1Score = 0
                var player2Score = 0
                var player1RoundsWon = 0
                var player2RoundsWon = 0

                rounds.forEach { round ->
                    
                    if (round.winnerId == player1Id) {
                        player1Score += round.score
                        player1RoundsWon++
                    } else if (round.winnerId == player2Id) {
                        player2Score += round.score
                        player2RoundsWon++
                    }
                }

                
                

                // Maç tablosunu güncelle
                val values = ContentValues().apply {
                    put(COLUMN_PLAYER1_SCORE, player1Score)
                    put(COLUMN_PLAYER2_SCORE, player2Score)
                    put(COLUMN_TOTAL_ROUNDS, rounds.size)
                    put(COLUMN_PLAYER1_ROUNDS_WON, player1RoundsWon)
                    put(COLUMN_PLAYER2_ROUNDS_WON, player2RoundsWon)
                }

                val updateResult = db.update(TABLE_MATCHES, values, "$COLUMN_MATCH_ID = ?", arrayOf(matchId.toString()))
                

            } else {
                
            }

            matchCursor.close()

        } catch (e: Exception) {
            
        }
    }
    // ✅ YENİ YARDIMCI FONKSİYON: Mevcut DB connection ile round'ları al
    private fun getMatchRoundsWithDb(matchId: Long, db: SQLiteDatabase): List<Round> {
        val roundsList = mutableListOf<Round>()
        val cursor = db.rawQuery("""
        SELECT * FROM $TABLE_ROUNDS 
        WHERE $COLUMN_ROUND_MATCH_ID = ?
        ORDER BY $COLUMN_ROUND_NUMBER ASC
    """, arrayOf(matchId.toString()))

        if (cursor.moveToFirst()) {
            do {
                val round = Round(
                    id = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_ROUND_ID)),
                    matchId = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_ROUND_MATCH_ID)),
                    roundNumber = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_ROUND_NUMBER)),
                    winnerId = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_ROUND_WINNER_ID)),
                    winType = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_WIN_TYPE)),
                    isDouble = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_IS_DOUBLE)) == 1,
                    doubleValue = if (cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_IS_DOUBLE)) == 1) {
                        when (cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_WIN_TYPE))) {
                            "SINGLE" -> cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_ROUND_SCORE)) / 1
                            "MARS" -> cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_ROUND_SCORE)) / 2
                            "BACKGAMMON" -> cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_ROUND_SCORE)) / 3
                            else -> 1
                        }
                    } else 1,
                    score = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_ROUND_SCORE)),
                    date = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_ROUND_DATE))
                )
                roundsList.add(round)
            } while (cursor.moveToNext())
        }

        cursor.close()
        return roundsList
    }
    // ✅ DEBUG VERSİYONU: recalculateMatchFromRounds fonksiyonunu da güncelle
    private fun recalculateMatchFromRounds(matchId: Long) {
        val db = this.writableDatabase

        try {
            

            // Tüm kalan round'ları al
            val rounds = getMatchRounds(matchId)

            

            // Maç bilgilerini al
            val matchCursor = db.rawQuery("""
            SELECT $COLUMN_PLAYER1_ID, $COLUMN_PLAYER2_ID 
            FROM $TABLE_MATCHES 
            WHERE $COLUMN_MATCH_ID = ?
        """, arrayOf(matchId.toString()))

            if (matchCursor.moveToFirst()) {
                val player1Id = matchCursor.getLong(0)
                val player2Id = matchCursor.getLong(1)

                

                // Skorları sıfırdan hesapla
                var player1Score = 0
                var player2Score = 0
                var player1RoundsWon = 0
                var player2RoundsWon = 0

                rounds.forEach { round ->
                    
                    if (round.winnerId == player1Id) {
                        player1Score += round.score
                        player1RoundsWon++
                    } else if (round.winnerId == player2Id) {
                        player2Score += round.score
                        player2RoundsWon++
                    }
                }

                
                

                // Maç tablosunu güncelle
                val values = ContentValues().apply {
                    put(COLUMN_PLAYER1_SCORE, player1Score)
                    put(COLUMN_PLAYER2_SCORE, player2Score)
                    put(COLUMN_TOTAL_ROUNDS, rounds.size)
                    put(COLUMN_PLAYER1_ROUNDS_WON, player1RoundsWon)
                    put(COLUMN_PLAYER2_ROUNDS_WON, player2RoundsWon)
                }

                val updateResult = db.update(TABLE_MATCHES, values, "$COLUMN_MATCH_ID = ?", arrayOf(matchId.toString()))
                

            } else {
                
            }

            matchCursor.close()

        } catch (e: Exception) {
            
        }
    }

    // BASIT VERSİYON: Round silme ve maç istatistiklerini yeniden hesaplama
    fun deleteRoundAndRecalculate(matchId: Long, roundId: Long): Boolean {
        return try {
            val db = this.writableDatabase

            // Önce round'u al (silmeden önce bilgilerini kaydet)
            val roundToDelete = getRoundById(roundId)
            if (roundToDelete == null) {
                db.close()
                return false
            }

            // Round'u sil
            val deleteResult = db.delete(TABLE_ROUNDS, "$COLUMN_ROUND_ID = ?", arrayOf(roundId.toString()))

            if (deleteResult > 0) {
                // Basit yeniden hesaplama
                recalculateMatchStatsSimple(matchId)
                db.close()
                true
            } else {
                db.close()
                false
            }
        } catch (e: Exception) {
            false
        }
    }

    // BASIT VERSİYON: Maç istatistiklerini yeniden hesapla
    private fun recalculateMatchStatsSimple(matchId: Long) {
        try {
            val db = this.writableDatabase

            // Maçın tüm round'larını al
            val rounds = getMatchRounds(matchId)

            // Skorları ve round sayılarını hesapla
            var player1Score = 0
            var player2Score = 0
            var player1RoundsWon = 0
            var player2RoundsWon = 0

            // Maç bilgilerini al
            val cursor = db.rawQuery("""
            SELECT $COLUMN_PLAYER1_ID, $COLUMN_PLAYER2_ID FROM $TABLE_MATCHES 
            WHERE $COLUMN_MATCH_ID = $matchId
        """, null)

            if (cursor.moveToFirst()) {
                val player1Id = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_PLAYER1_ID))
                val player2Id = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_PLAYER2_ID))

                rounds.forEach { round ->
                    if (round.winnerId == player1Id) {
                        player1Score += round.score
                        player1RoundsWon++
                    } else if (round.winnerId == player2Id) {
                        player2Score += round.score
                        player2RoundsWon++
                    }
                }

                // Maç tablosunu güncelle
                val values = ContentValues()
                values.put(COLUMN_PLAYER1_SCORE, player1Score)
                values.put(COLUMN_PLAYER2_SCORE, player2Score)
                values.put(COLUMN_TOTAL_ROUNDS, rounds.size)
                values.put(COLUMN_PLAYER1_ROUNDS_WON, player1RoundsWon)
                values.put(COLUMN_PLAYER2_ROUNDS_WON, player2RoundsWon)

                db.update(TABLE_MATCHES, values, "$COLUMN_MATCH_ID = ?", arrayOf(matchId.toString()))
            }

            cursor.close()
        } catch (e: Exception) {
            // Hata olursa sessizce devam et
        }
    }

    // Oyuncu istatistiklerini round silindikten sonra güncelle
    private fun updatePlayerStatsAfterRoundDelete(deletedRound: Round) {
        val db = this.writableDatabase

        val winnerId = deletedRound.winnerId

        // Maç bilgilerini al
        val matchCursor = db.rawQuery("""
            SELECT $COLUMN_PLAYER1_ID, $COLUMN_PLAYER2_ID FROM $TABLE_MATCHES 
            WHERE $COLUMN_MATCH_ID = ${deletedRound.matchId}
        """, null)

        if (matchCursor.moveToFirst()) {
            val player1Id = matchCursor.getLong(matchCursor.getColumnIndexOrThrow(COLUMN_PLAYER1_ID))
            val player2Id = matchCursor.getLong(matchCursor.getColumnIndexOrThrow(COLUMN_PLAYER2_ID))

            // Her iki oyuncunun da toplam round sayısını azalt
            db.execSQL("""
                UPDATE $TABLE_PLAYER_STATS 
                SET $COLUMN_STATS_TOTAL_ROUNDS = $COLUMN_STATS_TOTAL_ROUNDS - 1
                WHERE $COLUMN_STATS_PLAYER_ID IN ($player1Id, $player2Id)
            """)

            // Kazanan oyuncunun istatistiklerini azalt
            db.execSQL("""
                UPDATE $TABLE_PLAYER_STATS 
                SET $COLUMN_ROUNDS_WON = $COLUMN_ROUNDS_WON - 1
                WHERE $COLUMN_STATS_PLAYER_ID = $winnerId
            """)

            // Kazanma tipine göre özel istatistikleri azalt
            val column = when {
                deletedRound.isDouble && deletedRound.winType == "SINGLE" -> COLUMN_DOUBLE_SINGLE_WINS
                deletedRound.isDouble && deletedRound.winType == "MARS" -> COLUMN_DOUBLE_MARS_WINS
                deletedRound.isDouble && deletedRound.winType == "BACKGAMMON" -> COLUMN_DOUBLE_BACKGAMMON_WINS
                !deletedRound.isDouble && deletedRound.winType == "SINGLE" -> COLUMN_SINGLE_WINS
                !deletedRound.isDouble && deletedRound.winType == "MARS" -> COLUMN_MARS_WINS
                !deletedRound.isDouble && deletedRound.winType == "BACKGAMMON" -> COLUMN_BACKGAMMON_WINS
                else -> null
            }

            if (column != null) {
                db.execSQL("""
                    UPDATE $TABLE_PLAYER_STATS 
                    SET $column = CASE WHEN $column > 0 THEN $column - 1 ELSE 0 END
                    WHERE $COLUMN_STATS_PLAYER_ID = $winnerId
                """)
            }
        }

        matchCursor.close()
    }

    // ============== MEVCUT FONKSİYONLAR DEVAM EDİYOR ==============

    fun finishMatch(matchId: Long): Long {
        val db = this.writableDatabase
        val cursor = db.rawQuery("""
        SELECT * FROM $TABLE_MATCHES 
        WHERE $COLUMN_MATCH_ID = $matchId
    """, null)

        if (cursor.moveToFirst()) {
            val player1Id = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_PLAYER1_ID))
            val player2Id = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_PLAYER2_ID))
            val player1Score = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_PLAYER1_SCORE))
            val player2Score = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_PLAYER2_SCORE))
            val existingWinnerId = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_WINNER_ID))

            if (existingWinnerId != 0L) {
                cursor.close()
                db.close()
                return existingWinnerId
            }

            val winnerId = if (player1Score > player2Score) player1Id else player2Id
            val values = ContentValues()
            values.put(COLUMN_WINNER_ID, winnerId)
            db.update(TABLE_MATCHES, values, "$COLUMN_MATCH_ID = ?", arrayOf(matchId.toString()))
            updatePlayerStatForMatchWin(winnerId)

            cursor.close()
            db.close()
            return winnerId
        }

        cursor.close()
        db.close()
        return -1L
    }

    private fun updatePlayerStatForMatchWin(winnerId: Long) {
        val db = this.writableDatabase
        db.execSQL("""
            UPDATE $TABLE_PLAYER_STATS 
            SET $COLUMN_MATCHES_WON = $COLUMN_MATCHES_WON + 1
            WHERE $COLUMN_STATS_PLAYER_ID = $winnerId
        """)
        db.close()
    }

    fun getPlayerStats(playerId: Long): PlayerStats? {
        val db = this.readableDatabase
        val cursor = db.rawQuery("""
            SELECT * FROM $TABLE_PLAYER_STATS 
            WHERE $COLUMN_STATS_PLAYER_ID = $playerId
        """, null)

        if (cursor.moveToFirst()) {
            val stats = PlayerStats(
                playerId = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_STATS_PLAYER_ID)),
                totalMatches = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_TOTAL_MATCHES)),
                matchesWon = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_MATCHES_WON)),
                totalRounds = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_STATS_TOTAL_ROUNDS)),
                roundsWon = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_ROUNDS_WON)),
                singleWins = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_SINGLE_WINS)),
                marsWins = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_MARS_WINS)),
                backgammonWins = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_BACKGAMMON_WINS)),
                doubleSingleWins = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_DOUBLE_SINGLE_WINS)),
                doubleMarsWins = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_DOUBLE_MARS_WINS)),
                doubleBackgammonWins = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_DOUBLE_BACKGAMMON_WINS))
            )
            cursor.close()
            db.close()
            return stats
        }

        cursor.close()
        db.close()
        return null
    }

    fun getMatchDetails(matchId: Long): Match? {
        val db = this.readableDatabase
        val cursor = db.rawQuery("""
            SELECT * FROM $TABLE_MATCHES 
            WHERE $COLUMN_MATCH_ID = $matchId
        """, null)

        if (cursor.moveToFirst()) {
            val match = Match(
                id = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_MATCH_ID)),
                player1Id = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_PLAYER1_ID)),
                player2Id = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_PLAYER2_ID)),
                player1Score = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_PLAYER1_SCORE)),
                player2Score = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_PLAYER2_SCORE)),
                gameType = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_GAME_TYPE)),
                totalRounds = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_TOTAL_ROUNDS)),
                player1RoundsWon = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_PLAYER1_ROUNDS_WON)),
                player2RoundsWon = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_PLAYER2_ROUNDS_WON)),
                winnerId = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_WINNER_ID)),
                date = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_MATCH_DATE))
            )
            cursor.close()
            db.close()
            return match
        }

        cursor.close()
        db.close()
        return null
    }

    fun getAllMatches(): List<Match> {
        val matchesList = mutableListOf<Match>()
        val db = this.readableDatabase
        val cursor = db.rawQuery("""
            SELECT * FROM $TABLE_MATCHES 
            ORDER BY $COLUMN_MATCH_DATE DESC
        """, null)

        if (cursor.moveToFirst()) {
            do {
                val match = Match(
                    id = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_MATCH_ID)),
                    player1Id = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_PLAYER1_ID)),
                    player2Id = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_PLAYER2_ID)),
                    player1Score = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_PLAYER1_SCORE)),
                    player2Score = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_PLAYER2_SCORE)),
                    gameType = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_GAME_TYPE)),
                    totalRounds = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_TOTAL_ROUNDS)),
                    player1RoundsWon = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_PLAYER1_ROUNDS_WON)),
                    player2RoundsWon = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_PLAYER2_ROUNDS_WON)),
                    winnerId = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_WINNER_ID)),
                    date = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_MATCH_DATE))
                )
                matchesList.add(match)
            } while (cursor.moveToNext())
        }

        cursor.close()
        db.close()
        return matchesList
    }

    fun getPlayerVsPlayerStats(player1Id: Long, player2Id: Long): PlayerVsPlayerStats {
        val db = this.readableDatabase

        val totalMatchesQuery = """
            SELECT COUNT(*) FROM $TABLE_MATCHES 
            WHERE ($COLUMN_PLAYER1_ID = $player1Id AND $COLUMN_PLAYER2_ID = $player2Id)
               OR ($COLUMN_PLAYER1_ID = $player2Id AND $COLUMN_PLAYER2_ID = $player1Id)
        """
        var cursor = db.rawQuery(totalMatchesQuery, null)
        cursor.moveToFirst()
        val totalMatches = cursor.getInt(0)
        cursor.close()

        val player1WinsQuery = """
            SELECT COUNT(*) FROM $TABLE_MATCHES 
            WHERE (($COLUMN_PLAYER1_ID = $player1Id AND $COLUMN_PLAYER2_ID = $player2Id)
               OR ($COLUMN_PLAYER1_ID = $player2Id AND $COLUMN_PLAYER2_ID = $player1Id))
               AND $COLUMN_WINNER_ID = $player1Id
        """
        cursor = db.rawQuery(player1WinsQuery, null)
        cursor.moveToFirst()
        val player1Wins = cursor.getInt(0)
        cursor.close()

        val player2WinsQuery = """
            SELECT COUNT(*) FROM $TABLE_MATCHES 
            WHERE (($COLUMN_PLAYER1_ID = $player1Id AND $COLUMN_PLAYER2_ID = $player2Id)
               OR ($COLUMN_PLAYER1_ID = $player2Id AND $COLUMN_PLAYER2_ID = $player1Id))
               AND $COLUMN_WINNER_ID = $player2Id
        """
        cursor = db.rawQuery(player2WinsQuery, null)
        cursor.moveToFirst()
        val player2Wins = cursor.getInt(0)
        cursor.close()

        db.close()

        return PlayerVsPlayerStats(
            player1Id = player1Id,
            player2Id = player2Id,
            totalMatches = totalMatches,
            player1Wins = player1Wins,
            player2Wins = player2Wins
        )
    }

    private fun getCurrentDateTime(): String {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        val date = Date()
        return dateFormat.format(date)
    }

    fun getMatchRounds(matchId: Long): List<Round> {
        val roundsList = mutableListOf<Round>()
        val db = this.readableDatabase
        val cursor = db.rawQuery("""
            SELECT * FROM $TABLE_ROUNDS 
            WHERE $COLUMN_ROUND_MATCH_ID = $matchId
            ORDER BY $COLUMN_ROUND_NUMBER ASC
        """, null)

        if (cursor.moveToFirst()) {
            do {
                val round = Round(
                    id = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_ROUND_ID)),
                    matchId = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_ROUND_MATCH_ID)),
                    roundNumber = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_ROUND_NUMBER)),
                    winnerId = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_ROUND_WINNER_ID)),
                    winType = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_WIN_TYPE)),
                    isDouble = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_IS_DOUBLE)) == 1,
                    doubleValue = if (cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_IS_DOUBLE)) == 1) {
                        when (cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_WIN_TYPE))) {
                            "SINGLE" -> cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_ROUND_SCORE)) / 1
                            "MARS" -> cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_ROUND_SCORE)) / 2
                            "BACKGAMMON" -> cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_ROUND_SCORE)) / 3
                            else -> 1
                        }
                    } else 1,
                    score = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_ROUND_SCORE)),
                    date = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_ROUND_DATE))
                )
                roundsList.add(round)
            } while (cursor.moveToNext())
        }

        cursor.close()
        db.close()
        return roundsList
    }

    fun getMatchesBetweenPlayers(player1Id: Long, player2Id: Long): List<Match> {
        val matchesList = mutableListOf<Match>()
        val db = this.readableDatabase
        val cursor = db.rawQuery("""
            SELECT * FROM $TABLE_MATCHES 
            WHERE ($COLUMN_PLAYER1_ID = $player1Id AND $COLUMN_PLAYER2_ID = $player2Id)
               OR ($COLUMN_PLAYER1_ID = $player2Id AND $COLUMN_PLAYER2_ID = $player1Id)
            ORDER BY $COLUMN_MATCH_DATE DESC
        """, null)

        if (cursor.moveToFirst()) {
            do {
                val match = Match(
                    id = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_MATCH_ID)),
                    player1Id = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_PLAYER1_ID)),
                    player2Id = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_PLAYER2_ID)),
                    player1Score = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_PLAYER1_SCORE)),
                    player2Score = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_PLAYER2_SCORE)),
                    gameType = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_GAME_TYPE)),
                    totalRounds = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_TOTAL_ROUNDS)),
                    player1RoundsWon = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_PLAYER1_ROUNDS_WON)),
                    player2RoundsWon = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_PLAYER2_ROUNDS_WON)),
                    winnerId = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_WINNER_ID)),
                    date = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_MATCH_DATE))
                )
                matchesList.add(match)
            } while (cursor.moveToNext())
        }

        cursor.close()
        db.close()
        return matchesList
    }

    fun getPlayerMatches(playerId: Long): List<Match> {
        val matchesList = mutableListOf<Match>()
        val db = this.readableDatabase
        val cursor = db.rawQuery("""
            SELECT * FROM $TABLE_MATCHES 
            WHERE $COLUMN_PLAYER1_ID = $playerId OR $COLUMN_PLAYER2_ID = $playerId
            ORDER BY $COLUMN_MATCH_DATE DESC
        """, null)

        if (cursor.moveToFirst()) {
            do {
                val match = Match(
                    id = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_MATCH_ID)),
                    player1Id = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_PLAYER1_ID)),
                    player2Id = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_PLAYER2_ID)),
                    player1Score = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_PLAYER1_SCORE)),
                    player2Score = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_PLAYER2_SCORE)),
                    gameType = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_GAME_TYPE)),
                    totalRounds = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_TOTAL_ROUNDS)),
                    player1RoundsWon = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_PLAYER1_ROUNDS_WON)),
                    player2RoundsWon = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_PLAYER2_ROUNDS_WON)),
                    winnerId = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_WINNER_ID)),
                    date = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_MATCH_DATE))
                )
                matchesList.add(match)
            } while (cursor.moveToNext())
        }

        cursor.close()
        db.close()
        return matchesList
    }

    fun calculatePlayerMatchStats(rounds: List<Round>, playerId: Long): PlayerRoundStats {
        val playerRounds = rounds.filter { it.winnerId == playerId }
        val winTypeMap = mutableMapOf<String, Int>()

        playerRounds.forEach { round ->
            val key = round.combinedWinType
            winTypeMap[key] = (winTypeMap[key] ?: 0) + 1
        }

        var singleWins = 0
        var marsWins = 0
        var backgammonWins = 0
        var doubleSingleWins = 0
        var doubleMarsWins = 0
        var doubleBackgammonWins = 0
        var quadSingleWins = 0
        var quadMarsWins = 0
        var quadBackgammonWins = 0

        playerRounds.forEach { round ->
            when {
                round.isDouble && round.doubleValue >= 4 && round.winType == "SINGLE" -> quadSingleWins++
                round.isDouble && round.doubleValue >= 4 && round.winType == "MARS" -> quadMarsWins++
                round.isDouble && round.doubleValue >= 4 && round.winType == "BACKGAMMON" -> quadBackgammonWins++
                round.isDouble && round.doubleValue == 2 && round.winType == "SINGLE" -> doubleSingleWins++
                round.isDouble && round.doubleValue == 2 && round.winType == "MARS" -> doubleMarsWins++
                round.isDouble && round.doubleValue == 2 && round.winType == "BACKGAMMON" -> doubleBackgammonWins++
                !round.isDouble && round.winType == "SINGLE" -> singleWins++
                !round.isDouble && round.winType == "MARS" -> marsWins++
                !round.isDouble && round.winType == "BACKGAMMON" -> backgammonWins++
            }
        }

        return PlayerRoundStats(
            totalWins = playerRounds.size,
            winTypeMap = winTypeMap,
            singleWins = singleWins,
            marsWins = marsWins,
            backgammonWins = backgammonWins,
            doubleSingleWins = doubleSingleWins,
            doubleMarsWins = doubleMarsWins,
            doubleBackgammonWins = doubleBackgammonWins,
            quadSingleWins = quadSingleWins,
            quadMarsWins = quadMarsWins,
            quadBackgammonWins = quadBackgammonWins
        )
    }

    fun getDetailedPlayerVsPlayerStats(player1Id: Long, player2Id: Long): DetailedPlayerVsPlayerStats {
        val matches = getMatchesBetweenPlayers(player1Id, player2Id)
        val allRounds = mutableListOf<Round>()
        matches.forEach { match ->
            allRounds.addAll(getMatchRounds(match.id))
        }

        val player1Rounds = allRounds.filter { it.winnerId == player1Id }
        val player1SingleWins = player1Rounds.count { it.winType == "SINGLE" && !it.isDouble }
        val player1MarsWins = player1Rounds.count { it.winType == "MARS" && !it.isDouble }
        val player1BackgammonWins = player1Rounds.count { it.winType == "BACKGAMMON" && !it.isDouble }
        val player1DoubleSingleWins = player1Rounds.count { it.winType == "SINGLE" && it.isDouble && it.doubleValue == 2 }
        val player1DoubleMarsWins = player1Rounds.count { it.winType == "MARS" && it.isDouble && it.doubleValue == 2 }
        val player1DoubleBackgammonWins = player1Rounds.count { it.winType == "BACKGAMMON" && it.isDouble && it.doubleValue == 2 }
        val player1QuadSingleWins = player1Rounds.count { it.winType == "SINGLE" && it.isDouble && it.doubleValue >= 4 }
        val player1QuadMarsWins = player1Rounds.count { it.winType == "MARS" && it.isDouble && it.doubleValue >= 4 }
        val player1QuadBackgammonWins = player1Rounds.count { it.winType == "BACKGAMMON" && it.isDouble && it.doubleValue >= 4 }

        val player2Rounds = allRounds.filter { it.winnerId == player2Id }
        val player2SingleWins = player2Rounds.count { it.winType == "SINGLE" && !it.isDouble }
        val player2MarsWins = player2Rounds.count { it.winType == "MARS" && !it.isDouble }
        val player2BackgammonWins = player2Rounds.count { it.winType == "BACKGAMMON" && !it.isDouble }
        val player2DoubleSingleWins = player2Rounds.count { it.winType == "SINGLE" && it.isDouble && it.doubleValue == 2 }
        val player2DoubleMarsWins = player2Rounds.count { it.winType == "MARS" && it.isDouble && it.doubleValue == 2 }
        val player2DoubleBackgammonWins = player2Rounds.count { it.winType == "BACKGAMMON" && it.isDouble && it.doubleValue == 2 }
        val player2QuadSingleWins = player2Rounds.count { it.winType == "SINGLE" && it.isDouble && it.doubleValue >= 4 }
        val player2QuadMarsWins = player2Rounds.count { it.winType == "MARS" && it.isDouble && it.doubleValue >= 4 }
        val player2QuadBackgammonWins = player2Rounds.count { it.winType == "BACKGAMMON" && it.isDouble && it.doubleValue >= 4 }

        return DetailedPlayerVsPlayerStats(
            player1Id = player1Id,
            player2Id = player2Id,
            totalMatches = matches.size,
            player1Wins = matches.count { it.winnerId == player1Id },
            player2Wins = matches.count { it.winnerId == player2Id },
            player1SingleWins = player1SingleWins,
            player1MarsWins = player1MarsWins,
            player1BackgammonWins = player1BackgammonWins,
            player1DoubleSingleWins = player1DoubleSingleWins,
            player1DoubleMarsWins = player1DoubleMarsWins,
            player1DoubleBackgammonWins = player1DoubleBackgammonWins,
            player1QuadSingleWins = player1QuadSingleWins,
            player1QuadMarsWins = player1QuadMarsWins,
            player1QuadBackgammonWins = player1QuadBackgammonWins,
            player2SingleWins = player2SingleWins,
            player2MarsWins = player2MarsWins,
            player2BackgammonWins = player2BackgammonWins,
            player2DoubleSingleWins = player2DoubleSingleWins,
            player2DoubleMarsWins = player2DoubleMarsWins,
            player2DoubleBackgammonWins = player2DoubleBackgammonWins,
            player2QuadSingleWins = player2QuadSingleWins,
            player2QuadMarsWins = player2QuadMarsWins,
            player2QuadBackgammonWins = player2QuadBackgammonWins
        )
    }

    fun deleteMatch(matchId: Long): Int {
        val db = this.writableDatabase
        db.delete(TABLE_ROUNDS, "$COLUMN_ROUND_MATCH_ID = ?", arrayOf(matchId.toString()))
        val result = db.delete(TABLE_MATCHES, "$COLUMN_MATCH_ID = ?", arrayOf(matchId.toString()))
        db.close()
        return result
    }

    fun deleteMatches(matchIds: List<Long>): Int {
        if (matchIds.isEmpty()) return 0
        val db = this.writableDatabase
        var totalDeleted = 0
        matchIds.forEach { matchId ->
            db.delete(TABLE_ROUNDS, "$COLUMN_ROUND_MATCH_ID = ?", arrayOf(matchId.toString()))
            val deleted = db.delete(TABLE_MATCHES, "$COLUMN_MATCH_ID = ?", arrayOf(matchId.toString()))
            totalDeleted += deleted
        }
        db.close()
        return totalDeleted
    }

    fun deleteAllMatches(): Int {
        val db = this.writableDatabase
        db.delete(TABLE_ROUNDS, null, null)
        val deleted = db.delete(TABLE_MATCHES, null, null)
        db.close()
        return deleted
    }

    fun resetAllData(): Int {
        val db = this.writableDatabase
        db.delete(TABLE_ROUNDS, null, null)
        val deletedMatches = db.delete(TABLE_MATCHES, null, null)
        val resetPlayerStatsSQL = """
        UPDATE $TABLE_PLAYER_STATS 
        SET $COLUMN_TOTAL_MATCHES = 0,
            $COLUMN_MATCHES_WON = 0,
            $COLUMN_STATS_TOTAL_ROUNDS = 0,
            $COLUMN_ROUNDS_WON = 0,
            $COLUMN_SINGLE_WINS = 0,
            $COLUMN_MARS_WINS = 0,
            $COLUMN_BACKGAMMON_WINS = 0,
            $COLUMN_DOUBLE_SINGLE_WINS = 0,
            $COLUMN_DOUBLE_MARS_WINS = 0,
            $COLUMN_DOUBLE_BACKGAMMON_WINS = 0
    """
        db.execSQL(resetPlayerStatsSQL)
        db.close()
        return deletedMatches
    }
}
