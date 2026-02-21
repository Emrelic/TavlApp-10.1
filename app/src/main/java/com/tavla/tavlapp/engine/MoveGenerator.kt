package com.tavla.tavlapp.engine

/**
 * Gecerli hamle uretici.
 * Verilen tahta durumu, oyuncu ve zar degerleri icin tum gecerli hamleleri uretir.
 */
object MoveGenerator {

    /**
     * Hareket edebilecek kaynak noktalari doner.
     * @param board Tahta durumu
     * @param player Oyuncu
     * @param remainingDice Kalan zar degerleri
     * @return Hareket edebilecek kaynak nokta indeksleri (-1 = bar)
     */
    fun getMovablePoints(
        board: BoardState,
        player: PlayerColor,
        remainingDice: List<Int>
    ): List<Int> {
        if (remainingDice.isEmpty()) return emptyList()

        val sources = mutableSetOf<Int>()

        // Barda tas varsa sadece bardan oynayabilir
        if (board.hasOnBar(player)) {
            val uniqueDice = remainingDice.distinct()
            for (die in uniqueDice) {
                val entry = BackgammonEngine.barEntryPoint(player, die)
                if (entry in 0..23 && !BackgammonEngine.isPointBlocked(board, entry, player)) {
                    sources.add(-1)
                    break
                }
            }
            return sources.toList()
        }

        val uniqueDice = remainingDice.distinct()

        for (i in 0..23) {
            if (board.checkerCount(i, player) > 0) {
                for (die in uniqueDice) {
                    if (getDestinationsForDie(board, player, i, die).isNotEmpty()) {
                        sources.add(i)
                        break
                    }
                }
            }
        }

        return sources.toList().sorted()
    }

    /**
     * Belirli kaynak noktadan gidilebilecek hedef noktalari doner.
     * @param board Tahta durumu
     * @param player Oyuncu
     * @param from Kaynak (-1 = bar, 0-23 = nokta)
     * @param remainingDice Kalan zar degerleri
     * @return Gecerli hedef nokta indeksleri listesi (dieUsed ile birlikte)
     */
    fun getDestinations(
        board: BoardState,
        player: PlayerColor,
        from: Int,
        remainingDice: List<Int>
    ): List<Pair<Int, Int>> { // Pair<hedef, dieUsed>
        val destinations = mutableSetOf<Pair<Int, Int>>()

        for (die in remainingDice.distinct()) {
            for (dest in getDestinationsForDie(board, player, from, die)) {
                destinations.add(Pair(dest, die))
            }
        }

        return destinations.toList()
    }

    /**
     * Tek bir zar degeriyle gidilebilecek hedefleri doner.
     */
    private fun getDestinationsForDie(
        board: BoardState,
        player: PlayerColor,
        from: Int,
        die: Int
    ): List<Int> {
        val results = mutableListOf<Int>()

        if (from == -1) {
            // Bardan giris
            val entry = BackgammonEngine.barEntryPoint(player, die)
            if (entry in 0..23 && !BackgammonEngine.isPointBlocked(board, entry, player)) {
                results.add(entry)
            }
        } else {
            val dest = BackgammonEngine.destinationPoint(from, die, player)

            if (dest == -1) {
                // Bear off - tam cikarma
                if (BackgammonEngine.canBearOff(board, player)) {
                    results.add(-1)
                }
            } else if (dest in 0..23) {
                if (!BackgammonEngine.isPointBlocked(board, dest, player)) {
                    results.add(dest)
                }
            } else {
                // Overshoot - sadece en yuksek tastan cikarilabilir
                if (BackgammonEngine.canBearOffWithExactOrHigher(board, player, from, die)) {
                    results.add(-1)
                }
            }
        }

        return results
    }

