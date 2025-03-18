package ru.marat.pdf_reader.layout

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.lazy.layout.LazyLayout
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEach
import androidx.compose.ui.util.fastMap
import kotlinx.coroutines.Dispatchers
import ru.marat.pdf_reader.gestures.ReaderLayoutPositionState
import ru.marat.pdf_reader.gestures.readerGestures
import ru.marat.pdf_reader.layout.state.LayoutParams
import ru.marat.pdf_reader.layout.state.PagePosition
import ru.marat.pdf_reader.layout.state.ReaderState
import kotlin.math.roundToInt

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ReaderLayout(
    modifier: Modifier = Modifier,
    layoutState: ReaderState,
    spacing: Dp = 8.dp,
) {
    val scrollState by layoutState::positionsState
    val pages by scrollState.pages.collectAsState()
    val loadedPages by scrollState.loadedPages.collectAsState(emptyList(),Dispatchers.Main.immediate)
    val layoutParams by scrollState.layoutParams.collectAsState(Dispatchers.Main.immediate)

    val itemProvider = rememberPagesItemProvider(pages)
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
            .readerGestures(scrollState),
        itemProvider = { itemProvider }
    ) { constraints: Constraints ->
        val items = if (pages.isNotEmpty()) loadedPages.fastMap { //todo
            Pair(
                it, measure(
                    it.index,
                    constraints.copy(
                        minHeight = 0,
                        maxHeight = if (layoutParams.isVertical) Constraints.Infinity else constraints.maxHeight,
                        minWidth = 0
                    )
                )
            )
        } else emptyList()
        layout(width = constraints.maxWidth, height = constraints.maxHeight) {
            val newViewportSize = Size(
                constraints.maxWidth.toFloat(),
                constraints.maxHeight.toFloat()
            )
            scrollState.updateLayoutParams(
                spacing = spacing.toPx(),
                viewportSize = newViewportSize
            )
            items.fastForEach { (pos, placeables) ->
                val placeable = placeables.firstOrNull() ?: return@fastForEach
                if (layoutParams.isVertical)
                    verticalLayoutPage(
                        placeable = placeable,
                        pos = pos,
                        layoutState = scrollState,
                        params = layoutParams,
                        viewportSize = newViewportSize
                    )
                else horizontalLayoutPage(
                    placeable = placeable,
                    pos = pos,
                    layoutState = scrollState,
                    params = layoutParams,
                    viewportSize = newViewportSize
                )
            }
        }
    }
//    LaunchedEffect(key1 = Unit) {
//        snapshotFlow { layoutState.positionsState.viewportSize }
//            .filter { it.isSpecified }
//            .collectLatest { newSize ->
//                delay(300)
//                println("resize called")
//                layoutState.positionsState.pages.fastForEach { it.resizeBitmap(newSize) }
//            }
//    }
    DisposableEffect(Unit) {
        onDispose {
            layoutState.onDispose()
        }
    }
}

private fun Placeable.PlacementScope.verticalLayoutPage(
    placeable: Placeable,
    pos: PagePosition,
    layoutState: ReaderLayoutPositionState,
    params: LayoutParams,
    viewportSize: Size
) {
    if (params.fullHeight > viewportSize.height) {
        placeable.placeRelative(
            layoutState.offsetX.roundToInt(),
            (layoutState.offsetY + pos.start).roundToInt()
        )
    } else {
        val centeringOffsetY =
            ((viewportSize.height - params.fullHeight) / 2)
        placeable.placeRelative(
            layoutState.offsetX.roundToInt(),
            (layoutState.offsetY + pos.start + centeringOffsetY).roundToInt()
        )
    }
}

private fun Placeable.PlacementScope.horizontalLayoutPage(
    placeable: Placeable,
    pos: PagePosition,
    layoutState: ReaderLayoutPositionState,
    params: LayoutParams,
    viewportSize: Size
) {
    if (params.fullWidth > viewportSize.width) {
        val y = (viewportSize.height - placeable.height) / 2
        placeable.placeRelative(
            (layoutState.offsetX + pos.start).roundToInt(),
            y.roundToInt()
        )
    } else {
        val centeringOffsetY = ((viewportSize.height - placeable.height) / 2)
        val centeringOffsetX = ((viewportSize.width - params.fullWidth) / 2)
        placeable.placeRelative(
            (layoutState.offsetX + pos.start + centeringOffsetX).roundToInt(),
            centeringOffsetY.roundToInt()
        )
    }
}