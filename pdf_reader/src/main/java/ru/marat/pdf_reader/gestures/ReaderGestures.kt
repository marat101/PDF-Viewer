package ru.marat.pdf_reader.gestures

import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import kotlinx.coroutines.coroutineScope

fun Modifier.readerGestures(state: ReaderLayoutPositionState) = this.pointerInput(state) {
    coroutineScope {
        detectTransformGestures(
            cancelIfZoomCanceled = false,
//        canConsumeGesture = { pan, zoom ->
//            true //todo
//        },
            onGesture = { centroid, pan, zoom, timeMillis ->
                state.onScroll(this, pan, timeMillis)
            },
            onGestureStart = {
                state.onGestureStart(this)
            },
            onGestureEnd = {
                state.onGestureEnd(this)
            }
        )
    }
}
