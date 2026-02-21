package com.tavla.tavlapp.engine

/**
 * Tavla cekirdek oyun motoru.
 * Hamle uygulama, oyun durumu kontrolu ve kazanma turu tespiti.
 */
object BackgammonEngine {

    /**
     * Hamleyi tahtaya uygular, yeni BoardState doner.
     * @param board Mevcut tahta durumu
     * @param move Uygulanacak hamle
     * @param player Hamleyi yapan oyuncu
     * @return Yeni tahta durumu
     */
    fun applyMove(board: BoardState, move: Move, player: PlayerColor): BoardState {
        val newPoints = board.points.copyOf()
        var newWhiteBar = board.whiteBar
        var newBlackBar = board.blackBar
        var newWhiteBorneOff = board.whiteBorneOff
        var newBlackBorneOff = board.blackBorneOff

        // Kaynak noktadan tasi kaldir
        if (move.from == -1) {
            // Bardan cikis
            when (player) {
                PlayerColor.WHITE -> newWhiteBar--
                PlayerColor.BLACK -> newBlackBar--
            }
        } else {
            when (player) {
                PlayerColor.WHITE -> newPoints[move.from]--
                PlayerColor.BLACK -> newPoints[move.from]++
            }
        }

        // Hedef noktaya tasi koy
        if (move.to == -1) {
            // Bear off (tas cikarma)
            when (player) {
                PlayerColor.WHITE -> newWhiteBorneOff++
                PlayerColor.BLACK -> newBlackBorneOff++
            }
        } else {
            // Vurulan tas varsa bara gonder
            if (move.isHit) {
                when (player) {
                    PlayerColor.WHITE -> {
                        // Siyah tasi bara gonder
                        newPoints[move.to] = 0  // Siyahin tekli tasini kaldir
                        newBlackBar++
                    }
                    PlayerColor.BLACK -> {
                        // Beyaz tasi bara gonder
                        newPoints[move.to] = 0  // Beyazin tekli tasini kaldir
                        newWhiteBar++
                    }
                }
            }
            // Tasi hedef noktaya koy
            when (player) {
                PlayerColor.WHITE -> newPoints[move.to]++
                PlayerColor.BLACK -> newPoints[move.to]--
            }
        }

        return BoardState(
            points = newPoints,
            whiteBar = newWhiteBar,
            blackBar = newBlackBar,
            whiteBorneOff = newWhiteBorneOff,
            blackBorneOff = newBlackBorneOff
        )
    }

    /**
     * Hamle listesini sirayla uygular.
     */
    fun applyMoves(board: BoardState, moves: List<Move>, player: PlayerColor): BoardState {
        var current = board
        for (move in moves) {
            current = applyMove(current, move, player)
        }
        return current
    }

    /**
     * Oyun bitti mi kontrolu. 15 tas cikarilmissa bitmistir.
     */
    fun isGameOver(board: BoardState): Boolean {
        return board.whiteBorneOff == 15 || board.blackBorneOff == 15
    }

    /**
     * Kazanan oyuncuyu doner. Oyun bitmediyse null doner.
     */
    fun getWinner(board: BoardState): PlayerColor? {
        return when {
            board.whiteBorneOff == 15 -> PlayerColor.WHITE
            board.blackBorneOff == 15 -> PlayerColor.BLACK
            else -> null
        }
    }

    /**
     * Kazanma turunu belirler.
     * @param board Oyun bitis tahtasi
     * @param winner Kazanan oyuncu
     * @return WinType (SINGLE, MARS, BACKGAMMON)
     */
    fun getWinType(board: BoardState, winner: PlayerColor): WinType {
        val loser = winner.opponent()
        val loserBorneOff = board.borneOff(loser)

        // Kaybeden hic tas cikarmamissa
        if (loserBorneOff == 0) {
            // Kaybeden hala kazananin ic sahasinda veya barda tas varsa -> BACKGAMMON
            if (hasCheckersInOpponentHomeOrBar(board, loser, winner)) {
                return WinType.BACKGAMMON
            }
            // Kaybeden hic tas cikarmamis ama kazananin ic sahasinda tasi yok -> MARS
            return WinType.MARS
        }

        return WinType.SINGLE
    }

    /**
     * Kaybedenin, kazananin ic sahasinda veya barda tasi var mi kontrolu.
     * Backgammon tespiti icin kullanilir.
     */
    private fun hasCheckersInOpponentHomeOrBar(
        board: BoardState,
        loser: PlayerColor,
        winner: PlayerColor
    ): Boolean {
        // Kaybeden barda mi?
        if (board.hasOnBar(loser)) return true

        // Kazananin ic sahasi araligi
        val homeStart = winner.homeBoardStart()
        val homeEnd = winner.homeBoardEnd()

        // Kaybedenin kazananin ic sahasindaki taslari
        for (i in homeStart..homeEnd) {
            if (board.checkerCount(i, loser) > 0) return true
        }

        return false
    }

