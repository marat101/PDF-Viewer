package ru.marat.pdf_reader.gestures

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.splineBasedDecay
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.SaverScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import ru.marat.pdf_reader.layout.state.PagePosition

@Stable
class ReaderLayoutPositionState(
    density: Density,
    offsetFraction: Float,
    previousOffset: Offset? = null,
    prevFirstVisiblePageIndex: Int = 0,
) {
    internal var firstVisiblePageIndex = prevFirstVisiblePageIndex

    private val scope = CoroutineScope(Dispatchers.Main.immediate)

    private val decay = splineBasedDecay<Float>(density)

    private val _offsetY = Animatable((previousOffset ?: Offset.Zero).y)
    val offsetY get() = _offsetY.value

    private val _offsetX = Animatable((previousOffset ?: Offset.Zero).x)
    val offsetX get() = _offsetX.value

    var layoutHeight by mutableFloatStateOf(0f)
        private set

    var pagePositions by mutableStateOf<List<PagePosition>>(emptyList())
        internal set

    internal var scrollOffsetFraction by mutableFloatStateOf(offsetFraction)
        private set

    private val velocityTracker = VelocityTracker()

    fun onScroll(
        scope: CoroutineScope = this.scope,
        panChange: Offset,
        timeMillis: Long
    ): Boolean {
        val newOffsetX = 0f
        val newOffsetY = (offsetY + panChange.y).coerceIn(-layoutHeight, 0f)
        val canConsume = offsetY != newOffsetY
        return if (canConsume) {
            velocityTracker.addPosition(timeMillis, Offset(newOffsetX, newOffsetY))
            scope.launch {
                _offsetY.snapTo(newOffsetY)
                println("onOffsetChange $offsetY $layoutHeight")
                scrollOffsetFraction =
                    (offsetY / layoutHeight).run { if (isNaN()) 0f else this }//todo
                println("onOffsetChange scrollOffsetFraction $scrollOffsetFraction")
            }; true
        } else false
    }

    fun onGestureStart(
        scope: CoroutineScope = this.scope
    ) {
        scope.launch { _offsetY.stop() }
    }

    fun onGestureEnd(
        scope: CoroutineScope = this.scope
    ) {
        val velocity = velocityTracker.calculateVelocity()
        velocityTracker.resetTracking()
        scope.launch {
            println("onStopGestures velocity $velocity")
            _offsetY.updateBounds(
                lowerBound = -layoutHeight,
                upperBound = 0f
            )
            _offsetY.animateDecay(
                initialVelocity = velocity.y,
                animationSpec = decay
            )
        }
    }

    fun onPagesPositionsChanged(
        newLayoutHeight: Float,
    ) {
        layoutHeight = newLayoutHeight
        println("onPagesPositionsChanged layoutHeight $layoutHeight")
        scope.launch {
            _offsetY.snapTo(scrollOffsetFraction * layoutHeight)
            println("onPagesPositionsChanged:\noffsetY $offsetY\noffr $scrollOffsetFraction")
        }
    }

}

class ReaderLayoutPositionSaver(private val density: Density) :
    Saver<ReaderLayoutPositionState, List<Float>> {
    override fun restore(value: List<Float>): ReaderLayoutPositionState? {
        return ReaderLayoutPositionState(density, value[0], Offset(value[1], value[2]))
    }

    override fun SaverScope.save(value: ReaderLayoutPositionState): List<Float> {
        return listOf(value.scrollOffsetFraction, 0f, value.offsetY)
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