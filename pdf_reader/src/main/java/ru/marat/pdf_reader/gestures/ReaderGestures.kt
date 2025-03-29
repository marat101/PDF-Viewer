package ru.marat.pdf_reader.gestures

import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

fun Modifier.readerGestures(
    state: ReaderLayoutPositionState,
    onTap: () -> Unit
) = this.pointerInput(state) {
    coroutineScope {
        detectTransformGestures(
            cancelIfZoomCanceled = false,
            onGesture = { centroid, pan, zoom, timeMillis ->
                launch { state.onZoom(this, zoom, centroid) }
                launch { state.onScroll(this, pan, timeMillis) }
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
