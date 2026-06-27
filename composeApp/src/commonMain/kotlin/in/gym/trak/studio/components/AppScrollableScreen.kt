package `in`.gym.trak.studio.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.focus.onFocusEvent
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * Shared scroll + keyboard insets used across form screens (see Assign Subscription).
 */
object AppScrollDefaults {
    val horizontalPadding: Dp = 16.dp
    val bottomContentPadding: Dp = 32.dp

    /** Extra top inset when content scrolls under a transparent top app bar. */
    val transparentTopBarContentInset: Dp = 100.dp

    fun screenContentPadding(
        horizontal: Dp = horizontalPadding,
        topInset: Dp = 0.dp,
        bottom: Dp = bottomContentPadding,
    ): PaddingValues = PaddingValues(
        start = horizontal,
        end = horizontal,
        top = topInset,
        bottom = bottom,
    )

    fun sheetContentPadding(
        horizontal: Dp = horizontalPadding,
        bottom: Dp = 24.dp,
    ): PaddingValues = PaddingValues(
        start = horizontal,
        end = horizontal,
        top = 24.dp,
        bottom = bottom,
    )

    val noContentPadding: PaddingValues = PaddingValues(0.dp)
}

/**
 * Full-screen scrollable column: [imePadding], vertical scroll, and consistent content padding.
 */
@Composable
fun AppScrollableScreen(
    modifier: Modifier = Modifier,
    scrollState: androidx.compose.foundation.ScrollState = rememberScrollState(),
    contentPadding: PaddingValues = AppScrollDefaults.screenContentPadding(),
    imePaddingEnabled: Boolean = true,
    dismissKeyboardOnTap: Boolean = false,
    horizontalAlignment: Alignment.Horizontal = Alignment.Start,
    verticalArrangement: Arrangement.Vertical = Arrangement.Top,
    content: @Composable ColumnScope.() -> Unit,
) {
    val focusManager = LocalFocusManager.current
    val scrollModifier = modifier
        .fillMaxSize()
        .then(if (imePaddingEnabled) Modifier.imePadding() else Modifier)
        .verticalScroll(scrollState)
        .padding(contentPadding)
        .then(
            if (dismissKeyboardOnTap) {
                Modifier.clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                ) { focusManager.clearFocus() }
            } else {
                Modifier
            }
        )

    Column(
        modifier = scrollModifier,
        horizontalAlignment = horizontalAlignment,
        verticalArrangement = verticalArrangement,
        content = content,
    )
}

/**
 * Bottom sheet / modal body scroll column.
 */
@Composable
fun AppScrollableSheetColumn(
    modifier: Modifier = Modifier,
    scrollState: androidx.compose.foundation.ScrollState = rememberScrollState(),
    contentPadding: PaddingValues = AppScrollDefaults.sheetContentPadding(),
    imePaddingEnabled: Boolean = true,
    verticalArrangement: Arrangement.Vertical = Arrangement.Top,
    content: @Composable ColumnScope.() -> Unit,
) {
    val scrollModifier = modifier
        .navigationBarsPadding()
//        .then(if (imePaddingEnabled) Modifier.imePadding() else Modifier)
        .verticalScroll(scrollState)
        .padding(contentPadding)

    Column(
        modifier = scrollModifier,
        verticalArrangement = verticalArrangement,
        content = content,
    )
}

/**
 * Lazy list variant with the same content padding defaults.
 */
@Composable
fun AppScrollLazyColumn(
    modifier: Modifier = Modifier,
    state: LazyListState = rememberLazyListState(),
    contentPadding: PaddingValues = AppScrollDefaults.screenContentPadding(),
    imePaddingEnabled: Boolean = true,
    verticalArrangement: Arrangement.Vertical = Arrangement.Top,
    content: LazyListScope.() -> Unit,
) {
    val scrollModifier = modifier
        .fillMaxSize()
        .then(if (imePaddingEnabled) Modifier.imePadding() else Modifier)

    LazyColumn(
        modifier = scrollModifier,
        state = state,
        contentPadding = contentPadding,
        verticalArrangement = verticalArrangement,
        content = content,
    )
}

/**
 * Scroll focused field into view when the keyboard opens (Assign Subscription price field behavior).
 */
fun Modifier.bringIntoViewOnFocus(
    scrollState: androidx.compose.foundation.ScrollState,
    scope: CoroutineScope? = null,
): Modifier = composed {
    val coroutineScope = scope ?: rememberCoroutineScope()
    val requester = remember { BringIntoViewRequester() }
    bringIntoViewRequester(requester).onFocusEvent { focusState ->
        if (focusState.isFocused) {
            coroutineScope.launch {
                requester.bringIntoView()
                val remaining = scrollState.maxValue - scrollState.value
                if (remaining > 0) {
                    scrollState.animateScrollBy(remaining.toFloat())
                }
            }
        }
    }
}
