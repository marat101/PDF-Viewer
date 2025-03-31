package ru.marat.pdf_reader.layout.state

import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.center
import androidx.compose.ui.geometry.isUnspecified
import androidx.compose.ui.geometry.toRect
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.tooling.preview.Preview
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
        }.also { println("loaded pages $it") }

    fun drawPagesFragments() {
        if (loadedPages.isEmpty()) return
        val scaledViewportSize = viewportSize * (1f / zoom)
        val layoutPosition = getLayoutPosition(scaledViewportSize)
        println("current pos ${layoutPosition.toStringg()} size: ${layoutPosition.size}")
        loadedPages.fastForEach { pos ->
            val fragment = if (isVertical)
                pos.getVerticalLayoutFragment(layoutPosition)
            else {
                pos.getHorizontalLayoutFragment(layoutPosition)
            } ?: return@fastForEach
            val page = pages.fastFirst { it.index == pos.index }
            page.setVisibleFragment(fragment)
        }
    }

    fun getLayoutPosition(
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
        layoutPosition: Rect
    ): Rect? {
        return rect
            .intersect(layoutPosition)
            .translate(0f, -rect.top)
            .also {
                println("page $index visible rect ${it.toStringg()}")
            }
    }

    private fun PagePosition.getHorizontalLayoutFragment(
        layoutPosition: Rect
    ): Rect? {
        return rect
            .intersect(layoutPosition)
            .translate(-rect.left, -rect.top)
            .also {
                println("page $index visible rect ${it.toStringg()}")
            }
    }

    fun clearScaledFragments() {
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
            if (loadedPages.isEmpty()) return@lazy Bounds(viewportSize.center.y)
            Bounds(
                (loadedPages.maxOf { it.rect.height / 2 } - (viewportSize.center.y * (1f / zoom)))
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

@Preview(
    heightDp = 3000,
    widthDp = 3000
)
@Composable
fun Test() {

    val pages = listOf(
        Rect(top = 0.0f, bottom = 5138.823f, left = 0.0f, right = 1040.0f),
        Rect(top = 5150.823f, bottom = 5888.368f, left = 0.0f, right = 1040.0f),
        Rect(top = 5900.368f, bottom = 6637.913f, left = 0.0f, right = 1040.0f),
        Rect(top = 6649.913f, bottom = 7387.458f, left = 0.0f, right = 1040.0f)
    )
    val layoutPosition =
        Rect(top = 4684.5996f, bottom = 7531.5996f, left = -903.5f, right = 1943.5f)
    androidx.compose.foundation.Canvas(
        modifier = Modifier.fillMaxSize()
    ) {
        drawContext.canvas.also {
            it.scale(0.55f, 0.55f)
            val checkPoint =
                it.nativeCanvas.saveLayer(null, Paint().apply {
//                    blendMode = BlendMode.DstIn
                }.asFrameworkPaint())
            pages.forEach { rect ->
                it.drawRect(
                    paint = Paint().apply { color = Color.Yellow },
                    rect = rect
                )
            }

            it.nativeCanvas.restoreToCount(checkPoint)


            it.drawRect(
                paint = Paint().apply {
                    blendMode = BlendMode.DstIn
                    color = Color.Blue; alpha = 0.5f
                },
                rect = layoutPosition
            )
        }
    }
}

fun Rect.toStringg() = "Rect(top=${top}f, bottom=${bottom}f, left=${left}f, right=${right}f)"