package ru.marat.pdf_reader.items

import androidx.compose.ui.geometry.Size
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import ru.marat.pdf_reader.layout.state.LayoutParams
import ru.marat.pdf_reader.layout.state.PagePosition

interface PageLayoutHelper {

    val parentLayoutParams: StateFlow<LayoutParams>

    fun getPageSizeByIndex(index: Int): Flow<Size>

    fun getPositionByIndex(index: Int): PagePosition?
}