package com.tavla.tavlapp

/**
 * Rovansli Karsilasma (Rematch Encounter) veri modelleri
 *
 * Yapi:
 * - 1 Karsilasma = 100 Zar Partisi
 * - 1 Zar Partisi = 21 Zar Seti (maksimum 21 el icin)
 * - 1 Zar Seti = 200 cift zar + baslangic zarlari
 */

/**
 * Karsilasma durumu
 */
enum class RematchStatus {
    ACTIVE,             // Aktif karsilasma (ilk tur)
    ROUND1_COMPLETE,    // Ilk tur tamamlandi, rovans bekliyor
    ROUND2_ACTIVE,      // Rovans turu aktif
    COMPLETED,          // Tamamlandi
    CANCELLED           // Iptal edildi
}

/**
 * Ana karsilasma kaydi
 */
data class RematchEncounter(
    val id: Long,
    val player1Id: Long,
    val player2Id: Long,
    val player1Name: String = "",
    val player2Name: String = "",
    val totalParties: Int,              // Toplam parti sayisi (ornegin 100)
    val currentRound: Int,              // 1 = ilk tur, 2 = rovans turu
    val currentPartyIndex: Int,         // Mevcut parti indeksi (0'dan baslar)
    val currentGameIndex: Int,          // Mevcut el/oyun indeksi (0-20)
    val status: RematchStatus,
    val createdDate: String,
    val completedDate: String? = null
)

/**
 * Zar Partisi - Her parti icin 21 zar seti
 */
data class RematchDiceParty(
    val id: Long,
    val encounterId: Long,
    val partyIndex: Int                 // 0'dan baslar (0-99)
)

/**
 * Zar Seti - Her el/oyun icin 200 cift zar
 */
data class RematchDiceSet(
    val id: Long,
    val partyId: Long,
    val setIndex: Int,                          // 0-20 (bir partide max 21 set)
    val startingDicePlayer1: Int,               // Baslangic zari (1-6)
    val startingDicePlayer2: Int,               // Baslangic zari (1-6, farkli olmali)
    val player1Dice: List<Pair<Int, Int>>,      // 200 adet cift zar
    val player2Dice: List<Pair<Int, Int>>       // 200 adet cift zar
) {
    /**
     * Baslangic zarlarina gore hangi oyuncunun basladigini belirler
     * @return 1 = Oyuncu 1 baslar, 2 = Oyuncu 2 baslar
     */
    fun getFirstPlayer(): Int {
        return if (startingDicePlayer1 > startingDicePlayer2) 1 else 2
    }
}

/**
 * El/Oyun sonucu (bir el = bir zar seti kullanimi)
 */
data class RematchGameResult(
    val id: Long,
    val encounterId: Long,
    val partyIndex: Int,
    val setIndex: Int,
    val roundNumber: Int,               // 1 veya 2 (ilk tur veya rovans)
    val leftPlayerId: Long,             // Zar seti 1 kullanan oyuncu
    val rightPlayerId: Long,            // Zar seti 2 kullanan oyuncu
    val winnerId: Long? = null,
    val winType: String? = null,        // SINGLE, MARS, BACKGAMMON, RESIGN
    val cubeValue: Int = 1,
    val finalScore: Int? = null,
    val loserPipCount: Int? = null,
    val dicePairsUsed: Int? = null,     // Kac cift zar kullanildi
    val gameDate: String? = null
)

/**
 * Parti sonucu (11'lik parti)
 */
data class RematchPartyResult(
    val id: Long,
    val encounterId: Long,
    val partyIndex: Int,
    val roundNumber: Int,               // 1 veya 2
    val player1Score: Int,              // 0-11
    val player2Score: Int,              // 0-11
    val winnerId: Long? = null,
    val totalGamesPlayed: Int,          // Kac el oynandi
    val partyDate: String? = null
)

/**
 * Karsilasma istatistikleri
 */
