package ru.marat.pdf_reader.layout.state

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.SaverScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Rect
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.withContext
import ru.marat.pdf_reader.utils.pdf_info.PdfInfo
import ru.marat.pdf_reader.utils.pdf_info.PdfInfoProvider
import ru.marat.viewplayground.pdf_reader.reader.layout.items.Page

@Stable
class ReaderLayoutState internal constructor(
    pdfInfoProvider: PdfInfoProvider,
    savedPages: List<PageData>? = null
) {

    private var pdfInfo: PdfInfo? = null

    val pageCount: Int
        get() = pdfInfo?.pageCount ?: 0

    private val mutex = Mutex()
    private val scope = CoroutineScope(Dispatchers.IO)
    var loadingState by mutableStateOf<LoadingState>(LoadingState.Loading)
        private set
    var pages by mutableStateOf<List<Page>>(emptyList())
        private set

    init {
        scope.launch {
            pdfInfo = pdfInfoProvider.get()
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
                )
            }
            withContext(Dispatchers.Main.immediate) {
                this@ReaderLayoutState.pages = p
                loadingState = LoadingState.Ready
            }
        }
    }


    fun onDispose() {
        pdfInfo?.close()
    }
}

internal class ReaderSaver(private val provider: PdfInfoProvider) :
    Saver<ReaderLayoutState, List<List<Any>>> {
    override fun restore(value: List<List<Any>>): ReaderLayoutState {
        return ReaderLayoutState(
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

@Composable
fun rememberReaderLayoutState(pdfInfo: PdfInfoProvider): ReaderLayoutState {
    return rememberSaveable(
        key = pdfInfo.toString(),
        saver = ReaderSaver(pdfInfo)
    ) {
        ReaderLayoutState(pdfInfo)
    }
}
