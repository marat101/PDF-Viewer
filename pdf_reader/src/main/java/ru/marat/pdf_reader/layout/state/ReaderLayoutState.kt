package ru.marat.pdf_reader.layout.state

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.SaverScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.isSpecified
import androidx.compose.ui.geometry.isUnspecified
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.util.fastForEach
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ru.marat.pdf_reader.gestures.ReaderLayoutPositionState
import ru.marat.pdf_reader.gestures.rememberReaderLayoutPositionState
import ru.marat.pdf_reader.utils.pdf_info.AndroidPdfInfoProvider
import ru.marat.pdf_reader.utils.pdf_info.PdfInfo
import ru.marat.pdf_reader.utils.pdf_info.PdfInfoProvider
import ru.marat.viewplayground.pdf_reader.reader.layout.items.Page

@Stable
class ReaderLayoutState internal constructor(
    internal val scrollState: ReaderLayoutPositionState,
    pdfInfoProvider: PdfInfoProvider,
    savedPages: List<PageData>? = null
) {

    private var pdfInfo: PdfInfo? = null

    val pageCount: Int
        get() = pdfInfo?.pageCount ?: 0

    private val scope = CoroutineScope(Dispatchers.IO)
    var loadingState by mutableStateOf<LoadingState>(LoadingState.Loading(0f))
        private set

    private var _viewportSize = mutableStateOf(Size.Unspecified)
    var viewportSize
        get() = _viewportSize.value
        internal set(value) {
            if (_viewportSize.value == value &&
                pagePositions.isNotEmpty() &&
                viewportSize.isSpecified
            ) return
            val positions = mutableListOf<PagePosition>()
            var offset = 0f
            pages.fastForEach {
                val pos = PagePosition(
                    index = it.index,
                    start = offset,
                    end = (value.width * it.ratio) + offset
                )
                offset += pos.end - pos.start
                positions.add(pos)
            }
            pagePositions = positions
            _viewportSize.value = value
            scrollState.onViewportSizeChanged(
                value,
                (offset - value.height).coerceAtLeast(0f)
            )
        }

    var pagePositions by mutableStateOf<List<PagePosition>>(emptyList())
        private set

    var pages by mutableStateOf<List<Page>>(emptyList())
        private set

    init {
        scope.launch {
            pdfInfo = pdfInfoProvider.get()
//            println("")
            var counter = 0
            val p = savedPages?.map {
                Page(
                    pageRenderer = pdfInfo!!.pageRenderer,
                    ratio = it.ratio,
                    index = it.index
                )
            } ?: List(pageCount) { index ->
                Page(
                    pageRenderer = pdfInfo!!.pageRenderer,
                    ratio = pdfInfo!!.getPageAspectRatio(index),
                    index = index
                ).apply {
                    counter++
                    withContext(Dispatchers.Main.immediate) {
                        loadingState = LoadingState.Loading(counter.toFloat() / pageCount.toFloat())
                    }
                }
            }
            withContext(Dispatchers.Main.immediate) {
                this@ReaderLayoutState.pages = p
                loadingState = LoadingState.Ready
            }
        }
    }


    fun onDispose() {
        pages.forEach { it.onDispose() }
        pdfInfo?.close()
    }
}

internal class ReaderSaver(
    private val provider: PdfInfoProvider,
    private val scrollState: ReaderLayoutPositionState
) :
    Saver<ReaderLayoutState, List<List<Any>>> {
    override fun restore(value: List<List<Any>>): ReaderLayoutState {
        return ReaderLayoutState(
            scrollState = scrollState,
            pdfInfoProvider = provider,
            savedPages = value.map { PageData(it[0] as Int, it[1] as Float) }
        )
    }

    override fun SaverScope.save(value: ReaderLayoutState): List<List<Any>> {
        return value.pages.map { page -> listOf(page.index, page.ratio) }
    }
}

data class PageData(
    val index: Int,
    val ratio: Float,
)

data class PagePosition(
    val index: Int,
    val start: Float,
    val end: Float
)

@Composable
fun rememberReaderLayoutState(
    uri: Uri
): ReaderLayoutState {
    val context = LocalContext.current
    val pdfInfo = remember(uri) { AndroidPdfInfoProvider(context, uri) }
    val scrollState = rememberReaderLayoutPositionState()
    return rememberSaveable(
        inputs = arrayOf(pdfInfo),
        saver = ReaderSaver(pdfInfo, scrollState)
    ) {
        ReaderLayoutState(scrollState, pdfInfo)
    }
}