data class RematchEncounterStats(
    val id: Long,
    val encounterId: Long,
    val playerId: Long,
    val playerName: String = "",
    val round1PartiesWon: Int = 0,
    val round1GamesWon: Int = 0,
    val round1Points: Int = 0,
    val round2PartiesWon: Int = 0,
    val round2GamesWon: Int = 0,
    val round2Points: Int = 0,
    val totalPartiesWon: Int = 0,
    val totalGamesWon: Int = 0,
    val totalPoints: Int = 0
)

/**
 * Zar gosterim durumu
 */
data class DiceDisplayState(
    val partyIndex: Int,
    val setIndex: Int,
    val moveIndex: Int,                         // -1 = baslangic zari, 0-199 = hamle
    val isStartingDice: Boolean,
    val startingDicePlayer1: Int? = null,
    val startingDicePlayer2: Int? = null,
    val currentPlayer: Int,                     // 1 veya 2 - siradaki oyuncu
    val player1Dice: Pair<Int, Int>? = null,
    val player2Dice: Pair<Int, Int>? = null,
    val player1DicePlayed: Boolean = false,     // Oyuncu 1 zarini oynadi mi
    val player2DicePlayed: Boolean = false      // Oyuncu 2 zarini oynadi mi
)

/**
 * Rovans bilgilendirme verisi
 */
data class PreviousGameInfo(
    val wasPlayed: Boolean,
    val winnerId: Long? = null,
    val winnerName: String? = null,
    val winType: String? = null,
    val loserPipCount: Int? = null,
    val dicePairsUsed: Int? = null
)

/**
 * Kazanma tipi
 */
object WinTypes {
    const val SINGLE = "SINGLE"             // Tek sayi (1 puan)
    const val MARS = "MARS"                 // Mars (2 puan)
    const val BACKGAMMON = "BACKGAMMON"     // Backgammon (3 puan)
    const val RESIGN = "RESIGN"             // Pes (katlama reddi)
}

/**
 * Zar unicode karakterleri (noktalı görünüm)
 */
object DiceUnicode {
    fun getDiceChar(value: Int): String {
        return when (value) {
            1 -> "⚀"
            2 -> "⚁"
            3 -> "⚂"
            4 -> "⚃"
            5 -> "⚄"
            6 -> "⚅"
            else -> "?"
        }
    }

    fun getDicePairString(dice: Pair<Int, Int>): String {
        return "${getDiceChar(dice.first)} ${getDiceChar(dice.second)}"
    }
}

/**
 * Tek bir oyun icin karsilastirma verisi
 * Ayni parti/set indeksindeki Tur 1 ve Tur 2 sonuclari
 */
data class GameComparisonRow(
    val setIndex: Int,
    val round1Result: RematchGameResult?,
    val round2Result: RematchGameResult?
)

/**
 * Bir parti icin karsilastirma ozeti
 */
data class PartyComparisonData(
    val partyIndex: Int,
    val round1PartyResult: RematchPartyResult?,
    val round2PartyResult: RematchPartyResult?,
    val gameComparisons: List<GameComparisonRow>,
    val round1TotalDicePairs: Int,
    val round2TotalDicePairs: Int,
    val round1MarsCount: Int,
    val round2MarsCount: Int,
    val round1BackgammonCount: Int,
    val round2BackgammonCount: Int,
    val round1MaxCube: Int,
    val round2MaxCube: Int,
    val sameWinnerCount: Int,
    val differentWinnerCount: Int
)

/**
 * Tum karsilasma ozeti
 */
data class EncounterComparisonSummary(
    val encounterId: Long,
    val player1Name: String,
    val player2Name: String,
    val totalPartiesCompared: Int,
    val player1Round1PartiesWon: Int,
    val player1Round2PartiesWon: Int,
    val player2Round1PartiesWon: Int,
    val player2Round2PartiesWon: Int
)
