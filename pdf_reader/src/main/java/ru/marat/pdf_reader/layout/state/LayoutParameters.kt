package ru.marat.pdf_reader.layout.state

import androidx.compose.foundation.gestures.Orientation
import androidx.compose.runtime.Immutable
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.center
import androidx.compose.ui.geometry.isUnspecified
import androidx.compose.ui.geometry.toRect
import androidx.compose.ui.util.fastFirst
import androidx.compose.ui.util.fastForEach
import ru.marat.pdf_reader.gestures.Bounds
import ru.marat.pdf_reader.gestures.setBounds
import ru.marat.pdf_reader.gestures.setOffsetBounds
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
) {

    companion object {
        const val MIN_ZOOM = 0.5F
        const val MAX_ZOOM = 100F
    }

    val fullHeight: Float get() = fullSize.height
    val fullWidth: Float get() = fullSize.width

    val offsetX get() = offset.x
    val offsetY get() = offset.y

    val isVertical get() = orientation == Orientation.Vertical

    val loadedPages: List<PagePosition> =
        if (viewportSize.isUnspecified) emptyList()
        else pagePositions.filter {
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

    fun drawPagesFragments() {
        if (loadedPages.isEmpty()) return
        val size = (Size(viewportSize.maxDimension, viewportSize.maxDimension) * (1f / zoom)) * 1.2f
        val vSize = (viewportSize * (1f / zoom))

        val layoutPosition = Rect(
            center = Offset(
                x = size.center.x,
                y = -(offsetY - size.center.y) - (size.center.y - vSize.center.y)
            ),
            radius = size.center.y
        )

        println("current pos ${layoutPosition.toStringg()} size: $size")
        loadedPages.fastForEach { pos ->
            val page = pages.fastFirst { it.index == pos.index }
            val fragment = if (isVertical) {
                val offset = (size.center.x - vSize.center.x).coerceAtMost(horizontalBounds.max)
                pos.size.toRect().translate(
                    0f,
                    pos.start
                ).intersect(layoutPosition)
                    .also { println("current pos page ${pos.index} ${it.toStringg()}") }
                    .translate(
                        -(offsetX - horizontalBounds.max) - offset,
                        -pos.start
                    ).also { if (it.width <= 0f || it.height <= 0f) return@fastForEach }
            } else return@fastForEach // todo horizontal
            page.drawPageFragment(zoom, fragment)
        }
    }

    fun Rect.toStringg() = "Rect(top=$top, bottom=$bottom, left=$left, right=$right)"

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

    val zoomBounds by lazy(mode = LazyThreadSafetyMode.PUBLICATION) {
        val minZoom = maxOf(
            MIN_ZOOM,
            (if (isVertical) viewportSize.height / fullHeight else viewportSize.width / fullWidth)
                .coerceAtMost(1f)
        )

        Bounds(minZoom, MAX_ZOOM)
    }

    fun coerceToBounds() = this.copy(
        zoom = zoom.setBounds(zoomBounds),
        offset = offset.setOffsetBounds(horizontalBounds, verticalBounds)
    )
}