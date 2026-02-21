package com.tavla.tavlapp.ui.board

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.sp
import com.tavla.tavlapp.engine.*
import kotlin.math.abs
import kotlin.math.min

// Tahta renkleri
object BoardColors {
    val boardBackground = Color(0xFF2E1A0E)       // Koyu kahverengi arka plan
    val boardFrame = Color(0xFF5D3A1A)             // Cerceve rengi
    val barColor = Color(0xFF3D2714)               // Orta bar rengi
    val lightTriangle = Color(0xFFD4A76A)           // Acik ucgen
    val darkTriangle = Color(0xFF8B4513)            // Koyu ucgen
    val whiteChecker = Color(0xFFF5F5DC)            // Beyaz tas (krem)
    val whiteCheckerBorder = Color(0xFF8B8B7A)      // Beyaz tas kenari
    val blackChecker = Color(0xFF2C2C2C)            // Siyah tas
    val blackCheckerBorder = Color(0xFF1A1A1A)      // Siyah tas kenari
    val selectedHighlight = Color(0x8800FF00)       // Secili nokta vurgusu (yesil)
    val legalMoveMarker = Color(0x8800BFFF)         // Gecerli hedef isareti (mavi)
    val bearOffArea = Color(0xFF3D2714)             // Cikarma bolgesi
    val hitHighlight = Color(0x88FF4444)            // Vurus vurgusu
    val pointNumberColor = Color(0xFFAA8855)        // Nokta numarasi rengi
}

/** Dokunma bolgeleri haritasi */
data class BoardLayout(
    val pointRects: Map<Int, Rect> = emptyMap(),    // 0-23 nokta bolgeleri
    val barRect: Rect = Rect.Zero,                   // Bar bolgesi
    val whiteBearOffRect: Rect = Rect.Zero,          // Beyaz cikarma
    val blackBearOffRect: Rect = Rect.Zero,          // Siyah cikarma
    val boardWidth: Float = 0f,
    val boardHeight: Float = 0f,
    val checkerRadius: Float = 0f,
    val pointWidth: Float = 0f,
    val pointHeight: Float = 0f,
    val frameWidth: Float = 0f,
    val barWidth: Float = 0f,
    val bearOffWidth: Float = 0f
)

/**
 * Tavla tahtasi Composable.
 * Canvas tabanli tam grafiksel tahta cizimi.
 */
@Composable
fun BackgammonBoard(
    boardState: BoardState,
    perspective: PlayerColor,  // Hangi oyuncunun bakis acisi (kendi taslari altta)
    selectedPoint: Int?,       // Secili kaynak nokta (-1=bar, 0-23=nokta, null=yok)
    legalDestinations: List<Int>,  // Gecerli hedef noktalari
    modifier: Modifier = Modifier,
    onPointTapped: (Int) -> Unit = {},       // Nokta tiklandiginda (-1=bar, 0-23=nokta)
    onBearOffTapped: () -> Unit = {},        // Cikarma bolgesi tiklandiginda
    doublingCube: DoublingCubeState? = null,
    animatingMove: Move? = null              // Animasyonlu hamle
) {
    var layout by remember { mutableStateOf(BoardLayout()) }

    Canvas(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(layout) {
                detectTapGestures { offset ->
                    handleTap(offset, layout, perspective, onPointTapped, onBearOffTapped)
                }
            }
    ) {
        layout = calculateLayout(size.width, size.height)
        drawBoard(layout, boardState, perspective, selectedPoint, legalDestinations, doublingCube)
    }
}

/**
 * Layout boyutlarini hesaplar.
 */
