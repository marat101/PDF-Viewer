package ru.marat.viewplayground.pdf_reader.reader.layout.items

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.unit.IntSize
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import ru.marat.pdf_reader.utils.render.PageRenderer
import kotlin.math.roundToInt

@Stable
class Page(
//    private val mutex: Mutex,
//    private val scope: CoroutineScope,
    private val pageRenderer: PageRenderer,
    val ratio: Float,
    val index: Int
) {

    private var job: Job? = null
    private var scalingJob: Job? = null

    val rect: Rect = Rect(
        left = 0f,
        top = 0f,
        right = 1080f,
        bottom = 1080f * ratio
    )

    private val scope = CoroutineScope(Dispatchers.Default)

    var bitmap by mutableStateOf<ImageBitmap?>(null)
    var scaledPage by mutableStateOf<ScaledPage?>(null)

    internal fun prepareBitmap() {
        job = scope.launch {
            kotlin.runCatching {
                pageRenderer.renderPage(index, rect) { bitmap = it }
            }.getOrElse { it.printStackTrace() }
        }
    }

    internal fun scalePage(scaleRect: Rect) {
        val scale = 1f //todo
        if (scaledPage?.rect == scaleRect) return
        scalingJob = scope.launch {
            kotlin.runCatching {
                pageRenderer.renderPageFragment(index, rect, scaleRect, scale) { scaledPage = it }
            }.getOrElse { it.printStackTrace() }
        }
    }

    internal fun removeScale() {
        scalingJob?.cancel()
        scalingJob = null
        scaledPage = null
    }

    internal fun onDispose() {
        job?.cancel()
        job = null
        scalingJob?.cancel()
        scalingJob = null
        closePageAndRecycleBitmaps()
    }

    internal fun close() {
        job?.cancel()
        job = null
        scalingJob?.cancel()
        scalingJob = null
        closePageAndRecycleBitmaps()
    }

    private fun closePageAndRecycleBitmaps() {
        scaledPage = null
        bitmap = null
    }
}

class ScaledPage private constructor( //todo
    val rect: Rect,
    val topLeft: Offset,
    internal val bitmap: ImageBitmap,
    val srcSize: IntSize = IntSize(bitmap.width, bitmap.height),
    val dstSize: IntSize = IntSize(
        rect.size.width.roundToInt(),
        rect.size.height.roundToInt()
    )
) {
    constructor(
        pageSize: Rect,
        scaledFragment: Rect,
        bitmap: ImageBitmap
    ) : this(
        rect = scaledFragment,
        topLeft = scaledFragment.topLeft - pageSize.topLeft,
        bitmap = bitmap
    )
}

fun ImageBitmap.size() = Size(width.toFloat(), height.toFloat())

