package ru.marat.pdf_reader.items

import androidx.compose.ui.unit.IntSize
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import ru.marat.pdf_reader.layout.state.LayoutInfo
import ru.marat.pdf_reader.layout.state.PagePosition
import ru.marat.pdf_reader.utils.cache.PDFViewerCache

interface PageLayoutHelper {

    val cache: PDFViewerCache?

    val parentLayoutInfo: StateFlow<LayoutInfo>

    fun getPageSizeByIndex(index: Int): Flow<IntSize>

    fun getPositionByIndex(index: Int): PagePosition?
}