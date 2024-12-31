package ru.marat.pdf_reader.layout

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.gestures.rememberScrollableState
import androidx.compose.foundation.gestures.scrollable
import androidx.compose.foundation.lazy.layout.LazyLayout
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEach
import androidx.compose.ui.util.fastMap
import ru.marat.pdf_reader.layout.state.ReaderLayoutState
import kotlin.math.roundToInt

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ReaderLayout(
    modifier: Modifier = Modifier,
    state: ReaderLayoutState
) {
    val itemProvider = rememberPagesItemProvider(state.pages)
    val offset = remember { mutableFloatStateOf(0f) }
    val scrollstate = rememberScrollableState { offset.floatValue += it; it }
    LazyLayout(
        modifier = modifier
            .drawWithContent {
                drawContent()
                drawRect(
                    color = Color.Red,
                    style = Stroke(
                        width = 4.dp.toPx()
                    )
                )
            }
            .scrollable(scrollstate, orientation = Orientation.Vertical),
        itemProvider = { itemProvider }
    ) { constraints: Constraints ->
        val items = if (state.pages.isNotEmpty()) state.pages.subList(0, 3).fastMap { //todo
            Pair(
                it.rect,
                measure(
                    it.index,
                    constraints.copy(
                        minHeight = 0,
                        maxHeight = Constraints.Infinity,
                    )
                )
            )
        } else emptyList()
        layout(width = constraints.maxWidth, height = constraints.maxHeight) {
            var bottomOffset = 0 + offset.floatValue.roundToInt()
            items.fastForEach { (rect, placeables) ->
                placeables.firstOrNull()?.placeRelative(
                    rect.topLeft.x.roundToInt(),
                    bottomOffset
                )
                bottomOffset += placeables.firstOrNull()?.height ?: 0
            }
        }
    }
    DisposableEffect(Unit) {
        onDispose {
            state.onDispose()
        }
    }
}