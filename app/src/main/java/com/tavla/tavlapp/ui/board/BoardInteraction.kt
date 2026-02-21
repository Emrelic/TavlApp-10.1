package com.tavla.tavlapp.ui.board

import com.tavla.tavlapp.engine.*

/**
 * Tahta etkilesim yoneticisi.
 * Kullanici tiklamalarini hamlelere cevirir.
 */
class BoardInteraction(
    private val onMoveExecuted: (Move) -> Unit,
    private val onSelectionChanged: (selectedPoint: Int?, legalDestinations: List<Int>) -> Unit
) {
    private var selectedPoint: Int? = null
    private var legalDestinations: List<Pair<Int, Int>> = emptyList() // Pair<dest, dieUsed>

    /**
     * Nokta tiklandiginda cagirilir.
     * @param point Tiklanan nokta (-1=bar, 0-23=nokta)
     * @param board Mevcut tahta durumu
     * @param player Sira kimde
     * @param remainingDice Kalan zarlar
     */
    fun onPointTapped(
        point: Int,
        board: BoardState,
        player: PlayerColor,
        remainingDice: List<Int>
    ) {
        if (remainingDice.isEmpty()) {
            clearSelection()
            return
        }

        val currentSelected = selectedPoint

        if (currentSelected == null) {
            // Ilk tiklama: kaynak sec
            trySelect(point, board, player, remainingDice)
        } else if (currentSelected == point) {
            // Ayni noktaya tiklama: secimi kaldir
            clearSelection()
        } else {
            // Ikinci tiklama: hedefe hamle yap veya yeni kaynak sec
            val destination = legalDestinations.find { it.first == point }
            if (destination != null) {
                // Gecerli hedef - hamle yap
                executeMove(currentSelected, destination, board, player)
            } else {
                // Gecerli hedef degil - yeni kaynak secmeyi dene
                trySelect(point, board, player, remainingDice)
            }
        }
    }

    /**
     * Bear off bolgesi tiklandiginda cagirilir.
     */
    fun onBearOffTapped(
        board: BoardState,
        player: PlayerColor,
        remainingDice: List<Int>
    ) {
        val currentSelected = selectedPoint ?: return

        // Bear off hedefini ara
        val destination = legalDestinations.find { it.first == -1 }
        if (destination != null) {
            executeMove(currentSelected, destination, board, player)
        }
    }

    /**
     * Kaynak noktayi secmeyi dener.
     */
    private fun trySelect(
        point: Int,
        board: BoardState,
        player: PlayerColor,
        remainingDice: List<Int>
    ) {
        // Barda tas varsa sadece bar secilebilir
        if (board.hasOnBar(player)) {
            if (point != -1) {
                clearSelection()
                return
            }
        } else if (point == -1) {
            clearSelection()
            return
        }

        // Bu noktada oyuncunun tasi var mi?
        if (point != -1 && board.checkerCount(point, player) == 0) {
            clearSelection()
            return
        }

        // Gecerli hedefleri hesapla
        val destinations = MoveGenerator.getDestinations(board, player, point, remainingDice)

        if (destinations.isEmpty()) {
            clearSelection()
            return
        }

        selectedPoint = point
        legalDestinations = destinations
        onSelectionChanged(point, destinations.map { it.first })
    }

    /**
     * Hamle uygular.
     */
    private fun executeMove(
        from: Int,
        destination: Pair<Int, Int>,  // Pair<to, dieUsed>
        board: BoardState,
        player: PlayerColor
    ) {
        val to = destination.first
        val dieUsed = destination.second
        val isHit = to != -1 && BackgammonEngine.isHitMove(board, to, player)

        val move = Move(
            from = from,
            to = to,
            dieUsed = dieUsed,
            isHit = isHit
        )

        clearSelection()
        onMoveExecuted(move)
    }

    /**
     * Secimi temizler.
     */
    fun clearSelection() {
        selectedPoint = null
        legalDestinations = emptyList()
        onSelectionChanged(null, emptyList())
    }

    /**
     * Mevcut secili noktayi doner.
     */
    fun getSelectedPoint(): Int? = selectedPoint
}
