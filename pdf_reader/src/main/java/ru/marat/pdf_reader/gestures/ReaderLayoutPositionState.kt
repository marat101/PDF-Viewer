package ru.marat.pdf_reader.gestures

import androidx.compose.animation.SplineBasedFloatDecayAnimationSpec
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.animateDecay
import androidx.compose.animation.core.spring
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.util.fastFirst
import androidx.compose.ui.util.fastFirstOrNull
import androidx.compose.ui.util.fastMap
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.updateAndGet
import kotlinx.coroutines.launch
import ru.marat.pdf_reader.gestures.setBounds
import ru.marat.pdf_reader.layout.saver.ReaderLayoutPositionSaver
import ru.marat.pdf_reader.layout.state.LayoutInfo
import ru.marat.pdf_reader.layout.state.PagePosition
import ru.marat.pdf_reader.utils.Anchor
import ru.marat.pdf_reader.utils.createAnchor
import ru.marat.viewplayground.pdf_reader.reader.layout.items.Page

@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
@Stable
class ReaderLayoutPositionState(
    private val density: Density,
    private var anchor: Anchor?,
    orientation: Orientation = Orientation.Vertical,
) {

    private val scope = CoroutineScope(Dispatchers.Main.immediate)

    private var decayAnimationX: Job? = null
    private var decayAnimationY: Job? = null
    private val decayAnimSpec = SplineBasedFloatDecayAnimationSpec(density)
    private val zoomAnimSpec = spring<Float>(stiffness = 300f)

    val layoutInfo = MutableStateFlow(
        LayoutInfo(
            orientation = orientation,
        )
    )

    private val velocityTracker = VelocityTracker()

    fun onScroll(
        scope: CoroutineScope = this.scope,
        panChange: Offset,
        timeMillis: Long
    ): Boolean {
        val layoutInfo = layoutInfo.value
        val panChange = panChange / layoutInfo.zoom
        val newOffsetX =
            (layoutInfo.offsetX + panChange.x).setBounds(layoutInfo.horizontalBounds)
        val newOffsetY =
            (layoutInfo.offsetY + panChange.y).setBounds(layoutInfo.verticalBounds)
        val canConsume = layoutInfo.offsetY != newOffsetY || layoutInfo.offsetX != newOffsetX
        println("canConsume: $canConsume")
        return if (canConsume) {
            velocityTracker.addPosition(timeMillis, Offset(newOffsetX, newOffsetY))
            println("onScroll: $newOffsetX, $newOffsetY")
            setOffset(newOffsetX, newOffsetY)
            true
        } else false
    }

    fun onZoom(
        scope: CoroutineScope = this.scope,
        zoomChange: Float,
        centroid: Offset,
        panChange: Offset
    ) {
        val layoutInfo = layoutInfo.value
        val target = layoutInfo.copy(
            zoom = layoutInfo.zoom * zoomChange,
        )
        this.layoutInfo.update {
            target.copy(
                offset = target.offset.copy(
                    x = target.offsetX.setBounds(target.horizontalBounds),
                    y = target.offsetY.setBounds(target.verticalBounds)
                )
            )
        }
    }

    fun onDoubleTap(
        scope: CoroutineScope = this.scope,
        centroid: Offset
    ) {
        val currentScale = layoutInfo.value
        scope.launch {
            animate(
                initialValue = currentScale.zoom,
                targetValue = 1f,
                animationSpec = zoomAnimSpec
            ) { value, _ ->
                layoutInfo.update {
                    it.copy(
                        zoom = value
                    )
                }
            }
        }

    }

    fun onGestureStart(
        scope: CoroutineScope = this.scope
    ) {
        cancelDecay()
    }

    fun onGestureEnd(
        scope: CoroutineScope = this.scope
    ) {
        val velocity = velocityTracker.calculateVelocity()
        velocityTracker.resetTracking()
        val layout = layoutInfo.value
        val initialValue = layout.offset
        decayAnimationY = scope.launch {
            animateDecay(
                initialValue = initialValue.y,
                initialVelocity = velocity.y,
                animationSpec = decayAnimSpec
            ) { value, _ ->
                setOffset(null, value.setBounds(layout.verticalBounds))
            }
        }
        decayAnimationX = scope.launch {
            animateDecay(
                initialValue = initialValue.x,
                initialVelocity = velocity.x,
                animationSpec = decayAnimSpec
            ) { value, _ ->
                setOffset(value.setBounds(layout.horizontalBounds), null)
            }
        }
    }

    fun calculatePositions(
        pages: List<Page>,
        viewportSize: Size,
        spacing: Float,
        isVertical: Boolean
    ): Pair<Size, List<PagePosition>> {
        var fullHeight = 0f
        var fullWidth = 0f
        val positions = pages.fastMap {
            if (isVertical) {
                val start = fullHeight
                val end = (viewportSize.width * it.ratio) + fullHeight
                val pos = PagePosition(
                    index = it.index,
                    start = start,
                    end = end,
                    size = Size(viewportSize.width, end - start)
                )
                fullHeight += (pos.end - pos.start) + if (it.index == pages.lastIndex) 0f else spacing
                pos
            } else {
                var pageSize =
                    Size(viewportSize.width, viewportSize.width * it.ratio)
                if (pageSize.height > viewportSize.height)
                    pageSize *= (viewportSize.height / pageSize.height)

                if (pageSize.height > fullHeight) fullHeight = pageSize.height
                val pos = PagePosition(
                    index = it.index,
                    start = fullWidth,
                    end = pageSize.width + fullWidth,
                    size = pageSize
                )
                fullWidth += (pos.end - pos.start) + if (it.index == pages.lastIndex) 0f else spacing
                pos
            }
        }
        return Size(fullWidth, fullHeight) to positions
    }

    fun updateViewportSize(
        spacing: Float,
        viewportSize: Size
    ): LayoutInfo {
        val prevValue = layoutInfo.value

        val needUpdate =
            anchor != null || prevValue.spacing != spacing || prevValue.viewportSize != viewportSize || prevValue.pagePositions.isEmpty()
        if (!needUpdate) return prevValue
        cancelDecay()
        val (fullSize, positions) = calculatePositions(
            prevValue.pages,
            viewportSize,
            spacing,
            prevValue.isVertical
        )
        var targetValue = layoutInfo.value.copy(
            spacing = spacing,
            viewportSize = viewportSize,
            fullSize = fullSize,
            pagePositions = positions,
        )

        return layoutInfo.updateAndGet {
            if (anchor != null && targetValue.pagePositions.isNotEmpty()) {
                val result = calculateNewOffsetWithAnchor(anchor!!, targetValue)
                anchor = null
                result
            } else {
                val anchor = createAnchor(prevValue)
                if (anchor != null) calculateNewOffsetWithAnchor(anchor, targetValue)
                else calculateNewOffset(prevValue, targetValue)
            }
        }
    }

    private fun calculateNewOffset(prevValue: LayoutInfo, targetValue: LayoutInfo): LayoutInfo {
        return if (!prevValue.isVertical) {
            val newValue =
                prevValue.offsetX * (targetValue.fullWidth / prevValue.fullWidth)
            if (newValue.isNaN()) return targetValue
            targetValue.copy(
                offset = targetValue.offset.copy(
                    x = newValue.setBounds(targetValue.horizontalBounds),
                    y = targetValue.offsetY.setBounds(targetValue.verticalBounds)
                )
            )
        } else {
            targetValue.copy(
                offset = targetValue.offset.copy(
                    x = targetValue.offsetX.setBounds(targetValue.horizontalBounds),
                    y = targetValue.offsetY.setBounds(targetValue.verticalBounds)
                )
            )
        }
    }

    internal fun calculateNewOffsetWithAnchor(
        restoreData: Anchor,
        targetValue: LayoutInfo
    ): LayoutInfo {
        val fvPage =
            targetValue.pagePositions.fastFirst { it.index == restoreData.previousFirstVisiblePage }
        val newOffset = -fvPage.run { start + ((end - start) * restoreData.fraction) }
        return targetValue.copy(
            offset =
                if (layoutInfo.value.isVertical)
                    targetValue.offset.copy(
                        y = newOffset.setBounds(targetValue.verticalBounds),
                        x = targetValue.offsetX.setBounds(targetValue.horizontalBounds)
                    )
                else targetValue.offset.copy(
                    x = newOffset.setBounds(targetValue.horizontalBounds),
                    y = targetValue.offsetY.setBounds(targetValue.verticalBounds)
                )
        )
    }

    private fun setOffset(x: Float?, y: Float?): LayoutInfo {
        return layoutInfo.updateAndGet {
            it.copy(
                offset = it.offset.copy(
                    x = x ?: it.offsetX,
                    y = y ?: it.offsetY
                )
            )
        }
    }

    fun scrollToPage(index: Int): Boolean {
        val page =
            layoutInfo.value.pagePositions.fastFirstOrNull { it.index == index } ?: return false
        layoutInfo.update {
            it.copy(
                offset =
                    if (it.isVertical) it.offset.copy(y = -page.start)
                    else it.offset.copy(x = -page.start)
            )
        }
        return true
    }

    fun setOrientation(orientation: Orientation) {
        cancelDecay()
        layoutInfo.update {
            if (it.orientation == orientation) return
            anchor = createAnchor(it)
            it.copy(orientation = orientation)
        }
    }

    private fun cancelDecay() {
        decayAnimationX?.cancel()
        decayAnimationY?.cancel()
    }
}


@Composable
fun rememberReaderLayoutPositionState(vararg keys: Any?): ReaderLayoutPositionState {
    val density = LocalDensity.current
    val k = remember(keys) { keys.toMutableList().apply { add(density) } }
    return rememberSaveable(
        inputs = k.toTypedArray(),
        saver = ReaderLayoutPositionSaver(density)
    ) {
        ReaderLayoutPositionState(density, null)
    }
}