package ru.marat.pdf_reader.utils.cache

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.ImageBitmap
import ru.marat.pdf_reader.layout.saver.PageData

@Immutable
interface PDFViewerCache {

    companion object {
        const val CACHE_ROOT_PACKAGE = "ru.marat.pdfViewer"
    }

    suspend fun saveBoundaries(boundaries: List<PageData>)
    suspend fun getBoundaries(): List<PageData>?

    suspend fun savePage(index: Int, bm: ImageBitmap)
    suspend fun getPage(index: Int): ImageBitmap?

    suspend fun clear()
}