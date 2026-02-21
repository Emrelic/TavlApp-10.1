package com.tavla.tavlapp.engine

/**
 * Tur dogrulama.
 * Oyuncunun yaptigi hamlelerin kurallara uygunlugunu kontrol eder.
 */
object MoveValidator {

    data class ValidationResult(
        val isValid: Boolean,
        val errorMessage: String? = null
    )

    /**
     * Tum turun gecerligini dogrular.
     * @param board Baslangic tahta durumu
     * @param player Hamle yapan oyuncu
     * @param dice Zar degerleri (2 veya 4 eleman)
     * @param moves Oyuncunun yaptigi hamleler
     * @return ValidationResult
     */
    fun validateTurn(
        board: BoardState,
        player: PlayerColor,
        dice: List<Int>,
        moves: List<Move>
    ): ValidationResult {
        // Bos tur - sadece hamle yapamiyorsa gecerli
        if (moves.isEmpty()) {
            if (!MoveGenerator.hasAnyLegalMove(board, player, dice)) {
                return ValidationResult(true)
            }
            return ValidationResult(false, "Hamle yapilabilir durumda bos tur gecilemez")
        }

        // Her hamleyi sirayla dogrula
        var currentBoard = board
        val remainingDice = dice.toMutableList()

        for ((index, move) in moves.withIndex()) {
            // Kullanilan zar mevcut mu?
            val dieIndex = remainingDice.indexOf(move.dieUsed)
            if (dieIndex == -1) {
                return ValidationResult(false, "Hamle #${index + 1}: Zar degeri ${move.dieUsed} mevcut degil")
            }

            // Hamle gecerli mi?
            val singleValidation = validateSingleMove(currentBoard, move, player)
            if (!singleValidation.isValid) {
                return ValidationResult(false, "Hamle #${index + 1}: ${singleValidation.errorMessage}")
            }

            // Hamleyi uygula
            currentBoard = BackgammonEngine.applyMove(currentBoard, move, player)
            remainingDice.removeAt(dieIndex)
        }

        // Maksimum zar kullanimi kontrolu
        if (remainingDice.isNotEmpty()) {
            // Kalan zarlarla hamle yapilabilir mi?
            if (MoveGenerator.hasAnyLegalMove(currentBoard, player, remainingDice)) {
                return ValidationResult(false, "Tum kullanilabilir zarlar kullanilmali")
            }
        }

        // Maksimum zar kullanim kuralini kontrol et
        val maxValidation = validateMaximumDiceUsage(board, player, dice, moves)
        if (!maxValidation.isValid) {
            return maxValidation
        }

        return ValidationResult(true)
    }

    /**
     * Tek bir hamlenin gecerligini kontrol eder.
     */
    fun validateSingleMove(
        board: BoardState,
        move: Move,
        player: PlayerColor
    ): ValidationResult {
        // Barda tas varsa bardan oynamali
        if (board.hasOnBar(player) && move.from != -1) {
            return ValidationResult(false, "Barda tas varken sadece bardan oynayabilirsiniz")
        }

        // Kaynak kontrolu
        if (move.from == -1) {
            // Bar kontrolu
            if (!board.hasOnBar(player)) {
                return ValidationResult(false, "Barda tas yok")
            }
        } else if (move.from < 0 || move.from > 23) {
            return ValidationResult(false, "Gecersiz kaynak nokta: ${move.from}")
        } else if (board.checkerCount(move.from, player) == 0) {
            return ValidationResult(false, "Kaynak noktada tasiniz yok")
        }

        // Hedef kontrolu
        if (move.to == -1) {
            // Bear off kontrolu
            if (!BackgammonEngine.canBearOff(board, player)) {
                return ValidationResult(false, "Tum taslar ic sahada degil, cikaramazsiniz")
            }

            // Zar degeri ile cikarma kontrolu
            if (move.from != -1) {
                val dest = BackgammonEngine.destinationPoint(move.from, move.dieUsed, player)
                if (dest != -1) {
                    // Overshoot olabilir
                    if (!BackgammonEngine.canBearOffWithExactOrHigher(board, player, move.from, move.dieUsed)) {
                        return ValidationResult(false, "Bu zar degeri ile bu noktadan cikaramazsiniz")
                    }
                }
            }
        } else if (move.to < 0 || move.to > 23) {
            return ValidationResult(false, "Gecersiz hedef nokta: ${move.to}")
        } else {
            // Bloke kontrolu
            if (BackgammonEngine.isPointBlocked(board, move.to, player)) {
                return ValidationResult(false, "Hedef nokta bloke edilmis")
            }

            // Yon kontrolu
            if (move.from != -1) {
                val expectedDest = BackgammonEngine.destinationPoint(move.from, move.dieUsed, player)
                if (expectedDest != move.to) {
                    return ValidationResult(false, "Zar degeri ile hedef uyusmuyor")
                }
            } else {
                val expectedEntry = BackgammonEngine.barEntryPoint(player, move.dieUsed)
                if (expectedEntry != move.to) {
                    return ValidationResult(false, "Bar girisi ile hedef uyusmuyor")
                }
            }

            // Vurus kontrolu
            val shouldBeHit = BackgammonEngine.isHitMove(board, move.to, player)
            if (move.isHit != shouldBeHit) {
                return ValidationResult(false, "Vurus bilgisi yanlis")
            }
        }

        return ValidationResult(true)
    }

    /**
     * Maksimum zar kullanim kuralini kontrol eder.
     * Kurallara gore: mumkunse her iki zar kullanilmali,
     * sadece biri kullanilabiliyorsa buyuk olan tercih edilmeli.
     */
    private fun validateMaximumDiceUsage(
        board: BoardState,
        player: PlayerColor,
        dice: List<Int>,
        moves: List<Move>
    ): ValidationResult {
        // Tum gecerli turlari uret
        val allValidTurns = MoveGenerator.generateAllTurns(board, player, dice)

        // Bos olmayan en uzun tur uzunlugu
        val maxPossibleMoves = allValidTurns.maxOfOrNull { it.size } ?: 0

        if (moves.size < maxPossibleMoves) {
            return ValidationResult(false, "Daha fazla zar kullanilabilir ($maxPossibleMoves hamle mumkun)")
        }

        return ValidationResult(true)
    }
}
