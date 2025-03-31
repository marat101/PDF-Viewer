package ru.marat.pdf_reader.layout.state

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.util.fastMap
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ru.marat.pdf_reader.gestures.ReaderLayoutPositionState
import ru.marat.pdf_reader.gestures.rememberReaderLayoutPositionState
import ru.marat.pdf_reader.items.PageLayoutHelper
import ru.marat.pdf_reader.layout.saver.PageData
import ru.marat.pdf_reader.layout.saver.ReaderSaver
import ru.marat.pdf_reader.utils.cache.PdfViewerCache
import ru.marat.pdf_reader.utils.pdf_info.AndroidPdfInfoProvider
import ru.marat.pdf_reader.utils.pdf_info.PdfInfo
import ru.marat.pdf_reader.utils.pdf_info.PdfInfoProvider
import ru.marat.viewplayground.pdf_reader.reader.layout.items.Page

@Stable
class ReaderState internal constructor(
    internal val pdfViewerCache: PdfViewerCache? = null,
    val positionsState: ReaderLayoutPositionState,
    pdfInfoProvider: PdfInfoProvider,
    savedPages: List<PageData>? = null
) {


    companion object {
        private val readerCoroutineExceptionException =
            CoroutineExceptionHandler { context, throwable ->
                println("READER EXCEPTION")
                throwable.printStackTrace()
                throw throwable
            }
    }

    private var pdfInfo: PdfInfo? = null
    private val pageLayoutHelper = object : PageLayoutHelper {
        override val cache: PdfViewerCache?
            get() = pdfViewerCache

        override val parentLayoutInfo: StateFlow<LayoutInfo>
            get() = positionsState.layoutInfo

        override fun getPageSizeByIndex(index: Int): Flow<Size> {
            return positionsState.layoutInfo.map {
                it.pagePositions.getOrNull(index)?.rect?.size ?: Size.Unspecified
            }
        }

        override fun getPositionByIndex(index: Int): PagePosition? {
            return positionsState.layoutInfo.value.pagePositions.getOrNull(index)
        }
    }

    val pageCount: Int
        get() = pdfInfo?.pageCount ?: 0

    private val scope = CoroutineScope(Dispatchers.IO + readerCoroutineExceptionException)

    var loadingState by mutableStateOf<LoadingState>(LoadingState.Loading(0f))
        private set


    init {
        scope.launch {
            pdfInfo = pdfInfoProvider.get()
            var counter = 0
            val savedPages = savedPages ?: pdfViewerCache?.loadBoundaries()
            val p = savedPages?.fastMap {
                Page(
                    layoutHelper = pageLayoutHelper,
                    pageRenderer = pdfInfo!!.pageRenderer,
                    ratio = it.ratio,
                    index = it.index
                )
            } ?: List(pageCount) { index ->
                async {
                    Page(
                        layoutHelper = pageLayoutHelper,
                        pageRenderer = pdfInfo!!.pageRenderer,
                        ratio = pdfInfo!!.getPageAspectRatio(index),
                        index = index
                    ).apply {
                        counter++
                        withContext(Dispatchers.Main.immediate) {
                            loadingState =
                                LoadingState.Loading(counter.toFloat() / pageCount.toFloat())
                        }
                    }
                }
            }.fastMap { it.await() }
            pdfViewerCache?.saveBoundaries(p)
            withContext(Dispatchers.Main.immediate) {
                positionsState.layoutInfo.update { it.copy(pages = p) }
                loadingState = LoadingState.Ready
            }
        }
    }

    fun onDispose() {
        scope.cancel()
        positionsState.layoutInfo.value.pages.forEach { it.onDispose() }
    }
}

data class PagePosition(
    val index: Int,
    val start: Float,
    val end: Float,
    val rect: Rect,
)

@Composable
fun rememberReaderLayoutState(
    uri: Uri,
    enableCache: Boolean = true
): ReaderState {
    val context = LocalContext.current
    val pdfInfo = remember(uri, context) { AndroidPdfInfoProvider(context, uri) }
    val cache = remember(uri, enableCache) {
        if (enableCache) PdfViewerCache(context, uri.hashCode().toString()) else null
    }
    val scrollState = rememberReaderLayoutPositionState(pdfInfo, cache)
    return rememberSaveable(
        inputs = arrayOf(pdfInfo, cache),
        saver = ReaderSaver(pdfInfo, scrollState, cache)
    ) {
        ReaderState(
            pdfViewerCache = cache,
            positionsState = scrollState,
            pdfInfoProvider = pdfInfo,
        )
    }
}