private fun calculateLayout(width: Float, height: Float): BoardLayout {
    val frameWidth = width * 0.02f
    val barWidth = width * 0.04f
    val bearOffWidth = width * 0.06f

    val playableWidth = width - (2 * frameWidth) - barWidth - (2 * bearOffWidth)
    val pointWidth = playableWidth / 12f
    val pointHeight = (height - 2 * frameWidth) * 0.42f
    val checkerRadius = min(pointWidth * 0.42f, pointHeight / 12f)

    val pointRects = mutableMapOf<Int, Rect>()

    // Ust siranin x pozisyonlari (13-24, soldan saga)
    for (i in 0..11) {
        val logicalPoint = 12 + i  // 12-23
        val x = if (i < 6) {
            // Sol yari (13-18): bearOff + frame + i * pointWidth
            bearOffWidth + frameWidth + i * pointWidth
        } else {
            // Sag yari (19-24): + bar boslugu
            bearOffWidth + frameWidth + i * pointWidth + barWidth
        }
        pointRects[logicalPoint] = Rect(
            left = x,
            top = frameWidth,
            right = x + pointWidth,
            bottom = frameWidth + pointHeight
        )
    }

    // Alt siranin x pozisyonlari (12-1, soldan saga)
    for (i in 0..11) {
        val logicalPoint = 11 - i  // 11-0
        val x = if (i < 6) {
            bearOffWidth + frameWidth + i * pointWidth
        } else {
            bearOffWidth + frameWidth + i * pointWidth + barWidth
        }
        pointRects[logicalPoint] = Rect(
            left = x,
            top = height - frameWidth - pointHeight,
            right = x + pointWidth,
            bottom = height - frameWidth
        )
    }

    val barLeft = bearOffWidth + frameWidth + 6 * pointWidth
    val barRect = Rect(
        left = barLeft,
        top = frameWidth,
        right = barLeft + barWidth,
        bottom = height - frameWidth
    )

    val whiteBearOffRect = Rect(
        left = width - bearOffWidth,
        top = height / 2,
        right = width,
        bottom = height - frameWidth
    )

    val blackBearOffRect = Rect(
        left = width - bearOffWidth,
        top = frameWidth,
        right = width,
        bottom = height / 2
    )

    return BoardLayout(
        pointRects = pointRects,
        barRect = barRect,
        whiteBearOffRect = whiteBearOffRect,
        blackBearOffRect = blackBearOffRect,
        boardWidth = width,
        boardHeight = height,
        checkerRadius = checkerRadius,
        pointWidth = pointWidth,
        pointHeight = pointHeight,
        frameWidth = frameWidth,
        barWidth = barWidth,
        bearOffWidth = bearOffWidth
    )
}

/**
 * Tahtayi cizer.
 */
private fun DrawScope.drawBoard(
    layout: BoardLayout,
    board: BoardState,
    perspective: PlayerColor,
    selectedPoint: Int?,
    legalDestinations: List<Int>,
    doublingCube: DoublingCubeState?
) {
    val width = layout.boardWidth
    val height = layout.boardHeight

    // 1. Arka plan
    drawRect(color = BoardColors.boardFrame, size = Size(width, height))

    // 2. Oyun alani arka plan
    drawRect(
        color = BoardColors.boardBackground,
        topLeft = Offset(layout.bearOffWidth + layout.frameWidth, layout.frameWidth),
        size = Size(
            width - 2 * layout.bearOffWidth - 2 * layout.frameWidth,
            height - 2 * layout.frameWidth
        )
    )

    // 3. Bar
    drawRect(
        color = BoardColors.barColor,
        topLeft = Offset(layout.barRect.left, layout.barRect.top),
        size = Size(layout.barWidth, layout.barRect.height)
    )

    // 4. Cikarma bolgeleri
    drawRect(
        color = BoardColors.bearOffArea,
        topLeft = Offset(layout.whiteBearOffRect.left, layout.whiteBearOffRect.top),
        size = Size(layout.bearOffWidth, layout.whiteBearOffRect.height)
    )
    drawRect(
        color = BoardColors.bearOffArea,
        topLeft = Offset(layout.blackBearOffRect.left, layout.blackBearOffRect.top),
        size = Size(layout.bearOffWidth, layout.blackBearOffRect.height)
    )
    // Sol cikarma bolgesi
    drawRect(
        color = BoardColors.bearOffArea,
        topLeft = Offset(0f, layout.frameWidth),
        size = Size(layout.bearOffWidth, height - 2 * layout.frameWidth)
    )

    // 5. Ucgenleri ciz
    drawTriangles(layout, perspective)

    // 6. Gecerli hedef isaretleri
    for (dest in legalDestinations) {
        if (dest == -1) {
            // Bear off vurgusu
            val bearOffRect = if (perspective == PlayerColor.WHITE) layout.whiteBearOffRect else layout.blackBearOffRect
            drawRect(
                color = BoardColors.legalMoveMarker,
                topLeft = Offset(bearOffRect.left, bearOffRect.top),
                size = Size(bearOffRect.width, bearOffRect.height)
            )
        } else {
            val mappedPoint = mapPointForPerspective(dest, perspective)
            val rect = layout.pointRects[mappedPoint] ?: continue
            drawRect(
                color = BoardColors.legalMoveMarker,
                topLeft = Offset(rect.left, rect.top),
                size = Size(rect.width, rect.height)
            )
        }
    }

    // 7. Secili nokta vurgusu
    if (selectedPoint != null) {
        if (selectedPoint == -1) {
            // Bar secili
            drawRect(
                color = BoardColors.selectedHighlight,
                topLeft = Offset(layout.barRect.left, layout.barRect.top),
                size = Size(layout.barRect.width, layout.barRect.height)
            )
        } else {
            val mappedPoint = mapPointForPerspective(selectedPoint, perspective)
            val rect = layout.pointRects[mappedPoint] ?: return
            drawRect(
                color = BoardColors.selectedHighlight,
                topLeft = Offset(rect.left, rect.top),
                size = Size(rect.width, rect.height)
            )
        }
    }

    // 8. Taslari ciz
    drawCheckers(layout, board, perspective)

    // 9. Bar taslari
    drawBarCheckers(layout, board, perspective)

    // 10. Cikarilmis taslari goster
    drawBorneOffCheckers(layout, board, perspective)

    // 11. Katlama zari
    if (doublingCube != null) {
        drawDoublingCube(layout, doublingCube, perspective)
    }

    // 12. Nokta numaralari
    drawPointNumbers(layout, perspective)
}

