package ru.marat.pdf_reader.layout.state

import androidx.compose.foundation.gestures.Orientation
import androidx.compose.runtime.Immutable
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.center
import androidx.compose.ui.geometry.isUnspecified
import androidx.compose.ui.geometry.toRect
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.toSize
import androidx.compose.ui.util.fastFirst
import androidx.compose.ui.util.fastForEach
import ru.marat.pdf_reader.gestures.Bounds
import ru.marat.pdf_reader.gestures.setBounds
import ru.marat.pdf_reader.gestures.setOffsetBounds
import ru.marat.pdf_reader.utils.toIntRect
import ru.marat.pdf_reader.utils.toIntSize
import ru.marat.viewplayground.pdf_reader.reader.layout.items.Page

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
    private val userZoomBounds: Bounds = Bounds(MIN_ZOOM, MAX_ZOOM),
) {

    companion object {
        internal const val MIN_ZOOM = 0.5F
        internal const val MAX_ZOOM = 100F
    }

    val fullHeight: Float get() = fullSize.height
    val fullWidth: Float get() = fullSize.width

    val offsetX get() = offset.x
    val offsetY get() = offset.y

    val isVertical get() = orientation == Orientation.Vertical

    val loadedPages: List<PagePosition> =
        if (viewportSize.isUnspecified) emptyList()
        else {
            val size =
                if (isVertical) viewportSize.height * (1f / zoom) else viewportSize.width * (1f / zoom)
            pagePositions.filter {
                if (isVertical) {
                    val r = (viewportSize.copy(height = size))
                        .toRect()
                        .inflate(size * 0.3f)
                        .translate(0f, -offset.y)
                    r.overlaps(Rect(0f, it.start, 1f, it.end))
                } else {
                    val r = (viewportSize.copy(width = size))
                        .toRect()
                        .inflate(size * 0.3f)
                        .translate(-offset.x, 0f)
                    r.overlaps(Rect(it.start, 0f, it.end, 1f))
                }
            }
        }

    val visiblePages: List<PagePosition> =
        if (viewportSize.isUnspecified) emptyList()
        else loadedPages.filter {
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


    internal fun drawPagesFragments() {
        if (visiblePages.isEmpty()) return
        val scaledViewportSize = (viewportSize * (1f / zoom)).toIntSize()
        val layoutPosition = getLayoutPosition(scaledViewportSize.toSize()).toIntRect()
        visiblePages.fastForEach { pos ->
            val fragment = if (isVertical)
                pos.getVerticalLayoutFragment(layoutPosition)
            else {
                pos.getHorizontalLayoutFragment(layoutPosition)
            } ?: return@fastForEach
            val page = pages.fastFirst { it.index == pos.index }
            page.setVisibleFragment(IntRect(fragment.topLeft, fragment.size))
        }
    }

    private fun getLayoutPosition(
        scaledViewportSize: Size = viewportSize * (1f / zoom)
    ): Rect {
        val vSize = scaledViewportSize
        val drawDistance = ((viewportSize.maxDimension * 1.15f) * (1f / zoom))
        return if (isVertical)
            Rect(
                center = Offset(
                    x = (-offsetX + horizontalBounds.max) + vSize.center.x,
                    y = -(offsetY - drawDistance) - (drawDistance - vSize.center.y)
                ),
                radius = (drawDistance / 2)
            )
        else {
            Rect(
                center = Offset(
                    y = -offsetY + viewportSize.center.y,
                    x = -(offsetX - drawDistance) - (drawDistance - vSize.center.x)
                ),
                radius = (drawDistance / 2)
            )
        }
    }


    private fun PagePosition.getVerticalLayoutFragment(
        layoutPosition: IntRect
    ): IntRect? {
        return rect
            .intersect(layoutPosition)
            .translate(0, -rect.top)
    }

    private fun PagePosition.getHorizontalLayoutFragment(
        layoutPosition: IntRect
    ): IntRect? {
        return rect
            .intersect(layoutPosition)
            .translate(-rect.left, -rect.top)
    }

    internal fun clearScaledFragments() {
        pages.forEach { it.removeScale() }
    }

    val verticalBounds by lazy(mode = LazyThreadSafetyMode.PUBLICATION) {
        if (viewportSize.isUnspecified) return@lazy Bounds.Zero
        if (isVertical) {
            if (fullHeight > viewportSize.height) {
                val maxOffset =
                    (fullHeight - (viewportSize.height * (1f / zoom))).coerceAtLeast(0f)
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
            if (visiblePages.isEmpty()) return@lazy Bounds(viewportSize.center.y)
            Bounds(
                (visiblePages.maxOf { it.rect.height / 2 } - (viewportSize.center.y * (1f / zoom)))
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

    val zoomBounds by lazy(mode = LazyThreadSafetyMode.PUBLICATION) {
        if (viewportSize.isUnspecified) return@lazy Bounds(1f, 1f)
        val minZoom = maxOf(
            userZoomBounds.min,
            (if (isVertical) viewportSize.height / fullHeight else viewportSize.width / fullWidth)
                .coerceAtMost(1f)
        )

        Bounds(minZoom, userZoomBounds.max)
    }

    internal fun coerceToBounds() = this.copy(
        zoom = zoom.setBounds(zoomBounds),
        offset = offset.setOffsetBounds(horizontalBounds, verticalBounds)
    )
}