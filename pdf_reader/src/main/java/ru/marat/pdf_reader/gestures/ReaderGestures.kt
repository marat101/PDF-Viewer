package ru.marat.pdf_reader.gestures

import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import kotlinx.coroutines.coroutineScope

fun Modifier.readerGestures(
    state: ReaderLayoutPositionState,
    onTap: () -> Unit
) = this.pointerInput(state) {
    coroutineScope {
        detectTransformGestures(
            cancelIfZoomCanceled = false,
            onGesture = { centroid, pan, zoom, timeMillis ->
                if (zoom != 1f) state.onZoom(this, zoom, centroid, pan)
                else state.onScroll(this, pan, timeMillis)
                true //todo
            },
            onGestureStart = {
                state.onGestureStart(this)
            },
            onGestureEnd = {
                state.onGestureEnd(this)
            },
            onDoubleTap = {
                state.onDoubleTap(this, it)
            },
            onTap = { onTap() }
        )
    }
}
