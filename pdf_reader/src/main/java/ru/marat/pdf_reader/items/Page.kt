package ru.marat.viewplayground.pdf_reader.reader.layout.items

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.toIntRect
import androidx.compose.ui.unit.toIntSize
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ru.marat.pdf_reader.items.PageLayoutHelper
import ru.marat.pdf_reader.items.render.PageRenderer


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

    val size: StateFlow<IntSize> = layoutHelper.getPageSizeByIndex(index)
        .stateIn(scope, SharingStarted.Lazily, IntSize.Zero)

    private val scaledRect = MutableStateFlow<IntRect?>(null)

    internal val bitmap = isLoaded.flatMapLatest { loaded ->
        size.debounce {
            if (size.value == it) 0 else 300
        }.flatMapLatest { newLayoutSize ->
            flow {
                if (!loaded) {
                    emit(null)
                    return@flow
                }

                val savedBitmap = withContext(Dispatchers.IO) {
                    layoutHelper.cache?.getPage(index)?.asImageBitmap()
                }
                emit(savedBitmap)
                if (savedBitmap?.size()?.toIntSize() == newLayoutSize) return@flow
                val bm = drawPage(newLayoutSize)
                emit(bm)
                bm?.asAndroidBitmap()?.let {
                    withContext(Dispatchers.IO) { layoutHelper.cache?.savePage(index, it) }
                }
            }
        }
    }.stateIn(scope, SharingStarted.WhileSubscribed(3000), null)

    val scaledPage = isLoaded.flatMapLatest { loaded ->
        scaledRect.debounce {
            if (it == null || loaded == false) 0 else 200
        }.flatMapLatest { scaledRect ->
            flow {
                if (!loaded || scaledRect == null || scaledRect.width <= 0 || scaledRect.height <= 0) {
                    emit(null)
                    return@flow
                }
                while (bitmap.value == null) {
                    currentCoroutineContext().ensureActive()
                }
                val bitmapSize = bitmap.value?.size() ?: return@flow
                val size = size.value
                if (size == IntSize.Zero) return@flow
                val zoom = layoutHelper.parentLayoutInfo.value.zoom
                if (bitmapSize.width >= (size.width * zoom)) return@flow

                val bm = drawPageFragment(zoom, scaledRect)
                emit(bm)
            }
        }
    }.stateIn(scope, SharingStarted.WhileSubscribed(3000), null)

    private suspend fun drawPage(newSize: IntSize): ImageBitmap? {
        return kotlin.runCatching {
            var bm: ImageBitmap = pageRenderer.renderPage(
                index = index,
                pageSize = newSize
            )
            bm
        }.getOrElse {
            it.printStackTrace()
            throw it
        }
    }

    private suspend fun drawPageFragment(scale: Float, fragment: IntRect): ScaledPage =
        kotlin.runCatching {
            pageRenderer.renderPageFragment(index, size.value.toIntRect(), fragment, scale)
        }.getOrElse {
            it.printStackTrace()
            throw it
        }

    internal fun setVisibleFragment(rect: IntRect) {
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

@Immutable
class ScaledPage(
    pageSize: IntRect,
    scaledFragment: IntRect,
    internal val bitmap: ImageBitmap
) {
    val rect: IntRect = scaledFragment
    val topLeft: IntOffset = scaledFragment.topLeft - pageSize.topLeft
    val dstSize: IntSize = IntSize(
        rect.size.width,
        rect.size.height
    )
}

fun Offset.toIntOffset() = IntOffset(x.toInt(), y.toInt())

fun ImageBitmap.size() = Size(width.toFloat(), height.toFloat())

