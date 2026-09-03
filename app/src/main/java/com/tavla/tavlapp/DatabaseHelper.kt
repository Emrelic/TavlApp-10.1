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
        private const val DATABASE_VERSION = 11
        private const val DATABASE_NAME = "TavlaScoreboard.db"

        // Tablo adları
        private const val TABLE_PLAYERS = "players"
        private const val TABLE_MATCHES = "matches"
        private const val TABLE_ROUNDS = "rounds"
        private const val TABLE_PLAYER_STATS = "player_stats"
        private const val TABLE_DICE_STATS = "dice_statistics"
        private const val TABLE_DICE_EVALUATIONS = "dice_evaluations"
        private const val TABLE_ACTIVITY_LOGS = "activity_logs"

        // Rövanşlı Karşılaşma Tabloları (v8 - Party/Set yapısı)
        private const val TABLE_REMATCH_ENCOUNTERS = "rematch_encounters"
        private const val TABLE_REMATCH_DICE_PARTIES = "rematch_dice_parties"
        private const val TABLE_REMATCH_DICE_SETS = "rematch_dice_sets"
        private const val TABLE_REMATCH_GAME_RESULTS = "rematch_game_results"
        private const val TABLE_REMATCH_PARTY_RESULTS = "rematch_party_results"
        private const val TABLE_REMATCH_ENCOUNTER_STATS = "rematch_encounter_stats"

        // Eski tablo (migrasyon için)
        private const val TABLE_REMATCH_MATCH_RESULTS = "rematch_match_results"

        // Online Matches Tablosu
        private const val TABLE_ONLINE_MATCHES = "online_matches"

        // Activity Logs Tablo Sütunları
        private const val COLUMN_LOG_ID = "id"
        private const val COLUMN_LOG_TIMESTAMP = "timestamp"
        private const val COLUMN_LOG_DATETIME = "date_time"
        private const val COLUMN_LOG_ACTION_TYPE = "action_type"
        private const val COLUMN_LOG_DESCRIPTION = "description"
        private const val COLUMN_LOG_PLAYER1_NAME = "player1_name"
        private const val COLUMN_LOG_PLAYER2_NAME = "player2_name"
        private const val COLUMN_LOG_MATCH_ID = "match_id"
        private const val COLUMN_LOG_EXTRA_DATA = "extra_data"

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

        // Dice Statistics Tablo Sütunları
        private const val COLUMN_DICE_STATS_ID = "id"
        private const val COLUMN_DICE_MATCH_ID = "match_id"
        private const val COLUMN_DICE_PLAYER_ID = "player_id"

        // Atılan zar istatistikleri (21 kombinasyon: 1-1, 1-2, ..., 6-6)
        private const val COLUMN_DICE_1_1 = "dice_1_1"
        private const val COLUMN_DICE_1_2 = "dice_1_2"
        private const val COLUMN_DICE_1_3 = "dice_1_3"
        private const val COLUMN_DICE_1_4 = "dice_1_4"
        private const val COLUMN_DICE_1_5 = "dice_1_5"
        private const val COLUMN_DICE_1_6 = "dice_1_6"
        private const val COLUMN_DICE_2_2 = "dice_2_2"
        private const val COLUMN_DICE_2_3 = "dice_2_3"
        private const val COLUMN_DICE_2_4 = "dice_2_4"
        private const val COLUMN_DICE_2_5 = "dice_2_5"
        private const val COLUMN_DICE_2_6 = "dice_2_6"
        private const val COLUMN_DICE_3_3 = "dice_3_3"
        private const val COLUMN_DICE_3_4 = "dice_3_4"
        private const val COLUMN_DICE_3_5 = "dice_3_5"
        private const val COLUMN_DICE_3_6 = "dice_3_6"
        private const val COLUMN_DICE_4_4 = "dice_4_4"
        private const val COLUMN_DICE_4_5 = "dice_4_5"
        private const val COLUMN_DICE_4_6 = "dice_4_6"
        private const val COLUMN_DICE_5_5 = "dice_5_5"
        private const val COLUMN_DICE_5_6 = "dice_5_6"
        private const val COLUMN_DICE_6_6 = "dice_6_6"

        // Genel istatistikler
        private const val COLUMN_TOTAL_DICE_POWER = "total_dice_power"
        private const val COLUMN_TOTAL_DICE_PIECES = "total_dice_pieces"
        private const val COLUMN_DOUBLE_COUNT = "double_count"
        private const val COLUMN_DOUBLE_POWER = "double_power"

        // Oynanan
        private const val COLUMN_PLAYED_POWER = "played_power"
        private const val COLUMN_PLAYED_PIECES = "played_pieces"

        // Gele
        private const val COLUMN_WASTED_POWER = "wasted_power"
        private const val COLUMN_WASTED_PIECES = "wasted_pieces"

        // Kısmen boşa
        private const val COLUMN_PARTIAL_WASTED_POWER = "partial_wasted_power"
        private const val COLUMN_PARTIAL_WASTED_PIECES = "partial_wasted_pieces"

        // Bitiş artığı
        private const val COLUMN_END_WASTE_POWER = "end_waste_power"
        private const val COLUMN_END_WASTE_PIECES = "end_waste_pieces"

        // Dice Evaluations Tablo Sütunları
        private const val COLUMN_EVAL_ID = "id"
        private const val COLUMN_EVAL_MATCH_ID = "match_id"
        private const val COLUMN_EVAL_PLAYER_ID = "player_id"
        private const val COLUMN_EVAL_DICE_COMBO = "dice_combo"
        private const val COLUMN_EVAL_RATING = "rating"
        private const val COLUMN_EVAL_STATE = "state"
        private const val COLUMN_EVAL_TIMESTAMP = "timestamp"

        // Rövanşlı Karşılaşma Sütunları (v8 güncelleme)
        private const val COLUMN_ENCOUNTER_ID = "id"
        private const val COLUMN_ENCOUNTER_PLAYER1_ID = "player1_id"
        private const val COLUMN_ENCOUNTER_PLAYER2_ID = "player2_id"
        private const val COLUMN_ENCOUNTER_TOTAL_PARTIES = "total_parties"
        private const val COLUMN_ENCOUNTER_CURRENT_ROUND = "current_round"
        private const val COLUMN_ENCOUNTER_CURRENT_PARTY_INDEX = "current_party_index"
        private const val COLUMN_ENCOUNTER_CURRENT_GAME_INDEX = "current_game_index"
        private const val COLUMN_ENCOUNTER_STATUS = "status"
        private const val COLUMN_ENCOUNTER_CREATED_DATE = "created_date"
        private const val COLUMN_ENCOUNTER_COMPLETED_DATE = "completed_date"
        private const val COLUMN_ENCOUNTER_TARGET_SCORE = "target_score"
        private const val COLUMN_ENCOUNTER_TRACK_PIP = "track_pip_count"
        // Eski sütun adı (migrasyon uyumu)
        private const val COLUMN_ENCOUNTER_TOTAL_MATCHES = "total_matches"
        private const val COLUMN_ENCOUNTER_CURRENT_MATCH = "current_match"

        // Rövanşlı Zar Seti Sütunları
        private const val COLUMN_DICE_SET_ID = "id"
        private const val COLUMN_DICE_SET_ENCOUNTER_ID = "encounter_id"
        private const val COLUMN_DICE_SET_MATCH_INDEX = "match_index"
        private const val COLUMN_DICE_SET_STARTING_P1 = "starting_dice_player1"
        private const val COLUMN_DICE_SET_STARTING_P2 = "starting_dice_player2"
        private const val COLUMN_DICE_SET_P1_JSON = "player1_dice_json"
        private const val COLUMN_DICE_SET_P2_JSON = "player2_dice_json"

        // Rövanşlı Maç Sonucu Sütunları
        private const val COLUMN_REMATCH_RESULT_ID = "id"
        private const val COLUMN_REMATCH_RESULT_ENCOUNTER_ID = "encounter_id"
        private const val COLUMN_REMATCH_RESULT_MATCH_INDEX = "match_index"
        private const val COLUMN_REMATCH_RESULT_ROUND_NUMBER = "round_number"
        private const val COLUMN_REMATCH_RESULT_LEFT_PLAYER_ID = "left_player_id"
        private const val COLUMN_REMATCH_RESULT_RIGHT_PLAYER_ID = "right_player_id"
        private const val COLUMN_REMATCH_RESULT_WINNER_ID = "winner_id"
        private const val COLUMN_REMATCH_RESULT_WIN_TYPE = "win_type"
        private const val COLUMN_REMATCH_RESULT_CUBE_VALUE = "cube_value"
        private const val COLUMN_REMATCH_RESULT_FINAL_SCORE = "final_score"
        private const val COLUMN_REMATCH_RESULT_LOSER_PIP = "loser_pip_count"
        private const val COLUMN_REMATCH_RESULT_TOTAL_MOVES = "total_moves_played"
        private const val COLUMN_REMATCH_RESULT_DATE = "match_date"

        // Rövanşlı Karşılaşma İstatistikleri Sütunları
        private const val COLUMN_REMATCH_STATS_ID = "id"
        private const val COLUMN_REMATCH_STATS_ENCOUNTER_ID = "encounter_id"
        private const val COLUMN_REMATCH_STATS_PLAYER_ID = "player_id"
        private const val COLUMN_REMATCH_STATS_R1_WINS = "round1_wins"
        private const val COLUMN_REMATCH_STATS_R1_POINTS = "round1_points"
        private const val COLUMN_REMATCH_STATS_R1_MARS = "round1_mars_wins"
        private const val COLUMN_REMATCH_STATS_R1_BG = "round1_backgammon_wins"
        private const val COLUMN_REMATCH_STATS_R2_WINS = "round2_wins"
        private const val COLUMN_REMATCH_STATS_R2_POINTS = "round2_points"
        private const val COLUMN_REMATCH_STATS_R2_MARS = "round2_mars_wins"
        private const val COLUMN_REMATCH_STATS_R2_BG = "round2_backgammon_wins"
        private const val COLUMN_REMATCH_STATS_TOTAL_WINS = "total_wins"
        private const val COLUMN_REMATCH_STATS_TOTAL_POINTS = "total_points"
        private const val COLUMN_REMATCH_STATS_R1_PARTIES = "round1_parties_won"
        private const val COLUMN_REMATCH_STATS_R2_PARTIES = "round2_parties_won"
        private const val COLUMN_REMATCH_STATS_TOTAL_PARTIES = "total_parties_won"

        // Zar Partisi Sütunları (v8)
        private const val COLUMN_PARTY_ID = "id"
        private const val COLUMN_PARTY_ENCOUNTER_ID = "encounter_id"
        private const val COLUMN_PARTY_INDEX = "party_index"

        // Zar Seti Sütunları (v8 güncelleme - party_id eklendi)
        private const val COLUMN_DICE_SET_PARTY_ID = "party_id"
        private const val COLUMN_DICE_SET_INDEX = "set_index"

        // Oyun Sonucu Sütunları (v8)
        private const val COLUMN_GAME_RESULT_ID = "id"
        private const val COLUMN_GAME_RESULT_ENCOUNTER_ID = "encounter_id"
        private const val COLUMN_GAME_RESULT_PARTY_INDEX = "party_index"
        private const val COLUMN_GAME_RESULT_SET_INDEX = "set_index"
        private const val COLUMN_GAME_RESULT_ROUND_NUMBER = "round_number"
        private const val COLUMN_GAME_RESULT_LEFT_PLAYER_ID = "left_player_id"
        private const val COLUMN_GAME_RESULT_RIGHT_PLAYER_ID = "right_player_id"
        private const val COLUMN_GAME_RESULT_WINNER_ID = "winner_id"
        private const val COLUMN_GAME_RESULT_WIN_TYPE = "win_type"
        private const val COLUMN_GAME_RESULT_CUBE_VALUE = "cube_value"
        private const val COLUMN_GAME_RESULT_FINAL_SCORE = "final_score"
        private const val COLUMN_GAME_RESULT_LOSER_PIP = "loser_pip_count"
        private const val COLUMN_GAME_RESULT_DICE_PAIRS_USED = "dice_pairs_used"
        private const val COLUMN_GAME_RESULT_DATE = "game_date"
        private const val COLUMN_GAME_RESULT_DOUBLER_ID = "doubler_player_id"
        private const val COLUMN_GAME_RESULT_LEFT_DICE_TOTAL = "left_dice_total"
        private const val COLUMN_GAME_RESULT_RIGHT_DICE_TOTAL = "right_dice_total"
        private const val COLUMN_GAME_RESULT_LEFT_DOUBLES = "left_doubles_count"
        private const val COLUMN_GAME_RESULT_RIGHT_DOUBLES = "right_doubles_count"

        // Parti Sonucu Sütunları (v8)
        private const val COLUMN_PARTY_RESULT_ID = "id"
        private const val COLUMN_PARTY_RESULT_ENCOUNTER_ID = "encounter_id"
        private const val COLUMN_PARTY_RESULT_PARTY_INDEX = "party_index"
        private const val COLUMN_PARTY_RESULT_ROUND_NUMBER = "round_number"
        private const val COLUMN_PARTY_RESULT_P1_SCORE = "player1_score"
        private const val COLUMN_PARTY_RESULT_P2_SCORE = "player2_score"
        private const val COLUMN_PARTY_RESULT_WINNER_ID = "winner_id"
        private const val COLUMN_PARTY_RESULT_TOTAL_GAMES = "total_games_played"
        private const val COLUMN_PARTY_RESULT_DATE = "party_date"
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

        val createDiceStatsTable = """
            CREATE TABLE $TABLE_DICE_STATS (
                $COLUMN_DICE_STATS_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COLUMN_DICE_MATCH_ID INTEGER,
                $COLUMN_DICE_PLAYER_ID INTEGER,
                $COLUMN_DICE_1_1 INTEGER DEFAULT 0,
                $COLUMN_DICE_1_2 INTEGER DEFAULT 0,
                $COLUMN_DICE_1_3 INTEGER DEFAULT 0,
                $COLUMN_DICE_1_4 INTEGER DEFAULT 0,
                $COLUMN_DICE_1_5 INTEGER DEFAULT 0,
                $COLUMN_DICE_1_6 INTEGER DEFAULT 0,
                $COLUMN_DICE_2_2 INTEGER DEFAULT 0,
                $COLUMN_DICE_2_3 INTEGER DEFAULT 0,
                $COLUMN_DICE_2_4 INTEGER DEFAULT 0,
                $COLUMN_DICE_2_5 INTEGER DEFAULT 0,
                $COLUMN_DICE_2_6 INTEGER DEFAULT 0,
                $COLUMN_DICE_3_3 INTEGER DEFAULT 0,
                $COLUMN_DICE_3_4 INTEGER DEFAULT 0,
                $COLUMN_DICE_3_5 INTEGER DEFAULT 0,
                $COLUMN_DICE_3_6 INTEGER DEFAULT 0,
                $COLUMN_DICE_4_4 INTEGER DEFAULT 0,
                $COLUMN_DICE_4_5 INTEGER DEFAULT 0,
                $COLUMN_DICE_4_6 INTEGER DEFAULT 0,
                $COLUMN_DICE_5_5 INTEGER DEFAULT 0,
                $COLUMN_DICE_5_6 INTEGER DEFAULT 0,
                $COLUMN_DICE_6_6 INTEGER DEFAULT 0,
                $COLUMN_TOTAL_DICE_POWER INTEGER DEFAULT 0,
                $COLUMN_TOTAL_DICE_PIECES INTEGER DEFAULT 0,
                $COLUMN_DOUBLE_COUNT INTEGER DEFAULT 0,
                $COLUMN_DOUBLE_POWER INTEGER DEFAULT 0,
                $COLUMN_PLAYED_POWER INTEGER DEFAULT 0,
                $COLUMN_PLAYED_PIECES INTEGER DEFAULT 0,
                $COLUMN_WASTED_POWER INTEGER DEFAULT 0,
                $COLUMN_WASTED_PIECES INTEGER DEFAULT 0,
                $COLUMN_PARTIAL_WASTED_POWER INTEGER DEFAULT 0,
                $COLUMN_PARTIAL_WASTED_PIECES INTEGER DEFAULT 0,
                $COLUMN_END_WASTE_POWER INTEGER DEFAULT 0,
                $COLUMN_END_WASTE_PIECES INTEGER DEFAULT 0,
                FOREIGN KEY($COLUMN_DICE_MATCH_ID) REFERENCES $TABLE_MATCHES($COLUMN_MATCH_ID),
                FOREIGN KEY($COLUMN_DICE_PLAYER_ID) REFERENCES $TABLE_PLAYERS($COLUMN_PLAYER_ID)
            )
        """.trimIndent()
        db.execSQL(createDiceStatsTable)

        val createDiceEvaluationsTable = """
            CREATE TABLE $TABLE_DICE_EVALUATIONS (
                $COLUMN_EVAL_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COLUMN_EVAL_MATCH_ID INTEGER,
                $COLUMN_EVAL_PLAYER_ID INTEGER,
                $COLUMN_EVAL_DICE_COMBO TEXT,
                $COLUMN_EVAL_RATING INTEGER,
                $COLUMN_EVAL_STATE TEXT DEFAULT 'OYANDI',
                $COLUMN_EVAL_TIMESTAMP INTEGER,
                FOREIGN KEY($COLUMN_EVAL_MATCH_ID) REFERENCES $TABLE_MATCHES($COLUMN_MATCH_ID),
                FOREIGN KEY($COLUMN_EVAL_PLAYER_ID) REFERENCES $TABLE_PLAYERS($COLUMN_PLAYER_ID)
            )
        """.trimIndent()
        db.execSQL(createDiceEvaluationsTable)

        val createActivityLogsTable = """
            CREATE TABLE $TABLE_ACTIVITY_LOGS (
                $COLUMN_LOG_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COLUMN_LOG_TIMESTAMP TEXT,
                $COLUMN_LOG_DATETIME TEXT,
                $COLUMN_LOG_ACTION_TYPE TEXT,
                $COLUMN_LOG_DESCRIPTION TEXT,
                $COLUMN_LOG_PLAYER1_NAME TEXT,
                $COLUMN_LOG_PLAYER2_NAME TEXT,
                $COLUMN_LOG_MATCH_ID INTEGER,
                $COLUMN_LOG_EXTRA_DATA TEXT
            )
        """.trimIndent()
        db.execSQL(createActivityLogsTable)

        // Rövanşlı Karşılaşma Tabloları
        createRematchTables(db)

        // Online Matches Tablosu
        createOnlineMatchesTable(db)
    }

    private fun createRematchTables(db: SQLiteDatabase) {
        // Ana karsilasma tablosu (v8)
        val createRematchEncountersTable = """
            CREATE TABLE $TABLE_REMATCH_ENCOUNTERS (
                $COLUMN_ENCOUNTER_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COLUMN_ENCOUNTER_PLAYER1_ID INTEGER NOT NULL,
                $COLUMN_ENCOUNTER_PLAYER2_ID INTEGER NOT NULL,
                $COLUMN_ENCOUNTER_TOTAL_PARTIES INTEGER NOT NULL,
                $COLUMN_ENCOUNTER_TARGET_SCORE INTEGER DEFAULT 11,
                $COLUMN_ENCOUNTER_TRACK_PIP INTEGER DEFAULT 1,
                $COLUMN_ENCOUNTER_CURRENT_ROUND INTEGER DEFAULT 1,
                $COLUMN_ENCOUNTER_CURRENT_PARTY_INDEX INTEGER DEFAULT 0,
                $COLUMN_ENCOUNTER_CURRENT_GAME_INDEX INTEGER DEFAULT 0,
                $COLUMN_ENCOUNTER_STATUS TEXT DEFAULT 'ACTIVE',
                $COLUMN_ENCOUNTER_CREATED_DATE TEXT NOT NULL,
                $COLUMN_ENCOUNTER_COMPLETED_DATE TEXT,
                FOREIGN KEY($COLUMN_ENCOUNTER_PLAYER1_ID) REFERENCES $TABLE_PLAYERS($COLUMN_PLAYER_ID),
                FOREIGN KEY($COLUMN_ENCOUNTER_PLAYER2_ID) REFERENCES $TABLE_PLAYERS($COLUMN_PLAYER_ID)
            )
        """.trimIndent()
        db.execSQL(createRematchEncountersTable)

        // Zar partisi tablosu (v8) - 100 parti
        val createRematchDicePartiesTable = """
            CREATE TABLE $TABLE_REMATCH_DICE_PARTIES (
                $COLUMN_PARTY_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COLUMN_PARTY_ENCOUNTER_ID INTEGER NOT NULL,
                $COLUMN_PARTY_INDEX INTEGER NOT NULL,
                FOREIGN KEY($COLUMN_PARTY_ENCOUNTER_ID) REFERENCES $TABLE_REMATCH_ENCOUNTERS($COLUMN_ENCOUNTER_ID),
                UNIQUE($COLUMN_PARTY_ENCOUNTER_ID, $COLUMN_PARTY_INDEX)
            )
        """.trimIndent()
        db.execSQL(createRematchDicePartiesTable)

        // Zar seti tablosu (v8) - her parti icin 21 set
        val createRematchDiceSetsTable = """
            CREATE TABLE $TABLE_REMATCH_DICE_SETS (
                $COLUMN_DICE_SET_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COLUMN_DICE_SET_PARTY_ID INTEGER NOT NULL,
                $COLUMN_DICE_SET_INDEX INTEGER NOT NULL,
                $COLUMN_DICE_SET_STARTING_P1 INTEGER NOT NULL,
                $COLUMN_DICE_SET_STARTING_P2 INTEGER NOT NULL,
                $COLUMN_DICE_SET_P1_JSON TEXT NOT NULL,
                $COLUMN_DICE_SET_P2_JSON TEXT NOT NULL,
                FOREIGN KEY($COLUMN_DICE_SET_PARTY_ID) REFERENCES $TABLE_REMATCH_DICE_PARTIES($COLUMN_PARTY_ID),
                UNIQUE($COLUMN_DICE_SET_PARTY_ID, $COLUMN_DICE_SET_INDEX)
            )
        """.trimIndent()
        db.execSQL(createRematchDiceSetsTable)

        // Oyun sonucu tablosu (v8) - tek bir el/oyun sonucu
        val createRematchGameResultsTable = """
            CREATE TABLE $TABLE_REMATCH_GAME_RESULTS (
                $COLUMN_GAME_RESULT_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COLUMN_GAME_RESULT_ENCOUNTER_ID INTEGER NOT NULL,
                $COLUMN_GAME_RESULT_PARTY_INDEX INTEGER NOT NULL,
                $COLUMN_GAME_RESULT_SET_INDEX INTEGER NOT NULL,
                $COLUMN_GAME_RESULT_ROUND_NUMBER INTEGER NOT NULL,
                $COLUMN_GAME_RESULT_LEFT_PLAYER_ID INTEGER NOT NULL,
                $COLUMN_GAME_RESULT_RIGHT_PLAYER_ID INTEGER NOT NULL,
                $COLUMN_GAME_RESULT_WINNER_ID INTEGER,
                $COLUMN_GAME_RESULT_WIN_TYPE TEXT,
                $COLUMN_GAME_RESULT_CUBE_VALUE INTEGER DEFAULT 1,
                $COLUMN_GAME_RESULT_FINAL_SCORE INTEGER,
                $COLUMN_GAME_RESULT_LOSER_PIP INTEGER,
                $COLUMN_GAME_RESULT_DICE_PAIRS_USED INTEGER,
                $COLUMN_GAME_RESULT_DATE TEXT,
                $COLUMN_GAME_RESULT_DOUBLER_ID INTEGER,
                $COLUMN_GAME_RESULT_LEFT_DICE_TOTAL INTEGER DEFAULT 0,
                $COLUMN_GAME_RESULT_RIGHT_DICE_TOTAL INTEGER DEFAULT 0,
                $COLUMN_GAME_RESULT_LEFT_DOUBLES INTEGER DEFAULT 0,
                $COLUMN_GAME_RESULT_RIGHT_DOUBLES INTEGER DEFAULT 0,
                FOREIGN KEY($COLUMN_GAME_RESULT_ENCOUNTER_ID) REFERENCES $TABLE_REMATCH_ENCOUNTERS($COLUMN_ENCOUNTER_ID),
                FOREIGN KEY($COLUMN_GAME_RESULT_LEFT_PLAYER_ID) REFERENCES $TABLE_PLAYERS($COLUMN_PLAYER_ID),
                FOREIGN KEY($COLUMN_GAME_RESULT_RIGHT_PLAYER_ID) REFERENCES $TABLE_PLAYERS($COLUMN_PLAYER_ID),
                FOREIGN KEY($COLUMN_GAME_RESULT_WINNER_ID) REFERENCES $TABLE_PLAYERS($COLUMN_PLAYER_ID),
                UNIQUE($COLUMN_GAME_RESULT_ENCOUNTER_ID, $COLUMN_GAME_RESULT_PARTY_INDEX, $COLUMN_GAME_RESULT_SET_INDEX, $COLUMN_GAME_RESULT_ROUND_NUMBER)
            )
        """.trimIndent()
        db.execSQL(createRematchGameResultsTable)

        // Parti sonucu tablosu (v8) - 11'lik parti sonucu
        val createRematchPartyResultsTable = """
            CREATE TABLE $TABLE_REMATCH_PARTY_RESULTS (
                $COLUMN_PARTY_RESULT_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COLUMN_PARTY_RESULT_ENCOUNTER_ID INTEGER NOT NULL,
                $COLUMN_PARTY_RESULT_PARTY_INDEX INTEGER NOT NULL,
                $COLUMN_PARTY_RESULT_ROUND_NUMBER INTEGER NOT NULL,
                $COLUMN_PARTY_RESULT_P1_SCORE INTEGER DEFAULT 0,
                $COLUMN_PARTY_RESULT_P2_SCORE INTEGER DEFAULT 0,
                $COLUMN_PARTY_RESULT_WINNER_ID INTEGER,
                $COLUMN_PARTY_RESULT_TOTAL_GAMES INTEGER DEFAULT 0,
                $COLUMN_PARTY_RESULT_DATE TEXT,
                FOREIGN KEY($COLUMN_PARTY_RESULT_ENCOUNTER_ID) REFERENCES $TABLE_REMATCH_ENCOUNTERS($COLUMN_ENCOUNTER_ID),
                FOREIGN KEY($COLUMN_PARTY_RESULT_WINNER_ID) REFERENCES $TABLE_PLAYERS($COLUMN_PLAYER_ID),
                UNIQUE($COLUMN_PARTY_RESULT_ENCOUNTER_ID, $COLUMN_PARTY_RESULT_PARTY_INDEX, $COLUMN_PARTY_RESULT_ROUND_NUMBER)
            )
        """.trimIndent()
        db.execSQL(createRematchPartyResultsTable)

        // Karsilasma istatistikleri tablosu (v8 guncelleme)
        val createRematchEncounterStatsTable = """
            CREATE TABLE $TABLE_REMATCH_ENCOUNTER_STATS (
                $COLUMN_REMATCH_STATS_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COLUMN_REMATCH_STATS_ENCOUNTER_ID INTEGER NOT NULL,
                $COLUMN_REMATCH_STATS_PLAYER_ID INTEGER NOT NULL,
                $COLUMN_REMATCH_STATS_R1_PARTIES INTEGER DEFAULT 0,
                $COLUMN_REMATCH_STATS_R1_WINS INTEGER DEFAULT 0,
                $COLUMN_REMATCH_STATS_R1_POINTS INTEGER DEFAULT 0,
                $COLUMN_REMATCH_STATS_R1_MARS INTEGER DEFAULT 0,
                $COLUMN_REMATCH_STATS_R1_BG INTEGER DEFAULT 0,
                $COLUMN_REMATCH_STATS_R2_PARTIES INTEGER DEFAULT 0,
                $COLUMN_REMATCH_STATS_R2_WINS INTEGER DEFAULT 0,
                $COLUMN_REMATCH_STATS_R2_POINTS INTEGER DEFAULT 0,
                $COLUMN_REMATCH_STATS_R2_MARS INTEGER DEFAULT 0,
                $COLUMN_REMATCH_STATS_R2_BG INTEGER DEFAULT 0,
                $COLUMN_REMATCH_STATS_TOTAL_PARTIES INTEGER DEFAULT 0,
                $COLUMN_REMATCH_STATS_TOTAL_WINS INTEGER DEFAULT 0,
                $COLUMN_REMATCH_STATS_TOTAL_POINTS INTEGER DEFAULT 0,
                FOREIGN KEY($COLUMN_REMATCH_STATS_ENCOUNTER_ID) REFERENCES $TABLE_REMATCH_ENCOUNTERS($COLUMN_ENCOUNTER_ID),
                FOREIGN KEY($COLUMN_REMATCH_STATS_PLAYER_ID) REFERENCES $TABLE_PLAYERS($COLUMN_PLAYER_ID),
                UNIQUE($COLUMN_REMATCH_STATS_ENCOUNTER_ID, $COLUMN_REMATCH_STATS_PLAYER_ID)
            )
        """.trimIndent()
        db.execSQL(createRematchEncounterStatsTable)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        // Versiyon 3'ten 4'e gecis: activity_logs tablosu eklendi
        if (oldVersion < 4) {
            db.execSQL("""
                CREATE TABLE IF NOT EXISTS $TABLE_ACTIVITY_LOGS (
                    $COLUMN_LOG_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                    $COLUMN_LOG_TIMESTAMP TEXT,
                    $COLUMN_LOG_DATETIME TEXT,
                    $COLUMN_LOG_ACTION_TYPE TEXT,
                    $COLUMN_LOG_DESCRIPTION TEXT,
                    $COLUMN_LOG_PLAYER1_NAME TEXT,
                    $COLUMN_LOG_PLAYER2_NAME TEXT,
                    $COLUMN_LOG_MATCH_ID INTEGER,
                    $COLUMN_LOG_EXTRA_DATA TEXT
                )
            """.trimIndent())
        }
        
        // Versiyon 4'ten 5'e gecis: dice_evaluations tablosu eklendi
        if (oldVersion < 5) {
            db.execSQL("""
                CREATE TABLE IF NOT EXISTS $TABLE_DICE_EVALUATIONS (
                    $COLUMN_EVAL_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                    $COLUMN_EVAL_MATCH_ID INTEGER,
                    $COLUMN_EVAL_PLAYER_ID INTEGER,
                    $COLUMN_EVAL_DICE_COMBO TEXT,
                    $COLUMN_EVAL_RATING INTEGER,
                    $COLUMN_EVAL_TIMESTAMP INTEGER,
                    FOREIGN KEY($COLUMN_EVAL_MATCH_ID) REFERENCES $TABLE_MATCHES($COLUMN_MATCH_ID),
                    FOREIGN KEY($COLUMN_EVAL_PLAYER_ID) REFERENCES $TABLE_PLAYERS($COLUMN_PLAYER_ID)
                )
            """.trimIndent())
        }
        
        // Versiyon 5'ten 6'ya gecis: dice_evaluations tablosuna state kolonu eklendi
        if (oldVersion < 6) {
            db.execSQL("ALTER TABLE $TABLE_DICE_EVALUATIONS ADD COLUMN $COLUMN_EVAL_STATE TEXT DEFAULT 'OYANDI'")
        }

        // Versiyon 6'dan 7'ye gecis: Rovansli Karsilasma tablolari eklendi (eski yapi)
        if (oldVersion < 7 && oldVersion >= 6) {
            // Eski v7 tabloları artık kullanılmıyor, v8'de yeniden oluşturulacak
        }

        // Versiyon 7'den 8'e gecis: Party/Set yapısına gecis
        if (oldVersion < 8) {
            // Eski tabloları temizle (varsa)
            try {
                db.execSQL("DROP TABLE IF EXISTS $TABLE_REMATCH_MATCH_RESULTS")
                db.execSQL("DROP TABLE IF EXISTS $TABLE_REMATCH_DICE_SETS")
                db.execSQL("DROP TABLE IF EXISTS $TABLE_REMATCH_ENCOUNTER_STATS")
                db.execSQL("DROP TABLE IF EXISTS $TABLE_REMATCH_ENCOUNTERS")
            } catch (e: Exception) {
                // Tablo yoksa hata vermesin
            }
            // Yeni tabloları oluştur
            createRematchTables(db)
        }

        // Versiyon 8'den 9'a gecis: target_score ve track_pip_count kolonlari eklendi
        if (oldVersion < 9) {
            try {
                db.execSQL("ALTER TABLE $TABLE_REMATCH_ENCOUNTERS ADD COLUMN $COLUMN_ENCOUNTER_TARGET_SCORE INTEGER DEFAULT 11")
            } catch (e: Exception) { }
            try {
                db.execSQL("ALTER TABLE $TABLE_REMATCH_ENCOUNTERS ADD COLUMN $COLUMN_ENCOUNTER_TRACK_PIP INTEGER DEFAULT 1")
            } catch (e: Exception) { }
        }

        // Versiyon 9'dan 10'a gecis: küp ve zar istatistik kolonlari eklendi
        if (oldVersion < 10) {
            try {
                db.execSQL("ALTER TABLE $TABLE_REMATCH_GAME_RESULTS ADD COLUMN $COLUMN_GAME_RESULT_DOUBLER_ID INTEGER")
            } catch (e: Exception) { }
            try {
                db.execSQL("ALTER TABLE $TABLE_REMATCH_GAME_RESULTS ADD COLUMN $COLUMN_GAME_RESULT_LEFT_DICE_TOTAL INTEGER DEFAULT 0")
            } catch (e: Exception) { }
            try {
                db.execSQL("ALTER TABLE $TABLE_REMATCH_GAME_RESULTS ADD COLUMN $COLUMN_GAME_RESULT_RIGHT_DICE_TOTAL INTEGER DEFAULT 0")
            } catch (e: Exception) { }
            try {
                db.execSQL("ALTER TABLE $TABLE_REMATCH_GAME_RESULTS ADD COLUMN $COLUMN_GAME_RESULT_LEFT_DOUBLES INTEGER DEFAULT 0")
            } catch (e: Exception) { }
            try {
                db.execSQL("ALTER TABLE $TABLE_REMATCH_GAME_RESULTS ADD COLUMN $COLUMN_GAME_RESULT_RIGHT_DOUBLES INTEGER DEFAULT 0")
            } catch (e: Exception) { }
        }

        // Versiyon 10'dan 11'e gecis: online_matches tablosu eklendi
        if (oldVersion < 11) {
            createOnlineMatchesTable(db)
        }
    }

    private fun createOnlineMatchesTable(db: SQLiteDatabase) {
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS $TABLE_ONLINE_MATCHES (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                room_code TEXT,
                my_player_id INTEGER,
                opponent_name TEXT,
                my_color TEXT,
                my_score INTEGER,
                opponent_score INTEGER,
                target_score INTEGER,
                game_type TEXT,
                total_games INTEGER,
                winner TEXT,
                match_date TEXT,
                FOREIGN KEY(my_player_id) REFERENCES $TABLE_PLAYERS($COLUMN_PLAYER_ID)
            )
        """.trimIndent())
    }

    fun saveOnlineMatchResult(
        roomCode: String,
        myPlayerId: Long,
        opponentName: String,
        myColor: String,
        myScore: Int,
        opponentScore: Int,
        targetScore: Int,
        gameType: String,
        totalGames: Int,
        winner: String
    ): Long {
        val db = this.writableDatabase
        val values = ContentValues().apply {
            put("room_code", roomCode)
            put("my_player_id", myPlayerId)
            put("opponent_name", opponentName)
            put("my_color", myColor)
            put("my_score", myScore)
            put("opponent_score", opponentScore)
            put("target_score", targetScore)
            put("game_type", gameType)
            put("total_games", totalGames)
            put("winner", winner)
            put("match_date", SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date()))
        }
        val id = db.insert(TABLE_ONLINE_MATCHES, null, values)
        db.close()
        return id
    }

    fun addPlayer(playerName: String): Long {
        val db = this.writableDatabase
        val values = ContentValues()
        values.put(COLUMN_PLAYER_NAME, playerName)
        val id = db.insertWithOnConflict(TABLE_PLAYERS, null, values, SQLiteDatabase.CONFLICT_IGNORE)
        if (id != -1L) {
            initializePlayerStats(id)
            db.close()
            return id
        }
        // Oyuncu zaten varsa mevcut ID'yi dondur
        val cursor = db.rawQuery(
            "SELECT $COLUMN_PLAYER_ID FROM $TABLE_PLAYERS WHERE $COLUMN_PLAYER_NAME = ?",
            arrayOf(playerName)
        )
        val existingId = if (cursor.moveToFirst()) cursor.getLong(0) else -1L
        cursor.close()
        db.close()
        return existingId
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
        db.delete(TABLE_DICE_STATS, null, null)
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

    // ============== ZAR İSTATİSTİKLERİ FONKSİYONLARI ==============

    /**
     * Maç için oyuncu zar istatistiklerini başlat
     */
    fun initializeDiceStats(matchId: Long, playerId: Long): Long {
        val db = this.writableDatabase
        val values = ContentValues()
        values.put(COLUMN_DICE_MATCH_ID, matchId)
        values.put(COLUMN_DICE_PLAYER_ID, playerId)
        val id = db.insert(TABLE_DICE_STATS, null, values)
        db.close()
        return id
    }

    /**
     * Zar atışı sonucunu kaydet
     */
    fun saveDiceRoll(matchId: Long, playerId: Long, rollResult: DiceRollResult) {
        val db = this.writableDatabase

        // İstatistikleri hesapla
        val stats = DiceHelper.calculateStatisticsFromRoll(rollResult)

        // Zar kombinasyonu column adı
        val diceColumn = DiceHelper.getDiceColumnName(rollResult.dice1, rollResult.dice2)

        // Güncelleme SQL'i
        val updateSQL = """
            UPDATE $TABLE_DICE_STATS
            SET $diceColumn = $diceColumn + 1,
                $COLUMN_TOTAL_DICE_POWER = $COLUMN_TOTAL_DICE_POWER + ${stats["totalPower"]},
                $COLUMN_TOTAL_DICE_PIECES = $COLUMN_TOTAL_DICE_PIECES + ${stats["totalPieces"]},
                $COLUMN_DOUBLE_COUNT = $COLUMN_DOUBLE_COUNT + ${stats["doubleCount"]},
                $COLUMN_DOUBLE_POWER = $COLUMN_DOUBLE_POWER + ${stats["doublePower"]},
                $COLUMN_PLAYED_POWER = $COLUMN_PLAYED_POWER + ${stats["playedPower"]},
                $COLUMN_PLAYED_PIECES = $COLUMN_PLAYED_PIECES + ${stats["playedPieces"]},
                $COLUMN_WASTED_POWER = $COLUMN_WASTED_POWER + ${stats["wastedPower"]},
                $COLUMN_WASTED_PIECES = $COLUMN_WASTED_PIECES + ${stats["wastedPieces"]},
                $COLUMN_PARTIAL_WASTED_POWER = $COLUMN_PARTIAL_WASTED_POWER + ${stats["partialWastedPower"]},
                $COLUMN_PARTIAL_WASTED_PIECES = $COLUMN_PARTIAL_WASTED_PIECES + ${stats["partialWastedPieces"]},
                $COLUMN_END_WASTE_POWER = $COLUMN_END_WASTE_POWER + ${stats["endWastePower"]},
                $COLUMN_END_WASTE_PIECES = $COLUMN_END_WASTE_PIECES + ${stats["endWastePieces"]}
            WHERE $COLUMN_DICE_MATCH_ID = $matchId AND $COLUMN_DICE_PLAYER_ID = $playerId
        """.trimIndent()

        db.execSQL(updateSQL)
        db.close()
    }

    /**
     * Maç için oyuncu zar istatistiklerini al
     */
    fun getDiceStats(matchId: Long, playerId: Long): DiceStatistics? {
        val db = this.readableDatabase
        val cursor = db.rawQuery("""
            SELECT * FROM $TABLE_DICE_STATS
            WHERE $COLUMN_DICE_MATCH_ID = ? AND $COLUMN_DICE_PLAYER_ID = ?
        """, arrayOf(matchId.toString(), playerId.toString()))

        if (cursor.moveToFirst()) {
            val stats = DiceStatistics(
                id = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_DICE_STATS_ID)),
                matchId = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_DICE_MATCH_ID)),
                playerId = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_DICE_PLAYER_ID)),
                dice_1_1 = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_DICE_1_1)),
                dice_1_2 = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_DICE_1_2)),
                dice_1_3 = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_DICE_1_3)),
                dice_1_4 = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_DICE_1_4)),
                dice_1_5 = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_DICE_1_5)),
                dice_1_6 = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_DICE_1_6)),
                dice_2_2 = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_DICE_2_2)),
                dice_2_3 = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_DICE_2_3)),
                dice_2_4 = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_DICE_2_4)),
                dice_2_5 = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_DICE_2_5)),
                dice_2_6 = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_DICE_2_6)),
                dice_3_3 = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_DICE_3_3)),
                dice_3_4 = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_DICE_3_4)),
                dice_3_5 = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_DICE_3_5)),
                dice_3_6 = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_DICE_3_6)),
                dice_4_4 = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_DICE_4_4)),
                dice_4_5 = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_DICE_4_5)),
                dice_4_6 = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_DICE_4_6)),
                dice_5_5 = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_DICE_5_5)),
                dice_5_6 = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_DICE_5_6)),
                dice_6_6 = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_DICE_6_6)),
                totalDicePower = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_TOTAL_DICE_POWER)),
                totalDicePieces = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_TOTAL_DICE_PIECES)),
                doubleCount = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_DOUBLE_COUNT)),
                doublePower = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_DOUBLE_POWER)),
                playedPower = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_PLAYED_POWER)),
                playedPieces = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_PLAYED_PIECES)),
                wastedPower = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_WASTED_POWER)),
                wastedPieces = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_WASTED_PIECES)),
                partialWastedPower = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_PARTIAL_WASTED_POWER)),
                partialWastedPieces = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_PARTIAL_WASTED_PIECES)),
                endWastePower = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_END_WASTE_POWER)),
                endWastePieces = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_END_WASTE_PIECES))
            )
            cursor.close()
            db.close()
            return stats
        }

        cursor.close()
        db.close()
        return null
    }

    /**
     * Zar değerlendirmesi kaydet
     */
    fun saveDiceEvaluation(
        matchId: Long,
        playerId: Long,
        diceCombo: String,
        rating: Int,
        state: String = "OYANDI"
    ): Long {
        val db = this.writableDatabase
        val values = ContentValues()
        values.put(COLUMN_EVAL_MATCH_ID, matchId)
        values.put(COLUMN_EVAL_PLAYER_ID, playerId)
        values.put(COLUMN_EVAL_DICE_COMBO, diceCombo)
        values.put(COLUMN_EVAL_RATING, rating)
        values.put(COLUMN_EVAL_STATE, state)
        values.put(COLUMN_EVAL_TIMESTAMP, System.currentTimeMillis())
        
        val id = db.insert(TABLE_DICE_EVALUATIONS, null, values)
        db.close()
        return id
    }

    // ============== HAREKETLER DOKUMU (ACTIVITY LOG) FONKSİYONLARI ==============

    /**
     * Yeni bir aktivite logu ekle
     */
    fun addActivityLog(
        actionType: String,
        description: String,
        player1Name: String? = null,
        player2Name: String? = null,
        matchId: Long? = null,
        extraData: String? = null
    ): Long {
        val db = this.writableDatabase
        val values = ContentValues()

        val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
        val dateTimeFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        val now = Date()

        values.put(COLUMN_LOG_TIMESTAMP, timeFormat.format(now))
        values.put(COLUMN_LOG_DATETIME, dateTimeFormat.format(now))
        values.put(COLUMN_LOG_ACTION_TYPE, actionType)
        values.put(COLUMN_LOG_DESCRIPTION, description)
        values.put(COLUMN_LOG_PLAYER1_NAME, player1Name)
        values.put(COLUMN_LOG_PLAYER2_NAME, player2Name)
        values.put(COLUMN_LOG_MATCH_ID, matchId)
        values.put(COLUMN_LOG_EXTRA_DATA, extraData)

        val id = db.insert(TABLE_ACTIVITY_LOGS, null, values)
        db.close()
        return id
    }

    /**
     * Tum aktivite loglarini getir (en yeniden en eskiye)
     */
    fun getAllActivityLogs(): List<ActivityLog> {
        val logsList = mutableListOf<ActivityLog>()
        val db = this.readableDatabase
        val cursor = db.rawQuery("""
            SELECT * FROM $TABLE_ACTIVITY_LOGS
            ORDER BY $COLUMN_LOG_ID DESC
        """, null)

        if (cursor.moveToFirst()) {
            do {
                val log = ActivityLog(
                    id = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_LOG_ID)),
                    timestamp = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_LOG_TIMESTAMP)),
                    dateTime = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_LOG_DATETIME)),
                    actionType = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_LOG_ACTION_TYPE)),
                    description = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_LOG_DESCRIPTION)),
                    player1Name = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_LOG_PLAYER1_NAME)),
                    player2Name = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_LOG_PLAYER2_NAME)),
                    matchId = if (cursor.isNull(cursor.getColumnIndexOrThrow(COLUMN_LOG_MATCH_ID))) null
                              else cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_LOG_MATCH_ID)),
                    extraData = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_LOG_EXTRA_DATA))
                )
                logsList.add(log)
            } while (cursor.moveToNext())
        }

        cursor.close()
        db.close()
        return logsList
    }

    /**
     * Belirli bir maca ait aktivite loglarini getir
     */
    fun getActivityLogsByMatch(matchId: Long): List<ActivityLog> {
        val logsList = mutableListOf<ActivityLog>()
        val db = this.readableDatabase
        val cursor = db.rawQuery("""
            SELECT * FROM $TABLE_ACTIVITY_LOGS
            WHERE $COLUMN_LOG_MATCH_ID = ?
            ORDER BY $COLUMN_LOG_ID ASC
        """, arrayOf(matchId.toString()))

        if (cursor.moveToFirst()) {
            do {
                val log = ActivityLog(
                    id = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_LOG_ID)),
                    timestamp = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_LOG_TIMESTAMP)),
                    dateTime = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_LOG_DATETIME)),
                    actionType = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_LOG_ACTION_TYPE)),
                    description = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_LOG_DESCRIPTION)),
                    player1Name = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_LOG_PLAYER1_NAME)),
                    player2Name = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_LOG_PLAYER2_NAME)),
                    matchId = if (cursor.isNull(cursor.getColumnIndexOrThrow(COLUMN_LOG_MATCH_ID))) null
                              else cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_LOG_MATCH_ID)),
                    extraData = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_LOG_EXTRA_DATA))
                )
                logsList.add(log)
            } while (cursor.moveToNext())
        }

        cursor.close()
        db.close()
        return logsList
    }

    /**
     * Belirli tarihler arasindaki aktivite loglarini getir
     */
    fun getActivityLogsByDateRange(startDate: String, endDate: String): List<ActivityLog> {
        val logsList = mutableListOf<ActivityLog>()
        val db = this.readableDatabase
        val cursor = db.rawQuery("""
            SELECT * FROM $TABLE_ACTIVITY_LOGS
            WHERE $COLUMN_LOG_DATETIME BETWEEN ? AND ?
            ORDER BY $COLUMN_LOG_ID DESC
        """, arrayOf(startDate, endDate))

        if (cursor.moveToFirst()) {
            do {
                val log = ActivityLog(
                    id = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_LOG_ID)),
                    timestamp = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_LOG_TIMESTAMP)),
                    dateTime = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_LOG_DATETIME)),
                    actionType = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_LOG_ACTION_TYPE)),
                    description = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_LOG_DESCRIPTION)),
                    player1Name = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_LOG_PLAYER1_NAME)),
                    player2Name = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_LOG_PLAYER2_NAME)),
                    matchId = if (cursor.isNull(cursor.getColumnIndexOrThrow(COLUMN_LOG_MATCH_ID))) null
                              else cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_LOG_MATCH_ID)),
                    extraData = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_LOG_EXTRA_DATA))
                )
                logsList.add(log)
            } while (cursor.moveToNext())
        }

        cursor.close()
        db.close()
        return logsList
    }

    /**
     * Tum aktivite loglarini temizle
     */
    fun clearActivityLogs(): Int {
        val db = this.writableDatabase
        val deleted = db.delete(TABLE_ACTIVITY_LOGS, null, null)
        db.close()
        return deleted
    }

    /**
     * Belirli bir maca ait aktivite loglarini sil
     */
    fun clearActivityLogsByMatch(matchId: Long): Int {
        val db = this.writableDatabase
        val deleted = db.delete(TABLE_ACTIVITY_LOGS, "$COLUMN_LOG_MATCH_ID = ?", arrayOf(matchId.toString()))
        db.close()
        return deleted
    }

    /**
     * Son N adet aktivite logunu getir
     */
    fun getRecentActivityLogs(limit: Int = 100): List<ActivityLog> {
        val logsList = mutableListOf<ActivityLog>()
        val db = this.readableDatabase
        val cursor = db.rawQuery("""
            SELECT * FROM $TABLE_ACTIVITY_LOGS
            ORDER BY $COLUMN_LOG_ID DESC
            LIMIT ?
        """, arrayOf(limit.toString()))

        if (cursor.moveToFirst()) {
            do {
                val log = ActivityLog(
                    id = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_LOG_ID)),
                    timestamp = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_LOG_TIMESTAMP)),
                    dateTime = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_LOG_DATETIME)),
                    actionType = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_LOG_ACTION_TYPE)),
                    description = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_LOG_DESCRIPTION)),
                    player1Name = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_LOG_PLAYER1_NAME)),
                    player2Name = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_LOG_PLAYER2_NAME)),
                    matchId = if (cursor.isNull(cursor.getColumnIndexOrThrow(COLUMN_LOG_MATCH_ID))) null
                              else cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_LOG_MATCH_ID)),
                    extraData = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_LOG_EXTRA_DATA))
                )
                logsList.add(log)
            } while (cursor.moveToNext())
        }

        cursor.close()
        db.close()
        return logsList
    }

    /**
     * Bugunun aktivite loglarini getir
     */
    fun getTodayActivityLogs(): List<ActivityLog> {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val today = dateFormat.format(Date())
        val startDate = "$today 00:00:00"
        val endDate = "$today 23:59:59"
        return getActivityLogsByDateRange(startDate, endDate)
    }

    // ============== ROVANSLI KARSILASMA FONKSİYONLARI (v8 Party/Set yapısı) ==============

    /**
     * Yeni rovansli karsilasma olustur
     * @param totalParties Toplam parti sayısı (örn: 100)
     */
    fun createRematchEncounter(player1Id: Long, player2Id: Long, totalParties: Int, targetScore: Int = 11, trackPipCount: Boolean = true): Long {
        val db = this.writableDatabase
        // Yeni kolonlarin var oldugundan emin ol
        try { db.execSQL("ALTER TABLE $TABLE_REMATCH_ENCOUNTERS ADD COLUMN $COLUMN_ENCOUNTER_TARGET_SCORE INTEGER DEFAULT 11") } catch (_: Exception) {}
        try { db.execSQL("ALTER TABLE $TABLE_REMATCH_ENCOUNTERS ADD COLUMN $COLUMN_ENCOUNTER_TRACK_PIP INTEGER DEFAULT 1") } catch (_: Exception) {}
        val values = ContentValues()
        values.put(COLUMN_ENCOUNTER_PLAYER1_ID, player1Id)
        values.put(COLUMN_ENCOUNTER_PLAYER2_ID, player2Id)
        values.put(COLUMN_ENCOUNTER_TOTAL_PARTIES, totalParties)
        values.put(COLUMN_ENCOUNTER_TARGET_SCORE, targetScore)
        values.put(COLUMN_ENCOUNTER_TRACK_PIP, if (trackPipCount) 1 else 0)
        values.put(COLUMN_ENCOUNTER_CURRENT_ROUND, 1)
        values.put(COLUMN_ENCOUNTER_CURRENT_PARTY_INDEX, 0)
        values.put(COLUMN_ENCOUNTER_CURRENT_GAME_INDEX, 0)
        values.put(COLUMN_ENCOUNTER_STATUS, RematchStatus.ACTIVE.name)
        values.put(COLUMN_ENCOUNTER_CREATED_DATE, getCurrentDateTime())

        val encounterId = db.insert(TABLE_REMATCH_ENCOUNTERS, null, values)

        // Her iki oyuncu icin istatistik kaydi olustur
        if (encounterId != -1L) {
            initializeRematchStats(encounterId, player1Id, db)
            initializeRematchStats(encounterId, player2Id, db)
        }

        db.close()
        return encounterId
    }

    private fun initializeRematchStats(encounterId: Long, playerId: Long, db: SQLiteDatabase) {
        val values = ContentValues()
        values.put(COLUMN_REMATCH_STATS_ENCOUNTER_ID, encounterId)
        values.put(COLUMN_REMATCH_STATS_PLAYER_ID, playerId)
        db.insert(TABLE_REMATCH_ENCOUNTER_STATS, null, values)
    }

    /**
     * Karsilasma icin zar partileri ve setlerini uret ve kaydet
     * Yapi: N parti × (2*targetScore-1) set × 200 zar ciftii
     * @param totalParties Toplam parti sayısı
     * @param targetScore Parti hedef puanı (max el = 2*targetScore-1)
     */
    fun generateAndSaveDiceSets(encounterId: Long, totalParties: Int, targetScore: Int = 11): Boolean {
        val db = this.writableDatabase
        try {
            db.beginTransaction()

            for (partyIndex in 0 until totalParties) {
                // Parti olustur
                val partyValues = ContentValues()
                partyValues.put(COLUMN_PARTY_ENCOUNTER_ID, encounterId)
                partyValues.put(COLUMN_PARTY_INDEX, partyIndex)
                val partyId = db.insert(TABLE_REMATCH_DICE_PARTIES, null, partyValues)

                if (partyId == -1L) {
                    db.endTransaction()
                    db.close()
                    return false
                }

                // Bu parti icin zar setleri olustur (max el = 2*targetScore-1)
                val setsPerParty = DiceGenerator.maxSetsForTargetScore(targetScore)
                for (setIndex in 0 until setsPerParty) {
                    val diceSet = DiceGenerator.generateDiceSet()

                    val setValues = ContentValues()
                    setValues.put(COLUMN_DICE_SET_PARTY_ID, partyId)
                    setValues.put(COLUMN_DICE_SET_INDEX, setIndex)
                    setValues.put(COLUMN_DICE_SET_STARTING_P1, diceSet.startingDicePlayer1)
                    setValues.put(COLUMN_DICE_SET_STARTING_P2, diceSet.startingDicePlayer2)
                    setValues.put(COLUMN_DICE_SET_P1_JSON, DiceGenerator.diceSequenceToJson(diceSet.player1Dice))
                    setValues.put(COLUMN_DICE_SET_P2_JSON, DiceGenerator.diceSequenceToJson(diceSet.player2Dice))

                    db.insert(TABLE_REMATCH_DICE_SETS, null, setValues)
                }
            }

            db.setTransactionSuccessful()
            db.endTransaction()
            db.close()
            return true
        } catch (e: Exception) {
            try { db.endTransaction() } catch (_: Exception) {}
            db.close()
            return false
        }
    }

    /**
     * Karsilasma bilgisini getir
     */
    fun getRematchEncounter(encounterId: Long): RematchEncounter? {
        val db = this.readableDatabase
        val cursor = db.rawQuery("""
            SELECT e.*, p1.name as player1_name, p2.name as player2_name
            FROM $TABLE_REMATCH_ENCOUNTERS e
            LEFT JOIN $TABLE_PLAYERS p1 ON e.$COLUMN_ENCOUNTER_PLAYER1_ID = p1.$COLUMN_PLAYER_ID
            LEFT JOIN $TABLE_PLAYERS p2 ON e.$COLUMN_ENCOUNTER_PLAYER2_ID = p2.$COLUMN_PLAYER_ID
            WHERE e.$COLUMN_ENCOUNTER_ID = ?
        """, arrayOf(encounterId.toString()))

        if (cursor.moveToFirst()) {
            val encounter = RematchEncounter(
                id = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_ENCOUNTER_ID)),
                player1Id = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_ENCOUNTER_PLAYER1_ID)),
                player2Id = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_ENCOUNTER_PLAYER2_ID)),
                player1Name = cursor.getString(cursor.getColumnIndexOrThrow("player1_name")) ?: "",
                player2Name = cursor.getString(cursor.getColumnIndexOrThrow("player2_name")) ?: "",
                totalParties = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_ENCOUNTER_TOTAL_PARTIES)),
                targetScore = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_ENCOUNTER_TARGET_SCORE)),
                trackPipCount = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_ENCOUNTER_TRACK_PIP)) == 1,
                currentRound = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_ENCOUNTER_CURRENT_ROUND)),
                currentPartyIndex = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_ENCOUNTER_CURRENT_PARTY_INDEX)),
                currentGameIndex = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_ENCOUNTER_CURRENT_GAME_INDEX)),
                status = RematchStatus.valueOf(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_ENCOUNTER_STATUS))),
                createdDate = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_ENCOUNTER_CREATED_DATE)),
                completedDate = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_ENCOUNTER_COMPLETED_DATE))
            )
            cursor.close()
            db.close()
            return encounter
        }

        cursor.close()
        db.close()
        return null
    }

    /**
     * Aktif karsilasmalari getir
     */
    fun getActiveRematchEncounters(): List<RematchEncounter> {
        val encounters = mutableListOf<RematchEncounter>()
        val db = this.readableDatabase
        val cursor = db.rawQuery("""
            SELECT e.*, p1.name as player1_name, p2.name as player2_name
            FROM $TABLE_REMATCH_ENCOUNTERS e
            LEFT JOIN $TABLE_PLAYERS p1 ON e.$COLUMN_ENCOUNTER_PLAYER1_ID = p1.$COLUMN_PLAYER_ID
            LEFT JOIN $TABLE_PLAYERS p2 ON e.$COLUMN_ENCOUNTER_PLAYER2_ID = p2.$COLUMN_PLAYER_ID
            WHERE e.$COLUMN_ENCOUNTER_STATUS IN ('ACTIVE', 'ROUND1_COMPLETE', 'ROUND2_ACTIVE')
            ORDER BY e.$COLUMN_ENCOUNTER_CREATED_DATE DESC
        """, null)

        if (cursor.moveToFirst()) {
            do {
                val encounter = RematchEncounter(
                    id = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_ENCOUNTER_ID)),
                    player1Id = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_ENCOUNTER_PLAYER1_ID)),
                    player2Id = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_ENCOUNTER_PLAYER2_ID)),
                    player1Name = cursor.getString(cursor.getColumnIndexOrThrow("player1_name")) ?: "",
                    player2Name = cursor.getString(cursor.getColumnIndexOrThrow("player2_name")) ?: "",
                    totalParties = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_ENCOUNTER_TOTAL_PARTIES)),
                    targetScore = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_ENCOUNTER_TARGET_SCORE)),
                    trackPipCount = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_ENCOUNTER_TRACK_PIP)) == 1,
                    currentRound = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_ENCOUNTER_CURRENT_ROUND)),
                    currentPartyIndex = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_ENCOUNTER_CURRENT_PARTY_INDEX)),
                    currentGameIndex = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_ENCOUNTER_CURRENT_GAME_INDEX)),
                    status = RematchStatus.valueOf(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_ENCOUNTER_STATUS))),
                    createdDate = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_ENCOUNTER_CREATED_DATE)),
                    completedDate = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_ENCOUNTER_COMPLETED_DATE))
                )
                encounters.add(encounter)
            } while (cursor.moveToNext())
        }

        cursor.close()
        db.close()
        return encounters
    }

    /**
     * Tum karsilasmalari getir
     */
    fun getAllRematchEncounters(): List<RematchEncounter> {
        val encounters = mutableListOf<RematchEncounter>()
        val db = this.readableDatabase
        val cursor = db.rawQuery("""
            SELECT e.*, p1.name as player1_name, p2.name as player2_name
            FROM $TABLE_REMATCH_ENCOUNTERS e
            LEFT JOIN $TABLE_PLAYERS p1 ON e.$COLUMN_ENCOUNTER_PLAYER1_ID = p1.$COLUMN_PLAYER_ID
            LEFT JOIN $TABLE_PLAYERS p2 ON e.$COLUMN_ENCOUNTER_PLAYER2_ID = p2.$COLUMN_PLAYER_ID
            ORDER BY e.$COLUMN_ENCOUNTER_CREATED_DATE DESC
        """, null)

        if (cursor.moveToFirst()) {
            do {
                val encounter = RematchEncounter(
                    id = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_ENCOUNTER_ID)),
                    player1Id = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_ENCOUNTER_PLAYER1_ID)),
                    player2Id = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_ENCOUNTER_PLAYER2_ID)),
                    player1Name = cursor.getString(cursor.getColumnIndexOrThrow("player1_name")) ?: "",
                    player2Name = cursor.getString(cursor.getColumnIndexOrThrow("player2_name")) ?: "",
                    totalParties = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_ENCOUNTER_TOTAL_PARTIES)),
                    targetScore = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_ENCOUNTER_TARGET_SCORE)),
                    trackPipCount = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_ENCOUNTER_TRACK_PIP)) == 1,
                    currentRound = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_ENCOUNTER_CURRENT_ROUND)),
                    currentPartyIndex = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_ENCOUNTER_CURRENT_PARTY_INDEX)),
                    currentGameIndex = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_ENCOUNTER_CURRENT_GAME_INDEX)),
                    status = RematchStatus.valueOf(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_ENCOUNTER_STATUS))),
                    createdDate = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_ENCOUNTER_CREATED_DATE)),
                    completedDate = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_ENCOUNTER_COMPLETED_DATE))
                )
                encounters.add(encounter)
            } while (cursor.moveToNext())
        }

        cursor.close()
        db.close()
        return encounters
    }

    /**
     * Belirli bir parti ve oyun icin zar setini getir (round-aware)
     * Round 2'de aynı setleri ters oynatır
     */
    fun getDiceSetForGame(encounterId: Long, partyIndex: Int, setIndex: Int, currentRound: Int = 1): RematchDiceSet? {
        val db = this.readableDatabase

        // Önce party ID'yi bul
        val partyCursor = db.rawQuery("""
            SELECT $COLUMN_PARTY_ID FROM $TABLE_REMATCH_DICE_PARTIES
            WHERE $COLUMN_PARTY_ENCOUNTER_ID = ? AND $COLUMN_PARTY_INDEX = ?
        """, arrayOf(encounterId.toString(), partyIndex.toString()))

        if (!partyCursor.moveToFirst()) {
            partyCursor.close()
            db.close()
            return null
        }

        val partyId = partyCursor.getLong(partyCursor.getColumnIndexOrThrow(COLUMN_PARTY_ID))
        partyCursor.close()

        // Zar setini getir
        val cursor = db.rawQuery("""
            SELECT * FROM $TABLE_REMATCH_DICE_SETS
            WHERE $COLUMN_DICE_SET_PARTY_ID = ? AND $COLUMN_DICE_SET_INDEX = ?
        """, arrayOf(partyId.toString(), setIndex.toString()))

        if (cursor.moveToFirst()) {
            val originalDiceSet = RematchDiceSet(
                id = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_DICE_SET_ID)),
                partyId = partyId,
                setIndex = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_DICE_SET_INDEX)),
                startingDicePlayer1 = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_DICE_SET_STARTING_P1)),
                startingDicePlayer2 = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_DICE_SET_STARTING_P2)),
                player1Dice = DiceGenerator.jsonToDiceSequence(
                    cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_DICE_SET_P1_JSON))
                ),
                player2Dice = DiceGenerator.jsonToDiceSequence(
                    cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_DICE_SET_P2_JSON))
                )
            )
            cursor.close()
            db.close()
            
            // Round 2'de aynı setleri ters oynat (oyuncu pozisyonlarını değiştir)
            return if (currentRound == 2) {
                // Activity log ekle (sadece ilk kez reverse edildiğinde)
                val setId = DiceGenerator.generateSetId(partyIndex, setIndex, 0)
                addActivityLog(
                    actionType = ActionTypes.REMATCH_DICE_REVERSE,
                    description = "Tur 2: Aynı zar setleri ters oynatıldı (Set: $setId)",
                    extraData = "original_round=1,reverse_round=2,party_index=$partyIndex,set_index=$setIndex"
                )
                
                RematchDiceSet(
                    id = originalDiceSet.id,
                    partyId = originalDiceSet.partyId,
                    setIndex = originalDiceSet.setIndex,
                    // ✅ SADECE ZAR SEKVENSLERİNİ DEĞİŞTİR - Oyuncu pozisyonları aynı kalır
                    // İlk partide Player1'e gelen zarlar → Rövanş partisinde Player2'ye gider
                    startingDicePlayer1 = originalDiceSet.startingDicePlayer2,
                    startingDicePlayer2 = originalDiceSet.startingDicePlayer1,
                    // Player1 aynı pozisyonda kalır ama Player2'nin zarlarını alır
                    player1Dice = originalDiceSet.player2Dice,
                    player2Dice = originalDiceSet.player1Dice
                )
            } else {
                originalDiceSet
            }
        }

        cursor.close()
        db.close()
        return null
    }

    /**
     * Oyun (el) sonucunu kaydet
     */
    fun saveRematchGameResult(
        encounterId: Long,
        partyIndex: Int,
        setIndex: Int,
        roundNumber: Int,
        leftPlayerId: Long,
        rightPlayerId: Long,
        winnerId: Long,
        winType: String,
        cubeValue: Int,
        finalScore: Int,
        loserPipCount: Int,
        dicePairsUsed: Int,
        doublerPlayerId: Long? = null,
        leftDiceTotal: Int = 0,
        rightDiceTotal: Int = 0,
        leftDoublesCount: Int = 0,
        rightDoublesCount: Int = 0
    ): Long {
        val db = this.writableDatabase
        val values = ContentValues()
        values.put(COLUMN_GAME_RESULT_ENCOUNTER_ID, encounterId)
        values.put(COLUMN_GAME_RESULT_PARTY_INDEX, partyIndex)
        values.put(COLUMN_GAME_RESULT_SET_INDEX, setIndex)
        values.put(COLUMN_GAME_RESULT_ROUND_NUMBER, roundNumber)
        values.put(COLUMN_GAME_RESULT_LEFT_PLAYER_ID, leftPlayerId)
        values.put(COLUMN_GAME_RESULT_RIGHT_PLAYER_ID, rightPlayerId)
        values.put(COLUMN_GAME_RESULT_WINNER_ID, winnerId)
        values.put(COLUMN_GAME_RESULT_WIN_TYPE, winType)
        values.put(COLUMN_GAME_RESULT_CUBE_VALUE, cubeValue)
        values.put(COLUMN_GAME_RESULT_FINAL_SCORE, finalScore)
        values.put(COLUMN_GAME_RESULT_LOSER_PIP, loserPipCount)
        values.put(COLUMN_GAME_RESULT_DICE_PAIRS_USED, dicePairsUsed)
        values.put(COLUMN_GAME_RESULT_DATE, getCurrentDateTime())
        if (doublerPlayerId != null) {
            values.put(COLUMN_GAME_RESULT_DOUBLER_ID, doublerPlayerId)
        }
        values.put(COLUMN_GAME_RESULT_LEFT_DICE_TOTAL, leftDiceTotal)
        values.put(COLUMN_GAME_RESULT_RIGHT_DICE_TOTAL, rightDiceTotal)
        values.put(COLUMN_GAME_RESULT_LEFT_DOUBLES, leftDoublesCount)
        values.put(COLUMN_GAME_RESULT_RIGHT_DOUBLES, rightDoublesCount)

        val resultId = db.insert(TABLE_REMATCH_GAME_RESULTS, null, values)

        // Istatistikleri guncelle
        if (resultId != -1L) {
            updateRematchGameStats(encounterId, winnerId, roundNumber, finalScore, winType, db)
        }

        db.close()
        return resultId
    }

    /**
     * Oyun istatistiklerini guncelle (tek el icin)
     */
    private fun updateRematchGameStats(
        encounterId: Long,
        winnerId: Long,
        roundNumber: Int,
        points: Int,
        winType: String,
        db: SQLiteDatabase
    ) {
        val winsColumn = if (roundNumber == 1) COLUMN_REMATCH_STATS_R1_WINS else COLUMN_REMATCH_STATS_R2_WINS
        val pointsColumn = if (roundNumber == 1) COLUMN_REMATCH_STATS_R1_POINTS else COLUMN_REMATCH_STATS_R2_POINTS
        val marsColumn = if (roundNumber == 1) COLUMN_REMATCH_STATS_R1_MARS else COLUMN_REMATCH_STATS_R2_MARS
        val bgColumn = if (roundNumber == 1) COLUMN_REMATCH_STATS_R1_BG else COLUMN_REMATCH_STATS_R2_BG

        var updateSQL = """
            UPDATE $TABLE_REMATCH_ENCOUNTER_STATS
            SET $winsColumn = $winsColumn + 1,
                $pointsColumn = $pointsColumn + $points,
                $COLUMN_REMATCH_STATS_TOTAL_WINS = $COLUMN_REMATCH_STATS_TOTAL_WINS + 1,
                $COLUMN_REMATCH_STATS_TOTAL_POINTS = $COLUMN_REMATCH_STATS_TOTAL_POINTS + $points
        """

        if (winType == WinTypes.MARS) {
            updateSQL += ", $marsColumn = $marsColumn + 1"
        } else if (winType == WinTypes.BACKGAMMON) {
            updateSQL += ", $bgColumn = $bgColumn + 1"
        }

        updateSQL += " WHERE $COLUMN_REMATCH_STATS_ENCOUNTER_ID = $encounterId AND $COLUMN_REMATCH_STATS_PLAYER_ID = $winnerId"

        db.execSQL(updateSQL)
    }

    /**
     * Oyun istatistiklerini geri al (undo icin)
     */
    private fun reverseRematchGameStats(
        encounterId: Long,
        winnerId: Long,
        roundNumber: Int,
        points: Int,
        winType: String,
        db: SQLiteDatabase
    ) {
        val winsColumn = if (roundNumber == 1) COLUMN_REMATCH_STATS_R1_WINS else COLUMN_REMATCH_STATS_R2_WINS
        val pointsColumn = if (roundNumber == 1) COLUMN_REMATCH_STATS_R1_POINTS else COLUMN_REMATCH_STATS_R2_POINTS
        val marsColumn = if (roundNumber == 1) COLUMN_REMATCH_STATS_R1_MARS else COLUMN_REMATCH_STATS_R2_MARS
        val bgColumn = if (roundNumber == 1) COLUMN_REMATCH_STATS_R1_BG else COLUMN_REMATCH_STATS_R2_BG

        var updateSQL = """
            UPDATE $TABLE_REMATCH_ENCOUNTER_STATS
            SET $winsColumn = MAX(0, $winsColumn - 1),
                $pointsColumn = MAX(0, $pointsColumn - $points),
                $COLUMN_REMATCH_STATS_TOTAL_WINS = MAX(0, $COLUMN_REMATCH_STATS_TOTAL_WINS - 1),
                $COLUMN_REMATCH_STATS_TOTAL_POINTS = MAX(0, $COLUMN_REMATCH_STATS_TOTAL_POINTS - $points)
        """

        if (winType == WinTypes.MARS) {
            updateSQL += ", $marsColumn = MAX(0, $marsColumn - 1)"
        } else if (winType == WinTypes.BACKGAMMON) {
            updateSQL += ", $bgColumn = MAX(0, $bgColumn - 1)"
        }

        updateSQL += " WHERE $COLUMN_REMATCH_STATS_ENCOUNTER_ID = $encounterId AND $COLUMN_REMATCH_STATS_PLAYER_ID = $winnerId"

        db.execSQL(updateSQL)
    }

    /**
     * Tek bir rematch oyun sonucunu sil (undo icin)
     * Istatistikleri de geri alir
     */
    fun deleteRematchGameResult(resultId: Long): Int {
        val db = this.writableDatabase

        // Once sonuc verisini al (istatistik geri alma icin)
        val cursor = db.rawQuery(
            "SELECT * FROM $TABLE_REMATCH_GAME_RESULTS WHERE $COLUMN_GAME_RESULT_ID = ?",
            arrayOf(resultId.toString())
        )

        if (cursor.moveToFirst()) {
            val encounterId = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_GAME_RESULT_ENCOUNTER_ID))
            val winnerIdIdx = cursor.getColumnIndexOrThrow(COLUMN_GAME_RESULT_WINNER_ID)
            if (!cursor.isNull(winnerIdIdx)) {
                val winnerId = cursor.getLong(winnerIdIdx)
                val roundNumber = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_GAME_RESULT_ROUND_NUMBER))
                val finalScore = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_GAME_RESULT_FINAL_SCORE))
                val winType = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_GAME_RESULT_WIN_TYPE)) ?: ""

                // Istatistikleri geri al
                reverseRematchGameStats(encounterId, winnerId, roundNumber, finalScore, winType, db)
            }
        }
        cursor.close()

        // Kaydi sil
        val result = db.delete(TABLE_REMATCH_GAME_RESULTS, "$COLUMN_GAME_RESULT_ID = ?", arrayOf(resultId.toString()))
        db.close()
        return result
    }

    /**
     * Parti kazanan istatistiklerini guncelle
     */
    private fun updateRematchPartyStats(
        encounterId: Long,
        winnerId: Long,
        roundNumber: Int,
        db: SQLiteDatabase
    ) {
        val partiesColumn = if (roundNumber == 1) COLUMN_REMATCH_STATS_R1_PARTIES else COLUMN_REMATCH_STATS_R2_PARTIES

        val updateSQL = """
            UPDATE $TABLE_REMATCH_ENCOUNTER_STATS
            SET $partiesColumn = $partiesColumn + 1,
                $COLUMN_REMATCH_STATS_TOTAL_PARTIES = $COLUMN_REMATCH_STATS_TOTAL_PARTIES + 1
            WHERE $COLUMN_REMATCH_STATS_ENCOUNTER_ID = $encounterId AND $COLUMN_REMATCH_STATS_PLAYER_ID = $winnerId
        """

        db.execSQL(updateSQL)
    }

    /**
     * Parti sonucunu kaydet (11'lik parti bitti)
     */
    fun saveRematchPartyResult(
        encounterId: Long,
        partyIndex: Int,
        roundNumber: Int,
        player1Score: Int,
        player2Score: Int,
        winnerId: Long,
        totalGamesPlayed: Int
    ): Long {
        val db = this.writableDatabase
        val values = ContentValues()
        values.put(COLUMN_PARTY_RESULT_ENCOUNTER_ID, encounterId)
        values.put(COLUMN_PARTY_RESULT_PARTY_INDEX, partyIndex)
        values.put(COLUMN_PARTY_RESULT_ROUND_NUMBER, roundNumber)
        values.put(COLUMN_PARTY_RESULT_P1_SCORE, player1Score)
        values.put(COLUMN_PARTY_RESULT_P2_SCORE, player2Score)
        values.put(COLUMN_PARTY_RESULT_WINNER_ID, winnerId)
        values.put(COLUMN_PARTY_RESULT_TOTAL_GAMES, totalGamesPlayed)
        values.put(COLUMN_PARTY_RESULT_DATE, getCurrentDateTime())

        val resultId = db.insert(TABLE_REMATCH_PARTY_RESULTS, null, values)

        // Parti kazanma istatistiklerini guncelle
        if (resultId != -1L) {
            updateRematchPartyStats(encounterId, winnerId, roundNumber, db)
        }

        db.close()
        return resultId
    }

    /**
     * Parti sonucunu sil (parti sonu geri alma icin)
     * Parti kazanma istatistiklerini de geri alir
     */
    fun deleteRematchPartyResult(encounterId: Long, partyIndex: Int, roundNumber: Int): Int {
        val db = this.writableDatabase
        val args = arrayOf(encounterId.toString(), partyIndex.toString(), roundNumber.toString())
        val where = "$COLUMN_PARTY_RESULT_ENCOUNTER_ID = ? AND " +
                "$COLUMN_PARTY_RESULT_PARTY_INDEX = ? AND " +
                "$COLUMN_PARTY_RESULT_ROUND_NUMBER = ?"

        // Once kazanani al (istatistik geri alma icin)
        val cursor = db.rawQuery(
            "SELECT $COLUMN_PARTY_RESULT_WINNER_ID FROM $TABLE_REMATCH_PARTY_RESULTS WHERE $where",
            args
        )
        if (cursor.moveToFirst() && !cursor.isNull(0)) {
            reverseRematchPartyStats(encounterId, cursor.getLong(0), roundNumber, db)
        }
        cursor.close()

        val result = db.delete(TABLE_REMATCH_PARTY_RESULTS, where, args)
        db.close()
        return result
    }

    /**
     * Parti kazanan istatistiklerini geri al (undo icin)
     */
    private fun reverseRematchPartyStats(
        encounterId: Long,
        winnerId: Long,
        roundNumber: Int,
        db: SQLiteDatabase
    ) {
        val partiesColumn = if (roundNumber == 1) COLUMN_REMATCH_STATS_R1_PARTIES else COLUMN_REMATCH_STATS_R2_PARTIES

        db.execSQL("""
            UPDATE $TABLE_REMATCH_ENCOUNTER_STATS
            SET $partiesColumn = MAX(0, $partiesColumn - 1),
                $COLUMN_REMATCH_STATS_TOTAL_PARTIES = MAX(0, $COLUMN_REMATCH_STATS_TOTAL_PARTIES - 1)
            WHERE $COLUMN_REMATCH_STATS_ENCOUNTER_ID = $encounterId AND $COLUMN_REMATCH_STATS_PLAYER_ID = $winnerId
        """)
    }

    /**
     * Karsilasmayi onceki haline dondur (parti sonu geri alma icin)
     * Tur, parti/oyun indeksi ve durum birlikte geri yuklenir
     */
    fun restoreEncounterState(
        encounterId: Long,
        currentRound: Int,
        partyIndex: Int,
        gameIndex: Int,
        status: String
    ) {
        val db = this.writableDatabase
        val values = ContentValues()
        values.put(COLUMN_ENCOUNTER_CURRENT_ROUND, currentRound)
        values.put(COLUMN_ENCOUNTER_CURRENT_PARTY_INDEX, partyIndex)
        values.put(COLUMN_ENCOUNTER_CURRENT_GAME_INDEX, gameIndex)
        values.put(COLUMN_ENCOUNTER_STATUS, status)
        if (status != RematchStatus.COMPLETED.name) {
            values.putNull(COLUMN_ENCOUNTER_COMPLETED_DATE)
        }
        db.update(TABLE_REMATCH_ENCOUNTERS, values, "$COLUMN_ENCOUNTER_ID = ?", arrayOf(encounterId.toString()))
        db.close()
    }

    /**
     * Karsilasma ilerlemesini guncelle (parti ve oyun indeksi)
     */
    fun updateEncounterProgress(encounterId: Long, partyIndex: Int, gameIndex: Int) {
        val db = this.writableDatabase
        val values = ContentValues()
        values.put(COLUMN_ENCOUNTER_CURRENT_PARTY_INDEX, partyIndex)
        values.put(COLUMN_ENCOUNTER_CURRENT_GAME_INDEX, gameIndex)
        db.update(TABLE_REMATCH_ENCOUNTERS, values, "$COLUMN_ENCOUNTER_ID = ?", arrayOf(encounterId.toString()))
        db.close()
    }

    /**
     * Rovans turuna gec
     */
    fun advanceToRematchRound(encounterId: Long) {
        val db = this.writableDatabase
        val values = ContentValues()
        values.put(COLUMN_ENCOUNTER_CURRENT_ROUND, 2)
        values.put(COLUMN_ENCOUNTER_CURRENT_PARTY_INDEX, 0)
        values.put(COLUMN_ENCOUNTER_CURRENT_GAME_INDEX, 0)
        values.put(COLUMN_ENCOUNTER_STATUS, RematchStatus.ROUND2_ACTIVE.name)
        db.update(TABLE_REMATCH_ENCOUNTERS, values, "$COLUMN_ENCOUNTER_ID = ?", arrayOf(encounterId.toString()))
        db.close()
    }

    /**
     * Ilk turu tamamla (rovans bekleniyor)
     */
    fun completeFirstRound(encounterId: Long) {
        val db = this.writableDatabase
        val values = ContentValues()
        values.put(COLUMN_ENCOUNTER_STATUS, RematchStatus.ROUND1_COMPLETE.name)
        db.update(TABLE_REMATCH_ENCOUNTERS, values, "$COLUMN_ENCOUNTER_ID = ?", arrayOf(encounterId.toString()))
        db.close()
    }

    /**
     * Karsilasmyi tamamla
     */
    fun completeEncounter(encounterId: Long) {
        val db = this.writableDatabase
        val values = ContentValues()
        values.put(COLUMN_ENCOUNTER_STATUS, RematchStatus.COMPLETED.name)
        values.put(COLUMN_ENCOUNTER_COMPLETED_DATE, getCurrentDateTime())
        db.update(TABLE_REMATCH_ENCOUNTERS, values, "$COLUMN_ENCOUNTER_ID = ?", arrayOf(encounterId.toString()))
        db.close()
    }

    /**
     * Karsilasmyi iptal et
     */
    fun cancelEncounter(encounterId: Long) {
        val db = this.writableDatabase
        val values = ContentValues()
        values.put(COLUMN_ENCOUNTER_STATUS, RematchStatus.CANCELLED.name)
        values.put(COLUMN_ENCOUNTER_COMPLETED_DATE, getCurrentDateTime())
        db.update(TABLE_REMATCH_ENCOUNTERS, values, "$COLUMN_ENCOUNTER_ID = ?", arrayOf(encounterId.toString()))
        db.close()
    }

    /**
     * Karsilasma istatistiklerini getir
     */
    fun getRematchEncounterStats(encounterId: Long): List<RematchEncounterStats> {
        val statsList = mutableListOf<RematchEncounterStats>()
        val db = this.readableDatabase
        val cursor = db.rawQuery("""
            SELECT s.*, p.name as player_name
            FROM $TABLE_REMATCH_ENCOUNTER_STATS s
            LEFT JOIN $TABLE_PLAYERS p ON s.$COLUMN_REMATCH_STATS_PLAYER_ID = p.$COLUMN_PLAYER_ID
            WHERE s.$COLUMN_REMATCH_STATS_ENCOUNTER_ID = ?
        """, arrayOf(encounterId.toString()))

        if (cursor.moveToFirst()) {
            do {
                val stats = RematchEncounterStats(
                    id = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_REMATCH_STATS_ID)),
                    encounterId = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_REMATCH_STATS_ENCOUNTER_ID)),
                    playerId = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_REMATCH_STATS_PLAYER_ID)),
                    playerName = cursor.getString(cursor.getColumnIndexOrThrow("player_name")) ?: "",
                    round1PartiesWon = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_REMATCH_STATS_R1_PARTIES)),
                    round1GamesWon = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_REMATCH_STATS_R1_WINS)),
                    round1Points = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_REMATCH_STATS_R1_POINTS)),
                    round2PartiesWon = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_REMATCH_STATS_R2_PARTIES)),
                    round2GamesWon = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_REMATCH_STATS_R2_WINS)),
                    round2Points = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_REMATCH_STATS_R2_POINTS)),
                    totalPartiesWon = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_REMATCH_STATS_TOTAL_PARTIES)),
                    totalGamesWon = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_REMATCH_STATS_TOTAL_WINS)),
                    totalPoints = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_REMATCH_STATS_TOTAL_POINTS))
                )
                statsList.add(stats)
            } while (cursor.moveToNext())
        }

        cursor.close()
        db.close()
        return statsList
    }

    /**
     * Belirli bir karsilasmanin oyun sonuclarini getir
     */
    fun getRematchGameResults(encounterId: Long, roundNumber: Int? = null, partyIndex: Int? = null): List<RematchGameResult> {
        val resultsList = mutableListOf<RematchGameResult>()
        val db = this.readableDatabase

        val whereClause = StringBuilder("$COLUMN_GAME_RESULT_ENCOUNTER_ID = ?")
        val args = mutableListOf(encounterId.toString())

        if (roundNumber != null) {
            whereClause.append(" AND $COLUMN_GAME_RESULT_ROUND_NUMBER = ?")
            args.add(roundNumber.toString())
        }

        if (partyIndex != null) {
            whereClause.append(" AND $COLUMN_GAME_RESULT_PARTY_INDEX = ?")
            args.add(partyIndex.toString())
        }

        val query = """
            SELECT * FROM $TABLE_REMATCH_GAME_RESULTS
            WHERE $whereClause
            ORDER BY $COLUMN_GAME_RESULT_ROUND_NUMBER ASC, $COLUMN_GAME_RESULT_PARTY_INDEX ASC, $COLUMN_GAME_RESULT_SET_INDEX ASC
        """

        val cursor = db.rawQuery(query, args.toTypedArray())

        if (cursor.moveToFirst()) {
            do {
                val result = RematchGameResult(
                    id = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_GAME_RESULT_ID)),
                    encounterId = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_GAME_RESULT_ENCOUNTER_ID)),
                    partyIndex = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_GAME_RESULT_PARTY_INDEX)),
                    setIndex = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_GAME_RESULT_SET_INDEX)),
                    roundNumber = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_GAME_RESULT_ROUND_NUMBER)),
                    leftPlayerId = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_GAME_RESULT_LEFT_PLAYER_ID)),
                    rightPlayerId = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_GAME_RESULT_RIGHT_PLAYER_ID)),
                    winnerId = if (cursor.isNull(cursor.getColumnIndexOrThrow(COLUMN_GAME_RESULT_WINNER_ID))) null
                               else cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_GAME_RESULT_WINNER_ID)),
                    winType = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_GAME_RESULT_WIN_TYPE)),
                    cubeValue = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_GAME_RESULT_CUBE_VALUE)),
                    finalScore = if (cursor.isNull(cursor.getColumnIndexOrThrow(COLUMN_GAME_RESULT_FINAL_SCORE))) null
                                 else cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_GAME_RESULT_FINAL_SCORE)),
                    loserPipCount = if (cursor.isNull(cursor.getColumnIndexOrThrow(COLUMN_GAME_RESULT_LOSER_PIP))) null
                                    else cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_GAME_RESULT_LOSER_PIP)),
                    dicePairsUsed = if (cursor.isNull(cursor.getColumnIndexOrThrow(COLUMN_GAME_RESULT_DICE_PAIRS_USED))) null
                                    else cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_GAME_RESULT_DICE_PAIRS_USED)),
                    gameDate = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_GAME_RESULT_DATE)),
                    doublerPlayerId = try {
                        val idx = cursor.getColumnIndexOrThrow(COLUMN_GAME_RESULT_DOUBLER_ID)
                        if (cursor.isNull(idx)) null else cursor.getLong(idx)
                    } catch (e: Exception) { null },
                    leftDiceTotal = try { cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_GAME_RESULT_LEFT_DICE_TOTAL)) } catch (e: Exception) { 0 },
                    rightDiceTotal = try { cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_GAME_RESULT_RIGHT_DICE_TOTAL)) } catch (e: Exception) { 0 },
                    leftDoublesCount = try { cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_GAME_RESULT_LEFT_DOUBLES)) } catch (e: Exception) { 0 },
                    rightDoublesCount = try { cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_GAME_RESULT_RIGHT_DOUBLES)) } catch (e: Exception) { 0 }
                )
                resultsList.add(result)
            } while (cursor.moveToNext())
        }

        cursor.close()
        db.close()
        return resultsList
    }

    /**
     * Belirli bir karsilasmanin parti sonuclarini getir
     */
    fun getRematchPartyResults(encounterId: Long, roundNumber: Int? = null): List<RematchPartyResult> {
        val resultsList = mutableListOf<RematchPartyResult>()
        val db = this.readableDatabase

        val query = if (roundNumber != null) {
            """
                SELECT * FROM $TABLE_REMATCH_PARTY_RESULTS
                WHERE $COLUMN_PARTY_RESULT_ENCOUNTER_ID = ? AND $COLUMN_PARTY_RESULT_ROUND_NUMBER = ?
                ORDER BY $COLUMN_PARTY_RESULT_PARTY_INDEX ASC
            """
        } else {
            """
                SELECT * FROM $TABLE_REMATCH_PARTY_RESULTS
                WHERE $COLUMN_PARTY_RESULT_ENCOUNTER_ID = ?
                ORDER BY $COLUMN_PARTY_RESULT_ROUND_NUMBER ASC, $COLUMN_PARTY_RESULT_PARTY_INDEX ASC
            """
        }

        val cursor = if (roundNumber != null) {
            db.rawQuery(query, arrayOf(encounterId.toString(), roundNumber.toString()))
        } else {
            db.rawQuery(query, arrayOf(encounterId.toString()))
        }

        if (cursor.moveToFirst()) {
            do {
                val result = RematchPartyResult(
                    id = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_PARTY_RESULT_ID)),
                    encounterId = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_PARTY_RESULT_ENCOUNTER_ID)),
                    partyIndex = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_PARTY_RESULT_PARTY_INDEX)),
                    roundNumber = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_PARTY_RESULT_ROUND_NUMBER)),
                    player1Score = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_PARTY_RESULT_P1_SCORE)),
                    player2Score = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_PARTY_RESULT_P2_SCORE)),
                    winnerId = if (cursor.isNull(cursor.getColumnIndexOrThrow(COLUMN_PARTY_RESULT_WINNER_ID))) null
                               else cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_PARTY_RESULT_WINNER_ID)),
                    totalGamesPlayed = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_PARTY_RESULT_TOTAL_GAMES)),
                    partyDate = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_PARTY_RESULT_DATE))
                )
                resultsList.add(result)
            } while (cursor.moveToNext())
        }

        cursor.close()
        db.close()
        return resultsList
    }

    /**
     * Ilk turda oynanan oyunun rovanstaki sonucunu getir
     * @return PreviousGameInfo - Ilk turda bu parti/set icin oynanan oyunun sonucu
     */
    fun getPreviousRoundGameResult(encounterId: Long, partyIndex: Int, setIndex: Int): PreviousGameInfo {
        val db = this.readableDatabase
        val cursor = db.rawQuery("""
            SELECT g.*, p.name as winner_name
            FROM $TABLE_REMATCH_GAME_RESULTS g
            LEFT JOIN $TABLE_PLAYERS p ON g.$COLUMN_GAME_RESULT_WINNER_ID = p.$COLUMN_PLAYER_ID
            WHERE g.$COLUMN_GAME_RESULT_ENCOUNTER_ID = ?
              AND g.$COLUMN_GAME_RESULT_PARTY_INDEX = ?
              AND g.$COLUMN_GAME_RESULT_SET_INDEX = ?
              AND g.$COLUMN_GAME_RESULT_ROUND_NUMBER = 1
        """, arrayOf(encounterId.toString(), partyIndex.toString(), setIndex.toString()))

        val result = if (cursor.moveToFirst()) {
            PreviousGameInfo(
                wasPlayed = true,
                winnerId = if (cursor.isNull(cursor.getColumnIndexOrThrow(COLUMN_GAME_RESULT_WINNER_ID))) null
                           else cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_GAME_RESULT_WINNER_ID)),
                winnerName = cursor.getString(cursor.getColumnIndexOrThrow("winner_name")),
                winType = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_GAME_RESULT_WIN_TYPE)),
                loserPipCount = if (cursor.isNull(cursor.getColumnIndexOrThrow(COLUMN_GAME_RESULT_LOSER_PIP))) null
                                else cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_GAME_RESULT_LOSER_PIP)),
                dicePairsUsed = if (cursor.isNull(cursor.getColumnIndexOrThrow(COLUMN_GAME_RESULT_DICE_PAIRS_USED))) null
                                else cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_GAME_RESULT_DICE_PAIRS_USED))
            )
        } else {
            PreviousGameInfo(wasPlayed = false)
        }

        cursor.close()
        db.close()
        return result
    }

    /**
     * Karsilasmyi ve ilgili tum verileri sil
     */
    /**
     * Bir parti icindeki mevcut toplam skoru hesapla
     * @return Pair(player1Score, player2Score)
     */
    fun getPartyScore(encounterId: Long, partyIndex: Int, roundNumber: Int): Pair<Int, Int> {
        val encounter = getRematchEncounter(encounterId) ?: return Pair(0, 0)
        val db = this.readableDatabase

        val cursor = db.rawQuery("""
            SELECT $COLUMN_GAME_RESULT_WINNER_ID, SUM($COLUMN_GAME_RESULT_FINAL_SCORE) as total_score
            FROM $TABLE_REMATCH_GAME_RESULTS
            WHERE $COLUMN_GAME_RESULT_ENCOUNTER_ID = ?
              AND $COLUMN_GAME_RESULT_PARTY_INDEX = ?
              AND $COLUMN_GAME_RESULT_ROUND_NUMBER = ?
            GROUP BY $COLUMN_GAME_RESULT_WINNER_ID
        """, arrayOf(encounterId.toString(), partyIndex.toString(), roundNumber.toString()))

        var player1Score = 0
        var player2Score = 0

        if (cursor.moveToFirst()) {
            do {
                val winnerId = cursor.getLong(0)
                val score = cursor.getInt(1)
                if (winnerId == encounter.player1Id) player1Score = score
                else if (winnerId == encounter.player2Id) player2Score = score
            } while (cursor.moveToNext())
        }
        cursor.close()
        db.close()
        return Pair(player1Score, player2Score)
    }

    /**
     * Bir parti icin karsilastirma verisi getir
     * Her iki turun oyun sonuclarini setIndex ile eslestir
     */
    fun getPartyComparisonData(encounterId: Long, partyIndex: Int): PartyComparisonData {
        val round1Games = getRematchGameResults(encounterId, roundNumber = 1, partyIndex = partyIndex)
        val round2Games = getRematchGameResults(encounterId, roundNumber = 2, partyIndex = partyIndex)

        val round1PartyResult = getRematchPartyResults(encounterId, roundNumber = 1)
            .find { it.partyIndex == partyIndex }
        val round2PartyResult = getRematchPartyResults(encounterId, roundNumber = 2)
            .find { it.partyIndex == partyIndex }

        val maxSetIndex = maxOf(
            round1Games.maxOfOrNull { it.setIndex } ?: -1,
            round2Games.maxOfOrNull { it.setIndex } ?: -1
        )

        val gameComparisons = if (maxSetIndex >= 0) {
            (0..maxSetIndex).map { setIdx ->
                GameComparisonRow(
                    setIndex = setIdx,
                    round1Result = round1Games.find { it.setIndex == setIdx },
                    round2Result = round2Games.find { it.setIndex == setIdx }
                )
            }
        } else {
            emptyList()
        }

        // Istatistikleri hesapla
        val round1TotalDicePairs = round1Games.sumOf { it.dicePairsUsed ?: 0 }
        val round2TotalDicePairs = round2Games.sumOf { it.dicePairsUsed ?: 0 }
        val round1MarsCount = round1Games.count { it.winType == WinTypes.MARS }
        val round2MarsCount = round2Games.count { it.winType == WinTypes.MARS }
        val round1BackgammonCount = round1Games.count { it.winType == WinTypes.BACKGAMMON }
        val round2BackgammonCount = round2Games.count { it.winType == WinTypes.BACKGAMMON }
        val round1MaxCube = round1Games.maxOfOrNull { it.cubeValue } ?: 1
        val round2MaxCube = round2Games.maxOfOrNull { it.cubeValue } ?: 1

        // Zar istatistikleri
        val round1TotalDiceUnits = round1Games.sumOf { it.leftDiceTotal + it.rightDiceTotal }
        val round2TotalDiceUnits = round2Games.sumOf { it.leftDiceTotal + it.rightDiceTotal }
        val round1TotalDoubles = round1Games.sumOf { it.leftDoublesCount + it.rightDoublesCount }
        val round2TotalDoubles = round2Games.sumOf { it.leftDoublesCount + it.rightDoublesCount }
        val round1ResignCount = round1Games.count { it.winType == WinTypes.RESIGN }
        val round2ResignCount = round2Games.count { it.winType == WinTypes.RESIGN }
        val round1CubeUsedCount = round1Games.count { it.cubeValue > 1 }
        val round2CubeUsedCount = round2Games.count { it.cubeValue > 1 }

        // Ayni/farkli kazanan sayilari
        var sameWinnerCount = 0
        var differentWinnerCount = 0
        gameComparisons.forEach { row ->
            val r1Winner = row.round1Result?.winnerId
            val r2Winner = row.round2Result?.winnerId
            if (r1Winner != null && r2Winner != null) {
                if (r1Winner == r2Winner) sameWinnerCount++
                else differentWinnerCount++
            }
        }

        return PartyComparisonData(
            partyIndex = partyIndex,
            round1PartyResult = round1PartyResult,
            round2PartyResult = round2PartyResult,
            gameComparisons = gameComparisons,
            round1TotalDicePairs = round1TotalDicePairs,
            round2TotalDicePairs = round2TotalDicePairs,
            round1MarsCount = round1MarsCount,
            round2MarsCount = round2MarsCount,
            round1BackgammonCount = round1BackgammonCount,
            round2BackgammonCount = round2BackgammonCount,
            round1MaxCube = round1MaxCube,
            round2MaxCube = round2MaxCube,
            sameWinnerCount = sameWinnerCount,
            differentWinnerCount = differentWinnerCount,
            round1TotalDiceUnits = round1TotalDiceUnits,
            round2TotalDiceUnits = round2TotalDiceUnits,
            round1TotalDoubles = round1TotalDoubles,
            round2TotalDoubles = round2TotalDoubles,
            round1ResignCount = round1ResignCount,
            round2ResignCount = round2ResignCount,
            round1CubeUsedCount = round1CubeUsedCount,
            round2CubeUsedCount = round2CubeUsedCount
        )
    }

    // ✅ Tüm partilerin karşılaştırma verilerini getir
    fun getAllPartiesComparisonData(encounterId: Long): List<PartyComparisonData> {
        val encounter = getRematchEncounter(encounterId) ?: return emptyList()
        val allPartiesData = mutableListOf<PartyComparisonData>()
        
        // Tüm partiler için (0'dan totalParties-1'e kadar)
        for (partyIndex in 0 until encounter.totalParties) {
            val partyData = getPartyComparisonData(encounterId, partyIndex)
            allPartiesData.add(partyData)
        }
        
        return allPartiesData
    }

    fun deleteRematchEncounter(encounterId: Long): Int {
        val db = this.writableDatabase

        // Önce parti ID'lerini al
        val partyCursor = db.rawQuery("""
            SELECT $COLUMN_PARTY_ID FROM $TABLE_REMATCH_DICE_PARTIES
            WHERE $COLUMN_PARTY_ENCOUNTER_ID = ?
        """, arrayOf(encounterId.toString()))

        val partyIds = mutableListOf<Long>()
        if (partyCursor.moveToFirst()) {
            do {
                partyIds.add(partyCursor.getLong(partyCursor.getColumnIndexOrThrow(COLUMN_PARTY_ID)))
            } while (partyCursor.moveToNext())
        }
        partyCursor.close()

        // Zar setlerini sil (her parti icin)
        for (partyId in partyIds) {
            db.delete(TABLE_REMATCH_DICE_SETS, "$COLUMN_DICE_SET_PARTY_ID = ?", arrayOf(partyId.toString()))
        }

        // Partileri sil
        db.delete(TABLE_REMATCH_DICE_PARTIES, "$COLUMN_PARTY_ENCOUNTER_ID = ?", arrayOf(encounterId.toString()))

        // Oyun sonuclarini sil
        db.delete(TABLE_REMATCH_GAME_RESULTS, "$COLUMN_GAME_RESULT_ENCOUNTER_ID = ?", arrayOf(encounterId.toString()))

        // Parti sonuclarini sil
        db.delete(TABLE_REMATCH_PARTY_RESULTS, "$COLUMN_PARTY_RESULT_ENCOUNTER_ID = ?", arrayOf(encounterId.toString()))

        // Istatistikleri sil
        db.delete(TABLE_REMATCH_ENCOUNTER_STATS, "$COLUMN_REMATCH_STATS_ENCOUNTER_ID = ?", arrayOf(encounterId.toString()))

        // Son olarak ana kaydı sil
        val result = db.delete(TABLE_REMATCH_ENCOUNTERS, "$COLUMN_ENCOUNTER_ID = ?", arrayOf(encounterId.toString()))
        db.close()
        return result
    }
}
