package com.tavla.tavlapp.ui.board

import androidx.compose.animation.core.*
import androidx.compose.runtime.*
import androidx.compose.ui.geometry.Offset
import com.tavla.tavlapp.engine.Move
import com.tavla.tavlapp.engine.PlayerColor

/**
 * Tahta animasyonlari yonetimi.
 */

/** Animasyon durumu */
data class AnimationState(
    val isAnimating: Boolean = false,
    val currentMove: Move? = null,
    val fromPosition: Offset = Offset.Zero,
    val toPosition: Offset = Offset.Zero,
    val progress: Float = 0f,
    val animationType: AnimationType = AnimationType.MOVE
)

enum class AnimationType {
    MOVE,      // Normal tas hareketi
    HIT,       // Vurus (tas bara gider)
    BEAR_OFF   // Tas cikarma
}

/**
 * Tas hareket animasyonu state holder.
 */
class BoardAnimationController {
    private var _animationState = mutableStateOf(AnimationState())
    val animationState: State<AnimationState> = _animationState

    private var onAnimationComplete: (() -> Unit)? = null

    /**
     * Hamle animasyonu baslatir.
     */
    fun animateMove(
        move: Move,
        layout: BoardLayout,
        perspective: PlayerColor,
        onComplete: () -> Unit
    ) {
        val fromPos = getCheckerPosition(move.from, layout, perspective, isSource = true)
        val toPos = getCheckerPosition(move.to, layout, perspective, isSource = false)

        _animationState.value = AnimationState(
            isAnimating = true,
            currentMove = move,
            fromPosition = fromPos,
            toPosition = toPos,
            progress = 0f,
            animationType = when {
                move.to == -1 -> AnimationType.BEAR_OFF
                move.isHit -> AnimationType.HIT
                else -> AnimationType.MOVE
            }
        )

        onAnimationComplete = onComplete
    }

    /**
     * Animasyon ilerlemesini gunceller.
     */
    fun updateProgress(progress: Float) {
        _animationState.value = _animationState.value.copy(progress = progress)
        if (progress >= 1f) {
            completeAnimation()
        }
    }

    /**
     * Animasyonu tamamlar.
     */
    fun completeAnimation() {
        _animationState.value = AnimationState()
        onAnimationComplete?.invoke()
        onAnimationComplete = null
    }

    /**
     * Nokta veya bar pozisyonunun ekran koordinatlarini hesaplar.
     */
    private fun getCheckerPosition(
        point: Int,
        layout: BoardLayout,
        perspective: PlayerColor,
        isSource: Boolean
    ): Offset {
        return when (point) {
            -1 -> {
                // Bar veya bear off
                if (isSource) {
                    Offset(layout.barRect.center.x, layout.barRect.center.y)
                } else {
                    // Bear off pozisyonu
                    val bearOff = if (perspective == PlayerColor.WHITE) {
                        layout.whiteBearOffRect
                    } else {
                        layout.blackBearOffRect
                    }
                    Offset(bearOff.center.x, bearOff.center.y)
                }
            }
            in 0..23 -> {
                val mapped = mapPointForPerspective(point, perspective)
                val rect = layout.pointRects[mapped]
                if (rect != null) {
                    Offset(rect.center.x, rect.center.y)
                } else {
                    Offset.Zero
                }
            }
            else -> Offset.Zero
        }
    }
}

/**
 * Animasyonlu hamle Composable yardimcisi.
 * Hamleyi animasyonla gosterir, bitince callback cagrilir.
 */
@Composable
fun AnimatedMoveEffect(
    move: Move?,
    layout: BoardLayout,
    perspective: PlayerColor,
    onComplete: () -> Unit
) {
    if (move == null) return

    val animationController = remember { BoardAnimationController() }

    val progress by animateFloatAsState(
        targetValue = if (animationController.animationState.value.isAnimating) 1f else 0f,
        animationSpec = tween(
            durationMillis = 300,
            easing = FastOutSlowInEasing
        ),
        label = "move_animation",
        finishedListener = {
            animationController.completeAnimation()
        }
    )

    LaunchedEffect(move) {
        animationController.animateMove(move, layout, perspective, onComplete)
    }

    LaunchedEffect(progress) {
        animationController.updateProgress(progress)
    }
}
