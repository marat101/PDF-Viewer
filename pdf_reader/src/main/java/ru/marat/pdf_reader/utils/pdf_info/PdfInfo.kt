package ru.marat.pdf_reader.utils.pdf_info

import ru.marat.pdf_reader.items.render.PageRenderer

interface PdfInfo {

    val pageRenderer: PageRenderer

    val pageCount: Int

    suspend fun getPageAspectRatio(index: Int): Float
}