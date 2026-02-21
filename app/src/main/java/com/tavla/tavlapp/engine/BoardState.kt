package com.tavla.tavlapp.engine

/**
 * Tavla tahtasi durumu.
 * points[0] = 1. nokta (beyazin ic sahasi), points[23] = 24. nokta (siyahin ic sahasi)
 * Pozitif degerler beyaz taslari, negatif degerler siyah taslari temsil eder.
 *
 * Beyaz 24->1 yonunde, Siyah 1->24 yonunde hareket eder.
 */
data class BoardState(
    val points: IntArray = IntArray(24),
    val whiteBar: Int = 0,
    val blackBar: Int = 0,
    val whiteBorneOff: Int = 0,
    val blackBorneOff: Int = 0
) {
    companion object {
        /** Standart tavla baslangic pozisyonu */
        fun initial(): BoardState {
            val points = IntArray(24)
            // Beyaz taslar (pozitif) - 24->1 yonunde hareket eder, ic saha: 1-6
            points[23] = 2   // 24. nokta (rakibin ic sahasinda 2 kosucu)
            points[12] = 5   // 13. nokta
            points[7] = 3    // 8. nokta
            points[5] = 5    // 6. nokta (kendi ic sahasinda)
            // Siyah taslar (negatif) - 1->24 yonunde hareket eder, ic saha: 19-24
            points[0] = -2   // 1. nokta (rakibin ic sahasinda 2 kosucu)
            points[11] = -5  // 12. nokta
            points[16] = -3  // 17. nokta
            points[18] = -5  // 19. nokta (kendi ic sahasinda)
            return BoardState(points = points)
        }

        fun fromMap(map: Map<String, Any?>): BoardState {
            @Suppress("UNCHECKED_CAST")
            val pointsList = map["points"] as? List<Long> ?: return initial()
            val points = IntArray(24) { i -> pointsList.getOrElse(i) { 0L }.toInt() }
            return BoardState(
                points = points,
                whiteBar = (map["whiteBar"] as? Long)?.toInt() ?: 0,
                blackBar = (map["blackBar"] as? Long)?.toInt() ?: 0,
                whiteBorneOff = (map["whiteBorneOff"] as? Long)?.toInt() ?: 0,
                blackBorneOff = (map["blackBorneOff"] as? Long)?.toInt() ?: 0
            )
        }
    }

    fun toMap(): Map<String, Any> = mapOf(
        "points" to points.toList(),
        "whiteBar" to whiteBar,
        "blackBar" to blackBar,
        "whiteBorneOff" to whiteBorneOff,
        "blackBorneOff" to blackBorneOff
    )

    /** Tahta durumunu kopyalar (IntArray icin derin kopya) */
    fun deepCopy(): BoardState = BoardState(
        points.copyOf(), whiteBar, blackBar, whiteBorneOff, blackBorneOff
    )

    /** Oyuncunun barda tasi var mi */
    fun hasOnBar(player: PlayerColor): Boolean = when (player) {
        PlayerColor.WHITE -> whiteBar > 0
        PlayerColor.BLACK -> blackBar > 0
    }

    /** Oyuncunun belirli noktadaki tas sayisi (her zaman pozitif doner) */
    fun checkerCount(point: Int, player: PlayerColor): Int {
        val v = points[point]
        return when (player) {
            PlayerColor.WHITE -> if (v > 0) v else 0
            PlayerColor.BLACK -> if (v < 0) -v else 0
        }
    }

    /** Oyuncunun tum taslari ic sahada mi (bear off kontrolu) */
    fun allInHomeBoard(player: PlayerColor): Boolean {
        val barCount = if (player == PlayerColor.WHITE) whiteBar else blackBar
        if (barCount > 0) return false

        return when (player) {
            PlayerColor.WHITE -> {
                // Beyaz ic saha: points[0..5] (noktalar 1-6)
                // Dis sahada tas olmamali
                (6..23).all { points[it] <= 0 }
            }
            PlayerColor.BLACK -> {
                // Siyah ic saha: points[18..23] (noktalar 19-24)
                // Dis sahada tas olmamali
                (0..17).all { points[it] >= 0 }
            }
        }
    }

    /** Cikarilmis tas sayisi */
    fun borneOff(player: PlayerColor): Int = when (player) {
        PlayerColor.WHITE -> whiteBorneOff
        PlayerColor.BLACK -> blackBorneOff
    }

    /** Bardaki tas sayisi */
    fun barCount(player: PlayerColor): Int = when (player) {
        PlayerColor.WHITE -> whiteBar
        PlayerColor.BLACK -> blackBar
    }

    /** Tahtadaki toplam tas sayisi (bar + board) */
    fun totalCheckersOnBoard(player: PlayerColor): Int {
        val bar = barCount(player)
        val onBoard = when (player) {
            PlayerColor.WHITE -> points.sumOf { if (it > 0) it else 0 }
            PlayerColor.BLACK -> points.sumOf { if (it < 0) -it else 0 }
        }
        return bar + onBoard
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is BoardState) return false
        return points.contentEquals(other.points) &&
                whiteBar == other.whiteBar &&
                blackBar == other.blackBar &&
                whiteBorneOff == other.whiteBorneOff &&
                blackBorneOff == other.blackBorneOff
    }

    override fun hashCode(): Int {
        var result = points.contentHashCode()
        result = 31 * result + whiteBar
        result = 31 * result + blackBar
        result = 31 * result + whiteBorneOff
        result = 31 * result + blackBorneOff
        return result
    }
}

