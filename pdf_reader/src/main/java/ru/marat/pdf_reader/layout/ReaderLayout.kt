package ru.marat.pdf_reader.layout

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.rememberScrollableState
import androidx.compose.foundation.gestures.scrollable
import androidx.compose.foundation.lazy.layout.LazyLayout
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.isUnspecified
import androidx.compose.ui.geometry.toRect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEach
import androidx.compose.ui.util.fastMap
import ru.marat.pdf_reader.layout.state.LoadingState
import ru.marat.pdf_reader.layout.state.ReaderLayoutState
import kotlin.math.roundToInt

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ReaderLayout(
    modifier: Modifier = Modifier,
    state: ReaderLayoutState,
    offsetChange: (Float) -> Unit = {},
) {
    val itemProvider = rememberPagesItemProvider(state.pages)
    val scrollstate = rememberScrollableState {
        if (state.loadingState is LoadingState.Loading)
            return@rememberScrollableState 0f
        state.scrollState.onOffsetChange(Offset(0f, it))
        offsetChange(state.scrollState.offset.y)
        it
    }

    val loadedPages by remember {
        derivedStateOf {
            if (state.viewportSize.isUnspecified) return@derivedStateOf emptyList()
            state.pagePositions.filter {
                val r = state.viewportSize.toRect()
                    .translate(0f, -state.scrollState.offset.y)
                r.overlaps(
                    Rect(0f, it.start, 1f, it.end)
                )
            }
        }
    }

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
        val items = if (state.pages.isNotEmpty()) loadedPages.fastMap { //todo
            Pair(
                it,
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
            state.viewportSize =
                Size(constraints.maxWidth.toFloat(), constraints.maxHeight.toFloat())
            items.fastForEach { (pos, placeables) ->
                placeables.firstOrNull()?.placeRelative(
                    0,
                    state.scrollState.offset.y.roundToInt() + pos.start.roundToInt()
                )
            }
        }
    }
    DisposableEffect(Unit) {
        onDispose {
            state.onDispose()
        }
    }
}