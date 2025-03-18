package ru.marat.pdf_reader.gestures

import android.graphics.Bitmap
import android.graphics.RecordingCanvas
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.splineBasedDecay
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.SaverScope
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
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import ru.marat.pdf_reader.layout.saver.ReaderLayoutPositionSaver
import ru.marat.pdf_reader.layout.state.LayoutParameters
import ru.marat.pdf_reader.layout.state.LayoutParams
import ru.marat.pdf_reader.layout.state.PagePosition
import ru.marat.viewplayground.pdf_reader.reader.layout.items.Page

@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
@Stable
class ReaderLayoutPositionState(
    density: Density,
    offsetFraction: Float = 0f,
    previousOffset: Offset? = null,
    orientation: Orientation = Orientation.Vertical,
) : LayoutParameters(orientation) {

    private val scope = CoroutineScope(Dispatchers.Main.immediate)

    private val decay = splineBasedDecay<Float>(density)

    // Pages
    var pages = MutableStateFlow<List<Page>>(emptyList())

    override var pagePositions: StateFlow<List<PagePosition>> =
        layoutParams.combine(pages) { params, pages ->
            calculatePositions(pages, params)
        }.stateIn(scope, SharingStarted.Lazily, emptyList())

    val loadedPages =
        snapshotFlow {
            Offset(offsetX, offsetY)
        }.combine(pagePositions) { offset, pagePositions ->
            val layoutParams = layoutParams.value
            val viewportSize = layoutParams.viewportSize
            if (viewportSize.isUnspecified) return@combine emptyList()
            pagePositions.filter {
                if (layoutParams.orientation == Orientation.Vertical) {
                    val r = viewportSize.toRect()
                        .translate(0f, -offset.y)
                    r.overlaps(Rect(0f, it.start, 1f, it.end))
                } else {
                    val r = viewportSize.toRect()
                        .translate(-offset.x, 0f)
                    r.overlaps(Rect(it.start, 0f, it.end, 1f))
                }
            }.apply {
                println(this.map { it.index to it.size })
            }
        }


    // Gestures params
    private val _offsetY = Animatable((previousOffset ?: Offset.Zero).y)
    val offsetY get() = _offsetY.value

    private val _offsetX = Animatable((previousOffset ?: Offset.Zero).x)
    val offsetX get() = _offsetX.value


    private val velocityTracker = VelocityTracker()

    fun onScroll(
        scope: CoroutineScope = this.scope,
        panChange: Offset,
        timeMillis: Long
    ): Boolean {
        val newOffsetX =
            (offsetX + panChange.x).coerceIn(-layoutParams.value.calculateMaxOffsetX(), 0f)
        val newOffsetY =
            (offsetY + panChange.y).coerceIn(-layoutParams.value.calculateMaxOffsetY(), 0f)
        val canConsume = true
//            offsetY != newOffsetY || offsetX != newOffsetX
        println("canConsume: $canConsume")
        return if (canConsume) {
            velocityTracker.addPosition(timeMillis, Offset(newOffsetX, newOffsetY))
            scope.launch {
                println("onScroll: $newOffsetX, $newOffsetY")
                _offsetX.snapTo(newOffsetX)
                _offsetY.snapTo(newOffsetY)
            }; true
        } else false
    }

    fun onGestureStart(
        scope: CoroutineScope = this.scope
    ) {
        scope.launch {
            _offsetY.stop()
            _offsetX.stop()
        }
    }

    fun onGestureEnd(
        scope: CoroutineScope = this.scope
    ) {
        val velocity = velocityTracker.calculateVelocity()
        velocityTracker.resetTracking()
        scope.launch {
            _offsetY.updateBounds(
                lowerBound = -layoutParams.value.calculateMaxOffsetY(),
                upperBound = 0f
            )
            _offsetY.animateDecay(
                initialVelocity = velocity.y,
                animationSpec = decay
            )
        }
        scope.launch {
            _offsetX.updateBounds(
                lowerBound = -layoutParams.value.calculateMaxOffsetX(),
                upperBound = 0f
            )
            _offsetX.animateDecay(
                initialVelocity = velocity.x,
                animationSpec = decay
            )
        }
    }

    override fun calculatePositions(pages: List<Page>, params: LayoutParams): List<PagePosition> {
        val positions = mutableListOf<PagePosition>()
        var layoutHeight = 0f
        var layoutWidth = 0f
        pages.fastForEach {
            if (params.orientation == Orientation.Vertical) {
                val start = layoutHeight
                val end = (params.viewportSize.width * it.ratio) + layoutHeight
                val pos = PagePosition(
                    index = it.index,
                    start = start,
                    end = end,
                    size = Size(params.viewportSize.width, end - start)
                )
                layoutHeight += (pos.end - pos.start) + if (it.index == pages.lastIndex) 0f else params.spacing
                positions.add(pos)
            } else {
                var pageSize = Size(params.viewportSize.width, params.viewportSize.width * it.ratio)
                if (pageSize.height > params.viewportSize.height) {
                    pageSize *= (params.viewportSize.height / pageSize.height)
                }

//                val scale = min(
//                    viewportSize.width / it.rect.value.size.width,
//                    viewportSize.height / it.rect.value.size.height
//                )
//                val pageSize = Size(
//                    it.rect.value.size.width,
//                    it.rect.value.size.height
//                ) * scale

                if (pageSize.height > layoutHeight) layoutHeight = pageSize.height
                val pos = PagePosition(
                    index = it.index,
                    start = layoutWidth,
                    end = pageSize.width + layoutWidth,
                    size = pageSize
                )
                layoutWidth += (pos.end - pos.start) + if (it.index == pages.lastIndex) 0f else params.spacing
                positions.add(pos)
            }
        }
        updateLayoutParams(fullSize = Size(layoutWidth, layoutHeight))
        return positions
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