package ru.marat.pdf_reader.layout.state

import androidx.compose.foundation.gestures.Orientation
import androidx.compose.runtime.Immutable
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.center
import androidx.compose.ui.geometry.isUnspecified
import androidx.compose.ui.geometry.toRect
import ru.marat.pdf_reader.gestures.Bounds
import ru.marat.viewplayground.pdf_reader.reader.layout.items.Page
import kotlin.compareTo
import kotlin.times

@Immutable
data class LayoutInfo(
    val viewportSize: Size = Size.Unspecified,
    val spacing: Float = 0F,
    val orientation: Orientation,
    val fullSize: Size = Size.Zero,
    val pages: List<Page> = emptyList(),
    val pagePositions: List<PagePosition> = emptyList(),
    val offset: Offset = Offset.Zero,
    val zoom: Float = 1f,
) {

    val fullHeight: Float get() = fullSize.height
    val fullWidth: Float get() = fullSize.width

    val offsetX get() = offset.x
    val offsetY get() = offset.y

    val isVertical get() = orientation == Orientation.Vertical

    val loadedPages: List<PagePosition> =
        if (viewportSize.isUnspecified) emptyList()
        else pagePositions.filter { // todo calculate with scale
            if (isVertical) {
                val r = (viewportSize.copy(
                    height = viewportSize.height * (1f / zoom)
                )).toRect().translate(0f, -offset.y)
                r.overlaps(Rect(0f, it.start, 1f, it.end))
            } else {
                val r = (viewportSize.copy(
                    width = viewportSize.width * (1f / zoom)
                )).toRect().translate(-offset.x, 0f)
                r.overlaps(Rect(it.start, 0f, it.end, 1f))
            }
        }

    val verticalBounds by lazy(mode = LazyThreadSafetyMode.PUBLICATION) {
        if (viewportSize.isUnspecified) return@lazy Bounds.Zero
        if (isVertical) {
            if (fullHeight > viewportSize.height) {
                val maxOffset = (fullHeight - (viewportSize.height * (1f / zoom))).coerceAtLeast(0f)
                Bounds(-maxOffset, 0f)
            } else {
                if ((viewportSize.height * (1f / zoom) > fullHeight)) {
                    val offset = (viewportSize.center.y * (1f / zoom)) - fullSize.center.y
                    Bounds(offset, offset)
                } else {
                    val maxOffset =
                        (fullHeight - (viewportSize.height * (1f / zoom))).coerceAtLeast(0f)
                    Bounds(-maxOffset, 0f)
                }
            }
        } else {
            if (loadedPages.isEmpty()) return@lazy Bounds(viewportSize.center.y)
            Bounds(
                (loadedPages.maxOf { it.size.height / 2 } - (viewportSize.center.y * (1f / zoom)))
                    .coerceAtLeast(0f)
            )
        }
    }

    val horizontalBounds by lazy(mode = LazyThreadSafetyMode.PUBLICATION) {
        if (viewportSize.isUnspecified) return@lazy Bounds.Zero
        if (isVertical) {
            Bounds((viewportSize.center.x - (viewportSize.center.x * (1f / zoom))).coerceAtLeast(0f))
        } else {
            if (fullWidth > viewportSize.width) {
                val maxOffset = (fullWidth - (viewportSize.width * (1f / zoom))).coerceAtLeast(0f)
                Bounds(-maxOffset, 0f)
            } else {
                if ((viewportSize.width * (1f / zoom) > fullWidth)) {
                    val offset = (viewportSize.center.x * (1f / zoom)) - fullSize.center.x
                    Bounds(offset, offset)
                } else {
                    val maxOffset =
                        (fullWidth - (viewportSize.width * (1f / zoom))).coerceAtLeast(0f)
                    Bounds(-maxOffset, 0f)
                }
            }

        }
    }
}