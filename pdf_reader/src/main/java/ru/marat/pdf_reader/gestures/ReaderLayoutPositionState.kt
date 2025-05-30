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
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.center
import androidx.compose.ui.geometry.isUnspecified
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.util.fastFirst
import androidx.compose.ui.util.fastFirstOrNull
import androidx.compose.ui.util.fastMap
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.updateAndGet
import kotlinx.coroutines.launch
import ru.marat.pdf_reader.layout.saver.ReaderLayoutPositionSaver
import ru.marat.pdf_reader.layout.state.LayoutInfo
import ru.marat.pdf_reader.layout.state.PagePosition
import ru.marat.pdf_reader.utils.Anchor
import ru.marat.pdf_reader.utils.createAnchor
import ru.marat.pdf_reader.utils.toIntRect
import ru.marat.viewplayground.pdf_reader.reader.layout.items.Page

@Stable
class ReaderLayoutPositionState internal constructor(
    density: Density,
    minZoom: Float,
    maxZoom: Float,
    private var anchor: Anchor?,
    orientation: Orientation = Orientation.Vertical,
) {

    private val scope = CoroutineScope(Dispatchers.Main.immediate)

    private var decayAnimationX: Job? = null
    private var decayAnimationY: Job? = null
    private val decayAnimSpec = SplineBasedFloatDecayAnimationSpec(density)
    private val zoomAnimSpec = spring<Float>(stiffness = 300f)
    private val velocityTracker = VelocityTracker()

    private val _layoutInfo = MutableStateFlow(
        LayoutInfo(
            orientation = orientation,
            userZoomBounds = Bounds(minZoom, maxZoom),
        )
    )
    val layoutInfo: StateFlow<LayoutInfo>
        get() = _layoutInfo


    internal fun onScroll(
        panChange: Offset,
        timeMillis: Long
    ): Boolean {
        val layoutInfo = _layoutInfo.value
        val panChange = panChange / layoutInfo.zoom
        val newOffsetX =
            (layoutInfo.offsetX + panChange.x).setBounds(layoutInfo.horizontalBounds)
        val newOffsetY =
            (layoutInfo.offsetY + panChange.y).setBounds(layoutInfo.verticalBounds)
        val canConsume = layoutInfo.offsetY != newOffsetY || layoutInfo.offsetX != newOffsetX
        return if (canConsume) {
            velocityTracker.addPosition(timeMillis, Offset(newOffsetX, newOffsetY))
            setOffset(newOffsetX, newOffsetY)
            true
        } else false
    }

    internal fun onZoom(
        zoomChange: Float,
        centroid: Offset
    ) {
        val layoutInfo = _layoutInfo.value
        val newScale = (layoutInfo.zoom * zoomChange).setBounds(layoutInfo.zoomBounds)
        val zoomOffset =
            calculateZoomOffset(
                layoutInfo.isVertical,
                layoutInfo.viewportSize,
                layoutInfo.zoom,
                newScale,
                centroid
            )
        val target = layoutInfo.copy(
            zoom = newScale,
        )
        val newOffset = (layoutInfo.offset + zoomOffset)
            .setOffsetBounds(target.horizontalBounds, target.verticalBounds)

        this._layoutInfo.update {
            target.copy(
                offset = newOffset
            )
        }
    }

    internal fun onDoubleTap(
        scope: CoroutineScope = this.scope,
        centroid: Offset
    ) {
        val currentState = _layoutInfo.value
        var currentScale = currentState.zoom
        scope.launch {
            val newScale = when {
                0.95f <= currentScale && currentScale < 2f -> currentScale + 1f
                currentScale in 2f..3f -> currentScale + 2f
                else -> 1f
            }.setBounds(currentState.zoomBounds)
            animate(
                initialValue = currentState.zoom,
                targetValue = newScale,
                animationSpec = zoomAnimSpec
            ) { value, _ ->
                val offset = calculateZoomOffset(
                    currentState.isVertical,
                    currentState.viewportSize,
                    currentScale,
                    value,
                    centroid
                )
                _layoutInfo.update {
                    it.copy(
                        zoom = value.setBounds(it.zoomBounds),
                        offset = (it.offset + offset).setOffsetBounds(
                            it.horizontalBounds,
                            it.verticalBounds
                        )
                    )
                }
                currentScale = value
            }
            _layoutInfo.value.clearScaledFragments()
            _layoutInfo.value.drawPagesFragments()
        }
    }

    internal fun onGestureStart() {
        cancelDecay()
    }

    internal suspend fun onGestureEnd() {
        coroutineScope {
            val velocity = velocityTracker.calculateVelocity()
            velocityTracker.resetTracking()
            val layout = _layoutInfo.value
            val initialValue = layout.offset
            decayAnimationY = launch {
                animateDecay(
                    initialValue = initialValue.y,
                    initialVelocity = velocity.y,
                    animationSpec = decayAnimSpec
                ) { value, _ ->
                    setOffset(null, value.setBounds(layout.verticalBounds))
                }
            }
            decayAnimationX = launch {
                animateDecay(
                    initialValue = initialValue.x,
                    initialVelocity = velocity.x,
                    animationSpec = decayAnimSpec
                ) { value, _ ->
                    setOffset(value.setBounds(layout.horizontalBounds), null)
                }
            }
            scope.launch {
                decayAnimationX?.join()
                decayAnimationY?.join()
                _layoutInfo.value.drawPagesFragments()
            }
        }
    }

    internal fun calculatePositions(
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
                    rect = IntRect(
                        top = start.toInt(),
                        bottom = end.toInt(),
                        left = 0,
                        right = viewportSize.width.toInt()
                    )
                )
                fullHeight += (pos.end - pos.start) + if (it.index == pages.lastIndex) 0f else spacing
                pos
            } else {
                var height = viewportSize.width * it.ratio
                var width = viewportSize.width
                if (height > viewportSize.height) {
                    val scale = (viewportSize.height / height)
                    height *= scale
                    width *= scale
                }
                val top = (viewportSize.height - height) / 2
                var pageSize =
                    Rect(
                        top = top,
                        left = fullWidth,
                        right = fullWidth + width,
                        bottom = top + height
                    )

                if (pageSize.height > fullHeight) fullHeight = pageSize.height
                val pos = PagePosition(
                    index = it.index,
                    start = fullWidth,
                    end = pageSize.right,
                    rect = pageSize.toIntRect()
                )
                fullWidth += (pos.end - pos.start) + if (it.index == pages.lastIndex) 0f else spacing
                pos
            }
        }
        return Size(fullWidth, fullHeight) to positions
    }

    internal fun updateViewportSize(
        spacing: Float,
        viewportSize: Size
    ) {
        val prevValue = _layoutInfo.value

        val sizeChanged = prevValue.viewportSize != viewportSize
        val spacingChanged = prevValue.spacing != spacing
        val needUpdate =
            anchor != null || spacingChanged || sizeChanged || prevValue.pagePositions.isEmpty()
        if (!needUpdate) return
        cancelDecay()
        if (sizeChanged) {
            prevValue.clearScaledFragments()
        }
        val (fullSize, positions) = calculatePositions(
            prevValue.pages,
            viewportSize,
            spacing,
            prevValue.isVertical
        )
        var targetValue = _layoutInfo.value.copy(
            spacing = spacing,
            viewportSize = viewportSize,
            fullSize = fullSize,
            pagePositions = positions,
        )
        _layoutInfo.updateAndGet {
            if (anchor != null && targetValue.pagePositions.isNotEmpty()) {
                val result = calculateNewOffsetWithAnchor(anchor!!, targetValue)
                anchor = null
                result
            } else {
                val anchor = createAnchor(prevValue)
                if (anchor != null) calculateNewOffsetWithAnchor(anchor, targetValue)
                else calculateNewOffset(prevValue, targetValue)
            }.coerceToBounds()
        }.also {
            if (sizeChanged) it.drawPagesFragments()
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
        val firstVisiblePageIndex = restoreData.pageIndex.coerceAtMost(targetValue.pages.lastIndex)
        val fvPage =
            targetValue.pagePositions.fastFirst { it.index == firstVisiblePageIndex }
        val newOffset = -fvPage.run { start + ((end - start) * restoreData.offsetFraction) }
        return targetValue.copy(
            offset =
                if (_layoutInfo.value.isVertical)
                    targetValue.offset.copy(
                        y = newOffset.setBounds(targetValue.verticalBounds),
                        x = targetValue.offsetX.setBounds(targetValue.horizontalBounds)
                    )
                else
                    targetValue.offset.copy(
                        x = newOffset.setBounds(targetValue.horizontalBounds),
                        y = targetValue.offsetY.setBounds(targetValue.verticalBounds)
                    )
        )
    }

    private fun setOffset(x: Float?, y: Float?): LayoutInfo {
        return _layoutInfo.updateAndGet {
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
            _layoutInfo.value.pagePositions.fastFirstOrNull { it.index == index } ?: return false
        _layoutInfo.update {
            it.copy(
                offset =
                    if (it.isVertical) it.offset.copy(y = -page.start)
                    else it.offset.copy(x = -page.start),
                zoom = 1f
            ).coerceToBounds()
        }
        return true
    }

    private fun calculateZoomOffset(
        isVertical: Boolean,
        viewportSize: Size,
        oldScale: Float,
        newScale: Float,
        centroid: Offset
    ): Offset {
        if (centroid.isUnspecified) return Offset.Zero
        val x: Float
        val y: Float
        if (isVertical) {
            x = (centroid.x - viewportSize.center.x) * ((1f / newScale) - (1f / oldScale))
            y = (centroid.y) * ((1f / newScale) - (1f / oldScale))
        } else {
            x = (centroid.x) * ((1f / newScale) - (1f / oldScale))
            y = (centroid.y - viewportSize.center.y) * ((1f / newScale) - (1f / oldScale))
        }
        return Offset(x = x, y = y)
    }
    internal fun setPages(pages: List<Page>) {
        _layoutInfo.update { it.copy(pages = pages) }
    }

    fun setOrientation(orientation: Orientation) {
        cancelDecay()
        _layoutInfo.update {
            if (it.orientation == orientation) return
            anchor = createAnchor(it)
            it.copy(orientation = orientation, zoom = 1f)
                .coerceToBounds()
                .also { it.clearScaledFragments() }
        }
    }

    fun getAnchor() = createAnchor(_layoutInfo.value)

    private fun cancelDecay() {
        decayAnimationX?.cancel()
        decayAnimationY?.cancel()
    }
}


@Composable
fun rememberReaderLayoutPositionState(
    anchor: Anchor,
    minZoom: Float,
    maxZoom: Float,
    vararg keys: Any?
): ReaderLayoutPositionState {
    val density = LocalDensity.current
    val k = remember(keys) { keys.toMutableList().apply { add(density) } }
    return rememberSaveable(
        inputs = k.toTypedArray(),
        saver = ReaderLayoutPositionSaver(
            minZoom,
            maxZoom,
            density
        )
    ) {
        ReaderLayoutPositionState(
            density,
            minZoom,
            maxZoom,
            anchor
        )
    }
}