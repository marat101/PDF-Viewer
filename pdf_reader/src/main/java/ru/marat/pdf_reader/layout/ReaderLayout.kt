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
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastCoerceAtLeast
import androidx.compose.ui.util.fastCoerceAtMost
import androidx.compose.ui.util.fastForEach
import androidx.compose.ui.util.fastMap
import kotlinx.coroutines.Dispatchers
import ru.marat.pdf_reader.gestures.ReaderLayoutPositionState
import ru.marat.pdf_reader.gestures.readerGestures
import ru.marat.pdf_reader.layout.state.LayoutInfo
import ru.marat.pdf_reader.layout.state.PagePosition
import ru.marat.pdf_reader.layout.state.ReaderState
import kotlin.math.roundToInt

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ReaderLayout(
    modifier: Modifier = Modifier,
    layoutState: ReaderState,
    spacing: Dp = 8.dp,
    onTap: () -> Unit
) {
    val scrollState by layoutState::positionsState
    val layoutInfo by scrollState.layoutInfo.collectAsState(Dispatchers.Main)

    val itemProvider = rememberPagesItemProvider(layoutInfo.pages)
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
            .readerGestures(scrollState, onTap)
            .graphicsLayer {
                translationX = layoutInfo.offsetX * layoutInfo.zoom
                translationY = layoutInfo.offsetY * layoutInfo.zoom
                scaleX = layoutInfo.zoom
                scaleY = layoutInfo.zoom
                transformOrigin =
                    if (layoutInfo.isVertical) TransformOrigin(0.5f, 0f)
                    else TransformOrigin(0f, 0.5f)
            },
        itemProvider = { itemProvider }
    ) { constraints: Constraints ->
        var highestPage = 0f
        val items = if (layoutInfo.pages.isNotEmpty()) layoutInfo.loadedPages.fastMap { //todo
            Pair(
                it, measure(
                    it.index,
                    constraints.copy(
                        minHeight = 0,
                        maxHeight = if (layoutInfo.isVertical) Constraints.Infinity else constraints.maxHeight,
                        minWidth = 0
                    )
                )
            ).also {
                val height = it.second.maxOf { it.height }
                if (height > highestPage) highestPage = height.toFloat()
            }
        } else emptyList()
        layout(width = constraints.maxWidth, height = constraints.maxHeight) {
            val newViewportSize = Size(
                constraints.maxWidth.toFloat(),
                constraints.maxHeight.toFloat()
            )
            scrollState.updateViewportSize(
                spacing = spacing.toPx(),
                viewportSize = newViewportSize
            )
            items.fastForEach { (pos, placeables) ->
                val placeable = placeables.firstOrNull() ?: return@fastForEach
                if (layoutInfo.isVertical)
                    verticalLayoutPage(
                        placeable = placeable,
                        pos = pos,
                    )
                else horizontalLayoutPage(
                    placeable = placeable,
                    pos = pos,
                    viewportSize = newViewportSize
                )
            }
        }
    }
    DisposableEffect(Unit) {
        onDispose {
            layoutState.onDispose()
        }
    }
}

private fun Placeable.PlacementScope.verticalLayoutPage(
    placeable: Placeable,
    pos: PagePosition,
) {
    placeable.placeRelative(
        0,
        pos.start.roundToInt()
    )
}

private fun Placeable.PlacementScope.horizontalLayoutPage(
    placeable: Placeable,
    pos: PagePosition,
    viewportSize: Size
) {
    val y = (viewportSize.height - placeable.height) / 2
    placeable.placeRelative(
        pos.start.roundToInt(),
        y.roundToInt()
    )
}