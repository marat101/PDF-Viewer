package ru.marat.pdf_reader.gestures

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.SaverScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.util.VelocityTracker
import ru.marat.pdf_reader.layout.state.PagePosition

@Stable
class ReaderLayoutPositionState(
    offsetFraction: Float,
    previousOffset: Offset? = null,
    prevFirstVisiblePageIndex: Int = 0,
) {
    internal var firstVisiblePageIndex = prevFirstVisiblePageIndex

    var offset by mutableStateOf(previousOffset ?: Offset.Zero)
        private set

    var layoutHeight by mutableFloatStateOf(0f)
        private set

    var pagePositions by mutableStateOf<List<PagePosition>>(emptyList())
        internal set

    internal var scrollOffsetFraction by mutableFloatStateOf(offsetFraction)
        private set

    private val velocityTracker = VelocityTracker()

    fun onOffsetChange(
//        timeMillis: Long,
        offsetChange: Offset
    ) {
        offset = Offset(
            offset.x,
            (offset.y + offsetChange.y).coerceIn(-layoutHeight, 0f)
        )

        scrollOffsetFraction = offset.y / layoutHeight //todo
    }

    fun onPagesPositionsChanged(
        newLayoutHeight: Float,
    ) {
        layoutHeight = newLayoutHeight
        offset = Offset(0f, scrollOffsetFraction * layoutHeight)
    }

    fun onStopGestures() {
//        velocityTracker.resetTracking()
    }
}

class ReaderLayoutPositionSaver : Saver<ReaderLayoutPositionState, List<Float>> {
    override fun restore(value: List<Float>): ReaderLayoutPositionState? {
        return ReaderLayoutPositionState(value[0], Offset(value[1], value[2]))
    }

    override fun SaverScope.save(value: ReaderLayoutPositionState): List<Float> {
        return listOf(value.scrollOffsetFraction, value.offset.x, value.offset.y)
    }
}

@Composable
fun rememberReaderLayoutPositionState(vararg keys: Any?): ReaderLayoutPositionState {
    return rememberSaveable(
        inputs = keys,
        saver = ReaderLayoutPositionSaver()
    ) {
        ReaderLayoutPositionState(0f)
    }
}