    /**
     * Tum gecerli tur kombinasyonlarini uretir (backtracking ile).
     * Maksimum zar kullanim kuralini uygular:
     * - Mumkunse her iki zar da kullanilmali
     * - Sadece biri kullanilabiliyorsa buyuk olan tercih edilir
     * - Ciftlerde 4 hamle yapilmali (mumkun oldugunca)
     *
     * @param board Tahta durumu
     * @param player Oyuncu
     * @param dice Zar degerleri (2 veya 4 eleman - ciftlerde 4)
     * @return Gecerli tur listesi (her tur bir Move listesi)
     */
    fun generateAllTurns(
        board: BoardState,
        player: PlayerColor,
        dice: List<Int>
    ): List<List<Move>> {
        val allTurns = mutableListOf<List<Move>>()
        generateTurnsRecursive(board, player, dice.toMutableList(), mutableListOf(), allTurns)

        if (allTurns.isEmpty()) return listOf(emptyList()) // Hamle yapamiyorsa bos tur

        // Maksimum zar kullanim kuralini uygula
        val maxDiceUsed = allTurns.maxOf { it.size }
        val maxTurns = allTurns.filter { it.size == maxDiceUsed }

        // Eger sadece 1 zar kullanilabiliyorsa ve zarlar farkli ise, buyuk zari kullanmali
        if (maxDiceUsed == 1 && dice.distinct().size == 2) {
            val maxDieValue = dice.max()
            val turnsWithMaxDie = maxTurns.filter { it[0].dieUsed == maxDieValue }
            if (turnsWithMaxDie.isNotEmpty()) {
                return turnsWithMaxDie.distinctBy { turnToKey(it) }
            }
        }

        return maxTurns.distinctBy { turnToKey(it) }
    }

    /**
     * Tur kombinasyonlarini recursive uretir.
     */
    private fun generateTurnsRecursive(
        board: BoardState,
        player: PlayerColor,
        remainingDice: MutableList<Int>,
        currentMoves: MutableList<Move>,
        allTurns: MutableList<List<Move>>
    ) {
        if (remainingDice.isEmpty()) {
            allTurns.add(currentMoves.toList())
            return
        }

        var anyMoveFound = false

        // Barda tas varsa oncelik bardan giris
        if (board.hasOnBar(player)) {
            for (dieIdx in remainingDice.indices) {
                val die = remainingDice[dieIdx]
                val entry = BackgammonEngine.barEntryPoint(player, die)
                if (entry in 0..23 && !BackgammonEngine.isPointBlocked(board, entry, player)) {
                    anyMoveFound = true
                    val isHit = BackgammonEngine.isHitMove(board, entry, player)
                    val move = Move(from = -1, to = entry, dieUsed = die, isHit = isHit)
                    val newBoard = BackgammonEngine.applyMove(board, move, player)

                    currentMoves.add(move)
                    val removedDie = remainingDice.removeAt(dieIdx)

                    generateTurnsRecursive(newBoard, player, remainingDice, currentMoves, allTurns)

                    currentMoves.removeAt(currentMoves.lastIndex)
                    remainingDice.add(dieIdx, removedDie)
                }
            }
        } else {
            // Normal hamleler
            for (i in 0..23) {
                if (board.checkerCount(i, player) == 0) continue

                for (dieIdx in remainingDice.indices) {
                    val die = remainingDice[dieIdx]
                    val destinations = getDestinationsForDie(board, player, i, die)

                    for (dest in destinations) {
                        anyMoveFound = true
                        val isHit = dest != -1 && BackgammonEngine.isHitMove(board, dest, player)
                        val move = Move(from = i, to = dest, dieUsed = die, isHit = isHit)
                        val newBoard = BackgammonEngine.applyMove(board, move, player)

                        currentMoves.add(move)
                        val removedDie = remainingDice.removeAt(dieIdx)

                        generateTurnsRecursive(newBoard, player, remainingDice, currentMoves, allTurns)

                        currentMoves.removeAt(currentMoves.lastIndex)
                        remainingDice.add(dieIdx, removedDie)
                    }
                }
            }
        }

        if (!anyMoveFound) {
            // Daha fazla hamle yapılamıyor - mevcut hamleleri kaydet
            allTurns.add(currentMoves.toList())
        }
    }

    /**
     * Turlari karsilastirmak icin anahtar olusturur (tekrarlari elemek icin).
     */
    private fun turnToKey(moves: List<Move>): String {
        return moves.joinToString("|") { "${it.from}->${it.to}(${it.dieUsed})" }
    }

    /**
     * Oyuncunun herhangi bir gecerli hamlesi var mi.
     */
    fun hasAnyLegalMove(
        board: BoardState,
        player: PlayerColor,
        remainingDice: List<Int>
    ): Boolean {
        return getMovablePoints(board, player, remainingDice).isNotEmpty()
    }

    /**
     * Zar degerlerini listeye cevirir.
     * Normal zar: [a, b], Cift zar: [a, a, a, a]
     */
    fun diceToList(die1: Int, die2: Int): List<Int> {
        return if (die1 == die2) {
            listOf(die1, die1, die1, die1)
        } else {
            listOf(die1, die2)
        }
    }
}
