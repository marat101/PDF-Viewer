package ru.marat.pdf_reader.layout

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.lazy.layout.LazyLayout
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEach
import androidx.compose.ui.util.fastMap
import kotlinx.coroutines.Dispatchers
import ru.marat.pdf_reader.gestures.readerGestures
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
    val pages by remember { derivedStateOf { layoutInfo.pages } }
    val itemProvider = rememberPagesItemProvider(pages)
    LazyLayout(
        modifier = modifier
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
        val items = if (pages.isNotEmpty())
            layoutInfo.loadedPages.fastMap {
                Pair(
                    it, measure(
                        it.index,
                        constraints.copy(
                            minHeight = 0,
                            maxHeight = if (layoutInfo.isVertical) Constraints.Infinity else constraints.maxHeight,
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
            scrollState.updateViewportSize(
                spacing = (spacing.roundToPx() * (1f / layoutInfo.zoom)).coerceAtLeast(1f),
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
) {
    placeable.placeRelative(
        pos.start.roundToInt(),
        pos.rect.top.roundToInt()
    )
}