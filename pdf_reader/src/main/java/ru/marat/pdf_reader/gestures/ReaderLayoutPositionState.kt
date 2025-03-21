package ru.marat.pdf_reader.gestures

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.splineBasedDecay
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.isUnspecified
import androidx.compose.ui.geometry.toRect
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.util.fastForEach
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.updateAndGet
import kotlinx.coroutines.launch
import ru.marat.pdf_reader.layout.saver.ReaderLayoutPositionSaver
import ru.marat.pdf_reader.layout.state.LayoutInfo
import ru.marat.pdf_reader.layout.state.PagePosition

@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
@Stable
class ReaderLayoutPositionState(
    //todo
    density: Density,
    offsetFraction: Float = 0f,
    previousOffset: Offset? = null,
    orientation: Orientation = Orientation.Vertical,
) {

    private val scope = CoroutineScope(Dispatchers.Main.immediate)

    private val decay = splineBasedDecay<Float>(density)


    val layoutInfo = MutableStateFlow(
        LayoutInfo(
            orientation = orientation,
        )
    )

    internal fun updateLayoutParams(
        viewportSize: Size = layoutInfo.value.viewportSize,
        spacing: Float = layoutInfo.value.spacing,
        fullSize: Size = layoutInfo.value.fullSize,
        orientation: Orientation = layoutInfo.value.orientation
    ): LayoutInfo = layoutInfo.updateAndGet {
        it.copy(
            spacing = spacing,
            viewportSize = viewportSize,
            fullSize = fullSize,
            orientation = orientation
        )
    }

//    var pagePositions: StateFlow<List<PagePosition>> = layoutInfo
//        .combine(pages) { params, pages -> calculatePositions(pages, params) }
//        .flowOn(Dispatchers.Main.immediate)
//        .stateIn(scope, SharingStarted.Lazily, emptyList())

    val loadedPages = snapshotFlow { Offset(offsetX, offsetY) }
        .combine(layoutInfo) { offset, layoutInfo ->
            val viewportSize = layoutInfo.viewportSize
            if (viewportSize.isUnspecified) return@combine emptyList()
            layoutInfo.pagePositions.filter {
                if (layoutInfo.isVertical) {
                    val r = viewportSize.toRect().translate(0f, -offset.y)
                    r.overlaps(Rect(0f, it.start, 1f, it.end))
                } else {
                    val r = viewportSize.toRect().translate(-offset.x, 0f)
                    r.overlaps(Rect(it.start, 0f, it.end, 1f))
                }
            }
        }.flowOn(Dispatchers.Main.immediate)
        .stateIn(scope, SharingStarted.Lazily, emptyList())


    // Gestures params
    private val _offsetY = Animatable((previousOffset ?: Offset.Zero).y)
    val offsetY get() = _offsetY.value

    private val _offsetX = mutableFloatStateOf(0f)

    //        Animatable((previousOffset ?: Offset.Zero).x)
    val offsetX get() = _offsetX.floatValue


    private val velocityTracker = VelocityTracker()

    fun onScroll(
        scope: CoroutineScope = this.scope,
        panChange: Offset,
        timeMillis: Long
    ): Boolean { //todo wtf
        val newOffsetX =
            (offsetX + panChange.x).coerceIn(-layoutInfo.value.calculateMaxOffsetX(), 0f)
        val newOffsetY =
            (offsetY + panChange.y).coerceIn(-layoutInfo.value.calculateMaxOffsetY(), 0f)
        val canConsume = offsetY != newOffsetY || offsetX != newOffsetX
        println("canConsume: $canConsume")
        return if (canConsume) {
            velocityTracker.addPosition(timeMillis, Offset(newOffsetX, newOffsetY))
            scope.launch {
                println("onScroll: $newOffsetX, $newOffsetY")
//                _offsetX.snapTo(newOffsetX) todo animatable
                _offsetX.floatValue = newOffsetX
                _offsetY.snapTo(newOffsetY)
            }; true
        } else false
    }

    fun onGestureStart(
        scope: CoroutineScope = this.scope
    ) {
        scope.launch {
            _offsetY.stop()
//            _offsetX.stop() todo animatable
        }
    }

    fun onGestureEnd(
        scope: CoroutineScope = this.scope
    ) {
        val velocity = velocityTracker.calculateVelocity()
        velocityTracker.resetTracking()
        scope.launch {
            _offsetY.updateBounds(
                lowerBound = -layoutInfo.value.calculateMaxOffsetY(),
                upperBound = 0f
            )
            _offsetY.animateDecay(
                initialVelocity = velocity.y,
                animationSpec = decay
            )
        }
        scope.launch {
//            _offsetX.updateBounds(
//                lowerBound = -layoutInfo.value.calculateMaxOffsetX(),
//                upperBound = 0f todo animatable
//            )
//            _offsetX.animateDecay( todo animatable
//                initialVelocity = velocity.x,
//                animationSpec = decay
//            )
        }
    }

    fun calculatePositions(
        params: LayoutInfo
    ): Pair<Size, List<PagePosition>> {
        val pages = params.pages
        val positions = mutableListOf<PagePosition>()
        var fullHeight = 0f
        var fullWidth = 0f
        pages.fastForEach {
            if (params.isVertical) {
                val start = fullHeight
                val end = (params.viewportSize.width * it.ratio) + fullHeight
                val pos = PagePosition(
                    index = it.index,
                    start = start,
                    end = end,
                    size = Size(params.viewportSize.width, end - start)
                )
                fullHeight += (pos.end - pos.start) + if (it.index == pages.lastIndex) 0f else params.spacing
                positions.add(pos)
            } else {
                var pageSize =
                    Size(params.viewportSize.width, params.viewportSize.width * it.ratio)
                if (pageSize.height > params.viewportSize.height)
                    pageSize *= (params.viewportSize.height / pageSize.height)

                if (pageSize.height > fullHeight) fullHeight = pageSize.height
                val pos = PagePosition(
                    index = it.index,
                    start = fullWidth,
                    end = pageSize.width + fullWidth,
                    size = pageSize
                )
                fullWidth += (pos.end - pos.start) + if (it.index == pages.lastIndex) 0f else params.spacing
                positions.add(pos)
            }
        }
//        val newParams =
//            updateLayoutParams(fullSize = Size(fullWidth, fullHeight))
//        if (!params.isVertical) {
//            scope.launch(Dispatchers.Main.immediate) {
//                val newValue =
//                    _offsetX.value * (newParams.fullSize.width / params.fullSize.width)
//                if (!newValue.isNaN()) _offsetX.snapTo(
//                    newValue.coerceIn(
//                        -newParams.calculateMaxOffsetX(),
//                        0f
//                    )
//                )
//            }
//        }
        return Size(fullWidth, fullHeight) to positions
    }

    fun updateViewportSize(
        spacing: Float,
        viewportSize: Size
    ): LayoutInfo {
        val n = layoutInfo.value.copy(
            spacing = spacing,
            viewportSize = viewportSize
        )
        val (fullSize, positions) = calculatePositions(n)
        val newLayoutInfo = n.copy(
            fullSize = fullSize,
            pagePositions = positions,
        )
        if (!n.isVertical) {
            val newValue = _offsetX.floatValue *
            (layoutInfo.value.run { viewportSize.width / fullWidth } / newLayoutInfo.run { viewportSize.width / fullWidth })
//                _offsetX.floatValue * (newLayoutInfo.fullSize.width / n.fullSize.width)
            if (!newValue.isNaN()) _offsetX.floatValue =
                newValue.coerceIn(
                    -newLayoutInfo.calculateMaxOffsetX(),
                    0f
                )

        }
        return layoutInfo.updateAndGet {
            newLayoutInfo
//            n.copy(
//                fullSize = fullSize,
//                pagePositions = positions,
//            )
        }
    }

    fun setOrientation(orientation: Orientation) {
        updateLayoutParams(orientation = orientation)
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
        ReaderLayoutPositionState(density, 0f)
    }
}