    /**
     * Oyuncu tum taslarini cikarabilir mi (tum taslar ic sahada).
     */
    fun canBearOff(board: BoardState, player: PlayerColor): Boolean {
        return board.allInHomeBoard(player)
    }

    /**
     * Oyuncunun bardan tasi cikarma zorunlulugu var mi.
     */
    fun mustEnterFromBar(board: BoardState, player: PlayerColor): Boolean {
        return board.hasOnBar(player)
    }

    /**
     * Belirli bir noktanin belirli bir oyuncu tarafindan bloke edilip edilmedigini kontrol eder.
     * Bir noktada 2+ rakip tasi varsa bloke edilmistir.
     */
    fun isPointBlocked(board: BoardState, point: Int, player: PlayerColor): Boolean {
        val opponent = player.opponent()
        return board.checkerCount(point, opponent) >= 2
    }

    /**
     * Bir hamlenin bir vurus olup olmayacagini kontrol eder.
     * Hedef noktada tam olarak 1 rakip tasi varsa vurus olur.
     */
    fun isHitMove(board: BoardState, to: Int, player: PlayerColor): Boolean {
        if (to < 0 || to > 23) return false
        val opponent = player.opponent()
        return board.checkerCount(to, opponent) == 1
    }

    /**
     * Puani hesaplar: winType.multiplier * cubeValue
     */
    fun calculateScore(winType: WinType, cubeValue: Int): Int {
        return winType.multiplier * cubeValue
    }

    /**
     * Bardan giris noktasini hesaplar.
     * Beyaz: zar degeri - 1 (cunku 24. noktadan girer -> indeks 23'ten asagiya)
     * Siyah: 24 - zar degeri (cunku 1. noktadan girer -> indeks 0'dan yukariya)
     *
     * Beyaz 24->1 hareket eder, bardan girince 24'den baslayarak zar degeri kadar ilerler.
     * Siyah 1->24 hareket eder, bardan girince 1'den baslayarak zar degeri kadar ilerler.
     */
    fun barEntryPoint(player: PlayerColor, dieValue: Int): Int {
        return when (player) {
            PlayerColor.WHITE -> 24 - dieValue  // Beyaz: zar=1 -> 23, zar=6 -> 18
            PlayerColor.BLACK -> dieValue - 1   // Siyah: zar=1 -> 0, zar=6 -> 5
        }
    }

    /**
     * Tek bir zarla hedef noktayi hesaplar.
     * @param from Kaynak nokta indeksi (0-23)
     * @param dieValue Zar degeri (1-6)
     * @param player Hareket eden oyuncu
     * @return Hedef nokta indeksi (0-23) veya -1 (bear off)
     */
    fun destinationPoint(from: Int, dieValue: Int, player: PlayerColor): Int {
        val dest = from + dieValue * player.direction()
        return when (player) {
            PlayerColor.WHITE -> {
                if (dest < 0) -1  // Bear off
                else dest
            }
            PlayerColor.BLACK -> {
                if (dest > 23) -1  // Bear off
                else dest
            }
        }
    }

    /**
     * Bear off sirasinda tam olmayan zar kullanimi kontrolu.
     * Eger tam zar degeri kullanilamazsa, en yuksek noktadan tas cikarilabilir.
     * Bu kural sadece zar degerinden daha yuksek noktada tas yoksa gecerlidir.
     */
    fun canBearOffWithExactOrHigher(board: BoardState, player: PlayerColor, from: Int, dieValue: Int): Boolean {
        if (!canBearOff(board, player)) return false

        val dest = destinationPoint(from, dieValue, player)
        // Tam cikarma
        if (dest == -1) return true

        // Tahtadan disarida mi (overshoot)?
        val isOvershoot = when (player) {
            PlayerColor.WHITE -> (from - dieValue) < 0
            PlayerColor.BLACK -> (from + dieValue) > 23
        }

        if (isOvershoot) {
            // Daha yuksek noktada tas var mi kontrolu
            val homeStart = player.homeBoardStart()
            val homeEnd = player.homeBoardEnd()

            when (player) {
                PlayerColor.WHITE -> {
                    // from noktasindan yukari (daha buyuk indeks) bakiyoruz
                    for (i in (from + 1)..homeEnd) {
                        if (board.checkerCount(i, player) > 0) return false
                    }
                }
                PlayerColor.BLACK -> {
                    // from noktasindan asagi (daha kucuk indeks) bakiyoruz
                    for (i in homeStart until from) {
                        if (board.checkerCount(i, player) > 0) return false
                    }
                }
            }
            return true
        }

        return false
    }
}
