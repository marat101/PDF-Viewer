package ru.marat.pdf_reader.items

import androidx.compose.ui.geometry.Size
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import ru.marat.pdf_reader.layout.state.LayoutInfo
import ru.marat.pdf_reader.layout.state.PagePosition
import ru.marat.pdf_reader.utils.cache.PdfViewerCache

interface PageLayoutHelper {

    val cache: PdfViewerCache?

    val parentLayoutInfo: StateFlow<LayoutInfo>

    fun getPageSizeByIndex(index: Int): Flow<Size>

    fun getPositionByIndex(index: Int): PagePosition?
}