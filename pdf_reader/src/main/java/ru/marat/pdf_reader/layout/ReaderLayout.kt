package ru.marat.pdf_reader.layout

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.lazy.layout.LazyLayout
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEach
import androidx.compose.ui.util.fastMap
import ru.marat.pdf_reader.gestures.readerGestures
import ru.marat.pdf_reader.layout.state.ReaderLayoutState
import kotlin.math.roundToInt

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ReaderLayout(
    modifier: Modifier = Modifier,
    state: ReaderLayoutState,
    spacing: Dp = 8.dp,
) {
    val itemProvider = rememberPagesItemProvider(state.pages)

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
            .readerGestures(state.scrollState),
        itemProvider = { itemProvider }
    ) { constraints: Constraints ->
        val items = if (state.pages.isNotEmpty()) state.loadedPages.fastMap { //todo
            Pair(
                it, measure(
                    it.index,
                    constraints.copy(
                        minHeight = 0,
                        maxHeight = Constraints.Infinity,
                    )
                )
            )
        } else emptyList()
        layout(width = constraints.maxWidth, height = constraints.maxHeight) {
            state.setSpacing(spacing.toPx())
            state.setLayoutViewportSize(
                Size(
                    constraints.maxWidth.toFloat(),
                    constraints.maxHeight.toFloat()
                )
            )
            items.fastForEach { (pos, placeables) ->
                println("${state.scrollState.offsetY} ${pos.start}")
                placeables.firstOrNull()?.placeRelative(
                    state.scrollState.offsetX.roundToInt(),
                    (state.scrollState.offsetY + pos.start).roundToInt()
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