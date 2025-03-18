package ru.marat.pdf_reader.layout.state

import kotlinx.coroutines.flow.StateFlow
import ru.marat.viewplayground.pdf_reader.reader.layout.items.Page

internal interface PagePositionController {

    var pagePositions: StateFlow<List<PagePosition>>

    fun calculatePositions(pages: List<Page>, params: LayoutParams): List<PagePosition>
}