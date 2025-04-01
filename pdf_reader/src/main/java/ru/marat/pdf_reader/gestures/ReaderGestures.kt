package ru.marat.pdf_reader.gestures

import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

internal fun Modifier.readerGestures(
    state: ReaderLayoutPositionState,
    onTap: () -> Unit
) = this.pointerInput(state) {
    coroutineScope {
        detectTransformGestures(
            cancelIfZoomCanceled = false,
            onGesture = { centroid, pan, zoom, timeMillis ->
                launch { state.onZoom(zoom, centroid) }
                launch { state.onScroll(pan, timeMillis) }
                true //todo
            },
            onGestureStart = {
                state.onGestureStart()
            },
            onGestureEnd = {
                launch { state.onGestureEnd() }
            },
            onDoubleTap = {
                state.onDoubleTap(this, it)
            },
            onTap = { onTap() }
        )
    }
}
