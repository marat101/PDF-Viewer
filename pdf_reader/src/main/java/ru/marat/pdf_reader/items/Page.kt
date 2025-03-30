package ru.marat.viewplayground.pdf_reader.reader.layout.items

import androidx.compose.runtime.Stable
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
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flatMapConcat
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
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

    private val isLoaded = MutableStateFlow(false)

    private val scope = CoroutineScope(Dispatchers.Default)

    val size: StateFlow<Size> = layoutHelper.getPageSizeByIndex(index)
        .stateIn(scope, SharingStarted.Lazily, Size.Unspecified)

    private val scaledRect = MutableStateFlow<Rect?>(null)

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
    }.stateIn(scope, SharingStarted.WhileSubscribed(3000), null)

    val scaledPage = isLoaded.flatMapLatest { loaded ->
        scaledRect.debounce {
            if (it == null) 0 else 200
        }.flatMapLatest { scaledRect ->
            flow {
                if (!loaded || scaledRect == null || scaledRect.width <= 0 || scaledRect.height <= 0) {
                    emit(null)
                    return@flow
                }
                while (bitmap.value == null) {
                    currentCoroutineContext().ensureActive()
                }
                val bm = drawPageFragment(layoutHelper.parentLayoutInfo.value.zoom, scaledRect)
                emit(bm)
            }
        }
    }.stateIn(scope, SharingStarted.WhileSubscribed(3000), null)

    private suspend fun drawPage(newSize: Size): ImageBitmap? {
        return kotlin.runCatching {
            var bm: ImageBitmap = pageRenderer.renderPage(
                index = index,
                pageSize = newSize
            )
            println("bitmap size ${bm.size()}")
            bm
        }.getOrElse {
            it.printStackTrace()
            throw it
        }
    }

    internal suspend fun drawPageFragment(scale: Float, fragment: Rect): ScaledPage =
        kotlin.runCatching {
            println("page $index scaled fragment $fragment")
            pageRenderer.renderPageFragment(index, size.value.toRect(), fragment, scale)
                .also { println("page $index scaled bitmap size ${it.bitmap.size()}") }
        }.getOrElse {
            it.printStackTrace()
            throw it
        }

    internal fun setVisibleFragment(rect: Rect) {
        scaledRect.value = rect
    }

    internal fun removeScale() {
        scaledRect.value = null
    }

    internal fun onLoad() {
        scope.launch(Dispatchers.Main) {
            isLoaded.emit(true)
        }
    }

    internal fun onDispose() {
        scope.launch(Dispatchers.Main) {
            scaledRect.emit(null)
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

