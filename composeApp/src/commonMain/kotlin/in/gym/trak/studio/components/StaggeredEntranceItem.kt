package `in`.gym.trak.studio.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.animation.core.rememberTransition
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer

/**
 * Reusable staggered animation wrapper for list items or layout elements.
 * Provides a smooth fade-in and slide-up entrance effect.
 */
@Composable
fun StaggeredEntranceItem(
    index: Int,
    content: @Composable () -> Unit
) {
    val visibleState = remember {
        MutableTransitionState(false).apply { targetState = true }
    }
    
    val transition = rememberTransition(visibleState, label = "EntranceTransition")

    val alpha by transition.animateFloat(
        transitionSpec = {
            tween(
                durationMillis = 600,
                delayMillis = index * 80,
                easing = FastOutSlowInEasing
            )
        },
        label = "Alpha"
    ) { if (it) 1f else 0f }

    val translateY by transition.animateFloat(
        transitionSpec = {
            tween(
                durationMillis = 600,
                delayMillis = index * 80,
                easing = FastOutSlowInEasing
            )
        },
        label = "TranslateY"
    ) { if (it) 0f else 60f }

    Box(
        modifier = Modifier.graphicsLayer {
            this.alpha = alpha
            this.translationY = translateY
        }
    ) {
        content()
    }
}