/**
 * Ucgenleri cizer.
 */
private fun DrawScope.drawTriangles(layout: BoardLayout, perspective: PlayerColor) {
    for ((logicalPoint, rect) in layout.pointRects) {
        val isTop = rect.top < layout.boardHeight / 2
        val isLight = logicalPoint % 2 == 0
        val color = if (isLight) BoardColors.lightTriangle else BoardColors.darkTriangle

        val path = Path().apply {
            if (isTop) {
                // Ust ucgen: yukari bakan
                moveTo(rect.left, rect.top)
                lineTo(rect.right, rect.top)
                lineTo(rect.center.x, rect.bottom)
                close()
            } else {
                // Alt ucgen: asagi bakan
                moveTo(rect.left, rect.bottom)
                lineTo(rect.right, rect.bottom)
                lineTo(rect.center.x, rect.top)
                close()
            }
        }

        drawPath(path, color, style = Fill)
        drawPath(path, BoardColors.boardBackground, style = Stroke(width = 1f))
    }
}

/**
 * Taslari cizer.
 */
private fun DrawScope.drawCheckers(
    layout: BoardLayout,
    board: BoardState,
    perspective: PlayerColor
) {
    val radius = layout.checkerRadius
    val maxVisible = 5  // Bir noktada gosterilecek max tas

    for (logicalPoint in 0..23) {
        val mappedPoint = mapPointForPerspective(logicalPoint, perspective)
        val rect = layout.pointRects[mappedPoint] ?: continue
        val count = board.points[logicalPoint]

        if (count == 0) continue

        val isWhite = count > 0
        val absCount = abs(count)
        val color = if (isWhite) BoardColors.whiteChecker else BoardColors.blackChecker
        val borderColor = if (isWhite) BoardColors.whiteCheckerBorder else BoardColors.blackCheckerBorder

        val isTop = rect.top < layout.boardHeight / 2
        val displayCount = min(absCount, maxVisible)
        val spacing = min(radius * 2, rect.height / maxVisible)

        for (i in 0 until displayCount) {
            val cx = rect.center.x
            val cy = if (isTop) {
                rect.top + radius + i * spacing
            } else {
                rect.bottom - radius - i * spacing
            }

            // Tas dairesi
            drawCircle(color = color, radius = radius, center = Offset(cx, cy))
            drawCircle(color = borderColor, radius = radius, center = Offset(cx, cy), style = Stroke(width = 2f))

            // 5'ten fazla tas varsa sayi goster
            if (i == displayCount - 1 && absCount > maxVisible) {
                drawContext.canvas.nativeCanvas.apply {
                    val paint = android.graphics.Paint().apply {
                        textAlign = android.graphics.Paint.Align.CENTER
                        textSize = radius * 0.9f
                        this.color = if (isWhite) android.graphics.Color.BLACK else android.graphics.Color.WHITE
                        isFakeBoldText = true
                    }
                    drawText(absCount.toString(), cx, cy + radius * 0.3f, paint)
                }
            }
        }
    }
}

