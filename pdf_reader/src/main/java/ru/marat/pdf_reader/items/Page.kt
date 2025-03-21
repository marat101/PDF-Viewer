package ru.marat.viewplayground.pdf_reader.reader.layout.items

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.toRect
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.unit.IntSize
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import ru.marat.pdf_reader.items.PageLayoutHelper
import ru.marat.pdf_reader.utils.render.PageRenderer
import kotlin.math.roundToInt


@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
@Stable
class Page(
    internal val layoutHelper: PageLayoutHelper,
    private val pageRenderer: PageRenderer,
    val ratio: Float,
    val index: Int
) {

    private var job: Job? = null
    private var scalingJob: Job? = null


//    var viewportSize by mutableStateOf(Size.Unspecified)
//        internal set

    private val isLoaded = MutableStateFlow(false)

    private val scope = CoroutineScope(Dispatchers.Default)

    val size: StateFlow<Size> = layoutHelper.getPageSizeByIndex(index)
        .stateIn(scope, SharingStarted.Lazily, Size.Unspecified)

    val bitmap = isLoaded.flatMapLatest { loaded ->
        size.debounce(300).flatMapLatest { newLayoutSize ->
            flow {
                if (!loaded) {
                    emit(null)
                    return@flow
                }

                val bm = drawPage(newLayoutSize)
                emit(bm)
            }
        }
    }.stateIn(scope, SharingStarted.Lazily, null)

    var scaledPage by mutableStateOf<ScaledPage?>(null)

    private suspend fun drawPage(newSize: Size): ImageBitmap? {
        return kotlin.runCatching {
            var bm: ImageBitmap? = null
            pageRenderer.renderPage(
                index = index,
                pageSize = newSize
            ) {
                bm = it
                println("bitmap size ${bm?.size()}")
                println("bitmap size in bytes: ${sizeInBytes(bm)}")
            }
            bm
        }.getOrElse {
            it.printStackTrace()
            throw it
        }
    }

    private fun sizeInBytes(bm: ImageBitmap): Long { //todo delete
        return (bm.height * bm.width) * 4L
    }

    internal fun scalePage(scale: Float, scaleRect: Rect) {
        if (scaledPage?.rect == scaleRect) return
        scalingJob = scope.launch {
            kotlin.runCatching {
                pageRenderer.renderPageFragment(index, size.value.toRect(), scaleRect, scale) {
                    scaledPage = it
                }
            }.getOrElse { it.printStackTrace() }
        }
    }

    internal fun removeScale() {
        scalingJob?.cancel()
        scalingJob = null
        scaledPage = null
    }

    internal fun onLoad() {
        scope.launch(Dispatchers.Main.immediate) {
            isLoaded.emit(true)
        }
    }

    internal fun onDispose() {
        job?.cancel()
        job = null
        scalingJob?.cancel()
        scalingJob = null
        scaledPage = null
        scope.launch(Dispatchers.Main.immediate) {
            isLoaded.emit(false)
        }
    }
}

class ScaledPage private constructor( // todo constructor
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

