package com.tavla.tavlapp

// Zar istatistikleri data class
data class DiceStatistics(
    val id: Long = 0,
    val matchId: Long,
    val playerId: Long,

    // Atılan zar çeşitleri (21 kombinasyon)
    val dice_1_1: Int = 0,
    val dice_1_2: Int = 0,
    val dice_1_3: Int = 0,
    val dice_1_4: Int = 0,
    val dice_1_5: Int = 0,
    val dice_1_6: Int = 0,
    val dice_2_2: Int = 0,
    val dice_2_3: Int = 0,
    val dice_2_4: Int = 0,
    val dice_2_5: Int = 0,
    val dice_2_6: Int = 0,
    val dice_3_3: Int = 0,
    val dice_3_4: Int = 0,
    val dice_3_5: Int = 0,
    val dice_3_6: Int = 0,
    val dice_4_4: Int = 0,
    val dice_4_5: Int = 0,
    val dice_4_6: Int = 0,
    val dice_5_5: Int = 0,
    val dice_5_6: Int = 0,
    val dice_6_6: Int = 0,

    // Genel istatistikler
    val totalDicePower: Int = 0,
    val totalDicePieces: Int = 0,
    val doubleCount: Int = 0,
    val doublePower: Int = 0,

    // Oynanan
    val playedPower: Int = 0,
    val playedPieces: Int = 0,

    // Gele
    val wastedPower: Int = 0,
    val wastedPieces: Int = 0,

    // Kısmen boşa
    val partialWastedPower: Int = 0,
    val partialWastedPieces: Int = 0,

    // Bitiş artığı
    val endWastePower: Int = 0,
    val endWastePieces: Int = 0
)

// Tek zar atışı sonucu
data class DiceRollResult(
    val dice1: Int,  // 1-6
    val dice2: Int,  // 1-6

    // Checkbox durumları (4 zar için)
    val dice1State: DiceState,
    val dice2State: DiceState,
    val dice3State: DiceState = DiceState.NONE,  // Çift zar için
    val dice4State: DiceState = DiceState.NONE   // Çift zar için
)

// Checkbox durumları
enum class DiceState {
    NONE,            // Kullanılmıyor (normal zar için 3. ve 4. zarlar)
    PLAYED,          // ☑ Tam oynandı
    WASTED,          // ☐ Gele
    PARTIAL_WASTED,  // ↻ Kısmen boşa (döndürülmüş)
    END_WASTE        // ■ Bitiş artığı (kare)
}

// Zar kombinasyon helper fonksiyonları
object DiceHelper {

    /**
     * İki zarı normalize eder (küçük-büyük sırasına)
     * Örnek: (5,6) ve (6,5) → "5-6" olarak döner
     */
    fun normalizeDice(dice1: Int, dice2: Int): String {
        val min = minOf(dice1, dice2)
        val max = maxOf(dice1, dice2)
        return "$min-$max"
    }

    /**
     * Zar kombinasyonunun column adını döner
     * Örnek: (5,6) → "dice_5_6"
     */
    fun getDiceColumnName(dice1: Int, dice2: Int): String {
        val normalized = normalizeDice(dice1, dice2)
        return "dice_${normalized.replace("-", "_")}"
    }

    /**
     * Çift zar mı kontrol eder
     */
    fun isDouble(dice1: Int, dice2: Int): Boolean {
        return dice1 == dice2
    }

    /**
     * Zar kuvvetini hesaplar
     * Normal zar: dice1 + dice2
     * Çift zar: (dice1 + dice2) * 2
     */
    fun calculateDicePower(dice1: Int, dice2: Int): Int {
        return if (isDouble(dice1, dice2)) {
            (dice1 + dice2) * 2
        } else {
            dice1 + dice2
        }
    }

    /**
     * Zar paresi hesaplar
     * Normal zar: 2
     * Çift zar: 4
     */
    fun calculateDicePieces(dice1: Int, dice2: Int): Int {
        return if (isDouble(dice1, dice2)) 4 else 2
    }

    /**
     * Tüm 21 zar kombinasyonunun listesi
     */
    fun getAllDiceCombinations(): List<String> {
        return listOf(
            "1-1", "1-2", "1-3", "1-4", "1-5", "1-6",
            "2-2", "2-3", "2-4", "2-5", "2-6",
            "3-3", "3-4", "3-5", "3-6",
            "4-4", "4-5", "4-6",
            "5-5", "5-6",
            "6-6"
        )
    }

    /**
     * Kısmi oynama durumunda boşa giden değeri hesaplar
     * Örnek: 6 zarı 1 evinden toplandı → 5 boşa gitti
     */
    fun calculatePartialWaste(diceValue: Int, usedValue: Int): Int {
        return diceValue - usedValue
    }

    /**
     * DiceRollResult'tan istatistik değerlerini hesaplar
     */
    fun calculateStatisticsFromRoll(rollResult: DiceRollResult): Map<String, Int> {
        val dice1 = rollResult.dice1
        val dice2 = rollResult.dice2
        val isDouble = isDouble(dice1, dice2)

        val totalPower = calculateDicePower(dice1, dice2)
        val totalPieces = calculateDicePieces(dice1, dice2)

        var playedPower = 0
        var playedPieces = 0
        var wastedPower = 0
        var wastedPieces = 0
        var partialWastedPower = 0
        var partialWastedPieces = 0
        var endWastePower = 0
        var endWastePieces = 0

        // Her zar için duruma göre hesapla
        val states = if (isDouble) {
            listOf(
                rollResult.dice1State to dice1,
                rollResult.dice2State to dice2,
                rollResult.dice3State to dice1,
                rollResult.dice4State to dice2
            )
        } else {
            listOf(
                rollResult.dice1State to dice1,
                rollResult.dice2State to dice2
            )
        }

        states.forEach { (state, value) ->
            when (state) {
                DiceState.PLAYED -> {
                    playedPower += value
                    playedPieces++
                }
                DiceState.WASTED -> {
                    wastedPower += value
                    wastedPieces++
                }
                DiceState.PARTIAL_WASTED -> {
                    // Kısmi boşa: örneğin 6→1 oynanırsa 5 boşa gider
                    // UI'dan gelen değer kullanılır
                    partialWastedPower += value
                    partialWastedPieces++
                }
                DiceState.END_WASTE -> {
                    endWastePower += value
                    endWastePieces++
                }
                DiceState.NONE -> {
                    // Kullanılmıyor
                }
            }
        }

        return mapOf(
            "totalPower" to totalPower,
            "totalPieces" to totalPieces,
            "doubleCount" to if (isDouble) 1 else 0,
            "doublePower" to if (isDouble) totalPower else 0,
            "playedPower" to playedPower,
            "playedPieces" to playedPieces,
            "wastedPower" to wastedPower,
            "wastedPieces" to wastedPieces,
            "partialWastedPower" to partialWastedPower,
            "partialWastedPieces" to partialWastedPieces,
            "endWastePower" to endWastePower,
            "endWastePieces" to endWastePieces
        )
    }
}