/**
 * Bar (kirik) taslarini cizer.
 */
private fun DrawScope.drawBarCheckers(
    layout: BoardLayout,
    board: BoardState,
    perspective: PlayerColor
) {
    val radius = layout.checkerRadius * 0.9f
    val barCx = layout.barRect.center.x

    // Beyaz bar taslari (perspektife gore ust veya alt)
    if (board.whiteBar > 0) {
        val isBottom = perspective == PlayerColor.WHITE
        for (i in 0 until min(board.whiteBar, 4)) {
            val cy = if (isBottom) {
                layout.barRect.bottom - radius - i * radius * 2.2f
            } else {
                layout.barRect.top + radius + i * radius * 2.2f
            }
            drawCircle(color = BoardColors.whiteChecker, radius = radius, center = Offset(barCx, cy))
            drawCircle(color = BoardColors.whiteCheckerBorder, radius = radius, center = Offset(barCx, cy), style = Stroke(2f))
        }
        if (board.whiteBar > 4) {
            drawContext.canvas.nativeCanvas.apply {
                val paint = android.graphics.Paint().apply {
                    textAlign = android.graphics.Paint.Align.CENTER
                    textSize = radius
                    color = android.graphics.Color.BLACK
                    isFakeBoldText = true
                }
                val cy = if (isBottom) layout.barRect.bottom - radius else layout.barRect.top + radius
                drawText(board.whiteBar.toString(), barCx, cy + radius * 0.3f, paint)
            }
        }
    }

    // Siyah bar taslari
    if (board.blackBar > 0) {
        val isBottom = perspective == PlayerColor.BLACK
        for (i in 0 until min(board.blackBar, 4)) {
            val cy = if (isBottom) {
                layout.barRect.bottom - radius - i * radius * 2.2f
            } else {
                layout.barRect.top + radius + i * radius * 2.2f
            }
            drawCircle(color = BoardColors.blackChecker, radius = radius, center = Offset(barCx, cy))
            drawCircle(color = BoardColors.blackCheckerBorder, radius = radius, center = Offset(barCx, cy), style = Stroke(2f))
        }
        if (board.blackBar > 4) {
            drawContext.canvas.nativeCanvas.apply {
                val paint = android.graphics.Paint().apply {
                    textAlign = android.graphics.Paint.Align.CENTER
                    textSize = radius
                    color = android.graphics.Color.WHITE
                    isFakeBoldText = true
                }
                val cy = if (isBottom) layout.barRect.bottom - radius else layout.barRect.top + radius
                drawText(board.blackBar.toString(), barCx, cy + radius * 0.3f, paint)
            }
        }
    }
}

/**
 * Cikarilmis taslari gosterir.
 */
private fun DrawScope.drawBorneOffCheckers(
    layout: BoardLayout,
    board: BoardState,
    perspective: PlayerColor
) {
    val radius = layout.checkerRadius * 0.5f

    // Beyaz cikarilmis
    if (board.whiteBorneOff > 0) {
        val rect = if (perspective == PlayerColor.WHITE) layout.whiteBearOffRect else layout.blackBearOffRect
        val cx = rect.center.x
        for (i in 0 until min(board.whiteBorneOff, 15)) {
            val cy = rect.bottom - 4f - i * (radius * 1.3f)
            drawCircle(color = BoardColors.whiteChecker, radius = radius, center = Offset(cx, cy))
            drawCircle(color = BoardColors.whiteCheckerBorder, radius = radius, center = Offset(cx, cy), style = Stroke(1f))
        }
    }

    // Siyah cikarilmis
    if (board.blackBorneOff > 0) {
        val rect = if (perspective == PlayerColor.BLACK) layout.whiteBearOffRect else layout.blackBearOffRect
        val cx = rect.center.x
        for (i in 0 until min(board.blackBorneOff, 15)) {
            val cy = rect.top + 4f + i * (radius * 1.3f)
            drawCircle(color = BoardColors.blackChecker, radius = radius, center = Offset(cx, cy))
            drawCircle(color = BoardColors.blackCheckerBorder, radius = radius, center = Offset(cx, cy), style = Stroke(1f))
        }
    }
}