enum class PlayerColor {
    WHITE, BLACK;

    fun opponent(): PlayerColor = when (this) {
        WHITE -> BLACK
        BLACK -> WHITE
    }

    /** Hareket yonu: WHITE azalan (24->1), BLACK artan (1->24) */
    fun direction(): Int = when (this) {
        WHITE -> -1
        BLACK -> 1
    }

    /** Ic saha baslangic indeksi */
    fun homeBoardStart(): Int = when (this) {
        WHITE -> 0   // noktalar 1-6 (indeks 0-5)
        BLACK -> 18  // noktalar 19-24 (indeks 18-23)
    }

    /** Ic saha bitis indeksi (dahil) */
    fun homeBoardEnd(): Int = when (this) {
        WHITE -> 5
        BLACK -> 23
    }
}

/** Tek bir hamle */
data class Move(
    val from: Int,       // -1 = bardan, 0-23 = nokta indeksi
    val to: Int,         // -1 = cikarma (bear off), 0-23 = nokta indeksi
    val dieUsed: Int,    // Kullanilan zar degeri (1-6)
    val isHit: Boolean = false  // Rakip tasi vuruldu mu
) {
    fun toMap(): Map<String, Any> = mapOf(
        "from" to from,
        "to" to to,
        "dieUsed" to dieUsed,
        "isHit" to isHit
    )

    companion object {
        fun fromMap(map: Map<String, Any?>): Move = Move(
            from = (map["from"] as? Long)?.toInt() ?: 0,
            to = (map["to"] as? Long)?.toInt() ?: 0,
            dieUsed = (map["dieUsed"] as? Long)?.toInt() ?: 0,
            isHit = map["isHit"] as? Boolean ?: false
        )
    }
}

/** Oyun bitis turu */
enum class WinType(val multiplier: Int, val code: String) {
    SINGLE(1, "T"),
    MARS(2, "M"),
    BACKGAMMON(3, "B");
}

/** Katlama zarinin durumu */
data class DoublingCubeState(
    val value: Int = 1,
    val owner: String = "center",  // "center", "white", "black"
    val pendingOffer: String? = null  // Bekleyen teklif: "white" veya "black" (teklif eden)
) {
    fun toMap(): Map<String, Any?> = mapOf(
        "value" to value,
        "owner" to owner,
        "pendingOffer" to pendingOffer
    )

    companion object {
        fun fromMap(map: Map<String, Any?>): DoublingCubeState = DoublingCubeState(
            value = (map["value"] as? Long)?.toInt() ?: 1,
            owner = map["owner"] as? String ?: "center",
            pendingOffer = map["pendingOffer"] as? String
        )
    }
}
