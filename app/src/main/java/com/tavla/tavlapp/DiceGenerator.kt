package com.tavla.tavlapp

import kotlin.random.Random

/**
 * Rovansli Karsilasma icin zar uretim sinifi
 *
 * Yapi:
 * - 1 Karsilasma = N Zar Partisi (varsayilan 100)
 * - 1 Zar Partisi = 21 Zar Seti (maksimum 21 el icin)
 * - 1 Zar Seti = 200 cift zar + baslangic zarlari
 */
object DiceGenerator {

    const val DICE_PAIRS_PER_SET = 200      // Her zar setinde 200 cift zar
    const val SETS_PER_PARTY = 21           // Her partide maksimum 21 zar seti

    /**
     * Baslangic zarlarini uret (iki oyuncu icin farkli degerler)
     * @return Pair(oyuncu1Zari, oyuncu2Zari) - birbirinden farkli 1-6 arasi
     */
    fun generateStartingDice(): Pair<Int, Int> {
        val player1Die = Random.nextInt(1, 7)
        var player2Die: Int
        do {
            player2Die = Random.nextInt(1, 7)
        } while (player2Die == player1Die)
        return Pair(player1Die, player2Die)
    }

    /**
     * Tek bir zar cifti at
     * @return Pair(zar1, zar2) - her biri 1-6 arasi
     */
    fun rollDice(): Pair<Int, Int> {
        return Pair(Random.nextInt(1, 7), Random.nextInt(1, 7))
    }

    /**
     * Bir oyuncu icin tam zar dizisi uret
     * @param count Zar cifti sayisi (varsayilan 200)
     * @return Zar ciftlerinin listesi
     */
    fun generateDiceSequence(count: Int = DICE_PAIRS_PER_SET): List<Pair<Int, Int>> {
        return (1..count).map { rollDice() }
    }

    /**
     * Zar dizisini JSON formatina cevir (veritabaninda saklamak icin)
     * @param sequence Zar ciftlerinin listesi
     * @return JSON string: [[5,4],[6,1],[3,3],...]
     */
    fun diceSequenceToJson(sequence: List<Pair<Int, Int>>): String {
        val sb = StringBuilder("[")
        sequence.forEachIndexed { index, (d1, d2) ->
            if (index > 0) sb.append(",")
            sb.append("[$d1,$d2]")
        }
        sb.append("]")
        return sb.toString()
    }

    /**
     * JSON string'i zar dizisine cevir
     * @param json JSON formatinda zar dizisi
     * @return Zar ciftlerinin listesi
     */
    fun jsonToDiceSequence(json: String): List<Pair<Int, Int>> {
        val result = mutableListOf<Pair<Int, Int>>()
        val regex = "\\[(\\d),(\\d)\\]".toRegex()
        regex.findAll(json).forEach { match ->
            val d1 = match.groupValues[1].toInt()
            val d2 = match.groupValues[2].toInt()
            result.add(Pair(d1, d2))
        }
        return result
    }

    /**
     * Tek bir zar seti uret (bir el icin)
     * @return GeneratedDiceSet - baslangic zarlari ve 200'er adet cift zar
     */
    fun generateDiceSet(): GeneratedDiceSet {
        val starting = generateStartingDice()
        return GeneratedDiceSet(
            startingDicePlayer1 = starting.first,
            startingDicePlayer2 = starting.second,
            player1Dice = generateDiceSequence(),
            player2Dice = generateDiceSequence()
        )
    }

    /**
     * Eski isim uyumlulugu icin alias
     */
    fun generateMatchDiceSet(): GeneratedDiceSet = generateDiceSet()

    /**
     * Bir parti icin tum zar setlerini uret (21 set)
     * @return 21 adet zar seti listesi
     */
    fun generateDiceParty(): List<GeneratedDiceSet> {
        return (0 until SETS_PER_PARTY).map { generateDiceSet() }
    }

    /**
     * Tum karsilasma icin zar partilerini uret
     * @param partyCount Parti sayisi (varsayilan 100)
     * @return Parti listesi, her parti 21 zar seti icerir
     */
    fun generateAllParties(partyCount: Int): List<List<GeneratedDiceSet>> {
        return (0 until partyCount).map { generateDiceParty() }
    }

    /**
     * Buyuk zar atan oyuncuyu belirle
     * @return 1 = Oyuncu 1 baslar, 2 = Oyuncu 2 baslar
     */
    fun determineFirstPlayer(startingDice1: Int, startingDice2: Int): Int {
        return if (startingDice1 > startingDice2) 1 else 2
    }
}

/**
 * Uretilmis zar seti veri sinifi
 */
data class GeneratedDiceSet(
    val startingDicePlayer1: Int,           // Baslangic zari (1-6)
    val startingDicePlayer2: Int,           // Baslangic zari (1-6, farkli)
    val player1Dice: List<Pair<Int, Int>>,  // 200 adet cift zar
    val player2Dice: List<Pair<Int, Int>>   // 200 adet cift zar
) {
    /**
     * Ilk oynayan oyuncuyu belirle (buyuk zar atan)
     */
    fun getFirstPlayer(): Int {
        return DiceGenerator.determineFirstPlayer(startingDicePlayer1, startingDicePlayer2)
    }
}