/**
 * Katlama zarini cizer.
 */
private fun DrawScope.drawDoublingCube(
    layout: BoardLayout,
    cube: DoublingCubeState,
    perspective: PlayerColor
) {
    val cubeSize = layout.checkerRadius * 2.5f
    val cx = layout.barRect.center.x
    val cy = layout.boardHeight / 2  // Ortada

    // Kupu ciz
    drawRect(
        color = Color.White,
        topLeft = Offset(cx - cubeSize / 2, cy - cubeSize / 2),
        size = Size(cubeSize, cubeSize)
    )
    drawRect(
        color = Color.Black,
        topLeft = Offset(cx - cubeSize / 2, cy - cubeSize / 2),
        size = Size(cubeSize, cubeSize),
        style = Stroke(width = 2f)
    )

    // Kup degeri
    drawContext.canvas.nativeCanvas.apply {
        val paint = android.graphics.Paint().apply {
            textAlign = android.graphics.Paint.Align.CENTER
            textSize = cubeSize * 0.5f
            color = android.graphics.Color.BLACK
            isFakeBoldText = true
        }
        drawText(cube.value.toString(), cx, cy + cubeSize * 0.15f, paint)
    }
}

/**
 * Nokta numaralarini cizer.
 */
private fun DrawScope.drawPointNumbers(layout: BoardLayout, perspective: PlayerColor) {
    drawContext.canvas.nativeCanvas.apply {
        val paint = android.graphics.Paint().apply {
            textAlign = android.graphics.Paint.Align.CENTER
            textSize = layout.pointWidth * 0.3f
            color = BoardColors.pointNumberColor.hashCode()
        }

        for ((logicalPoint, rect) in layout.pointRects) {
            val displayNum = mapPointForDisplay(logicalPoint, perspective)
            val cx = rect.center.x
            val isTop = rect.top < layout.boardHeight / 2
            val cy = if (isTop) {
                rect.top - 4f
            } else {
                rect.bottom + paint.textSize + 2f
            }
            drawText(displayNum.toString(), cx, cy, paint)
        }
    }
}

/**
 * Perspektife gore nokta indeksini haritalandirir.
 * Beyaz perspektif: kendi taslari altta (1-6 sag alt)
 * Siyah perspektif: kendi taslari altta (19-24 sag alt)
 */
fun mapPointForPerspective(logicalPoint: Int, perspective: PlayerColor): Int {
    // Beyaz perspektif: standart gorunum (0=sag alt, 23=sol ust)
    // Siyah perspektif: ters gorunum
    return if (perspective == PlayerColor.WHITE) {
        logicalPoint
    } else {
        23 - logicalPoint
    }
}

/**
 * Gosterim icin nokta numarasini hesaplar.
 */
private fun mapPointForDisplay(logicalPoint: Int, perspective: PlayerColor): Int {
    val actualPoint = if (perspective == PlayerColor.WHITE) logicalPoint else 23 - logicalPoint
    return actualPoint + 1  // 1-indexed
}

/**
 * Dokunma olayini isler.
 */
private fun handleTap(
    offset: Offset,
    layout: BoardLayout,
    perspective: PlayerColor,
    onPointTapped: (Int) -> Unit,
    onBearOffTapped: () -> Unit
) {
    // Bar tiklanma kontrolu
    if (layout.barRect.contains(offset)) {
        onPointTapped(-1)
        return
    }

    // Cikarma bolgesi kontrolu (beyaz veya siyah perspektifine gore)
    if (layout.whiteBearOffRect.contains(offset) || layout.blackBearOffRect.contains(offset)) {
        onBearOffTapped()
        return
    }

    // Sol cikarma bolgesi kontrolu
    if (offset.x < layout.bearOffWidth) {
        onBearOffTapped()
        return
    }

    // Nokta tiklanma kontrolu
    for ((logicalPoint, rect) in layout.pointRects) {
        if (rect.contains(offset)) {
            // Perspektife gore gercek noktayi bul
            val actualPoint = if (perspective == PlayerColor.WHITE) logicalPoint else 23 - logicalPoint
            onPointTapped(actualPoint)
            return
        }
    }
}
