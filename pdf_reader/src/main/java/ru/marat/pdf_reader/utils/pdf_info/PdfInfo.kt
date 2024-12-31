package ru.marat.pdf_reader.utils.pdf_info

import ru.marat.pdf_reader.utils.render.PageRenderer

interface PdfInfo {

    val pageRenderer: PageRenderer

    val pageCount: Int

    fun getPageAspectRatio(index: Int): Float

    fun close()
}